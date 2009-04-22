package com.ferox.scene;

import java.awt.event.KeyEvent;
import java.io.IOException;

import org.openmali.vecmath.Matrix3f;
import org.openmali.vecmath.Vector3f;

import com.ferox.BasicApplication;
import com.ferox.InputManager;
import com.ferox.math.BoundSphere;
import com.ferox.math.Color;
import com.ferox.math.Transform;
import com.ferox.renderer.Renderer;
import com.ferox.resource.BufferData;
import com.ferox.resource.Geometry;
import com.ferox.resource.geometry.Box;
import com.ferox.resource.geometry.BufferedGeometryDescriptor;
import com.ferox.resource.geometry.TangentGenerator;
import com.ferox.resource.geometry.Teapot;
import com.ferox.resource.geometry.VertexArray;
import com.ferox.resource.geometry.VertexArrayGeometry;
import com.ferox.resource.glsl.GlslProgram;
import com.ferox.resource.glsl.GlslUniform;
import com.ferox.resource.glsl.GlslVertexAttribute;
import com.ferox.resource.glsl.GlslUniform.ValueUpdatePolicy;
import com.ferox.resource.glsl.GlslVertexAttribute.AttributeType;
import com.ferox.resource.texture.TextureImage;
import com.ferox.resource.texture.loader.TextureLoader;
import com.ferox.state.Appearance;
import com.ferox.state.GlslShader;
import com.ferox.state.LightReceiver;
import com.ferox.state.Material;
import com.ferox.state.MultiTexture;
import com.ferox.state.Texture;

public class GlslTest extends BasicApplication {
	public static final boolean DEBUG = false;
	
	private Group light;
	private Shape cube;
	
	public static void main(String[] args) {
		new GlslTest(DEBUG).run();
	}
	
	public GlslTest(boolean debug) {
		super(debug);
	}

	@Override
	protected SceneElement buildScene(Renderer renderer, ViewNode view) {
		Group root = new Group();
		
		view.getLocalTransform().getTranslation().z = 20f;
		root.add(view);
		
		Geometry cube = this.build(new Teapot(3f));
		renderer.requestUpdate(cube, true);
		Appearance app = this.createGlslAppearance(renderer);
		
		this.cube = new Shape(cube, app);
		root.add(this.cube);
		
		SpotLight light = new SpotLight(new Color(.5f, .5f, .5f), new Color(1f, 1f, 1f), new Color());
		light.setLocalBounds(new BoundSphere(20f));

		Shape lightCube = new Shape(new VertexArrayGeometry(new Box(.5f)), new Appearance(new Material(light.getDiffuse())));
		renderer.requestUpdate(lightCube.getGeometry(), true);
		
		this.light = new Group(2);
		this.light.getLocalTransform().getTranslation().set(-4f, 4f, 10f);
		this.light.add(light);
		this.light.add(lightCube);
		
		root.add(this.light);
		
		this.window.setVSyncEnabled(false);
		
		return root;
	}
	
	private Appearance createGlslAppearance(Renderer renderer) {
		GlslProgram program = this.createProgram(renderer);
		
		GlslShader shader = new GlslShader(program);
		program.getUniforms().get("diffuse").setValueUpdatePolicy(ValueUpdatePolicy.MANUAL);
		shader.setUniform(program.getUniforms().get("diffuse"), new int[] {0});
		program.getUniforms().get("specular").setValueUpdatePolicy(ValueUpdatePolicy.MANUAL);
		shader.setUniform(program.getUniforms().get("specular"), new int[] {1});
		program.getUniforms().get("normal").setValueUpdatePolicy(ValueUpdatePolicy.MANUAL);
		shader.setUniform(program.getUniforms().get("normal"), new int[] {2});
		
		
		TextureImage diffuse = null;
		TextureImage specular = null;
		TextureImage normal = null;
		
		try{
			diffuse = TextureLoader.readTexture(this.getClass().getClassLoader().getResource("data/textures/wall_diffuse.png"));
			specular = TextureLoader.readTexture(this.getClass().getClassLoader().getResource("data/textures/wall_specular.png"));
			normal = TextureLoader.readTexture(this.getClass().getClassLoader().getResource("data/textures/wall_normal.png"));
			
			renderer.requestUpdate(diffuse, true);
			renderer.requestUpdate(specular, true);
			renderer.requestUpdate(normal, true);
		} catch (IOException io) {
			// fail
			throw new RuntimeException(io);
		}
		
		MultiTexture textures = new MultiTexture(new Texture(diffuse), new Texture(specular), new Texture(normal));
		
		return new Appearance(textures, shader, new LightReceiver());
	}
	
	private GlslProgram createProgram(Renderer renderer) {
		String[] vertexShader = {
				"attribute vec3 tangent;",
				"attribute vec3 bitangent;",

				"varying vec3 eye_dir;",
				"varying vec3 light_dir;",

				"varying vec3 tan;",
				"varying vec3 bitan;",
				"varying vec3 nm;",

				"void main() {",
					"gl_Position = gl_ModelViewMatrix * gl_Vertex;",
					"gl_TexCoord[0] = gl_TextureMatrix[0] * gl_MultiTexCoord0;",

					"tan = tangent;",
					"bitan = bitangent;",
					"nm = gl_Normal;",

					"light_dir = gl_LightSource[0].position.xyz - gl_Position.xyz;",
					//"half_vector = gl_LightSource[0].halfVector.xyz;",
					"eye_dir = -gl_Position.xyz;",
					
					"gl_Position = gl_ProjectionMatrix * gl_Position;",
				"}"
			};

		String[] fragmentShader = {
			"uniform sampler2D diffuse;",
			"uniform sampler2D normal;",
			"uniform sampler2D specular;", 

			"varying vec3 eye_dir;",
			"varying vec3 light_dir;",

			"varying vec3 tan;",
			"varying vec3 bitan;",
			"varying vec3 nm;",

			"void main() {",
				"mat3 to_eye = gl_NormalMatrix * mat3(tan, bitan, nm);",
				"vec3 norm = to_eye * (texture2D(normal, gl_TexCoord[0].st).rgb * 2.0 - 1.0);",
				"vec3 baseColor = texture2D(diffuse, gl_TexCoord[0].st).rgb;",
				"vec3 lightVector = normalize(light_dir);",
				"float nxDir = max(.1, dot(norm, lightVector));",
				"vec4 diffuse = gl_LightSource[0].diffuse * nxDir;",

				"float specularPower = 0.0;",
				"if (nxDir > 0.0) {",
					"lightVector = normalize(eye_dir + light_dir);",
					"float nxHalf = max(0.0, dot(norm, lightVector));",
					"specularPower = min(1.0, pow(nxHalf, gl_FrontMaterial.shininess));",
				"}",

				"vec4 spec = (gl_LightSource[0].specular * vec4(texture2D(specular, gl_TexCoord[0].st).rgb, 1.0)) * specularPower;",
				"gl_FragColor = (diffuse * vec4(baseColor.rgb, 1.0)) + spec;",
			"}"
		};

		GlslProgram program = new GlslProgram(vertexShader, fragmentShader);
		program.bindAttribute("tangent", AttributeType.VEC3F, 1);
		program.bindAttribute("bitangent", AttributeType.VEC3F, 2);

		renderer.requestUpdate(program, true);
		renderer.flushRenderer(null);

		System.out.println(renderer.getStatus(program));
		System.out.println(renderer.getStatusMessage(program));

		for (GlslUniform u: program.getUniforms().values()) {
			System.out.println("Uniform: " + u.getName() + " " + u.getType() + " " + u.getLength());
		}

		for (GlslVertexAttribute a: program.getAttributes().values()) {
			System.out.println("Attribute: " + a.getName() + " " + a.getType() + " " + a.getBindingSlot());
		}

		return program;
	}
	
	@Override
	protected void handleInput(InputManager input, float dt) {
		super.handleInput(input, dt);
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
	private void adjustRotation(Node node, int x, int y) {
		Transform world = new Transform();
		node.localToWorld(identity, world, false);		
		world.mul(this.view.getView().getViewTransform(), world);
	
		int pixelWidth = this.window.getWidth();
		float cameraWidth = this.view.getView().getFrustumRight() - this.view.getView().getFrustumLeft();
		float sx = cameraWidth * x / pixelWidth;
		sx = (float)Math.atan(sx / this.view.getView().getFrustumNear()) * 3;
		
		int pixelHeight = this.window.getHeight();
		float cameraHeight = this.view.getView().getFrustumTop() - this.view.getView().getFrustumBottom();
		float sy = cameraHeight * y / pixelHeight;
		sy = (float)Math.atan(sy / this.view.getView().getFrustumNear()) * 3;
		
		mx.rotX(sy);
		my.rotY(sx);
		mx.mul(my);
		world.getRotation().mul(mx, world.getRotation());
		
		world.inverseMul(this.view.getView().getViewTransform(), world);		
		node.setWorldTransform(world);
	}
	
	private void adjustTranslation(Node node, int x, int y) {
		Transform world = new Transform();
		node.localToWorld(identity, world, false);		
		world.mul(this.view.getView().getViewTransform(), world);
		
		Vector3f trans = world.getTranslation();

		int pixelWidth = this.window.getWidth();
		float cameraWidth = this.view.getView().getFrustumRight() - this.view.getView().getFrustumLeft();
		float sx = cameraWidth * x / pixelWidth;
		sx = sx * trans.z / this.view.getView().getFrustumNear();
		
		int pixelHeight = this.window.getHeight();
		float cameraHeight = this.view.getView().getFrustumTop() - this.view.getView().getFrustumBottom();
		float sy = cameraHeight * y / pixelHeight;
		sy = sy * trans.z / this.view.getView().getFrustumNear();
		
		trans.x = trans.x - sx;
		trans.y = trans.y + sy;
		
		world.inverseMul(this.view.getView().getViewTransform(), world);
		node.setWorldTransform(world);
	}
	
	/*
	 * Junky code to build a cube with extra vertex attributes.
	 */
	
	private Geometry build(BufferedGeometryDescriptor b) {
		BufferData verts = b.getVertices();
		BufferData tcs = b.getTextureCoordinates();
		
		float[] tan = new float[verts.getCapacity()];
		float[] bitan = new float[verts.getCapacity()];
		TangentGenerator.generate((float[]) verts.getData(), (float[]) tcs.getData(), (int[]) b.getIndices().getData(), tan, bitan);
		
		VertexArrayGeometry box = new VertexArrayGeometry(b);
		box.setVertexAttributes(1, new BufferData(tan, false), new VertexArray(3));
		box.setVertexAttributes(2, new BufferData(bitan, false), new VertexArray(3));
		
		return box;
	}
}
