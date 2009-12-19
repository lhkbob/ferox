package com.ferox.renderer.impl.jogl;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import com.ferox.math.Color4f;
import com.ferox.math.Matrix4f;
import com.ferox.math.Vector3f;
import com.ferox.math.Vector4f;
import com.ferox.renderer.FixedFunctionRenderer.CombineFunction;
import com.ferox.renderer.FixedFunctionRenderer.CombineOp;
import com.ferox.renderer.FixedFunctionRenderer.CombineSource;
import com.ferox.renderer.FixedFunctionRenderer.EnvMode;
import com.ferox.renderer.FixedFunctionRenderer.TexCoord;
import com.ferox.renderer.FixedFunctionRenderer.TexCoordSource;
import com.ferox.renderer.Renderer.Comparison;
import com.ferox.renderer.impl.FixedFunctionRendererDelegate;
import com.ferox.renderer.impl.RenderInterruptedException;
import com.ferox.renderer.impl.ResourceHandle;
import com.ferox.renderer.impl.jogl.resource.GeometryHandle;
import com.ferox.renderer.impl.jogl.resource.VertexArray;
import com.ferox.resource.Geometry;
import com.ferox.resource.TextureImage;
import com.ferox.resource.Geometry.CompileType;
import com.ferox.resource.Resource.Status;
import com.ferox.resource.TextureImage.TextureTarget;

/**
 * An final implementation of FixedFunctionRendererDelgate that implements all
 * of the functionality required. This requires a GL2 enabled profile because
 * any higher version of OpenGL does not support the fixed-function pipeline.
 * 
 * @author Michael Ludwig
 */
public final class JoglFixedFunctionRendererDelegate extends FixedFunctionRendererDelegate {
	private static final Logger log = Logger.getLogger(JoglFramework.class.getPackage().getName());
	
	private final JoglContext context;
	private final JoglResourceManager resManager;
	
	private final float[] colorBuffer;
	private final float[] matrixBuffer;
	private final float[] vector4Buffer;
	private final float[] vector3Buffer;
	
	// state tracking
	private boolean alphaTestEnabled;
	private VertexArray boundVertices;
	private VertexArray boundNormals;
	private final VertexArray[] boundTexCoords;
	
	private GeometryHandle lastGeometry;
	private int lastGeometryVersion;

	/**
	 * Create a new JoglFixedFunctionRendererDelegate that is paired with the given
	 * JoglContext, and is to be used within the given JoglFramework.
	 * 
	 * @param context The JoglContext that provides the GL instances for this
	 *            delegate
	 * @param framework The JoglFramework that created the JoglContext
	 * @throws NullPointerException if either argument is null
	 */
	public JoglFixedFunctionRendererDelegate(JoglContext context, JoglFramework framework) {
		super(framework.getCapabilities().getMaxActiveLights(), framework.getCapabilities().getMaxFixedPipelineTextures());
		if (context == null)
			throw new NullPointerException("Context cannot be null");
		this.context = context;
		
		resManager = framework.getResourceManager();
		
		colorBuffer = new float[4];
		matrixBuffer = new float[16];
		vector4Buffer = new float[4];
		vector3Buffer = new float[3];
		alphaTestEnabled = false;
		
		boundVertices = null;
		boundNormals = null;
		boundTexCoords = new VertexArray[texBindings.length];
	}
	
	private void glEnable(int flag, boolean enable) {
		if (enable)
			context.getGL().glEnable(flag);
		else
			context.getGL().glDisable(flag);
	}

	@Override
	protected void glActiveTexture(int unit) {
		context.getRecord().setActiveTexture(context.getGL(), unit);
	}

	@Override
	protected void glAlphaTest(Comparison test, float ref) {
		if (test == Comparison.ALWAYS) {
			if (alphaTestEnabled) {
				alphaTestEnabled = false;
				glEnable(GL2.GL_ALPHA_TEST, false);
			}
		} else {
			if (!alphaTestEnabled) {
				alphaTestEnabled = true;
				glEnable(GL2.GL_ALPHA_TEST, true);
			}
			
			context.getGL2().glAlphaFunc(Utils.getGLPixelTest(test), ref);
		}
	}

	@Override
	protected void glEnableFog(boolean enable) {
		glEnable(GL2.GL_FOG, enable);
	}

	@Override
	protected void glEnableLight(int light, boolean enable) {
		glEnable(GL2.GL_LIGHT0 + light, enable);
	}

	@Override
	protected void glEnableLighting(boolean enable) {
		glEnable(GL2.GL_LIGHTING, enable);
	}

	@Override
	protected void glEnableLineAntiAliasing(boolean enable) {
		glEnable(GL2.GL_LINE_SMOOTH, enable);
	}

	@Override
	protected void glEnablePointAntiAliasing(boolean enable) {
		glEnable(GL2.GL_POINT_SMOOTH, enable);
	}

	@Override
	protected void glEnablePolyAntiAliasing(boolean enable) {
		glEnable(GL2.GL_POLYGON_SMOOTH, enable);
	}

	@Override
	protected void glEnableSmoothShading(boolean enable) {
		context.getGL2().glShadeModel(enable ? GL2.GL_SMOOTH : GL2.GL_FLAT);
	}

	@Override
	protected void glEnableTexture(TextureTarget target, boolean enable) {
		int type = Utils.getGLTextureTarget(target);
		glEnable(type, enable);
	}

	@Override
	protected void glEnableTwoSidedLighting(boolean enable) {
		context.getGL2().glLightModeli(GL2.GL_LIGHT_MODEL_TWO_SIDE, (enable ? GL.GL_TRUE : GL.GL_FALSE));
	}

	@Override
	protected void glFogColor(Color4f color) {
		Utils.get(color, colorBuffer);
		context.getGL2().glFogfv(GL2.GL_FOG_COLOR, colorBuffer, 0);
	}

	@Override
	protected void glFogDensity(float density) {
		context.getGL2().glFogf(GL2.GL_FOG_DENSITY, density);
	}

	@Override
	protected void glFogMode(FogMode fog) {
		switch(fog) {
		case EXP:
			context.getGL2().glFogi(GL2.GL_FOG_MODE, GL2.GL_EXP);
			break;
		case EXP_SQUARED:
			context.getGL2().glFogi(GL2.GL_FOG_MODE, GL2.GL_EXP2);
			break;
		case LINEAR:
			context.getGL2().glFogi(GL2.GL_FOG_MODE, GL2.GL_LINEAR);
			break;
		}
	}

	@Override
	protected void glFogRange(float start, float end) {
		context.getGL2().glFogf(GL2.GL_FOG_START, start);
		context.getGL2().glFogf(GL2.GL_FOG_END, end);
	}

	@Override
	protected void glGlobalLighting(Color4f ambient) {
		Utils.get(ambient, colorBuffer);
		context.getGL2().glLightModelfv(GL2.GL_LIGHT_MODEL_AMBIENT, colorBuffer, 0);
	}

	@Override
	protected void glLightAngle(int light, float angle) {
		context.getGL2().glLightf(GL2.GL_LIGHT0 + light, GL2.GL_SPOT_CUTOFF, angle);
	}

	@Override
	protected void glLightAttenuation(int light, float constant, float linear, float quadratic) {
		light += GL2.GL_LIGHT0;
		GL2 gl = context.getGL2();
		gl.glLightf(light, GL2.GL_CONSTANT_ATTENUATION, constant);
		gl.glLightf(light, GL2.GL_LINEAR_ATTENUATION, linear);
		gl.glLightf(light, GL2.GL_QUADRATIC_ATTENUATION, quadratic);
	}

	@Override
	protected void glLightColor(int light, LightColor lc, Color4f color) {
		Utils.get(color, colorBuffer);
		int c = getGLLight(lc);
		context.getGL2().glLightfv(GL2.GL_LIGHT0 + light, c, colorBuffer, 0);
	}

	@Override
	protected void glLightDirection(int light, Vector3f dir) {
		dir.get(vector3Buffer, 0);
		context.getGL2().glLightfv(GL2.GL_LIGHT0 + light, GL2.GL_SPOT_DIRECTION, vector3Buffer, 0);
	}

	@Override
	protected void glLightPosition(int light, Vector4f pos) {
		pos.get(vector4Buffer, 0);
		context.getGL2().glLightfv(GL2.GL_LIGHT0 + light, GL2.GL_POSITION, vector4Buffer, 0);
	}

	@Override
	protected void glLineWidth(float width) {
		context.getGL2().glLineWidth(width);
	}

	@Override
	protected void glMaterialColor(LightColor component, Color4f color) {
		Utils.get(color, colorBuffer);
		int c = getGLLight(component);
		if (component == LightColor.DIFFUSE)
			context.getGL2().glColor4fv(colorBuffer, 0);
		else
			context.getGL2().glMaterialfv(GL.GL_FRONT_AND_BACK, c, colorBuffer, 0);
	}
	
	private int getGLLight(LightColor c) {
		switch(c) {
		case AMBIENT: return GL2.GL_AMBIENT;
		case DIFFUSE: return GL2.GL_DIFFUSE;
		case EMISSIVE: return GL2.GL_EMISSION;
		case SPECULAR: return GL2.GL_SPECULAR;
		}
		return -1;
	}

	@Override
	protected void glMaterialShininess(float shininess) {
		context.getGL2().glMaterialf(GL.GL_FRONT_AND_BACK, GL2.GL_SHININESS, shininess);
	}

	@Override
	protected void glMatrixMode(MatrixMode mode) {
		switch(mode) {
		case MODELVIEW:
			context.getGL2().glMatrixMode(GL2.GL_MODELVIEW);
			break;
		case PROJECTION:
			context.getGL2().glMatrixMode(GL2.GL_PROJECTION);
			break;
		case TEXTURE:
			context.getGL2().glMatrixMode(GL2.GL_TEXTURE);
			break;
		}
	}

	@Override
	protected void glPointWidth(float width) {
		context.getGL2().glPointSize(width);
	}

	@Override
	protected void glSetMatrix(Matrix4f matrix) {
		matrixBuffer[0] = matrix.m00;
		matrixBuffer[1] = matrix.m10;
		matrixBuffer[2] = matrix.m20;
		matrixBuffer[3] = matrix.m30;
		
		matrixBuffer[4] = matrix.m01;
		matrixBuffer[5] = matrix.m11;
		matrixBuffer[6] = matrix.m21;
		matrixBuffer[7] = matrix.m31;
		
		matrixBuffer[8] = matrix.m02;
		matrixBuffer[9] = matrix.m12;
		matrixBuffer[10] = matrix.m22;
		matrixBuffer[11] = matrix.m32;
		
		matrixBuffer[12] = matrix.m03;
		matrixBuffer[13] = matrix.m13;
		matrixBuffer[14] = matrix.m23;
		matrixBuffer[15] = matrix.m33;
		
		context.getGL2().glLoadMatrixf(matrixBuffer, 0);
	}

	@Override
	protected void glCombineFunction(CombineFunction func, boolean rgb) {
		int c = Utils.getGLCombineFunc(func);
		int target = (rgb ? GL2.GL_COMBINE_RGB : GL2.GL_COMBINE_ALPHA);
		context.getGL2().glTexEnvi(GL2.GL_TEXTURE_ENV, target, c);
	}

	@Override
	protected void glCombineOp(int operand, CombineOp op, boolean rgb) {
		int o = Utils.getGLCombineOp(op);
		int target = -1;
		if (rgb) {
			switch(operand) {
			case 0: target = GL2.GL_OPERAND0_RGB; break;
			case 1: target = GL2.GL_OPERAND1_RGB; break;
			case 2: target = GL2.GL_OPERAND2_RGB; break;
			}
		} else {
			switch(operand) {
			case 0: target = GL2.GL_OPERAND0_ALPHA; break;
			case 1: target = GL2.GL_OPERAND1_ALPHA; break;
			case 2: target = GL2.GL_OPERAND2_ALPHA; break;
			}
		}
		
		context.getGL2().glTexEnvi(GL2.GL_TEXTURE_ENV, target, o);
	}

	@Override
	protected void glCombineSrc(int operand, CombineSource src, boolean rgb) {
		int o = Utils.getGLCombineSrc(src);
		int target = -1;
		if (rgb) {
			switch(operand) {
			case 0: target = GL2.GL_SOURCE0_RGB; break;
			case 1: target = GL2.GL_SOURCE1_RGB; break;
			case 2: target = GL2.GL_SOURCE2_RGB; break;
			}
		} else {
			switch(operand) {
			case 0: target = GL2.GL_SOURCE0_ALPHA; break;
			case 1: target = GL2.GL_SOURCE1_ALPHA; break;
			case 2: target = GL2.GL_SOURCE2_ALPHA; break;
			}
		}
		
		context.getGL2().glTexEnvi(GL2.GL_TEXTURE_ENV, target, o);
	}

	@Override
	protected void glTexEnvMode(EnvMode mode) {
		int envMode = Utils.getGLTexEnvMode(mode);
		context.getGL2().glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, envMode);
	}

	@Override
	protected void glTexEyePlane(TexCoord coord, Vector4f plane) {
		plane.get(vector4Buffer, 0);
		int tc = Utils.getGLTexCoord(coord, false);
		context.getGL2().glTexGenfv(tc, GL2.GL_EYE_PLANE, vector4Buffer, 0);
	}
	
	@Override
	protected void glTexGen(TexCoord coord, TexCoordSource gen) {
		if (gen == TexCoordSource.ATTRIBUTE)
			return; // don't need to do anything, it's already disabled
		
		int mode = Utils.getGLTexGen(gen);
		int tc = Utils.getGLTexCoord(coord, false);
		context.getGL2().glTexGeni(tc, GL2.GL_TEXTURE_GEN_MODE, mode);
	}
	
	@Override
	protected void glEnableTexGen(TexCoord coord, boolean enable) {
		glEnable(Utils.getGLTexCoord(coord, true), enable);
	}

	@Override
	protected void glTexObjPlane(TexCoord coord, Vector4f plane) {
		plane.get(vector4Buffer, 0);
		int tc = Utils.getGLTexCoord(coord, false);
		context.getGL2().glTexGenfv(tc, GL2.GL_OBJECT_PLANE, vector4Buffer, 0);
	}

	@Override
	protected void glTextureColor(Color4f color) {
		Utils.get(color, colorBuffer);
		context.getGL2().glTexEnvfv(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_COLOR, colorBuffer, 0);
	}
	
	@Override
	protected void glBindTexture(TextureTarget target, TextureImage img) {
		int glTarget = Utils.getGLTextureTarget(target);
		ResourceHandle handle = (img == null ? null : resManager.getHandle(img));
		
		// the BoundObjectState takes care of the same id for us
		if (handle == null) {
			context.getRecord().bindTexture(context.getGL(), glTarget, 0);
		} else {
			context.getRecord().bindTexture(context.getGL(), glTarget, handle.getId());
		}
	}
	
	@Override
	public void reset() {
		super.reset();
		GL2 gl = context.getGL2();
		
		// unbind vbos
		BoundObjectState state = context.getRecord();
		state.bindArrayVbo(gl, 0);
		state.bindElementVbo(gl, 0);
		
		// disable all vertex pointers
		if (boundVertices != null) {
			gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
			boundVertices = null;
		}
		if (boundNormals != null) {
			gl.glDisableClientState(GL2.GL_NORMAL_ARRAY);
			boundNormals = null;
		}
		for (int i = 0; i < texBindings.length; i++) {
			if (boundTexCoords[i] != null) {
				gl.glClientActiveTexture(GL.GL_TEXTURE0 + i);
				gl.glDisableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
				boundTexCoords[i] = null;
			}
		}
		
		// reset geom tracker
		lastGeometry = null;
		lastGeometryVersion = 0;
	}

	@Override
	public int render(Geometry geom) {
		if (Thread.interrupted())
			throw new RenderInterruptedException();
		
		ResourceHandle handle = resManager.getHandle(geom);
		if (handle.getStatus() == Status.READY) {
			return renderImpl((GeometryHandle) handle);
		} else
			return 0;
	}
	
	private int renderImpl(GeometryHandle handle) {
		GL2 gl = context.getGL2();
		BoundObjectState state = context.getRecord();
		
		VertexArray vertices = getVertexArray(handle, vertexBinding);
		if (vertices == null || vertices.elementSize == 1)
			return 0; // can't use this va as vertices
		log.log(Level.FINEST, "Rendering geometry with " + handle.polyCount + " polygons");

		boolean useVbos = handle.compile != CompileType.NONE;
		
		// BoundObjectState takes care of the same id for us
		if (lastGeometry != handle || lastGeometryVersion != handle.version) {
			boolean override = lastGeometryVersion != handle.version;
			
			if (!useVbos) {
				state.bindArrayVbo(gl, 0);
				state.bindElementVbo(gl, 0);
			} else {
				state.bindArrayVbo(gl, handle.arrayVbo);
				state.bindElementVbo(gl, handle.elementVbo);
			}

			if (boundVertices != vertices || override) {
				if (boundVertices == null)
					gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
				glVertexPointer(gl, vertices, useVbos);
				boundVertices = vertices;
			}

			VertexArray normals = getVertexArray(handle, normalBinding);
			if (lightingEnabled && normals != null && normals.elementSize == 3) {
				if (boundNormals != normals || override) {
					// update pointer
					if (boundNormals == null)
						gl.glEnableClientState(GL2.GL_NORMAL_ARRAY);
					glNormalPointer(gl, normals, useVbos);
					boundNormals = normals;
				}
			} else {
				// don't send normals
				if (boundNormals != null) {
					gl.glDisableClientState(GL2.GL_NORMAL_ARRAY);
					boundNormals = null;
				}
			}

			VertexArray tcs;
			for (int i = 0; i < texBindings.length; i++) {
				tcs = getVertexArray(handle, texBindings[i]);
				if (textures[i].enabled && state.getTexture(i) != 0 && tcs != null) {
					if (boundTexCoords[i] != tcs || override) {
						// update pointer
						gl.glClientActiveTexture(GL.GL_TEXTURE0 + i);
						if (boundTexCoords[i] == null)
							gl.glEnableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
						glTexCoordPointer(gl, tcs, useVbos);
						boundTexCoords[i] = tcs;
					}
				} else {
					// disable texcoords
					if (boundTexCoords[i] != null) {
						gl.glClientActiveTexture(GL.GL_TEXTURE0 + i);
						gl.glDisableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
						boundTexCoords[i] = null;
					}
				}
			}
		}
		
		if (useVbos)
			gl.glDrawRangeElements(handle.glPolyType, handle.minIndex, handle.maxIndex, 
								   handle.indexCount, GL2.GL_UNSIGNED_INT, 0);
		else
			gl.glDrawRangeElements(handle.glPolyType, handle.minIndex, handle.maxIndex, 
								   handle.indexCount, GL2.GL_UNSIGNED_INT, handle.indices.rewind());
		
		lastGeometry = handle;
		lastGeometryVersion = handle.version;
		return handle.polyCount;
	}
	
	private void glVertexPointer(GL2 gl, VertexArray vertices, boolean vbo) {
		if (vbo)
			gl.glVertexPointer(vertices.elementSize, GL.GL_FLOAT, 0, vertices.offset);
		else
			gl.glVertexPointer(vertices.elementSize, GL.GL_FLOAT, 0, vertices.buffer.rewind());
	}
	
	private void glNormalPointer(GL2 gl, VertexArray normals, boolean vbo) {
		if (vbo)
			gl.glNormalPointer(GL.GL_FLOAT, 0, normals.offset);
		else
			gl.glNormalPointer(GL.GL_FLOAT, 0, normals.buffer.rewind());
	}
	
	private void glTexCoordPointer(GL2 gl, VertexArray tcs, boolean vbo) {
		if (vbo)
			gl.glTexCoordPointer(tcs.elementSize, GL.GL_FLOAT, 0, tcs.offset);
		else
			gl.glTexCoordPointer(tcs.elementSize, GL.GL_FLOAT, 0, tcs.buffer.rewind());
	}
	
	private VertexArray getVertexArray(GeometryHandle handle, String name) {
		if (name == null)
			return null;
		
		VertexArray arr;
		int len = handle.compiledPointers.size();
		for (int i = 0; i < len; i++) {
			arr = handle.compiledPointers.get(i);
			if (arr.name.equals(name))
				return arr;
		}
		// couldn't find a match
		return null;
	}
}
