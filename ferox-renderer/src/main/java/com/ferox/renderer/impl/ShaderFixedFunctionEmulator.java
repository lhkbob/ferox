package com.ferox.renderer.impl;

import com.ferox.math.Const;
import com.ferox.math.Matrix4;
import com.ferox.math.Vector3;
import com.ferox.math.Vector4;
import com.ferox.renderer.*;
import com.ferox.renderer.builder.ShaderBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 *
 */
public class ShaderFixedFunctionEmulator implements FixedFunctionRenderer, Activateable {
    private static final String VERTEX_SHADER = "ffp.vert";
    private static final String FRAGMENT_SHADER = "ffp.frag";

    private final GlslRenderer glsl;

    private final Vector3 cachedFogConfig = new Vector3();
    private final Vector3 cachedAttenuations = new Vector3();

    // lazily allocated during the first activate()
    private Shader shader;
    private ContextState<GlslRenderer> defaultState;

    /*
     * Vertex shader uniforms
     */
    private Shader.Uniform modelviewMatrix; // mat4
    private Shader.Uniform projectionMatrix; // mat4

    private Shader.Uniform enableLighting; // bool

    private Shader.Uniform globalAmbient; // vec4
    private Shader.Uniform[] enableSingleLight; // bool[8]
    private Shader.Uniform[] lightPosition; // vec4[8]
    private Shader.Uniform[] ambientLightColors; // vec4[8]
    private Shader.Uniform[] diffuseLightColors; // vec4[8]
    private Shader.Uniform[] specularLightColors; // vec4[8]
    private Shader.Uniform[] spotlightDirections; // vec3[8]
    private Shader.Uniform[] spotlightCutoffs; // float[8]
    private Shader.Uniform[] spotlightExponents; // float[8]
    private Shader.Uniform[] lightAttenuations; // vec3[8]

    private Shader.Uniform ambientMaterial; // vec4
    private Shader.Uniform specularMaterial; // vec4
    private Shader.Uniform emittedMaterial; // vec4
    private Shader.Uniform shininess; // float

    /*
     * Fragment shader uniforms
     */
    private Shader.Uniform enableAlphaTest; // bool
    private Shader.Uniform alphaTest; // int
    private Shader.Uniform alphaRefValue; // float

    private Shader.Uniform fogConfig; // vec3 0 = start/density 1 = end 2 = signal (0 = linear, > = exp, < = exp squared)
    private Shader.Uniform fogColor; // vec4
    private Shader.Uniform enableFog; // bool

    /*
     * Shader attributes
     */
    private Shader.Attribute vertices;
    private Shader.Attribute normals;
    private Shader.Attribute colors;

    public ShaderFixedFunctionEmulator(GlslRenderer shaderRenderer) {
        glsl = shaderRenderer;
        shader = null;
    }

    @Override
    public void setStencilUpdate(StencilUpdate stencilFail, StencilUpdate depthFail,
                                 StencilUpdate depthPass) {
        glsl.setStencilUpdate(stencilFail, depthFail, depthPass);
    }

    @Override
    public void setStencilUpdateFront(StencilUpdate stencilFail, StencilUpdate depthFail,
                                      StencilUpdate depthPass) {
        glsl.setStencilUpdateFront(stencilFail, depthFail, depthPass);
    }

    @Override
    public void setStencilUpdateBack(StencilUpdate stencilFail, StencilUpdate depthFail,
                                     StencilUpdate depthPass) {
        glsl.setStencilUpdateBack(stencilFail, depthFail, depthPass);
    }

    @Override
    public void setStencilTest(Comparison test, int refValue, int testMask) {
        glsl.setStencilTest(test, refValue, testMask);
    }

    @Override
    public void setStencilTestFront(Comparison test, int refValue, int testMask) {
        glsl.setStencilTestFront(test, refValue, testMask);
    }

    @Override
    public void setStencilTestBack(Comparison test, int refValue, int testMask) {
        glsl.setStencilTestBack(test, refValue, testMask);
    }

    @Override
    public void setStencilTestEnabled(boolean enable) {
        glsl.setStencilTestEnabled(enable);
    }

    @Override
    public void setStencilWriteMask(int mask) {
        glsl.setStencilWriteMask(mask);
    }

    @Override
    public void setStencilWriteMask(int front, int back) {
        glsl.setStencilWriteMask(front, back);
    }

    @Override
    public void setDepthTest(Comparison test) {
        glsl.setDepthTest(test);
    }

    @Override
    public void setDepthWriteMask(boolean mask) {
        glsl.setDepthWriteMask(mask);
    }

    @Override
    public void setDepthOffsets(double factor, double units) {
        glsl.setDepthOffsets(factor, units);
    }

    @Override
    public void setDepthOffsetsEnabled(boolean enable) {
        glsl.setDepthOffsetsEnabled(enable);
    }

    @Override
    public void setDrawStyle(DrawStyle style) {
        glsl.setDrawStyle(style);
    }

    @Override
    public void setDrawStyle(DrawStyle front, DrawStyle back) {
        glsl.setDrawStyle(front, back);
    }

    @Override
    public void setColorWriteMask(boolean red, boolean green, boolean blue, boolean alpha) {
        glsl.setColorWriteMask(red, green, blue, alpha);
    }

    @Override
    public void setBlendingEnabled(boolean enable) {
        glsl.setBlendingEnabled(enable);
    }

    @Override
    public void setBlendColor(@Const Vector4 color) {
        glsl.setBlendColor(color);
    }

    @Override
    public void setBlendMode(BlendFunction function, BlendFactor src, BlendFactor dst) {
        glsl.setBlendMode(function, src, dst);
    }

    @Override
    public void setBlendModeRgb(BlendFunction function, BlendFactor src, BlendFactor dst) {
        glsl.setBlendModeRgb(function, src, dst);
    }

    @Override
    public void setBlendModeAlpha(BlendFunction function, BlendFactor src, BlendFactor dst) {
        glsl.setBlendModeAlpha(function, src, dst);
    }

    @Override
    public void setIndices(ElementBuffer indices) {
        glsl.setIndices(indices);
    }

    @Override
    public int render(PolygonType polyType, int offset, int count) {
        return glsl.render(polyType, offset, count);
    }

    @Override
    public void clear(boolean clearColor, boolean clearDepth, boolean clearStencil) {
        glsl.clear(clearColor, clearDepth, clearStencil);
    }

    @Override
    public void clear(boolean clearColor, boolean clearDepth, boolean clearStencil, @Const Vector4 color,
                      double depth, int stencil) {
        glsl.clear(clearColor, clearDepth, clearStencil, color, depth, stencil);
    }

    @Override
    public void setViewport(int x, int y, int width, int height) {
        glsl.setViewport(x, y, width, height);
    }

    @Override
    public ContextState<FixedFunctionRenderer> getCurrentState() {
        return new WrappedState(glsl.getCurrentState());
    }

    @Override
    public void reset() {
        glsl.setCurrentState(defaultState);
    }

    @Override
    public void setCurrentState(ContextState<FixedFunctionRenderer> state) {
        WrappedState s = (WrappedState) state;
        glsl.setCurrentState(s.realState);
    }

    @Override
    public void setFogEnabled(boolean enable) {
        glsl.setUniform(enableFog, enable);
    }

    @Override
    public void setFogColor(@Const Vector4 color) {
        glsl.setUniform(fogColor, color);
    }

    @Override
    public void setFogLinear(double start, double end) {
        cachedFogConfig.set(start, end, 0.0);
        glsl.setUniform(fogConfig, cachedFogConfig);
    }

    @Override
    public void setFogExponential(double density, boolean squared) {
        cachedFogConfig.set(density, 0.0, squared ? -1.0 : 1.0);
        glsl.setUniform(fogConfig, cachedFogConfig);
    }

    @Override
    public void setPointAntiAliasingEnabled(boolean enable) {
    }

    @Override
    public void setLineAntiAliasingEnabled(boolean enable) {
    }

    @Override
    public void setPolygonAntiAliasingEnabled(boolean enable) {
    }

    @Override
    public void setPointSize(double width) {
    }

    @Override
    public void setLineSize(double width) {
    }

    @Override
    public void setAlphaTest(Comparison test, double refValue) {
        glsl.setUniform(enableAlphaTest, test != Comparison.ALWAYS);
        glsl.setUniform(alphaTest, test.ordinal());
        glsl.setUniform(alphaRefValue, refValue);
    }

    @Override
    public void setLightingEnabled(boolean enable) {
        glsl.setUniform(enableLighting, enable);
    }

    @Override
    public void setGlobalAmbientLight(@Const Vector4 ambient) {
        glsl.setUniform(globalAmbient, ambient);
    }

    @Override
    public void setLightEnabled(int light, boolean enable) {
        glsl.setUniform(enableSingleLight[light], enable);
    }

    @Override
    public void setLightPosition(int light, @Const Vector4 pos) {
        // FIXME validate w
        // FIXME transform into eye space
        glsl.setUniform(lightPosition[light], pos);
    }

    @Override
    public void setLightColor(int light, @Const Vector4 amb, @Const Vector4 diff, @Const Vector4 spec) {
        glsl.setUniform(ambientLightColors[light], amb);
        glsl.setUniform(diffuseLightColors[light], diff);
        glsl.setUniform(specularLightColors[light], spec);
    }

    @Override
    public void setSpotlight(int light, @Const Vector3 dir, double angle, double exponent) {
        // FIXME prenormalize the direction
        // FIXME transform into eye space
        glsl.setUniform(spotlightDirections[light], dir);
        glsl.setUniform(spotlightCutoffs[light], angle);
        glsl.setUniform(spotlightExponents[light], exponent);
    }

    @Override
    public void setLightAttenuation(int light, double constant, double linear, double quadratic) {
        cachedAttenuations.set(constant, linear, quadratic);
        glsl.setUniform(lightAttenuations[light], cachedAttenuations);
    }

    @Override
    public void setMaterial(@Const Vector4 amb, @Const Vector4 diff, @Const Vector4 spec,
                            @Const Vector4 emm) {
        setMaterialAmbient(amb);
        setMaterialDiffuse(diff);
        setMaterialSpecular(spec);
        setMaterialEmissive(emm);
    }

    @Override
    public void setMaterialDiffuse(@Const Vector4 diff) {
        glsl.bindAttribute(colors, diff);
    }

    @Override
    public void setMaterialAmbient(@Const Vector4 amb) {
        glsl.setUniform(ambientMaterial, amb);
    }

    @Override
    public void setMaterialSpecular(@Const Vector4 spec) {
        glsl.setUniform(specularMaterial, spec);
    }

    @Override
    public void setMaterialEmissive(@Const Vector4 emm) {
        glsl.setUniform(emittedMaterial, emm);
    }

    @Override
    public void setMaterialShininess(double shininess) {
        glsl.setUniform(this.shininess, shininess);
    }

    @Override
    public void setTexture(int tex, Sampler image) {
    }

    @Override
    public void setTextureColor(int tex, @Const Vector4 color) {
    }

    @Override
    public void setTextureCoordGeneration(int tex, TexCoordSource gen) {
    }

    @Override
    public void setTextureCoordGeneration(int tex, TexCoord coord, TexCoordSource gen) {
    }

    @Override
    public void setTextureObjectPlane(int tex, TexCoord coord, @Const Vector4 plane) {
    }

    @Override
    public void setTextureObjectPlanes(int tex, @Const Matrix4 planes) {
    }

    @Override
    public void setTextureEyePlane(int tex, TexCoord coord, @Const Vector4 plane) {
    }

    @Override
    public void setTextureEyePlanes(int tex, @Const Matrix4 planes) {
    }

    @Override
    public void setTextureTransform(int tex, @Const Matrix4 matrix) {
    }

    @Override
    public void setTextureCombineRGB(int tex, CombineFunction function, CombineSource src0,
                                     CombineOperand op0, CombineSource src1, CombineOperand op1,
                                     CombineSource src2, CombineOperand op2) {
    }

    @Override
    public void setTextureCombineAlpha(int tex, CombineFunction function, CombineSource src0,
                                       CombineOperand op0, CombineSource src1, CombineOperand op1,
                                       CombineSource src2, CombineOperand op2) {
    }

    @Override
    public void setProjectionMatrix(@Const Matrix4 projection) {
        glsl.setUniform(projectionMatrix, projection);
    }

    @Override
    public void setModelViewMatrix(@Const Matrix4 modelView) {
        glsl.setUniform(modelviewMatrix, modelView);
        // FIXME store modelview so that we can compute eye space params for lights and eye planes
    }

    @Override
    public void setVertices(VertexAttribute vertices) {
        glsl.bindAttribute(this.vertices, vertices);
    }

    @Override
    public void setNormals(VertexAttribute normals) {
        glsl.bindAttribute(this.normals, normals);
    }

    @Override
    public void setColors(VertexAttribute colors) {
        glsl.bindAttribute(this.colors, colors);
    }

    @Override
    public void setTextureCoordinates(int tex, VertexAttribute texCoords) {
    }

    @Override
    public void activate(AbstractSurface surface) {
        if (glsl instanceof Activateable) {
            ((Activateable) glsl).activate(surface);
        }

        if (shader == null) {
            ShaderBuilder shaderBuilder = surface.getFramework().newShader();

            try (BufferedReader vertIn = new BufferedReader(
                    new InputStreamReader(getClass().getResourceAsStream(VERTEX_SHADER)))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = vertIn.readLine()) != null) {
                    sb.append(line).append('\n');
                }
                shaderBuilder.withVertexShader(sb.toString());
            } catch (IOException e) {
                throw new FrameworkException("Unable to load vertex shader for FFP emulation", e);
            }

            try (BufferedReader fragIn = new BufferedReader(
                    new InputStreamReader(getClass().getResourceAsStream(FRAGMENT_SHADER)))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = fragIn.readLine()) != null) {
                    sb.append(line).append('\n');
                }
                shaderBuilder.withFragmentShader(sb.toString());
            } catch (IOException e) {
                throw new FrameworkException("Unable to load fragment shader for FFP emulation", e);
            }

            shaderBuilder.bindColorBuffer("fColor", 0);

            shader = shaderBuilder.build();

            // get references to every uniform available
            alphaRefValue = shader.getUniform("uAlphaRefValue");
            alphaTest = shader.getUniform("uAlphaComparison");
            enableAlphaTest = shader.getUniform("uEnableAlphaTest");

            enableFog = shader.getUniform("uEnableFog");
            fogConfig = shader.getUniform("uFogConfig");
            fogColor = shader.getUniform("uFogColor");

            modelviewMatrix = shader.getUniform("uModelview");
            projectionMatrix = shader.getUniform("uProjection");
            enableLighting = shader.getUniform("uEnableLighting");
            globalAmbient = shader.getUniform("uGlobalLight");

            ambientMaterial = shader.getUniform("uMatAmbient");
            specularMaterial = shader.getUniform("uMatSpecular");
            emittedMaterial = shader.getUniform("uMatEmissive");
            shininess = shader.getUniform("uMatShininess");

            vertices = shader.getAttribute("aVertex");
            normals = shader.getAttribute("aNormal");
            colors = shader.getAttribute("aDiffuse");

            System.out.println(vertices);
            System.out.println(normals);
            System.out.println(colors);

            enableSingleLight = new Shader.Uniform[8];
            lightPosition = new Shader.Uniform[8];
            diffuseLightColors = new Shader.Uniform[8];
            specularLightColors = new Shader.Uniform[8];
            ambientLightColors = new Shader.Uniform[8];
            spotlightDirections = new Shader.Uniform[8];
            spotlightCutoffs = new Shader.Uniform[8];
            spotlightExponents = new Shader.Uniform[8];
            lightAttenuations = new Shader.Uniform[8];
            for (int i = 0; i < 8; i++) {
                enableSingleLight[i] = shader.getUniform("uEnableLight[" + i + "]");
                lightPosition[i] = shader.getUniform("uLightPos[" + i + "]");
                diffuseLightColors[i] = shader.getUniform("uLightDiffuse[" + i + "]");
                specularLightColors[i] = shader.getUniform("uLightSpecular[" + i + "]");
                ambientLightColors[i] = shader.getUniform("uLightAmbient[" + i + "]");
                spotlightDirections[i] = shader.getUniform("uSpotlightDirection[" + i + "]");
                spotlightExponents[i] = shader.getUniform("uSpotlightExponent[" + i + "]");
                spotlightCutoffs[i] = shader.getUniform("uSpotlightCutoff[" + i + "]");
                lightAttenuations[i] = shader.getUniform("uLightAttenuation[" + i + "]");
            }

            // FIXME assign default values to every uniform
            defaultState = glsl.getCurrentState();
        }
    }

    private static class WrappedState implements ContextState<FixedFunctionRenderer> {
        private final ContextState<GlslRenderer> realState;

        public WrappedState(ContextState<GlslRenderer> realState) {
            this.realState = realState;
        }
    }
}
