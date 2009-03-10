package com.ferox.renderer.util;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

import com.ferox.renderer.Renderer;
import com.ferox.resource.Resource;
import com.ferox.resource.ResourceManager;

/** DefaultResourceManager provides an implementation of ResourceManager
 * that meets the requirements of the implicit resource manager that
 * a Renderer will use with its requestUpdate() and requestCleanUp() methods.
 * 
 * @author Michael Ludwig
 *
 */
public class DefaultResourceManager implements ResourceManager {
	private static abstract class PendingAction {
		Resource res;
		
		abstract void doAction(Renderer renderer);
	}
	
	private static class UpdateAction extends PendingAction {
		boolean forceFull;
		
		public void doAction(Renderer renderer) {
			renderer.update(this.res, this.forceFull);
		}
	}
	
	private static class CleanupAction extends PendingAction {
		public void doAction(Renderer renderer) {
			renderer.cleanUp(this.res);
		}
	}
	
	private List<PendingAction> pendingActions;
	private IdentityHashMap<Resource, Boolean> pendingResources;

	/** Construct a new DefaultResourceManager. */
	public DefaultResourceManager() {
		this.pendingActions = new ArrayList<PendingAction>();
		this.pendingResources = new IdentityHashMap<Resource, Boolean>();
	}
	
	@Override
	public void manage(Renderer renderer) {
		int numActions = this.pendingActions.size();
		if (numActions > 0) {
			for (int i = 0; i < numActions; i++) 
				this.pendingActions.get(i).doAction(renderer);
			this.pendingActions.clear();
			this.pendingResources.clear();
		}
	}
	
	/** Store the given request so that it manage() will
	 * call update() on the given resource.  This will override
	 * any other request on the given resource that is pending the
	 * next call to manage().
	 * 
	 * This should not be called from within the manage()
	 * method because the resource request will be ignored.
	 * 
	 * Does nothing if resource is null. */
	public void requestUpdate(Resource resource, boolean forceFullUpdate) {
		if (resource == null)
			return;
		
		UpdateAction a = new UpdateAction();
		a.res = resource;
		a.forceFull = forceFullUpdate;
		this.addAction(a);
	}
	
	/** Equivalent to requestUpdate() but for cleaning-up the
	 * given resource.
	 * 
	 * It shouldn't be called within the manage() 
	 * method because the resource request will be ignored.
	 * 
	 * Does nothing if resource is null. */
	public void requestCleanup(Resource resource) {
		if (resource == null)
			return;
		
		CleanupAction a = new CleanupAction();
		a.res = resource;
		this.addAction(a);
	}
	
	// Override any old action, add a's resource to the pending
	// resource set and add the action to the list.
	private void addAction(PendingAction a) {
		if (!this.overrideResource(a.res))
			this.pendingResources.put(a.res, true);
		this.pendingActions.add(a);
	}
	
	// Clear out the last pending action that has res == resource.
	// Returns true if a resource was removed.
	private boolean overrideResource(Resource resource) {
		if (this.pendingResources.containsKey(resource)) {
			PendingAction a;
			for (int i = this.pendingActions.size(); i >= 0; i--) {
				a = this.pendingActions.get(i);
				if (a.res == resource) {
					this.pendingActions.remove(i);
					break;
				}
			}
			return true;
		}
		
		return false;
	}

}
