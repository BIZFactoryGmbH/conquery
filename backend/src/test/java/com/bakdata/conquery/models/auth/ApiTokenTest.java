package com.bakdata.conquery.models.auth;

import static com.bakdata.conquery.models.auth.apitoken.ApiTokenCreator.*;
import static org.assertj.core.api.Assertions.*;

import com.bakdata.conquery.models.auth.apitoken.ApiTokenCreator;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.util.CharArrayBuffer;
import org.junit.jupiter.api.Test;

import java.util.Random;

@Slf4j
public class ApiTokenTest {

	@Test
	public void checkToken () {
		final ApiTokenCreator apiTokenCreator = new ApiTokenCreator(new Random(1));

		final CharArrayBuffer token = apiTokenCreator.createToken();

		log.info("Testing token: {}", token);

		assertThat(token).hasSize(TOKEN_LENGTH + TOKEN_PREFIX.length() + 1);

		assertThat(token).matches(TOKEN_PREFIX + "_" + "[\\w\\d_]{"+ TOKEN_LENGTH +"}");

		assertThat(token.toString().substring(TOKEN_PREFIX.length()+2)).containsPattern("[a-zA-Z]");
	}
}
