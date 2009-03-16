package com.ferox.renderer.impl;

import com.ferox.renderer.RenderSurface;

/** A ContextRecordSurface is a RenderSurface that is meant to
 * be based on a common properties of an OpenGL-like graphics
 * context.  There are two important concepts for this:
 *  - The context that holds all of the resources and graphics
 *    data can be shared by all created surfaces.
 *  - Each surface has a state record that affects all rendered
 *    primitives.  This can also be shared (e.g. if
 *    the hardware supports frame buffer objects).
 *    
 * Before a ContextRecordSurface can be used to render or
 * access its resources, it must be made current.
 * 
 * @author Michael Ludwig
 *
 */
public interface ContextRecordSurface extends RenderSurface {
	/** Return an arbitrary id for this surface that is unique
	 * among the non-destroyed surfaces from the same renderer.
	 * It must be an integer >= 0 since AbstractRenderer will use it
	 * to access arrays.
	 * 
	 * The value returned by destroyed surfaces is meaningless. */
	public int getSurfaceId();
	
	/** Get the implementation specific object that represents
	 * the collection of all possible state attributes for a surface.
	 * Two surfaces should only return the same instance if a low-level
	 * state modification on one surface affects the low-level state 
	 * on the other (i.e. modifying state for a frame buffer object and
	 * the state of a normal surface which it is attached to). 
	 * 
	 * This object is passed to all drivers when performing low-level
	 * operations on a surface, so implementations must document which
	 * object type is returned so drivers may access it properly. 
	 * 
	 * Must not return null. */
	public Object getStateRecord();
	
	/** This method assigns this surface a Runnable to be 
	 * executed each time the surface is rendered.  This method
	 * will properly handle flushing each of the surface's
	 * render passes.
	 * 
	 * It is the surface's responsibility to properly clear its
	 * buffers before executing this, and to perform any actions
	 * afterwards necessary to display the results correctly.
	 * 
	 * This method will be called once right after the surface
	 * is created. */
	public void setRenderAction(Runnable action);
}
