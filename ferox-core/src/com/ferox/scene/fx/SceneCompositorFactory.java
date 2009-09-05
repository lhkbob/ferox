package com.ferox.scene.fx;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.ferox.renderer.Framework;
import com.ferox.renderer.RenderSurface;
import com.ferox.renderer.View;
import com.ferox.scene.Scene;
import com.ferox.scene.fx.impl.fixed.FixedFunctionSceneCompositor;

public class SceneCompositorFactory {
	public static final int SC_FIXED = 0x1;
	public static final int SC_FORWARD = 0x2;
	public static final int SC_DEFERRED = 0x4;
	
	public static enum CommonGeometryProfile {
		BLANK, PRIMARY_TEX0, PRIMARY_TEX0_DECAL_TEX1
	}
	
	private final Framework framework;
	private final Scene scene;

	private final Map<RenderSurface, View> surfaces;
	
	private int optionsBits;
	private GeometryProfile profile;
	
	// built SceneCompositor
	private SceneCompositor sceneCompositor;
	
	public SceneCompositorFactory(Framework framework, Scene scene) {
		if (framework == null)
			throw new NullPointerException("Null framework");
		if (scene == null)
			throw new NullPointerException("Null scene");
		
		this.framework = framework;
		this.scene = scene;
		this.surfaces = new HashMap<RenderSurface, View>();
	}
	
	public SceneCompositorFactory addOptions(int bits) {
		optionsBits |= bits;
		return this;
	}
	
	public SceneCompositorFactory removeOptions(int bits) {
		optionsBits &= ~bits;
		return this;
	}
	
	public SceneCompositorFactory setOptions(int bits) {
		optionsBits = bits;
		return this;
	}
	
	public SceneCompositorFactory setGeometryProfile(GeometryProfile profile) {
		this.profile = profile;
		return this;
	}
	
	public SceneCompositorFactory setCommonProfile(CommonGeometryProfile profile) {
		if (profile == null)
			profile = CommonGeometryProfile.BLANK;
		GeometryProfile common = new GeometryProfile();
		switch(profile) {
		case BLANK:
			// do nothing
			break;
		case PRIMARY_TEX0:
			common.setTextureUnitBound(0, true);
			break;
		case PRIMARY_TEX0_DECAL_TEX1:
			common.setTextureUnitBound(0, true);
			common.setTextureUnitBound(1, true);
			break;
		}
		
		return setGeometryProfile(common);
	}
	
	public SceneCompositorFactory attach(RenderSurface surface, View view) {
		if (surface == null)
			throw new NullPointerException("RenderSurface can't be null");
		if (view == null)
			throw new NullPointerException("View can't be null");
		
		if (surface.isDestroyed())
			throw new IllegalArgumentException("Cannot attach a RenderSurface which is destroyed");
		if (surface.getFramework() != framework)
			throw new IllegalArgumentException("Cannot attach a RenderSurface belonging to a different Framework");
		
		surfaces.put(surface, view);
		return this;
	}
	
	public SceneCompositorFactory detach(RenderSurface surface) {
		if (surface != null && surface.getFramework() == framework)
			surfaces.remove(surface);
		return this;
	}
	
	public SceneCompositor getCompositor() {
		if (sceneCompositor == null) {
			// FIXME: do bit caps determination of type of scene compositor
			sceneCompositor = new FixedFunctionSceneCompositor();
			
			if (profile == null)
				setCommonProfile(CommonGeometryProfile.BLANK);
			
			for (Entry<RenderSurface, View> e: surfaces.entrySet()) {
				sceneCompositor.attach(e.getKey(), e.getValue());
			}
			sceneCompositor.initialize(scene, profile, optionsBits);
		}
		
		return sceneCompositor;
	}
}
