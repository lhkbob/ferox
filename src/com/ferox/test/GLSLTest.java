package com.ferox.test;

import java.awt.Component;
import java.awt.event.KeyEvent;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Set;

import javax.swing.JFrame;

import org.openmali.vecmath.Matrix3f;
import org.openmali.vecmath.Vector3f;
import org.openmali.vecmath.Vector4f;

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
import com.ferox.core.scene.states.DirectionLight;
import com.ferox.core.scene.states.SpotLight;
import com.ferox.core.states.StateBranch;
import com.ferox.core.states.StateLeaf;
import com.ferox.core.states.StateTree;
import com.ferox.core.states.atoms.BufferData;
import com.ferox.core.states.atoms.GLSLShaderObject;
import com.ferox.core.states.atoms.GLSLShaderProgram;
import com.ferox.core.states.atoms.GLSLUniform;
import com.ferox.core.states.atoms.Material;
import com.ferox.core.states.atoms.VertexArray;
import com.ferox.core.states.atoms.GLSLShaderObject.GLSLType;
import com.ferox.core.states.atoms.GLSLUniform.UniformType;
import com.ferox.core.states.manager.GLSLShaderProgramManager;
import com.ferox.core.states.manager.Geometry;
import com.ferox.core.states.manager.LightManager;
import com.ferox.core.states.manager.MaterialManager;
import com.ferox.core.system.DisplayOptions;
import com.ferox.core.system.OnscreenRenderSurface;
import com.ferox.core.util.InputManager;
import com.ferox.core.util.TimeSmoother;
import com.ferox.impl.jsr231.JOGLPassiveRenderContext;
import com.sun.opengl.util.BufferUtil;

public class GLSLTest implements FrameListener, InitializationListener {
	private RenderManager manager;
	private JFrame window;
	private SpatialTree scene;
	private View view;
	private Transform transform;
	
	private InputManager input;
	private boolean quit;
	private TimeSmoother timer;
	private FrameStatistics stats;
	private GLSLShaderProgramManager program;
	private GLSLShaderProgramManager program2;
	
	private long elapsedTime;
	
	public static void main(String[] args) {
		new GLSLTest().run();
	}
	
	private GLSLTest() {
		this.manager = new RenderManager(new JOGLPassiveRenderContext(new DisplayOptions()));
		
		this.window = new JFrame("GLSLTest");
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
		this.manager.addFrameListener(this);
		this.manager.addInitializationListener(this);
		
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
		
		// Basic cube
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
		
		// GLSL cube
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
		
		buildProgram();
		
		MaterialManager white = new MaterialManager(new Material(new float[] {1f, 1f, 1f, 1f}, new float[] {1f, 0f, 0f, 1f}, 50));
		MaterialManager red = new MaterialManager(new Material(new float[] {1f, 0f, 0f, 1f}));
		MaterialManager green = new MaterialManager(new Material(new float[] {0f, 1f, 0f, 1f}));
		
		sl1.addStateManager(white);
		sl1.addStateManager(this.program2);
		sl2.addStateManager(white);
		sl2.addStateManager(this.program);
		hl1.addStateManager(green);
		hl2.addStateManager(red);
		
		LightManager lm = new LightManager();
		lm.setGlobalAmbientLight(new float[] {.5f, .5f, .5f, 1f});
		lm.setLocalViewer(true);
		sl1.addStateManager(lm);
		sl2.addStateManager(lm);
		
		DirectionLight light = new DirectionLight(new Vector3f(0f, 1f, -1f), new float[] {1f, 1f, 1f, 1f});
		//light.setSpotCutoffAngle(45);
		InfluenceLeaf l = new InfluenceLeaf(root, light);
		l.setCullMode(SpatialNode.CullMode.NEVER);
		l.getLocalTransform().getTranslation().set(2f, 5f, 2f);
		BoundingSphere s = new BoundingSphere();
		s.setCenter(new Vector3f());
		s.setRadius(15f);
		l.setInfluence(s);
		
		DirectionLight light2 = new DirectionLight(new Vector3f(0f, -1f, 1f), new float[] {1f, 0f, 1f, 1f});
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
	
	private GLSLShaderProgramManager buildProgram() {
		String[] vSource = new String[] {
				"uniform vec4 mat_diff;",
				"uniform vec3 mat_amb;",
				"void main() {",
					"vec3 normal, lightDir;",
					"vec4 diffuse;",
					"float NdotL;",
					"normal = normalize(gl_NormalMatrix * gl_Normal);",
					"lightDir = normalize(vec3(gl_LightSource[0].position));",
					"NdotL = max(dot(normal, lightDir), 0.0);",
					"diffuse = mat_diff * gl_LightSource[0].diffuse;",
					"gl_FrontColor = NdotL * diffuse + 0.4 * vec4(mat_amb, 1.0);",
					"gl_Position = ftransform();",
				"}"
		};
		String[] fSource = new String[] {
				"void main() {",
					"gl_FragColor = gl_Color;",
				"}"
		};
		GLSLShaderObject vert = new GLSLShaderObject(vSource, GLSLType.VERTEX);
		GLSLShaderObject frag = new GLSLShaderObject(fSource, GLSLType.FRAGMENT);
		
		GLSLShaderProgram program = new GLSLShaderProgram(new GLSLShaderObject[] {vert, frag});
		GLSLShaderProgramManager man = new GLSLShaderProgramManager(program);
		man.setUniform(new GLSLUniform("mat_diff", UniformType.VEC4F, 1), new Vector4f(1f, 0f, 0f, 1f));
		man.setUniform(new GLSLUniform("mat_amb", UniformType.VEC3F, 1), new Vector3f(1f, 1f, 1f));
		this.program = man;
		
		man = new GLSLShaderProgramManager(program);
		man.setUniform(new GLSLUniform("mat_diff", UniformType.VEC4F, 1), new Vector4f(1f, 0f, 0f, 1f));
		man.setUniform(new GLSLUniform("mat_amb", UniformType.VEC3F, 1), new Vector3f(1f, 0f, 0f));
		this.program2 = man;
		return man;
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
			rx.mul(ry);
			this.transform.getRotation().mul(ry);
			
			Vector4f v = (Vector4f)this.program.getUniform(this.program.getStateAtom().getUniformByName("mat_diff"));
			v.x = (float)Math.sin(this.elapsedTime * Math.PI / 512);
			v.y = (float)Math.cos(this.elapsedTime * Math.PI / 1024);
			v.z = (float)Math.tan(this.elapsedTime * Math.PI / 768 + Math.PI / 2);
			
			Vector3f m = (Vector3f)this.program2.getUniform(this.program.getStateAtom().getUniformByName("mat_amb"));
			m.x = (float)Math.sin(this.elapsedTime * Math.PI / 512);
			m.y = (float)Math.cos(this.elapsedTime * Math.PI / 1024);
			m.z = (float)Math.tan(this.elapsedTime * Math.PI / 768 + Math.PI / 2);
			
			this.elapsedTime = now;
		}
	}

	public void onInit(RenderManager manager) {
		GLSLShaderProgram program = this.program.getStateAtom();
		program.update(manager);
		System.out.println(program.isCompiled() + " " + program.getInfoLog());
		for (int i = 0; i < program.getShaders().length; i++) {
			GLSLShaderObject e = program.getShaders()[i];
			System.out.println(e.getShaderType() + " " + e.isCompiled() + " " + e.getInfoLog());
		}
		Set<GLSLUniform> au = program.getAvailableUniforms();
		if (au != null) {
			System.out.println("Uniforms: " + au.size());
			for (GLSLUniform o: au) {
				System.out.println(o.getName() + " " + o.getType() + " " + o.getSize());
			}
		}
		
		
	}
}
