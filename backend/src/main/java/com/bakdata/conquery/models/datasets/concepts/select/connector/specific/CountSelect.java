package com.bakdata.conquery.models.datasets.concepts.select.connector.specific;

import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.io.jackson.serializer.NsIdRefCollection;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.DistinctValuesWrapperAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.CountAggregator;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

@CPSType(id = "COUNT", base = Select.class)
@NoArgsConstructor
public class CountSelect extends Select {

	@Getter
	@Setter
	private boolean distinct = false;

	@Getter
	@Setter
	@NsIdRef
	@NotNull
	private Column column;


	@Getter
	@Setter
	@NsIdRefCollection
	private List<Column> distinctByColumn;


	@Override
	public Aggregator<?> createAggregator() {
		if (distinct) {
			return new DistinctValuesWrapperAggregator<>(new CountAggregator(getColumn()), DistinctValuesWrapperAggregator.fallback(getDistinctByColumn(), getColumn()));
		}
		return new CountAggregator(getColumn());
	}

	@Nullable
	@Override
	public Column[] getRequiredColumns() {
		List<Column> out = new ArrayList<>();

		out.add(getColumn());

		if (distinctByColumn != null){
			out.addAll(getDistinctByColumn());
		}

		return out.toArray(Column[]::new);
	}
}
