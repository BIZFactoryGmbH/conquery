package com.bakdata.conquery.models.config.auth;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import javax.ws.rs.client.Client;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationRealm;
import com.bakdata.conquery.models.auth.basic.JWTokenHandler;
import com.bakdata.conquery.models.auth.oidc.IntrospectionDelegatingRealm;
import com.bakdata.conquery.models.auth.oidc.keycloak.KeycloakApi;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.id.ClientID;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.servlets.tasks.Task;
import io.dropwizard.validation.ValidationMethod;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;


/**
 * Bridge class for realms that authenticate users by submitting their token to the IDP for introspection.
 */
@Slf4j
public class IntrospectionDelegatingRealmFactory extends Configuration {

	public static final String CONFIDENTIAL_CREDENTIAL = "secret";

	/**
	 * Attribute of keycloak group object that holds the conquery group id to map to.
	 * If it is blank, the group mapping will be skipped.
	 */
	@Getter
	@Setter
	public String groupIdAttribute = "";


	private transient AuthzClient authClient;

	public ConqueryAuthenticationRealm createRealm(ManagerNode managerNode) {

		// Register token extractor for JWT Tokens
		managerNode.getAuthController().getAuthenticationFilter().registerTokenExtractor(JWTokenHandler::extractToken);

		// At start up, try tp retrieve the idp client api object if possible. If the idp service is not up don't fail start up.
		authClient = getAuthClient(false);

		// Register task to retrieve the idp client api, so the realm can be used, when the idp service is available.
		if (managerNode.getEnvironment().admin() != null) {
			managerNode.getEnvironment().admin().addTask(new Task("keycloak-update-authz-client") {

				@Override
				public void execute(Map<String, List<String>> parameters, PrintWriter output) throws Exception {
					// Fail if api could not be received
					authClient = getAuthClient(true);
				}
			});
		}

		// Setup keycloak api
		final Client client = new JerseyClientBuilder(managerNode.getEnvironment()).build("keycloak-api");
		final KeycloakApi keycloakApi = new KeycloakApi(this, client);

		return new IntrospectionDelegatingRealm(managerNode.getStorage(), this, keycloakApi);
	}


	/**
	 * Retrieves the token endpoint from the idp client api. If the api is not available authenticatio is not possible
	 * @return
	 */
	@JsonIgnore
	public String getTokenEndpoint(){
		return getAuthClient(true).getServerConfiguration().getTokenEndpoint();
	}

	@JsonIgnore
	public String getIntrospectionEndpoint() {
		return getAuthClient(true).getServerConfiguration().getIntrospectionEndpoint();
	}

	@JsonIgnore
	public String getClientId() {
		return getResource();
	}

	@JsonIgnore
	public String getClientSecret() {
		return (String) credentials.get(CONFIDENTIAL_CREDENTIAL);
	}


	@JsonIgnore
	public final ClientAuthentication getClientAuthentication() {
		return new ClientSecretBasic(new ClientID(getClientId()), new Secret(getClientSecret()));
	}

	@JsonIgnore
	public AuthzClient getAuthClient(boolean exceptionOnFailedRetrieval) {
		if(authClient != null) {
			return authClient;
		}
		try {
			// This tries to contact the identity providers discovery endpoint and can possibly timeout
			return AuthzClient.create(this);
		} catch (RuntimeException e) {
			log.warn("Unable to establish connection to auth server.", log.isTraceEnabled()? e : null );
			if(exceptionOnFailedRetrieval) {
				throw e;
			}
		}
		return null;
	}

	///// VALIDATION

	@JsonIgnore
	@ValidationMethod(message = "Realm was emtpy")
	public boolean isRealmFilled() {
		return realm != null && !realm.isBlank();
	}

	@JsonIgnore
	@ValidationMethod(message = "Resource was emtpy")
	public boolean isResourceFilled() {
		return resource != null && !resource.isBlank();
	}

	@JsonIgnore
	@ValidationMethod(message = "Secret not found")
	public boolean isSecretFilled() {
		if(credentials == null) {
			return false;
		}

		Object secret = credentials.get(CONFIDENTIAL_CREDENTIAL);
		if(secret == null) {
			return false;
		}

		if(!(secret instanceof String)) {
			return false;
		}

		return !((String)secret).isBlank();
	}
}
