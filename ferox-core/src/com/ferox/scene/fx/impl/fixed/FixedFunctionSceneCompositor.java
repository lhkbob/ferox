package com.ferox.scene.fx.impl.fixed;

import java.util.Iterator;
import java.util.Map.Entry;

import com.ferox.renderer.FrameStatistics;
import com.ferox.renderer.RenderSurface;
import com.ferox.renderer.View;
import com.ferox.scene.Scene;
import com.ferox.scene.fx.Appearance;
import com.ferox.scene.fx.GeometryProfile;
import com.ferox.scene.fx.impl.AbstractSceneCompositor;
import com.ferox.scene.fx.impl.AppearanceCache;
import com.ferox.scene.fx.impl.SceneQueryCache;

public class FixedFunctionSceneCompositor extends AbstractSceneCompositor<FixedFunctionAttachedSurface> {
	public static enum RenderMode {
		NO_TEXTURE_NO_SHADOWMAP,
		SINGLE_TEXTURE_NO_SHADOWMAP,
		DUAL_TEXTURE_NO_SHADOWMAP,
		SINGLE_TEXTURE_SHADOWMAP,
		DUAL_TEXTURE_SHADOWMAP
	}
	
	private SceneQueryCache sceneCache;
	private AppearanceCache<FixedFunctionAppearance> appearanceCache;

	private int shadowMapTextureUnit;
	private GeometryProfile geomProfile;
	private RenderMode renderMode;
	
	public FixedFunctionSceneCompositor() {
		super();
	}
	
	// TODO add access to the scene cache
	// TODO add methods to get a FixedFunctionAppearance

	@Override
	public void compile(Appearance a) {
		validateState();
		
		appearanceCache.compile(a);
	}
	
	@Override
	public void clear(Appearance a) {
		validateState();
		
		appearanceCache.clean(a);
	}

	@Override
	public void destroy() {
		super.destroy();
		
		for (Entry<RenderSurface, FixedFunctionAttachedSurface> e: surfaces.entrySet()) {
			e.getValue().destroy();
		}
		appearanceCache.cleanAll();
	}

	@Override
	public void initialize(Scene scene, GeometryProfile geomProfile, int capBits) {
		super.initialize(scene, geomProfile, capBits);
		// TODO determine render mode, choose shadow map texture unit
		// and then call initialize on all attached surfaces
		
		// To have shadow mapping we need:
		// pbuffers or fbos, 1+ textures and depth texture support
		// + have shadows requested by capBits
		
		// DUAL_TEX_SM - shadow map support w/ 3+ textures
		// SING_TEX_SM - shadow map support w/ 2 textures
		// DUAL_TEX_NSM - 2+ textures w/ no sm support
		// SING_TEX_NSM - 1 texture (don't care about sm support)
		// NO_TEX_NSM - 0 textures
	}

	@Override
	public FrameStatistics render(FrameStatistics stats) {
		validateState();
		
		Entry<RenderSurface, FixedFunctionAttachedSurface> e;
		Iterator<Entry<RenderSurface, FixedFunctionAttachedSurface>> it = surfaces.entrySet().iterator();
		while(it.hasNext()) {
			e = it.next();
			if (e.getKey().isDestroyed()) {
				e.getValue().destroy();
				it.remove();
			} else {
				// the attached surface will handle everything
				e.getValue().queue();
			}
		}
		
		try {
			return getFramework().renderFrame(stats);
		} finally {
			appearanceCache.reset();
			sceneCache.reset();
		}
	}

	@Override
	protected FixedFunctionAttachedSurface createSurface(RenderSurface surface, View view) {
		return new FixedFunctionAttachedSurface(this, surface, view);
	}
}
