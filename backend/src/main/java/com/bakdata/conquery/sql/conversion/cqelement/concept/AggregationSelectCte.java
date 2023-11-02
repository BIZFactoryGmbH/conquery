package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.List;

import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;

class AggregationSelectCte extends ConceptCte {

	@Override
	public QueryStep.QueryStepBuilder convertStep(ConceptCteContext conceptCteContext) {

		List<SqlSelect> requiredInAggregationFilterStep = conceptCteContext.allConceptSelects()
																		   .flatMap(sqlSelects -> sqlSelects.getForAggregationSelectStep().stream())
																		   .distinct()
																		   .toList();

		Selects aggregationSelectSelects = new Selects(conceptCteContext.getPrimaryColumn(), requiredInAggregationFilterStep);

		return QueryStep.builder()
						.selects(aggregationSelectSelects)
						.groupBy(List.of(conceptCteContext.getPrimaryColumn()));
	}

	@Override
	public ConceptCteStep cteStep() {
		return ConceptCteStep.AGGREGATION_SELECT;
	}

}
