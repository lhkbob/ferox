package com.ferox.scene;

import java.io.IOException;

import org.openmali.vecmath.Vector3f;

import com.ferox.BasicApplication;
import com.ferox.math.BoundSphere;
import com.ferox.math.Color;
import com.ferox.renderer.DisplayOptions;
import com.ferox.renderer.Renderer;
import com.ferox.renderer.TextureSurface;
import com.ferox.renderer.View;
import com.ferox.renderer.DisplayOptions.DepthFormat;
import com.ferox.renderer.DisplayOptions.PixelFormat;
import com.ferox.renderer.util.BasicRenderPass;
import com.ferox.resource.Geometry;
import com.ferox.resource.geometry.Box;
import com.ferox.resource.geometry.Square;
import com.ferox.resource.geometry.VertexArrayGeometry;
import com.ferox.resource.geometry.VertexBufferGeometry;
import com.ferox.resource.texture.TextureImage;
import com.ferox.resource.texture.TextureImage.TextureTarget;
import com.ferox.resource.texture.loader.TextureLoader;
import com.ferox.scene.Fog.FogEquation;
import com.ferox.state.Appearance;
import com.ferox.state.DepthTest;
import com.ferox.state.FogReceiver;
import com.ferox.state.LightReceiver;
import com.ferox.state.Material;
import com.ferox.state.PolygonStyle;
import com.ferox.state.Texture;
import com.ferox.state.FogReceiver.FogCoordSource;
import com.ferox.state.PolygonStyle.DrawStyle;
import com.ferox.state.State.PixelTest;
import com.ferox.state.State.Quality;
import com.ferox.state.Texture.EnvMode;
import com.ferox.state.Texture.TexCoordGen;

public class RttCubeTest extends BasicApplication {
	public static final boolean DEBUG = false;
	public static final boolean USE_VBO = false;
	public static final boolean RANDOM_PLACEMENT = false;
	
	public static final int NUM_CUBES = 10000;
	public static final int BOUNDS = 100;
	
	public static final Color bgColor = new Color(.5f, .5f, .5f);
	
	protected Geometry geom;
	protected TextureSurface sceneDepth;
	
	public static void main(String[] args) {
		new RttCubeTest(DEBUG).run();
	}
	
	public RttCubeTest(boolean debug) {
		super(debug);
	}

	@Override
	protected SceneElement buildScene(Renderer renderer, ViewNode view) {
		//this.window.setClearColor(bgColor);
		view.getView().setPerspective(60f, this.window.getWidth() / (float) this.window.getHeight(), 1f, 300f);
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
		
		Fog fog = new Fog(bgColor, 10f, 300f, 1f, FogEquation.LINEAR, Quality.BEST);
		fog.setLocalBounds(new BoundSphere(BOUNDS));
		root.add(fog);
		
		Appearance[] apps = this.createAppearances(renderer);
		PolygonStyle ps = new PolygonStyle();
		ps.setFrontStyle(DrawStyle.SOLID);
		ps.setBackStyle(DrawStyle.NONE);
		for (Appearance a: apps)
			a.addState(ps);
		
		if (USE_VBO)
			this.geom = new VertexBufferGeometry(new Box(2f).requestVboUpdate(renderer, true));
		else
			this.geom = new VertexArrayGeometry(new Box(2f));
		renderer.requestUpdate(this.geom, true);
		
		// vars for regular gridding
		int sideCubeCount = (int) (Math.ceil(Math.pow(NUM_CUBES, 1.0 / 3.0)));
		float scale = BOUNDS / (float) sideCubeCount;
		int x = 0;
		int y = 0;
		int z = 0;
		
		for (int i = 0; i < NUM_CUBES; i++) {
			Shape shape = new Shape(geom, apps[i % apps.length]);
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
		
		this.sceneDepth = this.setupSceneDepthSurface(renderer, root, view.getView());
		return root;
	}
	
	@Override
	public boolean render(Renderer renderer) {
		renderer.queueRender(this.sceneDepth);
		return super.render(renderer);
	}
	
	private Geometry buildSquare(Renderer renderer, float left, float right, float bottom, float top) {
		Square sq = new Square(left, right, bottom, top);
		
		if (USE_VBO) {
			VertexBufferGeometry vbo = new VertexBufferGeometry(sq.requestVboUpdate(renderer, true));
			renderer.requestUpdate(vbo, true);
			return vbo;
		} else {
			VertexArrayGeometry var = new VertexArrayGeometry(sq);
			renderer.requestUpdate(var, true);
			return var;
		}		
	}
	
	private TextureSurface setupSceneDepthSurface(Renderer renderer, SceneElement root, View view) {
		TextureSurface sceneDepth = renderer.createTextureSurface(new DisplayOptions(PixelFormat.RGB_24BIT, DepthFormat.DEPTH_24BIT), TextureTarget.T_2D, 
																				     this.window.getWidth(), this.window.getHeight(), 1, 0, 0, false);
		sceneDepth.setClearColor(bgColor);
		
		BasicRenderPass depthPass = new BasicRenderPass(root, view);		
		sceneDepth.addRenderPass(depthPass);
		
		// this pass will display the color and depth textures in the window
		TextureImage depthTex = sceneDepth.getDepthBuffer();	
		TextureImage colorTex = sceneDepth.getColorBuffer(0);
		
		DepthTest dt = new DepthTest();
		dt.setTest(PixelTest.ALWAYS);
		
		Shape depthShape = new Shape(this.buildSquare(renderer, this.window.getWidth() - 128, this.window.getWidth(), 0, 128), new Appearance(new Texture(depthTex), dt));
		Shape colorShape = new Shape(this.buildSquare(renderer, 0, this.window.getWidth(), 0, this.window.getHeight()), 
													  new Appearance(new Texture(colorTex), dt));	
		
		View ortho = new View();
		ortho.setOrthogonalProjection(true);
		ortho.setFrustum(0, this.window.getWidth(), 0, this.window.getHeight(), -1, 1);
		
		this.window.removeRenderPass(this.pass);
		this.window.addRenderPass(new BasicRenderPass(colorShape, ortho));
		this.window.addRenderPass(new BasicRenderPass(depthShape, ortho));
		
		return sceneDepth;
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
