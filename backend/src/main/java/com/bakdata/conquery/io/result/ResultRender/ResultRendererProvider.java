package com.bakdata.conquery.io.result.ResultRender;

import java.net.URL;
import java.nio.charset.Charset;
import java.util.Collection;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.dropwizard.jersey.setup.JerseyEnvironment;

@CPSBase
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
public interface ResultRendererProvider {

	/**
	 * The provider can return a result url if its renderer supports the execution type.
	 * If additionally allProviders is set to true it should output an url.
	 * @param exec The execution whose result needs to be rendered.
	 * @param uriBuilder The pre-configured builder for the url.
	 * @param allProviders A flag that should override internal "hide-this-url" flags.
	 * @return An Optional with the url or an empty optional.
	 */
	Collection<URL> generateResultURLs(ManagedExecution<?> exec, UriBuilder uriBuilder, boolean allProviders);

	Response createResult(Subject subject, ManagedExecution<?> exec, Dataset datasetId, boolean pretty, Charset charset);

	void registerResultResource(JerseyEnvironment environment, ManagerNode manager);
}
