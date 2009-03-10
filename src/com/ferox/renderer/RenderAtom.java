package com.ferox.renderer;

import com.ferox.math.BoundVolume;
import com.ferox.math.Transform;
import com.ferox.resource.Geometry;
import com.ferox.state.Appearance;

/** A RenderAtom represents a visual object on the screen.  Generally
 * it is a condensed representation of some scene element.
 * 
 * @author Michael Ludwig
 *
 */
public interface RenderAtom {
	/** Get the bound volume (world space) for this atom.  It is allowed to return
	 * null, although it may make some influence atoms behave poorly because they
	 * may rely on bounds for accurate influence calculations.
	 * 
	 * It is an influence atom's responsibility to compute influence as best
	 * as possible if null is returned. */
	public BoundVolume getBounds();
	
	/** Get the transform (world space) for this atom.  This represents the
	 * 3D position of the atom when rendered.  
	 *
	 * Must not return null. */
	public Transform getTransform();
	
	/** Get the appearance of the render atom.  Essentially an atom is a vehicle
	 * for delivering the appearance to the renderer, along with a transform for
	 * positioning the appearance on a render surface.  
	 * 
	 * If this method returns null, the atom will be rendered with the default appearance. */
	public Appearance getAppearance();
	
	/** Get the Geometry that is rendered when the atom is submitted to the renderer.
	 * 
	 * Must not return null - if there is no geometry, then the atom arbiter must not
	 * submit the geometry to the RenderQueues. */
	public Geometry getGeometry();
	
	/** Get the RenderQueue specific data for this RenderAtom.
	 * Returns null if the RenderQueue never set any data, or if it set null.
	 * 
	 * Returns null if the RenderQueue is null. */
	public Object getRenderQueueData(RenderQueue pipe);
	
	/** Set the RenderQueue specific data for this RenderAtom, overwriting
	 * any previous value.
	 * 
	 * Does nothing if the RenderQueue is null. */
	public void setRenderQueueData(RenderQueue pipe, Object data);
}
