package com.bakdata.conquery.models.concepts.filters;

import org.apache.commons.lang3.ArrayUtils;

import com.bakdata.conquery.apiv1.FilterTemplate;
import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.models.api.description.FEFilter;
import com.bakdata.conquery.models.concepts.Connector;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.models.identifiable.Labeled;
import com.bakdata.conquery.models.identifiable.ids.specific.FilterId;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * This class is the abstract superclass for all filters.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
@CPSBase
public abstract class Filter<FE_TYPE> extends Labeled<FilterId> {

	private String unit;
	private String description;
	@JsonBackReference
	private Connector connector;
	private FilterTemplate template;
	private String pattern;
	private Boolean allowDropFile;

	public abstract void configureFrontend(FEFilter f) throws ConceptConfigurationException;

	@JsonIgnore
	public abstract Column[] getRequiredColumns();

	public final boolean requiresColumn(Column c) {
		return ArrayUtils.contains(getRequiredColumns(), c);
	}

	public abstract FilterNode createAggregator(FE_TYPE filterValue);

	@Override
	public FilterId createId() {
		return new FilterId(connector.getId(), getName());
	}

	/**
	 * This method is called once at startup or if the dataset changes for each new import that
	 * concerns this filter. Use this to collect metadata from the import. It is not guaranteed that
	 * any blocks or cBlocks exist at this time. Any data created by this method should be volatile
	 * and @JsonIgnore.
	 * @param imp the import added
	 */
	public void addImport(Import imp) {}

	/*
	public Condition createSimpleCondition(FE_TYPE qf) {return null;}
	public Condition createGroupCondition(FE_TYPE qf) {return null;}
	public Select<Record> generateComplexFilter(ComplexFilterExecutor exec, QueryContext context, FE_TYPE qf, Table<?> baseTable) {return null;}

	public abstract Field<Object> select();
	public abstract Field<Object> aggregate(QueryContext context);
	public Field<Object> combine(QueryContext context) {
		return JooqHelper.aggregateSameValueOrNull(DSL.field(DSL.name(DBNames.QUERY_RESULTS.FEATURE)));
	}
	public Field<Object> map(Field<Object> f) {
		return f;
	}*/
}
