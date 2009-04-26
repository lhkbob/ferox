package com.ferox.math;

/**
 * BoundsCache is a convenience class that implements bound volume caching for
 * spheres and axis-aligned boxes to make it easier for Boundable
 * implementations to implement their getBounds() method.
 * 
 * @author Michael Ludwig
 */
public class BoundsCache {
	private final Boundable boundable;

	private final AxisAlignedBox axisCache;
	private final BoundSphere sphereCache;

	private boolean axisDirty;
	private boolean sphereDirty;

	/**
	 * Construct a bounds cache for the given Boundable instance.
	 * 
	 * @param boundable The Boundable that is used by this cache
	 * @throws NullPointerException if boundable is null
	 */
	public BoundsCache(Boundable boundable) throws NullPointerException {
		if (boundable == null)
			throw new NullPointerException("geom can't be null");

		this.boundable = boundable;

		axisCache = new AxisAlignedBox();
		sphereCache = new BoundSphere();

		axisDirty = true;
		sphereDirty = true;
	}

	/**
	 * <p>
	 * Store the bounds of this cache's geometry into result. Does nothing if
	 * result is null or not a BoundSphere or AxisAlignedBox.
	 * </p>
	 * <p>
	 * If the cache has been marked dirty since the last getBounds() call, the
	 * axis or sphere cache will first be updated by calling enclose() on this
	 * cache's geometry.
	 * </p>
	 * 
	 * @param result The result that stores the bounds of this cache's Boundable
	 */
	public void getBounds(BoundVolume result) {
		if (result != null)
			if (result instanceof AxisAlignedBox) {
				if (axisDirty) {
					AabbBoundableUtil.getBounds(boundable, axisCache);
					axisDirty = false;
				}
				axisCache.clone(result);
			} else if (result instanceof BoundSphere) {
				if (sphereDirty) {
					SphereBoundableUtil.getBounds(boundable, sphereCache);
					sphereDirty = false;
				}
				sphereCache.clone(result);
			}
	}

	/**
	 * Mark the cache as dirty, so that the next call to getBounds() will update
	 * the cache to reflect changes to the geometry.
	 */
	public void setCacheDirty() {
		axisDirty = true;
		sphereDirty = true;
	}
}
