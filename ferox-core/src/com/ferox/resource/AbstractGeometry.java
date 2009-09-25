package com.ferox.resource;

import com.ferox.math.bounds.BoundVolume;
import com.ferox.math.bounds.BoundsCache;
import com.ferox.renderer.Framework;
import com.ferox.util.FastMap;

/**
 * AbstractGeometry provides implementations for many of the methods of
 * Geometry.
 * 
 * @author Michael Ludwig
 */
public abstract class AbstractGeometry implements Geometry {
	private final BoundsCache boundsCache;
	private final FastMap<Framework, Object> renderCache;
	private final CompileType compileType;

	/**
	 * Create an AbstractGeometry that uses the given compile type. It is
	 * intended that the type be immutable during an AbstractGeometry's
	 * lifetime.
	 * 
	 * @param type The compile type to use, null == NONE
	 */
	public AbstractGeometry(CompileType type) {
		compileType = (type == null ? CompileType.NONE : type);
		renderCache = new FastMap<Framework, Object>(Framework.class);
		boundsCache = new BoundsCache(this);
	}

	/**
	 * Clear the cached bounds so that it's recomputed for the next getBounds()
	 * call.
	 */
	protected void clearBoundsCache() {
		boundsCache.setCacheDirty();
	}

	@Override
	public CompileType getCompileType() {
		return compileType;
	}

	@Override
	public void getBounds(BoundVolume result) {
		boundsCache.getBounds(result);
	}

	@Override
	public void clearDirtyDescriptor() {
		// do nothing
	}

	@Override
	public Object getDirtyDescriptor() {
		return null;
	}

	@Override
	public Object getRenderData(Framework renderer) {
		return renderCache.get(renderer);
	}

	@Override
	public void setRenderData(Framework renderer, Object data) {
		renderCache.put(renderer, data);
	}
}
