package com.bakdata.conquery.models.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import c10n.C10N;
import com.bakdata.conquery.apiv1.ExecutionStatus;
import com.bakdata.conquery.apiv1.FullExecutionStatus;
import com.bakdata.conquery.apiv1.query.Query;
import com.bakdata.conquery.apiv1.query.QueryDescription;
import com.bakdata.conquery.apiv1.query.SecondaryIdQuery;
import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.apiv1.query.concept.specific.CQReusedQuery;
import com.bakdata.conquery.apiv1.query.concept.specific.external.CQExternal;
import com.bakdata.conquery.internationalization.CQElementC10n;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.execution.InternalExecution;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.i18n.I18n;
import com.bakdata.conquery.models.identifiable.ids.specific.WorkerId;
import com.bakdata.conquery.models.messages.namespaces.WorkerMessage;
import com.bakdata.conquery.models.messages.namespaces.specific.ExecuteQuery;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.resultinfo.UniqueNamer;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.ShardResult;
import com.bakdata.conquery.models.worker.WorkerInformation;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@ToString(callSuper = true)
@Slf4j
@CPSType(base = ManagedExecution.class, id = "MANAGED_QUERY")
public class ManagedQuery extends ManagedExecution implements SingleTableResult, InternalExecution<ShardResult> {

	private static final int MAX_CONCEPT_LABEL_CONCAT_LENGTH = 70;
	// Needs to be resolved externally before being executed
	private Query query;
	/**
	 * The number of contained entities the last time this query was executed.
	 */
	private Long lastResultCount;

	//TODO this can actually be known ahead and reduced to speedup queries.
	@JsonIgnore
	private transient Set<WorkerId> involvedWorkers;
	@JsonIgnore
	private transient List<ColumnDescriptor> columnDescriptions;

	protected ManagedQuery(@JacksonInject(useInput = OptBoolean.FALSE) MetaStorage storage) {
		super(storage);
	}

	public ManagedQuery(Query query, User owner, Dataset submittedDataset, MetaStorage storage) {
		super(owner, submittedDataset, storage);
		this.query = query;
	}

	@Override
	protected void doInitExecutable() {

		query.resolve(new QueryResolveContext(getNamespace(), getConfig(), getStorage(), null));

	}

	@Override
	public void addResult(ShardResult result) {
		log.debug("Received Result[size={}] for Query[{}]", result.getResults().size(), result.getQueryId());

		log.trace("Received Result\n{}", result.getResults());

		if (result.getError().isPresent()) {
			fail(result.getError().get());
			return;
		}

		involvedWorkers.remove(result.getWorkerId());

		getNamespace().getExecutionManager().addQueryResult(this, result.getResults());

		if (involvedWorkers.isEmpty() && getState() == ExecutionState.RUNNING) {
			finish(ExecutionState.DONE);
		}
	}

	@Override
	protected void finish(ExecutionState executionState) {
		lastResultCount = query.countResults(streamResults());

		super.finish(executionState);
	}

	public Stream<EntityResult> streamResults() {
		return getNamespace().getExecutionManager().streamQueryResults(this);
	}

	@Override
	public long resultRowCount() {
		if (lastResultCount == null) {
			throw new IllegalStateException("Result row count is unknown, because the query has not yet finished.");
		}
		return lastResultCount;
	}

	@Override
	public void start() {
		super.start();
		involvedWorkers = Collections.synchronizedSet(getNamespace().getWorkers().stream()
																	.map(WorkerInformation::getId)
																	.collect(Collectors.toSet()));
	}

	@Override
	public void setStatusBase(@NonNull Subject subject, @NonNull ExecutionStatus status) {
		super.setStatusBase(subject, status);
		status.setNumberOfResults(lastResultCount);

		status.setQueryType(query.getClass().getAnnotation(CPSType.class).id());

		if (query instanceof SecondaryIdQuery) {
			status.setSecondaryId(((SecondaryIdQuery) query).getSecondaryId().getId());
		}
	}

	@Override
	protected void setAdditionalFieldsForStatusWithColumnDescription(Subject subject, FullExecutionStatus status) {
		super.setAdditionalFieldsForStatusWithColumnDescription(subject, status);
		if (columnDescriptions == null) {
			columnDescriptions = generateColumnDescriptions();
		}
		status.setColumnDescriptions(columnDescriptions);
	}

	/**
	 * Generates a description of each column that will appear in the resulting csv.
	 */
	public List<ColumnDescriptor> generateColumnDescriptions() {
		Preconditions.checkArgument(isInitialized(), "The execution must have been initialized first");
		List<ColumnDescriptor> columnDescriptions = new ArrayList<>();

		final Locale locale = I18n.LOCALE.get();

		PrintSettings settings = new PrintSettings(true, locale, getNamespace(), getConfig(), null);

		UniqueNamer uniqNamer = new UniqueNamer(settings);

		// First add the id columns to the descriptor list. The are the first columns
		for (ResultInfo header : getConfig().getIdColumns().getIdResultInfos()) {
			columnDescriptions.add(ColumnDescriptor.builder()
												   .label(uniqNamer.getUniqueName(header))
												   .type(header.getType().typeInfo())
												   .semantics(header.getSemantics())
												   .build());
		}

		final UniqueNamer collector = new UniqueNamer(settings);
		getResultInfos().forEach(info -> columnDescriptions.add(info.asColumnDescriptor(settings, collector)));
		return columnDescriptions;
	}

	@JsonIgnore
	public List<ResultInfo> getResultInfos() {
		return query.getResultInfos();
	}

	@Override
	@JsonIgnore
	public QueryDescription getSubmitted() {
		return query;
	}

	/**
	 * Creates a default label based on the submitted {@link QueryDescription}.
	 * The Label is customized by mentioning that a description contained a
	 * {@link CQExternal}, {@link CQReusedQuery} or {@link CQConcept}, in this order.
	 * In case of one ore more {@link CQConcept} the distinct labels of the concepts are chosen
	 * and concatinated until a length of {@value #MAX_CONCEPT_LABEL_CONCAT_LENGTH} is reached.
	 * All further labels are dropped.
	 */
	@Override
	protected String makeDefaultLabel(PrintSettings cfg) {
		final StringBuilder sb = new StringBuilder();

		final Map<Class<? extends Visitable>, List<Visitable>> sortedContents =
				Visitable.stream(query)
						 .collect(Collectors.groupingBy(Visitable::getClass));

		int sbStartSize = sb.length();

		// Check for CQExternal
		List<Visitable> externals = sortedContents.getOrDefault(CQExternal.class, Collections.emptyList());
		if (!externals.isEmpty()) {
			if (sb.length() > 0) {
				sb.append(" ");
			}
			sb.append(C10N.get(CQElementC10n.class, I18n.LOCALE.get()).external());
		}

		// Check for CQReused
		if (sortedContents.containsKey(CQReusedQuery.class)) {
			if (sb.length() > 0) {
				sb.append(" ");
			}
			sb.append(C10N.get(CQElementC10n.class, I18n.LOCALE.get()).reused());
		}


		// Check for CQConcept
		if (sortedContents.containsKey(CQConcept.class)) {
			if (sb.length() > 0) {
				sb.append(" ");
			}
			// Track length of text we are appending for concepts.
			final AtomicInteger length = new AtomicInteger();

			sortedContents.get(CQConcept.class)
						  .stream()
						  .map(CQConcept.class::cast)

						  .map(c -> makeLabelWithRootAndChild(c, cfg))
						  .filter(Predicate.not(Strings::isNullOrEmpty))
						  .distinct()

						  .takeWhile(elem -> length.addAndGet(elem.length()) < MAX_CONCEPT_LABEL_CONCAT_LENGTH)
						  .forEach(label -> sb.append(label).append(" "));

			// Last entry will output one Space that we don't want
			if (sb.length() > 0) {
				sb.deleteCharAt(sb.length() - 1);
			}

			// If not all Concept could be included in the name, point that out
			if (length.get() > MAX_CONCEPT_LABEL_CONCAT_LENGTH) {
				sb.append(" ").append(C10N.get(CQElementC10n.class, I18n.LOCALE.get()).furtherConcepts());
			}
		}


		// Fallback to id if nothing could be extracted from the query description
		if (sbStartSize == sb.length()) {
			sb.append(getId().getExecution());
		}

		return sb.toString();
	}

	private static String makeLabelWithRootAndChild(CQConcept cqConcept, PrintSettings cfg) {
		String label = cqConcept.getUserOrDefaultLabel(cfg.getLocale());
		if (label == null) {
			label = cqConcept.getConcept().getLabel();
		}

		// Concat everything with dashes
		return label.replace(" ", "-");
	}

	@Override
	public WorkerMessage createExecutionMessage() {
		return new ExecuteQuery(getId(), getQuery());
	}

	@Override
	public void visit(Consumer<Visitable> visitor) {
		visitor.accept(this);
		query.visit(visitor);
	}
}
