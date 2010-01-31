package com.ferox.scene.fx.fixed;

import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.renderer.Renderer.BlendFactor;
import com.ferox.renderer.Renderer.BlendFunction;

public class BaseLightPass extends AbstractFfpRenderPass {
	private final int maxNumLights;
	
	public BaseLightPass(RenderMode render, int maxNumLights,
						 String vertexBinding, String normalBinding, String texCoordBinding) {
		super(render, vertexBinding, normalBinding, texCoordBinding);
		this.maxNumLights = maxNumLights;
	}
	
	@Override
	protected void render(FixedFunctionRenderer ffp) {
		
		int lightCount = lightAtoms.size();
		for (int firstLight = 0; firstLight < lightCount; firstLight += maxNumLights) {
			// configure the lights for the scene
			for (int li = 0; li < maxNumLights; li++) {
				if (firstLight + li < lightCount)
					setLight(li, lightAtoms.get(firstLight + li));
				else
					setLight(li, null);
			}
			
			if (firstLight >= maxNumLights) {
				// no longer first pass so we must enable additive blending
				ffp.setBlendingEnabled(true);
				ffp.setBlendMode(BlendFunction.ADD, BlendFactor.ONE, BlendFactor.ONE);
			}
			
			// render all RenderAtoms
			int renderCount = renderAtoms.size();
			boolean useShadowLight = true;

			RenderAtom atom;
			LightAtom light;
			for (int ri = 0; ri < renderCount; ri++) {
				atom = renderAtoms.get(ri);
				// enable/disable lights as needed
				for (int li = 0; li < maxNumLights; li++) {
					if (firstLight + li < lightCount) {
						light = lightAtoms.get(firstLight + li);
						ffp.setLightEnabled(li, (light.worldBounds != null ? light.worldBounds.intersects(atom.worldBounds) : true));
						
						if (light == shadowLight) {
							if (useShadowLight && !atom.receivesShadow) {
								// use full light color
								useShadowLight = false;
								setLightColor(li, light, false);
							} else if (!useShadowLight && atom.receivesShadow) {
								// use dimmed light color
								useShadowLight = true;
								setLightColor(li, light, true);
							}
						}
					}
				}
				
				// render it
				renderAtom(atom);
			}
		}
	}
}
