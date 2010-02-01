package com.ferox.renderer.impl.jogl;

import com.ferox.math.Color4f;
import com.ferox.math.Vector3f;
import com.ferox.math.bounds.AxisAlignedBox;
import com.ferox.math.bounds.BoundVolume;
import com.ferox.renderer.DisplayOptions;
import com.ferox.renderer.FrameStatistics;
import com.ferox.renderer.Framework;
import com.ferox.renderer.OnscreenSurface;
import com.ferox.renderer.DisplayOptions.PixelFormat;
import com.ferox.renderer.Renderer.DrawStyle;
import com.ferox.resource.Geometry.CompileType;
import com.ferox.scene.DirectedLight;
import com.ferox.scene.Light;
import com.ferox.scene.SceneCompositor;
import com.ferox.scene.SceneCompositorFactory;
import com.ferox.scene.SceneElement;
import com.ferox.scene.fx.BlinnPhongLightingModel;
import com.ferox.scene.fx.Renderable;
import com.ferox.scene.fx.ShadowCaster;
import com.ferox.scene.fx.ShadowReceiver;
import com.ferox.scene.fx.Shape;
import com.ferox.scene.fx.ViewNode;
import com.ferox.util.entity.Entity;
import com.ferox.util.entity.EntitySystem;
import com.ferox.util.geom.Box;
import com.ferox.util.geom.PrimitiveGeometry;
import com.ferox.util.geom.Rectangle;
import com.ferox.util.geom.Sphere;
import com.ferox.util.geom.Teapot;

public class FixedFunctionSceneCompositorTest {
	private static final boolean USE_VBOS = true;
	private static final int BOUNDS = 50;
	private static final int NUM_SHAPES = 10000;
	
	public static void main(String[] args) {
		Framework framework = new FixedFunctionJoglFramework(false);
		SceneCompositor scene = new SceneCompositorFactory(framework, 1024).build();
		
		EntitySystem system = scene.getEntitySystem();
		buildScene(system);
		
		OnscreenSurface surface = buildSurface(framework, system);
		Runtime r = Runtime.getRuntime();
		while(true) {
			long now = -System.nanoTime();
			FrameStatistics stats = scene.render(false);
			now += System.nanoTime();
			
			surface.setTitle(String.format("Polys: %d, FPS: %.2f (%d of %d), Mem: %.2f", stats.getPolygonCount(), 1e9f / now, (int) (stats.getRenderTime() / 1e6f), (int) (now / 1e6f), (((r.totalMemory() - r.freeMemory()) / (1024f * 1024f)))));
		}
	}
	
	private static OnscreenSurface buildSurface(Framework framework, EntitySystem system) {
		DisplayOptions options = new DisplayOptions(PixelFormat.RGB_24BIT);
		OnscreenSurface surface = framework.createWindowSurface(options, 0, 0, 800, 600, true, false);
		
		// camera
		ViewNode vn = new ViewNode(surface, 60f, 1f, 3 * BOUNDS);
		SceneElement el = new SceneElement();
		el.getTransform().setTranslation(0f, 0f, -1.5f * BOUNDS);
		
		Entity e = system.newEntity();
		e.add(el);
		e.add(vn);
		
		surface.setClearColor(new Color4f(.5f, .5f, .5f, 1f));
		return surface;
	}
	
	private static void buildScene(EntitySystem scene) {
		// shapes
		PrimitiveGeometry geom = new Box(2f, (USE_VBOS ? CompileType.RESIDENT_STATIC : CompileType.NONE));
		//geom = new Sphere(4f, 32, (USE_VBOS ? CompileType.RESIDENT_STATIC : CompileType.NONE));
		//geom = new Teapot(2f, (USE_VBOS ? CompileType.RESIDENT_STATIC : CompileType.NONE));
		
		BoundVolume bounds = new AxisAlignedBox(geom.getVertices().getData());
		
		Renderable toRender = new Renderable();
		
		Shape shape = new Shape(geom);
		ShadowCaster sc = new ShadowCaster();
		ShadowReceiver sr = new ShadowReceiver();
		
		BlinnPhongLightingModel material = new BlinnPhongLightingModel(new Color4f(1f, 1f, 1f), new Color4f(1f, 0f, 0f));
		
		for (int i = 0; i < NUM_SHAPES; i++) {
			float x = (float) (Math.random() * BOUNDS - BOUNDS / 2);
			float y = (float) (Math.random() * BOUNDS - BOUNDS / 2);
			float z = (float) (Math.random() * BOUNDS - BOUNDS / 2);
			
			Entity e = scene.newEntity();
			SceneElement element = new SceneElement();
			element.setLocalBounds(bounds);
			element.getTransform().setTranslation(x, y, z);
			
			e.add(element);
			e.add(toRender);
			e.add(shape);
			e.add(material);
			e.add(sc);
			e.add(sr);
		}
		
		// some walls
		Rectangle backWall = new Rectangle(new Vector3f(0f, 1f, 0f), new Vector3f(0f, 0f, 1f), -BOUNDS, BOUNDS, -BOUNDS, BOUNDS);
		
		Entity e = scene.newEntity();
		SceneElement pos = new SceneElement();
		pos.getTransform().setTranslation(-BOUNDS, 0f, 0f);
		pos.setLocalBounds(new AxisAlignedBox(backWall.getVertices().getData()));
		
		e.add(pos);
		e.add(new Shape(backWall));
		e.add(material);
		e.add(new Renderable(DrawStyle.SOLID, DrawStyle.SOLID));
		e.add(sr);
		
		Rectangle bottomWall = new Rectangle(new Vector3f(-1f, 0f, 0f), new Vector3f(0f, 0f, 1f), -BOUNDS, BOUNDS, -BOUNDS, BOUNDS);
		pos = new SceneElement();
		pos.getTransform().setTranslation(0f, -BOUNDS, 0f);
		pos.setLocalBounds(new AxisAlignedBox(bottomWall.getVertices().getData()));
		e = scene.newEntity();
		
		e.add(pos);
		e.add(new Shape(bottomWall));
		e.add(material);
		e.add(new Renderable(DrawStyle.SOLID, DrawStyle.SOLID));
		e.add(sr);
		
		// ambient light
		Entity light = scene.newEntity();
		light.add(new Light(new Color4f(.2f, .2f, .2f)));
		
		// a point light
		light = scene.newEntity();
		light.add(new Light(new Color4f(.5f, .8f, 0f)));
		pos = new SceneElement();
		pos.getTransform().setTranslation(-BOUNDS / 2f, 0f, -BOUNDS);
		light.add(pos);
		
		// a directed light
		light = scene.newEntity();
		light.add(new Light(new Color4f(1f, 1f, 1f)));
		light.add(new DirectedLight(new Vector3f(-1f, 0f, 1f).normalize()));
		light.add(sc);
	}
}
