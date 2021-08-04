package com.bakdata.conquery.resources.admin.rest;

import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.AuthorizationHelper;
import com.bakdata.conquery.models.auth.entities.*;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.util.ConqueryEscape;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.collect.Multimap;
import com.univocity.parsers.csv.CsvWriter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.validation.Validator;
import java.io.StringWriter;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

/**
 * This class holds the logic for several admin http endpoints.
 */
// TODO clean up methods and refactor
@Getter
@Slf4j
@RequiredArgsConstructor
public class AdminProcessor {

	private final ConqueryConfig config;
	private final MetaStorage storage;
	private final DatasetRegistry datasetRegistry;
	private final JobManager jobManager;
	private final ScheduledExecutorService maintenanceService;
	private final Validator validator;
	private final ObjectWriter jsonWriter = Jackson.MAPPER.writer();



	public synchronized void addRole(Role role) throws JSONException {
		ValidatorHelper.failOnError(log, validator.validate(role));
		log.trace("New role:\tLabel: {}\tName: {}\tId: {} ", role.getLabel(), role.getName(), role.getId());
		storage.addRole(role);
	}

	public void addRoles(List<Role> roles) {

		for (Role role : roles) {
			try {
				addRole(role);
			}
			catch (Exception e) {
				log.error(String.format("Failed to add Role: %s", role), e);
			}
		}
	}

	/**
	 * Deletes the mandator, that is identified by the id. Its references are
	 * removed from the users, the groups, and from the storage.
	 *
	 * @param role the role to delete
	 */
	public void deleteRole(Role role) {
		AuthorizationHelper.deleteRole(storage, role);
	}

	public SortedSet<Role> getAllRoles() {
		return new TreeSet<>(storage.getAllRoles());
	}


	/**
	 * Handles creation of permissions.
	 *
	 * @param owner to which the permission is assigned
	 * @param permission The permission to create.
	 * @throws JSONException is thrown upon processing JSONs.
	 */
	public void createPermission(PermissionOwner<?> owner, ConqueryPermission permission) throws JSONException {
		AuthorizationHelper.addPermission(owner, permission, storage);
	}

	/**
	 * Handles deletion of permissions.
	 *
	 *
	 * @param owner the owner of the permission
	 * @param permission The permission to delete.
	 */
	public void deletePermission(PermissionOwner<?> owner, ConqueryPermission permission) {
		AuthorizationHelper.removePermission(owner, permission, storage);
	}


	public TreeSet<User> getAllUsers() {
		return new TreeSet<>(storage.getAllUsers());
	}

	public synchronized void deleteUser(User user) {
		for (Group group : storage.getAllGroups()) {
			group.removeMember(storage, user);
		}
		storage.removeUser(user.getId());
		log.trace("Removed user {} from the storage.", user.getId());
	}

	public void addUser(User user) throws JSONException {
		ValidatorHelper.failOnError(log, validator.validate(user));
		storage.addUser(user);
		log.trace("New user:\tLabel: {}\tName: {}\tId: {} ", user.getLabel(), user.getName(), user.getId());
	}

	public void addUsers(List<User> users) {

		for (User user : users) {
			try {
				addUser(user);
			}
			catch (Exception e) {
				log.error(String.format("Failed to add User: %s", user), e);
			}
		}
	}

	public TreeSet<Group> getAllGroups() {
		return new TreeSet<>(storage.getAllGroups());
	}

	public synchronized void addGroup(Group group) throws JSONException {
		ValidatorHelper.failOnError(log, validator.validate(group));
		storage.addGroup(group);
		log.trace("New group:\tLabel: {}\tName: {}\tId: {} ", group.getLabel(), group.getName(), group.getId());

	}

	public void addGroups(List<Group> groups) {

		for (Group group : groups) {
			try {
				addGroup(group);
			}
			catch (Exception e) {
				log.error(String.format("Failed to add Group: %s", group), e);
			}
		}
	}

	public void addUserToGroup(Group group, User user) {
		synchronized (storage) {
			group.addMember(storage, user);

			log.trace("Added user {} to group {}", user, group);
		}
	}

	public void deleteUserFromGroup(Group group, User user) {
		synchronized (storage) {
			group.removeMember(storage,user);
		}
		log.trace("Removed user {} from group {}", user, group);
	}

	public void deleteGroup(Group group) {
		storage.removeGroup(group.getId());
		log.trace("Removed group {}", group);
	}

	public void  deleteRoleFrom(RoleOwner owner, Role role) {
		synchronized (storage) {
			AuthorizationHelper.deleteRoleFrom(storage, owner, role);
		}

	}

	public  void addRoleTo(RoleOwner owner, Role role) {
		AuthorizationHelper.addRoleTo(getStorage(), role, owner);
	}

	/**
	 * Renders the permission overview for all users in form of a CSV.
	 */
	public String getPermissionOverviewAsCSV() {
		return getPermissionOverviewAsCSV(storage.getAllUsers());
	}


	/**
	 * Renders the permission overview for all users in a certain {@link Group} in form of a CSV.
	 */
	public String getPermissionOverviewAsCSV(Group group) {
		return getPermissionOverviewAsCSV(group.getMembers().stream().map(storage::getUser).collect(Collectors.toList()));
	}

	/**
	 * Renders the permission overview for certain {@link User} in form of a CSV.
	 */
	public String getPermissionOverviewAsCSV(Collection<User> users) {
		StringWriter sWriter = new StringWriter();
		CsvWriter writer = config.getCsv().createWriter(sWriter);
		List<String> scope = config
									 .getAuthorization()
									 .getOverviewScope();
		// Header
		writeAuthOverviewHeader(writer, scope);
		// Body
		for (User user : users) {
			writeAuthOverviewUser(writer, scope, user, storage, config);
		}
		return sWriter.toString();
	}

	/**
	 * Writes the header of the CSV auth overview to the specified writer.
	 */
	private static void writeAuthOverviewHeader(CsvWriter writer, List<String> scope) {
		List<String> headers = new ArrayList<>();
		headers.add("User");
		headers.addAll(scope);
		writer.writeHeaders(headers);
	}

	/**
	 * Writes the given {@link User}s (one per line) with their effective permission to the specified CSV writer.
	 */
	private static void writeAuthOverviewUser(CsvWriter writer, List<String> scope, User user, MetaStorage storage, ConqueryConfig config) {
		// Print the user in the first column
		writer.addValue(String.format("%s %s", user.getLabel(), ConqueryEscape.unescape(user.getName())));

		// Print the permission per domain in the remaining columns
		Multimap<String, ConqueryPermission> permissions = AuthorizationHelper.getEffectiveUserPermissions(user, scope, storage);
		for (String domain : scope) {
			writer.addValue(permissions.get(domain).stream()
									   .map(Object::toString)
									   .collect(Collectors.joining(config.getCsv().getLineSeparator())));
		}
		writer.writeValuesToRow();
	}


}
