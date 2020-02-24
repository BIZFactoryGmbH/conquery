package com.bakdata.conquery.models.jobs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.bakdata.conquery.io.xodus.WorkerStorage;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.concepts.Connector;
import com.bakdata.conquery.models.concepts.tree.ConceptTreeCache;
import com.bakdata.conquery.models.concepts.tree.ConceptTreeChild;
import com.bakdata.conquery.models.concepts.tree.ConceptTreeConnector;
import com.bakdata.conquery.models.concepts.tree.ConceptTreeNode;
import com.bakdata.conquery.models.concepts.tree.TreeChildPrefixIndex;
import com.bakdata.conquery.models.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.concepts.virtual.VirtualConceptConnector;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.BucketEntry;
import com.bakdata.conquery.models.events.BucketManager;
import com.bakdata.conquery.models.events.CBlock;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.models.identifiable.ids.specific.CBlockId;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.specific.AStringType;
import com.bakdata.conquery.util.CalculatedValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor @Slf4j
public class CalculateCBlocksJob extends Job {

	private final List<CalculationInformation> infos = new ArrayList<>();
	private final WorkerStorage storage;
	private final BucketManager bucketManager;
	private final Connector connector;
	private final Table table;

	@Override
	public String getLabel() {
		return "Calculate " + infos.size() + " CBlocks for " + connector.getId();
	}

	public void addCBlock(Import imp, Bucket bucket, CBlockId cBlockId) {
		infos.add(new CalculationInformation(table, imp, bucket, cBlockId));
	}

	@Override
	public void execute() throws Exception {
		boolean treeConcept = connector.getConcept() instanceof TreeConcept;
		this.progressReporter.setMax(infos.size());

		for (CalculationInformation info : infos) {
			try {
				if (bucketManager.hasCBlock(info.getCBlockId())) {
					continue;
				}

				CBlock cBlock = createCBlock(connector, info);
				cBlock.initIndizes(info.getBucket().getBucketSize());
				if (treeConcept) {
					calculateCBlock(cBlock, (ConceptTreeConnector) connector, info);
				}
				else {
					calculateCBlock(cBlock, (VirtualConceptConnector) connector, info);
				}
				setDateRangeIndex(cBlock, info);
				bucketManager.addCalculatedCBlock(cBlock);
				storage.addCBlock(cBlock);
			}
			catch (Exception e) {
				throw new Exception(
						String.format(
								"Exception in CalculateCBlocksJob (CBlock=%s, connector=%s, table=%s)",
								info.getCBlockId(),
								connector,
								table
						),
						e
				);
			}
			finally {
				this.progressReporter.report(1);
			}
		}
		progressReporter.done();
	}

	private void setDateRangeIndex(CBlock cBlock, CalculationInformation info) {
		Bucket bucket = info.getBucket();
		Table table = storage.getDataset().getTables().get(bucket.getImp().getTable());
		for (Column column : table.getColumns()) {
			if (column.getType().isDateCompatible()) {
				for (BucketEntry entry : bucket.entries()) {
					if (bucket.has(entry.getEvent(), column)) {
						CDateRange range = bucket.getAsDateRange(entry.getEvent(), column);
						cBlock.getMinDate()[entry.getLocalEntity()] = Math
							.min(cBlock.getMinDate()[entry.getLocalEntity()], range.getMinValue());
						cBlock.getMaxDate()[entry.getLocalEntity()] = Math
							.max(cBlock.getMaxDate()[entry.getLocalEntity()], range.getMaxValue());
					}
				}
			}
		}
	}

	private CBlock createCBlock(Connector connector, CalculationInformation info) {
		return new CBlock(info.getBucket().getId(), connector.getId());
	}

	private void calculateCBlock(CBlock cBlock, VirtualConceptConnector connector, CalculationInformation info) {}

	private void calculateCBlock(CBlock cBlock, ConceptTreeConnector connector, CalculationInformation info) {

		Bucket bucket = info.getBucket();

		CType<?, ?> cType = info.getImp().getColumns()[connector.getColumn().getPosition()].getType();

		if (!(cType instanceof AStringType)) {
			return;
		}
		AStringType<?> stringType = (AStringType<?>) cType;

		final TreeConcept treeConcept = connector.getConcept();

		final ImportId importId = info.getImp().getId();

		// Create index and insert into Tree.
		TreeChildPrefixIndex.putIndexInto(treeConcept);

		treeConcept.initializeIdCache(stringType, importId);

		cBlock.setMostSpecificChildren(new ArrayList<>(bucket.getNumberOfEvents()));


		final ConceptTreeCache cache = treeConcept.getCache(importId);

		for (BucketEntry entry : bucket.entries()) {
			try {
				final int event = entry.getEvent();

				// Lazy
				final CalculatedValue<Map<String, Object>> rowMap = new CalculatedValue<>(() -> bucket.calculateMap(event, info.getImp()));
				int valueIndex = bucket.getString(event, connector.getColumn());
				final String stringValue = stringType.getElement(valueIndex);

				// Events without values are skipped
				// Events can also be filtered, allowing a single table to be used by multiple connectors.
				if (!bucket.has(event, connector.getColumn())
					|| (connector.getCondition() != null && !connector.getCondition().matches(stringValue, rowMap))
				) {
					cBlock.getMostSpecificChildren().add(null);
					continue;
				}

				ConceptTreeChild child = cache.findMostSpecificChild(valueIndex, stringValue, rowMap);

				if (child != null) {
					cBlock.getMostSpecificChildren().add(child.getPrefix());
					ConceptTreeNode<?> it = child;
					while (it != null) {
						cBlock.getIncludedConcepts()[entry.getLocalEntity()] |= it.calculateBitMask();
						it = it.getParent();
					}
				}
				else {
					// see #174 improve handling by copying the relevant things from the old project
					cBlock.getMostSpecificChildren().add(null);
				}
			}
			catch (ConceptConfigurationException ex) {
				log.error("Failed to resolve event " + bucket + "-" + entry.getEvent() + " against concept " + connector, ex);
			}
		}

		// see #175 metrics candidate
		log
			.trace(
				"Hits: {}, Misses: {}, Hits/Misses: {}, %Hits: {} (Up to now)",
				cache.getHits(),
				cache.getMisses(),
				(double) cache.getHits() / cache.getMisses(),
				(double) cache.getHits() / (cache.getHits() + cache.getMisses()));
	}

	public boolean isEmpty() {
		return infos.isEmpty();
	}

	@RequiredArgsConstructor
	@Getter
	@Setter
	private static class CalculationInformation {

		private final Table table;
		private final Import imp;
		private final Bucket bucket;
		private final CBlockId cBlockId;
	}
}
