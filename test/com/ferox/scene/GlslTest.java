package com.ferox.scene;

import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.openmali.vecmath.Vector3f;

import com.ferox.BasicApplication;
import com.ferox.math.BoundSphere;
import com.ferox.math.Color;
import com.ferox.renderer.Renderer;
import com.ferox.resource.Geometry;
import com.ferox.resource.GlslProgram;
import com.ferox.resource.GlslUniform;
import com.ferox.resource.GlslVertexAttribute;
import com.ferox.resource.TextureImage;
import com.ferox.resource.VertexArray;
import com.ferox.resource.VertexArrayGeometry;
import com.ferox.resource.BufferedGeometry.PolygonType;
import com.ferox.resource.GlslUniform.ValueUpdatePolicy;
import com.ferox.resource.GlslVertexAttribute.AttributeType;
import com.ferox.resource.util.TextureIO;
import com.ferox.state.Appearance;
import com.ferox.state.GlslShader;
import com.ferox.state.LightReceiver;
import com.ferox.state.Material;
import com.ferox.state.MultiTexture;
import com.ferox.state.Texture;
import com.sun.opengl.util.BufferUtil;

public class GlslTest extends BasicApplication {
	public static final boolean DEBUG = false;
	
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
		
		Geometry cube = this.buildCube(4f);
		renderer.requestUpdate(cube, true);
		Appearance app = this.createGlslAppearance(renderer);
		
		Shape s = new Shape(cube, app);
		s.setLocalBounds(new BoundSphere());
		root.add(s);
		
		SpotLight light = new SpotLight(new Color(1f, 1f, 1f), new Color(1f, 0f, 0f), new Color());
		light.setLocalBounds(new BoundSphere(20f));
		light.getLocalTransform().getTranslation().set(-4f, 4f, 10f);
		root.add(light);

		Shape lightCube = new Shape(buildCube(renderer, .5f, false), 
				new Appearance(new Material(light.getDiffuse())));
		lightCube.setLocalTransform(light.getLocalTransform());
		lightCube.setLocalBounds(new BoundSphere());
		root.add(lightCube);

		this.window.setVSyncEnabled(true);
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
		
		try {
			diffuse = TextureIO.readTexture(new File("data/textures/wall_diffuse.png"));
			specular = TextureIO.readTexture(new File("data/textures/wall_specular.png"));
			normal = TextureIO.readTexture(new File("data/textures/wall_normal.png"));
			
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

				"varying vec3 half_vector;",
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
					"half_vector = gl_LightSource[0].halfVector.xyz;",

					"gl_Position = gl_ProjectionMatrix * gl_Position;",
				"}"
			};

		String[] fragmentShader = {
			"uniform sampler2D diffuse;",
			"uniform sampler2D normal;",
			"uniform sampler2D specular;", 

			"varying vec3 half_vector;",
			"varying vec3 light_dir;",

			"varying vec3 tan;",
			"varying vec3 bitan;",
			"varying vec3 nm;",

			"void main() {",
				"mat3 to_eye = gl_NormalMatrix * mat3(tan, bitan, nm);",
				"vec3 norm = to_eye * (texture2D(normal, gl_TexCoord[0].st).grb * 2.0 - 1.0);",
				"vec3 baseColor = texture2D(diffuse, gl_TexCoord[0].st).rgb;",
				"vec3 lightVector = normalize(light_dir);",
				"float nxDir = max(.1, dot(norm, lightVector));",
				"vec4 diffuse = gl_LightSource[0].diffuse * nxDir;",

				"float specularPower = 0.0;",
				"if (nxDir > 0.0) {",
					"lightVector = normalize(half_vector);",
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
	
	/*
	 * Junky code to build a cube with extra vertex attributes.
	 */
	
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
		
		i = new int[48];
		for (int u = 0; u < 6; u++) {
			int t1 = u * 4;
			int t2 = u * 4 + 1;
			int t3 = u * 4 + 2;
			int t4 = u * 4 + 3;
			
			i[u * 6] = t1;
			i[u * 6 + 1] = t2;
			i[u * 6 + 2] = t4;
			
			i[u * 6 + 3] = t2;
			i[u * 6 + 4] = t3;
			i[u * 6 + 5] = t4;
		}
		
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
		
		VertexArrayGeometry geom = new VertexArrayGeometry(vb, new VertexArray(3), ib, new VertexArray(1), PolygonType.TRIANGLES);
		geom.setNormals(nb, new VertexArray(3));
		geom.setTextureCoordinates(0, tb, new VertexArray(2));
		geom.setVertexAttributes(1, tanb, new VertexArray(3));
		geom.setVertexAttributes(2, btanb, new VertexArray(3));

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
}
