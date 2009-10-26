package com.ferox.scene;

import com.ferox.resource.Geometry;
import com.ferox.scene.fx.Appearance;

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
	
	/**
	 * Create a new Shape that uses the given Geometry and Appearance. The
	 * constructed Shape will have no local bounds yet, so it must be set
	 * later.
	 * 
	 * @param geometry The geometry to use
	 * @param appearance The Appearance used to render the associated Geometry
	 * @throws NullPointerException if geometry or appearance are null
	 */
	public Shape(Geometry geometry, Appearance appearance) {
		setAppearance(appearance);
		setGeometry(geometry);
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
}
