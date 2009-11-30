package com.ferox.scene.fx.impl;

import java.util.IdentityHashMap;

import com.ferox.renderer.Framework;
import com.ferox.renderer.RenderSurface;
import com.ferox.scene.Scene;
import com.ferox.scene.View;
import com.ferox.scene.fx.GeometryProfile;
import com.ferox.scene.fx.SceneCompositor;

public abstract class AbstractSceneCompositor<T extends AttachedRenderSurface> implements SceneCompositor {
	private boolean initialized;
	private boolean destroyed;
	
	// FIXME: this must be a SortedMap(?)
	// or we should just have parallel lists or something
	protected final IdentityHashMap<RenderSurface, T> surfaces;
	private Framework framework; // extracted from surfaces
	private Scene scene;
	
	public AbstractSceneCompositor() {
		surfaces = new IdentityHashMap<RenderSurface, T>();
		initialized = false;
		destroyed = false;
		
		framework = null;
		scene = null;
	}
	
	@Override
	public Framework getFramework() {
		return framework;
	}
	
	@Override
	public Scene getScene() {
		return scene;
	}
	
	@Override
	public void attach(RenderSurface surface, View view) {
		// special validation so can't use validateState()
		if (destroyed)
			throw new IllegalStateException("Cannot perform action on destroyed SceneCompositor");
		if (initialized)
			throw new IllegalStateException("Cannot attach a RenderSurface on an initialized SceneCompositor");
		
		if (surface == null)
			throw new NullPointerException("Cannot attach a null RenderSurface");
		if (surface.isDestroyed())
			throw new IllegalArgumentException("Cannot attach a destroyed RenderSurface");
		
		if (view != null) {
			// attach a new surface or re-assign a view
			surfaces.put(surface, createSurface(surface, view));
		} else {
			// remove an already attached surface
			surfaces.remove(surface);
		}
	}

	@Override
	public void destroy() {
		validateState();
		destroyed = true;
		initialized = false;
	}

	@Override
	public void initialize(Scene scene, GeometryProfile geomProfile, int capBits) {
		// special validation so can't use validateState()
		if (destroyed)
			throw new IllegalStateException("Cannot perform action on destroyed SceneCompositor");
		if (initialized)
			throw new IllegalStateException("Already initialized");
		
		if (scene == null)
			throw new NullPointerException("Cannot initialize a SceneCompositor with a null Scene");
		
		if (surfaces.isEmpty())
			throw new IllegalStateException("SceneCompositor requires at least 1 attached surface");
		for (RenderSurface surface: surfaces.keySet()) {
			if (surface.isDestroyed())
				throw new IllegalStateException("Cannot use a destroyed RenderSurface");
			if (framework == null)
				framework = surface.getFramework();
			
			if (framework != surface.getFramework())
				throw new IllegalStateException("All attached surfaces must share the same Framework");
		}
		
		this.scene = scene;
		initialized = true;
	}

	@Override
	public boolean isDestroyed() {
		return destroyed;
	}

	@Override
	public boolean isInitialized() {
		return initialized;
	}
	
	protected abstract T createSurface(RenderSurface surface, View view);
	
	protected void validateState() {
		if (isDestroyed())
			throw new IllegalStateException("Cannot perform action on destroyed SceneCompositor");
		if (!isInitialized())
			throw new IllegalStateException("Cannot perform action on unitialized SceneCompositor");
	}
}
