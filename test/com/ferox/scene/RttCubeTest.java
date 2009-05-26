package com.ferox.scene;

import java.io.IOException;

import org.openmali.vecmath.Vector3f;

import com.ferox.BasicApplication;
import com.ferox.effect.DepthTest;
import com.ferox.effect.GlobalLighting;
import com.ferox.effect.Material;
import com.ferox.effect.PolygonStyle;
import com.ferox.effect.Texture;
import com.ferox.effect.Effect.PixelTest;
import com.ferox.effect.Effect.Quality;
import com.ferox.effect.Fog.FogEquation;
import com.ferox.effect.PolygonStyle.DrawStyle;
import com.ferox.effect.Texture.EnvMode;
import com.ferox.effect.Texture.TexCoordGen;
import com.ferox.math.Color;
import com.ferox.renderer.DisplayOptions;
import com.ferox.renderer.Framework;
import com.ferox.renderer.TextureSurface;
import com.ferox.renderer.View;
import com.ferox.renderer.DisplayOptions.DepthFormat;
import com.ferox.renderer.DisplayOptions.PixelFormat;
import com.ferox.resource.Geometry;
import com.ferox.resource.TextureImage;
import com.ferox.resource.Geometry.CompileType;
import com.ferox.resource.TextureImage.TextureTarget;
import com.ferox.util.geom.Box;
import com.ferox.util.geom.Rectangle;
import com.ferox.util.texture.loader.TextureLoader;

public class RttCubeTest extends BasicApplication {
	public static final boolean DEBUG = false;
	public static final boolean USE_VBO = true;
	public static final boolean RANDOM_PLACEMENT = true;

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
	protected Node buildScene(Framework renderer, ViewNode view) {
		this.window.setClearColor(bgColor);
		view.getView().setPerspective(60f,
						window.getWidth() / (float) window.getHeight(), 1f,
						300f);
		view.getLocalTransform().getTranslation().z = 2f * BOUNDS;

		Group root = new Group();
		root.add(view);

		SpotLightNode spotLight = new SpotLightNode();
		spotLight.setEffectRadius(BOUNDS);
		view.add(spotLight);

		DirectionLightNode directionLight = new DirectionLightNode(new Vector3f(-1f, -1f, -1f));
		directionLight.setEffectRadius(BOUNDS);
		root.add(directionLight);

		FogNode fog = new FogNode(bgColor, 10f, 300f, 1f, FogEquation.LINEAR,
						Quality.BEST);
		fog.setEffectRadius(BOUNDS);
		root.add(fog);

		Appearance[] apps = createAppearances(renderer);
		PolygonStyle ps = new PolygonStyle(DrawStyle.SOLID, DrawStyle.NONE);
		for (Appearance a : apps) {
			a.setPolygonStyle(ps);
		}
		
		geom = new Box(2f, (USE_VBO ? CompileType.VBO_STATIC : CompileType.VERTEX_ARRAY));
		renderer.requestUpdate(geom, true);

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

		sceneDepth = setupSceneDepthSurface(renderer, root, view.getView());
		return root;
	}

	@Override
	public boolean render(Framework renderer) {
		renderer.queueRender(sceneDepth);
		return super.render(renderer);
	}

	private Geometry buildSquare(Framework renderer, float left, float right,
					float bottom, float top) {
		Rectangle sq = new Rectangle(left, right, bottom, top);
		renderer.requestUpdate(sq, true);
		return sq;
	}

	private TextureSurface setupSceneDepthSurface(Framework renderer,
					Node root, View view) {
		TextureSurface sceneDepth = renderer.createTextureSurface(
						new DisplayOptions(PixelFormat.RGB_24BIT,
										DepthFormat.DEPTH_24BIT),
						TextureTarget.T_2D, window.getWidth(), window
										.getHeight(), 1, 0, 0, false);
		sceneDepth.setClearColor(bgColor);

		SceneRenderPass depthPass = new SceneRenderPass(root, view);
		sceneDepth.addRenderPass(depthPass);

		// this pass will display the color and depth textures in the window
		TextureImage depthTex = sceneDepth.getDepthBuffer();
		TextureImage colorTex = sceneDepth.getColorBuffer(0);

		DepthTest dt = new DepthTest();
		dt.setTest(PixelTest.ALWAYS);

		Shape depthShape = new Shape(buildSquare(renderer,
						window.getWidth() - 128, window.getWidth(), 0, 128),
						new Appearance().setDepthTest(dt).setTextures(new Texture(depthTex)));
		Shape colorShape = new Shape(buildSquare(renderer, 0,
						window.getWidth(), 0, window.getHeight()),
						new Appearance().setDepthTest(dt).setTextures(new Texture(colorTex)));

		View ortho = new View();
		ortho.setOrthogonalProjection(true);
		ortho.setFrustum(0, window.getWidth(), 0, window.getHeight(), -1, 1);

		window.removeRenderPass(pass);
		window.addRenderPass(new SceneRenderPass(colorShape, ortho));
		window.addRenderPass(new SceneRenderPass(depthShape, ortho));

		window.setColorBufferCleared(false);
		window.setDepthBufferCleared(false);

		return sceneDepth;
	}

	private Appearance[] createAppearances(Framework renderer) {
		Appearance[] apps = new Appearance[4];
		for (int i = 0; i < apps.length; i++) {
			apps[i] = createAppearance(renderer, i, apps.length);
		}
		return apps;
	}

	private Appearance createAppearance(Framework renderer, int i, int max) {
		float percent = (float) i / max;
		
		Appearance a = new Appearance();
		
		if (percent < .3333f) {
			a.setMaterial(new Material(new Color(1f, percent, percent)));
		} else if (percent < .666667f) {
			a.setMaterial(new Material(new Color(percent / 2f, 1f, percent / 2f)));
		} else {
			a.setMaterial(new Material(new Color(percent / 3f, percent / 3f, 1f)));
		}

		if (i % 2 == 0) {
			a.setGlobalLighting(new GlobalLighting()).getGlobalLighting().setSeparateSpecular(true);
			a.getMaterial().setSmoothShaded(true);
		}

		Texture t = null;

		if (i < max / 2) {
			try {
				TextureImage image = TextureLoader.readTexture(this.getClass()
								.getClassLoader().getResource(
												"data/textures/squiggles.tga"));
				renderer.requestUpdate(image, false);
				t = new Texture(image, EnvMode.MODULATE, new Color(),
								TexCoordGen.NONE);
			} catch (IOException io) {
				throw new RuntimeException(io);
			}
		} else {
			try {
				TextureImage image = TextureLoader
								.readTexture(this
												.getClass()
												.getClassLoader()
												.getResource(
																"data/textures/grace_cube.dds"));
				renderer.requestUpdate(image, false);
				t = new Texture(image, EnvMode.MODULATE, new Color(),
								TexCoordGen.OBJECT);
			} catch (IOException io) {
				throw new RuntimeException(io);
			}
		}
		a.setTextures(t);
		a.setFogEnabled(true);

		return a;
	}
}
