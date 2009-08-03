package com.ferox.scene2;

import com.ferox.math.bounds.BoundSphere;
import com.ferox.resource.Geometry;

/**
 * <p>
 * A Shape represents the simplest type of Scene component, that of a Geometry
 * with an associated Appearance. It also has an auto-bound policy that will
 * calculate its local bounds from its assigned Geometry.
 * </p>
 * <p>
 * A Shape should be suitable for most simple renderings that don't require
 * animation, levels of detail or dynamic effects such as billboarding or
 * particles.
 * </p>
 * 
 * @author Michael Ludwig
 */
public class Shape extends AbstractSceneElement {
	private Appearance appearance;
	private Geometry geometry;
	private boolean autoBound;
	
	/**
	 * Create a new Shape that uses the given Geometry and Appearance. The
	 * constructed Shape will have its auto-bound policy set to true.
	 * 
	 * @param geometry The geometry to use
	 * @param appearance The Appearance used to render the associated Geometry
	 * @throws NullPointerException if geometry or appearance are null
	 */
	public Shape(Geometry geometry, Appearance appearance) {
		setGeometry(geometry);
		setAutoComputeBounds(true);
	}
	
	/**
	 * Return the current Appearance in use by this Shape. This will not be
	 * null.
	 * 
	 * @return The Shape's Appearance
	 */
	public Appearance getAppearance() {
		return appearance;
	}
	
	/**
	 * Assign a new Appearance to the Shape. Where possible, it is recommended
	 * to use the same Appearance instance to improve rendering speed. A Shape
	 * must have a non-null Appearance.
	 * 
	 * @param appearance The new Appearance to use
	 * @throws NullPointerException if appearance is null
	 */
	public void setAppearance(Appearance appearance) {
		if (appearance == null)
			throw new NullPointerException("Must have a non-null Appearance");
		this.appearance = appearance;
	}
	
	/**
	 * Return the Geometry that should be rendered for the given Shape when it's
	 * being processed.
	 * 
	 * @return This Shape's Geometry.
	 */
	public Geometry getGeometry() {
		return geometry;
	}
	
	/**
	 * Assign geom to be this Shape's new Geometry for use when rendering. Every
	 * Shape requires a non-null Geometry. If geom doesn't have a status of OK
	 * or DIRTY when the Shape will be rendered, the Shape will not appear.
	 * 
	 * @param geometry The new Geometry to use
	 * @throws NullPointerException if geom is null
	 */
	public void setGeometry(Geometry geometry) {
		if (geometry == null)
			throw new NullPointerException("Cannot have a null Geometry");
		this.geometry = geometry;
	}
	
	/**
	 * Set whether or not this Shape will auto-compute its local bounds from its
	 * assigned Geometry during each update. If this Shape has a null local
	 * bounds when an update occurs, and auto-bounding is true, then the Shape
	 * will use a BoundSphere. Otherwise, auto-bounding uses whatever instance
	 * was last assigned as its local bounds.
	 * 
	 * @param autoBound The new auto-bound policy for this Shape
	 */
	public void setAutoComputeBounds(boolean autoBound) {
		this.autoBound = autoBound;
	}
	
	/**
	 * Return true if this Shape will auto-compute its local bounds from the
	 * assigned Geometry during each update.
	 * @return The auto-bound policy for this Shape
	 */
	public boolean getAutoComputeBounds() {
		return autoBound;
	}
	
	@Override
	public boolean update(float timeDelta) {
		if (autoBound) {
			if (localBounds == null)
				localBounds = new BoundSphere();
			geometry.getBounds(localBounds);
		}
		return super.update(timeDelta);
	}
}
