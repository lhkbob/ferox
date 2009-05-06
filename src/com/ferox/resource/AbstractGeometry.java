package com.ferox.resource;

import com.ferox.math.BoundVolume;
import com.ferox.math.BoundsCache;
import com.ferox.renderer.RenderDataCache;
import com.ferox.renderer.Renderer;

/**
 * AbstractGeometry provides implementations for many of the methods of
 * Geometry.
 * 
 * @author Michael Ludwig
 */
public abstract class AbstractGeometry implements Geometry {
	private final BoundsCache boundsCache;
	private final RenderDataCache renderCache;
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
		renderCache = new RenderDataCache();
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
	public Object getRenderData(Renderer renderer) {
		return renderCache.getRenderData(renderer);
	}

	@Override
	public void setRenderData(Renderer renderer, Object data) {
		renderCache.setRenderData(renderer, data);
	}
}
