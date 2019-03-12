package com.bakdata.conquery.models.query.filter;

import com.bakdata.conquery.models.common.IRange;
import com.bakdata.conquery.models.concepts.filters.Filter;
import com.bakdata.conquery.models.query.concept.filter.FilterValue;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;

import lombok.extern.slf4j.Slf4j;

/**
 * Includes entities when the specified column is one of many values.
 */
@Slf4j
public class RangeFilterNode<TYPE extends Comparable> extends AggregationResultFilterNode<Aggregator<TYPE>, FilterValue<IRange<TYPE, ?>>, Filter<FilterValue<IRange<TYPE, ?>>>> {

	public RangeFilterNode(Filter filter, FilterValue<IRange<TYPE, ?>> filterValue, Aggregator<TYPE> aggregator) {
		super(aggregator, filter, filterValue);
	}

	@Override
	public RangeFilterNode doClone(CloneContext ctx) {
		return new RangeFilterNode(filter, filterValue, getAggregator().clone(ctx));
	}

	@Override
	public boolean isContained() {
		return filterValue.getValue().contains(getAggregator().getAggregationResult());
	}
}
