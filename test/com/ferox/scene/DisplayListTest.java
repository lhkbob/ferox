package com.ferox.scene;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.openmali.vecmath.Vector3f;

import com.ferox.BasicApplication;
import com.ferox.math.BoundSphere;
import com.ferox.math.BoundVolume;
import com.ferox.math.Color;
import com.ferox.math.Transform;
import com.ferox.renderer.RenderAtom;
import com.ferox.renderer.RenderQueue;
import com.ferox.renderer.Renderer;
import com.ferox.renderer.util.RenderQueueDataCache;
import com.ferox.resource.Geometry;
import com.ferox.resource.geometry.Box;
import com.ferox.resource.geometry.VertexArrayGeometry;
import com.ferox.resource.geometry.VertexBufferGeometry;
import com.ferox.resource.texture.TextureImage;
import com.ferox.resource.texture.loader.TextureLoader;
import com.ferox.scene.Fog.FogEquation;
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
	public static final boolean DEBUG = true;
	public static final boolean USE_VBO = true;
	public static final boolean RANDOM_PLACEMENT = true;
	
	public static final int NUM_CUBES = 10000;
	public static final int BOUNDS = 100;
	
	public static final Color bgColor = new Color(0f, 0f, 0f);
	
	protected Geometry displayList;
	
	public static void main(String[] args) {
		new DisplayListTest(DEBUG).run();
	}
	
	public DisplayListTest(boolean debug) {
		super(debug);
	}

	@Override
	protected SceneElement buildScene(Renderer renderer, ViewNode view) {
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
		
		Geometry cube;
		if (USE_VBO)
			cube = new VertexBufferGeometry(new Box(2f).requestVboUpdate(renderer, true));
		else
			cube = new VertexArrayGeometry(new Box(2f));
		renderer.requestUpdate(cube, true);
		renderer.flushRenderer(null);
		
		// vars for regular gridding
		int sideCubeCount = (int) (Math.ceil(Math.pow(NUM_CUBES, 1.0 / 3.0)));
		float scale = BOUNDS / (float) sideCubeCount;
		int x = 0;
		int y = 0;
		int z = 0;
		
		List<RenderAtom> compiledList = new ArrayList<RenderAtom>();
		for (int i = 0; i < NUM_CUBES; i++) {
			Vector3f pos = new Vector3f();
			
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
			compiledList.add(new DisplayListRenderAtom(cube, apps[(int) ((float) i / NUM_CUBES * apps.length)], pos));
		}
		this.displayList = renderer.compile(compiledList);
		
		root.add(new Shape(this.displayList, null));
		
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
	
	private static class DisplayListRenderAtom implements RenderAtom {
		private Appearance apperance;
		private BoundVolume bounds;
		private Geometry geometry;
		private Transform transform;
		private RenderQueueDataCache cache;
		
		public DisplayListRenderAtom(Geometry geom, Appearance app, Vector3f location) {
			this.geometry = geom;
			this.apperance = app;
			
			this.bounds = new BoundSphere();
			geom.getBounds(this.bounds);
			
			this.transform = new Transform(location);
			this.cache = new RenderQueueDataCache();
		}
		
		@Override
		public Appearance getAppearance() {
			return this.apperance;
		}

		@Override
		public BoundVolume getBounds() {
			return this.bounds;
		}

		@Override
		public Geometry getGeometry() {
			return this.geometry;
		}

		@Override
		public Object getRenderQueueData(RenderQueue pipe) {
			return this.cache.getRenderQueueData(pipe);
		}

		@Override
		public Transform getTransform() {
			return this.transform;
		}

		@Override
		public void setRenderQueueData(RenderQueue pipe, Object data) {
			this.cache.setRenderQueueData(pipe, this);
		}
	}
}
