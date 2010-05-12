package com.ferox.scene.controller;

import java.util.Iterator;

import com.ferox.scene.DirectionLight;
import com.ferox.scene.SceneElement;
import com.ferox.scene.SpotLight;
import com.ferox.entity.Component;
import com.ferox.entity.ComponentId;
import com.ferox.entity.Controller;
import com.ferox.entity.Entity;
import com.ferox.entity.EntitySystem;

/**
 * <p>
 * The LightUpdateController process an {@link EntitySystem} to update
 * {@link SpotLight SpotLights} and {@link DirectionLight DirectionLights} to
 * match the {@link SceneElement} they are attached to. Any SpotLight or
 * DirectionLight that isn't also a SceneElement is left unmodified. Because
 * this Controller assumes that the SceneElement's transforms are completed, it
 * should be run after the elements are updated (such as via a
 * {@link SceneController}).
 * </p>
 * <p>
 * The LightUpdateController performs the following operations during
 * {@link #process(EntitySystem)}:
 * <ol>
 * <li>For Entities that are both DirectionLights and SceneElements, the
 * direction light's direction vector is set to the 3rd column of the scene
 * element's rotation.</li>
 * <li>For Entities that are both SpotLights and SceneElements, the spot light's
 * direction vector is set to the 3rd column of the scene element's rotation.
 * The light's position is set to the element's translation.</li>
 * </ol>
 * </p>
 * 
 * @author Michael Ludwig
 */
public class LightUpdateController extends Controller {
	private static final ComponentId<SpotLight> SL_ID = Component.getComponentId(SpotLight.class);
	private static final ComponentId<DirectionLight> DL_ID = Component.getComponentId(DirectionLight.class);
	
	private static final ComponentId<SceneElement> SE_ID = Component.getComponentId(SceneElement.class);
	
	public LightUpdateController(EntitySystem system) {
	    super(system);
	}
	
	@Override
	public void process() {
		// update all direction lights so their direction vector
		// matches the z-axis of an attached scene element
		Entity e;
		Iterator<Entity> it = system.iterator(DL_ID);
		while(it.hasNext()) {
			e = it.next();
			processDirectionLight(e.get(DL_ID), e.get(SE_ID));
		}
		
		// update all spot lights so their direction vector
		// matches the z-axis, and their position matches the
		// that of any attached scene element
		it = system.iterator(SL_ID);
		while(it.hasNext()) {
			e = it.next();
			processSpotLight(e.get(SL_ID), e.get(SE_ID));
		}
	}
	
	private void processDirectionLight(DirectionLight dl, SceneElement se) {
		if (se != null) {
			// copy the 3rd column into dl's direction
			se.getTransform().getRotation().getCol(3, dl.getDirection());
		}
	}
	
	private void processSpotLight(SpotLight sl, SceneElement se) {
		if (se != null) {
			// copy the 3rd column into sl's direction
			se.getTransform().getRotation().getCol(3, sl.getDirection());
			
			// copy the translation into sl's position
			sl.getPosition().set(se.getTransform().getTranslation());
		}
	}
}
