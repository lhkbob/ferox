package com.ferox.scene.fx.impl.fixed;

import java.util.Iterator;
import java.util.Map.Entry;

import com.ferox.math.Frustum;
import com.ferox.math.bounds.BoundVolume;
import com.ferox.renderer.FrameStatistics;
import com.ferox.renderer.RenderCapabilities;
import com.ferox.renderer.RenderSurface;
import com.ferox.renderer.View;
import com.ferox.scene.Scene;
import com.ferox.scene.SceneElement;
import com.ferox.scene.fx.Appearance;
import com.ferox.scene.fx.GeometryProfile;
import com.ferox.scene.fx.SceneCompositor;
import com.ferox.scene.fx.impl.AbstractSceneCompositor;
import com.ferox.scene.fx.impl.AppearanceCache;
import com.ferox.scene.fx.impl.SceneQueryCache;
import com.ferox.util.Bag;

public class FixedFunctionSceneCompositor extends AbstractSceneCompositor<FixedFunctionAttachedSurface> {
	public static enum RenderMode {
		NO_TEXTURE_NO_SHADOWMAP(0, false),
		SINGLE_TEXTURE_NO_SHADOWMAP(1, false),
		DUAL_TEXTURE_NO_SHADOWMAP(2, false),
		SINGLE_TEXTURE_SHADOWMAP(1, true),
		DUAL_TEXTURE_SHADOWMAP(2, true);
		
		private int numTex; private boolean shadows;
		private RenderMode(int numTex, boolean shadows) {
			this.numTex = numTex; this.shadows = shadows;
		}
		
		public int getMinimumTextures() { return numTex; }
		
		public boolean getShadowsEnabled() { return shadows; }
	}
	
	private static final int SM_ALL = SceneCompositor.SM_G_CASCADE | SceneCompositor.SM_G_SIMPLE | SceneCompositor.SM_G_PERSPECTIVE;
	
	private SceneQueryCache sceneCache;
	private AppearanceCache<FixedFunctionAppearance> appearanceCache;

	private int shadowMapTextureUnit;
	private RenderMode renderMode;
	
	public FixedFunctionSceneCompositor() {
		super();
	}
	
	FixedFunctionAppearance get(Appearance a) {
		return appearanceCache.get(a);
	}
	
	Bag<SceneElement> query(BoundVolume volume, Class<? extends SceneElement> index) {
		return sceneCache.query(volume, index);
	}
	
	Bag<SceneElement> query(Frustum frustum, Class<? extends SceneElement> index) {
		return sceneCache.query(frustum, index);
	}
	
	public RenderMode getRenderMode() {
		return renderMode;
	}
	
	public int getShadowMapTextureUnit() {
		return shadowMapTextureUnit;
	}
	
	@Override
	public void compile(Appearance a) {
		validateState();
		appearanceCache.compile(a);
	}
	
	@Override
	public void clean(Appearance a) {
		validateState();
		appearanceCache.clean(a);
	}

	@Override
	public void destroy() {
		super.destroy();
		
		appearanceCache.cleanAll();
		for (Entry<RenderSurface, FixedFunctionAttachedSurface> e: surfaces.entrySet()) {
			e.getValue().destroy();
		}
	}

	@Override
	public void initialize(Scene scene, GeometryProfile geomProfile, int capBits) {
		super.initialize(scene, geomProfile, capBits);
		
		RenderCapabilities caps = getFramework().getCapabilities();
		
		int numTex = caps.getMaxFixedPipelineTextures();
		boolean shadowsRequested = (capBits & SM_ALL) != 0; // any SM_? request counts
		boolean shadowSupport = (caps.getFboSupport() || caps.getPbufferSupport()) && 
								numTex > 1 && caps.getVersion() > 1.3f; // FIXME: is this the correct version
								
		RenderMode mode = null;
		if (shadowsRequested && shadowSupport) {
			// choose between DUAL_TEX_SM or SING_TEX_SM
			if (numTex > 2)
				mode = RenderMode.DUAL_TEXTURE_SHADOWMAP;
			else
				mode = RenderMode.SINGLE_TEXTURE_SHADOWMAP;

			int texUnit = -1;
			for (int i = 0; i < numTex; i++) {
				if (!geomProfile.isTextureUnitBound(i)) {
					texUnit = i;
					break;
				}
			}
			
			if (texUnit < 0) {
				// no tex unit, so disable shadow mapping
				mode = null;
			} else {
				// store for later
				shadowMapTextureUnit = texUnit;
				geomProfile.setTextureUnitBound(texUnit, true);
			}
		}
		
		if (mode == null) {
			// choose between DUAL_TEX_NSM, SING_TEX_NSM, and NO_TEX_NSM
			if (numTex > 1)
				mode = RenderMode.DUAL_TEXTURE_NO_SHADOWMAP;
			else if (numTex == 1)
				mode = RenderMode.SINGLE_TEXTURE_NO_SHADOWMAP;
			else
				mode = RenderMode.NO_TEXTURE_NO_SHADOWMAP;
			shadowMapTextureUnit = -1;
		}
		renderMode = mode;
		
		FixedFunctionAppearanceCompiler compiler = new FixedFunctionAppearanceCompiler(mode);
		appearanceCache = new AppearanceCache<FixedFunctionAppearance>(this, compiler);
		sceneCache = new SceneQueryCache(scene);
		
		Entry<RenderSurface, FixedFunctionAttachedSurface> e;
		Iterator<Entry<RenderSurface, FixedFunctionAttachedSurface>> it = surfaces.entrySet().iterator();
		while(it.hasNext()) {
			e = it.next();
			if (e.getKey().isDestroyed())
				it.remove();
			else
				e.getValue().initialize(renderMode, shadowMapTextureUnit);
		}
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
