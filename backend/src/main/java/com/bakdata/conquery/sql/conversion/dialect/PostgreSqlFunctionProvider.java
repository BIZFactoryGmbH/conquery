package com.bakdata.conquery.sql.conversion.dialect;

import java.sql.Date;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.ValidityDate;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import org.jooq.ArrayAggOrderByStep;
import org.jooq.Condition;
import org.jooq.DataType;
import org.jooq.DatePart;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.impl.DSL;

/**
 * Provider of SQL functions for PostgresSQL.
 *
 * @see <a href="https://www.postgresql.org/docs/15/functions.html">PostgreSQL Documentation</a>
 */
class PostgreSqlFunctionProvider implements SqlFunctionProvider {

	private static final String INFINITY_DATE_VALUE = "infinity";
	private static final String MINUS_INFINITY_DATE_VALUE = "-infinity";
	private static final String ANY_CHAR_REGEX = "%";

	@Override
	public String getMaxDateExpression() {
		return INFINITY_DATE_VALUE;
	}

	@Override
	public <T> Field<T> cast(Field<?> field, DataType<T> type) {
		return DSL.cast(field, type);
	}

	@Override
	public String getAnyCharRegex() {
		return ANY_CHAR_REGEX;
	}

	@Override
	public String getMinDateExpression() {
		return MINUS_INFINITY_DATE_VALUE;
	}

	@Override
	public Condition dateRestriction(ColumnDateRange dateRestriction, ColumnDateRange validityDate) {
		if (!validityDate.isSingleColumnRange()) {
			throw new UnsupportedOperationException("The validity date range has to be converted to a daterange field in the preprocessing step.");
		}
		else {
			// the && operator checks if two ranges overlap (see https://www.postgresql.org/docs/15/functions-range.html)
			return DSL.condition(
					"{0} && {1}",
					dateRestriction.getRange(),
					validityDate.getRange()
			);
		}
	}

	@Override
	public ColumnDateRange daterange(CDateRange dateRestriction) {

		String startDateExpression = MINUS_INFINITY_DATE_VALUE;
		String endDateExpression = INFINITY_DATE_VALUE;

		if (dateRestriction.hasLowerBound()) {
			startDateExpression = dateRestriction.getMin().toString();
		}
		if (dateRestriction.hasUpperBound()) {
			endDateExpression = dateRestriction.getMax().toString();
		}

		Field<Object> dateRestrictionRange = DSL.function(
				"daterange",
				Object.class,
				toDateField(startDateExpression),
				toDateField(endDateExpression),
				DSL.val("[]")
		);

		return ColumnDateRange.of(dateRestrictionRange)
							  .asDateRestrictionRange();
	}

	@Override
	public ColumnDateRange daterange(ValidityDate validityDate, String qualifier, String label) {

		Field<?> dateRange;

		if (validityDate.getEndColumn() != null) {

			Field<?> startColumn = DSL.coalesce(
					DSL.field(DSL.name(qualifier, validityDate.getStartColumn().getName())),
					toDateField(MINUS_INFINITY_DATE_VALUE)
			);
			Field<?> endColumn = DSL.coalesce(
					DSL.field(DSL.name(qualifier, validityDate.getEndColumn().getName())),
					toDateField(INFINITY_DATE_VALUE)
			);

			dateRange = daterange(startColumn, endColumn, "[]");
		}
		else {
			Column validityDateColumn = validityDate.getColumn();
			dateRange = switch (validityDateColumn.getType()) {
				// if validityDateColumn is a DATE_RANGE we can make use of Postgres' integrated daterange type.
				case DATE_RANGE -> DSL.field(validityDateColumn.getName());
				// if the validity date column is not of daterange type, we construct it manually
				case DATE -> {
					Field<Date> column = DSL.field(DSL.name(qualifier, validityDate.getColumn().getName()), Date.class);
					Field<Date> startColumn = DSL.coalesce(column, toDateField(MINUS_INFINITY_DATE_VALUE));
					Field<Date> endColumn = DSL.coalesce(column, toDateField(INFINITY_DATE_VALUE));
					yield daterange(startColumn, endColumn, "[]");
				}
				default -> throw new IllegalArgumentException(
						"Given column type '%s' can't be converted to a proper date restriction.".formatted(validityDateColumn.getType())
				);
			};
		}

		return ColumnDateRange.of(dateRange)
							  .asValidityDateRange(label);
	}

	@Override
	public ColumnDateRange aggregated(ColumnDateRange columnDateRange) {
		return ColumnDateRange.of(DSL.function("range_agg", Object.class, columnDateRange.getRange()));
	}

	@Override
	public Field<String> validityDateStringAggregation(ColumnDateRange columnDateRange) {
		if (!columnDateRange.isSingleColumnRange()) {
			throw new UnsupportedOperationException("All column date ranges should have been converted to single column ranges.");
		}
		Field<String> aggregatedValidityDate = DSL.field("{0}::{1}", String.class, columnDateRange.getRange(), DSL.keyword("varchar"));
		return replace(aggregatedValidityDate, INFINITY_DATE_VALUE, INFINITY_SIGN);
	}

	@Override
	public Field<Integer> dateDistance(ChronoUnit timeUnit, Name startDateColumnName, Date endDateExpression) {

		Field<Date> startDate = DSL.field(startDateColumnName, Date.class);
		Field<Date> endDate = toDateField(endDateExpression.toString());

		if (timeUnit == ChronoUnit.DAYS) {
			return endDate.minus(startDate).coerce(Integer.class);
		}

		Field<Integer> age = DSL.function("AGE", Integer.class, endDate, startDate);

		return switch (timeUnit) {
			case MONTHS -> extract(DatePart.YEAR, age).multiply(12)
													  .plus(extract(DatePart.MONTH, age));
			case YEARS -> extract(DatePart.YEAR, age);
			case DECADES -> extract(DatePart.DECADE, age);
			case CENTURIES -> extract(DatePart.CENTURY, age);
			default -> throw new UnsupportedOperationException("Given ChronoUnit %s is not supported.");
		};
	}

	@Override
	public Field<Date> addDays(Field<Date> dateColumn, int amountOfDays) {
		return dateColumn.plus(amountOfDays);
	}

	@Override
	public <T> Field<T> first(Field<T> column, List<Field<?>> orderByColumn) {
		return DSL.field("({0})[1]", column.getType(), DSL.arrayAgg(column));
	}

	@Override
	public <T> Field<T> last(Field<T> column, List<Field<?>> orderByColumns) {
		ArrayAggOrderByStep<Object[]> arrayAgg = DSL.arrayAgg(DSL.field(
																	  "{0} {1} {2} {3}",
																	  column,
																	  DSL.keyword("ORDER BY"),
																	  DSL.sql(orderByColumns.stream().map(Field::toString).collect(Collectors.joining(","))),
																	  DSL.keyword("DESC")
															  )
		);
		return DSL.field("({0})[1]", column.getType(), arrayAgg);
	}

	@Override
	public <T> Field<T> random(Field<T> column) {
		ArrayAggOrderByStep<Object[]> arrayAgg = DSL.arrayAgg(DSL.field(
				"{0} {1} {2}",
				column,
				DSL.keyword("ORDER BY"),
				DSL.function("random", Object.class)
		));
		return DSL.field("({0})[1]", column.getType(), arrayAgg);
	}

	@Override
	public Condition likeRegex(Field<String> field, String pattern) {
		return field.similarTo(pattern);
	}

	@Override
	public Field<String> yearQuarter(Field<Date> dateField) {
		return DSL.field(
				"{0}::varchar || '-Q' || {1}::varchar",
				String.class,
				DSL.extract(dateField, DatePart.YEAR),
				DSL.extract(dateField, DatePart.QUARTER)
		);
	}

	@Override
	public Field<Object[]> asArray(List<Field<?>> fields) {
		String arrayExpression = fields.stream()
									   .map(Field::toString)
									   .collect(Collectors.joining(", ", "array[", "]"));
		return DSL.field(arrayExpression, Object[].class);
	}

	private Field<?> daterange(Field<?> startColumn, Field<?> endColumn, String bounds) {
		return DSL.function(
				"daterange",
				Object.class,
				startColumn,
				endColumn,
				DSL.val(bounds)
		);
	}

	private Field<Integer> extract(DatePart datePart, Field<Integer> timeInterval) {
		return DSL.field(
				"{0}({1} {2} {3})",
				Integer.class,
				DSL.keyword("EXTRACT"),
				DSL.keyword(datePart.toSQL()),
				DSL.keyword("FROM"),
				timeInterval
		);
	}

	@Override
	public Field<Date> toDateField(String dateValue) {
		return DSL.field("{0}::{1}", Date.class, DSL.val(dateValue), DSL.keyword("date"));
	}

}
