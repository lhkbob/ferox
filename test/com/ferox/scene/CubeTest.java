package com.ferox.scene;

import java.io.File;
import java.io.IOException;

import org.openmali.vecmath.Vector3f;

import com.ferox.BasicApplication;
import com.ferox.math.BoundSphere;
import com.ferox.math.Color;
import com.ferox.renderer.Renderer;
import com.ferox.resource.Geometry;
import com.ferox.resource.TextureImage;
import com.ferox.resource.util.TextureIO;
import com.ferox.state.Appearance;
import com.ferox.state.LightReceiver;
import com.ferox.state.Material;
import com.ferox.state.Texture;
import com.ferox.state.Texture.EnvMode;
import com.ferox.state.Texture.TexCoordGen;

public class CubeTest extends BasicApplication {
	public static final boolean DEBUG = false;
	public static final boolean USE_VBO = true;
	public static final boolean RANDOM_PLACEMENT = false;
	
	public static final int NUM_CUBES = 10000;
	public static final int BOUNDS = 100;
	
	protected Geometry geom;
	
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
		
		Light spotLight = new SpotLight();
		
		spotLight.setLocalBounds(new BoundSphere(BOUNDS));
		spotLight.getLocalTransform().getTranslation().set(0f, 0f, 0f);
		
		view.add(spotLight);
		
		Light directionLight = new DirectionLight(new Vector3f(-1f, -1f, -1f));
		directionLight.setLocalBounds(new BoundSphere(BOUNDS));
		root.add(directionLight);
		
		Appearance[] apps = this.createAppearances(renderer);
		this.geom = buildCube(renderer, 2f, USE_VBO);
		
		// vars for regular gridding
		int sideCubeCount = (int) (Math.ceil(Math.pow(NUM_CUBES, 1.0 / 3.0)));
		float scale = BOUNDS / (float) sideCubeCount;
		int x = 0;
		int y = 0;
		int z = 0;
		
		for (int i = 0; i < NUM_CUBES; i++) {
			Shape shape = new Shape(geom, apps[i % apps.length]);
			shape.setLocalBounds(new BoundSphere());
			Vector3f pos = shape.getLocalTransform().getTranslation();
			
			if (RANDOM_PLACEMENT) {
				if (i != 0) {
					// randomly place all but the 1st cube
					pos.x = (float) (Math.random() * BOUNDS - BOUNDS / 2f);
					pos.y = (float) (Math.random() * BOUNDS - BOUNDS / 2f);
					pos.z = (float) (Math.random() * BOUNDS - BOUNDS / 2f);
				}
			} else {
				pos.x = scale * x - BOUNDS / 2f;
				pos.y = scale * y - BOUNDS / 2f;
				pos.z = scale * z - BOUNDS / 2f;
				
				x++;
				if (x >= sideCubeCount) {
					x = 0;
					y++;
					if (y >= sideCubeCount) {
						y = 0;
						z++;
					}
				}
			}
			
			root.add(shape);
		}
		
		return root;
	}
	
	private Appearance[] createAppearances(Renderer renderer) {
		Appearance[] apps = new Appearance[4];
		for (int i = 0; i < apps.length; i++)
			apps[i] = this.createAppearance(renderer, i, apps.length);
		return apps;
	}
	
	private Appearance createAppearance(Renderer renderer, int i, int max) {
		float percent = (float) i / max;
		Material m;
		
		if (percent < .3333f) {
			m = new Material(new Color(1f, percent, percent));
		} else if (percent < .666667f) {
			m = new Material(new Color(percent / 2f, 1f, percent / 2f));
		} else {
			m = new Material(new Color(percent / 3f, percent / 3f, 1f));
		}

		LightReceiver lr = null;
		if (i % 2 == 0) {
			lr = new LightReceiver();
			m.setSmoothShaded(true);
		}
		
		Texture t = null;
		
		if (i < max / 2) {
			try {
				TextureImage image = TextureIO.readTexture(new File("data/textures/squiggles.tga"));
				renderer.requestUpdate(image, false);
				t = new Texture(image, EnvMode.MODULATE, new Color(), TexCoordGen.NONE);
			} catch (IOException io) {
				throw new RuntimeException(io);
			}
		} else {
			try {
				TextureImage image = TextureIO.readTexture(new File("data/textures/grace_cube.dds"));
				renderer.requestUpdate(image, false);
				t = new Texture(image, EnvMode.MODULATE, new Color(), TexCoordGen.OBJECT);
			} catch (IOException io) {
				throw new RuntimeException(io);
			}
		}
		
		return new Appearance(m, lr, t);
	}
}
