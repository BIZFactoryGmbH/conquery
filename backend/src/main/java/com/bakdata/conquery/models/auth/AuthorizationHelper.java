package com.bakdata.conquery.models.auth;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.PermissionOwner;
import com.bakdata.conquery.models.auth.entities.Role;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.auth.permissions.DatasetPermission;
import com.bakdata.conquery.models.auth.permissions.QueryPermission;
import com.bakdata.conquery.models.auth.permissions.WildcardPermission;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.query.ManagedQuery;
import org.apache.shiro.authz.Permission;

/**
 * Helper for easier and cleaner authorization.
 *
 */
public class AuthorizationHelper {
	
	// Dataset Instances
	/**
	 * Helper function for authorizing an ability on a dataset.
	 * @param user The subject that needs authorization.
	 * @param dataset The id of the object that needs to be checked.
	 * @param ability The kind of ability that is checked.
	 */
	public static void authorize(User user, DatasetId dataset, Ability ability) {
		authorize(user, dataset, EnumSet.of(ability));
	}
	
	/**
	 * Helper function for authorizing an ability on a dataset.
	 * @param user The subject that needs authorization.
	 * @param dataset The id of the object that needs to be checked.
	 * @param ability The kind of ability that is checked.
	 */
	public static void authorize(User user, DatasetId dataset, EnumSet<Ability> abilities) {
		user.checkPermission(DatasetPermission.onInstance(abilities, dataset));
	}
	
	// Query Instances
	/**
	 * Helper function for authorizing an ability on a query.
	 * @param user The subject that needs authorization.
	 * @param query The id of the object that needs to be checked.
	 * @param ability The kind of ability that is checked.
	 */
	public static void authorize(User user, ManagedExecutionId query, Ability ability) {
		authorize(user, query, EnumSet.of(ability));
	}
	
	/**
	 * Helper function for authorizing an ability on a query.
	 * @param user The subject that needs authorization.
	 * @param query The id of the object that needs to be checked.
	 * @param ability The kind of ability that is checked.
	 */
	public static void authorize(User user, ManagedExecutionId query, EnumSet<Ability> abilities) {
		user.checkPermission(QueryPermission.onInstance(abilities, query));
	}
	
	/**
	 * Helper function for authorizing an ability on a query.
	 * @param user The subject that needs authorization.
	 * @param query The object that needs to be checked.
	 * @param ability The kind of ability that is checked.
	 */
	public static void authorize(User user, ManagedQuery query, Ability ability) {
		authorize(user, query.getId(), EnumSet.of(ability));
	}
	
	/**
	 * Helper function for authorizing an ability on a query.
	 * @param user The subject that needs authorization.
	 * @param query The object that needs to be checked.
	 * @param ability The kind of ability that is checked.
	 */
	public static void authorize(User user, ManagedQuery query, EnumSet<Ability> abilities) {
		user.checkPermission(QueryPermission.onInstance(abilities, query.getId()));
	}
	
	/**
	 * Helper function for authorizing an ability on a query.
	 * @param user The subject that needs authorization.
	 * @param query The object that needs to be checked.
	 * @param ability The kind of ability that is checked.
	 */
	public static void authorize(User user, ConqueryPermission toBeChecked) {
		user.checkPermission(toBeChecked);
	}
	
	/**
	 * Utility function to add a permission to a subject (e.g {@link User}).
	 * @param owner The subject to own the new permission.
	 * @param permission The permission to add.
	 * @param storage A storage where the permission are added for persistence.
	 * @throws JSONException When the permission object could not be formed in to the appropriate JSON format.
	 */
	public static void addPermission(PermissionOwner<?> owner, ConqueryPermission permission, MasterMetaStorage storage) throws JSONException {
		Objects.requireNonNull(owner).addPermission(storage, permission);
	}
	
	/**
	 * Utility function to remove a permission from a subject (e.g {@link User}).
	 * @param owner The subject to own the new permission.
	 * @param permission The permission to remove.
	 * @param storage A storage where the permission is removed from.
	 * @throws JSONException When the permission object could not be formed in to the appropriate JSON format.
	 */
	public static void removePermission(PermissionOwner<?> owner, Permission permission, MasterMetaStorage storage) throws JSONException {
		Objects.requireNonNull(owner).removePermission(storage, permission);
	}
	
	public static List<Group> getGroupsOf(User user, MasterMetaStorage storage){
		List<Group> userGroups = new ArrayList<>();
		for (Group group : storage.getAllGroups()) {
			if(group.containsMember(user)) {
				userGroups.add(group);
			}
		}
		return userGroups;
	}
	

	
	/**
	 * Returns a list of the effective permissions. These are the permissions of the owner and
	 * the permission of the roles it inherits.
	 * @return Owned and inherited permissions.
	 */
	public static Set<ConqueryPermission> getEffectiveUserPermissions(UserId userId, MasterMetaStorage storage) {
		User user = Objects.requireNonNull(
			storage.getUser(userId),
			() -> String.format("User with id %s was not found", userId));
		Set<ConqueryPermission> permissions = new HashSet<>(user.getPermissions());
		for (Role role : user.getRoles()) {
			permissions.addAll(role.getPermissions());
		}
		
		for (Group group : storage.getAllGroups()) {
			if(group.containsMember(user)) {
				// Get Permissions of the group
				permissions.addAll(group.getPermissions());
				// And all of all roles a group holds
				group.getRoles().forEach(r -> permissions.addAll(r.getPermissions()));
			}
		}
		return permissions;
	}
	
	/**
	 * Returns a list of the effective permissions. These are the permissions of the owner and
	 * the permission of the roles it inherits. The query can be filtered by the Permission domain.
	 * @return Owned and inherited permissions.
	 */
	public static Set<WildcardPermission> getEffectiveUserPermissions(UserId userId, List<String> domainSpecifier, MasterMetaStorage storage) {
		Set<ConqueryPermission> permissions = getEffectiveUserPermissions(userId, storage);
		return permissions.stream()
			.filter(WildcardPermission.class::isInstance)
			.map(WildcardPermission.class::cast)
			.filter(p -> domainSpecifier.containsAll(p.getParts().get(0)))
			.collect(Collectors.toSet());
	}
}
