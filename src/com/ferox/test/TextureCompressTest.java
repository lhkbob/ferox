package com.ferox.test;

import java.awt.Component;
import java.awt.event.KeyEvent;
import java.io.File;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.swing.JFrame;

import org.openmali.vecmath.Matrix3f;
import org.openmali.vecmath.Vector3f;

import com.ferox.core.renderer.FrameListener;
import com.ferox.core.renderer.FrameStatistics;
import com.ferox.core.renderer.InitializationListener;
import com.ferox.core.renderer.RenderManager;
import com.ferox.core.scene.InfluenceLeaf;
import com.ferox.core.scene.SpatialBranch;
import com.ferox.core.scene.SpatialLeaf;
import com.ferox.core.scene.SpatialNode;
import com.ferox.core.scene.SpatialTree;
import com.ferox.core.scene.Transform;
import com.ferox.core.scene.View;
import com.ferox.core.scene.ViewNode;
import com.ferox.core.scene.bounds.AxisAlignedBox;
import com.ferox.core.scene.bounds.BoundingSphere;
import com.ferox.core.scene.states.SpotLight;
import com.ferox.core.states.StateBranch;
import com.ferox.core.states.StateLeaf;
import com.ferox.core.states.StateTree;
import com.ferox.core.states.atoms.BufferData;
import com.ferox.core.states.atoms.Material;
import com.ferox.core.states.atoms.Texture;
import com.ferox.core.states.atoms.TextureCubeMap;
import com.ferox.core.states.atoms.VertexArray;
import com.ferox.core.states.atoms.Texture.AutoTCGen;
import com.ferox.core.states.atoms.Texture.EnvMode;
import com.ferox.core.states.atoms.TextureCubeMap.Face;
import com.ferox.core.states.atoms.TextureData.MagFilter;
import com.ferox.core.states.atoms.TextureData.MinFilter;
import com.ferox.core.states.atoms.TextureData.TextureCompression;
import com.ferox.core.states.atoms.TextureData.TextureFormat;
import com.ferox.core.states.atoms.TextureData.TextureType;
import com.ferox.core.states.manager.Geometry;
import com.ferox.core.states.manager.LightManager;
import com.ferox.core.states.manager.MaterialManager;
import com.ferox.core.states.manager.TextureManager;
import com.ferox.core.system.DisplayOptions;
import com.ferox.core.system.OnscreenRenderSurface;
import com.ferox.core.util.InputManager;
import com.ferox.core.util.TextureResourceManager;
import com.ferox.core.util.TimeSmoother;
import com.ferox.core.util.DataTransfer.Slice;
import com.ferox.impl.jsr231.JOGLPassiveRenderContext;
import com.sun.opengl.util.BufferUtil;

public class TextureCompressTest implements InitializationListener, FrameListener {
	private RenderManager manager;
	private JFrame window;
	private TextureCubeMap base;
	private TextureCubeMap compressed;
	private SpatialTree scene;
	private View view;
	private Transform transform;
	
	private InputManager input;
	private boolean quit;
	private TimeSmoother timer;
	private FrameStatistics stats;
	
	private long elapsedTime;
	
	public static void main(String[] args) {
		new TextureCompressTest().run();
	}
	
	private TextureCompressTest() {
		this.manager = new RenderManager(new JOGLPassiveRenderContext(new DisplayOptions()));
		
		this.window = new JFrame("TextureCompressTest");
		this.window.add((Component)((OnscreenRenderSurface)this.manager.getRenderingSurface()).getRenderSurface());
		this.window.setSize(800, 600);
		
		this.view = new View();
		this.view.setPerspective(60, 4f/3f, 1, 200);
		ViewNode node = new ViewNode(this.view);
		node.getLocalTransform().setTranslation(0f, 0f, -10f);
		
		this.scene = this.buildScene(this.manager);
		this.transform = this.scene.getRootNode().getChild(0).getLocalTransform();
		this.scene.getRootNode().add(node);
		
		this.manager.setSpatialTree(this.scene);
		this.manager.setView(this.view);
		
		this.manager.getRenderPass(0).setClearedColor(new float[] {.5f, .5f, .5f, 1f});
		this.manager.addInitializationListener(this);
		this.manager.addFrameListener(this);
		
		this.timer = new TimeSmoother();
		this.input = new InputManager(this.window);
		this.input.setKeyBehavior(KeyEvent.VK_ESCAPE, InputManager.INITIAL_PRESS);
		this.input.setKeyBehavior(KeyEvent.VK_R, InputManager.INITIAL_PRESS);
	}
	
	private SpatialTree buildScene(RenderManager manager) {
		SpatialBranch top = new SpatialBranch(null, 1);
		
		SpatialBranch root = new SpatialBranch(top, 4);
		StateBranch sRoot = new StateBranch(null, 4);
		
		Geometry cube = this.buildCube(2f);
		Geometry cube2 = this.buildCube(.5f);
		
		// Basic cube map
		StateLeaf sl1 = new StateLeaf(sRoot);
		sl1.addStateManager(cube);
		SpatialLeaf l1 = new SpatialLeaf(root, sl1);
		l1.getLocalTransform().getTranslation().set(3, 0, 0);
		l1.setModelBounds(new AxisAlignedBox());
		
		StateLeaf hl1 = new StateLeaf(sRoot);
		hl1.addStateManager(cube2);
		SpatialLeaf h1 = new SpatialLeaf(root, hl1);
		h1.getLocalTransform().getTranslation().set(3, 2, 0);
		h1.setModelBounds(new BoundingSphere());
		
		// DXT cube map
		StateLeaf sl2 = new StateLeaf(sRoot);
		sl2.addStateManager(cube);
		SpatialLeaf l2 = new SpatialLeaf(root, sl2);
		l2.getLocalTransform().getTranslation().set(-3, 0, 0);
		l2.setModelBounds(new AxisAlignedBox());
		
		StateLeaf hl2 = new StateLeaf(sRoot);
		hl2.addStateManager(cube2);
		SpatialLeaf h2 = new SpatialLeaf(root, hl2);
		h2.getLocalTransform().getTranslation().set(-3, 2, 0);
		h2.setModelBounds(new AxisAlignedBox());
		
		this.base = null;
		this.compressed = null;
		
		try {
			this.base = TextureResourceManager.readTextureCubeMap(new File("data/textures/grace_cube.dds"), true, MinFilter.NEAREST_MIP_NEAREST, MagFilter.NEAREST);
			TextureCompression comp = TextureCompression.DXT1;
			if (this.base.getDataFormat().getNumComponents() == 4)
				comp = TextureCompression.DXT5;
			
			this.base.setTextureFormat(this.base.getDataFormat(), this.base.getDataType(), comp);
			
			TextureFormat c1;
			if (comp == TextureCompression.DXT1)
				c1 = TextureFormat.COMPRESSED_RGB_DXT1;
			else 
				c1 = TextureFormat.COMPRESSED_RGBA_DXT5;
						
			this.compressed = new TextureCubeMap(makeDummyBuffers(c1, TextureType.UNSIGNED_BYTE, this.base.getSideLength()), makeDummyBuffers(c1, TextureType.UNSIGNED_BYTE, this.base.getSideLength()), makeDummyBuffers(c1, TextureType.UNSIGNED_BYTE, this.base.getSideLength()),
												 makeDummyBuffers(c1, TextureType.UNSIGNED_BYTE, this.base.getSideLength()), makeDummyBuffers(c1, TextureType.UNSIGNED_BYTE, this.base.getSideLength()), makeDummyBuffers(c1, TextureType.UNSIGNED_BYTE, this.base.getSideLength()),
												 this.base.getSideLength(), TextureType.UNSIGNED_BYTE, c1, MinFilter.LINEAR_MIP_LINEAR, MagFilter.LINEAR);
		} catch(Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
		
		Texture t1 = new Texture(this.base, EnvMode.MODULATE, new float[4], AutoTCGen.OBJECT);
		Texture t2 = new Texture(this.compressed, EnvMode.MODULATE, new float[4], AutoTCGen.OBJECT);
		TextureManager tm1 = new TextureManager();
		tm1.setTexture(0, t1);
		TextureManager tm2 = new TextureManager();
		tm2.setTexture(0, t2);
		sl1.addStateManager(tm1);
		sl2.addStateManager(tm2);
		
		MaterialManager white = new MaterialManager(new Material(new float[] {1f, 1f, 1f, 1f}, new float[] {1f, 0f, 0f, 1f}, 50));
		MaterialManager red = new MaterialManager(new Material(new float[] {1f, 0f, 0f, 1f}));
		MaterialManager green = new MaterialManager(new Material(new float[] {0f, 1f, 0f, 1f}));
		
		sl1.addStateManager(white);
		sl2.addStateManager(white);
		hl1.addStateManager(green);
		hl2.addStateManager(red);
		
		LightManager lm = new LightManager();
		lm.setGlobalAmbientLight(new float[] {.5f, .5f, .5f, 1f});
		lm.setLocalViewer(true);
		//sl1.addStateManager(lm);
		//sl2.addStateManager(lm);
		
		SpotLight light = new SpotLight(new Vector3f(0f, 0f, -1f), new float[] {1f, 1f, 1f, 1f});
		//light.setSpotCutoffAngle(45);
		InfluenceLeaf l = new InfluenceLeaf(root, light);
		l.setCullMode(SpatialNode.CullMode.NEVER);
		l.getLocalTransform().getTranslation().set(2f, 5f, 2f);
		BoundingSphere s = new BoundingSphere();
		s.setCenter(new Vector3f());
		s.setRadius(5f);
		l.setInfluence(s);
		
		SpotLight light2 = new SpotLight(new Vector3f(0f, 0f, -1f), new float[] {1f, 0f, 1f, 1f});
		//light.setSpotCutoffAngle(45);
		InfluenceLeaf l2Leaf = new InfluenceLeaf(root, light2);
		l2Leaf.setCullMode(SpatialNode.CullMode.NEVER);
		l2Leaf.getLocalTransform().getTranslation().set(3f, -5f, -2f);
		BoundingSphere s2 = new BoundingSphere();
		s2.setCenter(new Vector3f());
		s2.setRadius(15f);
		l2Leaf.setInfluence(s2);
		
		manager.enableUpdate(new StateTree(sRoot));
		manager.setSpatialTree(new SpatialTree(top));
		return manager.getSpatialTree();
	}
	
	private Buffer[] makeDummyBuffers(TextureFormat format, TextureType type, int width) {
		int numM = (int)(Math.log(width) / Math.log(2)) + 1;
		//numM = 1;
		ByteBuffer[] d = new ByteBuffer[numM];
		for (int i = 0; i < d.length; i++) {
			d[i] = com.ferox.core.util.BufferUtil.newByteBuffer(format.getBufferSize(type, width, width), false, false);
			width = Math.max(1, width >> 1);
		}
		return d;
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
	
	public void checkInput() {
		if (input.isKeyPressed(KeyEvent.VK_ESCAPE)) 
			quit = true;
		if (input.isKeyPressed(KeyEvent.VK_R)) {
			System.out.println(stats);
			System.out.println(timer.getFrameRate());
		}
		if (input.isKeyPressed(KeyEvent.VK_LEFT))
			this.transform.getTranslation().x += -.1f;
		if (input.isKeyPressed(KeyEvent.VK_RIGHT))
			this.transform.getTranslation().x += .1f;
		if (input.isKeyPressed(KeyEvent.VK_S))
			this.transform.getTranslation().y += -.1f;
		if (input.isKeyPressed(KeyEvent.VK_W))
			this.transform.getTranslation().y += .1f;
		if (input.isKeyPressed(KeyEvent.VK_UP))
			this.transform.getTranslation().z += .1;
		if (input.isKeyPressed(KeyEvent.VK_DOWN))
			this.transform.getTranslation().z += -.1;
	}
	
	public Geometry buildCube() {
		return this.buildCube(1f);
	}
	
	public Geometry buildCube(float side) {
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
		
		for (int i = 0; i < v.length; i++)
			v[i] = v[i] * side / 2f;
		
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

	public void onInit(RenderManager manager) {
		long now = System.currentTimeMillis();
		this.base.update(manager);
		System.out.println("base creation = " + (System.currentTimeMillis() - now));
		now = System.currentTimeMillis();
		for (int i = 0; i < this.compressed.getNumMipmaps(); i++) {
			manager.getRenderContext().getTexture(this.base, Face.PX, i, this.compressed.getPositiveXMipmap(i), new Slice(this.compressed.getPositiveXMipmap(i)));
			manager.getRenderContext().getTexture(this.base, Face.NX, i, this.compressed.getNegativeXMipmap(i), new Slice(this.compressed.getPositiveXMipmap(i)));
			manager.getRenderContext().getTexture(this.base, Face.PY, i, this.compressed.getPositiveYMipmap(i), new Slice(this.compressed.getPositiveXMipmap(i)));
			manager.getRenderContext().getTexture(this.base, Face.NY, i, this.compressed.getNegativeYMipmap(i), new Slice(this.compressed.getPositiveXMipmap(i)));
			manager.getRenderContext().getTexture(this.base, Face.PZ, i, this.compressed.getPositiveZMipmap(i), new Slice(this.compressed.getPositiveXMipmap(i)));
			manager.getRenderContext().getTexture(this.base, Face.NZ, i, this.compressed.getNegativeZMipmap(i), new Slice(this.compressed.getPositiveXMipmap(i)));
		}
		System.out.println("fetch = " + (System.currentTimeMillis() - now));
		now = System.currentTimeMillis();
		this.compressed.update(manager);
		System.out.println("compressed creation = " + (System.currentTimeMillis() - now));
		
		this.elapsedTime = System.currentTimeMillis();
	}

	public void endFrame(RenderManager manager) {
		// do nothing
	}

	public void startFrame(RenderManager renderManager) {
		long now = System.currentTimeMillis();
		
		if (now - this.elapsedTime > 12) {
			Matrix3f rx = new Matrix3f();
			Matrix3f ry = new Matrix3f();
			rx.rotX((float)Math.PI / 512);
			ry.rotY((float)Math.PI / 512);
			//rx.mul(ry);
			this.transform.getRotation().mul(ry);
			this.elapsedTime = now;
		}
	}
}
