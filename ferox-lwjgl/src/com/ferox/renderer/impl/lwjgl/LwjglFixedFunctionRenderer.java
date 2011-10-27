package com.ferox.renderer.impl.lwjgl;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;

import com.ferox.math.Color3f;
import com.ferox.math.Matrix4f;
import com.ferox.math.Vector3f;
import com.ferox.math.Vector4f;
import com.ferox.renderer.impl.AbstractFixedFunctionRenderer;
import com.ferox.renderer.impl.Context;
import com.ferox.renderer.impl.RenderInterruptedException;
import com.ferox.renderer.impl.resource.GeometryHandle;
import com.ferox.renderer.impl.resource.ResourceHandle;
import com.ferox.renderer.impl.resource.VertexArray;
import com.ferox.resource.Texture;
import com.ferox.resource.Texture.Target;
import com.ferox.util.geom.Geometry;
import com.ferox.util.geom.Geometry.CompileType;

public class LwjglFixedFunctionRenderer extends AbstractFixedFunctionRenderer {
    private final FloatBuffer colorBuffer;
    private final FloatBuffer matrixBuffer;
    private final FloatBuffer vector4Buffer;
    private final FloatBuffer vector3Buffer;
    
    // state tracking
    private boolean alphaTestEnabled;
    private VertexArray boundVertices;
    private VertexArray boundNormals;
    private final VertexArray[] boundTexCoords;
    
    private GeometryHandle lastGeometry;

    /**
     * Create a new JoglFixedFunctionRendererDelegate that is paired with the given
     * JoglContext, and is to be used within the given JoglFramework.
     * 
     * @param context The JoglContext that provides the GL instances for this
     *            delegate
     * @param framework The JoglFramework that created the JoglContext
     * @throws NullPointerException if either argument is null
     */
    public LwjglFixedFunctionRenderer(LwjglFramework framework) {
        super(new LwjglRendererDelegate(), framework);
        
        colorBuffer = BufferUtils.createFloatBuffer(4);
        matrixBuffer = BufferUtils.createFloatBuffer(16);
        vector4Buffer = BufferUtils.createFloatBuffer(4);
        vector3Buffer = BufferUtils.createFloatBuffer(3);
        alphaTestEnabled = false;
        
        boundVertices = null;
        boundNormals = null;
        boundTexCoords = new VertexArray[texBindings.length];
    }
    
    private void glEnable(int flag, boolean enable) {
        if (enable)
            GL11.glEnable(flag);
        else
            GL11.glDisable(flag);
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
                glEnable(GL11.GL_ALPHA_TEST, false);
            }
        } else {
            if (!alphaTestEnabled) {
                alphaTestEnabled = true;
                glEnable(GL11.GL_ALPHA_TEST, true);
            }
            
            GL11.glAlphaFunc(Utils.getGLPixelTest(test), ref);
        }
    }

    @Override
    protected void glEnableFog(boolean enable) {
        glEnable(GL11.GL_FOG, enable);
    }

    @Override
    protected void glEnableLight(int light, boolean enable) {
        glEnable(GL11.GL_LIGHT0 + light, enable);
    }

    @Override
    protected void glEnableLighting(boolean enable) {
        glEnable(GL11.GL_LIGHTING, enable);
    }

    @Override
    protected void glEnableLineAntiAliasing(boolean enable) {
        glEnable(GL11.GL_LINE_SMOOTH, enable);
    }

    @Override
    protected void glEnablePointAntiAliasing(boolean enable) {
        glEnable(GL11.GL_POINT_SMOOTH, enable);
    }

    @Override
    protected void glEnablePolyAntiAliasing(boolean enable) {
        glEnable(GL11.GL_POLYGON_SMOOTH, enable);
    }

    @Override
    protected void glEnableSmoothShading(boolean enable) {
        GL11.glShadeModel(enable ? GL11.GL_SMOOTH : GL11.GL_FLAT);
    }

    @Override
    protected void glEnableTexture(Target target, boolean enable) {
        int type = Utils.getGLTextureTarget(target);
        glEnable(type, enable);
    }

    @Override
    protected void glEnableTwoSidedLighting(boolean enable) {
        GL11.glLightModeli(GL11.GL_LIGHT_MODEL_TWO_SIDE, (enable ? GL11.GL_TRUE : GL11.GL_FALSE));
    }

    @Override
    protected void glFogColor(Color3f color) {
        Utils.get(color, colorBuffer);
        GL11.glFog(GL11.GL_FOG_COLOR, colorBuffer);
    }

    @Override
    protected void glFogDensity(float density) {
        GL11.glFogf(GL11.GL_FOG_DENSITY, density);
    }

    @Override
    protected void glFogMode(FogMode fog) {
        switch(fog) {
        case EXP:
            GL11.glFogi(GL11.GL_FOG_MODE, GL11.GL_EXP);
            break;
        case EXP_SQUARED:
            GL11.glFogi(GL11.GL_FOG_MODE, GL11.GL_EXP2);
            break;
        case LINEAR:
            GL11.glFogi(GL11.GL_FOG_MODE, GL11.GL_LINEAR);
            break;
        }
    }

    @Override
    protected void glFogRange(float start, float end) {
        GL11.glFogf(GL11.GL_FOG_START, start);
        GL11.glFogf(GL11.GL_FOG_END, end);
    }

    @Override
    protected void glGlobalLighting(Color3f ambient) {
        Utils.get(ambient, colorBuffer);
        GL11.glLightModel(GL11.GL_LIGHT_MODEL_AMBIENT, colorBuffer);
    }

    @Override
    protected void glLightAngle(int light, float angle) {
        GL11.glLightf(GL11.GL_LIGHT0 + light, GL11.GL_SPOT_CUTOFF, angle);
    }

    @Override
    protected void glLightAttenuation(int light, float constant, float linear, float quadratic) {
        light += GL11.GL_LIGHT0;
        GL11.glLightf(light, GL11.GL_CONSTANT_ATTENUATION, constant);
        GL11.glLightf(light, GL11.GL_LINEAR_ATTENUATION, linear);
        GL11.glLightf(light, GL11.GL_QUADRATIC_ATTENUATION, quadratic);
    }

    @Override
    protected void glLightColor(int light, LightColor lc, Color3f color) {
        Utils.get(color, colorBuffer);
        int c = getGLLight(lc);
        GL11.glLight(GL11.GL_LIGHT0 + light, c, colorBuffer);
    }

    @Override
    protected void glLightDirection(int light, Vector3f dir) {
        Utils.get(dir, vector3Buffer);
        GL11.glLight(GL11.GL_LIGHT0 + light, GL11.GL_SPOT_DIRECTION, vector3Buffer);
    }

    @Override
    protected void glLightPosition(int light, Vector4f pos) {
        Utils.get(pos, vector4Buffer);
        GL11.glLight(GL11.GL_LIGHT0, GL11.GL_POSITION, vector4Buffer);
    }

    @Override
    protected void glLineWidth(float width) {
        GL11.glLineWidth(width);
    }

    @Override
    protected void glMaterialColor(LightColor component, Color3f color) {
        if (component != LightColor.DIFFUSE) {
            Utils.get(color, colorBuffer);
            int c = getGLLight(component);
            GL11.glMaterial(GL11.GL_FRONT_AND_BACK, c, colorBuffer);
        } else
            GL11.glColor4f(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
    }
    
    private int getGLLight(LightColor c) {
        switch(c) {
        case AMBIENT: return GL11.GL_AMBIENT;
        case DIFFUSE: return GL11.GL_DIFFUSE;
        case EMISSIVE: return GL11.GL_EMISSION;
        case SPECULAR: return GL11.GL_SPECULAR;
        }
        return -1;
    }

    @Override
    protected void glMaterialShininess(float shininess) {
        GL11.glMaterialf(GL11.GL_FRONT_AND_BACK, GL11.GL_SHININESS, shininess);
    }

    @Override
    protected void glMatrixMode(MatrixMode mode) {
        switch(mode) {
        case MODELVIEW:
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            break;
        case PROJECTION:
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            break;
        case TEXTURE:
            GL11.glMatrixMode(GL11.GL_TEXTURE);
            break;
        }
    }

    @Override
    protected void glPointWidth(float width) {
        GL11.glPointSize(width);
    }

    @Override
    protected void glSetMatrix(Matrix4f matrix) {
        matrixBuffer.put(0, matrix.m00);
        matrixBuffer.put(1, matrix.m10);
        matrixBuffer.put(2, matrix.m20);
        matrixBuffer.put(3, matrix.m30);
        
        matrixBuffer.put(4, matrix.m01);
        matrixBuffer.put(5, matrix.m11);
        matrixBuffer.put(6, matrix.m21);
        matrixBuffer.put(7, matrix.m31);
        
        matrixBuffer.put(8, matrix.m02);
        matrixBuffer.put(9, matrix.m12);
        matrixBuffer.put(10, matrix.m22);
        matrixBuffer.put(11, matrix.m32);
        
        matrixBuffer.put(12, matrix.m03);
        matrixBuffer.put(13, matrix.m13);
        matrixBuffer.put(14, matrix.m23);
        matrixBuffer.put(15, matrix.m33);

        GL11.glLoadMatrix(matrixBuffer);
    }

    @Override
    protected void glCombineFunction(CombineFunction func, boolean rgb) {
        int c = Utils.getGLCombineFunc(func);
        int target = (rgb ? GL13.GL_COMBINE_RGB : GL13.GL_COMBINE_ALPHA);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, target, c);
    }

    @Override
    protected void glCombineOp(int operand, CombineOp op, boolean rgb) {
        int o = Utils.getGLCombineOp(op);
        int target = -1;
        if (rgb) {
            switch(operand) {
            case 0: target = GL13.GL_OPERAND0_RGB; break;
            case 1: target = GL13.GL_OPERAND1_RGB; break;
            case 2: target = GL13.GL_OPERAND2_RGB; break;
            }
        } else {
            switch(operand) {
            case 0: target = GL13.GL_OPERAND0_ALPHA; break;
            case 1: target = GL13.GL_OPERAND1_ALPHA; break;
            case 2: target = GL13.GL_OPERAND2_ALPHA; break;
            }
        }
        
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, target, o);
    }

    @Override
    protected void glCombineSrc(int operand, CombineSource src, boolean rgb) {
        int o = Utils.getGLCombineSrc(src);
        int target = -1;
        if (rgb) {
            switch(operand) {
            case 0: target = GL13.GL_SOURCE0_RGB; break;
            case 1: target = GL13.GL_SOURCE1_RGB; break;
            case 2: target = GL13.GL_SOURCE2_RGB; break;
            }
        } else {
            switch(operand) {
            case 0: target = GL13.GL_SOURCE0_ALPHA; break;
            case 1: target = GL13.GL_SOURCE1_ALPHA; break;
            case 2: target = GL13.GL_SOURCE2_ALPHA; break;
            }
        }
        
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, target, o);
    }

    @Override
    protected void glTexEnvMode(EnvMode mode) {
        int envMode = Utils.getGLTexEnvMode(mode);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, envMode);
    }

    @Override
    protected void glTexEyePlane(TexCoord coord, Vector4f plane) {
        Utils.get(plane, vector4Buffer);
        int tc = Utils.getGLTexCoord(coord, false);
        GL11.glTexGen(tc, GL11.GL_EYE_PLANE, vector4Buffer);
    }
    
    @Override
    protected void glTexGen(TexCoord coord, TexCoordSource gen) {
        if (gen == TexCoordSource.ATTRIBUTE)
            return; // don't need to do anything, it's already disabled
        
        int mode = Utils.getGLTexGen(gen);
        int tc = Utils.getGLTexCoord(coord, false);
        GL11.glTexGeni(tc, GL11.GL_TEXTURE_GEN_MODE, mode);
    }
    
    @Override
    protected void glEnableTexGen(TexCoord coord, boolean enable) {
        glEnable(Utils.getGLTexCoord(coord, true), enable);
    }

    @Override
    protected void glTexObjPlane(TexCoord coord, Vector4f plane) {
        Utils.get(plane, vector4Buffer);
        int tc = Utils.getGLTexCoord(coord, false);
        GL11.glTexGen(tc, GL11.GL_OBJECT_PLANE, vector4Buffer);
    }

    @Override
    protected void glTextureColor(Color3f color) {
        Utils.get(color, colorBuffer);
        GL11.glTexEnv(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_COLOR, colorBuffer);
    }
    
    @Override
    protected void glBindTexture(Target target, Texture img) {
        int glTarget = Utils.getGLTextureTarget(target);
        ResourceHandle handle = (img == null ? null : resourceManager.getHandle(img));
        
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
        
        // unbind vbos
        BoundObjectState state = context.getRecord();
        state.bindArrayVbo(0);
        state.bindElementVbo(0);
        
        // disable all vertex pointers
        if (boundVertices != null) {
            GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
            boundVertices = null;
        }
        if (boundNormals != null) {
            GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);
            boundNormals = null;
        }
        for (int i = 0; i < texBindings.length; i++) {
            if (boundTexCoords[i] != null) {
                GL13.glClientActiveTexture(GL13.GL_TEXTURE0 + i);
                GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
                boundTexCoords[i] = null;
            }
        }
        
        // reset geom tracker
        lastGeometry = null;
    }

    @Override
    public int render(Geometry geom) {
        if (Thread.interrupted())
            throw new RenderInterruptedException();
        
        ResourceHandle handle = resourceManager.getHandle(geom);
        if (handle != null) {
            super.render(geom);
            return renderImpl((GeometryHandle) handle);
        } else
            return 0;
    }
    
    private int renderImpl(GeometryHandle handle) {
        BoundObjectState state = context.getRecord();
        
        VertexArray vertices = getVertexArray(handle, vertexBinding);
        if (vertices == null || vertices.elementSize == 1)
            return 0; // can't use this va as vertices

        boolean useVbos = handle.compile != CompileType.NONE;
        int vertexCount = (useVbos ? vertices.vboLen / (4 * vertices.elementSize) : vertices.buffer.capacity() / vertices.elementSize);
        
        // BoundObjectState takes care of the same id for us
        if (lastGeometry != handle) {
            if (!useVbos) {
                state.bindArrayVbo(0);
                state.bindElementVbo(0);
            } else {
                state.bindArrayVbo(handle.arrayVbo);
                state.bindElementVbo(handle.elementVbo);
            }

            if (boundVertices != vertices) {
                if (boundVertices == null)
                    GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
                glVertexPointer(vertices, useVbos);
                boundVertices = vertices;
            }

            VertexArray normals = getVertexArray(handle, normalBinding);
            if (lightingEnabled && normals != null && normals.elementSize == 3) {
                if (boundNormals != normals) {
                    // update pointer
                    if (boundNormals == null)
                        GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
                    glNormalPointer(normals, useVbos);
                    boundNormals = normals;
                }
            } else {
                // don't send normals
                if (boundNormals != null) {
                    GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);
                    boundNormals = null;
                }
            }

            VertexArray tcs;
            for (int i = 0; i < texBindings.length; i++) {
                tcs = getVertexArray(handle, texBindings[i]);
                if (textures[i].enabled && state.getTexture(i) != 0 && tcs != null) {
                    if (boundTexCoords[i] != tcs) {
                        // update pointer
                        GL13.glClientActiveTexture(GL13.GL_TEXTURE0 + i);
                        if (boundTexCoords[i] == null)
                            GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
                        glTexCoordPointer(tcs, useVbos);
                        boundTexCoords[i] = tcs;
                    }
                } else {
                    // disable texcoords
                    if (boundTexCoords[i] != null) {
                        GL13.glClientActiveTexture(GL13.GL_TEXTURE0 + i);
                        GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
                        boundTexCoords[i] = null;
                    }
                }
            }
        }
        
        int glPolyType = Utils.getGLPolygonConnectivity(handle.polyType);
        if (useVbos)
            GL12.glDrawRangeElements(glPolyType, handle.minIndex, handle.maxIndex, 
                                     handle.indexCount, GL11.GL_UNSIGNED_INT, 0);
        else
            GL12.glDrawRangeElements(glPolyType, handle.minIndex, handle.maxIndex, handle.indices);
        
        lastGeometry = handle;
        Context.getCurrent().getFrameStatistics().add(1, vertexCount, handle.polyCount);
        return handle.polyCount;
    }
    
    private void glVertexPointer(VertexArray vertices, boolean vbo) {
        if (vbo)
            GL11.glVertexPointer(vertices.elementSize, GL11.GL_FLOAT, 0, vertices.offset);
        else
            GL11.glVertexPointer(vertices.elementSize, 0, vertices.buffer);
    }
    
    private void glNormalPointer(VertexArray normals, boolean vbo) {
        if (vbo)
            GL11.glNormalPointer(GL11.GL_FLOAT, 0, normals.offset);
        else
            GL11.glNormalPointer(0, normals.buffer);
    }
    
    private void glTexCoordPointer(VertexArray tcs, boolean vbo) {
        if (vbo)
            GL11.glTexCoordPointer(tcs.elementSize, GL11.GL_FLOAT, 0, tcs.offset);
        else
            GL11.glTexCoordPointer(tcs.elementSize, 0, tcs.buffer);
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

    @Override
    protected void init() {
        GL11.glColorMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_DIFFUSE);
        GL11.glEnable(GL11.GL_COLOR_MATERIAL);
    }
}
