package com.ferox.renderer.impl.jogl;

import com.ferox.math.Color4f;
import com.ferox.math.Vector3f;
import com.ferox.math.bounds.AxisAlignedBox;
import com.ferox.math.bounds.SimpleSpatialHierarchy;
import com.ferox.math.bounds.SpatialHierarchy;
import com.ferox.renderer.DisplayOptions;
import com.ferox.renderer.FrameStatistics;
import com.ferox.renderer.Framework;
import com.ferox.renderer.OnscreenSurface;
import com.ferox.renderer.RenderThreadingOrganizer;
import com.ferox.renderer.DisplayOptions.AntiAliasMode;
import com.ferox.renderer.DisplayOptions.PixelFormat;
import com.ferox.renderer.Renderer.DrawStyle;
import com.ferox.resource.Geometry.CompileType;
import com.ferox.scene.AmbientLight;
import com.ferox.scene.BlinnPhongLightingModel;
import com.ferox.scene.DirectionLight;
import com.ferox.scene.Renderable;
import com.ferox.scene.SceneElement;
import com.ferox.scene.ShadowCaster;
import com.ferox.scene.ShadowReceiver;
import com.ferox.scene.Shape;
import com.ferox.scene.SpotLight;
import com.ferox.scene.ViewNode;
import com.ferox.scene.controller.LightUpdateController;
import com.ferox.scene.controller.SceneController;
import com.ferox.scene.controller.ViewNodeController;
import com.ferox.scene.ffp.FixedFunctionRenderController;
import com.ferox.scene.ffp.ShadowMapFrustumController;
import com.ferox.util.entity.Entity;
import com.ferox.util.entity.EntitySystem;
import com.ferox.util.geom.Box;
import com.ferox.util.geom.PrimitiveGeometry;
import com.ferox.util.geom.Rectangle;
import com.ferox.util.geom.Sphere;
import com.ferox.util.geom.Teapot;

public class FixedFunctionSceneCompositorTest {
	private static final boolean USE_VBOS = true;
	private static final int BOUNDS = 70;
	private static final int NUM_SHAPES = 1000;
	
	public static void main(String[] args) {
		Framework framework = new FixedFunctionJoglFramework(false);
		RenderThreadingOrganizer organizer = new RenderThreadingOrganizer(framework);
		
		EntitySystem system = new EntitySystem();
		SpatialHierarchy<Entity> sh = new SimpleSpatialHierarchy<Entity>();
		
		SceneController c1 = new SceneController(sh);
		LightUpdateController c2 = new LightUpdateController();
		ViewNodeController c3 = new ViewNodeController(sh);
		ShadowMapFrustumController c4 = new ShadowMapFrustumController(sh, .1f, 1024);
		FixedFunctionRenderController c5 = new FixedFunctionRenderController(organizer, 1024);
		
		buildScene(system);
		
		OnscreenSurface surface = buildSurface(framework, system);
		organizer.setSurfaceGroup(surface, "ffp_sct");
		
		try {
			Runtime r = Runtime.getRuntime();
			while(true) {
				if (surface.isDestroyed())
					break;

				long now = -System.nanoTime();

				c1.process(system);
				c2.process(system);
				c3.process(system);
				c4.process(system);
				c5.process(system);

				organizer.flush("ffp_sct");
				FrameStatistics stats = framework.renderAndWait();
				now += System.nanoTime();

				surface.setTitle(String.format("Polys: %d, FPS: %.2f (%d of %d), Mem: %.2f", stats.getPolygonCount(), 1e9f / now, (int) (stats.getRenderTime() / 1e6f), (int) (now / 1e6f), (((r.totalMemory() - r.freeMemory()) / (1024f * 1024f)))));
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
		framework.destroy();
	}
	
	private static OnscreenSurface buildSurface(Framework framework, EntitySystem system) {
		DisplayOptions options = new DisplayOptions(PixelFormat.RGB_24BIT, AntiAliasMode.EIGHT_X);
		OnscreenSurface surface = framework.createWindowSurface(options, 0, 0, 800, 600, true, false);
		
		// camera
		ViewNode vn = new ViewNode(surface, 60f, 1f, 3 * BOUNDS);
		SceneElement el = new SceneElement();
		el.getTransform().setTranslation(0f, 0f, -1.5f * BOUNDS);
		
		system.add(new Entity(el, vn));
		surface.setClearColor(new Color4f(.5f, .5f, .5f, 1f));
		return surface;
	}
	
	private static void buildScene(EntitySystem scene) {
		// shapes
		PrimitiveGeometry geom = new Box(2f, (USE_VBOS ? CompileType.RESIDENT_STATIC : CompileType.NONE));
		PrimitiveGeometry geom2 = new Sphere(4f, 32, (USE_VBOS ? CompileType.RESIDENT_STATIC : CompileType.NONE));
		PrimitiveGeometry geom3 = new Teapot(2f, (USE_VBOS ? CompileType.RESIDENT_STATIC : CompileType.NONE));
		
		AxisAlignedBox bounds1 = new AxisAlignedBox(geom.getVertices().getData());
		AxisAlignedBox bounds2 = new AxisAlignedBox(geom2.getVertices().getData());
		AxisAlignedBox bounds3 = new AxisAlignedBox(geom3.getVertices().getData());

		Renderable toRender = new Renderable();
		
		Shape shape1 = new Shape(geom);
		Shape shape2 = new Shape(geom2);
		Shape shape3 = new Shape(geom3);
		
		ShadowCaster sc = new ShadowCaster();
		ShadowReceiver sr = new ShadowReceiver();
		
		BlinnPhongLightingModel material = new BlinnPhongLightingModel(new Color4f(1f, 1f, 1f), new Color4f(.2f, 0f, .1f));
		
		for (int i = 0; i < NUM_SHAPES; i++) {
			float x = (float) (Math.random() * BOUNDS - BOUNDS / 2);
			float y = (float) (Math.random() * BOUNDS - BOUNDS / 2);
			float z = (float) (Math.random() * BOUNDS - BOUNDS / 2);
			
			int choice = (int) (Math.random() * 3 + 1);
			
			SceneElement element = new SceneElement();
			element.getTransform().setTranslation(x, y, z);
			
			Entity e = new Entity(element, toRender, material, sc, sr);
			
			switch(choice) {
			case 1:
				e.add(shape1);
				element.setLocalBounds(bounds1);
				break;
			case 2:
				e.add(shape2);
				element.setLocalBounds(bounds2);
				break;
			case 3:
				e.add(shape3);
				element.setLocalBounds(bounds3);
				break;
			}
			
			scene.add(e);
		}
		
		// some walls
		Rectangle backWall = new Rectangle(new Vector3f(0f, 1f, 0f), new Vector3f(0f, 0f, 1f), -BOUNDS, BOUNDS, -BOUNDS, BOUNDS);
		SceneElement pos = new SceneElement();
		pos.getTransform().setTranslation(-BOUNDS, 0f, 0f);
		pos.setLocalBounds(new AxisAlignedBox(backWall.getVertices().getData()));
		
		scene.add(new Entity(pos, new Shape(backWall), material, new Renderable(DrawStyle.SOLID, DrawStyle.SOLID), sr));
		
		Rectangle bottomWall = new Rectangle(new Vector3f(-1f, 0f, 0f), new Vector3f(0f, 0f, 1f), -BOUNDS, BOUNDS, -BOUNDS, BOUNDS);
		pos = new SceneElement();
		pos.getTransform().setTranslation(0f, -BOUNDS, 0f);
		pos.setLocalBounds(new AxisAlignedBox(bottomWall.getVertices().getData()));
		scene.add(new Entity(pos, new Shape(bottomWall), material, new Renderable(DrawStyle.SOLID, DrawStyle.SOLID), sr));

		// ambient light
		scene.add(new Entity(new AmbientLight(new Color4f(.2f, .2f, .2f, 1f))));
		
		// a point light
		scene.add(new Entity(new SpotLight(new Color4f(.5f, .8f, 0f), 
										   new Vector3f(-BOUNDS / 2f, 0f, -BOUNDS))));
		
		// a directed light, which casts shadows
		scene.add(new Entity(new DirectionLight(new Color4f(1f, 1f, 1f),
												new Vector3f(-1f, -1f, 1f).normalize()),
							 sc));
	}
}
