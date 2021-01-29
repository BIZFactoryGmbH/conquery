package com.bakdata.conquery.models.config;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.commands.ShardNode;
import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.io.xodus.MetaStorage;
import com.bakdata.conquery.io.xodus.NamespaceStorage;
import com.bakdata.conquery.io.xodus.WorkerStorage;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import javax.validation.Validator;
import java.util.List;

@CPSBase
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
public interface StorageFactory {

	MetaStorage createMetaStorage(Validator validator, List<String> pathName, DatasetRegistry datasets);

	NamespaceStorage createNamespaceStorage(Validator validator, List<String> pathName, boolean returnNullOnExisting);

	WorkerStorage createWorkerStorage(Validator validator, List<String> pathName, boolean returnNullOnExisting);

	void loadNamespaceStorages(ManagerNode managerNode);

	void loadWorkerStorages(ShardNode shardNode);
}
