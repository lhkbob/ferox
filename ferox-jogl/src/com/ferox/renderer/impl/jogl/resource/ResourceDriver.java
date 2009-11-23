package com.ferox.renderer.impl.jogl.resource;

import com.ferox.renderer.impl.ResourceHandle;
import com.ferox.resource.Resource;
import com.ferox.resource.Resource.DirtyState;
import com.ferox.resource.Resource.Status;

public interface ResourceDriver {
	public ResourceHandle init(Resource res);
	
	public Status update(Resource res, ResourceHandle handle, DirtyState<?> dirtyState);
	
	public void dispose(ResourceHandle handle);
}
