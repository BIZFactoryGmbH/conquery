package com.bakdata.conquery.apiv1;

import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.execution.Labelable;
import com.bakdata.conquery.models.execution.Shareable;
import com.bakdata.conquery.models.execution.Shareable.ShareInformation;
import com.bakdata.conquery.models.execution.Taggable;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.models.identifiable.ids.specific.GroupId;
import com.bakdata.conquery.util.QueryUtils;
import lombok.Builder;
import lombok.Data;
import org.apache.shiro.authz.Permission;

@Data
@Builder
public class MetaDataPatch implements Taggable, Labelable, ShareInformation {

	private String[] tags;
	private String label;
	private Boolean shared;
	private List<GroupId> groups;
	


	/**
	 * Patches the given {@link Identifiable} by checking if the user holds the necessary Permission for that operation.
	 * Hence the patched instance must have a corresponding {@link Permission}-type.
	 * Tagging and Labeling only alters the state of the instance while sharing also alters the state of {@link Group}s.
	 * @param <INST>	Type of the instance that is patched
	 * @param storage	Storage that persists the instance and also auth information.
	 * @param user		The user on whose behalf the patch is executed
	 * @param instance	The instance to patch
	 * @param patch		The patch that is applied to the instance
	 * @param permissionCreator	A function that produces a {@link Permission} that targets the given instance (e.g QueryPermission, FormConfigPermission).
	 */
	public static <ID extends IId<?>, INST extends Taggable & Shareable & Labelable & Identifiable<? extends ID>> void patchIdentifialble(MasterMetaStorage storage, User user, INST instance, MetaDataPatch patch, PermissionCreator<ID> permissionCreator) {
		
		Consumer<MetaDataPatch> patchConsumerChain = QueryUtils.getNoOpEntryPoint();
		
		if(patch.getTags() != null && user.isPermitted(permissionCreator.apply(Ability.TAG.asSet(), instance.getId()))) {
			patchConsumerChain = patchConsumerChain.andThen(instance.tagger());
		}
		if(patch.getLabel() != null && user.isPermitted(permissionCreator.apply(Ability.LABEL.asSet(), instance.getId()))) {
			patchConsumerChain = patchConsumerChain.andThen(instance.labeler());
		}
		if(patch.getShared() != null && user.isPermitted(permissionCreator.apply(Ability.SHARE.asSet(), instance.getId()))) {
			patchConsumerChain = patchConsumerChain.andThen(instance.sharer(storage, user, permissionCreator));
		}
		patchConsumerChain.accept(patch);
	}

	
	@FunctionalInterface
	public interface PermissionCreator<ID extends IId<?>> extends BiFunction<Set<Ability>,ID, ConqueryPermission> {
		
	}
}
