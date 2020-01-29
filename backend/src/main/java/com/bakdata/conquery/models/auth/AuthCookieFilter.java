package com.bakdata.conquery.models.auth;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.ext.Provider;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.http.HttpHeader;

/**
 * This filter is only used for the admin endpoints which serve static sites
 * with links to these endpoints. Since it is not possible to set the
 * authorization header from static sites and adding the token to all links and
 * fetches on these sites, we use a cookie that is automatically send from the
 * browser with every request.
 *
 */
@Slf4j
@Provider
@PreMatching
public class AuthCookieFilter implements ContainerRequestFilter, ContainerResponseFilter {

	private static final String ACCESS_TOKEN = "access_token";
	private static final String PREFIX = "bearer";
	// Define a maximum age since most browsers use session restoring making session cookies virtual permanent (see https://developer.mozilla.org/en-US/docs/Web/HTTP/Cookies)
	private static final int COOKIE_MAX_AGE = (int) TimeUnit.HOURS.toSeconds(12);

	/**
	 * The filter tries to extract a token from a cookie and puts it into the the
	 * authorization header of the request. This simplifies the token retrival
	 * process for the realms.
	 */
	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		Cookie cookie = requestContext.getCookies().get(ACCESS_TOKEN);
		if (cookie != null) {
			// Get the token from the cookie and put it into the header
			requestContext.getHeaders().add(HttpHeaders.AUTHORIZATION, String.join(" ", PREFIX, cookie.getValue()));
			// Remove the cookie for the rest of this processing
			requestContext.getCookies().remove(ACCESS_TOKEN);
		}

	}

	/**
	 * Sets a cookie with the access_token from the requests query string.
	 */
	@Override
	public void filter(ContainerRequestContext request, ContainerResponseContext response) throws IOException {
		Cookie cookie = request.getCookies().get(ACCESS_TOKEN);
		String token = request.getUriInfo().getQueryParameters().getFirst(ACCESS_TOKEN);

		// Set cookie only if a token is present
		if (token != null && !token.isEmpty()) {
			if (cookie != null) {
				log.debug("Overwriting {} cookie", ACCESS_TOKEN);
			}
			response.getHeaders().add(
				HttpHeader.SET_COOKIE.toString(),
				new NewCookie(ACCESS_TOKEN, token, "/admin", null, 0, null, COOKIE_MAX_AGE, null, false, false));
		}
	}

}
