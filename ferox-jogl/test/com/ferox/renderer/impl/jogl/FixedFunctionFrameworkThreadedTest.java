package com.ferox.renderer.impl.jogl;

import java.io.File;

import com.ferox.math.Color4f;
import com.ferox.math.Matrix4f;
import com.ferox.math.Vector3f;
import com.ferox.math.Vector4f;
import com.ferox.renderer.DisplayOptions;
import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.renderer.Framework;
import com.ferox.renderer.OnscreenSurface;
import com.ferox.renderer.RenderPass;
import com.ferox.renderer.RenderSurface;
import com.ferox.renderer.Renderer;
import com.ferox.resource.Geometry;
import com.ferox.resource.TextureImage;
import com.ferox.resource.Geometry.CompileType;
import com.ferox.scene.View;
import com.ferox.util.geom.Box;
import com.ferox.util.texture.loader.TextureLoader;

public class FixedFunctionFrameworkThreadedTest {
	private static final int BOUNDS = 50;
	
	public static void main(String[] args) throws Exception {
		final Framework f = new FixedFunctionJoglFramework(false);

		System.out.println("Vender: " + f.getCapabilities().getVendor());
		System.out.println("Version: " + f.getCapabilities().getVersion());
		System.out.println("Shader Language: " + f.getCapabilities().getGlslVersion());
		
		final Geometry shape = new Box(2f, CompileType.RESIDENT_STATIC);
		System.out.println("Geometry status: " + f.update(shape, false).get());
		final TextureImage texture = TextureLoader.readTexture(new File("ferox-gl.png"));
		System.out.println("Texture status: " + f.update(texture, false).get());
		
		Thread renderer1 = new Thread(new Runnable() {
			@Override
			public void run() {
				RenderPass pass = new ShapeRenderPass(shape, texture, 10000, BOUNDS);
				OnscreenSurface surface = f.createWindowSurface(new DisplayOptions(), 10, 10, 800, 600, false, false);

				surface.setClearColor(new Color4f(.4f, .2f, 1f));
				surface.setTitle("Render 1");
				
				Runtime r = Runtime.getRuntime();
				while(true) {
					try {
						surface.setTitle(String.format("Render 1, Mem: %.2f", ((r.totalMemory() - r.freeMemory()) / (1024f * 1024f))));
						f.queue(surface, pass);
						f.render().get();
					} catch(Exception e) {
						System.out.println(e.getClass().getSimpleName() + "-" + e.getMessage());
						break;
					}
				}
			}
		});
		renderer1.setName("Renderer 1");
		renderer1.start();
		
		Thread renderer2 = new Thread(new Runnable() {
			@Override
			public void run() {
				RenderPass pass = new ShapeRenderPass(shape, texture, 10000, BOUNDS);
				OnscreenSurface surface = f.createWindowSurface(new DisplayOptions(), 500, 10, 800, 600, false, false);
				
				surface.setClearColor(new Color4f(.2f, .2f,.2f, 1f));
				surface.setTitle("Render 2");
				
				Runtime r = Runtime.getRuntime();
				while(true) {
					try {
						surface.setTitle(String.format("Render 2, Mem: %.2f", ((r.totalMemory() - r.freeMemory()) / (1024f * 1024f))));

						f.queue(surface, pass);
						f.render().get();
					} catch(Exception e) {
						System.out.println(e.getClass().getSimpleName() + "-" + e.getMessage());
						break;
					}
				}
			}
		});
		renderer2.setName("Renderer 2");
		renderer2.start();
		
		Thread destroyer = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(4000);
					f.destroy();
				} catch(Exception e) {
					System.out.println(e.getClass().getSimpleName() + "-" + e.getMessage());
				}
			}
		});
		destroyer.setName("Destroyer");
		//destroyer.start();
	}
	
	private static class ShapeRenderPass implements RenderPass {
		private static final Matrix4f convert = new Matrix4f(-1, 0, 0, 0, 
															  0, 1, 0, 0, 
															  0, 0, -1, 0,
															  0, 0, 0, 1);
		private final int numShapes;
		private final Geometry shape;
		private final TextureImage texture;
		
		private final Matrix4f[] shapeTransforms;
		private final View view;
		
		public ShapeRenderPass(Geometry shape, TextureImage texture, int numShapes, int bounds) {
			this.numShapes = numShapes;
			this.shape = shape;
			this.texture = texture;
			
			shapeTransforms = new Matrix4f[numShapes];
			for (int i = 0; i < numShapes; i++) {
				float x = (float) (Math.random() * bounds - bounds / 2);
				float y = (float) (Math.random() * bounds - bounds / 2);
				float z = (float) (Math.random() * bounds - bounds / 2);
				
				Matrix4f t = new Matrix4f().setIdentity();
				t.set(0, 3, x).set(1, 3, y).set(2, 3, z).set(3, 3, 1);
				shapeTransforms[i] = t;
			}
			
			view = new View();
			view.getFrustum().setPerspective(45f, 1.33f, .1f, bounds * 2f);
			view.setLocation(new Vector3f(0f, 0f, 1.5f * bounds));
		}
		
		private static final Color4f black = new Color4f(0f, 0f, 0f, 1f);
		private static final Color4f diffuse = new Color4f(.9f, .4f, .4f, 1f);
		private static final Color4f amb = new Color4f(.2f, .2f, .2f, 1f);
		private static final Color4f diff_l = new Color4f(.5f, .3f, .5f, 1f);
		private static final Color4f white = new Color4f(1f, 1f, 1f, 1f);
		
		@Override
		public void render(Renderer renderer, RenderSurface surface) {
			view.updateView();
			FixedFunctionRenderer f = (FixedFunctionRenderer) renderer;
			
			f.setProjectionMatrix(view.getProjectionMatrix());
			Matrix4f v = view.getViewTransform().get(null);
			convert.mul(v, v);
			
			f.setModelViewMatrix(v);
			f.setLightingEnabled(true);
			f.setLightEnabled(0, true);
			f.setLightColor(0, amb, diff_l, white);
			f.setLightPosition(0, new Vector4f(0f, 0f, 1f, 0f));
			
			f.setTexture(0, texture);
			f.setTextureEnabled(0, true);
			
			Matrix4f mv = new Matrix4f();
			for (int i = 0; i < numShapes; i++) {
				v.mul(shapeTransforms[i], mv);
				
				f.setModelViewMatrix(mv);
				f.setMaterial(black, diffuse, black, black);

				f.render(shape);
			}
		}
	}
}
