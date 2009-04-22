package com.ferox.scene;

import java.io.IOException;

import org.openmali.vecmath.Vector3f;

import com.ferox.BasicApplication;
import com.ferox.math.AxisAlignedBox;
import com.ferox.math.BoundSphere;
import com.ferox.math.Color;
import com.ferox.renderer.Renderer;
import com.ferox.resource.Geometry;
import com.ferox.resource.geometry.Box;
import com.ferox.resource.geometry.Sphere;
import com.ferox.resource.geometry.VertexArrayGeometry;
import com.ferox.resource.geometry.VertexBufferGeometry;
import com.ferox.resource.geometry.Sphere.SphereTextureMode;
import com.ferox.resource.texture.TextureImage;
import com.ferox.resource.texture.loader.TextureLoader;
import com.ferox.scene.Fog.FogEquation;
import com.ferox.scene.Node.CullMode;
import com.ferox.state.Appearance;
import com.ferox.state.FogReceiver;
import com.ferox.state.LightReceiver;
import com.ferox.state.Material;
import com.ferox.state.PolygonStyle;
import com.ferox.state.Texture;
import com.ferox.state.FogReceiver.FogCoordSource;
import com.ferox.state.PolygonStyle.DrawStyle;
import com.ferox.state.State.Quality;
import com.ferox.state.Texture.EnvMode;
import com.ferox.state.Texture.TexCoordGen;

public class DisplayListTest extends BasicApplication {
	public static final boolean DEBUG = false;
	public static final boolean USE_VBO = false;
	public static final boolean RANDOM_PLACEMENT = true;
	
	public static final int NUM_CUBES = 10000;
	public static final int BOUNDS = 100;
	
	public static final Color bgColor = new Color(.5f, .2f, .2f);
	
	protected Geometry displayList;
	
	public static void main(String[] args) {
		new DisplayListTest(DEBUG).run();
	}
	
	public DisplayListTest(boolean debug) {
		super(debug);
	}

	@Override
	protected SceneElement buildScene(Renderer renderer, ViewNode view) {
		this.window.setClearColor(bgColor);
		view.getView().setPerspective(60f, this.window.getWidth() / (float) this.window.getHeight(), 1f, 300f);
		view.getLocalTransform().getTranslation().z = 2f * BOUNDS;
		
		Geometry cube;
		if (USE_VBO)
			cube = new VertexBufferGeometry(new Box(2f).requestVboUpdate(renderer, true));
		else
			cube = new VertexArrayGeometry(new Box(2f));
		renderer.requestUpdate(cube, true);
		
		// vars for regular gridding
		int sideCubeCount = (int) (Math.ceil(Math.pow(NUM_CUBES, 1.0 / 3.0)));
		float scale = BOUNDS / (float) sideCubeCount;
		int x = 0;
		int y = 0;
		int z = 0;
		
		Appearance[] apps = this.createAppearances(renderer);
		Group compiledScene = new Group(NUM_CUBES);
		for (int i = 0; i < NUM_CUBES; i++) {
			Shape shape = new Shape(cube, apps[i % apps.length]);
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
			
			compiledScene.add(shape);
		}
		
		renderer.flushRenderer(null);
		compiledScene.update(true);
		this.displayList = renderer.compile(compiledScene.compile(null));
		
		Group root = new Group();
		root.add(view);
		
		Light spotLight = new SpotLight();
		
		spotLight.setLocalBounds(new BoundSphere(BOUNDS));
		spotLight.getLocalTransform().getTranslation().set(0f, 0f, 0f);
		view.add(spotLight);
		
		Light directionLight = new DirectionLight(new Vector3f(-1f, -1f, -1f));
		directionLight.setLocalBounds(new BoundSphere(BOUNDS));
		root.add(directionLight);
		
		Fog fog = new Fog(bgColor, 10f, 300f, 1f, FogEquation.LINEAR, Quality.BEST);
		fog.setLocalBounds(new BoundSphere(BOUNDS));
		root.add(fog);
		
		BoundSphere b = new BoundSphere();
		this.displayList.getBounds(b);
		
		VertexArrayGeometry dlBounds = new VertexArrayGeometry(new Sphere(b.getCenter(), b.getRadius(), 32, 32, SphereTextureMode.ORIGINAL));
		renderer.requestUpdate(dlBounds, true);
		Shape dlBoundsShape = new Shape(dlBounds, new Appearance(new Material(new Color(1f, 0f, 0f)), new PolygonStyle(DrawStyle.LINE, DrawStyle.NONE)));
		dlBoundsShape.setCullMode(CullMode.NEVER);
		root.add(dlBoundsShape);
		
		AxisAlignedBox aabb = new AxisAlignedBox();
		this.displayList.getBounds(aabb);
		dlBounds = new VertexArrayGeometry(new Box(aabb.getMin(), aabb.getMax()));
		renderer.requestUpdate(dlBounds, true);
		dlBoundsShape = new Shape(dlBounds, new Appearance(new Material(new Color(1f, 0f, 0f)), new PolygonStyle(DrawStyle.LINE, DrawStyle.NONE)));
		dlBoundsShape.setCullMode(CullMode.NEVER);
		root.add(dlBoundsShape);
		
		root.add(new Shape(this.displayList, null));
		
		this.window.setVSyncEnabled(true);
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
		FogReceiver fr = null;
		if (i % 2 == 0) {
			lr = new LightReceiver();
			lr.setSeparateSpecular(true);
			m.setSmoothShaded(true);
		} {
			fr = new FogReceiver();
			fr.setFogCoordinateSource(FogCoordSource.FRAGMENT_DEPTH);
		}
		
		Texture t = null;
		
		if (i < max / 2) {
			try {
				TextureImage image = TextureLoader.readTexture(this.getClass().getClassLoader().getResource("data/textures/squiggles.tga"));
				renderer.requestUpdate(image, false);
				t = new Texture(image, EnvMode.MODULATE, new Color(), TexCoordGen.NONE);
			} catch (IOException io) {
				throw new RuntimeException(io);
			}
		} else {
			try {
				TextureImage image = TextureLoader.readTexture(this.getClass().getClassLoader().getResource("data/textures/grace_cube.dds"));
				renderer.requestUpdate(image, false);
				t = new Texture(image, EnvMode.MODULATE, new Color(), TexCoordGen.OBJECT);
			} catch (IOException io) {
				throw new RuntimeException(io);
			}
		}
		
		return new Appearance(m, lr, t, fr);
	}
}
