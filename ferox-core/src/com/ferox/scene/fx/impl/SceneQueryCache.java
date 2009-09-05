package com.ferox.scene.fx.impl;

import com.ferox.math.Frustum;
import com.ferox.math.bounds.BoundVolume;
import com.ferox.scene.Scene;
import com.ferox.scene.SceneElement;
import com.ferox.util.Bag;

public class SceneQueryCache {
	private Scene scene;
	
	public void reset() {
		
	}

	public Bag<SceneElement> query(BoundVolume volume, Class<? extends SceneElement> index) {
		
	}
	
	public Bag<SceneElement> query(Frustum frustum, Class<? extends SceneElement> index) {
		
	}
}
