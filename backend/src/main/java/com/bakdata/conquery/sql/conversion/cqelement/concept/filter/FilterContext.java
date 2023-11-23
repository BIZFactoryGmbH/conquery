package com.bakdata.conquery.sql.conversion.cqelement.concept.filter;

import com.bakdata.conquery.apiv1.query.concept.filter.FilterValue;
import com.bakdata.conquery.sql.conversion.Context;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.model.SqlTables;
import lombok.Value;

@Value
public class FilterContext<V> implements Context {
	/**
	 * A filter value ({@link FilterValue#getValue()})
	 */
	V value;
	ConversionContext parentContext;
	SqlTables<ConceptCteStep> conceptTables;
}
