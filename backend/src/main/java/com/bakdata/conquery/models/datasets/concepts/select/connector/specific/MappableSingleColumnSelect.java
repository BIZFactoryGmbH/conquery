package com.bakdata.conquery.models.datasets.concepts.select.connector.specific;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiFunction;

import javax.annotation.Nullable;
import javax.validation.Valid;

import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.io.jackson.View;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.select.connector.SingleColumnSelect;
import com.bakdata.conquery.models.index.InternToExternMapper;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.SelectResultInfo;
import com.bakdata.conquery.models.types.ResultType;
import com.bakdata.conquery.models.types.SemanticType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;

public abstract class MappableSingleColumnSelect extends SingleColumnSelect {

	/**
	 * If a mapping was provided the mapping changes the aggregator result before it is processed by a {@link com.bakdata.conquery.io.result.ResultRender.ResultRendererProvider}.
	 */
	@Getter
	@Valid
	@Nullable
	@View.ApiManagerPersistence
	@NsIdRef
	private final InternToExternMapper mapping;

	@JsonIgnore
	protected final BiFunction<Object, PrintSettings, String> mapper;

	public MappableSingleColumnSelect(Column column,
									  @Nullable InternToExternMapper mapping) {
		super(column);
		this.mapping = mapping;

		if (mapping != null) {
			mapper = (value, cfg) -> applyMapping(value);
		}
		else {
			mapper = null;
		}
	}

	@Override
	public SelectResultInfo getResultInfo(CQConcept cqConcept) {

		final Set<SemanticType> semantics = new HashSet<>();

		if (isCategorical()) {
			semantics.add(new SemanticType.CategoricalT());
		}

		return new SelectResultInfo(this, cqConcept, semantics);
	}

	@Override
	public ResultType getResultType() {
		if (mapping == null) {
			return super.getResultType();
		}
		return new ResultType.StringT(mapper);

	}

	public void loadMapping() {
		if (mapping != null) {
			mapping.init();
		}
	}

	private String applyMapping(Object intern) {
		return intern == null || getMapping() == null ? "" : getMapping().external(intern.toString());
	}
}
