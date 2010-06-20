package com.ferox.renderer.impl.jogl;

import com.ferox.entity.Component;
import com.ferox.entity.Entity;
import com.ferox.entity.EntitySystem;
import com.ferox.math.Color4f;
import com.ferox.math.Vector3f;
import com.ferox.math.bounds.AxisAlignedBox;
import com.ferox.math.bounds.Octree;
import com.ferox.math.bounds.SpatialHierarchy;
import com.ferox.renderer.FrameStatistics;
import com.ferox.renderer.Framework;
import com.ferox.renderer.OnscreenSurface;
import com.ferox.renderer.OnscreenSurfaceOptions;
import com.ferox.renderer.ThreadQueueManager;
import com.ferox.renderer.OnscreenSurfaceOptions.AntiAliasMode;
import com.ferox.renderer.OnscreenSurfaceOptions.PixelFormat;
import com.ferox.renderer.Renderer.DrawStyle;
import com.ferox.resource.Geometry.CompileType;
import com.ferox.scene.AmbientLight;
import com.ferox.scene.Billboarded;
import com.ferox.scene.BlinnPhongLightingModel;
import com.ferox.scene.DirectionLight;
import com.ferox.scene.Renderable;
import com.ferox.scene.SceneElement;
import com.ferox.scene.ShadowCaster;
import com.ferox.scene.ShadowReceiver;
import com.ferox.scene.Shape;
import com.ferox.scene.SpotLight;
import com.ferox.scene.ViewNode;
import com.ferox.scene.Billboarded.Axis;
import com.ferox.scene.controller.BillboardController;
import com.ferox.scene.controller.LightUpdateController;
import com.ferox.scene.controller.SceneController;
import com.ferox.scene.controller.ViewNodeController;
import com.ferox.scene.controller.ffp.FixedFunctionRenderController;
import com.ferox.scene.controller.ffp.ShadowMapFrustumController;
import com.ferox.util.geom.Box;
import com.ferox.util.geom.PrimitiveGeometry;
import com.ferox.util.geom.Rectangle;
import com.ferox.util.geom.Sphere;
import com.ferox.util.geom.Teapot;

public class FixedFunctionSceneCompositorTest {
	private static final boolean USE_VBOS = true;
	private static final int BOUNDS = 70;
	private static final int NUM_SHAPES = 1000;
	
	public static void main(String[] args) throws Exception {
		Framework framework = new FixedFunctionJoglFramework(true);
		ThreadQueueManager organizer = new ThreadQueueManager(framework);
		
		EntitySystem system = new EntitySystem();
		SpatialHierarchy<Entity> sh = new Octree<Entity>(new AxisAlignedBox(new Vector3f(-BOUNDS, -BOUNDS, -BOUNDS), 
																			new Vector3f(BOUNDS, BOUNDS, BOUNDS)));
		
		BillboardController c0 = new BillboardController(system);
		SceneController c1 = new SceneController(system, sh);
		LightUpdateController c2 = new LightUpdateController(system);
		ViewNodeController c3 = new ViewNodeController(system, sh);
		// FIXME: make this sizing/placement of shadowmap better and more intuitive
		ShadowMapFrustumController c4 = new ShadowMapFrustumController(system, sh, .075f, 1024);
		FixedFunctionRenderController c5 = new FixedFunctionRenderController(system, organizer, 2048);
		
		buildScene(system);
		
		OnscreenSurface surface = buildSurface(framework, system);
		organizer.setSurfaceGroup(surface, "ffp_sct");
		
		try {
			Runtime r = Runtime.getRuntime();
			
			// scene element controlling the viewnode
			SceneElement vse = system.iterator(Component.getComponentId(ViewNode.class)).next().get(Component.getComponentId(SceneElement.class));
			float radius = vse.getTransform().getTranslation().length();
			
			float phi = 0;
			float theta = 0;
			
			long start = System.nanoTime();
			long renderTime = 0;
			int framesCompleted = 0;
			
			int numFramesPerUpdate = 10;
			while(true) {
				if (surface.isDestroyed())
					break;
 				
				// animate the viewnode
				vse.getTransform().setTranslation((float) (radius * Math.cos(theta) * Math.sin(phi)), 
												  (float) (radius * Math.sin(theta) * Math.sin(phi)), 
												  (float) (radius * Math.cos(phi)));
				theta += .007;
				phi += .007;
				
				c0.processWithProfile();
				c1.processWithProfile();
				c2.processWithProfile();
				c3.processWithProfile();
				c4.processWithProfile();
				c5.processWithProfile();
				
				// begin rendering the frame
				organizer.flush("ffp_sct");
				FrameStatistics stats = framework.renderAndWait();
				renderTime += stats.getRenderTime();
				framesCompleted++;
				
				if (framesCompleted >= numFramesPerUpdate) {
					// update statistics
					long now = System.nanoTime();
					float fps = numFramesPerUpdate * 1e9f / (now - start);
					int timeInRender = (int) (renderTime / 1e6f) / numFramesPerUpdate;
					int timeInProcess = (int) ((now - start) / 1e6f) / numFramesPerUpdate - timeInRender;
					
					surface.setTitle(String.format("Polys: %d, FPS: %.2f (%d + %d), Mem: %.2f", 
												   stats.getPolygonCount(), fps, timeInRender, timeInProcess, 
												   (r.totalMemory() - r.freeMemory()) / (1024f * 1024f)));
					
					// reset counters
					start = now;
					framesCompleted = 0;
					renderTime = 0;
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
		framework.destroy();
	}
	
	private static OnscreenSurface buildSurface(Framework framework, EntitySystem system) {
	    OnscreenSurfaceOptions options = new OnscreenSurfaceOptions().setPixelFormat(PixelFormat.RGB_24BIT)
	                                                                 .setAntiAliasMode(AntiAliasMode.EIGHT_X)
	                                                                 .setWidth(800)
	                                                                 .setHeight(600)
	                                                                 .setUndecorated(false)
	                                                                 .setResizable(true);
	    OnscreenSurface surface = framework.createSurface(options);
		surface.setClearColor(new Color4f(.5f, .5f, .5f, 1f));
//		surface.setVSyncEnabled(true);

		// camera
		ViewNode vn = new ViewNode(surface, 60f, 1f, 3 * BOUNDS);
		SceneElement el = new SceneElement();
		el.getTransform().setTranslation(0f, 0f, 1f * BOUNDS);
		
		Billboarded bb = new Billboarded();
		bb.setBillboardPoint(new Vector3f(), Axis.Z);
		
		system.add(new Entity(el, vn, bb));
		return surface;
	}
	
	private static void buildScene(EntitySystem scene) throws Exception {
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
		
		//TexturedMaterial texture = new TexturedMaterial(TextureLoader.readTexture(new File("ferox-gl.png")));
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
		Rectangle backWall = new Rectangle(new Vector3f(0f, 1f, 0f), new Vector3f(0f, 0f, -1f), -BOUNDS, BOUNDS, -BOUNDS, BOUNDS);
		SceneElement pos = new SceneElement();
		pos.getTransform().setTranslation(BOUNDS, 0f, 0f);
		pos.setLocalBounds(new AxisAlignedBox(backWall.getVertices().getData()));
		
		scene.add(new Entity(pos, new Shape(backWall), material, new Renderable(DrawStyle.SOLID, DrawStyle.SOLID), sr));
		
		Rectangle bottomWall = new Rectangle(new Vector3f(1f, 0f, 0f), new Vector3f(0f, 0f, -1f), -BOUNDS, BOUNDS, -BOUNDS, BOUNDS);
		pos = new SceneElement();
		pos.getTransform().setTranslation(0f, -BOUNDS, 0f);
		pos.setLocalBounds(new AxisAlignedBox(bottomWall.getVertices().getData()));
		scene.add(new Entity(pos, new Shape(bottomWall), material, new Renderable(DrawStyle.SOLID, DrawStyle.SOLID), sr));

		// ambient light
		scene.add(new Entity(new AmbientLight(new Color4f(.2f, .2f, .2f, 1f))));
		
		// a point light
		scene.add(new Entity(new SpotLight(new Color4f(.5f, .8f, 0f), 
										   new Vector3f(BOUNDS / 2f, 0f, BOUNDS))));
		
		// a directed light, which casts shadows
		scene.add(new Entity(new DirectionLight(new Color4f(1f, 1f, 1f),
												new Vector3f(1f, -1f, -1f).normalize()),
							 sc));
	}
}
