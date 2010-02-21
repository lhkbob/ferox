package com.ferox.scene.ffp;

import com.ferox.math.Frustum;

public interface RenderConnection {
	public static interface Stream<T> {
		public void push(T item);
		
		public T newInstance();
	}
	
	public Stream<ShadowAtom> openShadowAtomStream();
	
	public Stream<RenderAtom> openRenderAtomStream();
	
	public Stream<LightAtom> openLightAtomStream();
	
	public void setShadowLight(Frustum shadowFrustum, LightAtom shadowCaster);
	
	public void setView(Frustum frustum, float left, float right, float bottom, float top);
}
