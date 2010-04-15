package com.ferox.scene.ffp;

import java.util.BitSet;

import com.ferox.math.Frustum;
import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.util.Bag;

public class DefaultLightingPass extends AbstractFfpRenderPass {
	private final LightAtom[] lightMap;
	private final BitSet activeLights;
	
	public DefaultLightingPass(int maxNumLights, int maxMaterialTexUnits,
						 	   String vertexBinding, String normalBinding, String texCoordBinding) {
		super(maxMaterialTexUnits, vertexBinding, normalBinding, texCoordBinding);
		
		lightMap = new LightAtom[maxNumLights];
		activeLights = new BitSet(maxNumLights);
	}
	
	@Override
	protected void render(FixedFunctionRenderer ffp) {
		Bag<LightAtom> lightAtoms = renderDescr.getLightAtoms();
		Bag<RenderAtom> renderAtoms = renderDescr.getRenderAtoms();
		
		RenderAtom atom;
		int renderCount = renderAtoms.size();
		for (int ri = 0; ri < renderCount; ri++) {
			atom = renderAtoms.get(ri);
			// configure lighting for the render atom
			assignLights(atom, lightAtoms);
			
			// render it, AbstractFfpRenderPass takes care of state setting
			renderAtom(atom);
			
			// flag the atom as needing a second pass if it receives shadows
			// and the shadow light was active for its rendering
			atom.requiresShadowPass = atom.receivesShadow && (renderDescr.getShadowCaster() == null ? false : renderDescr.getShadowCaster().active);
		}
		
		// clear light map
		for (int i = 0; i < lightMap.length; i++) {
			if (lightMap[i] != null) {
				lightMap[i].active = false;
				lightMap[i] = null;
			}
		}
		activeLights.clear();
	}
	
	private void assignLights(RenderAtom ra, Bag<LightAtom> lights) {
		// first check all active lights to see which continue 
		// influencing the render atom
		LightAtom la;
		for (int i = activeLights.nextSetBit(0); i >= 0; i = activeLights.nextSetBit(i + 1)) {
			la = lightMap[i];
			if (!enableLight(i, ra, la)) {
				// we're not reusing this light so clear it
				lightMap[i].active = false;
				lightMap[i] = null;
				activeLights.clear(i);
			}
		}
		
		// go through all lights that aren't active and apply them
		int nextLightIndex = activeLights.nextSetBit(0);
		int lightCount = lights.size();
		for (int i = 0; i < lightCount; i++) {
			if (nextLightIndex < 0)
				break; // no more room for lights in gl state
			
			la = lights.get(i);
			if (!la.active && enableLight(nextLightIndex, ra, la)) {
				// light was applied, so advance the nextLightIndex
				nextLightIndex = activeLights.nextSetBit(nextLightIndex + 1);
			}
		}
		
		// disable all lights that don't have a mapped atom, just in case
		for (int i = 0; i < lightMap.length; i++) {
			if (lightMap[i] == null)
				setLight(i, null);
		}
	}
	
	private boolean enableLight(int light, RenderAtom ra, LightAtom la) {
		if (ra.worldBounds == null || la.worldBounds == null || 
			la.worldBounds.intersects(ra.worldBounds)) {
			// the light influences the atom
			// invoke opengl commands if the atom has changed, or if it's the
			// shadow casting light (in case the render atom's have changed shadow policy)
			if (lightMap[light] != la || la == renderDescr.getShadowCaster())
				setLight(light, la, la == renderDescr.getShadowCaster() && ra.receivesShadow);
			
			// update light tracking
			lightMap[light] = la;
			activeLights.set(light);
			
			la.active = true;
			
			return true;
		} else
			return false;
	}

	@Override
	protected Frustum getFrustum() {
		return (renderDescr == null ? null : renderDescr.getViewFrustum());
	}

	@Override
	protected boolean initViewport(FixedFunctionRenderer ffp) {
		// set the viewport based on the description
		ffp.setViewport(renderDescr.getViewportX(), renderDescr.getViewportY(),
						renderDescr.getViewportWidth(), renderDescr.getViewportHeight());
		return true;
	}
}
