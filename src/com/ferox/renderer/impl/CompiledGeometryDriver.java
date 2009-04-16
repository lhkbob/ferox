package com.ferox.renderer.impl;

import java.util.List;

import com.ferox.renderer.RenderAtom;
import com.ferox.resource.Geometry;

/** Driver class that provides support for the compile() method
 * in the Renderer interface.
 * 
 * @author Michael Ludwig
 *
 */
public interface CompiledGeometryDriver {
	/** Perform the compile() functionality as described
	 * in Renderer.  It can be assumed that atoms is not null.
	 * The given ResourceData is the data that will be associated
	 * with the returned Geometry. 
	 * 
	 * This method should effectively perform an update() if it
	 * were a GeometryDriver, since update() for the GeometryDriver
	 * should do nothing for the compiled geometries.
	 * 
	 * It can be assumed that low-level operations can be performed
	 * (just like the methods in ResourceDriver). 
	 * 
	 * The low-level state will be having no active View (e.g.
	 * the modelview starts out as the identity matrix, instead of the
	 * view position), and the state record has been set to the
	 * defaults. */
	public Geometry compile(List<RenderAtom> atoms, ResourceData data);
	
	/** Return a GeometryDriver to use for the returned Geometries
	 * from compile(). */
	public GeometryDriver getDriver();
}
