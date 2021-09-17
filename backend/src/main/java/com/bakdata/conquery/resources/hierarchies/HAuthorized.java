package com.bakdata.conquery.resources.hierarchies;

import java.security.Principal;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response.Status;

import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.entities.Userish;
import lombok.Getter;
import lombok.Setter;
import org.glassfish.jersey.server.ContainerRequest;

@Getter
@Setter
public abstract class HAuthorized {

	protected Userish user;
	@Context
	protected ContainerRequest request;
	@Context 
	protected HttpServletRequest servletRequest;

	@PostConstruct
	public void init() {
		/*
		 *  We need to extract the user ourself here because @Auth does not work anymore on fields.
		 *  See https://github.com/dropwizard/dropwizard/issues/3407
		 */
		user = provide();
		if(user == null) {
			throw new WebApplicationException(Status.UNAUTHORIZED);
		}
	}
	
    /** From PrincipalContainerRequestValueFactory
     * @return {@link Principal} stored on the request, or {@code null}
     *         if no object was found.
     */
    public Userish provide() {
        final Userish principal = (Userish) request.getSecurityContext().getUserPrincipal();
        if (principal == null) {
            throw new IllegalStateException("Cannot inject a custom principal into unauthenticated request");
        }
        return principal;
    }
}
