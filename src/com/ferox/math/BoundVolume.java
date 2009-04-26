package com.ferox.math;

import org.openmali.vecmath.Vector3f;

import com.ferox.renderer.View;
import com.ferox.renderer.View.FrustumIntersection;

/**
 * A BoundVolume represents a partitioning of space into what's outside and
 * what's inside the BoundVolume. Its main purpose is used for efficient view
 * culling and picking, as well as spatial queries.
 * 
 * @author Michael Ludwig
 * 
 */
public interface BoundVolume {
	/**
	 * Clone this BoundVolume into result. If result is of an unsupported type
	 * or null, create a new BoundVolume of this volume's type to store the
	 * clone.
	 * 
	 * @param result Storage for clone of this BoundVolume
	 * @return Clone stored in result, or new BoundVolume holding clone
	 */
	public BoundVolume clone(BoundVolume result);

	/**
	 * Grow this BoundVolume to completely enclose the given BoundVolume. Do
	 * nothing if toEnclose is null, preserving previous bounds.
	 * 
	 * @param toEnclose BoundVolume to merge into this
	 * @throws UnsupportedOperationException if toEnclose is an unsupported
	 *             BoundVolume implementation
	 */
	public void enclose(BoundVolume toEnclose)
					throws UnsupportedOperationException;

	/**
	 * Test for intersection between this and another BoundVolume. Return false
	 * if other is null.
	 * 
	 * @return True if other intersects this BoundVolume
	 * @throws UnsupportedOperationException if other is an unsupported
	 *             BoundVolume implementation
	 */
	public boolean intersects(BoundVolume other)
					throws UnsupportedOperationException;

	/**
	 * Apply the given transform to this BoundVolume. This effectively changes
	 * the coordinate space of the BoundVolume. Do nothing if trans is null.
	 * 
	 * @param trans Transform to apply to this BoundVolume
	 */
	public void applyTransform(Transform trans);

	/**
	 * Test this BoundVolume against the view frustum. view must have had
	 * updateView() called before this is invoked. Implementations can assume
	 * that the view's plane state is valid and can modify the plane state.
	 * 
	 * @param view View to check frustum intersection
	 * @return FrustumIntersection result
	 * 
	 * @throws NullPointerException if view is null
	 */
	public FrustumIntersection testFrustum(View view)
					throws NullPointerException;

	/**
	 * Compute the farthest extent of this volume along the given direction
	 * vector and store it in result. If reverse is true, instead find the
	 * extent in the opposite direction of dir.
	 * 
	 * @param dir Direction to compute the extent along
	 * @param reverse Whether or not dir should be effectively negated
	 * @param result Storage of the extent
	 * 
	 * @return Computed extent, either result or a new Vector3f if result is
	 *         null
	 * 
	 * @throws NullPointerException if dir is null
	 */
	public Vector3f getExtent(Vector3f dir, boolean reverse, Vector3f result)
					throws NullPointerException;

	/**
	 * As enclose(toEnclose) but this BoundVolume is unmodified and the output
	 * is stored in result (or a new instance).
	 * 
	 * @see clone()
	 * @see enclose()
	 * @param toEnclose BoundVolume to enclose with this BoundVolume
	 * @param result BoundVolume to hold computed enclosure of this and
	 *            toEnclose
	 */
	public BoundVolume enclose(BoundVolume toEnclose, BoundVolume result);

	/**
	 * As applyTransform(trans) but this BoundVolume is unmodified and the output
	 * is stored in result (or a new instance).
	 * 
	 * @see clone()
	 * @see applyTransform()
	 * @param trans Transform applied to this BoundVolume
	 * @param result BoundVolume to hold computed enclosure of this and
	 *            toEnclose
	 */
	public BoundVolume applyTransform(Transform trans, BoundVolume result);
}
