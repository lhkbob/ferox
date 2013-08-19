package com.ferox.renderer.impl;

import com.ferox.math.*;
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

    private final Vector3 cachedSpotlightDirection = new Vector3();
    private final Vector4 cachedLightPos = new Vector4();

    private final Matrix4 cachedEyePlanes = new Matrix4();

    private final Matrix4 modelview = new Matrix4();
    private final Matrix4 inverseModelview = new Matrix4();
    private final Matrix3 normal = new Matrix3();

    // lazily allocated during the first activate()
    private Shader shader;
    private ContextState<GlslRenderer> defaultState;

    /*
     * Vertex shader uniforms
     */
    private Shader.Uniform modelviewMatrix; // mat4
    private Shader.Uniform projectionMatrix; // mat4
    private Shader.Uniform normalMatrix; // mat3

    private Shader.Uniform enableLighting; // bool

    private Shader.Uniform globalAmbient; // vec4
    private Shader.Uniform enableSingleLight; // bool[8]
    private Shader.Uniform lightPosition; // vec4[8]
    private Shader.Uniform ambientLightColors; // vec4[8]
    private Shader.Uniform diffuseLightColors; // vec4[8]
    private Shader.Uniform specularLightColors; // vec4[8]
    private Shader.Uniform spotlightDirections; // vec3[8]
    private Shader.Uniform spotlightCutoffs; // float[8]
    private Shader.Uniform spotlightExponents; // float[8]
    private Shader.Uniform lightAttenuations; // vec3[8]

    private Shader.Uniform ambientMaterial; // vec4
    private Shader.Uniform specularMaterial; // vec4
    private Shader.Uniform emittedMaterial; // vec4
    private Shader.Uniform shininess; // float

    private Shader.Uniform textureMatrix; // mat4[4]
    private Shader.Uniform objPlanes; // mat4[4]
    private Shader.Uniform eyePlanes; // mat4[4]
    private Shader.Uniform texCoordSource; // int[4]

    /*
     * Fragment shader uniforms
     */
    private Shader.Uniform alphaTest; // int
    private Shader.Uniform alphaRefValue; // float

    private Shader.Uniform fogConfig; // vec3 0 = start/density 1 = end 2 = signal (0 = linear, > = exp, < = exp squared)
    private Shader.Uniform fogColor; // vec4
    private Shader.Uniform enableFog; // bool

    private Shader.Uniform sampler1D; // sampler1D[4]
    private Shader.Uniform sampler2D; // sampler2D[4]
    private Shader.Uniform sampler3D; // sampler3D[4]
    private Shader.Uniform samplerCube; // samplerCube[4]
    private Shader.Uniform texConfig; // int[4]
    private Shader.Uniform depthComparison; // int[4]

    private Shader.Uniform combineSrcAlpha; // ivec3[4]
    private Shader.Uniform combineSrcRGB; // ivec3[4]
    private Shader.Uniform combineOpAlpha; // ivec3[4]
    private Shader.Uniform combineOpRGB; // ivec3[4]
    private Shader.Uniform combineFuncAlpha; // int[4]
    private Shader.Uniform combineFuncRGB; // int[4]
    private Shader.Uniform combineColor; // vec4[4]

    /*
     * Shader attributes
     */
    private Shader.Attribute vertices;
    private Shader.Attribute normals;
    private Shader.Attribute colors;
    private Shader.Attribute texCoords; // vec4[4]

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
        glsl.setPointAntiAliasingEnabled(enable);
    }

    @Override
    public void setLineAntiAliasingEnabled(boolean enable) {
        glsl.setLineAntiAliasingEnabled(enable);
    }

    @Override
    public void setPolygonAntiAliasingEnabled(boolean enable) {
        glsl.setPolygonAntiAliasingEnabled(enable);
    }

    @Override
    public void setPointSize(double width) {
        glsl.setPointSize(width);
    }

    @Override
    public void setLineSize(double width) {
        glsl.setLineSize(width);
    }

    @Override
    public void setAlphaTest(Comparison test, double refValue) {
        // pass -1 to disable completely
        glsl.setUniform(alphaTest, test == Comparison.ALWAYS ? -1 : test.ordinal());
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
        glsl.setUniformArray(enableSingleLight, light, enable);
    }

    @Override
    public void setLightPosition(int light, @Const Vector4 pos) {
        if (pos.w != 0 && pos.w != 1.0) {
            throw new IllegalArgumentException(
                    "Light position must have a w component of 0 or 1, not: " + pos.w);
        }
        cachedLightPos.mul(modelview, pos);
        glsl.setUniformArray(lightPosition, light, cachedLightPos);
    }

    @Override
    public void setLightColor(int light, @Const Vector4 amb, @Const Vector4 diff, @Const Vector4 spec) {
        glsl.setUniformArray(ambientLightColors, light, amb);
        glsl.setUniformArray(diffuseLightColors, light, diff);
        glsl.setUniformArray(specularLightColors, light, spec);
    }

    @Override
    public void setSpotlight(int light, @Const Vector3 dir, double angle, double exponent) {
        cachedSpotlightDirection.transform(modelview, dir, 0.0).normalize();
        glsl.setUniformArray(spotlightDirections, light, cachedSpotlightDirection);
        glsl.setUniformArray(spotlightCutoffs, light, Math.cos(Math.toRadians(angle)));
        glsl.setUniformArray(spotlightExponents, light, exponent);
    }

    @Override
    public void setLightAttenuation(int light, double constant, double linear, double quadratic) {
        cachedAttenuations.set(constant, linear, quadratic);
        glsl.setUniformArray(lightAttenuations, light, cachedAttenuations);
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
        // FIXME set others to null?
        if (image instanceof Texture1D) {
            glsl.setUniformArray(sampler1D, tex, image);
            glsl.setUniformArray(texConfig, tex, 0);
            glsl.setUniformArray(depthComparison, tex, -1);
            System.out.println("setting tex config to 1D for : " + tex);
        } else if (image instanceof Texture2D) {
            glsl.setUniformArray(sampler2D, tex, image);
            glsl.setUniformArray(texConfig, tex, 1);
            glsl.setUniformArray(depthComparison, tex, -1);
            System.out.println("setting tex config to 2D for : " + tex);
        } else if (image instanceof Texture3D) {
            glsl.setUniformArray(sampler3D, tex, image);
            glsl.setUniformArray(texConfig, tex, 2);
            glsl.setUniformArray(depthComparison, tex, -1);
            System.out.println("setting tex config to 3D for : " + tex);
        } else if (image instanceof TextureCubeMap) {
            glsl.setUniformArray(samplerCube, tex, image);
            glsl.setUniformArray(texConfig, tex, 3);
            glsl.setUniformArray(depthComparison, tex, -1);
            System.out.println("setting tex config to cube for : " + tex);
        } else if (image instanceof DepthMap2D) {
            glsl.setUniformArray(sampler2D, tex, image);
            glsl.setUniformArray(texConfig, tex, 1);

            DepthMap2D map = (DepthMap2D) image;
            int compare = (map.getDepthComparison() == null ? -1 : map.getDepthComparison().ordinal());
            glsl.setUniformArray(depthComparison, tex, compare);
            System.out.println("setting tex config to depth2d for : " + tex);

        } else {
            glsl.setUniformArray(texConfig, tex, -1);
        }
    }

    @Override
    public void setTextureColor(int tex, @Const Vector4 color) {
        glsl.setUniformArray(combineColor, tex, color);
    }

    @Override
    public void setTextureCoordinateSource(int tex, TexCoordSource gen) {
        glsl.setUniformArray(texCoordSource, tex, gen.ordinal());
    }

    @Override
    public void setTextureObjectPlanes(int tex, @Const Matrix4 planes) {
        glsl.setUniformArray(objPlanes, tex, planes);
    }

    @Override
    public void setTextureEyePlanes(int tex, @Const Matrix4 planes) {
        cachedEyePlanes.mul(planes, inverseModelview);
        glsl.setUniformArray(eyePlanes, tex, cachedEyePlanes);
    }

    @Override
    public void setTextureTransform(int tex, @Const Matrix4 matrix) {
        glsl.setUniformArray(textureMatrix, tex, matrix);
    }

    @Override
    public void setTextureCombineRGB(int tex, CombineFunction function, CombineSource src0,
                                     CombineOperand op0, CombineSource src1, CombineOperand op1,
                                     CombineSource src2, CombineOperand op2) {
        glsl.setUniform(combineFuncRGB, tex, function.ordinal());
        glsl.setUniform(combineSrcRGB, tex, src0.ordinal(), src1.ordinal(), src2.ordinal());
        glsl.setUniform(combineOpRGB, tex, op0.ordinal(), op1.ordinal(), op2.ordinal());
    }

    @Override
    public void setTextureCombineAlpha(int tex, CombineFunction function, CombineSource src0,
                                       CombineOperand op0, CombineSource src1, CombineOperand op1,
                                       CombineSource src2, CombineOperand op2) {
        glsl.setUniform(combineFuncAlpha, tex, function.ordinal());
        glsl.setUniform(combineSrcAlpha, tex, src0.ordinal(), src1.ordinal(), src2.ordinal());
        glsl.setUniform(combineOpAlpha, tex, op0.ordinal(), op1.ordinal(), op2.ordinal());
    }

    @Override
    public void setProjectionMatrix(@Const Matrix4 projection) {
        glsl.setUniform(projectionMatrix, projection);
    }

    @Override
    public void setModelViewMatrix(@Const Matrix4 modelView) {
        glsl.setUniform(modelviewMatrix, modelView);
        modelview.set(modelView);

        // compute normal matrix
        inverseModelview.inverse(modelView);
        normal.setUpper(inverseModelview).transpose();
        glsl.setUniform(normalMatrix, normal);
    }

    // FIXME validate element size and or type? type should be handled, but element size is more flexible
    // in glsl renderer compared to ffp spec
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
        glsl.bindAttributeArray(this.texCoords, tex, texCoords);
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

            // get references to every uniform available and
            // issue defaults to the uniforms so that we can snapshot that state as the default
            loadVariables();
            loadDefaultState();
        }
    }

    private void loadVariables() {
        alphaRefValue = shader.getUniform("uAlphaRefValue");
        alphaTest = shader.getUniform("uAlphaComparison");

        enableFog = shader.getUniform("uEnableFog");
        fogConfig = shader.getUniform("uFogConfig");
        fogColor = shader.getUniform("uFogColor");

        sampler1D = shader.getUniform("uTex1D");
        sampler2D = shader.getUniform("uTex2D");
        sampler3D = shader.getUniform("uTex3D");
        samplerCube = shader.getUniform("uTexCube");
        texConfig = shader.getUniform("uTexConfig");
        depthComparison = shader.getUniform("uDepthComparison");

        combineSrcAlpha = shader.getUniform("uCombineSrcAlpha");
        combineSrcRGB = shader.getUniform("uCombineSrcRGB");
        combineOpAlpha = shader.getUniform("uCombineOpAlpha");
        combineOpRGB = shader.getUniform("uCombineOpRGB");
        combineFuncAlpha = shader.getUniform("uCombineFuncAlpha");
        combineFuncRGB = shader.getUniform("uCombineFuncRGB");
        combineColor = shader.getUniform("uCombineColor");

        modelviewMatrix = shader.getUniform("uModelview");
        projectionMatrix = shader.getUniform("uProjection");
        normalMatrix = shader.getUniform("uNormalMatrix");
        enableLighting = shader.getUniform("uEnableLighting");
        globalAmbient = shader.getUniform("uGlobalLight");

        ambientMaterial = shader.getUniform("uMatAmbient");
        specularMaterial = shader.getUniform("uMatSpecular");
        emittedMaterial = shader.getUniform("uMatEmissive");
        shininess = shader.getUniform("uMatShininess");

        enableSingleLight = shader.getUniform("uEnableLight");
        lightPosition = shader.getUniform("uLightPos");
        diffuseLightColors = shader.getUniform("uLightDiffuse");
        specularLightColors = shader.getUniform("uLightSpecular");
        ambientLightColors = shader.getUniform("uLightAmbient");
        spotlightDirections = shader.getUniform("uSpotlightDirection");
        spotlightExponents = shader.getUniform("uSpotlightExponent");
        spotlightCutoffs = shader.getUniform("uSpotlightCutoff");
        lightAttenuations = shader.getUniform("uLightAttenuation");

        textureMatrix = shader.getUniform("uTextureMatrix");
        objPlanes = shader.getUniform("uTexGenObjPlanes");
        eyePlanes = shader.getUniform("uTexGenEyePlanes");
        texCoordSource = shader.getUniform("uTexCoordSource");

        vertices = shader.getAttribute("aVertex");
        normals = shader.getAttribute("aNormal");
        colors = shader.getAttribute("aDiffuse");
        texCoords = shader.getAttribute("aTexCoord");
    }

    private void loadDefaultState() {
        glsl.setShader(shader);
        FixedFunctionState defaults = new FixedFunctionState();

        // alpha test uniforms
        glsl.setUniform(alphaRefValue, defaults.alphaRefValue);
        glsl.setUniform(alphaTest,
                        defaults.alphaTest == Comparison.ALWAYS ? -1 : defaults.alphaTest.ordinal());

        // fog uniforms
        glsl.setUniform(fogColor, defaults.fogColor);
        glsl.setUniform(enableFog, defaults.fogEnabled);

        if (defaults.fogMode == FixedFunctionState.FogMode.EXP) {
            cachedFogConfig.set(defaults.fogDensity, 0.0, 1.0);
        } else if (defaults.fogMode == FixedFunctionState.FogMode.EXP_SQUARED) {
            cachedFogConfig.set(defaults.fogDensity, 0.0, -1.0);
        } else {
            cachedFogConfig.set(defaults.fogStart, defaults.fogEnd, 0.0);
        }
        glsl.setUniform(fogConfig, cachedFogConfig);

        // transform uniforms
        modelview.set(defaults.modelView);
        glsl.setUniform(modelviewMatrix, defaults.modelView);
        normal.setUpper(modelview).inverse().transpose();
        glsl.setUniform(normalMatrix, normal);
        glsl.setUniform(projectionMatrix, defaults.projection);

        // lighting
        glsl.setUniform(enableLighting, defaults.lightingEnabled);
        glsl.setUniform(globalAmbient, defaults.globalAmbient);
        for (int i = 0; i < FixedFunctionState.MAX_LIGHTS; i++) {
            glsl.setUniformArray(enableSingleLight, i, defaults.lights[i].enabled);
            glsl.setUniformArray(lightPosition, i, defaults.lights[i].position);
            glsl.setUniformArray(diffuseLightColors, i, defaults.lights[i].diffuse);
            glsl.setUniformArray(specularLightColors, i, defaults.lights[i].specular);
            glsl.setUniformArray(ambientLightColors, i, defaults.lights[i].ambient);
            glsl.setUniformArray(spotlightDirections, i, defaults.lights[i].spotlightDirection);
            glsl.setUniformArray(spotlightCutoffs, i, Math.cos(Math.toRadians(defaults.lights[i].spotAngle)));
            glsl.setUniformArray(spotlightExponents, i, defaults.lights[i].spotExponent);
            cachedAttenuations
                    .set(defaults.lights[i].constAtt, defaults.lights[i].linAtt, defaults.lights[i].quadAtt);
            glsl.setUniformArray(lightAttenuations, i, cachedAttenuations);
        }

        // material
        glsl.setUniform(ambientMaterial, defaults.matAmbient);
        glsl.setUniform(specularMaterial, defaults.matSpecular);
        glsl.setUniform(emittedMaterial, defaults.matEmissive);
        glsl.setUniform(shininess, defaults.matShininess);

        // texturing
        for (int i = 0; i < FixedFunctionState.MAX_TEXTURES; i++) {
            glsl.setUniformArray(textureMatrix, i, defaults.textures[i].textureMatrix);
            glsl.setUniformArray(objPlanes, i, defaults.textures[i].objPlanes);
            glsl.setUniformArray(eyePlanes, i, defaults.textures[i].eyePlanes);
            glsl.setUniformArray(texCoordSource, i, defaults.textures[i].source.ordinal());

            glsl.setUniformArray(combineSrcAlpha, i, defaults.textures[i].srcAlpha[0].ordinal(),
                                 defaults.textures[i].srcAlpha[1].ordinal(),
                                 defaults.textures[i].srcAlpha[2].ordinal());
            glsl.setUniformArray(combineSrcRGB, i, defaults.textures[i].srcRgb[0].ordinal(),
                                 defaults.textures[i].srcRgb[1].ordinal(),
                                 defaults.textures[i].srcRgb[2].ordinal());
            glsl.setUniformArray(combineOpAlpha, i, defaults.textures[i].opAlpha[0].ordinal(),
                                 defaults.textures[i].opAlpha[1].ordinal(),
                                 defaults.textures[i].opAlpha[2].ordinal());
            glsl.setUniformArray(combineOpRGB, i, defaults.textures[i].opRgb[0].ordinal(),
                                 defaults.textures[i].opRgb[1].ordinal(),
                                 defaults.textures[i].opRgb[2].ordinal());
            glsl.setUniformArray(combineFuncAlpha, i, defaults.textures[i].alphaFunc.ordinal());
            glsl.setUniformArray(combineFuncRGB, i, defaults.textures[i].rgbFunc.ordinal());
            glsl.setUniformArray(combineColor, i, defaults.textures[i].color);

            glsl.setUniformArray(texConfig, i, -1); // no textures enabled by default

            glsl.bindAttribute(texCoords, new Vector4(0, 0, 0, 1));
        }

        glsl.bindAttribute(colors, defaults.matDiffuse);
        glsl.bindAttribute(vertices, new Vector4(0, 0, 0, 1));
        glsl.bindAttribute(normals, new Vector3(0, 0, 1));


        defaultState = glsl.getCurrentState();
    }

    private static class WrappedState implements ContextState<FixedFunctionRenderer> {
        private final ContextState<GlslRenderer> realState;

        public WrappedState(ContextState<GlslRenderer> realState) {
            this.realState = realState;
        }
    }
}
