package com.ferox.renderer.impl;

import com.ferox.math.Const;
import com.ferox.math.Matrix4;
import com.ferox.math.Vector3;
import com.ferox.math.Vector4;
import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.renderer.Renderer;
import com.ferox.renderer.impl.drivers.TextureHandle;
import com.ferox.renderer.impl.drivers.VertexBufferObjectHandle;
import com.ferox.resource.BufferData.DataType;
import com.ferox.resource.Texture;
import com.ferox.resource.Texture.Target;
import com.ferox.resource.VertexAttribute;
import com.ferox.resource.VertexBufferObject;

/**
 * <p>
 * The AbstractFixedFunctionRenderer is an abstract implementation of
 * {@link FixedFunctionRenderer}. It uses a {@link RendererDelegate} to handle
 * implementing the methods exposed by {@link Renderer}. The
 * AbstractFixedFunctionRenderer tracks the current state, and when necessary,
 * delegate to protected abstract methods which have the responsibility of
 * actually making OpenGL calls.
 * </p>
 * <p>
 * It makes a best-effort attempt to preserve the texture and vertex attribute
 * state when resource deadlocks must be resolved. It is possible that a texture
 * must be unbound or will have its data changed based on the actions of another
 * render task.
 * </p>
 * 
 * @author Michael Ludwig
 */
public abstract class AbstractFixedFunctionRenderer extends AbstractRenderer implements FixedFunctionRenderer {
    /**
     * FogMode represents the three different eye fog modes that are available
     * in OpenGL.
     */
    protected static enum FogMode {
        LINEAR, EXP, EXP_SQUARED
    }

    /**
     * When configuring lighting and material colors, OpenGL uses the same
     * functions to control the different types of color. For light colors, the
     * EMISSIVE enum is unused, since it's only available for material colors.
     */
    protected static enum LightColor {
        AMBIENT, DIFFUSE, SPECULAR, EMISSIVE
    }

    /**
     * OpenGL provides only one way to update matrices, and to switch
     * between matrix types, you must set the current mode.
     */
    protected static enum MatrixMode {
        MODELVIEW, PROJECTION, TEXTURE
    }

    protected static enum VertexTarget {
        VERTICES, NORMALS, TEXCOORDS, COLORS
    }

    // cached defaults
    private static final Matrix4 IDENTITY = new Matrix4();

    private static final Vector4 DEFAULT_MAT_A_COLOR = new Vector4(.2, .2, .2, 1);
    private static final Vector4 DEFAULT_MAT_D_COLOR = new Vector4(.8, .8, .8, 1);

    private static final Vector4 ZERO = new Vector4(0, 0, 0, 0);
    private static final Vector4 BLACK = new Vector4(0, 0, 0, 1);
    private static final Vector4 WHITE = new Vector4(1, 1, 1, 1);

    private static final Vector4 DEFAULT_LIGHT_POS = new Vector4(0, 0, 1, 0);
    private static final Vector3 DEFAULT_SPOT_DIR = new Vector3(0, 0, -1);

    private static final Vector4 DEFAULT_S_PLANE = new Vector4(1, 0, 0, 0);
    private static final Vector4 DEFAULT_T_PLANE = new Vector4(0, 1, 0, 0);
    private static final Vector4 DEFAULT_RQ_PLANE = new Vector4(0, 0, 0, 0);


    /**
     * An inner class that contains per-light state. Although it's accessible to
     * sub-classes, it should be considered read-only because the
     * AbstractFixedFunctionRenderer manages the updates to its variables.
     */
    protected class LightState {
        // LightState does not track position or direction since
        // they're stored by OpenGL after being modified by the current MV matrix
        private boolean modifiedSinceReset = false;

        public final Vector4 ambient = new Vector4(BLACK);
        public final Vector4 specular = new Vector4(BLACK);
        public final Vector4 diffuse = new Vector4(BLACK);

        public double constAtt = 1;
        public double linAtt = 0;
        public double quadAtt = 0;

        public double spotAngle = 180;

        public boolean enabled = false;
    }

    /**
     * An inner class that contains per-texture unit state. Although it's
     * accessible to sub-classes, it should be considered read-only because the
     * AbstractFixedFunctionRenderer manages the updates to its variables.
     */
    protected class TextureState {
        public final int unit;

        // TextureState does not track texture transforms, or eye planes
        // since these are difficult to track
        public boolean transformModifiedSinceReset = false;

        public Texture texture;
        public TextureHandle handle;

        public Vector4 color = new Vector4(0, 0, 0, 0);

        public TexCoordSource tcS = TexCoordSource.ATTRIBUTE;
        public TexCoordSource tcT = TexCoordSource.ATTRIBUTE;
        public TexCoordSource tcR = TexCoordSource.ATTRIBUTE;
        public TexCoordSource tcQ = TexCoordSource.ATTRIBUTE;

        public final Vector4 objPlaneS = new Vector4(DEFAULT_S_PLANE);
        public final Vector4 objPlaneT = new Vector4(DEFAULT_T_PLANE);
        public final Vector4 objPlaneR = new Vector4(DEFAULT_RQ_PLANE);
        public final Vector4 objPlaneQ = new Vector4(DEFAULT_RQ_PLANE);

        public CombineFunction rgbFunc = CombineFunction.MODULATE;
        public CombineFunction alphaFunc = CombineFunction.MODULATE;

        public final CombineOperand[] opRgb = {CombineOperand.COLOR, CombineOperand.COLOR, CombineOperand.ALPHA};
        public final CombineOperand[] opAlpha = {CombineOperand.ALPHA, CombineOperand.ALPHA, CombineOperand.ALPHA};

        public final CombineSource[] srcRgb = {CombineSource.CURR_TEX, CombineSource.PREV_TEX, CombineSource.CONST_COLOR};
        public final CombineSource[] srcAlpha = {CombineSource.CURR_TEX, CombineSource.PREV_TEX, CombineSource.CONST_COLOR};

        public TextureState(int unit) {
            this.unit = unit;
        }
    }

    protected class VertexState {
        // Used to handle relocking/unlocking
        public final VertexTarget target;
        public final int slot;

        public VertexBufferObject vbo;
        public VertexBufferObjectHandle handle;

        public int offset;
        public int stride;
        public int elementSize;

        private VertexState(VertexTarget target, int slot) {
            this.target = target;
            this.slot = slot;
        }

        public void activateSlot() {
            if (target == VertexTarget.TEXCOORDS && slot != activeClientTex) {
                // Special case slot handling for texture coordinates (other targets ignore slot)
                activeClientTex = slot;
                glActiveClientTexture(slot);
            }
        }
    }

    // alpha test
    protected Comparison alphaTest = Comparison.ALWAYS;
    protected double alphaRefValue = 1;

    // fog
    protected final Vector4 fogColor = new Vector4(ZERO);

    protected double fogStart = 0;
    protected double fogEnd = 1;
    protected double fogDensity = 1;

    protected FogMode fogMode = FogMode.EXP;
    protected boolean fogEnabled = false;

    // global lighting
    protected final Vector4 globalAmbient = new Vector4(DEFAULT_MAT_A_COLOR);
    protected boolean lightingEnabled = false;
    protected boolean lightingTwoSided = false;
    protected boolean lightingSmoothed = true;

    // lights
    protected LightState[] lights; // "final"

    // material
    protected final Vector4 matDiffuse = new Vector4(DEFAULT_MAT_D_COLOR);
    protected final Vector4 matAmbient = new Vector4(DEFAULT_MAT_A_COLOR);
    protected final Vector4 matSpecular = new Vector4(BLACK);
    protected final Vector4 matEmmissive = new Vector4(BLACK);

    protected double matShininess = 0;

    // primitive size/aa
    protected boolean lineAAEnabled = false;
    protected boolean pointAAEnabled = false;
    protected boolean polyAAEnabled = false;

    protected double lineWidth = 1;
    protected double pointWidth = 1;

    // texturing
    protected int activeTex = 0;
    protected TextureState[] textures = null; // "final"

    // bindings for vbos and rendering
    protected final VertexState vertexBinding = new VertexState(VertexTarget.VERTICES, 0);
    protected final VertexState normalBinding = new VertexState(VertexTarget.NORMALS, 0);
    protected final VertexState colorBinding = new VertexState(VertexTarget.COLORS, 0);
    protected VertexState[] texBindings = null; // "final"

    protected VertexBufferObject arrayVboBinding = null;
    protected int activeArrayVbos = 0;
    protected int activeClientTex = 0;

    // matrix
    protected MatrixMode matrixMode = MatrixMode.MODELVIEW;
    private final Matrix4 dirtyModelView = new Matrix4(); // last set model view that hasn't been sent yet
    private boolean isModelViewDirty = true;

    /**
     * Create an AbstractFixedFunctionRenderer that will use the given
     * RendererDelegate. If this renderer is used with another GlslRenderer on
     * the same context, they should share RendererDelegate instances.
     * 
     * @param delegate The RendererDelegate that completes the implementations
     *            Renderer behavior
     * @throws NullPointerException if delegate is null
     */
    public AbstractFixedFunctionRenderer(RendererDelegate delegate) {
        super(delegate);
    }

    @Override
    public void activate(AbstractSurface surface, OpenGLContext context, ResourceManager resourceManager) {
        super.activate(surface, context, resourceManager);

        // Complete initialization the first time we're activated. We can't do this
        // in the constructor because we didn't have a framework at that point.
        if (lights == null) {
            int numLights = surface.getFramework().getCapabilities().getMaxActiveLights();

            lights = new LightState[numLights];
            for (int i = 0; i < lights.length; i++) {
                lights[i] = new LightState();
            }
            // modify 0th light's colors
            lights[0].specular.set(WHITE);
            lights[0].diffuse.set(WHITE);
        }

        if (textures == null) {
            int numTextures = surface.getFramework().getCapabilities().getMaxFixedPipelineTextures();

            textures = new TextureState[numTextures];
            for (int i = 0; i < textures.length; i++) {
                textures[i] = new TextureState(i);
            }
        }

        if (texBindings == null) {
            int numBindings = surface.getFramework().getCapabilities().getMaxTextureCoordinates();

            texBindings = new VertexState[numBindings];
            for (int i = 0; i < texBindings.length; i++) {
                texBindings[i] = new VertexState(VertexTarget.TEXCOORDS, i);
            }
        }
    }

    @Override
    public void reset() {
        super.reset();

        setModelViewMatrix(IDENTITY);
        setProjectionMatrix(IDENTITY);

        setAlphaTest(Comparison.ALWAYS, 1f);

        setFogColor(ZERO);
        setFogExponential(1f, false);
        setFogEnabled(false);

        setGlobalAmbientLight(DEFAULT_MAT_A_COLOR);
        setLightingEnabled(false);
        setSmoothedLightingEnabled(true);
        setTwoSidedLightingEnabled(false);

        setMaterial(DEFAULT_MAT_A_COLOR, DEFAULT_MAT_D_COLOR, BLACK, BLACK);
        setMaterialShininess(0f);

        setLineAntiAliasingEnabled(false);
        setPointAntiAliasingEnabled(false);
        setPolygonAntiAliasingEnabled(false);

        setLineSize(1f);
        setPointSize(1f);

        // reset all lights
        for (int i = 0; i < lights.length; i++) {
            setLightEnabled(i, false);
            if (lights[i].modifiedSinceReset) {
                // special check to avoid repeated no-point calls
                setLightPosition(i, DEFAULT_LIGHT_POS);
                setSpotlight(i, DEFAULT_SPOT_DIR, 180.0);
                lights[i].modifiedSinceReset = false;
            }

            if (i == 0) {
                setLightColor(i, BLACK, WHITE, WHITE);
            } else {
                setLightColor(i, BLACK, BLACK, BLACK);
            }

            setLightAttenuation(i, 1.0, 0.0, 0.0);
        }

        // reset all textures
        for (int i = 0; i < textures.length; i++) {
            setTexture(i, null);

            setTextureColor(i, ZERO);

            setTextureCombineRGB(i, CombineFunction.MODULATE,
                                 CombineSource.CURR_TEX, CombineOperand.COLOR,
                                 CombineSource.PREV_TEX, CombineOperand.COLOR,
                                 CombineSource.CONST_COLOR, CombineOperand.ALPHA);
            setTextureCombineAlpha(i, CombineFunction.MODULATE,
                                   CombineSource.CURR_TEX, CombineOperand.ALPHA,
                                   CombineSource.PREV_TEX, CombineOperand.ALPHA,
                                   CombineSource.CONST_COLOR, CombineOperand.ALPHA);

            setTextureCoordGeneration(i, TexCoordSource.ATTRIBUTE);

            if (textures[i].transformModifiedSinceReset) {
                // special check to only do it if something was modified
                setTextureTransform(i, IDENTITY);
                setTextureEyePlane(i, TexCoord.S, DEFAULT_S_PLANE);
                setTextureEyePlane(i, TexCoord.T, DEFAULT_T_PLANE);
                setTextureEyePlane(i, TexCoord.R, DEFAULT_RQ_PLANE);
                setTextureEyePlane(i, TexCoord.Q, DEFAULT_RQ_PLANE);
                textures[i].transformModifiedSinceReset = false;
            }

            setTextureObjectPlane(i, TexCoord.S, DEFAULT_S_PLANE);
            setTextureObjectPlane(i, TexCoord.T, DEFAULT_T_PLANE);
            setTextureObjectPlane(i, TexCoord.R, DEFAULT_RQ_PLANE);
            setTextureObjectPlane(i, TexCoord.Q, DEFAULT_RQ_PLANE);
        }

        // reset attribute binding
        setVertices(null);
        setNormals(null);
        setColors(null);
        for (int i = 0; i < texBindings.length; i++) {
            setTextureCoordinates(i, null);
        }
    }

    @Override
    public int render(PolygonType polyType, VertexBufferObject indices, int offset, int count) {
        flushModelView();
        return super.render(polyType, indices, offset, count);
    }

    @Override
    public int render(PolygonType polyType, int first, int count) {
        flushModelView();
        return super.render(polyType, first, count);
    }

    @Override
    public void setModelViewMatrix(@Const Matrix4 matrix) {
        if (matrix == null) {
            throw new NullPointerException("Matrix cannot be null");
        }

        dirtyModelView.set(matrix);
        isModelViewDirty = true;
    }

    /**
     * Invoke OpenGL calls to set the matrix mode
     */
    protected abstract void glMatrixMode(MatrixMode mode);

    /**
     * Invoke OpenGL calls to set the matrix for the current mode
     */
    protected abstract void glSetMatrix(@Const Matrix4 matrix);

    private void setMatrixMode(MatrixMode mode) {
        if (matrixMode != mode) {
            matrixMode = mode;
            glMatrixMode(mode);
        }
    }

    private void flushModelView() {
        if (isModelViewDirty) {
            setMatrixMode(MatrixMode.MODELVIEW);
            glSetMatrix(dirtyModelView);
            isModelViewDirty = false;
        }
    }

    @Override
    public void setProjectionMatrix(@Const Matrix4 matrix) {
        if (matrix == null) {
            throw new NullPointerException("Matrix cannot be null");
        }

        setMatrixMode(MatrixMode.PROJECTION);
        glSetMatrix(matrix);
    }

    @Override
    public void setAlphaTest(Comparison test, double refValue) {
        if (test == null) {
            throw new NullPointerException("Null comparison");
        }
        if (alphaTest != test || alphaRefValue != refValue) {
            alphaTest = test;
            alphaRefValue = refValue;
            glAlphaTest(test, refValue);
        }
    }

    /**
     * Invoke OpenGL calls to set the alpha test
     */
    protected abstract void glAlphaTest(Comparison test, double ref);

    @Override
    public void setFogColor(@Const Vector4 color) {
        if (color == null) {
            throw new NullPointerException("Null fog color");
        }
        if (!fogColor.equals(color)) {
            fogColor.set(color);
            glFogColor(color);
        }
    }

    /**
     * Invoke OpenGL calls to set the fog color. Don't need to clamp since
     * OpenGL does that for us.
     */
    protected abstract void glFogColor(@Const Vector4 color);

    @Override
    public void setFogEnabled(boolean enable) {
        if (fogEnabled != enable) {
            fogEnabled = enable;
            glEnableFog(enable);
        }
    }

    /**
     * Invoke OpenGL calls to enable fog
     */
    protected abstract void glEnableFog(boolean enable);

    @Override
    public void setFogExponential(double density, boolean squared) {
        if (density < 0f) {
            throw new IllegalArgumentException("Density must be >= 0, not: " + density);
        }

        if (fogDensity != density) {
            fogDensity = density;
            glFogDensity(density);
        }

        if (squared && fogMode != FogMode.EXP_SQUARED) {
            fogMode = FogMode.EXP_SQUARED;
            glFogMode(FogMode.EXP_SQUARED);
        } else if (fogMode != FogMode.EXP) {
            fogMode = FogMode.EXP;
            glFogMode(FogMode.EXP);
        }
    }

    /**
     * Invoke OpenGL calls to set the fog density
     */
    protected abstract void glFogDensity(double density);

    @Override
    public void setFogLinear(double start, double end) {
        if (end <= start) {
            throw new IllegalArgumentException("Illegal start/end range: " + start + ", " + end);
        }

        if (fogStart != start || fogEnd != end) {
            fogStart = start;
            fogEnd = end;
            glFogRange(start, end);
        }

        if (fogMode != FogMode.LINEAR) {
            fogMode = FogMode.LINEAR;
            glFogMode(FogMode.LINEAR);
        }
    }

    /**
     * Invoke OpenGL calls to set the linear fog range
     */
    protected abstract void glFogRange(double start, double end);

    /**
     * Invoke OpenGL calls to set the fog equation
     */
    protected abstract void glFogMode(FogMode fog);

    private void clamp(@Const Vector4 input, double min, double max, Vector4 out) {
        out.set(Math.max(min, Math.min(input.x, max)),
                Math.max(min, Math.min(input.y, max)),
                Math.max(min, Math.min(input.z, max)),
                Math.max(min, Math.min(input.w, max)));
    }

    @Override
    public void setGlobalAmbientLight(@Const Vector4 ambient) {
        if (ambient == null) {
            throw new NullPointerException("Null global ambient color");
        }
        if (!globalAmbient.equals(ambient)) {
            clamp(ambient, 0, Float.MAX_VALUE, globalAmbient);
            glGlobalLighting(globalAmbient);
        }
    }

    /**
     * Invoke OpenGL calls to set the global lighting color
     */
    protected abstract void glGlobalLighting(@Const Vector4 ambient);

    @Override
    public void setLightColor(int light, @Const Vector4 amb, @Const Vector4 diff, @Const Vector4 spec) {
        if (amb == null || diff == null || spec == null) {
            throw new NullPointerException("Colors cannot be null");
        }

        LightState l = lights[light];
        if (!l.ambient.equals(amb)) {
            clamp(amb, 0, Float.MAX_VALUE, l.ambient);
            glLightColor(light, LightColor.AMBIENT, l.ambient);
        }
        if (!l.diffuse.equals(diff)) {
            clamp(diff, 0, Float.MAX_VALUE, l.diffuse);
            glLightColor(light, LightColor.DIFFUSE, l.diffuse);
        }
        if (!l.specular.equals(spec)) {
            clamp(spec, 0, Float.MAX_VALUE, l.specular);
            glLightColor(light, LightColor.SPECULAR, l.specular);
        }
    }

    /**
     * Invoke OpenGL calls to set the light color for the given light. The color
     * has already been clamped correctly.
     */
    protected abstract void glLightColor(int light, LightColor lc, @Const Vector4 color);

    @Override
    public void setLightEnabled(int light, boolean enable) {
        LightState l = lights[light];
        if (l.enabled != enable) {
            l.enabled = enable;
            glEnableLight(light, enable);
        }
    }

    /**
     * Invoke OpenGL calls to enable a specific light
     */
    protected abstract void glEnableLight(int light, boolean enable);

    @Override
    public void setLightPosition(int light, @Const Vector4 pos) {
        if (pos == null) {
            throw new NullPointerException("Light position can't be null");
        }
        if (pos.w != 0.0 && pos.w != 1.0) {
            throw new NullPointerException("pos.w must be 0 or 1, not: " + pos.w);
        }

        // always set the light position since pos will be transformed by
        // the current matrix
        lights[light].modifiedSinceReset = true;
        flushModelView();
        glLightPosition(light, pos);
    }

    /**
     * Invoke OpenGL calls to set a light's position vector
     */
    protected abstract void glLightPosition(int light, @Const Vector4 pos);

    @Override
    public void setSpotlight(int light, @Const Vector3 dir, double angle) {
        if (dir == null) {
            throw new NullPointerException("Spotlight direction can't be null");
        }
        if ((angle < 0f || angle > 90f) && angle != 180f) {
            throw new IllegalArgumentException("Spotlight angle must be in [0, 90] or be 180, not: " + angle);
        }

        LightState l = lights[light];
        if (l.spotAngle != angle) {
            l.spotAngle = angle;
            glLightAngle(light, angle);
        }

        // always set the spotlight direction since it will be transformed
        // by the current matrix
        l.modifiedSinceReset = true;
        flushModelView();
        glLightDirection(light, dir);
    }

    /**
     * Invoke OpenGL calls to set a light's spotlight direction
     */
    protected abstract void glLightDirection(int light, @Const Vector3 dir);

    /**
     * Invoke OpenGL calls to set a light's spotlight angle
     */
    protected abstract void glLightAngle(int light, double angle);

    @Override
    public void setLightAttenuation(int light, double constant, double linear, double quadratic) {
        if (constant < 0f) {
            throw new IllegalArgumentException("Constant factor must be positive: " + constant);
        }
        if (linear < 0f) {
            throw new IllegalArgumentException("Linear factor must be positive: " + linear);
        }
        if (quadratic < 0f) {
            throw new IllegalArgumentException("Quadratic factor must be positive: " + quadratic);
        }

        LightState l = lights[light];
        if (l.constAtt != constant || l.linAtt != linear || l.quadAtt != quadratic) {
            l.constAtt = constant;
            l.linAtt = linear;
            l.quadAtt = quadratic;
            glLightAttenuation(light, constant, linear, quadratic);
        }
    }

    /**
     * Invoke OpenGL calls to set a light's attenuation factors
     */
    protected abstract void glLightAttenuation(int light, double constant, double linear, double quadratic);

    @Override
    public void setLightingEnabled(boolean enable) {
        if (lightingEnabled != enable) {
            lightingEnabled = enable;
            glEnableLighting(enable);
        }
    }

    /**
     * Invoke OpenGL calls to enable lighting
     */
    protected abstract void glEnableLighting(boolean enable);

    @Override
    public void setSmoothedLightingEnabled(boolean smoothed) {
        if (lightingSmoothed != smoothed) {
            lightingSmoothed = smoothed;
            glEnableSmoothShading(smoothed);
        }
    }

    @Override
    public void setTwoSidedLightingEnabled(boolean enable) {
        if (lightingTwoSided != enable) {
            lightingTwoSided = enable;
            glEnableTwoSidedLighting(enable);
        }
    }

    /**
     * Invoke OpenGL calls to set smooth shading
     */
    protected abstract void glEnableSmoothShading(boolean enable);

    /**
     * Invoke OpenGL calls to set two-sided lighting
     */
    protected abstract void glEnableTwoSidedLighting(boolean enable);

    @Override
    public void setLineAntiAliasingEnabled(boolean enable) {
        if (lineAAEnabled != enable) {
            lineAAEnabled = enable;
            glEnableLineAntiAliasing(enable);
        }
    }

    /**
     * Invoke OpenGL calls to enable line aa
     */
    protected abstract void glEnableLineAntiAliasing(boolean enable);

    @Override
    public void setLineSize(double width) {
        if (width < 1f) {
            throw new IllegalArgumentException("Line width must be at least 1, not: " + width);
        }
        if (lineWidth != width) {
            lineWidth = width;
            glLineWidth(width);
        }
    }

    /**
     * Invoke OpenGL calls to set line width
     */
    protected abstract void glLineWidth(double width);

    @Override
    public void setMaterial(@Const Vector4 amb, @Const Vector4 diff, @Const Vector4 spec, @Const Vector4 emm) {
        if (amb == null || diff == null || spec == null || emm == null) {
            throw new NullPointerException("Material colors can't be null: " + amb + ", " + diff + ", " + spec + ", " + emm);
        }
        if (!matAmbient.equals(amb)) {
            clamp(amb, 0, 1, matAmbient);
            glMaterialColor(LightColor.AMBIENT, matAmbient);
        }

        if (!matDiffuse.equals(diff)) {
            clamp(diff, 0, 1, matDiffuse);
            glMaterialColor(LightColor.DIFFUSE, matDiffuse);
        }

        if (!matSpecular.equals(spec)) {
            matSpecular.set(spec);
            clamp(spec, 0, 1, matSpecular);
            glMaterialColor(LightColor.SPECULAR, matSpecular);
        }

        if (!matEmmissive.equals(emm)) {
            clamp(emm, 0, Float.MAX_VALUE, matEmmissive);
            glMaterialColor(LightColor.EMISSIVE, matEmmissive);
        }
    }

    /**
     * Invoke OpenGL calls to set the material color for the LightColor. The
     * color has already been clamped correctly.
     */
    protected abstract void glMaterialColor(LightColor component, @Const Vector4 color);

    @Override
    public void setMaterialShininess(double shininess) {
        if (shininess < 0.0 || shininess > 128.0) {
            throw new IllegalArgumentException("Shininess must be in [0, 128], not: " + shininess);
        }
        if (matShininess != shininess) {
            matShininess = shininess;
            glMaterialShininess(shininess);
        }
    }

    /**
     * Invoke OpenGL calls to set the material shininess
     */
    protected abstract void glMaterialShininess(double shininess);

    @Override
    public void setPointAntiAliasingEnabled(boolean enable) {
        if (pointAAEnabled != enable) {
            pointAAEnabled = enable;
            glEnablePointAntiAliasing(enable);
        }
    }

    /**
     * Invoke OpenGL calls to enable point aa
     */
    protected abstract void glEnablePointAntiAliasing(boolean enable);

    @Override
    public void setPointSize(double width) {
        if (width < 1.0) {
            throw new IllegalArgumentException("Point width must be at least 1, not: " + width);
        }
        if (pointWidth != width) {
            pointWidth = width;
            glPointWidth(width);
        }
    }

    /**
     * Invoke OpenGL calls to set point width
     */
    protected abstract void glPointWidth(double width);

    @Override
    public void setPolygonAntiAliasingEnabled(boolean enable) {
        if (polyAAEnabled != enable) {
            polyAAEnabled = enable;
            glEnablePolyAntiAliasing(enable);
        }
    }

    /**
     * Invoke OpenGL calls to enable polygon aa
     */
    protected abstract void glEnablePolyAntiAliasing(boolean enable);

    @Override
    public void setTexture(int tex, Texture image) {
        TextureState t = textures[tex];
        if (t.texture != image) {
            // Release current texture if need-be
            Target oldTarget = null;
            if (t.texture != null) {
                resourceManager.unlock(t.texture);
                oldTarget = t.handle.target;
                t.texture = null;
                t.handle = null;
            }

            // Lock new texture if needed
            TextureHandle newHandle = null;
            Target newTarget = null;
            if (image != null) {
                newHandle = (TextureHandle) resourceManager.lock(context, image);
                if (newHandle != null) {
                    newTarget = newHandle.target;

                    t.texture = image;
                    t.handle = newHandle;
                }
            }

            // Update the active texture unit
            setTextureUnit(tex);

            if (oldTarget != null && oldTarget != newTarget) {
                // Unbind old texture and disable old target
                glEnableTexture(oldTarget, false);
                glBindTexture(oldTarget, null);
            }

            if (newHandle != null) {
                // Enable new target (old target was already disabled if needed)
                if (newTarget != oldTarget) {
                    glEnableTexture(newTarget, true);
                }

                // Bind new texture
                glBindTexture(newTarget, newHandle);
            }
        }
    }

    /**
     * Invoke OpenGL calls to bind a Texture to the active texture
     */
    protected abstract void glBindTexture(Target target, TextureHandle image);

    /**
     * Invoke OpenGL to enable the active texture unit
     */
    protected abstract void glEnableTexture(Target target, boolean enable);

    @Override
    public void setTextureColor(int tex, @Const Vector4 color) {
        if (color == null) {
            throw new NullPointerException("Texture color can't be null");
        }

        TextureState t = textures[tex];
        if (!t.color.equals(color)) {
            t.color.set(color);
            setTextureUnit(tex);
            glTextureColor(color);
        }
    }

    /**
     * Invoke OpenGL calls to set the texture color for the active texture.
     * OpenGL clamps the values for us.
     */
    protected abstract void glTextureColor(@Const Vector4 color);

    @Override
    public void setTextureCombineRGB(int tex, CombineFunction function, CombineSource src0,
                                     CombineOperand op0, CombineSource src1, CombineOperand op1,
                                     CombineSource src2, CombineOperand op2) {
        if (function == null || src0 == null || src1 == null || src2 == null
                || op0 == null || op1 == null || op2 == null) {
            throw new NullPointerException("Arguments cannot be null");
        }

        TextureState t = textures[tex];
        setTextureUnit(tex);
        if (t.rgbFunc != function) {
            t.rgbFunc = function;
            glCombineFunction(function, true);
        }

        if (t.srcRgb[0] != src0) {
            t.srcRgb[0] = src0;
            glCombineSrc(0, src0, true);
        }
        if (t.srcRgb[1] != src1) {
            t.srcRgb[1] = src1;
            glCombineSrc(1, src1, true);
        }
        if (t.srcRgb[2] != src2) {
            t.srcRgb[2] = src2;
            glCombineSrc(2, src2, true);
        }

        if (t.opRgb[0] != op0) {
            t.opRgb[0] = op0;
            glCombineOp(0, op0, true);
        }
        if (t.opRgb[1] != op1) {
            t.opRgb[1] = op1;
            glCombineOp(1, op1, true);
        }
        if (t.opRgb[2] != op2) {
            t.opRgb[2] = op2;
            glCombineOp(2, op2, true);
        }
    }

    @Override
    public void setTextureCombineAlpha(int tex, CombineFunction function, CombineSource src0,
                                       CombineOperand op0, CombineSource src1, CombineOperand op1,
                                       CombineSource src2, CombineOperand op2) {
        if (function == null || src0 == null || src1 == null || src2 == null
                || op0 == null || op1 == null || op2 == null) {
            throw new NullPointerException("Arguments cannot be null");
        }
        if (function == CombineFunction.DOT3_RGB || function == CombineFunction.DOT3_RGBA) {
            throw new IllegalArgumentException("Alpha CombineFunction can't be DOT3_RGB or DOT3_RGBA");
        }
        checkAlphaCombineOperand(op0);
        checkAlphaCombineOperand(op1);
        checkAlphaCombineOperand(op2);

        TextureState t = textures[tex];
        setTextureUnit(tex);
        if (t.alphaFunc != function) {
            t.alphaFunc = function;
            glCombineFunction(function, true);
        }

        if (t.srcAlpha[0] != src0) {
            t.srcAlpha[0] = src0;
            glCombineSrc(0, src0, true);
        }
        if (t.srcAlpha[1] != src1) {
            t.srcAlpha[1] = src1;
            glCombineSrc(1, src1, true);
        }
        if (t.srcAlpha[2] != src2) {
            t.srcRgb[2] = src2;
            glCombineSrc(2, src2, true);
        }

        if (t.opAlpha[0] != op0) {
            t.opAlpha[0] = op0;
            glCombineOp(0, op0, true);
        }
        if (t.opAlpha[1] != op1) {
            t.opAlpha[1] = op1;
            glCombineOp(1, op1, true);
        }
        if (t.opAlpha[2] != op2) {
            t.opAlpha[2] = op2;
            glCombineOp(2, op2, true);
        }
    }

    /**
     * Invoke OpenGL calls to set a combine function, either rgb or alpha
     */
    protected abstract void glCombineFunction(CombineFunction func, boolean rgb);

    private void checkAlphaCombineOperand(CombineOperand op) {
        if (op == CombineOperand.COLOR || op == CombineOperand.ONE_MINUS_COLOR) {
            throw new IllegalArgumentException("Illegal CombineOperand for alpha: " + op);
        }
    }

    /**
     * Invoke OpenGL calls to set the combine source
     */
    protected abstract void glCombineSrc(int operand, CombineSource src, boolean rgb);

    /**
     * Invoke OpenGL calls to set the combine op
     */
    protected abstract void glCombineOp(int operand, CombineOperand op, boolean rgb);

    @Override
    public void setTextureCoordGeneration(int tex, TexCoordSource gen) {
        setTextureCoordGeneration(tex, TexCoord.S, gen);
        setTextureCoordGeneration(tex, TexCoord.T, gen);
        setTextureCoordGeneration(tex, TexCoord.R, gen);
        setTextureCoordGeneration(tex, TexCoord.Q, gen);
    }

    @Override
    public void setTextureCoordGeneration(int tex, TexCoord coord, TexCoordSource gen) {
        if (coord == null) {
            throw new NullPointerException("TexCoord can't be null");
        }
        if (gen == null) {
            throw new NullPointerException("TexCoordSource can't be null");
        }
        if (tex < 0) {
            throw new IllegalArgumentException("Texture unit must be at least 0, not: " + tex);
        }
        if (tex >= textures.length)
        {
            return; // Ignore it
        }

        TextureState t = textures[tex];
        switch(coord) {
        case S:
            if (t.tcS != gen) {
                setTextureUnit(tex);
                if (t.tcS == TexCoordSource.ATTRIBUTE) {
                    glEnableTexGen(coord, true);
                } else if (gen == TexCoordSource.ATTRIBUTE) {
                    glEnableTexGen(coord, false);
                }

                t.tcS = gen;
                glTexGen(coord, gen);
            }
            break;
        case R:
            if (t.tcR != gen) {
                setTextureUnit(tex);
                if (t.tcR == TexCoordSource.ATTRIBUTE) {
                    glEnableTexGen(coord, true);
                } else if (gen == TexCoordSource.ATTRIBUTE) {
                    glEnableTexGen(coord, false);
                }

                t.tcR = gen;
                glTexGen(coord, gen);
            }
            break;
        case T:
            if (t.tcT != gen) {
                setTextureUnit(tex);
                if (t.tcT == TexCoordSource.ATTRIBUTE) {
                    glEnableTexGen(coord, true);
                } else if (gen == TexCoordSource.ATTRIBUTE) {
                    glEnableTexGen(coord, false);
                }

                t.tcT = gen;
                glTexGen(coord, gen);
            }
            break;
        case Q:
            if (t.tcQ != gen) {
                setTextureUnit(tex);
                if (t.tcQ == TexCoordSource.ATTRIBUTE) {
                    glEnableTexGen(coord, true);
                } else if (gen == TexCoordSource.ATTRIBUTE) {
                    glEnableTexGen(coord, false);
                }

                t.tcQ = gen;
                glTexGen(coord, gen);
            }
            break;
        }
    }

    /**
     * Invoke OpenGL to set the coordinate generation for the active texture
     */
    protected abstract void glTexGen(TexCoord coord, TexCoordSource gen);

    /**
     * Invoke OpenGL operations to enable/disable coord generation
     */
    protected abstract void glEnableTexGen(TexCoord coord, boolean enable);

    @Override
    public void setTextureEyePlane(int tex, TexCoord coord, @Const Vector4 plane) {
        if (plane == null) {
            throw new NullPointerException("Eye plane cannot be null");
        }
        if (coord == null) {
            throw new NullPointerException("TexCoord cannot be null");
        }
        if (tex < 0) {
            throw new IllegalArgumentException("Texture unit must be at least 0, not: " + tex);
        }
        if (tex >= textures.length)
        {
            return; // Ignore it
        }

        // always send plane
        flushModelView();
        textures[tex].transformModifiedSinceReset = true;

        setTextureUnit(tex);
        glTexEyePlane(coord, plane);
    }

    /**
     * Invoke OpenGL to set the eye plane for the given coordinate on the active texture
     */
    protected abstract void glTexEyePlane(TexCoord coord, @Const Vector4 plane);

    @Override
    public void setTextureObjectPlane(int tex, TexCoord coord, @Const Vector4 plane) {
        if (plane == null) {
            throw new NullPointerException("Object plane cannot be null");
        }
        if (coord == null) {
            throw new NullPointerException("TexCoord cannot be null");
        }
        if (tex < 0) {
            throw new IllegalArgumentException("Texture unit must be at least 0, not: " + tex);
        }
        if (tex >= textures.length)
        {
            return; // Ignore it
        }

        TextureState t = textures[tex];
        switch(coord) {
        case S:
            if (!t.objPlaneS.equals(plane)) {
                t.objPlaneS.set(plane);
                setTextureUnit(tex);
                glTexObjPlane(coord, plane);
            }
            break;
        case T:
            if (!t.objPlaneT.equals(plane)) {
                t.objPlaneT.set(plane);
                setTextureUnit(tex);
                glTexObjPlane(coord, plane);
            }
            break;
        case R:
            if (!t.objPlaneR.equals(plane)) {
                t.objPlaneR.set(plane);
                setTextureUnit(tex);
                glTexObjPlane(coord, plane);
            }
            break;
        case Q:
            if (!t.objPlaneQ.equals(plane)) {
                t.objPlaneQ.set(plane);
                setTextureUnit(tex);
                glTexObjPlane(coord, plane);
            }
            break;
        }
    }

    /**
     * Invoke OpenGL to set the object plane for the active texture
     */
    protected abstract void glTexObjPlane(TexCoord coord, @Const Vector4 plane);

    @Override
    public void setTextureTransform(int tex, @Const Matrix4 matrix) {
        if (matrix == null) {
            throw new NullPointerException("Matrix cannot be null");
        }
        if (tex < 0) {
            throw new IllegalArgumentException("Texture unit must be at least 0, not: " + tex);
        }
        if (tex >= textures.length)
        {
            return; // Ignore it
        }

        // always send texture matrix
        textures[tex].transformModifiedSinceReset = true;

        setTextureUnit(tex);
        setMatrixMode(MatrixMode.TEXTURE);
        glSetMatrix(matrix);
    }

    private void setTextureUnit(int unit) {
        if (unit != activeTex) {
            activeTex = unit;
            glActiveTexture(unit);
        }
    }

    /**
     * Invoke OpenGL calls to set the active texture unit
     */
    protected abstract void glActiveTexture(int unit);

    @Override
    public void setVertices(VertexAttribute vertices) {
        if (vertices != null && vertices.getElementSize() == 1) {
            throw new IllegalArgumentException("Vertices element size cannot be 1");
        }
        setAttribute(vertexBinding, vertices);
    }

    @Override
    public void setNormals(VertexAttribute normals) {
        if (normals != null && normals.getElementSize() != 3) {
            throw new IllegalArgumentException("Normals element size must be 3");
        }
        setAttribute(normalBinding, normals);
    }

    @Override
    public void setColors(VertexAttribute colors) {
        if (colors != null && colors.getElementSize() != 3 && colors.getElementSize() != 4) {
            throw new IllegalArgumentException("Colors element size must be 3 or 4");
        }
        setAttribute(colorBinding, colors);
        if (colorBinding.handle == null) {
            // per-vertex coloring is disabled, so make sure we have a predictable diffuse color
            glMaterialColor(LightColor.DIFFUSE, DEFAULT_MAT_D_COLOR);
            matDiffuse.set(DEFAULT_MAT_D_COLOR);
        }
    }

    @Override
    public void setTextureCoordinates(int tex, VertexAttribute texCoords) {
        if (tex < 0) {
            throw new IllegalArgumentException("Texture unit must be at least 0");
        }
        if (tex >= texBindings.length)
        {
            return; // ignore it
        }
        setAttribute(texBindings[tex], texCoords);
    }

    private void setAttribute(VertexState state, VertexAttribute attr) {
        if (attr != null) {
            // We are setting a new vertex attribute
            boolean accessDiffers = (state.offset != attr.getOffset() ||
                    state.stride != attr.getStride() ||
                    state.elementSize != attr.getElementSize());
            if (state.vbo != attr.getData() || accessDiffers) {
                // The attributes will be different so must make a change
                VertexBufferObject oldVbo = state.vbo;
                boolean failTypeCheck = false;

                if (state.vbo != null && oldVbo != attr.getData()) {
                    // Unlock the old one
                    resourceManager.unlock(oldVbo);
                    state.vbo = null;
                    state.handle = null;
                }

                if (state.vbo == null) {
                    // Lock the new vbo
                    VertexBufferObjectHandle newHandle = (VertexBufferObjectHandle) resourceManager.lock(context, attr.getData());
                    if (newHandle != null && newHandle.dataType != DataType.FLOAT) {
                        resourceManager.unlock(attr.getData());
                        failTypeCheck = true;

                        state.vbo = null;
                        state.handle = null;
                    } else {
                        // VBO is ready
                        state.vbo = attr.getData();
                        state.handle = newHandle;
                    }
                }

                // Make sure OpenGL is operating on the correct unit for subsequent commands
                state.activateSlot();
                if (state.vbo != null) {
                    // At this point, state.vbo is the new VBO (or possibly old VBO)
                    state.elementSize = attr.getElementSize();
                    state.offset = attr.getOffset();
                    state.stride = attr.getStride();

                    bindArrayVbo(attr.getData(), state.handle, oldVbo);

                    if (oldVbo == null) {
                        glEnableAttribute(state.target, true);
                    }
                    glAttributePointer(state.target, state.handle, state.offset, state.stride, state.elementSize);
                } else if (oldVbo != null) {
                    // Since there was an old vbo we need to clean some things up
                    // which weren't cleaned up when we unlocked the old vbo
                    glEnableAttribute(state.target, false);
                    unbindArrayVboMaybe(oldVbo);
                }

                if (failTypeCheck) {
                    throw new IllegalArgumentException("VertexAttribute type must be FLOAT");
                }
            }
        } else {
            // The attribute is meant to be unbound
            if (state.vbo != null) {
                // Change the slot to support multiple texture coordinates
                state.activateSlot();

                // Disable the attribute
                glEnableAttribute(state.target, false);
                // Possibly unbind it from the array vbo
                unbindArrayVboMaybe(state.vbo);

                // Unlock it
                resourceManager.unlock(state.vbo);
                state.vbo = null;
                state.handle = null;
            }
        }
    }

    private void bindArrayVbo(VertexBufferObject vbo, VertexBufferObjectHandle handle, VertexBufferObject oldVboOnSlot) {
        if (vbo != arrayVboBinding) {
            glBindArrayVbo(handle);
            activeArrayVbos = 0;
            arrayVboBinding = vbo;

            // If we're binding the vbo, then the last vbo on the slot doesn't matter
            // since it wasn't counted in the activeArrayVbos counter
            oldVboOnSlot = null;
        }

        // Only update the count if the new vbo isn't replacing the same vbo in the same slot
        if (oldVboOnSlot != vbo) {
            activeArrayVbos++;
        }
    }

    private void unbindArrayVboMaybe(VertexBufferObject vbo) {
        if (vbo == arrayVboBinding) {
            activeArrayVbos--;
            if (activeArrayVbos == 0) {
                glBindArrayVbo(null);
                arrayVboBinding = null;
            }
        }
    }

    /**
     * Invoke OpenGL commands to change the active texture used by client-side
     * state.
     */
    protected abstract void glActiveClientTexture(int unit);

    /**
     * Bind the given resource handle as the array vbo. If null, unbind the
     * array vbo.
     */
    protected abstract void glBindArrayVbo(VertexBufferObjectHandle handle);

    /**
     * Invoke OpenGL commands to set the given attribute pointer. The resource
     * will have already been bound using glBindArrayVbo. If this is for a
     * texture coordinate, glActiveClientTexture will already have been called.
     */
    protected abstract void glAttributePointer(VertexTarget target, VertexBufferObjectHandle handle, int offset, int stride, int elementSize);

    /**
     * Invoke OpenGL commands to enable the given client attribute pointer. If
     * this is for a texture coordinate, glActiveClientTexture will have been
     * called.
     */
    protected abstract void glEnableAttribute(VertexTarget target, boolean enable);
}
