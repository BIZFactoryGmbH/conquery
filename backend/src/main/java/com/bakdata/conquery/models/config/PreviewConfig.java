package com.bakdata.conquery.models.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.error.ConqueryError;
import com.bakdata.conquery.models.identifiable.ids.specific.SelectId;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
@AllArgsConstructor
@NoArgsConstructor
public class PreviewConfig {
	private List<String> infoCardSelects = List.of();

	public List<Select> resolveInfoCardSelects(Dataset dataset, DatasetRegistry registry) {
		final List<Select> infoCardSelects = new ArrayList<>();

		for (String name : getInfoCardSelects()) {
			SelectId selectId = SelectId.Parser.INSTANCE.parsePrefixed(dataset.getName(), name);
			Select resolved = registry.resolve(selectId);

			infoCardSelects.add(resolved);
		}

		final Set<Select> nonConnectorSelects = infoCardSelects.stream()
															   .filter(select -> !(select.getHolder() instanceof Connector))
															   .collect(Collectors.toSet());

		if (!nonConnectorSelects.isEmpty()) {
			log.error("The selects {} are not connector-Selects", nonConnectorSelects);
			throw new ConqueryError.ExecutionCreationErrorUnspecified();
		}

		return infoCardSelects;

	}
}
