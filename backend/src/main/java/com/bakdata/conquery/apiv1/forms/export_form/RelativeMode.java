package com.bakdata.conquery.apiv1.forms.export_form;

import java.util.function.Consumer;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.forms.IndexPlacement;
import com.bakdata.conquery.apiv1.query.ArrayConceptQuery;
import com.bakdata.conquery.apiv1.query.concept.specific.temporal.TemporalSamplerFactory;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.View;
import com.bakdata.conquery.models.forms.export.RelExportGenerator;
import com.bakdata.conquery.models.forms.managed.RelativeFormQuery;
import com.bakdata.conquery.models.forms.util.CalendarUnit;
import com.bakdata.conquery.models.query.DateAggregationMode;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.Visitable;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@CPSType(id = "RELATIVE", base = Mode.class)
@EqualsAndHashCode(callSuper = true)
public class RelativeMode extends Mode {
	@NotNull
	private CalendarUnit timeUnit;
	@Min(0)
	private int timeCountBefore;
	@Min(0)
	private int timeCountAfter;
	@NotNull
	private IndexPlacement indexPlacement;
	@NotNull
	private TemporalSamplerFactory indexSelector;


	@JsonView(View.InternalCommunication.class)
	@EqualsAndHashCode.Exclude
	private ArrayConceptQuery resolvedFeatures;

	@Override
	public void visit(Consumer<Visitable> visitor) {

	}

	@Override
	public RelativeFormQuery createSpecializedQuery() {
		return RelExportGenerator.generate(this);
	}

	@Override
	public void resolve(QueryResolveContext context) {
		resolvedFeatures = ArrayConceptQuery.createFromFeatures(getForm().getFeatures());

		// Resolve all
		resolvedFeatures.resolve(context.withDateAggregationMode(DateAggregationMode.NONE));
	}
}
