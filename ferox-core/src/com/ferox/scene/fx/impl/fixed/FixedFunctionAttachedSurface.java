package com.ferox.scene.fx.impl.fixed;

import com.ferox.renderer.RenderSurface;
import com.ferox.renderer.TextureSurface;
import com.ferox.renderer.View;
import com.ferox.scene.LightNode;
import com.ferox.scene.fx.impl.AttachedRenderSurface;
import com.ferox.scene.fx.impl.fixed.FixedFunctionSceneCompositor.RenderMode;

public class FixedFunctionAttachedSurface extends AttachedRenderSurface {
	private TextureSurface shadowMap;
	
	private ShadowMapRenderPass shadowMapPass;
	private BaseRenderPass basePass;
	private LightRenderPass lightPass;
	
	private RenderMode renderMode;
	
	private LightNode<?> lastShadowLight;
	
	private FixedFunctionSceneCompositor compositor;
	
	public FixedFunctionAttachedSurface(FixedFunctionSceneCompositor compositor, 
										RenderSurface surface, View view) {
		super(surface, view);
		this.compositor = compositor;
	}
	
	public void initialize(RenderMode renderMode) {
		if (renderMode == RenderMode.SINGLE_TEXTURE_SHADOWMAP 
			|| renderMode == RenderMode.DUAL_TEXTURE_SHADOWMAP) {
			// require shadow map configuration
			
			// FIXME: create the shadow map
			// and the shadowMapPass and the LightRenderPass
		}
		
		// FIXME: create the base pass
	}
	
	public void destroy() {
		// Remove all non-null render passes
		// destroy the shadow map, too
	}
	
	public void queue() {
		
	}
}
