package com.ferox.test;

import java.awt.Component;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashSet;
import java.util.Set;

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
import com.ferox.core.states.atoms.GLSLAttribute;
import com.ferox.core.states.atoms.GLSLShaderObject;
import com.ferox.core.states.atoms.GLSLShaderProgram;
import com.ferox.core.states.atoms.GLSLUniform;
import com.ferox.core.states.atoms.Material;
import com.ferox.core.states.atoms.Texture;
import com.ferox.core.states.atoms.Texture2D;
import com.ferox.core.states.atoms.VertexArray;
import com.ferox.core.states.atoms.GLSLAttribute.AttributeType;
import com.ferox.core.states.atoms.GLSLShaderObject.GLSLType;
import com.ferox.core.states.atoms.GLSLUniform.UniformType;
import com.ferox.core.states.atoms.TextureData.MagFilter;
import com.ferox.core.states.atoms.TextureData.MinFilter;
import com.ferox.core.states.manager.GLSLShaderProgramManager;
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

public class AdvancedGLSLTest implements FrameListener, InitializationListener {
	private RenderManager manager;
	private JFrame frame;
	private ViewNode view;
	private SpatialLeaf cube;
	private SpatialBranch light;
	
	private GLSLShaderProgramManager shader;
	
	private TimeSmoother timer;
	private FrameStatistics stats;
	private InputManager input;
	
	public static void main(String[] args) {
		new AdvancedGLSLTest().run();
	}
	
	public AdvancedGLSLTest() {
		DisplayOptions op = new DisplayOptions();
		//op.setNumMultiSamples(4);
		this.manager = new RenderManager(new JOGLPassiveRenderContext(op));
		this.buildScene();
		this.manager.addFrameListener(this);
		this.manager.addInitializationListener(this);
		
		this.frame = new JFrame("Advanced GLSL Test");
		this.frame.setSize(640, 480);
		Component canvas = (Component)((OnscreenRenderSurface)this.manager.getRenderContext().getRenderSurface()).getRenderSurface();
		canvas.setEnabled(false);
		canvas.setFocusable(false);
		this.frame.add(canvas);
		this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		this.input = new InputManager(this.frame);
		this.input.setKeyBehavior(KeyEvent.VK_ESCAPE, InputManager.INITIAL_PRESS);
		this.input.setKeyBehavior(KeyEvent.VK_R, InputManager.INITIAL_PRESS);
	}
	
	public void run() {
		this.frame.setVisible(true);
		this.timer = new TimeSmoother();
		
		while(true) {
			long time = -System.currentTimeMillis();
			this.stats = this.manager.render();
			time += System.currentTimeMillis();
			this.timer.addSample(time);
			
			checkInput();
		}
	}
	
	private void buildScene() {
		SpatialTree scene = new SpatialTree();
		SpatialBranch root = new SpatialBranch();
		
		StateLeaf appearance = this.buildStates();
		this.cube = new SpatialLeaf(root);
		this.cube.setStates(appearance);
		this.cube.setModelBounds(new AxisAlignedBox());
		
		View camera = new View();
		camera.setPerspective(60f, 1.5f, 1f, 100f);
		
		this.view = new ViewNode(camera, root);
		this.view.getLocalTransform().setTranslation(0f, 0f, -16f);
		
		this.light = new SpatialBranch(root, 2);
		
		SpotLight light = new SpotLight(new Vector3f(0f, 0f, 1f), new float[] {.8f, .8f, .8f, 1f}, new float[] {1f, .4f, .4f, 1f});
		InfluenceLeaf inf = new InfluenceLeaf(this.light);
		inf.setState(light);
		inf.setInfluence(new BoundingSphere(25f));
		
		SpatialLeaf lightPos = new SpatialLeaf(this.light);
		StateLeaf l1 = new StateLeaf();
		l1.addStateManager(buildCube(.4f));
		l1.addStateManager(new MaterialManager(new Material(new float[] {5f, 0f, 0f, 1f})));
		
		appearance.getParent().add(l1);
		lightPos.setStates(l1);
		
		this.light.getLocalTransform().setTranslation(6f, 8f, -5f);
		
		scene.setRootNode(root);
		this.manager.setSpatialTree(scene);
		this.manager.setView(camera);
	}
	
	private StateLeaf buildStates() {
		StateTree stateTree = new StateTree();
		StateBranch root = new StateBranch();
		
		StateLeaf leaf = new StateLeaf(root);
		Texture2D diffuse = null;
		Texture2D normal = null;
		Texture2D specular = null;
		try {
			diffuse = TextureResourceManager.readTexture2D(new File("data/textures/wall_diffuse.png"), true, MinFilter.LINEAR, MagFilter.LINEAR);
			normal = TextureResourceManager.readTexture2D(new File("data/textures/wall_normal.png"), true, MinFilter.LINEAR, MagFilter.LINEAR);
			specular = TextureResourceManager.readTexture2D(new File("data/textures/wall_specular.png"), true, MinFilter.LINEAR, MagFilter.LINEAR);
		} catch (IOException ioe) {
			ioe.printStackTrace();
			System.exit(0);
		}
		
		TextureManager textures = new TextureManager();
		textures.setTexture(0, new Texture(diffuse));
		textures.setTexture(1, new Texture(normal));
		textures.setTexture(2, new Texture(specular));

		leaf.addStateManager(buildCube(4f));
		leaf.addStateManager(textures);
		leaf.addStateManager(new LightManager());
		leaf.addStateManager(new MaterialManager(new Material(new float[] {.7f, .7f, .7f, 1f})));
		//this.buildShaders();
		leaf.addStateManager(this.buildShaders());
		
		stateTree.setRootNode(root);
		this.manager.enableUpdate(stateTree);
		return leaf;
	}
	
	private GLSLShaderProgramManager buildShaders() {
		String[] vertexSource = new String[] {
			"uniform vec3 CAMERA_POSITION;",

			"attribute vec3 tangent;",
			"attribute vec3 bitangent;",
			
			"varying mat3 to_eye_space;",
			"varying vec3 half_vector;",
			"varying vec3 light_dir;",
				
			"void main() {",
				"gl_Position = gl_ModelViewMatrix * gl_Vertex;",
				"gl_TexCoord[0] = gl_TextureMatrix[0] * gl_MultiTexCoord0;",

				"mat3 tbnMatrix = mat3(tangent, bitangent, gl_Normal);",
				"to_eye_space = gl_NormalMatrix * tbnMatrix;",
				"light_dir = (gl_LightSource[0].position.xyz - gl_Position.xyz);",
				//"half_vector = CAMERA_POSITION;",
				"half_vector = gl_LightSource[0].halfVector.xyz;",

				"gl_Position = gl_ProjectionMatrix * gl_Position;",
			"}"
		};
		
		String[] fragmentSource = new String[] {
			"uniform sampler2D diffuse;",
			"uniform sampler2D normal;",
			"uniform sampler2D specular;", 
			
			"varying mat3 to_eye_space;",
			"varying vec3 half_vector;",
			"varying vec3 light_dir;",
			
			"void main() {",
				"vec3 norm = to_eye_space * (texture2D(normal, gl_TexCoord[0].st).grb * 2.0 - 1.0);",
				//"norm = to_eye_space * vec3(0.0, 0.0, 1.0);",
				"vec3 baseColor = texture2D(diffuse, gl_TexCoord[0].st).rgb;",
				//"float dist = length(lightvec);",
				"vec3 lightVector = normalize(light_dir);",
				"float nxDir = max(0.0, dot(norm, lightVector));",
				"vec4 diffuse = gl_LightSource[0].diffuse * nxDir;",
				
				"float specularPower = 0.0;",
				"if (nxDir != 0.0) {",
					"float nxHalf = max(0.0, dot(norm, half_vector));",
					"specularPower = pow(nxHalf, gl_FrontMaterial.shininess);",
				"}",
				"vec4 spec = (gl_LightSource[0].specular * vec4(texture2D(specular, gl_TexCoord[0].st).rgb, 1.0)) * specularPower;",
				//"gl_FragColor = spec;",
				"gl_FragColor = (diffuse * vec4(baseColor.rgb, 1.0)) + spec;",
			"}"
		};
		
		GLSLShaderObject v = new GLSLShaderObject(vertexSource, GLSLType.VERTEX);
		GLSLShaderObject f = new GLSLShaderObject(fragmentSource, GLSLType.FRAGMENT);
		
		GLSLShaderProgram prog = new GLSLShaderProgram(new GLSLShaderObject[] {v, f});
		GLSLShaderProgramManager manager = new GLSLShaderProgramManager(prog);
		
		manager.setUniform(new GLSLUniform("diffuse", UniformType.SAMPLER_2D, 1), 0);
		manager.setUniform(new GLSLUniform("normal", UniformType.SAMPLER_2D, 1), 1);
		manager.setUniform(new GLSLUniform("specular", UniformType.SAMPLER_2D, 1), 2);
		manager.setUniform(new GLSLUniform("CAMERA_POSITION", UniformType.VEC3F, 1), new Vector3f(0f, 0f, 0f));
		
		GLSLAttribute tan = new GLSLAttribute("tangent", AttributeType.VEC3F);
		tan.setBinding(0);
		GLSLAttribute bitan = new GLSLAttribute("bitangent", AttributeType.VEC3F);
		bitan.setBinding(1);
		
		Set<GLSLAttribute> attrs = new HashSet<GLSLAttribute>();
		attrs.add(tan);
		attrs.add(bitan);
		
		prog.setAvailableVertexAttributes(attrs);
		
		this.shader = manager;
		return manager;
	}
	
	private void checkInput() {
		if (input.isKeyPressed(KeyEvent.VK_ESCAPE))
			System.exit(0);
		if (input.isKeyPressed(KeyEvent.VK_R)) {
			System.out.println(this.stats);
			System.out.println(this.timer.getFrameRate());
		}
		
		if (input.isKeyPressed(KeyEvent.VK_UP))
			this.view.getLocalTransform().getTranslation().z += .01f;
		if (input.isKeyPressed(KeyEvent.VK_DOWN))
			this.view.getLocalTransform().getTranslation().z -= .01f;
		
		
		boolean leftPressed = input.isMousePressed(InputManager.leftClick);
		boolean rightPressed = input.isMousePressed(InputManager.rightClick) || (leftPressed && input.isKeyPressed(KeyEvent.VK_CONTROL));		

		int x = input.getMouseXChange();
		int y = input.getMouseYChange();
		
		if (rightPressed) {
			this.adjustTranslation(this.light, x, y);
		} else if (leftPressed) {
			this.adjustRotation(this.cube, x, y);
		}
	}
	
	static Transform identity = new Transform();
	static Matrix3f mx = new Matrix3f();
	static Matrix3f my = new Matrix3f();
	private void adjustRotation(SpatialNode node, int x, int y) {
		Transform world = new Transform();
		node.localToWorld(identity, world);		
		world.mul(this.view.getView().getInverseWorldTransform(), world);
	
		int pixelWidth = this.frame.getWidth();
		float cameraWidth = this.view.getView().getFrustumRight() - this.view.getView().getFrustumLeft();
		float sx = cameraWidth * x / pixelWidth;
		sx = (float)Math.atan(sx / this.view.getView().getFrustumNear()) * 3;
		
		int pixelHeight = this.frame.getHeight();
		float cameraHeight = this.view.getView().getFrustumTop() - this.view.getView().getFrustumBottom();
		float sy = cameraHeight * y / pixelHeight;
		sy = (float)Math.atan(sy / this.view.getView().getFrustumNear()) * 3;
		
		mx.rotX(sy);
		my.rotY(sx);
		mx.mul(my);
		world.getRotation().mul(mx, world.getRotation());
		
		world.mul(this.view.getView().getWorldTransform(), world);		
		node.setWorldTransform(world);
	}
	
	private void adjustTranslation(SpatialNode node, int x, int y) {
		Transform world = new Transform();
		node.localToWorld(identity, world);		
		world.mul(this.view.getView().getInverseWorldTransform(), world);
		
		Vector3f trans = world.getTranslation();

		int pixelWidth = this.frame.getWidth();
		float cameraWidth = this.view.getView().getFrustumRight() - this.view.getView().getFrustumLeft();
		float sx = cameraWidth * x / pixelWidth;
		sx = sx * trans.z / this.view.getView().getFrustumNear();
		
		int pixelHeight = this.frame.getHeight();
		float cameraHeight = this.view.getView().getFrustumTop() - this.view.getView().getFrustumBottom();
		float sy = cameraHeight * y / pixelHeight;
		sy = sy * trans.z / this.view.getView().getFrustumNear();
		
		trans.x = trans.x - sx;
		trans.y = trans.y + sy;
		
		world.mul(this.view.getView().getWorldTransform(), world);		
		node.setWorldTransform(world);
	}
	
	private Geometry buildCube(float side) {
		float[] v = new float[72];
		float[] n = new float[72];
		float[] t = new float[48];
		float[] tan = new float[72];
		float[] btan = new float[72];
		
		// front
		v[0] = 1f; v[1] = 1f; v[2] = 1f; n[0] = 0f; n[1] = 0f; n[2] = 1f; t[0] = 1f; t[1] = 1f;
		v[3] = -1f; v[4] = 1f; v[5] = 1f; n[3] = 0f; n[4] = 0f; n[5] = 1f; t[2] = 0f; t[3] = 1f;
		v[6] = -1f; v[7] = -1f; v[8] = 1f; n[6] = 0f; n[7] = 0f; n[8] = 1f; t[4] = 0f; t[5] = 0f;
		v[9] = 1f; v[10] = -1f; v[11] = 1f; n[9] = 0f; n[10] = 0f; n[11] = 1f; t[6] = 1f; t[7] = 0f;
		//back
		v[12] = -1f; v[13] = -1f; v[14] = -1f; n[12] = 0f; n[13] = 0f; n[14] = -1f; t[8] = 1f; t[9] = 1f;
		v[21] = 1f; v[22] = -1f; v[23] = -1f; n[21] = 0f; n[22] = 0f; n[23] = -1f; t[10] = 0f; t[11] = 1f;
		v[18] = 1f; v[19] = 1f; v[20] = -1f; n[18] = 0f; n[19] = 0f; n[20] = -1f; t[12] = 0f; t[13] = 0f;
		v[15] = -1f; v[16] = 1f; v[17] = -1f; n[15] = 0f; n[16] = 0f; n[17] = -1f; t[14] = 1f; t[15] = 0f;
		//right
		v[24] = 1f; v[25] = 1f; v[26] = -1f; n[24] = 1f; n[25] = 0f; n[26] = 0f; t[16] = 1f; t[17] = 1f;
		v[27] = 1f; v[28] = 1f; v[29] = 1f; n[27] = 1f; n[28] = 0f; n[29] = 0f; t[18] = 0f; t[19] = 1f;
		v[30] = 1f; v[31] = -1f; v[32] = 1f; n[30] = 1f; n[31] = 0f; n[32] = 0f; t[20] = 0f; t[21] = 0f;
		v[33] = 1f; v[34] = -1f; v[35] = -1f; n[33] = 1f; n[34] = 0f; n[35] = 0f; t[22] = 1f; t[23] = 0f;
		//left
		v[36] = -1f; v[37] = -1f; v[38] = 1f; n[36] = -1f; n[37] = 0f; n[38] = 0f; t[24] = 1f; t[25] = 1f;
		v[45] = -1f; v[46] = -1f; v[47] = -1f; n[45] = -1f; n[46] = 0f; n[47] = 0f; t[26] = 0f; t[27] = 1f;
		v[42] = -1f; v[43] = 1f; v[44] = -1f; n[42] = -1f; n[43] = 0f; n[44] = 0f; t[28] = 0f; t[29] = 0f;
		v[39] = -1f; v[40] = 1f; v[41] = 1f; n[39] = -1f; n[40] = 0f; n[41] = 0f; t[30] = 1f; t[31] = 0f;
		//top
		v[48] = -1f; v[49] = 1f; v[50] = -1f; n[48] = 0f; n[49] = 1f; n[50] = 0f; t[32] = 1f; t[33] = 1f;
		v[57] = 1f; v[58] = 1f; v[59] = -1f; n[57] = 0f; n[58] = 1f; n[59] = 0f; t[34] = 0f; t[35] = 1f;
		v[54] = 1f; v[55] = 1f; v[56] = 1f; n[54] = 0f; n[55] = 1f; n[56] = 0f; t[36] = 0f; t[37] = 0f;
		v[51] = -1f; v[52] = 1f; v[53] = 1f; n[51] = 0f; n[52] = 1f; n[53] = 0f; t[38] = 1f; t[39] = 0f;
		//bottom
		v[60] = 1f; v[61] = -1f; v[62] = 1f; n[60] = 0f; n[61] = -1f; n[62] = 0f; t[40] = 1f; t[41] = 1f;
		v[63] = -1f; v[64] = -1f; v[65] = 1f; n[63] = 0f; n[64] = -1f; n[65] = 0f; t[42] = 0f; t[43] = 1f;
		v[66] = -1f; v[67] = -1f; v[68] = -1f; n[66] = 0f; n[67] = -1f; n[68] = 0f; t[44] = 0f; t[45] = 0f;
		v[69] = 1f; v[70] = -1f; v[71] = -1f; n[69] = 0f; n[70] = -1f; n[71] = 0f; t[46] = 1f; t[47] = 0f;
		
		for (int i = 0; i < v.length; i++)
			v[i] = v[i] * side / 2f;
		
		int[] i = new int[24];
		for (int u = 0; u < 24; u++) {
			i[u] = u;
		}
		
		computeTangentBiTangent(v, t, i, tan, btan);
		cleanTangentBiTangent(n, tan, btan);
		
		/*for (int u = 0; u < 6; u++) {
			System.out.println("Polygon: " + u);
			int i1 = i[u*4];
			int i2 = i[u*4+1];
			int i3 = i[u*4+2];
			int i4 = i[u*4+3];
			System.out.println("Vertex 0: <" + v[i1*3] + ", " + v[i1*3+1] + ", " + v[i1*3+2] + "> <" + n[i1*3] + ", " + n[i1*3+1] + ", " + n[i1*3+2] + "> <" + t[i1*2] + ", " + t[i1*2+1] + "> <" + tan[i1*3] + ", " + tan[i1*3+1] + ", " + tan[i1*3+2] + "> <" + btan[i1*3] + ", " + btan[i1*3+1] + ", " + btan[i1*3+2] + ">");
			System.out.println("Vertex 1: <" + v[i2*3] + ", " + v[i2*3+1] + ", " + v[i2*3+2] + "> <" + n[i2*3] + ", " + n[i2*3+1] + ", " + n[i2*3+2] + "> <" + t[i2*2] + ", " + t[i2*2+1] + "> <" + tan[i2*3] + ", " + tan[i2*3+1] + ", " + tan[i2*3+2] + "> <" + btan[i2*3] + ", " + btan[i2*3+1] + ", " + btan[i2*3+2] + ">");
			System.out.println("Vertex 2: <" + v[i3*3] + ", " + v[i3*3+1] + ", " + v[i3*3+2] + "> <" + n[i3*3] + ", " + n[i3*3+1] + ", " + n[i3*3+2] + "> <" + t[i3*2] + ", " + t[i3*2+1] + "> <" + tan[i3*3] + ", " + tan[i3*3+1] + ", " + tan[i3*3+2] + "> <" + btan[i3*3] + ", " + btan[i3*3+1] + ", " + btan[i3*3+2] + ">");
			System.out.println("Vertex 3: <" + v[i4*3] + ", " + v[i4*3+1] + ", " + v[i4*3+2] + "> <" + n[i4*3] + ", " + n[i4*3+1] + ", " + n[i4*3+2] + "> <" + t[i4*2] + ", " + t[i4*2+1] + "> <" + tan[i4*3] + ", " + tan[i4*3+1] + ", " + tan[i4*3+2] + "> <" + btan[i4*3] + ", " + btan[i4*3+1] + ", " + btan[i4*3+2] + ">");
			System.out.println("");
		}*/
		
		FloatBuffer vb = BufferUtil.newFloatBuffer(v.length);
		vb.put(v).rewind();
		FloatBuffer nb = BufferUtil.newFloatBuffer(n.length);
		nb.put(n).rewind();
		FloatBuffer tb = BufferUtil.newFloatBuffer(t.length);
		tb.put(t).rewind();
		FloatBuffer tanb = BufferUtil.newFloatBuffer(tan.length);
		tanb.put(tan).rewind();
		FloatBuffer btanb = BufferUtil.newFloatBuffer(btan.length);
		btanb.put(btan).rewind();
		
		IntBuffer ib = BufferUtil.newIntBuffer(i.length);
		ib.put(i).rewind();
		
		BufferData vbd = new BufferData(vb, BufferData.DataType.FLOAT, vb.capacity(), BufferData.BufferTarget.ARRAY_BUFFER);
		BufferData nbd = new BufferData(nb, BufferData.DataType.FLOAT, nb.capacity(), BufferData.BufferTarget.ARRAY_BUFFER);
		BufferData tbd = new BufferData(tb, BufferData.DataType.FLOAT, tb.capacity(), BufferData.BufferTarget.ARRAY_BUFFER);
		BufferData tanbd = new BufferData(tanb, BufferData.DataType.FLOAT, tanb.capacity(), BufferData.BufferTarget.ARRAY_BUFFER);
		BufferData btanbd = new BufferData(btanb, BufferData.DataType.FLOAT, btanb.capacity(), BufferData.BufferTarget.ARRAY_BUFFER);
		
		BufferData ibd = new BufferData(ib, BufferData.DataType.UNSIGNED_INT, ib.capacity(), BufferData.BufferTarget.ELEMENT_BUFFER);
		
		VertexArray iva = new VertexArray(ibd, 1);
		
		Geometry geom = new Geometry(new VertexArray(vbd, 3), new VertexArray(nbd, 3), iva, Geometry.PolygonType.QUADS);
		geom.setTexCoords(new VertexArray(tbd, 2), 0);
		geom.setTexCoords(geom.getTexCoords(0), 1);
		geom.setTexCoords(geom.getTexCoords(0), 2);

		geom.setVertexAttributes(new VertexArray(tanbd, 3), 0);
		geom.setVertexAttributes(new VertexArray(btanbd, 3), 1);
		return geom;
	}

	private static void cleanTangentBiTangent(float[] normals, float[] tan, float[] bitan) {
		Vector3f tp = new Vector3f();
		Vector3f bp = new Vector3f();
		Vector3f normal = new Vector3f();
		Vector3f tangent = new Vector3f();
		Vector3f bitangent = new Vector3f();
		
		float dot;
		for (int i = 0; i < normals.length / 3; i++) {
			normal.set(normals[i*3], normals[i*3+1], normals[i*3+2]);
			tangent.set(tan[i*3], tan[i*3+1], tan[i*3+2]);
			bitangent.set(bitan[i*3], bitan[i*3+1], bitan[i*3+2]);

			dot = normal.dot(tangent);
			tp.set(tangent);
			tangent.scale(-dot, normal);
			tp.add(tangent);
			tp.normalize();
			
			tan[i*3] = tp.x;
			tan[i*3+1] = tp.y;
			tan[i*3+2] = tp.z;
			
			dot = tp.dot(bitangent);
			tangent.scale(-dot, tp);
			dot = normal.dot(bitangent);
			tp.scale(-dot, normal);
			
			bp.set(bitangent);
			bp.add(tp);
			bp.add(tangent);
			bp.normalize();
			
			bitan[i*3] = bp.x;
			bitan[i*3+1] = bp.y;
			bitan[i*3+2] = bp.z;
		}
	}
	
	private static void computeTangentBiTangent(float[] verts, float[] texcoords, int[] indices, float[] tan, float[] bitan) {
		for (int i = 0; i < indices.length / 4; i++) {
			computeForTriangle(indices[i*4], indices[i*4+1], indices[i*4+3], verts, texcoords, tan, bitan);
			computeForTriangle(indices[i*4+1], indices[i*4], indices[i*4+2], verts, texcoords, tan, bitan);
			computeForTriangle(indices[i*4+2], indices[i*4+1], indices[i*4+3], verts, texcoords, tan, bitan);
			computeForTriangle(indices[i*4+3], indices[i*4], indices[i*4+2], verts, texcoords, tan, bitan);
		}
	}
	
	private static void computeForTriangle(int v0, int v1, int v2, float[] verts, float[] texcoords, float[] tan, float[] bitan) {
		float s1, t1;
		float s2, t2;
		
		Vector3f q1 = new Vector3f();
		Vector3f q2 = new Vector3f();
		
		q1.x = verts[v1*3] - verts[v0*3];
		q1.y = verts[v1*3 + 1] - verts[v0*3 + 1];
		q1.z = verts[v1*3 + 2] - verts[v0*3 + 2];
		
		q2.x = verts[v2*3] - verts[v0*3];
		q2.y = verts[v2*3 + 1] - verts[v0*3 + 1];
		q2.z = verts[v2*3 + 2] - verts[v0*3 + 2];
		
		s1 = texcoords[v1*2] - texcoords[v0*2];
		t1 = texcoords[v1*2+1] - texcoords[v0*2+1];
		
		s2 = texcoords[v2*2] - texcoords[v0*2];
		t2 = texcoords[v2*2+1] - texcoords[v0*2+1];
		
		float scale = 1 / (s1*t2 - s2*t1);
		
		
		// as of yet, unnormalized or guaranteed orthogonal
		tan[v0*3] = scale * (t2 * q1.x - t1 * q2.x);
		tan[v0*3+1] = scale * (t2 * q1.y - t1 * q2.y);
		tan[v0*3+2] = scale * (t2 * q1.z - t1 * q2.z);
		
		bitan[v0*3] = scale * (-s2 * q1.x + s1 * q2.x);
		bitan[v0*3+1] = scale * (-s2 * q1.y + s1 * q2.y);
		bitan[v0*3+2] = scale * (-s2 * q1.z + s1 * q2.z);
	}
	
	public void endFrame(RenderManager manager) {
		// do nothing
	}

	public void startFrame(RenderManager renderManager) {
		Vector3f camPos = renderManager.getRenderPass(0).getView().getLocation();
		this.shader.setUniform(this.shader.getStateAtom().getUniformByName("CAMERA_POSITION"), camPos);
	}

	public void onInit(RenderManager manager) {
		GLSLShaderProgram program = this.shader.getStateAtom();
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
		
		Set<GLSLAttribute> aa = program.getAvailableVertexAttributes();
		if (aa != null) {
			System.out.println("Attributes: " + aa.size());
			for (GLSLAttribute a: aa) {
				System.out.println(a.getName() + " " + a.getType() + " " + a.getBinding());
			}
		}
	}
}
