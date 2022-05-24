package com.bakdata.conquery.resources.api;

import static com.bakdata.conquery.io.result.ResultUtil.checkSingleTableResult;
import static com.bakdata.conquery.io.result.ResultUtil.determineCharset;
import static com.bakdata.conquery.resources.ResourceConstants.DATASET;
import static com.bakdata.conquery.resources.ResourceConstants.QUERY;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import com.bakdata.conquery.apiv1.AdditionalMediaTypes;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.CsvResultRenderer;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.query.SingleTableResult;
import com.bakdata.conquery.resources.ResourceConstants;
import io.dropwizard.auth.Auth;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Path("datasets/{" + DATASET + "}/result/")
public class ResultCsvResource {

	public static final String GET_RESULT_PATH_METHOD = "getAsCsv";
	@Inject
	private CsvResultRenderer processor;
	@Inject
	private ConqueryConfig config;

	public static <E extends ManagedExecution<?> & SingleTableResult> URL getDownloadURL(UriBuilder uriBuilder, E exec) throws MalformedURLException {
		return uriBuilder
				.path(ResultCsvResource.class)
				.resolveTemplate(ResourceConstants.DATASET, exec.getDataset().getName())
				.path(ResultCsvResource.class, GET_RESULT_PATH_METHOD)
				.resolveTemplate(ResourceConstants.QUERY, exec.getId().toString())
				.build()
				.toURL();
	}

	@GET
	@Path("{" + QUERY + "}.csv")
	@Produces(AdditionalMediaTypes.CSV)
	public Response getAsCsv(
			@Auth Subject subject,
			@PathParam(DATASET) Dataset dataset,
			@PathParam(QUERY) ManagedExecution<?> execution,
			@HeaderParam("subject-agent") String userAgent,
			@QueryParam("charset") String queryCharset,
			@QueryParam("pretty") Optional<Boolean> pretty) {
		checkSingleTableResult(execution);
		log.info("Result for {} download on dataset {} by subject {} ({}).", execution, dataset.getId(), subject.getId(), subject.getName());
		return processor.createResult(subject, execution, dataset, pretty.orElse(Boolean.TRUE), determineCharset(userAgent, queryCharset), () -> {
		});
	}
}
