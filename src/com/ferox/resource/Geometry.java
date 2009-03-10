package com.ferox.resource;

import com.ferox.math.Boundable;

/** A Geometry represents an abstract representation of something
 * on the screen. A Geometry is the union of a boundable and
 * a resource.  By being a boundable, it allows bounding volumes to 
 * be created around it.  By extending a resource, creating and 
 * modifying the geometry must follow the same process as any other
 * resource, allowing optimizations to be performed.
 * 
 * Attempting to render a Geometry that has a status of ERROR
 * depends on the implementation of Renderer.  It should not cause
 * rendering to fail, and likely nothing will be rendered for the
 * erroneous geometry.
 * 
 * Geometry provides no additional methods to Boundable 
 * and Resource.
 * 
 * @author Michael Ludwig
 *
 */
public interface Geometry extends Boundable, Resource {
	
}
