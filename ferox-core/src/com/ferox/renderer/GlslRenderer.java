package com.ferox.renderer;

import java.util.Map;

import com.ferox.math.ReadOnlyColor3f;
import com.ferox.math.ReadOnlyMatrix3f;
import com.ferox.math.ReadOnlyMatrix4f;
import com.ferox.math.ReadOnlyVector3f;
import com.ferox.math.ReadOnlyVector4f;
import com.ferox.resource.GlslShader;
import com.ferox.resource.GlslShader.AttributeType;
import com.ferox.resource.GlslUniform;
import com.ferox.resource.Texture;
import com.ferox.resource.VertexAttribute;

public interface GlslRenderer extends Renderer {
    public void bindRenderTarget(String fragmentVariable, int target);
    
    public void setShader(GlslShader shader);
    
    public Map<String, AttributeType> getAttributes();
    
    public Map<String, GlslUniform> getUniforms();
    
    
    public void bindAttribute(String glslAttrName, VertexAttribute attr);
    
    public void bindAttribute(String glslAttrName, int column, VertexAttribute attr);
    
    public void bindAttribute(String glslAttrName, float val);
    
    public void bindAttribute(String glslAttrName, float v1, float v2);
    
    public void bindAttribute(String glslAttrName, ReadOnlyVector3f v);
    
    public void bindAttribute(String glslAttrName, ReadOnlyVector4f v);
    
    public void bindAttribute(String glslAttrName, ReadOnlyMatrix3f v);
    
    public void bindAttribute(String glslAttrName, ReadOnlyMatrix4f v);
    
    
    public void setUniform(String name, float val);
    
    public void setUniform(String name, float v1, float v2);
    
    public void setUniform(String name, float v1, float v2, float v3);
    
    public void setUniform(String name, float v1, float v2, float v3, float v4);
    
    public void setUniform(String name, ReadOnlyVector3f v);
    
    public void setUniform(String name, ReadOnlyVector4f v);
    
    public void setUniform(String name, ReadOnlyMatrix3f val);
    
    public void setUniform(String name, ReadOnlyMatrix4f val);
    
    public void setUniform(String name, float[] vals);
    
    
    public void setUniform(String name, int val);
    
    public void setUniform(String name, int v1, int v2);
    
    public void setUniform(String name, int v1, int v2, int v3);
    
    public void setUniform(String name, int v1, int v2, int v3, int v4);
    
    public void setUniform(String name, int[] vals);
    
    
    public void setUniform(String name, boolean val);
    
    public void setUniform(String name, boolean[] vals);
    

    public void setUniform(String name, Texture texture);
    
    public void setUniform(String name, ReadOnlyColor3f color);
    
    public void setUniform(String name, ReadOnlyColor3f color, boolean isHDR);
}
