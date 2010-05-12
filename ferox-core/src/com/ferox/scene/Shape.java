package com.ferox.scene;

import com.ferox.resource.Geometry;
import com.ferox.entity.AbstractComponent;

/**
 * <p>
 * Shape is relatively simple Component that can associate a Geometry resource
 * to an Entity. By itself Shape holds little value but can be quite powerful
 * when combined with other Components. Some examples include adding additional
 * Components to describe the surface color, in which case this Shape can then
 * be rendered. It may be possible to describe the detailed region of fog, or
 * can be used to give a light a visible shape (such as with a light
 * bulb).
 * </p>
 * <p>
 * It is very likely that Controllers that use Shape will also require the
 * owning Entity to be a SceneElement. Generally Shape is intended for a
 * graphical description of geometry and is likely to have a too high resolution
 * for efficient use in a physics system. It is recommended that Shape's be
 * shared across Entities which require the same Geometry.
 * </p>
 * 
 * @author Michael Ludwig
 */
public final class Shape extends AbstractComponent<Shape> {
	private Geometry geometry;

	/**
	 * Create a new Shape that uses the given Geometry instance.
	 * 
	 * @param geom The Geometry that this Shape represents
	 * @throws NullPointerException if geom is null
	 */
	public Shape(Geometry geom) {
		super(Shape.class);
		setGeometry(geom);
	}

	/**
	 * Assign a new Geometry instance to this Shape.
	 * 
	 * @param geom The Geometry to use
	 * @throws NullPointerException if geom is null
	 */
	public void setGeometry(Geometry geom) {
		if (geom == null)
			throw new NullPointerException("Geometry cannot be null");
		geometry = geom;
	}
	
	/**
	 * @return The Geometry that this Shape embodies
	 */
	public Geometry getGeometry() {
		return geometry;
	}
}
