package com.ferox.scene;

import java.io.IOException;

import com.ferox.BasicApplication;
import com.ferox.effect.Effect.Quality;
import com.ferox.effect.Fog.FogEquation;
import com.ferox.math.Color4f;
import com.ferox.math.Vector3f;
import com.ferox.renderer.Framework;
import com.ferox.resource.Geometry;
import com.ferox.resource.TextureImage;
import com.ferox.resource.Geometry.CompileType;
import com.ferox.scene.fx.Appearance;
import com.ferox.scene.fx.GeometryProfile;
import com.ferox.scene.fx.LightingModel;
import com.ferox.scene.fx.PhongLightingModel;
import com.ferox.scene.fx.SceneCompositor;
import com.ferox.scene.fx.SceneCompositorFactory;
import com.ferox.scene.fx.ShadowCaster;
import com.ferox.scene.fx.SolidLightingModel;
import com.ferox.scene.fx.TextureUnit;
import com.ferox.scene.fx.TexturedMaterial;
import com.ferox.scene.fx.SceneCompositorFactory.CommonGeometryProfile;
import com.ferox.scene.fx.impl.fixed.FixedFunctionSceneCompositor;
import com.ferox.util.geom.Box;
import com.ferox.util.texture.loader.TextureLoader;

public class FixedFunctionTest extends BasicApplication {
	public static final boolean DEBUG = false;
	public static final boolean USE_VBO = true;
	public static final boolean RANDOM_PLACEMENT = true;

	public static final int NUM_CUBES = 10000;
	public static final int BOUNDS = 100;

	public static final Color4f bgColor = new Color4f(.5f, .5f, .5f);
	
	public static void main(String[] args) {
		new FixedFunctionTest(DEBUG).run();
	}
	
	public FixedFunctionTest(boolean debug) {
		super(debug);
	}

	@Override
	protected SceneCompositor buildScene(Framework renderer, ViewNode view) {
		window.setClearColor(bgColor);
		view.getLocalTransform().getTranslation().z = 2f * BOUNDS;
		view.setDirty();
		
		Scene scene = new Scene();
		scene.add(view);
		// FIXME: add an octree cell once implemented
		
		FixedFunctionSceneCompositor sc = new FixedFunctionSceneCompositor();
		sc.setShadowsEnabled(false);
		sc.attach(window, view.getView());
		GeometryProfile gp = new GeometryProfile();
		gp.setTextureUnitBound(0, true);
		sc.initialize(scene, gp);
		
		// add in lights and fog
		SpotLightNode spotLight = new SpotLightNode();
		spotLight.setEffectRadius(BOUNDS);
		spotLight.setController(new AttachController(view));
		scene.add(spotLight); 

		DirectionLightNode directionLight = new DirectionLightNode(new Vector3f(-1f, -1f, -1f));
		directionLight.setEffectRadius(BOUNDS);
		directionLight.setShadowCaster(true);
		scene.add(directionLight);
		
		FogNode fog = new FogNode(bgColor, 10f, 300f, 1f, FogEquation.LINEAR, Quality.BEST);
		fog.setEffectRadius(BOUNDS);
		scene.add(fog);
		
		// vars for regular gridding
		int sideCubeCount = (int) (Math.ceil(Math.pow(NUM_CUBES, 1.0 / 3.0)));
		float scale = BOUNDS / (float) sideCubeCount;
		int x = 0;
		int y = 0;
		int z = 0;
		
		// add NUM_CUBES cubes
		Geometry box = new Box(2f, (USE_VBO ? CompileType.VBO_STATIC : CompileType.VERTEX_ARRAY));
		renderer.requestUpdate(box, true);
		Appearance[] apps = createAppearances(sc, renderer);
		for (int i = 0; i < NUM_CUBES; i++) {
			Shape cube = new Shape(box, apps[i % apps.length]);

			if (RANDOM_PLACEMENT) {
				if (i != 0) {
					// randomly place all but the 1st cube
					cube.setTranslation((float) (Math.random() * BOUNDS - BOUNDS / 2f),
										(float) (Math.random() * BOUNDS - BOUNDS / 2f), 
										(float) (Math.random() * BOUNDS - BOUNDS / 2f));
				} else
					cube.setTranslation(0f, 0f, 0f);
			} else {
				cube.setTranslation(scale * x - BOUNDS / 2f, 
								    scale * y - BOUNDS / 2f, 
								    scale * z - BOUNDS / 2f);
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
			
			scene.add(cube);
		}
		
		return sc;
	}

	private Appearance[] createAppearances(SceneCompositor sc, Framework renderer) {
		Appearance[] apps = new Appearance[4];
		for (int i = 0; i < apps.length; i++) {
			apps[i] = createAppearance(renderer, i, apps.length);
			sc.compile(apps[i]);
		}
		return apps;
	}

	private Appearance createAppearance(Framework renderer, int i, int max) {
		float percent = (float) i / max;
		
		Color4f color;
		LightingModel lm;
		
		if (percent < .3333f) {
			color = new Color4f(1f, percent, percent);
		} else if (percent < .666667f) {
			color = new Color4f(percent / 2f, 1f, percent / 2f);
		} else {
			color = new Color4f(percent / 3f, percent / 3f, 1f);
		}

		if (i % 2 == 0)
			lm = new PhongLightingModel(color, false);
		else
			lm = new SolidLightingModel(color, false);

		TextureImage image = null;
		try {
			if (i < max / 2)
				image = TextureLoader.readTexture(this.getClass().getClassLoader().getResource("data/textures/squiggles.tga"));
			else
				image = TextureLoader.readTexture(this.getClass().getClassLoader().getResource("data/textures/wall_diffuse.png"));
			renderer.requestUpdate(image, true);
		} catch (IOException io) {
			throw new RuntimeException(io);
		}
		
		TexturedMaterial tm = new TexturedMaterial(new TextureUnit(image, 0));
		return new Appearance(lm, tm, new ShadowCaster());
	}
}
