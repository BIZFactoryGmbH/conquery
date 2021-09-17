package com.bakdata.conquery.models.auth.apitoken;

import com.bakdata.conquery.apiv1.auth.ApiTokenDataRepresentation;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.io.storage.xodus.stores.SerializingStore;
import com.bakdata.conquery.io.storage.xodus.stores.XodusStore;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationInfo;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationRealm;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.entities.Userish;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.config.XodusConfig;
import com.bakdata.conquery.models.config.XodusStoreFactory;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.fasterxml.jackson.databind.ObjectMapper;
import jetbrains.exodus.ExodusException;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.EnvironmentClosedException;
import jetbrains.exodus.env.Environments;
import jetbrains.exodus.env.Store;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.util.CharArrayBuffer;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.realm.AuthenticatingRealm;
import org.apache.shiro.util.Destroyable;

import javax.validation.Validator;
import javax.validation.constraints.NotNull;
import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;


/**
 * This realm provides and checks long-living API tokens. The tokens support a limited scope of actions that is backed
 * by the actual permissions of the invoking user.
 *
 */
@Slf4j
public class ApiTokenRealm extends AuthenticatingRealm implements ConqueryAuthenticationRealm, Destroyable {

	private static final int ENVIRONMNENT_CLOSING_RETRYS = 2;
	private static final int ENVIRONMNENT_CLOSING_TIMEOUT = 2; // seconds

	private final Path storageDir;
	private final XodusConfig storeConfig;
	private final String storeName = "api-token";
	private final Validator validator;
	private final ObjectMapper objectMapper;
	private final ArrayList<Store> openStoresInEnv = new ArrayList<>();
	private final MetaStorage storage;
	private final ApiTokenCreator apiTokenCreator = new ApiTokenCreator();

	private transient Environment tokenEnvironment;
	private transient SerializingStore<byte[], ApiTokenData> tokenDataStore;
	private transient SerializingStore<UUID, ApiTokenData.MetaData> tokenMetaDataStore;

	public ApiTokenRealm(MetaStorage storage, Path storageDir, XodusConfig storeConfig, Validator validator, ObjectMapper objectMapper) {
		this.storage = storage;
		this.storageDir = storageDir;
		this.storeConfig = storeConfig;
		this.validator = validator;
		this.objectMapper = objectMapper;
		this.setAuthenticationTokenClass(ApiToken.class);
	}


	@Override
	protected void onInit() {
		super.onInit();
		// Open/create the database/store
		File tokenStore = new File(storageDir.toFile(), storeName);
		tokenEnvironment = Environments.newInstance(tokenStore, storeConfig.createConfig());
		tokenDataStore = new SerializingStore<>(new XodusStore(tokenEnvironment,"DATA", openStoresInEnv, Environment::close, XodusStoreFactory::removeEnvironmentHook),validator, objectMapper, byte[].class, ApiTokenData.class, true, false, null);
		tokenMetaDataStore = new SerializingStore<>(new XodusStore(tokenEnvironment,"META", openStoresInEnv, Environment::close, XodusStoreFactory::removeEnvironmentHook),validator, objectMapper, UUID.class, ApiTokenData.MetaData.class, true, false, null);
	}

	@Override
	public ConqueryAuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
		if (!(token instanceof ApiToken)) {
			return null;
		}

		final CharArrayBuffer credentials = ((ApiToken) token).getCredentials();
		byte[] tokenHash = ApiTokenCreator.hashToken(credentials);

		// Clear the token
		credentials.clear();


		ApiTokenData tokenData = tokenDataStore.get(tokenHash);
		if (tokenData == null) {
			log.trace("Unknown token, cannot map token hash to token data. Aborting authentication");
			throw new IncorrectCredentialsException();
		}

		final ApiTokenData.MetaData metaData = new ApiTokenData.MetaData(LocalDate.now());
		tokenMetaDataStore.update(tokenData.getId(), metaData);

		final UserId userId = tokenData.getUserId();
		final User user = storage.getUser(userId);

		if (user == null) {
			throw new UnknownAccountException("The UserId does not map to a user: " + userId);
		}

		return new ConqueryAuthenticationInfo(new UserToken(user, tokenData), token, this, false);
	}

	public ApiToken createApiToken(User user, ApiTokenDataRepresentation.Request tokenRepresentation) {

		CharArrayBuffer token = null;
		byte[] hash = null;

		synchronized (this) {
			// Generate a token that does not collide with another tokens hash
			do {
				token = apiTokenCreator.createToken();
				hash = ApiTokenCreator.hashToken(token);

			} while(tokenDataStore.get(hash) != null);

			final ApiTokenData apiTokenData = tokenRepresentation.toInternalRepresentation(user, hash, storage);

			tokenDataStore.add(hash, apiTokenData);
		}

		return new ApiToken(token);
	}

	public Collection<ApiTokenDataRepresentation.Response> listUserToken(Userish user) {
		ArrayList<ApiTokenDataRepresentation.Response> summary = new ArrayList<>();

		final Collection<ApiTokenData> allToken = tokenDataStore.getAll();
		for (ApiTokenData apiTokenData : allToken) {
			// Find all token data belonging to a user
			if (!user.getId().equals(apiTokenData.getUserId())){
				continue;
			}

			// Fill in the response with the details
			final ApiTokenDataRepresentation.Response response = new ApiTokenDataRepresentation.Response();
			response.setCreationDate(apiTokenData.getCreationDate());
			response.setName(apiTokenData.getName());
			response.setExpirationDate(apiTokenData.getExpirationDate());
			response.setScopes(apiTokenData.getScopes());

			// If the token was ever used it should have an meta data entry
			ApiTokenData.MetaData meta = tokenMetaDataStore.get(apiTokenData.getId());
			if (meta != null) {
				response.setLastUsed(meta.getLastUsed());
			}
			summary.add(response);
		}
		return summary;
	}

	public void deleteToken(@NotNull Userish user, @NonNull UUID tokenId) {
		AtomicReference<byte[]> targetHash = new AtomicReference<>();

		// Find the corresponding token data and extract its hash
		tokenDataStore.forEach((bytes, tokenData, size) -> {
			if (tokenId.equals(tokenData.getId())) {
				targetHash.set(tokenData.getTokenHash());
			}
		});


		final byte[] hash = targetHash.get();
		if (hash == null) {
			log.warn("No token with id {} was found", tokenId);
			return;
		}

		synchronized (this) {
			// This should never return null
			ApiTokenData data = tokenDataStore.get(hash);
			if (data == null) {
				throw new IllegalStateException("Unable to retrieve token data for hash.");
			}

			user.authorize(data, Ability.DELETE);

			tokenDataStore.remove(hash);
			tokenMetaDataStore.remove(tokenId);
		}

		Arrays.fill(hash,(byte) 0);
	}

	@Override
	public void destroy() throws InterruptedException {
		for(int retries = 0; retries < ENVIRONMNENT_CLOSING_RETRYS; retries++) {
			try {
				log.info("Closing the password environment.");
				tokenEnvironment.close();
				return;
			}
			catch (EnvironmentClosedException e) {
				log.warn("Password environment was already closed, which is odd but mayby the stop() lifecycle event fired twice");
				return;
			}
			catch (ExodusException e) {
				if (retries == 0) {
					log.info("The environment is still working on some transactions. Retry");
				}
				log.info("Waiting for {} seconds to retry.", ENVIRONMNENT_CLOSING_TIMEOUT);
				Thread.sleep(ENVIRONMNENT_CLOSING_TIMEOUT*1000 /* milliseconds */);
			}
		}
		// Close the environment with force
		log.info("Closing the environment forcefully");
		tokenEnvironment.getEnvironmentConfig().setEnvCloseForcedly(true);
		tokenEnvironment.close();

	}
}
