package com.bakdata.conquery.resources.api;

import static com.bakdata.conquery.resources.ResourceConstants.*;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.AbstractSelectFilter;
import com.bakdata.conquery.resources.api.ConceptsProcessor.ResolvedConceptsResult;
import com.bakdata.conquery.resources.hierarchies.HFilters;
import io.dropwizard.logback.shaded.checkerframework.checker.nullness.qual.Nullable;
import lombok.Data;
import lombok.Setter;

@Setter
@Produces({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})
@Consumes({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})
@Path("datasets/{" + DATASET + "}/concepts/{" + CONCEPT + "}/tables/{" + TABLE + "}/filters/{" + FILTER + "}")
public class FilterResource extends HFilters {

	@Inject
	protected ConceptsProcessor processor;

	@POST
	@Path("resolve")
	public ResolvedConceptsResult resolveFilterValues(FilterValues filterValues) {
		return processor.resolveFilterValues((AbstractSelectFilter<?>) filter, filterValues.getValues());
	}

	@POST
	@Path("autocomplete")
	public ConceptsProcessor.AutoCompleteResult autocompleteTextFilter(@Valid FilterResource.AutocompleteRequest request) {

		if (!(filter instanceof AbstractSelectFilter)) {
			throw new WebApplicationException(filter.getId() + " is not a SELECT filter, but " + filter.getClass().getSimpleName() + ".", Status.BAD_REQUEST);
		}


		try {
			return processor.autocompleteTextFilter((AbstractSelectFilter<?>) filter, request.getText(), request.getPage(), request.getPageSize());
		}catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		}
	}

	@Data
	public static class FilterValues {
		private List<String> values;
	}

	@Data
	public static class AutocompleteRequest {
		@Nullable
		private Optional<String> text;
		private OptionalInt page;
		private OptionalInt pageSize;
	}
}
