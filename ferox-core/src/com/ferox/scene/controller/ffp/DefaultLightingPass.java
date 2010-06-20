package com.ferox.scene.controller.ffp;

import java.util.BitSet;

import com.ferox.entity.Component;
import com.ferox.entity.ComponentId;
import com.ferox.entity.Entity;
import com.ferox.math.bounds.AxisAlignedBox;
import com.ferox.math.bounds.Frustum;
import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.renderer.Surface;
import com.ferox.scene.SceneElement;
import com.ferox.scene.ShadowReceiver;
import com.ferox.util.Bag;

public class DefaultLightingPass extends AbstractFfpRenderPass {
    private static final ComponentId<SceneElement> SE_ID = Component.getComponentId(SceneElement.class);
    private static final ComponentId<ShadowReceiver> SR_ID = Component.getComponentId(ShadowReceiver.class);
    
	private final Component[] lightMap;
	private final AxisAlignedBox[] boundMap;
	private final BitSet activeLights;
	
	public DefaultLightingPass(RenderConnection connection, int maxNumLights, int maxMaterialTexUnits,
						 	   String vertexBinding, String normalBinding, String texCoordBinding) {
		super(connection, maxMaterialTexUnits, vertexBinding, normalBinding, texCoordBinding);
		
		lightMap = new Component[maxNumLights];
		boundMap = new AxisAlignedBox[maxNumLights];
		activeLights = new BitSet(maxNumLights);
	}
	
	@Override
	protected void render(FixedFunctionRenderer ffp) {
		Bag<Component> lightAtoms = connection.getLights();
		Bag<AxisAlignedBox> boundAtoms = connection.getLightBounds();
		Bag<Entity> renderAtoms = connection.getRenderedEntities();
		
		Entity atom;
		int renderCount = renderAtoms.size();
		for (int ri = 0; ri < renderCount; ri++) {
			atom = renderAtoms.get(ri);
			// configure lighting for the render atom
			assignLights(atom, lightAtoms, boundAtoms);
			
			// render it, AbstractFfpRenderPass takes care of state setting
			render(atom);
		}
		
		// clear light map
		for (int i = 0; i < lightMap.length; i++) {
		    lightMap[i] = null;
		    boundMap[i] = null;
		}
		activeLights.clear();
	}
	
	private void assignLights(Entity ra, Bag<Component> lights, Bag<AxisAlignedBox> bounds) {
	    SceneElement se = ra.get(SE_ID);
	    AxisAlignedBox aabb = (se == null ? null : se.getWorldBounds());
	    boolean receivesShadow = ra.get(SR_ID) != null;
	    
		// first check all active lights to see which continue influencing the render atom
		Component la;
		for (int i = activeLights.nextSetBit(0); i >= 0; i = activeLights.nextSetBit(i + 1)) {
			if (!enableLight(i, aabb, receivesShadow, lightMap[i], boundMap[i])) {
				// we're not reusing this light so clear it
				lightMap[i] = null;
				boundMap[i] = null;
				activeLights.clear(i);
			}
		}
		
		// go through all lights that aren't active and apply them
		int nextLightIndex = activeLights.nextClearBit(0);
		int lightCount = lights.size();
		for (int i = 0; i < lightCount; i++) {
			if (nextLightIndex < 0)
				break; // no more room for lights in gl state
			
			la = lights.get(i);
			if (lightMap[i] != la && enableLight(nextLightIndex, aabb, receivesShadow, la, bounds.get(i))) {
				// light was applied, so advance the nextLightIndex
				nextLightIndex = activeLights.nextClearBit(nextLightIndex + 1);
			}
		}
		
		// disable all lights that don't have a mapped atom, just in case
		for (int i = 0; i < lightMap.length; i++) {
			if (lightMap[i] == null)
				setLight(i, null);
		}
	}
	
	private boolean enableLight(int light, AxisAlignedBox rBounds, boolean receivesShadow, 
	                            Component la, AxisAlignedBox lBounds) {
	    if (rBounds == null || lBounds.intersects(rBounds)) {
	        if (lightMap[light] != la || la == connection.getShadowCastingLight()) {
	            if (la != connection.getShadowCastingLight() || !receivesShadow)
	                setLight(light, la);
	            else
	                setLight(light, null);
	        }

	        lightMap[light] = la;
	        boundMap[light] = lBounds;
	        activeLights.set(light);
	        
	        return true;
	    } else
	        return false;
	}

	@Override
	protected Frustum getFrustum() {
		return connection.getViewFrustum();
	}

    @Override
    protected void configureViewport(FixedFunctionRenderer renderer, Surface surface) {
        renderer.setViewport(connection.getViewportLeft(), connection.getViewportBottom(), 
                             connection.getViewportRight() - connection.getViewportLeft(), 
                             connection.getViewportTop() - connection.getViewportBottom());
        renderer.clear(true, true, true, surface.getClearColor(), surface.getClearDepth(), surface.getClearStencil());
    }

    @Override
    protected void notifyPassBegin() {
        connection.notifyBaseLightingPassBegin();
    }

    @Override
    protected void notifyPassEnd() {
        connection.notifyBaseLightingPassEnd();
    }
}
