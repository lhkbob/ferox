package com.ferox.resource;

import com.ferox.math.AxisAlignedBox;
import com.ferox.math.BoundSphere;
import com.ferox.math.BoundVolume;

/** GeometryBoundsCache is a convenience class that implements 
 * bound volume caching for spheres and axis-aligned boxes to
 * make it easier for Geometry implementations to implement
 * their getBounds() method.
 * 
 * @author Michael Ludwig
 *
 */
public class GeometryBoundsCache {
	private final Geometry geometry;
	
	private final AxisAlignedBox axisCache;
	private final BoundSphere sphereCache;
	
	private boolean axisDirty;
	private boolean sphereDirty;
	
	/** Construct a bounds cache for the given geometry instance.
	 * If it's null, an exception is thrown. */
	public GeometryBoundsCache(Geometry geom) throws NullPointerException {
		if (geom == null)
			throw new NullPointerException("geom can't be null");
		
		this.geometry = geom;
		this.axisCache = new AxisAlignedBox();
		this.sphereCache = new BoundSphere();
		
		this.axisDirty = true;
		this.sphereDirty = true;
	}
	
	/** Store the bounds of this cache's geometry into result.
	 * Does nothing if result is null.
	 * 
	 * If the cache has been marked dirty since the last getBounds()
	 * call, the axis or sphere cache will first be updated by calling
	 * enclose() on this cache's geometry. */
	public void getBounds(BoundVolume result) {
		if (result != null) {
			if (result instanceof AxisAlignedBox) {
				if (this.axisDirty) {
					this.axisCache.enclose(this.geometry);
					this.axisDirty = false;
				}
				this.axisCache.clone(result);
			} else if (result instanceof BoundSphere) {
				if (this.sphereDirty) {
					this.sphereCache.enclose(this.geometry);
					this.sphereDirty = false;
				}
				this.sphereCache.clone(result);
			} else
				result.enclose(this.geometry);
		}
	}
	
	/** Mark the cache as dirty, so that the next call to getBounds()
	 * will update the cache to reflect changes to the geometry. */
	public void setCacheDirty() {
		this.axisDirty = true;
		this.sphereDirty = true;
	}
}
