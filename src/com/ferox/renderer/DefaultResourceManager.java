package com.ferox.renderer;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

import com.ferox.resource.Resource;

/**
 * DefaultResourceManager provides an implementation of ResourceManager that
 * meets the requirements of the implicit resource manager that a Framework will
 * use with its requestUpdate() and requestCleanUp() methods.
 * 
 * @author Michael Ludwig
 */
public class DefaultResourceManager implements ResourceManager {
	private static abstract class PendingAction {
		Resource res;

		abstract void doAction(Renderer renderer);
	}

	private static class UpdateAction extends PendingAction {
		boolean forceFull;

		@Override
		public void doAction(Renderer renderer) {
			renderer.update(res, forceFull);
		}
	}

	private static class CleanupAction extends PendingAction {
		@Override
		public void doAction(Renderer renderer) {
			renderer.cleanUp(res);
		}
	}

	private final List<PendingAction> pendingActions;
	private final IdentityHashMap<Resource, Boolean> pendingResources;

	/** Construct a new DefaultResourceManager. */
	public DefaultResourceManager() {
		pendingActions = new ArrayList<PendingAction>();
		pendingResources = new IdentityHashMap<Resource, Boolean>();
	}

	@Override
	public void manage(Renderer renderer) {
		int numActions = pendingActions.size();
		if (numActions > 0) {
			for (int i = 0; i < numActions; i++)
				pendingActions.get(i).doAction(renderer);
			pendingActions.clear();
			pendingResources.clear();
		}
	}

	/**
	 * <p>
	 * Store the given request so that it manage() will call update() on the
	 * given resource. This will override any other request on the given
	 * resource that is pending the next call to manage().
	 * </p>
	 * <p>
	 * This should not be called from within the manage() method because the
	 * resource request will be ignored.
	 * </p>
	 * <p>
	 * Does nothing if resource is null.
	 * </p>
	 * 
	 * @param resource Resource to that has a requested update
	 * @param forceFullUpdate Whether or not the update should be full
	 */
	public void requestUpdate(Resource resource, boolean forceFullUpdate) {
		if (resource == null)
			return;

		UpdateAction a = new UpdateAction();
		a.res = resource;
		a.forceFull = forceFullUpdate;
		addAction(a);
	}

	/**
	 * <p>
	 * As requestUpdate() but for cleaning-up the given resource.
	 * </p>
	 * <p>
	 * It shouldn't be called within the manage() method because the resource
	 * request will be ignored.
	 * </p>
	 * <p>
	 * Does nothing if resource is null.
	 * </p>
	 * 
	 * @param resource The Resource that has a requested clean-up
	 */
	public void requestCleanup(Resource resource) {
		if (resource == null)
			return;

		CleanupAction a = new CleanupAction();
		a.res = resource;
		addAction(a);
	}

	// Override any old action, add a's resource to the pending
	// resource set and add the action to the list.
	private void addAction(PendingAction a) {
		if (!overrideResource(a.res))
			pendingResources.put(a.res, true);
		pendingActions.add(a);
	}

	// Clear out the last pending action that has res == resource.
	// Returns true if a resource was removed.
	private boolean overrideResource(Resource resource) {
		if (pendingResources.containsKey(resource)) {
			PendingAction a;
			for (int i = pendingActions.size(); i >= 0; i--) {
				a = pendingActions.get(i);
				if (a.res == resource) {
					pendingActions.remove(i);
					break;
				}
			}
			return true;
		}

		return false;
	}

}
