package com.ferox.math.bounds;

import com.ferox.math.Frustum;
import com.ferox.math.Transform;
import com.ferox.math.Vector3f;
import com.ferox.math.Frustum.FrustumIntersection;

/**
 * A BoundVolume represents a partitioning of space into what's outside and
 * what's inside the BoundVolume. Its main purpose is used for efficient view
 * culling and picking, as well as spatial queries.
 * 
 * @author Michael Ludwig
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
	public void enclose(BoundVolume toEnclose);

	/**
	 * Test for intersection between this and another BoundVolume. Return false
	 * if other is null.
	 * 
	 * @return True if other intersects this BoundVolume
	 * @throws UnsupportedOperationException if other is an unsupported
	 *             BoundVolume implementation
	 */
	public boolean intersects(BoundVolume other);

	/**
	 * Apply the given transform to this BoundVolume. This effectively changes
	 * the coordinate space of the BoundVolume. Do nothing if trans is null.
	 * 
	 * @param trans Transform to apply to this BoundVolume
	 */
	public void transform(Transform trans);

	/**
	 *<p>
	 * Test this BoundVolume against frustum. frustum must have had
	 * updateFrustumPlanes() called before this is invoked.
	 * </p>
	 * <p>
	 * Implementations can assume that the given PlaneState is valid. If a
	 * programmer does not wish to use a PlaneState, then null should be
	 * specified. A null PlaneState should be treated just like a PlaneState
	 * requiring all planes to be tested.
	 * </p>
	 * <p>
	 * If planeState is non-null, implementations should update the PlaneState
	 * based on the intermediate results of the plane tests.
	 * </p>
	 * 
	 * @param frustum Frustum to test intersection with
	 * @param planeState The PlaneState to use for efficient testing, may be
	 *            null
	 * @return FrustumIntersection result
	 * @throws NullPointerException if view is null
	 */
	public FrustumIntersection testFrustum(Frustum frustum, PlaneState planeState);

	/**
	 * Compute the farthest extent of this volume along the given direction
	 * vector and store it in result. If reverse is true, instead find the
	 * extent in the opposite direction of dir.
	 * 
	 * @param dir Direction to compute the extent along
	 * @param reverse Whether or not dir should be effectively negated
	 * @param result Storage of the extent
	 * @return Computed extent, either result or a new Vector3f if result is
	 *         null
	 * @throws NullPointerException if dir is null
	 */
	public Vector3f getExtent(Vector3f dir, boolean reverse, Vector3f result);

	/**
	 * As enclose(toEnclose) but this BoundVolume is unmodified and the output
	 * is stored in result (or a new instance).
	 * 
	 * @see #clone(BoundVolume)
	 * @see #enclose(BoundVolume)
	 * @param toEnclose BoundVolume to enclose with this BoundVolume
	 * @param result BoundVolume to hold computed enclosure of this and
	 *            toEnclose
	 */
	public BoundVolume enclose(BoundVolume toEnclose, BoundVolume result);

	/**
	 * As applyTransform(trans) but this BoundVolume is unmodified and the
	 * output is stored in result (or a new instance).
	 * 
	 * @see #clone(BoundVolume)
	 * @see #transform(Transform)
	 * @param trans Transform applied to this BoundVolume
	 * @param result BoundVolume to hold computed enclosure of this and
	 *            toEnclose
	 */
	public BoundVolume transform(Transform trans, BoundVolume result);
}
