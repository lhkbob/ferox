package com.ferox.scene.ffp;

import com.ferox.math.bounds.Frustum;

public interface RenderConnection {
	public static interface Stream<T> {
		public void push(T item);
		
		public T newInstance();
	}
	
	public Stream<ShadowAtom> getShadowAtomStream();
	
	public Stream<RenderAtom> getRenderAtomStream();
	
	public Stream<LightAtom> getLightAtomStream();
	
	public Stream<FogAtom> getFogAtomStream();
	
	public void setShadowLight(Frustum shadowFrustum, LightAtom shadowCaster);
	
	public void setView(Frustum frustum, int left, int right, int bottom, int top);
	
	public void close();
}
