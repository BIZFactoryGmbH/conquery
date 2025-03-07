package com.bakdata.conquery.sql.conversion.model.aggregator;

import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.DateDistanceFilter;
import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.DateDistanceSelect;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConnectorCteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.FilterContext;
import com.bakdata.conquery.sql.conversion.model.select.SelectContext;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.SqlTables;
import com.bakdata.conquery.sql.conversion.model.filter.DateDistanceCondition;
import com.bakdata.conquery.sql.conversion.model.filter.WhereClauses;
import com.bakdata.conquery.sql.conversion.model.filter.WhereCondition;
import com.bakdata.conquery.sql.conversion.model.select.ExtractingSqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.FieldWrapper;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelects;
import com.bakdata.conquery.sql.conversion.supplier.DateNowSupplier;
import lombok.Value;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.impl.DSL;

@Value
public class DateDistanceSqlAggregator implements SqlAggregator {

	SqlSelects sqlSelects;
	WhereClauses whereClauses;

	private DateDistanceSqlAggregator(
			Column column,
			String alias,
			CDateRange dateRestriction,
			ChronoUnit timeUnit,
			SqlTables<ConnectorCteStep> connectorTables,
			DateNowSupplier dateNowSupplier,
			Range.LongRange filterValue,
			SqlFunctionProvider functionProvider
	) {
		Date endDate = getEndDate(dateRestriction, dateNowSupplier);
		if (column.getType() != MajorTypeId.DATE) {
			throw new UnsupportedOperationException("Can't calculate date distance to column of type " + column.getType());
		}
		Name dateColumnName = DSL.name(connectorTables.getRootTable(), column.getName());
		FieldWrapper<Integer> dateDistanceSelect = new FieldWrapper<>(functionProvider.dateDistance(timeUnit, dateColumnName, endDate).as(alias));

		SqlSelects.SqlSelectsBuilder builder = SqlSelects.builder().preprocessingSelect(dateDistanceSelect);

		if (filterValue == null) {

			Field<Integer> qualifiedDateDistance = dateDistanceSelect.qualify(connectorTables.getPredecessor(ConnectorCteStep.AGGREGATION_SELECT))
																	 .select();
			FieldWrapper<Integer> minDateDistance = new FieldWrapper<>(DSL.min(qualifiedDateDistance).as(alias));

			String finalPredecessor = connectorTables.getPredecessor(ConnectorCteStep.AGGREGATION_FILTER);
			ExtractingSqlSelect<Integer> finalSelect = minDateDistance.qualify(finalPredecessor);

			this.sqlSelects = builder.aggregationSelect(minDateDistance)
									 .finalSelect(finalSelect)
									 .build();
			this.whereClauses = WhereClauses.empty();
		}
		else {
			this.sqlSelects = builder.build();
			String predecessorCte = connectorTables.getPredecessor(ConnectorCteStep.EVENT_FILTER);
			Field<Integer> qualifiedDateDistanceSelect = dateDistanceSelect.qualify(predecessorCte).select();
			WhereCondition dateDistanceCondition = new DateDistanceCondition(qualifiedDateDistanceSelect, filterValue);
			this.whereClauses = WhereClauses.builder()
											.eventFilter(dateDistanceCondition)
											.build();
		}
	}

	public static DateDistanceSqlAggregator create(
			DateDistanceSelect dateDistanceSelect,
			SelectContext selectContext
	) {
		return new DateDistanceSqlAggregator(
				dateDistanceSelect.getColumn(),
				selectContext.getNameGenerator().selectName(dateDistanceSelect),
				selectContext.getParentContext().getDateRestrictionRange(),
				dateDistanceSelect.getTimeUnit(),
				selectContext.getConnectorTables(),
				selectContext.getParentContext().getSqlDialect().getDateNowSupplier(),
				null,
				selectContext.getParentContext().getSqlDialect().getFunctionProvider()
		);
	}

	public static DateDistanceSqlAggregator create(
			DateDistanceFilter dateDistanceFilter,
			FilterContext<Range.LongRange> filterContext
	) {
		return new DateDistanceSqlAggregator(
				dateDistanceFilter.getColumn(),
				filterContext.getNameGenerator().selectName(dateDistanceFilter),
				filterContext.getParentContext().getDateRestrictionRange(),
				dateDistanceFilter.getTimeUnit(),
				filterContext.getConnectorTables(),
				filterContext.getParentContext().getSqlDialect().getDateNowSupplier(),
				filterContext.getValue(),
				filterContext.getParentContext().getSqlDialect().getFunctionProvider()
		);
	}

	private Date getEndDate(CDateRange dateRange, DateNowSupplier dateNowSupplier) {
		LocalDate endDate;
		// if a date restriction is set, the max of the date restriction equals the end date of the date distance
		// but there is also the possibility that the user set's an empty daterange which will be non-null but with null values
		if (Objects.nonNull(dateRange) && dateRange.getMax() != null) {
			endDate = dateRange.getMax();
		}
		else {
			// otherwise the current date is the upper bound
			endDate = dateNowSupplier.getLocalDateNow();
		}
		return Date.valueOf(endDate);
	}


}
