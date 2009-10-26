package com.ferox.scene.fx.impl.fixed;

import com.ferox.math.Color4f;
import com.ferox.math.Vector3f;
import com.ferox.renderer.DisplayOptions;
import com.ferox.renderer.Framework;
import com.ferox.renderer.RenderSurface;
import com.ferox.renderer.TextureSurface;
import com.ferox.renderer.View;
import com.ferox.renderer.DisplayOptions.DepthFormat;
import com.ferox.renderer.DisplayOptions.PixelFormat;
import com.ferox.resource.TextureImage.TextureTarget;
import com.ferox.scene.DirectionLightNode;
import com.ferox.scene.LightNode;
import com.ferox.scene.SceneElement;
import com.ferox.scene.fx.impl.AttachedRenderSurface;
import com.ferox.scene.fx.impl.fixed.FixedFunctionSceneCompositor.RenderMode;
import com.ferox.shader.DirectionLight;
import com.ferox.util.Bag;

public class FixedFunctionAttachedSurface extends AttachedRenderSurface {
	private TextureSurface shadowMap; // FIXME: once new RS system is done, this can be shared / compositor
	
	private ShadowMapRenderPass shadowMapPass;
	private BaseRenderPass basePass;
	private LightRenderPass lightPass;
	
	private DirectionLight lastShadowLight;
	private final DirectionLight dimShadowLight;
	
	private final FixedFunctionSceneCompositor compositor;
	
	public FixedFunctionAttachedSurface(FixedFunctionSceneCompositor compositor, 
										RenderSurface surface, View view) {
		super(surface, view);
		this.compositor = compositor;
		dimShadowLight = new DirectionLight(new Vector3f(0f, 0f, 1f));
	}
	
	public DirectionLight getDimmedShadowLight() {
		return (lastShadowLight == null ? null : dimShadowLight);
	}
	
	public DirectionLight getShadowCastingLight() {
		return lastShadowLight;
	}
	
	public FixedFunctionSceneCompositor getCompositor() {
		return compositor;
	}
	
	public TextureSurface getShadowMap() {
		return shadowMap;
	}
	
	public void initialize(RenderMode renderMode, int shadowMapUnit) {
		basePass = new BaseRenderPass(this);
		
		if (renderMode.getShadowsEnabled()) {
			// requires shadow map configuration
			Framework f = compositor.getFramework();
			shadowMap = f.createTextureSurface(new DisplayOptions(PixelFormat.NONE, DepthFormat.DEPTH_24BIT), 
											   TextureTarget.T_2D, 512, 512, 1, 0, 0, false);
			
			shadowMapPass = new ShadowMapRenderPass(this);
			
			lightPass = new LightRenderPass(this);
		}
	}
	
	public void destroy() {
		if (shadowMap != null) {
			compositor.getFramework().destroy(shadowMap);
			shadowMap = null;
		}
		
		basePass = null;
		lightPass = null;
		shadowMapPass = null;
	}
	
	public void queue() {
		getView().updateView();
		Bag<SceneElement> lights = compositor.query(getView().getFrustum(), LightNode.class);
		lastShadowLight = chooseLight(lights, getView());
		if (lastShadowLight != null) {
			// reconfigure the dim light
			
		}
		
		Framework f = compositor.getFramework();
		if (shadowMap != null)
			f.queue(shadowMap, shadowMapPass, false, true, false);
		f.queue(getSurface(), basePass);
		f.queue(getSurface(), lightPass);
	}
	
	private DirectionLight chooseLight(Bag<SceneElement> lights, View view) {
		float weight = 0f;
		DirectionLightNode node = null;
		
		LightNode<?> l;
		float w;
		int size = lights.size();
		for (int i = 0; i < size; i++) {
			l = (LightNode<?>) lights.get(i);
			if (l instanceof DirectionLightNode && l.isShadowCaster()) {
				w = calculateLightWeight(((DirectionLightNode) l).getLight(), view);
				if (w > weight) {
					node = (DirectionLightNode) l;
					weight = w;
				}
			}
		}
		
		return (node == null ? null : node.getLight());
	}
	
	private float calculateLightWeight(DirectionLight light, View view) {
		// [0, 3]
		float brightness = .6f * colorSum(light.getDiffuse()) + .4f * colorSum(light.getSpecular());
		
		// [-1, 1]
		float direction = view.getDirection().dot(light.getDirection());
		
		// [0, 1]
		float bonus = (light == lastShadowLight ? 1f : 0f);
		
		// [0, 1]
		return (brightness / 3f + (direction + 1f) / 2f + bonus) / 3f;
	}
	
	private float colorSum(Color4f color) {
		return color.getRed() + color.getBlue() + color.getGreen();
	}
}
