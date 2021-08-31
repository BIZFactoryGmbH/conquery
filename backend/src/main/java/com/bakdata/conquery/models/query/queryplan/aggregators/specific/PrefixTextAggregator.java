package com.bakdata.conquery.models.query.queryplan.aggregators.specific;

import java.util.HashSet;
import java.util.Set;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.aggregators.SingleColumnAggregator;
import com.google.common.collect.ImmutableSet;

/**
 * Aggregator, returning all values of a column, beginning with a specified value.
 */
public class PrefixTextAggregator extends SingleColumnAggregator<Set<String>> {

	private final Set<String> entries = new HashSet<>();
	private final String prefix;

	public PrefixTextAggregator(Column column, String prefix) {
		super(column);
		this.prefix = prefix;
	}

	@Override
	public void init(Entity entity, QueryExecutionContext context) {
		entries.clear();
	}

	@Override
	public void acceptEvent(Bucket bucket, int event) {
		if (!bucket.has(event, getColumn())) {
			return;
		}

		String value = (String) bucket.createScriptValue(event, getColumn());

		// if performance is a problem we could find the prefix once in the dictionary
		// and then only check the values
		if (value.startsWith(prefix)) {
			entries.add(value);
		}

	}

	@Override
	public Set<String> getAggregationResult() {
		return entries.isEmpty() ? null : ImmutableSet.copyOf(entries);
	}

	@Override
	public ResultType getResultType() {
		return ResultType.StringT.INSTANCE;
	}
}
