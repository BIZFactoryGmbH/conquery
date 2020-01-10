package com.bakdata.conquery.models.worker;

import com.bakdata.conquery.io.mina.MessageSender;
import com.bakdata.conquery.io.mina.NetworkSession;
import com.bakdata.conquery.io.xodus.WorkerStorage;
import com.bakdata.conquery.models.events.BucketManager;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.models.messages.namespaces.NamespaceMessage;
import com.bakdata.conquery.models.messages.network.MasterMessage;
import com.bakdata.conquery.models.messages.network.NetworkMessage;
import com.bakdata.conquery.models.messages.network.specific.ForwardToNamespace;
import com.bakdata.conquery.models.query.QueryExecutor;
import lombok.Getter;
import lombok.Setter;

import java.io.Closeable;
import java.io.IOException;

public class Worker implements MessageSender.Transforming<NamespaceMessage, NetworkMessage<?>>, Closeable {
	@Getter
	private final JobManager jobManager;
	@Getter
	private final WorkerStorage storage;
	@Getter
	private final QueryExecutor queryExecutor;
	@Getter
	private final WorkerInformation info;
	@Setter
	private NetworkSession session;
	
	public Worker(WorkerInformation info, JobManager jobManager, WorkerStorage storage, QueryExecutor queryExecutor) {
		this.info = info;
		this.jobManager = jobManager;
		this.storage = storage;
		BucketManager bucketManager = new BucketManager(jobManager, storage, this);
		storage.setBucketManager(bucketManager);
		this.queryExecutor = queryExecutor;
	}

	@Override
	public NetworkSession getMessageParent() {
		return session;
	}

	@Override
	public MasterMessage transform(NamespaceMessage message) {
		return new ForwardToNamespace(info.getDataset(), message);
	}
	
	@Override
	public void close() throws IOException {
		queryExecutor.close();
		storage.close();
	}
	
	@Override
	public String toString() {
		return "Worker[" + info.getId() + ", " + session.getLocalAddress() + "]";
	}
}