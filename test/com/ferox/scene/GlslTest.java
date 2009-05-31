package com.ferox.scene;

import java.awt.event.KeyEvent;
import java.io.IOException;

import org.openmali.vecmath.Matrix3f;
import org.openmali.vecmath.Vector3f;

import com.ferox.BasicApplication;
import com.ferox.InputManager;
import com.ferox.effect.GlobalLighting;
import com.ferox.effect.GlslShader;
import com.ferox.effect.Material;
import com.ferox.effect.MultiTexture;
import com.ferox.effect.Texture;
import com.ferox.math.Color;
import com.ferox.math.Transform;
import com.ferox.renderer.Framework;
import com.ferox.resource.Geometry;
import com.ferox.resource.GlslProgram;
import com.ferox.resource.GlslUniform;
import com.ferox.resource.GlslVertexAttribute;
import com.ferox.resource.IndexedArrayGeometry;
import com.ferox.resource.TextureImage;
import com.ferox.resource.Geometry.CompileType;
import com.ferox.resource.GlslUniform.ValueUpdatePolicy;
import com.ferox.resource.GlslVertexAttribute.AttributeType;
import com.ferox.resource.IndexedArrayGeometry.VectorBuffer;
import com.ferox.resource.TextureImage.TextureWrap;
import com.ferox.util.geom.Box;
import com.ferox.util.geom.TangentGenerator;
import com.ferox.util.geom.Teapot;
import com.ferox.util.texture.loader.TextureLoader;

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
	protected Node buildScene(Framework renderer, ViewNode view) {
		Group root = new Group();

		view.getLocalTransform().getTranslation().z = 20f;
		root.add(view);

		Geometry cube = build(new Teapot(3f, CompileType.VBO_STATIC));
		renderer.requestUpdate(cube, true);
		Appearance app = createGlslAppearance(renderer);

		this.cube = new Shape(cube, app);
		root.add(this.cube);

		SpotLightNode light =
			new SpotLightNode(new Color(.5f, .5f, .5f), new Color(1f, 1f, 1f),
				new Color());
		light.setEffectRadius(20f);

		Shape lightCube =
			new Shape(new Box(.5f, CompileType.NONE), new Appearance()
				.setMaterial(new Material(light.getDiffuse())));
		renderer.requestUpdate(lightCube.getGeometry(), true);

		this.light = new Group(2);
		this.light.getLocalTransform().getTranslation().set(-4f, 4f, 10f);
		this.light.add(light);
		this.light.add(lightCube);

		root.add(this.light);

		window.setVSyncEnabled(false);

		return root;
	}

	private Appearance createGlslAppearance(Framework renderer) {
		GlslProgram program = createProgram(renderer);

		GlslShader shader = new GlslShader(program);
		program.getUniforms().get("diffuse").setValueUpdatePolicy(
			ValueUpdatePolicy.MANUAL);
		shader
			.setUniform(program.getUniforms().get("diffuse"), new int[] { 0 });
		program.getUniforms().get("specular").setValueUpdatePolicy(
			ValueUpdatePolicy.MANUAL);
		shader.setUniform(program.getUniforms().get("specular"),
			new int[] { 1 });
		program.getUniforms().get("normal").setValueUpdatePolicy(
			ValueUpdatePolicy.MANUAL);
		shader.setUniform(program.getUniforms().get("normal"), new int[] { 2 });

		TextureImage diffuse = null;
		TextureImage specular = null;
		TextureImage normal = null;

		try {
			diffuse =
				TextureLoader.readTexture(this.getClass().getClassLoader()
					.getResource("data/textures/wall_diffuse.png"));
			diffuse.setWrapSTR(TextureWrap.REPEAT);
			
			specular =
				TextureLoader.readTexture(this.getClass().getClassLoader()
					.getResource("data/textures/wall_specular.png"));
			specular.setWrapSTR(TextureWrap.REPEAT);
			normal =
				TextureLoader.readTexture(this.getClass().getClassLoader()
					.getResource("data/textures/wall_normal.png"));
			normal.setWrapSTR(TextureWrap.REPEAT);

			renderer.requestUpdate(diffuse, true);
			renderer.requestUpdate(specular, true);
			renderer.requestUpdate(normal, true);
		} catch (IOException io) {
			// fail
			throw new RuntimeException(io);
		}

		MultiTexture textures =
			new MultiTexture(new Texture(diffuse), new Texture(specular),
				new Texture(normal));

		return new Appearance().setMultiTexture(textures).setGlobalLighting(
			new GlobalLighting()).setGlslShader(shader);
	}

	private GlslProgram createProgram(Framework renderer) {
		String[] vertexShader =
			{
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
				// "half_vector = gl_LightSource[0].halfVector.xyz;",
				"eye_dir = -gl_Position.xyz;",

				"gl_Position = gl_ProjectionMatrix * gl_Position;", "}" };

		String[] fragmentShader =
			{
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
				"}" };

		GlslProgram program = new GlslProgram(vertexShader, fragmentShader);
		program.bindAttribute("tangent", AttributeType.VEC3F, 1);
		program.bindAttribute("bitangent", AttributeType.VEC3F, 2);

		renderer.requestUpdate(program, true);
		renderer.renderFrame(null);

		System.out.println(renderer.getStatus(program));
		System.out.println(renderer.getStatusMessage(program));

		for (GlslUniform u : program.getUniforms().values()) {
			System.out.println("Uniform: " + u.getName() + " " + u.getType()
				+ " " + u.getLength());
		}

		for (GlslVertexAttribute a : program.getAttributes().values()) {
			System.out.println("Attribute: " + a.getName() + " " + a.getType()
				+ " " + a.getBindingSlot());
		}

		return program;
	}

	@Override
	protected void handleInput(InputManager input, float dt) {
		super.handleInput(input, dt);
		boolean leftPressed = input.isMousePressed(InputManager.leftClick);
		boolean rightPressed =
			input.isMousePressed(InputManager.rightClick)
				|| (leftPressed && input.isKeyPressed(KeyEvent.VK_CONTROL));

		int x = input.getMouseXChange();
		int y = input.getMouseYChange();

		if (rightPressed) {
			adjustTranslation(light, x, y);
		} else if (leftPressed) {
			adjustRotation(cube, x, y);
		}
	}

	static Transform identity = new Transform();
	static Matrix3f mx = new Matrix3f();
	static Matrix3f my = new Matrix3f();

	private void adjustRotation(Node node, int x, int y) {
		Transform world = new Transform();
		node.localToWorld(identity, world, false);
		world.mul(view.getView().getViewTransform(), world);

		int pixelWidth = window.getWidth();
		float cameraWidth =
			view.getView().getFrustumRight() - view.getView().getFrustumLeft();
		float sx = cameraWidth * x / pixelWidth;
		sx = (float) Math.atan(sx / view.getView().getFrustumNear()) * 3;

		int pixelHeight = window.getHeight();
		float cameraHeight =
			view.getView().getFrustumTop() - view.getView().getFrustumBottom();
		float sy = cameraHeight * y / pixelHeight;
		sy = -(float) Math.atan(sy / view.getView().getFrustumNear()) * 3;

		mx.rotX(sy);
		my.rotY(sx);
		mx.mul(my);
		world.getRotation().mul(mx, world.getRotation());

		world.inverseMul(view.getView().getViewTransform(), world);
		node.setWorldTransform(world);
	}

	private void adjustTranslation(Node node, int x, int y) {
		Transform world = new Transform();
		node.localToWorld(identity, world, false);
		world.mul(view.getView().getViewTransform(), world);

		Vector3f trans = world.getTranslation();

		int pixelWidth = window.getWidth();
		float cameraWidth =
			view.getView().getFrustumRight() - view.getView().getFrustumLeft();
		float sx = cameraWidth * x / pixelWidth;
		sx = sx * trans.z / view.getView().getFrustumNear();

		int pixelHeight = window.getHeight();
		float cameraHeight =
			view.getView().getFrustumTop() - view.getView().getFrustumBottom();
		float sy = cameraHeight * y / pixelHeight;
		sy = sy * trans.z / view.getView().getFrustumNear();

		trans.x = trans.x - sx; // in right hand system, view's right is negative x
		trans.y = trans.y - sy; // downward motion == pos. mouse y change, so negate it

		world.inverseMul(view.getView().getViewTransform(), world);
		node.setWorldTransform(world);
	}

	/*
	 * Junky code to build a cube with extra vertex attributes.
	 */

	private Geometry build(IndexedArrayGeometry b) {
		float[] verts = b.getVertices();
		float[] tcs = b.getTextureCoordinates(0).getBuffer();

		float[] tan = new float[verts.length];
		float[] bitan = new float[verts.length];
		TangentGenerator.generate(verts, tcs, b.getIndices(), tan, bitan);

		b.setVertexAttributes(1, new VectorBuffer(tan, 3));
		b.setVertexAttributes(2, new VectorBuffer(bitan, 3));

		return b;
	}
}
