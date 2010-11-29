package com.ferox.renderer.impl;

import com.ferox.math.Color4f;
import com.ferox.math.ReadOnlyMatrix4f;
import com.ferox.math.ReadOnlyVector3f;
import com.ferox.math.ReadOnlyVector4f;
import com.ferox.math.Vector4f;
import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.renderer.GlslRenderer;
import com.ferox.resource.Geometry;
import com.ferox.resource.Texture;
import com.ferox.resource.Texture.Target;

public class FixedFunctionGlslRenderer extends AbstractFixedFunctionRenderer {
    private final GlslRenderer glslRenderer;
    
    public FixedFunctionGlslRenderer(GlslRenderer renderer) {
        
    }

    @Override
    protected void glMatrixMode(MatrixMode mode) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void glSetMatrix(ReadOnlyMatrix4f matrix) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void glAlphaTest(Comparison test, float ref) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void glFogColor(Color4f color) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void glEnableFog(boolean enable) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void glFogDensity(float density) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void glFogRange(float start, float end) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void glFogMode(FogMode fog) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void glGlobalLighting(Color4f ambient) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void glLightColor(int light, LightColor lc, Color4f color) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void glEnableLight(int light, boolean enable) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void glLightPosition(int light, ReadOnlyVector4f pos) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void glLightDirection(int light, ReadOnlyVector3f dir) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void glLightAngle(int light, float angle) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void glLightAttenuation(int light, float constant, float linear, float quadratic) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void glEnableLighting(boolean enable) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void glEnableSmoothShading(boolean enable) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void glEnableTwoSidedLighting(boolean enable) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void glEnableLineAntiAliasing(boolean enable) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void glLineWidth(float width) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void glMaterialColor(LightColor component, Color4f color) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void glMaterialShininess(float shininess) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void glEnablePointAntiAliasing(boolean enable) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void glPointWidth(float width) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void glEnablePolyAntiAliasing(boolean enable) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void glBindTexture(Target target, Texture image) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void glEnableTexture(Target target, boolean enable) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void glTextureColor(Color4f color) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void glCombineFunction(CombineFunction func, boolean rgb) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void glCombineSrc(int operand, CombineSource src, boolean rgb) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void glCombineOp(int operand, CombineOp op, boolean rgb) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void glTexGen(TexCoord coord, TexCoordSource gen) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void glEnableTexGen(TexCoord coord, boolean enable) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void glTexEyePlane(TexCoord coord, ReadOnlyVector4f plane) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void glTexEnvMode(EnvMode mode) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void glTexObjPlane(TexCoord coord, Vector4f plane) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void glActiveTexture(int unit) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void init() {
        // TODO Auto-generated method stub
        
    }
}
