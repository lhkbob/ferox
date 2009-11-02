package com.ferox.scene.fx;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.ferox.renderer.Framework;
import com.ferox.renderer.RenderSurface;
import com.ferox.scene.Scene;
import com.ferox.scene.fx.impl.fixed.FixedFunctionSceneCompositor;
import com.ferox.shader.View;

public class SceneCompositorFactory {
	public static final int SC_FIXED = 0x1;
	public static final int SC_FORWARD = 0x2;
	public static final int SC_DEFERRED = 0x4;
	
	// FIXME: improve SM bit options
	public static final int SM_G_SIMPLE = 0x10;
	public static final int SM_G_PERSPECTIVE = 0x20;
	public static final int SM_G_CASCADE = 0x40;
	
	public static final int SM_L_PCF = 0x100;
	public static final int SM_L_VARIANCE = 0x200;
	public static final int SM_L_PCSS = 0x400;
	
	public static final int FS_BLOOM = 0x1000;
	public static final int FS_HDR = 0x2000;
	public static final int FS_MOTION_BLUR = 0x4000;
	public static final int FS_DEPTH_OF_FIELD = 0x8000;
	public static final int FS_AMBIENT_OCCLUSION = 0x10000;
	
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
