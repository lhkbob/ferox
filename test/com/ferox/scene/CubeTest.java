package com.ferox.scene;

import org.openmali.vecmath.Vector3f;

import com.ferox.BasicApplication;
import com.ferox.math.BoundSphere;
import com.ferox.math.Color;
import com.ferox.renderer.Renderer;
import com.ferox.resource.Geometry;
import com.ferox.state.Appearance;
import com.ferox.state.LightReceiver;
import com.ferox.state.Material;

public class CubeTest extends BasicApplication {
	public static final boolean DEBUG = false;
	public static final boolean USE_VBO = true;
	
	public static final int NUM_CUBES = 10000;
	public static final int BOUNDS = 100;
	
	protected Geometry geom;
	protected Shape firstCube;
	
	public static void main(String[] args) {
		new CubeTest(DEBUG).run();
	}
	
	public CubeTest(boolean debug) {
		super(debug);
	}

	@Override
	protected SceneElement buildScene(Renderer renderer, ViewNode view) {
		view.getView().setPerspective(60f, 1f, 1f, 1000f);
		view.getLocalTransform().getTranslation().z = 2f * BOUNDS;
		
		Group root = new Group();
		root.add(view);
		
		Light light = new SpotLight();
		
		light.setAmbient(new Color(.2f, 2f, 2f, 1f));
		light.setSpecular(new Color(0f, 0f, 0f, 1f));
		light.setLocalBounds(new BoundSphere(BOUNDS));
		light.getLocalTransform().getTranslation().set(0f, 0f, 0f);
		
		view.add(light);
		
		Appearance a = this.createAppearance();
		this.geom = buildCube(renderer, 2f, USE_VBO);
		
		for (int i = 0; i < NUM_CUBES; i++) {
			Shape shape = new Shape(geom, a);
			shape.setLocalBounds(new BoundSphere());
			Vector3f pos = shape.getLocalTransform().getTranslation();
			
			if (i != 0) {
				// randomly place all but the 1st cube
				pos.x = (float) (Math.random() * BOUNDS - BOUNDS / 2f);
				pos.y = (float) (Math.random() * BOUNDS - BOUNDS / 2f);
				pos.z = (float) (Math.random() * BOUNDS - BOUNDS / 2f);
			} else
				this.firstCube = shape;
			
			root.add(shape);
		}
		
		return root;
	}
	
	private Appearance createAppearance() {
		Material m = new Material(new Color(.8f, .5f, .5f));
		LightReceiver lr = new LightReceiver();
		m.setSmoothShaded(false);
		return new Appearance(m, lr);
	}
}
