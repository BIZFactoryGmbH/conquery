package com.bakdata.conquery.models.datasets.concepts.filters.specific;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.frontend.FrontendFilterConfiguration;
import com.bakdata.conquery.apiv1.frontend.FrontendFilterType;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.io.jackson.serializer.NsIdRefCollection;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.models.query.filter.RangeFilterNode;
import com.bakdata.conquery.models.query.queryplan.aggregators.DistinctValuesWrapperAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.CountAggregator;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConnectorCteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.FilterContext;
import com.bakdata.conquery.sql.conversion.model.filter.SqlFilters;
import com.bakdata.conquery.sql.conversion.model.select.CountSqlAggregator;
import lombok.Data;
import lombok.NoArgsConstructor;

@CPSType(id = "COUNT", base = Filter.class)
@NoArgsConstructor
@Data
public class CountFilter extends Filter<Range.LongRange> {

	@NsIdRef
	private Column column;

	@NsIdRefCollection
	@NotNull
	private List<Column> distinctByColumn = Collections.emptyList();

	private boolean distinct;

	@Override
	public void configureFrontend(FrontendFilterConfiguration.Top f, ConqueryConfig conqueryConfig) {
		f.setType(FrontendFilterType.Fields.INTEGER_RANGE);
		f.setMin(1);
	}


	@SuppressWarnings({"unchecked", "rawtypes"})
	@Override
	public FilterNode createFilterNode(Range.LongRange value) {
		if (!isDistinct()) {
			return new RangeFilterNode(value, new CountAggregator(getColumn()));
		}

		if (distinctByColumn != null && !getDistinctByColumn().isEmpty()) {
			return new RangeFilterNode(value, new DistinctValuesWrapperAggregator(new CountAggregator(getColumn()), getDistinctByColumn()));
		}

		return new RangeFilterNode(value, new DistinctValuesWrapperAggregator(new CountAggregator(), List.of(getColumn())));

	}

	@Override
	public List<Column> getRequiredColumns() {
		final List<Column> out = new ArrayList<>();
		out.add(getColumn());
		if (distinctByColumn != null) {
			out.addAll(getDistinctByColumn());
		}

		return out;
	}

	@Override
	public SqlFilters convertToSqlFilter(FilterContext<Range.LongRange> filterContext) {
		return CountSqlAggregator.create(this, filterContext).getSqlFilters();
	}

	@Override
	public Set<ConnectorCteStep> getRequiredSqlSteps() {
		return ConnectorCteStep.withOptionalSteps(ConnectorCteStep.AGGREGATION_FILTER);
	}

}
