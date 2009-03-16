package com.ferox.scene;

import com.ferox.math.AxisAlignedBox;
import com.ferox.math.BoundVolume;
import com.ferox.math.Transform;
import com.ferox.renderer.RenderQueue;
import com.ferox.renderer.RenderAtom;
import com.ferox.renderer.View;
import com.ferox.renderer.util.RenderQueueDataCache;
import com.ferox.resource.Geometry;
import com.ferox.state.Appearance;

/** A Shape represents the basic visual element of a scene, the union
 * of a Geometry and an Appearance (describing how the Geometry is colored).
 * 
 * Shape provides the functionality to auto-compute its local bounds based
 * off of its geometry.  If local bounds is never set, or set to null, and
 * its re-computing, then it will create an AxisAlignedBox for its use.
 * 
 * Appearances and geometry should be shared whenever possible to minimize state changes
 * and geometry updates.
 * 
 * @author Michael Ludwig
 *
 */
public class Shape extends Leaf implements RenderAtom {
	private boolean autoBound;
	private final RenderQueueDataCache cache;
	private Appearance appearance;
	private Geometry geom;
	
	/** Construct a shape with the given appearance and geometry, and set to
	 * automatically compute bounds. */
	public Shape(Geometry geom, Appearance app) {
		this.cache = new RenderQueueDataCache();
		
		this.setAppearance(app);
		this.setGeometry(geom);
		this.setAutoComputeBounds(true);
	}
	
	/** Set the appearance to use for this shape. If app is null, the default
	 * appearance is used when rendering. */
	public void setAppearance(Appearance app) {
		this.appearance = app;
	}
	
	/** Get the appearance used by this Shape (and what's used as a RenderAtom). */
	@Override
	public Appearance getAppearance() {
		return this.appearance;
	}
	
	/** Set whether or not to compute local bounds based on
	 * the Shape's appearance's renderable. */
	public void setAutoComputeBounds(boolean auto) {
		this.autoBound = auto;
	}
	
	/** Set the Geometry that is rendered as this Shape.  If null, the shape
	 * will not submit its render atom to the RenderQueue. */
	public void setGeometry(Geometry geom) {
		this.geom = geom;
	}
	
	/** Get the geometry used by this Shape. */
	@Override
	public Geometry getGeometry() {
		return this.geom;
	}
	
	/** Returns whether or not this Shape's local bounds are updated to
	 * enclose the Shape's geometry.  If false, the local bounds are kept at whatever they
	 * were last set to. */
	public boolean getAutoComputeBounds() {
		return this.autoBound;
	}
	
	/** Override visit to submit a render atom to the RenderQueue if necessary. */
	@Override
	public VisitResult visit(RenderQueue renderQueue, View view, VisitResult parentResult) {
		if (this.geom == null)
			return VisitResult.FAIL;
		
		VisitResult sp = super.visit(renderQueue, view, parentResult);
		if (sp != VisitResult.FAIL)
			renderQueue.add(this);

		return sp;
	}
	
	/** Override to store the geometry's bounds into local.  Only do it if flag is set 
	 * and has a non-null geometry present. If local is null and we're auto-updating, create a 
	 * new bound volume to store the bounds in. */
	@Override
	protected BoundVolume adjustLocalBounds(BoundVolume local) {
		if (this.autoBound && this.geom != null) {
			if (local == null)
				local = new AxisAlignedBox();
			this.geom.getBounds(local);
		}
		return local;
	}
	
	@Override
	public Object getRenderQueueData(RenderQueue pipe) {
		return this.cache.getRenderQueueData(pipe);
	}
	
	@Override
	public void setRenderQueueData(RenderQueue pipe, Object data) {
		this.cache.setRenderQueueData(pipe, data);
	}

	/** Return the world bounds of this Shape (used as RenderAtom). 
	 * Do not modify. */
	@Override
	public BoundVolume getBounds() {
		return this.worldBounds;
	}

	/** Return the world transform of this Shape (used as RenderAtom).
	 * Do not modify. */
	@Override
	public Transform getTransform() {
		return this.worldTransform;
	}
}
