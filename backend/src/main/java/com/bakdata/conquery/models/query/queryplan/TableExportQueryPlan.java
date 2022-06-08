package com.bakdata.conquery.models.query.queryplan;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.specific.Leaf;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.MultilineEntityResult;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * The QueryPlan creates a full dump of the given table within a certain
 * date range.
 */
@RequiredArgsConstructor
public class TableExportQueryPlan implements QueryPlan<MultilineEntityResult> {

	private final QueryPlan<? extends EntityResult> subPlan;
	private final CDateRange dateRange;
	private final List<TableExportDescription> tables;
	private final Map<Column, Integer> positions;

	@Override
	public boolean isOfInterest(Entity entity) {
		return subPlan.isOfInterest(entity);
	}

	@Override
	public Optional<Aggregator<CDateSet>> getValidityDateAggregator() {
		// TODO create a fake aggregator and feed it inside the loop, return it here.
		return Optional.empty();
	}

	@Override
	public void init(QueryExecutionContext ctxt, Entity entity) {
		subPlan.init(ctxt, entity);
	}

	@Override
	public Optional<MultilineEntityResult> execute(QueryExecutionContext ctx, Entity entity) {
		Optional<? extends EntityResult> result = subPlan.execute(ctx, entity);

		final QPNode query = new Leaf(); //TODO pull from actual queries instead.

		if (result.isEmpty() || tables.isEmpty()) {
			return Optional.empty();
		}

		final List<Object[]> results = new ArrayList<>();

		final int totalColumns = positions.values().stream().mapToInt(i -> i).max().getAsInt() + 1;

		for (TableExportDescription exportDescription : tables) {

			for (Bucket bucket : ctx.getEntityBucketsForTable(entity, exportDescription.getTable())) {

				int entityId = entity.getId();

				if (!bucket.containsEntity(entityId)) {
					continue;
				}

				final int start = bucket.getEntityStart(entityId);
				final int end = bucket.getEntityEnd(entityId);

				for (int event = start; event < end; event++) {

					// Export Full-table if it has no validity date.
					if (exportDescription.getValidityDateColumn() != null
						&& !bucket.eventIsContainedIn(event, exportDescription.getValidityDateColumn(), CDateSet.create(dateRange))) {
						continue;
					}

					if (filterRow(ctx, entity, query, bucket, event)) {
						continue;
					}

					final Object[] entry = collectRow(totalColumns, exportDescription, bucket, event);

					results.add(entry);
				}
			}
		}

		return Optional.of(new MultilineEntityResult(entity.getId(), results));
	}

	private boolean filterRow(QueryExecutionContext ctx, Entity entity, QPNode query, Bucket bucket, int event) {
		query.init(entity, ctx);

		if (!query.isOfInterest(entity)) {
			return true;
		}

		query.nextTable(ctx, bucket.getTable());
		query.nextBlock(bucket);

		if (!query.isOfInterest(bucket)) {
			return true;
		}

		query.acceptEvent(bucket, event);

		return !query.isContained();
	}

	private Object[] collectRow(int totalColumns, TableExportDescription exportDescription, Bucket bucket, int event) {
		final Object[] entry = new Object[totalColumns];
		entry[1] = exportDescription.getTable().getName(); // TODO Or Id or Label?

		for (Column column : exportDescription.getTable().getColumns()) {

			if (!bucket.has(event, column)) {
				continue;
			}

			if (column.equals(exportDescription.getValidityDateColumn())) {
				entry[0] = List.of(bucket.getAsDateRange(event, column));
			}
			else {
				entry[positions.get(column)] = bucket.createScriptValue(event, column);
			}
		}
		return entry;
	}

	@RequiredArgsConstructor
	@Getter
	public static class TableExportDescription {
		private final Table table;
		@Nullable
		private final Column validityDateColumn;
	}
}
