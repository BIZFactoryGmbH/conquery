package com.bakdata.conquery.models.query.queryplan.aggregators.specific;

import java.util.Set;

import javax.annotation.Nullable;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.aggregators.ColumnAggregator;
import com.bakdata.conquery.models.types.ResultType;
import lombok.ToString;

/**
 * Aggregator counting the number of present values in a column.
 */
@ToString(callSuper = true, onlyExplicitlyIncluded = true)
public class CountAggregator extends ColumnAggregator<Long> {

	@Nullable
	@ToString.Include
	private final Column column;
	private long count = 0;

	public CountAggregator() {
		column = null;
	}

	public CountAggregator(Column column) {
		this.column = column;
	}

	@Override
	public void init(Entity entity, QueryExecutionContext context) {
		count = 0;
	}

	@Override
	public void acceptEvent(Bucket bucket, int event) {
		if (column == null || bucket.has(event, column)) {
			count++;
		}
	}

	@Override
	public Long createAggregationResult() {
		return count > 0 ? count : null;
	}

	@Override
	public ResultType getResultType() {
		return ResultType.IntegerT.INSTANCE;
	}

	@Override
	public final Column[] getRequiredColumns() {
		if (column == null){
			return new Column[0];
		}

		return new Column[] { column };
	}

	@Override
	public final void collectRequiredTables(Set<Table> out) {
		if (column == null){
			return;
		}
		out.add(column.getTable());
	}

}
