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
import com.bakdata.conquery.models.config.ExcelResultProvider;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.query.SingleTableResult;
import com.bakdata.conquery.resources.ResourceConstants;
import io.dropwizard.auth.Auth;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Path("datasets/{" + DATASET + "}/result/")
public class ResultExcelResource {

	public static final String GET_RESULT_PATH_METHOD = "get";

	@Inject
	private ExcelResultProvider processor;
	
	@GET
	@Path("{" + QUERY + "}.xlsx")
	@Produces(AdditionalMediaTypes.EXCEL)
	public Response get(
		@Auth Subject subject,
		@PathParam(DATASET) Dataset dataset,
		@PathParam(QUERY) ManagedExecution<?> execution,
		@HeaderParam("subject-agent") String userAgent,
		@QueryParam("charset") String queryCharset,
		@QueryParam("pretty") Optional<Boolean> pretty) {
		checkSingleTableResult(execution);
		log.info("Result for {} download on dataset {} by subject {} ({}).", execution.getId(), dataset, subject.getId(), subject.getName());
		return processor.createResult(subject, execution, dataset, pretty.orElse(true), determineCharset(userAgent, queryCharset), () -> {
		});
	}

	public static <E extends ManagedExecution<?> & SingleTableResult> URL getDownloadURL(UriBuilder uriBuilder, E exec) throws MalformedURLException {
		return uriBuilder
				.path(ResultExcelResource.class)
				.resolveTemplate(ResourceConstants.DATASET, exec.getDataset().getName())
				.path(ResultExcelResource.class, GET_RESULT_PATH_METHOD)
				.resolveTemplate(ResourceConstants.QUERY, exec.getId().toString())
				.build()
				.toURL();
	}
}
