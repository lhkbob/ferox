package com.ferox.test;

import java.awt.Component;
import java.awt.event.KeyEvent;
import java.io.File;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.swing.JFrame;

import org.openmali.vecmath.Matrix3f;
import org.openmali.vecmath.Vector3f;

import com.ferox.core.renderer.FrameStatistics;
import com.ferox.core.renderer.RenderManager;
import com.ferox.core.renderer.RenderPass;
import com.ferox.core.scene.InfluenceLeaf;
import com.ferox.core.scene.SpatialBranch;
import com.ferox.core.scene.SpatialLeaf;
import com.ferox.core.scene.SpatialTree;
import com.ferox.core.scene.Transform;
import com.ferox.core.scene.View;
import com.ferox.core.scene.ViewNode;
import com.ferox.core.scene.bounds.BoundingSphere;
import com.ferox.core.scene.states.DirectionLight;
import com.ferox.core.scene.states.SpotLight;
import com.ferox.core.states.StateBranch;
import com.ferox.core.states.StateLeaf;
import com.ferox.core.states.StateTree;
import com.ferox.core.states.atoms.BlendState;
import com.ferox.core.states.atoms.BufferData;
import com.ferox.core.states.atoms.Material;
import com.ferox.core.states.atoms.Texture;
import com.ferox.core.states.atoms.TextureCubeMap;
import com.ferox.core.states.atoms.VertexArray;
import com.ferox.core.states.atoms.Texture.AutoTCGen;
import com.ferox.core.states.atoms.Texture.EnvMode;
import com.ferox.core.states.atoms.TextureData.MagFilter;
import com.ferox.core.states.atoms.TextureData.MinFilter;
import com.ferox.core.states.atoms.TextureData.TexClamp;
import com.ferox.core.states.manager.Geometry;
import com.ferox.core.states.manager.LightManager;
import com.ferox.core.states.manager.MaterialManager;
import com.ferox.core.states.manager.TextureManager;
import com.ferox.core.system.DisplayOptions;
import com.ferox.core.system.OnscreenRenderSurface;
import com.ferox.core.util.InputManager;
import com.ferox.core.util.TextureResourceManager;
import com.ferox.core.util.TimeSmoother;
import com.ferox.impl.jsr231.JOGLPassiveRenderContext;
import com.sun.opengl.util.BufferUtil;

public class LightTest {
	private JFrame frame;
	private InputManager input;
	private RenderManager manager;
	private RenderPass pass;
	private SpatialBranch node;
	private ViewNode camera;
	private boolean quit;
	private FrameStatistics stats;
	private float rotX=0;
	private float rotY =0;
	private boolean paused;
	private Transform rotater;
	private TimeSmoother timer;
	private Material m1;
	private BlendState blend;
	
	public static void main(String[] args) {
		new LightTest().run();
	}
	
	public LightTest() {
		frame = new JFrame("LightTest");
		frame.setSize(640, 480);
		
		timer = new TimeSmoother();
		DisplayOptions d = new DisplayOptions();
		//d.setHeavyweight(false);
		//d.setNumMultiSamples(4);
		manager = new RenderManager(new JOGLPassiveRenderContext(d));
		
		pass = new RenderPass();
		
		node = new SpatialBranch(null, 10);
		camera = new ViewNode(null);
		camera.getLocalTransform().setTranslation(0, 0, -100);
	
		input = new InputManager(frame);
		input.setKeyBehavior(KeyEvent.VK_ESCAPE, InputManager.INITIAL_PRESS);
		input.setKeyBehavior(KeyEvent.VK_R, InputManager.INITIAL_PRESS);
		input.setKeyBehavior(KeyEvent.VK_B, InputManager.INITIAL_PRESS);
		input.setKeyBehavior(KeyEvent.VK_P, InputManager.INITIAL_PRESS);
		
		frame.add((Component)((OnscreenRenderSurface)manager.getRenderingSurface()).getRenderSurface());
		manager.addRenderPass(pass);
		//manager.setClearColor(new float[] {.05f, .1f, .6f, 0f});
		//pass.setStateManagerTypeMasked(StateManager.SM_TEXTURE, true);
		//pass.setStateManagerTypeMasked(StateManager.SM_MATERIAL, true);
		//pass.setStateManagerTypeMasked(StateManager.SM_LIGHT, true);
		
		TextureCubeMap tcm = null;
		try {
			tcm = TextureResourceManager.readTextureCubeMap(new File("data/textures/mars.dds"), true, MinFilter.NEAREST, MagFilter.NEAREST);
			tcm.setTexClampSTR(TexClamp.MIRROR);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
		
		Texture tex = new Texture(tcm, EnvMode.MODULATE, new float[4], AutoTCGen.OBJECT);
		TextureManager tman = new TextureManager();
		tman.setTexture(0, tex);
		
		Material mat1 = new Material(new float[] {1f, 1f, 1f, 1f});
		MaterialManager app = new MaterialManager(mat1);
		Material mat2 = new Material(new float[] {1f, 0f, 0f, 1f});
		MaterialManager app2 = new MaterialManager(mat2);
		
		DirectionLight dl = new DirectionLight(new Vector3f(1f, 1f, 1f), new float[] {1f, 0f, 0f, 1f}, new float[] {1f,1f, 1f, 1f});
		SpotLight sl = new SpotLight(new Vector3f(0f, 0f, -1f), new float[] {1f, 1f, 1f, 1f});
		
		LightManager l1 = new LightManager();
		l1.setLocalViewer(true);
		l1.setSeperateSpecularHighlight(true);
				
		StateBranch states;
		states = new StateBranch(null, 2);
		states.addStateManager(tman);
		
		StateBranch a1 = new StateBranch(states, 2);
		StateBranch a2 = new StateBranch(states, 2);
		
		StateLeaf a3 = new StateLeaf(a1);
		a3.addStateManager(l1);
		a3.addStateManager(app);
		
		StateLeaf a4 = new StateLeaf(a1);
		a4.addStateManager(app);
		
		StateLeaf a5 = new StateLeaf(a2);
		a5.addStateManager(l1);
		a5.addStateManager(app2);
		
		StateLeaf a6 = new StateLeaf(a2);
		a6.addStateManager(app2);

		Geometry geom = null;
		Geometry geom2 = null;
		/*try {
			now = System.currentTimeMillis();
			geom = (Geometry)com.ferox.util.io.IOManager.read(new File("dragon_4.ido2"));
			System.out.println("total read/write time: " + (System.currentTimeMillis() - now));
			System.out.println("geom verts: " + geom.getVertices().getNumElements() + " geom indices: " + geom.getIndices().getNumElements() + " polys: " + geom.getPolygonCount());
		} catch(IOException ioe) {
			System.out.println(ioe);
			System.exit(1);
		}*/
		/*try {
			now = System.currentTimeMillis();
			geom2 = (Geometry)com.ferox.util.io.IOManager.read(new File("dragon_big.ido2"));
			System.out.println("total read/write time: " + (System.currentTimeMillis() - now));
			System.out.println("geom verts: " + geom.getVertices().getNumElements() + " geom indices: " + geom.getIndices().getNumElements() + " polys: " + geom.getPolygonCount());
		} catch(IOException ioe) {
			System.out.println(ioe);
			System.exit(1);
		}*/ 
		geom = buildCube();
		geom2 = geom;
		
		a1.addStateManager(geom);
		a2.addStateManager(geom2);
		
		for (int i = 0; i < 100; i++) {
			SpatialLeaf spat_atom;
			StateLeaf stat_atom;
			double r = Math.random();
			
			if (r < .25) {
				stat_atom = a3;
			} else if (r < .5) {
				stat_atom = a4;
			} else if (r < .75) {
				stat_atom = a5;
			} else {
				stat_atom = a6;
			}
						
			spat_atom = new SpatialLeaf(node, stat_atom);
			if (i != 0)
				spat_atom.getLocalTransform().setTranslation((float)(Math.random() * 100 - 50), (float)(Math.random() * 100 - 50), (float)(Math.random() * 100 - 50));
			assignBounds(spat_atom);
		}
	
		InfluenceLeaf sln = new InfluenceLeaf(camera);
		sln.setState(sl);
		BoundingSphere s = new BoundingSphere();
		s.setRadius(60f);
		sln.setInfluence(s);
		sln.getLocalTransform().setTranslation(0, 0, 0);

		if (rotater == null)
			rotater = node.getChild(0).getLocalTransform();
		
		SpatialBranch root = new SpatialBranch();
		root.add(node);
		root.add(camera);
		
		pass.setSpatialTree(new SpatialTree(root));
		camera.setView(new View());
		pass.setView(camera.getView());
		pass.getView().setPerspective(60f, 1f, 1f, 1000);
		manager.enableUpdate(new StateTree(states));
		manager.enableUpdate(pass.getSpatialTree());
		manager.getRenderPass(0).setClearedColor(new float[] {.5f, .5f, .5f, 1f});
		quit = false;
	}
	
	public Geometry buildCube() {
		float[] v = new float[72];
		float[] n = new float[72];
		
		// front
		v[0] = 1f; v[1] = 1f; v[2] = 1f; n[0] = 0f; n[1] = 0f; n[2] = 1f;
		v[3] = -1f; v[4] = 1f; v[5] = 1f; n[3] = 0f; n[4] = 0f; n[5] = 1f;
		v[6] = -1f; v[7] = -1f; v[8] = 1f; n[6] = 0f; n[7] = 0f; n[8] = 1f;
		v[9] = 1f; v[10] = -1f; v[11] = 1f; n[9] = 0f; n[10] = 0f; n[11] = 1f;
		//back
		v[12] = -1f; v[13] = -1f; v[14] = -1f; n[12] = 0f; n[13] = 0f; n[14] = -1f;
		v[21] = 1f; v[22] = -1f; v[23] = -1f; n[21] = 0f; n[22] = 0f; n[23] = -1f;
		v[18] = 1f; v[19] = 1f; v[20] = -1f; n[18] = 0f; n[19] = 0f; n[20] = -1f;
		v[15] = -1f; v[16] = 1f; v[17] = -1f; n[15] = 0f; n[16] = 0f; n[17] = -1f;
		//right
		v[24] = 1f; v[25] = 1f; v[26] = -1f; n[24] = 1f; n[25] = 0f; n[26] = 0f;
		v[27] = 1f; v[28] = 1f; v[29] = 1f; n[27] = 1f; n[28] = 0f; n[29] = 0f;
		v[30] = 1f; v[31] = -1f; v[32] = 1f; n[30] = 1f; n[31] = 0f; n[32] = 0f;
		v[33] = 1f; v[34] = -1f; v[35] = -1f; n[33] = 1f; n[34] = 0f; n[35] = 0f;
		//left
		v[36] = -1f; v[37] = -1f; v[38] = 1f; n[36] = -1f; n[37] = 0f; n[38] = 0f;
		v[45] = -1f; v[46] = -1f; v[47] = -1f; n[45] = -1f; n[46] = 0f; n[47] = 0f;
		v[42] = -1f; v[43] = 1f; v[44] = -1f; n[42] = -1f; n[43] = 0f; n[44] = 0f;
		v[39] = -1f; v[40] = 1f; v[41] = 1f; n[39] = -1f; n[40] = 0f; n[41] = 0f;
		//top
		v[48] = -1f; v[49] = 1f; v[50] = -1f; n[48] = 0f; n[49] = 1f; n[50] = 0f;
		v[57] = 1f; v[58] = 1f; v[59] = -1f; n[57] = 0f; n[58] = 1f; n[59] = 0f;
		v[54] = 1f; v[55] = 1f; v[56] = 1f; n[54] = 0f; n[55] = 1f; n[56] = 0f;
		v[51] = -1f; v[52] = 1f; v[53] = 1f; n[51] = 0f; n[52] = 1f; n[53] = 0f;
		//bottom
		v[60] = 1f; v[61] = -1f; v[62] = 1f; n[60] = 0f; n[61] = -1f; n[62] = 0f;
		v[63] = -1f; v[64] = -1f; v[65] = 1f; n[63] = 0f; n[64] = -1f; n[65] = 0f;
		v[66] = -1f; v[67] = -1f; v[68] = -1f; n[66] = 0f; n[67] = -1f; n[68] = 0f;
		v[69] = 1f; v[70] = -1f; v[71] = -1f; n[69] = 0f; n[70] = -1f; n[71] = 0f;
		
		int[] i = new int[24];
		for (int u = 0; u < 24; u++)
			i[u] = u;
		
		FloatBuffer vb = BufferUtil.newFloatBuffer(v.length);
		vb.put(v).rewind();
		FloatBuffer nb = BufferUtil.newFloatBuffer(n.length);
		nb.put(n).rewind();
		
		IntBuffer ib = BufferUtil.newIntBuffer(i.length);
		ib.put(i).rewind();
		
		BufferData vbd = new BufferData(vb, BufferData.DataType.FLOAT, vb.capacity(), BufferData.BufferTarget.ARRAY_BUFFER);
		BufferData nbd = new BufferData(nb, BufferData.DataType.FLOAT, nb.capacity(), BufferData.BufferTarget.ARRAY_BUFFER);
		BufferData ibd = new BufferData(ib, BufferData.DataType.UNSIGNED_INT, ib.capacity(), BufferData.BufferTarget.ELEMENT_BUFFER);
		
		VertexArray iva = new VertexArray(ibd, 1);
		
		Geometry geom = new Geometry(new VertexArray(vbd, 3), new VertexArray(nbd, 3), iva, Geometry.PolygonType.QUADS);
		return geom;
	}
	
	private void assignBounds(SpatialLeaf node) {
		node.setModelBounds(new BoundingSphere());
	}
	
	public void run() {
		this.frame.setVisible(true);
		paused = false;

		while (!quit) {
			if (!paused) {
				//System.out.println("frame start");
				stats = this.manager.render();
				timer.addSample(stats.getDuration() / 1000000);
			}
			checkInput();
	
			if (paused) {
				try {
					Thread.sleep(5);
				} catch (Exception e) {
					
				}
			}
		}
	
		manager.destroy();
		System.exit(0);
	}
	
	public void checkInput() {
		if (input.isKeyPressed(KeyEvent.VK_ESCAPE)) 
			quit = true;
		if (!paused) {
		if (input.isKeyPressed(KeyEvent.VK_R)) {
			System.out.println(stats);
			System.out.println(timer.getFrameRate());
		}
		if (input.isKeyPressed(KeyEvent.VK_LEFT))
			camera.getLocalTransform().getTranslation().x += -.1f;
		if (input.isKeyPressed(KeyEvent.VK_RIGHT))
			camera.getLocalTransform().getTranslation().x += .1f;
		if (input.isKeyPressed(KeyEvent.VK_S))
			camera.getLocalTransform().getTranslation().y += -.1f;
		if (input.isKeyPressed(KeyEvent.VK_W))
			camera.getLocalTransform().getTranslation().y += .1f;
		if (input.isKeyPressed(KeyEvent.VK_UP))
			camera.getLocalTransform().getTranslation().z += -.1;
		if (input.isKeyPressed(KeyEvent.VK_DOWN))
			camera.getLocalTransform().getTranslation().z += .1;
		if (input.isKeyPressed(KeyEvent.VK_E))
			rotY+=.01f;
		if (input.isKeyPressed(KeyEvent.VK_Q))
			rotY-=.01f;
		if (input.isKeyPressed(KeyEvent.VK_A))
			rotX+=.01f;
		if (input.isKeyPressed(KeyEvent.VK_D))
			rotX-=.01f;
		if (input.isKeyPressed(KeyEvent.VK_K))
			rotater.getTranslation().y -= .01;
		if (input.isKeyPressed(KeyEvent.VK_I))
			rotater.getTranslation().y += .01;
		if (input.isKeyPressed(KeyEvent.VK_J))
			rotater.getTranslation().x -= .01;
		if (input.isKeyPressed(KeyEvent.VK_L))
			rotater.getTranslation().x += .01;
		
		if (input.isKeyPressed(KeyEvent.VK_EQUALS)) {
			Vector3f s = rotater.getScale();
			s.set(s.x + .01f, s.y + .01f, s.z + .01f);
			s.set(Math.min(10, s.x), Math.min(10, s.y), Math.min(10, s.z));
		}
		if (input.isKeyPressed(KeyEvent.VK_MINUS)) {
			Vector3f s = rotater.getScale();
			s.set(s.x - .01f, s.y - .01f, s.z - .01f);
			s.set(Math.max(.01f, s.x), Math.max(.01f, s.y), Math.max(.01f, s.z));
		}
		
		Matrix3f m = new Matrix3f();
		m.setIdentity();
		m.rotX(rotX);
		rotater.getRotation().rotY(rotY);
		rotater.getRotation().mul(m);
		}
		if (input.isKeyPressed(KeyEvent.VK_P)) {
			View view = manager.getRenderPass(0).getView();
			if (view.isOrthoProjection()) {
				view.setUseOrthoProjection(false);
				view.setPerspective(60f, 1f, .1f, 100);
				pass.getView().setViewBottom(0f);
				pass.getView().setViewTop(1f);
				pass.getView().setViewRight(1f);
				pass.getView().setViewLeft(0f);
			} else {
				view.setUseOrthoProjection(true);
				 float aspect = (float) manager.getRenderContext().getContextWidth() / manager.getRenderContext().getContextHeight();
				 
			     view.setFrustum(-50 * aspect, 50 * aspect, 50, -50, -100, 1000);
			     pass.getView().setViewBottom(0f);
			     pass.getView().setViewTop(1f);
			     pass.getView().setViewRight(1f);
				pass.getView().setViewLeft(0f);
			}
		}
	}
}
