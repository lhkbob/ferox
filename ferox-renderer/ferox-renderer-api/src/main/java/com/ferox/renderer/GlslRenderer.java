package com.ferox.renderer;

import java.util.Map;

import com.ferox.math.ColorRGB;
import com.ferox.math.Const;
import com.ferox.math.Matrix3;
import com.ferox.math.Matrix4;
import com.ferox.math.Vector3;
import com.ferox.math.Vector4;
import com.ferox.resource.GlslShader;
import com.ferox.resource.GlslShader.AttributeType;
import com.ferox.resource.GlslUniform;
import com.ferox.resource.Texture;
import com.ferox.resource.VertexAttribute;

public interface GlslRenderer extends Renderer {
    // FIXME: for advanced shaders, this is the fragment variable to GL_COLOR_ATTACHMENT0+target
    //    and is configured with glBindFragDataLocation
    // for older shaders, they have to write to glFragData[target], so maybe switch
    //  order of arguments, and say null reverts to default output (e.g. glFragData())
    public void bindRenderTarget(String fragmentVariable, int target);

    public void setShader(GlslShader shader);

    public Map<String, AttributeType> getAttributes();

    public Map<String, GlslUniform> getUniforms();

    public void bindAttribute(String glslAttrName, VertexAttribute attr);

    public void bindAttribute(String glslAttrName, int column, VertexAttribute attr);

    public void bindAttribute(String glslAttrName, float val);

    public void bindAttribute(String glslAttrName, float v1, float v2);

    public void bindAttribute(String glslAttrName, @Const Vector3 v);

    public void bindAttribute(String glslAttrName, @Const Vector4 v);

    public void bindAttribute(String glslAttrName, @Const Matrix3 v);

    public void bindAttribute(String glslAttrName, @Const Matrix4 v);

    // FIXME should these be changed to doubles?
    public void setUniform(String name, float val);

    public void setUniform(String name, float v1, float v2);

    public void setUniform(String name, float v1, float v2, float v3);

    public void setUniform(String name, float v1, float v2, float v3, float v4);

    public void setUniform(String name, @Const Vector3 v);

    public void setUniform(String name, @Const Vector4 v);

    public void setUniform(String name, @Const Matrix3 val);

    public void setUniform(String name, @Const Matrix4 val);

    // FIXME should I get rid of the array versions?
    public void setUniform(String name, float[] vals);

    public void setUniform(String name, int val);

    public void setUniform(String name, int v1, int v2);

    public void setUniform(String name, int v1, int v2, int v3);

    public void setUniform(String name, int v1, int v2, int v3, int v4);

    public void setUniform(String name, int[] vals);

    public void setUniform(String name, boolean val);

    public void setUniform(String name, boolean[] vals);

    public void setUniform(String name, Texture texture);

    public void setUniform(String name, @Const ColorRGB color);

    public void setUniform(String name, @Const ColorRGB color, boolean isHDR);
}
