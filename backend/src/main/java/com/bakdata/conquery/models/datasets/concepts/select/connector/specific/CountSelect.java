package com.bakdata.conquery.models.datasets.concepts.select.connector.specific;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

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
	@NsIdRefCollection
	private List<Column> distinctByColumn = Collections.emptyList();

	@Getter
	@Setter
	@NsIdRef
	@NotNull
	private Column column;

	@Override
	public Aggregator<?> createAggregator() {
		if (distinct || !distinctByColumn.isEmpty()) {
			return new DistinctValuesWrapperAggregator<>(new CountAggregator(getColumn()), getDistinctByColumn());
		}
		return new CountAggregator(getColumn());
	}

	@Nullable
	@Override
	public Column[] getRequiredColumns() {
		return Stream.concat(getDistinctByColumn().stream(), Stream.of(getColumn()))
					 .filter(Objects::nonNull)
					 .toArray(Column[]::new);
	}
}
