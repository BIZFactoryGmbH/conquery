package com.bakdata.conquery.models.identifiable.ids;

import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.IdentifiableImpl;

public interface NamespacedIdentifiable<ID extends IId<? extends IdentifiableImpl<? extends ID>> & NamespacedId> extends Identifiable<ID> {
	Dataset getDataset();
}
