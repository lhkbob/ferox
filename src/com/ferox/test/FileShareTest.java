package com.ferox.test;

import java.awt.Component;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.JFrame;

import org.openmali.vecmath.Matrix3f;

import com.ferox.core.renderer.FrameStatistics;
import com.ferox.core.renderer.RenderManager;
import com.ferox.core.scene.SpatialBranch;
import com.ferox.core.scene.SpatialLeaf;
import com.ferox.core.scene.SpatialTree;
import com.ferox.core.scene.Transform;
import com.ferox.core.scene.View;
import com.ferox.core.scene.ViewNode;
import com.ferox.core.states.StateBranch;
import com.ferox.core.states.StateLeaf;
import com.ferox.core.states.StateTree;
import com.ferox.core.states.atoms.Material;
import com.ferox.core.states.atoms.Texture;
import com.ferox.core.states.atoms.Texture2D;
import com.ferox.core.states.atoms.Texture.AutoTCGen;
import com.ferox.core.states.atoms.Texture.EnvMode;
import com.ferox.core.states.manager.Geometry;
import com.ferox.core.states.manager.MaterialManager;
import com.ferox.core.states.manager.TextureManager;
import com.ferox.core.system.DisplayOptions;
import com.ferox.core.system.OnscreenRenderSurface;
import com.ferox.core.util.InputManager;
import com.ferox.core.util.TextureResourceManager;
import com.ferox.core.util.TimeSmoother;
import com.ferox.core.util.io.IOManager;
import com.ferox.core.util.io.binary.BinaryExporter;
import com.ferox.core.util.io.binary.BinaryImporter;
import com.ferox.impl.jsr231.JOGLPassiveRenderContext;

public class FileShareTest {
	private StateLeaf appearance;
	private SpatialBranch scene;
	private View view;
	private RenderManager manager;
	private Transform transform;
	private float rotX, rotY;
	private JFrame window;
	
	private InputManager input;
	private boolean quit;
	private TimeSmoother timer;
	private FrameStatistics stats;
	
	public static void main(String[] args) {
		Texture2D t1 = null;
		Texture2D t2 = null;
		Geometry g = null;
		try {
			t1 = TextureResourceManager.readTexture2D(new File("data/textures/Coin.dds"));
			t2 = TextureResourceManager.readTexture2D(new File("data/textures/dirt.dds"));
			g = (Geometry)IOManager.read(new File("data/models/dragon_1.ido2"), new BinaryImporter()).getPrimaryObject();
		} catch (IOException ioe) {
			ioe.printStackTrace();
			System.exit(0);
		}
		
		Texture tt1 = new Texture(t1, EnvMode.MODULATE, new float[4], AutoTCGen.OBJECT);
		Texture tt2 = new Texture(t2, EnvMode.MODULATE, new float[4], AutoTCGen.EYE);
		TextureManager tm1 = new TextureManager();
		tm1.setTexture(0, tt1);
		tm1.setTexture(1, tt2);
		MaterialManager m = new MaterialManager(new Material(new float[] {.5f, .5f, .5f, 1f}, new float[] {1f, 0f, 0f, 1f}, 50f));
		
		StateLeaf app = new StateLeaf();
		app.addStateManager(tm1);
		app.addStateManager(m);
		app.addStateManager(g);
		
		try {
			IOManager.write(new File("data/textures/td_1.tex"), t1, new BinaryExporter(), false, true);
			IOManager.write(new File("data/textures/td_2.tex"), t2, new BinaryExporter(), false, true);
			IOManager.write(new File("data/models/dragon_tex.app"), app, new BinaryExporter(), true, false);
		} catch (IOException ioe) {
			ioe.printStackTrace();
			System.exit(0);
		}
		
		IOManager.clearIOManagerCache();
		new FileShareTest().run();
	}
	
	public FileShareTest() {
		this.manager = new RenderManager(new JOGLPassiveRenderContext(new DisplayOptions()));
		
		this.window = new JFrame("GLSLTest");
		this.window.add((Component)((OnscreenRenderSurface)this.manager.getRenderingSurface()).getRenderSurface());
		this.window.setSize(800, 600);
		
		this.view = new View();
		this.view.setPerspective(60, 4f/3f, 1, 200);
		ViewNode node = new ViewNode(this.view);
		node.getLocalTransform().setTranslation(0f, 0f, -10f);
		
		this.scene = new SpatialBranch();
		this.scene.add(node);
		SpatialLeaf geom = new SpatialLeaf(this.scene);
		geom.getLocalTransform().setTranslation(0f, 0f, 10f);
		this.transform = geom.getLocalTransform();
		
		try {
			this.appearance = (StateLeaf)IOManager.read(new File("data/models/dragon_tex.app"), new BinaryImporter()).getPrimaryObject();
		} catch (IOException ioe) {
			ioe.printStackTrace();
			System.exit(0);
		}
		geom.setStates(this.appearance);
		
		StateBranch sRoot = new StateBranch();
		sRoot.add(this.appearance);
		
		this.manager.setSpatialTree(new SpatialTree(this.scene));
		this.manager.enableUpdate(new StateTree(sRoot));
		this.manager.setView(this.view);
		
		this.manager.getRenderPass(0).setClearedColor(new float[] {.5f, .5f, .5f, 1f});
		
		this.timer = new TimeSmoother();
		this.input = new InputManager(this.window);
		this.input.setKeyBehavior(KeyEvent.VK_ESCAPE, InputManager.INITIAL_PRESS);
		this.input.setKeyBehavior(KeyEvent.VK_R, InputManager.INITIAL_PRESS);
	}
	
	public void run() {
		this.window.setVisible(true);
		
		while (!quit) {
			stats = this.manager.render();
			timer.addSample(stats.getDuration() / 1000000);
			/*try {
				Thread.sleep(5);
			} catch(Exception e) {}*/
			
			checkInput();
		}
	
		this.manager.destroy();
		System.exit(0);
	}
	
	private void checkInput() {
		if (input.isKeyPressed(KeyEvent.VK_ESCAPE)) 
			quit = true;
		if (input.isKeyPressed(KeyEvent.VK_R)) {
			System.out.println(stats);
			System.out.println(timer.getFrameRate());
		}
		if (input.isKeyPressed(KeyEvent.VK_LEFT))
			transform.getTranslation().x += -.1f;
		if (input.isKeyPressed(KeyEvent.VK_RIGHT))
			transform.getTranslation().x += .1f;
		if (input.isKeyPressed(KeyEvent.VK_S))
			transform.getTranslation().y += -.1f;
		if (input.isKeyPressed(KeyEvent.VK_W))
			transform.getTranslation().y += .1f;
		if (input.isKeyPressed(KeyEvent.VK_UP))
			transform.getTranslation().z += -.1;
		if (input.isKeyPressed(KeyEvent.VK_DOWN))
			transform.getTranslation().z += .1;
		if (input.isKeyPressed(KeyEvent.VK_E))
			rotY+=.01f;
		if (input.isKeyPressed(KeyEvent.VK_Q))
			rotY-=.01f;
		if (input.isKeyPressed(KeyEvent.VK_A))
			rotX+=.01f;
		if (input.isKeyPressed(KeyEvent.VK_D))
			rotX-=.01f;
		
		Matrix3f m = new Matrix3f();
		m.setIdentity();
		m.rotX(rotX);
		transform.getRotation().rotY(rotY);
		transform.getRotation().mul(m);
	}
}
