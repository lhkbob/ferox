package com.ferox.scene.controller;

import java.util.Iterator;

import com.ferox.math.bounds.SpatialIndex;
import com.ferox.scene.DirectionLight;
import com.ferox.scene.Influence;
import com.ferox.scene.InfluenceBounds;
import com.ferox.scene.Renderable;
import com.googlecode.entreri.AbstractController;
import com.googlecode.entreri.EntitySystem;

public class LightController extends AbstractController {
    
    @Override
    public void process(EntitySystem system, float dt) {
        SpatialIndex<Renderable> scene = system.getControllerManager().getData(RenderableController.RENDERABLE_HIERARCHY);
        processSpotLights(system, scene);
        processPointLights(system, scene);
        processAmbientLights(system, scene);
        processDirectionLights(system, scene);
    }
    
    private void processSpotLights(EntitySystem system, SpatialIndex<Renderable> scene) {
        
    }
    
    private void processPointLights(EntitySystem system, SpatialIndex<Renderable> scene) {
        
    }
    
    private void processAmbientLights(EntitySystem system, SpatialIndex<Renderable> scene) {
        
    }
    
    private void processDirectionLights(EntitySystem system, SpatialIndex<Renderable> scene) {
        Iterator<DirectionLight> lights = system.iterator(DirectionLight.ID);
        while(lights.hasNext()) {
            DirectionLight light = lights.next();
            Influence influenceList = light.getEntity().get(Influence.ID);
            InfluenceBounds bounds = light.getEntity().get(InfluenceBounds.ID);
        }
    }
}
