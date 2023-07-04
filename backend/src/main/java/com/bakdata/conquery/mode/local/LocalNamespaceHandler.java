package com.bakdata.conquery.mode.local;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.mode.InternalObjectMapperCreator;
import com.bakdata.conquery.mode.NamespaceSetupData;
import com.bakdata.conquery.mode.NamespaceHandler;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.query.ExecutionManager;
import com.bakdata.conquery.models.worker.LocalNamespace;
import com.bakdata.conquery.sql.conquery.SqlExecutionManager;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LocalNamespaceHandler implements NamespaceHandler<LocalNamespace> {

	private final ConqueryConfig config;
	private final InternalObjectMapperCreator mapperCreator;

	@Override
	public LocalNamespace createNamespace(NamespaceStorage namespaceStorage, MetaStorage metaStorage) {
		NamespaceSetupData namespaceData = NamespaceHandler.createNamespaceSetup(namespaceStorage, config, mapperCreator);
		ExecutionManager executionManager = new SqlExecutionManager();
		return new LocalNamespace(
				namespaceData.getPreprocessMapper(),
				namespaceData.getCommunicationMapper(),
				namespaceStorage,
				executionManager,
				namespaceData.getJobManager(),
				namespaceData.getFilterSearch(),
				namespaceData.getIndexService(),
				namespaceData.getInjectables()
		);
	}

	@Override
	public void removeNamespace(DatasetId id, LocalNamespace namespace) {
		// nothing to do
	}

}
