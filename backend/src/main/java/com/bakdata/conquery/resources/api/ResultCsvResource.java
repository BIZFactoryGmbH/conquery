package com.bakdata.conquery.resources.api;

import static com.bakdata.conquery.io.result.ResultUtil.checkSingleTableResult;
import static com.bakdata.conquery.io.result.ResultUtil.determineCharset;
import static com.bakdata.conquery.resources.ResourceConstants.QUERY;

import java.net.MalformedURLException;
import java.net.URL;

import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import com.bakdata.conquery.apiv1.AdditionalMediaTypes;
import com.bakdata.conquery.io.result.csv.ResultCsvProcessor;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.query.SingleTableResult;
import com.bakdata.conquery.resources.ResourceConstants;
import io.dropwizard.auth.Auth;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Path("result/csv")
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ResultCsvResource {

	public static final String GET_RESULT_PATH_METHOD = "getAsCsv";

	private final ResultCsvProcessor processor;

	public static <E extends ManagedExecution & SingleTableResult> URL getDownloadURL(UriBuilder uriBuilder, E exec) throws MalformedURLException {
		return uriBuilder.path(ResultCsvResource.class)
						 .path(ResultCsvResource.class, GET_RESULT_PATH_METHOD)
						 .resolveTemplate(ResourceConstants.QUERY, exec.getId().toString())
						 .build()
						 .toURL();
	}

	@GET
	@Path("{" + QUERY + "}.csv")
	@Produces(AdditionalMediaTypes.CSV)
	public <E extends ManagedExecution & SingleTableResult> Response getAsCsv(
			@Auth Subject subject,
			@PathParam(QUERY) ManagedExecution execution,
			@HeaderParam(HttpHeaders.USER_AGENT) String userAgent,
			@QueryParam("charset") String queryCharset,
			@QueryParam("pretty") @DefaultValue("true") boolean pretty) {

		checkSingleTableResult(execution);
		log.info("Result for {} download on dataset {} by subject {} ({}).", execution, execution.getDataset().getId(), subject.getId(), subject.getName());

		return processor.createResult(subject, (E) execution, pretty, determineCharset(userAgent, queryCharset));
	}
}
