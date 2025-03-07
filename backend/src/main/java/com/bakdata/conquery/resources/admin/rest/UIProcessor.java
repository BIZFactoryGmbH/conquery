package com.bakdata.conquery.resources.admin.rest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.bakdata.conquery.io.cps.CPSTypeIdResolver;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.auth.AuthorizationHelper;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.Role;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.auth.permissions.StringPermissionBuilder;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeNode;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.events.CBlock;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.index.IndexKey;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.resources.admin.ui.model.FrontendAuthOverview;
import com.bakdata.conquery.resources.admin.ui.model.FrontendGroupContent;
import com.bakdata.conquery.resources.admin.ui.model.FrontendPermission;
import com.bakdata.conquery.resources.admin.ui.model.FrontendRoleContent;
import com.bakdata.conquery.resources.admin.ui.model.FrontendUserContent;
import com.bakdata.conquery.resources.admin.ui.model.ImportStatistics;
import com.bakdata.conquery.resources.admin.ui.model.TableStatistics;
import com.bakdata.conquery.resources.admin.ui.model.UIContext;
import com.google.common.cache.CacheStats;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Wrapper processor that transforms internal representations of the {@link AdminProcessor} into
 * objects that are more convenient to handle with freemarker.
 */
@Slf4j
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class UIProcessor {

	@Getter
	private final AdminProcessor adminProcessor;

	public DatasetRegistry<? extends Namespace> getDatasetRegistry() {
		return adminProcessor.getDatasetRegistry();
	}

	public MetaStorage getStorage() {
		return adminProcessor.getStorage();
	}

	public UIContext getUIContext() {
		return new UIContext(adminProcessor.getNodeProvider());
	}

	public Set<IndexKey<?>> getLoadedIndexes() {
		return getAdminProcessor().getLoadedIndexes();
	}

	public CacheStats getIndexServiceStatistics() {
		return adminProcessor.getIndexServiceStatistics();
	}

	public FrontendAuthOverview getAuthOverview() {
		Collection<FrontendAuthOverview.OverviewRow> overview = new TreeSet<>();
		for (User user : getStorage().getAllUsers()) {
			Collection<Group> userGroups = AuthorizationHelper.getGroupsOf(user, getStorage());
			Set<Role> effectiveRoles = user.getRoles().stream()
										   .map(getStorage()::getRole)
										   // Filter role_ids that might not map TODO how do we handle those
										   .filter(Predicate.not(Objects::isNull))
										   .collect(Collectors.toCollection(HashSet::new));
			userGroups.forEach(g -> effectiveRoles.addAll(g.getRoles().stream()
														   .map(getStorage()::getRole)
														   // Filter role_ids that might not map TODO how do we handle those
														   .filter(Predicate.not(Objects::isNull))
														   .sorted().toList()));
			overview.add(FrontendAuthOverview.OverviewRow.builder().user(user).groups(userGroups).effectiveRoles(effectiveRoles).build());
		}
		return FrontendAuthOverview.builder().overview(overview).build();
	}


	public FrontendRoleContent getRoleContent(Role role) {
		return FrontendRoleContent.builder()
								  .permissions(wrapInFEPermission(role.getPermissions()))
								  .permissionTemplateMap(preparePermissionTemplate())
								  .users(getUsers(role))
								  .groups(getGroups(role))
								  .owner(role)
								  .build();
	}

	private Map<String, Pair<Set<Ability>, List<Object>>> preparePermissionTemplate() {
		Map<String, Pair<Set<Ability>, List<Object>>> permissionTemplateMap = new HashMap<>();

		// Grab all possible permission types for the "Create Permission" section
		Set<Class<? extends StringPermissionBuilder>> permissionTypes = CPSTypeIdResolver
				.listImplementations(StringPermissionBuilder.class);
		for (Class<? extends StringPermissionBuilder> permissionType : permissionTypes) {
			try {
				StringPermissionBuilder instance = (StringPermissionBuilder) permissionType.getField("INSTANCE").get(null);
				// Right argument is for possible targets of a specific permission type, but it
				// is left empty for now.
				permissionTemplateMap.put(instance.getDomain(), Pair.of(instance.getAllowedAbilities(), List.of()));
			}
			catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
				log.error("Could not access allowed abilities for permission type: {}", permissionType, e);
			}

		}
		return permissionTemplateMap;
	}

	public List<User> getUsers(Role role) {
		Collection<User> user = getStorage().getAllUsers();
		return user.stream().filter(u -> u.getRoles().contains(role.getId())).sorted().collect(Collectors.toList());
	}

	private List<Group> getGroups(Role role) {
		Collection<Group> groups = getStorage().getAllGroups();
		return groups.stream()
					 .filter(g -> g.getRoles().contains(role.getId()))
					 .sorted()
					 .collect(Collectors.toList());
	}

	private SortedSet<FrontendPermission> wrapInFEPermission(Collection<ConqueryPermission> permissions) {
		TreeSet<FrontendPermission> fePermissions = new TreeSet<>();

		for (ConqueryPermission permission : permissions) {
			fePermissions.add(FrontendPermission.from(permission));
		}
		return fePermissions;
	}

	public FrontendUserContent getUserContent(User user) {
		final Collection<Group> availableGroups = new ArrayList<>(getStorage().getAllGroups());
		availableGroups.removeIf(g -> g.containsMember(user));

		return FrontendUserContent
				.builder()
				.owner(user)
				.groups(AuthorizationHelper.getGroupsOf(user, getStorage()))
				.availableGroups(availableGroups)
				.roles(user.getRoles().stream().map(getStorage()::getRole).collect(Collectors.toList()))
				.availableRoles(getStorage().getAllRoles())
				.permissions(wrapInFEPermission(user.getPermissions()))
				.permissionTemplateMap(preparePermissionTemplate())
				.build();
	}


	public FrontendGroupContent getGroupContent(Group group) {

		Set<UserId> membersIds = group.getMembers();
		ArrayList<User> availableMembers = new ArrayList<>(getStorage().getAllUsers());
		availableMembers.removeIf(u -> membersIds.contains(u.getId()));
		return FrontendGroupContent
				.builder()
				.owner(group)
				.members(membersIds.stream().map(getStorage()::getUser).collect(Collectors.toList()))
				.availableMembers(availableMembers)
				.roles(group.getRoles().stream().map(getStorage()::getRole).collect(Collectors.toList()))
				.availableRoles(getStorage().getAllRoles())
				.permissions(wrapInFEPermission(group.getPermissions()))
				.permissionTemplateMap(preparePermissionTemplate())
				.build();
	}

	public TableStatistics getTableStatistics(Table table) {
		final NamespaceStorage storage = getDatasetRegistry().get(table.getDataset().getId()).getStorage();
		List<Import> imports = table.findImports(storage).collect(Collectors.toList());

		final long entries = imports.stream().mapToLong(Import::getNumberOfEntries).sum();

		return new TableStatistics(
				table,
				entries,
				//total size of dictionaries
				imports.stream()
					   .flatMap(imp -> imp.getDictionaries().stream())
					   .filter(Objects::nonNull)
					   .map(storage::getDictionary)
					   .mapToLong(Dictionary::estimateMemoryConsumption)
					   .sum(),
				//total size of entries
				imports.stream()
					   .mapToLong(Import::estimateMemoryConsumption)
					   .sum(),
				// Total size of CBlocks
				imports.stream()
					   .mapToLong(imp -> calculateCBlocksSizeBytes(imp, storage.getAllConcepts()))
					   .sum(),
				imports,
				storage.getAllConcepts().stream()
					   .map(Concept::getConnectors)
					   .flatMap(Collection::stream)
					   .filter(conn -> conn.getTable().equals(table))
					   .map(Connector::getConcept).collect(Collectors.toSet())

		);
	}

	public ImportStatistics getImportStatistics(Import imp) {
		final NamespaceStorage storage = getDatasetRegistry().get(imp.getDataset().getId()).getStorage();

		final long cBlockSize = calculateCBlocksSizeBytes(imp, storage.getAllConcepts());

		return new ImportStatistics(imp, cBlockSize);
	}

	public static long calculateCBlocksSizeBytes(Import imp, Collection<? extends Concept<?>> concepts) {

		// CBlocks are created per (per Bucket) Import per Connector targeting this table
		// Since the overhead of a single CBlock is minor, we gloss over the fact, that there are multiple and assume it is only a single very large one.
		return concepts.stream()
					   .filter(TreeConcept.class::isInstance)
					   .flatMap(concept -> ((TreeConcept) concept).getConnectors().stream())
					   .filter(con -> con.getTable().equals(imp.getTable()))
					   .mapToLong(con -> {
						   // Per event an int array is stored marking the path to the concept child.
						   final double avgDepth = con.getConcept()
													  .getAllChildren()
													  .mapToInt(ConceptTreeNode::getDepth)
													  .average()
													  .orElse(1d);

						   return CBlock.estimateMemoryBytes(imp.getNumberOfEntities(), imp.getNumberOfEntries(), avgDepth);
					   })
					   .sum();
	}
}
