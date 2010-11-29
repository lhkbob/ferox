package com.ferox.renderer.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.ferox.math.ReadOnlyMatrix3f;
import com.ferox.math.ReadOnlyMatrix4f;
import com.ferox.math.ReadOnlyVector3f;
import com.ferox.math.ReadOnlyVector4f;
import com.ferox.renderer.GlslRenderer;
import com.ferox.renderer.RenderCapabilities;
import com.ferox.renderer.impl.resource.GlslShaderHandle;
import com.ferox.renderer.impl.resource.GlslShaderHandle.Attribute;
import com.ferox.renderer.impl.resource.GlslShaderHandle.Uniform;
import com.ferox.renderer.impl.resource.TextureHandle;
import com.ferox.resource.Geometry;
import com.ferox.resource.GlslShader;
import com.ferox.resource.Texture.Target;
import com.ferox.resource.TextureFormat;
import com.ferox.resource.GlslShader.AttributeType;
import com.ferox.resource.GlslUniform;
import com.ferox.resource.GlslUniform.UniformType;
import com.ferox.resource.Texture;

public abstract class AbstractGlslRenderer extends AbstractRenderer implements GlslRenderer {
    protected static class AttributeBinding {
        public final Attribute attr;
        
        public final String[] columnNames;
        public final float[] columnValues; // column-major if multiple columns exist
        public final boolean[] columnValuesValid;
        
        public AttributeBinding(Attribute attr) {
            this.attr = attr;
            columnNames = new String[attr.type.getColumns()];
            columnValues = new float[attr.type.getColumns() * attr.type.getRows()];
            columnValuesValid = new boolean[attr.type.getColumns()];
        }
    }
    
    protected static class UniformBinding {
        public final Uniform uniform;
        
        public final float[] floatValues;
        public final int[] intValues;
        public boolean textureValid;
        
        public UniformBinding(Uniform uniform) {
            this.uniform = uniform;
            textureValid = false;
            
            int length = uniform.uniform.getLength() * uniform.uniform.getType().getPrimitiveCount();
            switch(uniform.uniform.getType()) {
            case FLOAT: case FLOAT_MAT2: case FLOAT_MAT3: case FLOAT_MAT4: 
            case FLOAT_VEC2: case FLOAT_VEC3: case FLOAT_VEC4: 
                floatValues = new float[length];
                intValues = null;
                break;
            case INT: case INT_VEC2: case INT_VEC3: case INT_VEC4: case BOOL:
            case SHADOW_MAP: case TEXTURE_1D: case TEXTURE_2D: case TEXTURE_3D: case TEXTURE_CUBEMAP:
                intValues = new int[length];
                floatValues = null;
                break;
            default:
                intValues = null;
                floatValues = null;
                break;
            }
        }
    }
    
    protected static class TextureBinding {
        public TextureHandle currentTexture;
        public int referenceCount;
        
        public TextureBinding() {
            currentTexture = null;
            referenceCount = 0;
        }
    }
    
    /*
     * Valid type definitions for the different setUniform() calls
     */
    private static final UniformType[] VALID_FLOAT = { UniformType.FLOAT };
    private static final UniformType[] VALID_FLOAT2 = { UniformType.FLOAT_VEC2 };
    private static final UniformType[] VALID_FLOAT3 = { UniformType.FLOAT_VEC3 };
    private static final UniformType[] VALID_FLOAT4 = { UniformType.FLOAT_VEC4, UniformType.FLOAT_MAT2 };
    private static final UniformType[] VALID_FLOAT_MAT3 = { UniformType.FLOAT_MAT3 };
    private static final UniformType[] VALID_FLOAT_MAT4 = { UniformType.FLOAT_MAT4 };
    private static final UniformType[] VALID_FLOAT_ANY = { UniformType.FLOAT, UniformType.FLOAT_VEC2, UniformType.FLOAT_VEC3,
                                                           UniformType.FLOAT_VEC3, UniformType.FLOAT_VEC4, UniformType.FLOAT_MAT2,
                                                           UniformType.FLOAT_MAT3, UniformType.FLOAT_MAT4 };
    private static final UniformType[] VALID_INT = { UniformType.INT };
    private static final UniformType[] VALID_INT2 = { UniformType.INT_VEC2 };
    private static final UniformType[] VALID_INT3 = { UniformType.INT_VEC3 };
    private static final UniformType[] VALID_INT4 = { UniformType.INT_VEC4 };
    private static final UniformType[] VALID_INT_ANY = { UniformType.INT, UniformType.INT_VEC2,
                                                         UniformType.INT_VEC3, UniformType.INT_VEC4 };
    private static final UniformType[] VALID_BOOL = { UniformType.BOOL };
    private static final UniformType[] VALID_T1D = { UniformType.TEXTURE_1D };
    private static final UniformType[] VALID_T2D = { UniformType.TEXTURE_2D, UniformType.SHADOW_MAP };
    private static final UniformType[] VALID_T3D = { UniformType.TEXTURE_3D };
    private static final UniformType[] VALID_TCM = { UniformType.TEXTURE_CUBEMAP };
    private static final UniformType[] VALID_TEXTURE_ANY = { UniformType.TEXTURE_1D, UniformType.TEXTURE_2D,
                                                             UniformType.TEXTURE_3D, UniformType.TEXTURE_CUBEMAP,
                                                             UniformType.SHADOW_MAP };

    
    protected GlslShader currentShader;
    protected GlslShaderHandle currentShaderHandle;
    
    protected AttributeBinding[] attributeBindings;
    protected Map<String, UniformBinding> uniformBindings;
    protected final TextureBinding[] textureBindings;
    
    protected final ResourceManager resourceManager;
    
    // cached float arrays to use for matrix uniform equality checks
    private final float[] tempFloatMat3;
    private final float[] tempFloatMat4;
    
    public AbstractGlslRenderer(RendererDelegate delegate, AbstractFramework framework) {
        super(delegate);
        
        RenderCapabilities caps = framework.getCapabilities();
        int numTextures = Math.max(caps.getMaxVertexShaderTextures(), caps.getMaxFragmentShaderTextures());
        textureBindings = new TextureBinding[numTextures];
        for (int i = 0; i < numTextures; i++)
            textureBindings[i] = new TextureBinding();
        
        resourceManager = framework.getResourceManager();
        tempFloatMat3 = new float[9];
        tempFloatMat4 = new float[16];
    }
    
    private void verifyShader() {
        if (currentShader == null)
            throw new IllegalStateException("No GlslShader in use");
    }

    @Override
    public void setShader(GlslShader shader) {
        if (shader != currentShader) {
            GlslShaderHandle handle = null;

            if (shader != null) {
                // lookup shader handle to get uniform and attr information
                handle = (GlslShaderHandle) resourceManager.getHandle(shader);
                if (handle == null)
                    shader = null;
            }
            
            if (handle == null) {
                // no valid shader, so unbind the current one
                glUseProgram(null);
                
                currentShader = null;
                currentShaderHandle = null;
                attributeBindings = null;
            } else {
                // valid shader
                glUseProgram(handle);
                
                currentShader = shader;
                currentShaderHandle = handle;
                
                // fill in the attribute bindings
                attributeBindings = new AttributeBinding[handle.attributes.size()];
                int i = 0;
                for (Entry<String, Attribute> a: handle.attributes.entrySet()) {
                    attributeBindings[i++] = new AttributeBinding(a.getValue());
                }
                
                // fill in the uniform bindings
                uniformBindings = new HashMap<String, UniformBinding>();
                for (Entry<String, Uniform> u: handle.uniforms.entrySet()) {
                    uniformBindings.put(u.getKey(), new UniformBinding(u.getValue()));
                }
            }
        }
    }
    
    /**
     * Bind the given shader handle so that subsequent invocations of render()
     * will use it as the active GLSL shader. If null, it should unbind the
     * current program.
     */
    protected abstract void glUseProgram(GlslShaderHandle shader);

    @Override
    public Map<String, AttributeType> getAttributes() {
        verifyShader();
        
        Attribute attr;
        Map<String, AttributeType> attrs = new HashMap<String, AttributeType>();
        for (Entry<String, Attribute> a: currentShaderHandle.attributes.entrySet()) {
            attr = a.getValue();
            attrs.put(attr.name, attr.type);
        }
        
        return Collections.unmodifiableMap(attrs);
    }

    @Override
    public Map<String, GlslUniform> getUniforms() {
        verifyShader();
        
        Uniform uniform;
        Map<String, GlslUniform> uniforms = new HashMap<String, GlslUniform>();
        for (Entry<String, Uniform> u: currentShaderHandle.uniforms.entrySet()) {
            uniform = u.getValue();
            uniforms.put(uniform.name, uniform.uniform);
        }
        
        return Collections.unmodifiableMap(uniforms);
    }
    
    @Override
    public void bindAttribute(String glslAttrName, String geometryAttrName) {
        bindAttribute(glslAttrName, 0, geometryAttrName);
    }

    @Override
    public void bindAttribute(String glslAttrName, int column, String geometryAttrName) {
        Attribute a = verifyAttribute(glslAttrName);
        if (a.type == AttributeType.UNSUPPORTED)
            return; // ignore call for unsupported attribute types
        
        if (a.type.getColumns() <= column)
            throw new IllegalArgumentException("GLSL attribute with a type of " + a.type + " cannot use column " + column);
        
        for (int i = 0; i < attributeBindings.length; i++) {
            if (attributeBindings[i].attr == a) {
                attributeBindings[i].columnNames[column] = geometryAttrName;
                attributeBindings[i].columnValuesValid[column] = false; // clear any generic attr values
                onBindAttribute(glslAttrName);
                break;
            }
        }
    }
    
    private Attribute verifyAttribute(String glslAttrName) {
        if (glslAttrName == null)
            throw new NullPointerException("GLSL attribute name cannot be null");
        verifyShader();
        
        Attribute a = currentShaderHandle.attributes.get(glslAttrName);
        if (a == null)
            throw new IllegalArgumentException("GLSL attribute is not used in the current program: " + glslAttrName);
        return a;
    }
    
    private AttributeBinding getAttributeBinding(String glslAttrName) {
        Attribute a = verifyAttribute(glslAttrName);
        if (a.type == AttributeType.UNSUPPORTED)
            return null;
        
        for (int i = 0; i < attributeBindings.length; i++) {
            if (attributeBindings[i].attr == a)
                return attributeBindings[i];
        }
        return null; // shouldn't happen
    }
    
    private void bindAttribute(AttributeBinding a, int col, int rowCount, float v1, float v2, float v3, float v4) {
        if (a == null)
            return; // unsupported or unknown binding, ignore at this stage
        if (a.attr.type.getColumns() <= col)
            throw new IllegalArgumentException("GLSL attribute with a type of " + a.attr.type + " cannot use column " + col);
        if (a.attr.type.getRows() != rowCount)
            throw new IllegalArgumentException("GLSL attribute with a type of " + a.attr.type + " cannot use " + rowCount + " rows");
        
        a.columnNames[col] = null; // clear any name binding
        a.columnValuesValid[col] = true;
        
        int index = col * rowCount;
        switch(rowCount) {
        case 4:
            a.columnValues[index + 3] = v4;
        case 3:
            a.columnValues[index + 2] = v3;
        case 2:
            a.columnValues[index + 1] = v2;
        case 1:
            a.columnValues[index] = v1;
        }
    }

    @Override
    public void bindAttribute(String glslAttrName, float val) {
        AttributeBinding ab = getAttributeBinding(glslAttrName);
        bindAttribute(ab, 0, 1, val, -1f, -1f, -1f);
        onBindAttribute(glslAttrName);
    }

    @Override
    public void bindAttribute(String glslAttrName, float v1, float v2) {
        AttributeBinding ab = getAttributeBinding(glslAttrName);
        bindAttribute(ab, 0, 2, v1, v2, -1f, -1f);
        onBindAttribute(glslAttrName);
    }

    @Override
    public void bindAttribute(String glslAttrName, ReadOnlyVector3f v) {
        AttributeBinding ab = getAttributeBinding(glslAttrName);
        bindAttribute(ab, 0, 3, v.getX(), v.getY(), v.getZ(), -1f);
        onBindAttribute(glslAttrName);
    }

    @Override
    public void bindAttribute(String glslAttrName, ReadOnlyVector4f v) {
        AttributeBinding ab = getAttributeBinding(glslAttrName);
        bindAttribute(ab, 0, 4, v.getX(), v.getY(), v.getZ(), v.getW());
        onBindAttribute(glslAttrName);
    }

    @Override
    public void bindAttribute(String glslAttrName, ReadOnlyMatrix3f v) {
        AttributeBinding ab = getAttributeBinding(glslAttrName);

        // set in reverse order so that row/col errors fail at the beginning
        bindAttribute(ab, 2, 3, v.get(0, 2), v.get(1, 2), v.get(2, 2), -1f);
        bindAttribute(ab, 1, 3, v.get(0, 1), v.get(1, 1), v.get(2, 1), -1f);
        bindAttribute(ab, 0, 3, v.get(0, 0), v.get(1, 0), v.get(2, 0), -1f);
        
        onBindAttribute(glslAttrName);
    }

    @Override
    public void bindAttribute(String glslAttrName, ReadOnlyMatrix4f v) {
        AttributeBinding ab = getAttributeBinding(glslAttrName);

        // set in reverse order so that row/col errors fail at the beginning
        bindAttribute(ab, 3, 4, v.get(0, 3), v.get(1, 3), v.get(2, 3), v.get(3, 3));
        bindAttribute(ab, 2, 4, v.get(0, 2), v.get(1, 2), v.get(2, 2), v.get(3, 2));
        bindAttribute(ab, 1, 4, v.get(0, 1), v.get(1, 1), v.get(2, 1), v.get(3, 1));
        bindAttribute(ab, 0, 4, v.get(0, 0), v.get(1, 0), v.get(2, 0), v.get(3, 0));
        
        onBindAttribute(glslAttrName);
    }
    
    protected abstract void onBindAttribute(String glslAttrName);
    
    private UniformBinding verifyUniform(String name, UniformType[] validTypes) {
        if (name == null)
            throw new NullPointerException("Uniform name cannot be null");
        verifyShader();
        UniformBinding u = uniformBindings.get(name);
        if (u == null)
            throw new IllegalArgumentException("Uniform not used in current program: " + name);
        
        UniformType type = u.uniform.uniform.getType();
        if (type == UniformType.UNSUPPORTED)
            return null;
        
        boolean valid = false;
        for (int i = 0; i < validTypes.length; i++) {
            if (validTypes[i] == type) {
                valid = true;
                break;
            }
        }
        
        if (!valid) {
            String validTypeStr = Arrays.toString(validTypes);
            throw new IllegalArgumentException("Expected a type in " + validTypeStr + " but uniform " + name + " is " + type);
        }
        return u;
    }
    
    @Override
    public void setUniform(String name, float val) {
        UniformBinding u = verifyUniform(name, VALID_FLOAT);
        if (u == null)
            return; // ignore unsupported uniforms
        
        if (u.floatValues[0] != val) {
            u.floatValues[0] = val;
            glUniform(u.uniform, u.floatValues, 1);
        }
    }
    
    @Override
    public void setUniform(String name, float v1, float v2) {
        UniformBinding u = verifyUniform(name, VALID_FLOAT2);
        if (u == null)
            return; // ignore unsupported uniforms
        if (u.floatValues[0] != v1 || u.floatValues[1] != v2) {
            u.floatValues[0] = v1;
            u.floatValues[1] = v2;
            glUniform(u.uniform, u.floatValues, 1);
        }
    }

    /**
     * Set the given uniform's values. The uniform could have any of the FLOAT_
     * types and could possibly be an array.
     */
    protected abstract void glUniform(Uniform u, float[] values, int count);

    @Override
    public void setUniform(String name, float v1, float v2, float v3) {
        UniformBinding u = verifyUniform(name, VALID_FLOAT3);
        if (u == null)
            return; // ignore unsupported uniforms
        
        if (u.floatValues[0] != v1 || u.floatValues[1] != v2 || u.floatValues[2] != v3) {
            u.floatValues[0] = v1;
            u.floatValues[1] = v2;
            u.floatValues[2] = v3;
            glUniform(u.uniform, u.floatValues, 1);
        }
    }

    @Override
    public void setUniform(String name, float v1, float v2, float v3, float v4) {
        UniformBinding u = verifyUniform(name, VALID_FLOAT4);
        if (u == null)
            return; // ignore unsupported uniforms
        
        if (u.floatValues[0] != v1 || u.floatValues[1] != v2 || u.floatValues[2] != v3 || u.floatValues[3] != v4) {
            u.floatValues[0] = v1;
            u.floatValues[1] = v2;
            u.floatValues[2] = v3;
            u.floatValues[3] = v4;
            
            glUniform(u.uniform, u.floatValues, 1);
        }
    }

    @Override
    public void setUniform(String name, ReadOnlyMatrix3f val) {
        if (val == null)
            throw new NullPointerException("Matrix cannot be null");
        UniformBinding u = verifyUniform(name, VALID_FLOAT_MAT3);
        if (u == null)
            return; // ignore unsupported uniforms
        
        val.get(tempFloatMat3, 0, false);
        if (!Arrays.equals(tempFloatMat3, u.floatValues)) {
            System.arraycopy(tempFloatMat3, 0, u.floatValues, 0, 9);
            glUniform(u.uniform, u.floatValues, 1);
        }
    }
    
    @Override
    public void setUniform(String name, ReadOnlyMatrix4f val) {
        if (val == null)
            throw new NullPointerException("Matrix cannot be null");
        UniformBinding u = verifyUniform(name, VALID_FLOAT_MAT4);
        if (u == null)
            return; // ignore unsupported uniforms
        
        val.get(tempFloatMat4, 0, false);
        if (!Arrays.equals(tempFloatMat4, u.floatValues)) {
            System.arraycopy(tempFloatMat4, 0, u.floatValues, 0, 16);
            glUniform(u.uniform, u.floatValues, 1);
        }
    }
    
    @Override
    public void setUniform(String name, ReadOnlyVector3f v) {
        if (v == null)
            throw new NullPointerException("Vector cannot be null");
        setUniform(name, v.getX(), v.getY(), v.getZ());
    }

    @Override
    public void setUniform(String name, ReadOnlyVector4f v) {
        if (v == null)
            throw new NullPointerException("Vector cannot be null");
        setUniform(name, v.getX(), v.getY(), v.getZ(), v.getW());
    }

    @Override
    public void setUniform(String name, float[] vals) {
        UniformBinding u = verifyUniform(name, VALID_FLOAT_ANY);
        if (u == null)
            return; // ignore unsupported uniforms
        
        GlslUniform uniform = u.uniform.uniform;
        int primitiveCount = uniform.getType().getPrimitiveCount();
        if (vals.length % primitiveCount != 0)
            throw new IllegalArgumentException("Length does not align with primitive count of uniform " 
                                               + name + " with type " + uniform.getType());
        
        int totalElements = vals.length / primitiveCount;
        if (totalElements != uniform.getLength())
            throw new IllegalArgumentException("Number of elements ( " + totalElements + ") does not equal the length of uniform " 
                                               + name + " with " + uniform.getLength() + " elements");
        
        // the float array is of the proper length, we assume that it is
        // laid out properly for the uniform's specific float type
        // - also, we don't verify that the array is equal since we expect 
        // - the array equals check to take too long
        System.arraycopy(vals, 0, u.floatValues, 0, vals.length);
        glUniform(u.uniform, u.floatValues, totalElements);
    }

    @Override
    public void setUniform(String name, int val) {
        UniformBinding u = verifyUniform(name, VALID_INT);
        if (u == null)
            return; // ignore unsupported uniforms
        
        if (u.intValues[0] != val) {
            u.intValues[0] = val;
            
            glUniform(u.uniform, u.intValues, 1);
        }
    }

    /**
     * Set the given uniform's values. The uniform could have any of the INT_
     * types, the BOOL type or any of the texture sampler types, and could
     * possibly be an array.
     */
    protected abstract void glUniform(Uniform u, int[] values, int count);

    @Override
    public void setUniform(String name, int v1, int v2) {
        UniformBinding u = verifyUniform(name, VALID_INT2);
        if (u == null)
            return; // ignore unsupported uniforms
        
        if (u.intValues[0] != v1 || u.intValues[1] != v2) {
            u.intValues[0] = v1;
            u.intValues[1] = v2;
            
            glUniform(u.uniform, u.intValues, 1);
        }
    }

    @Override
    public void setUniform(String name, int v1, int v2, int v3) {
        UniformBinding u = verifyUniform(name, VALID_INT3);
        if (u == null)
            return; // ignore unsupported uniforms
        
        if (u.intValues[0] != v1 || u.intValues[1] != v2 || u.intValues[2] != v3) {
            u.intValues[0] = v1;
            u.intValues[1] = v2;
            u.intValues[2] = v3;
            
            glUniform(u.uniform, u.intValues, 1);
        }
    }

    @Override
    public void setUniform(String name, int v1, int v2, int v3, int v4) {
        UniformBinding u = verifyUniform(name, VALID_INT4);
        if (u == null)
            return; // ignore unsupported uniforms
        
        if (u.intValues[0] != v1 || u.intValues[1] != v2 || u.intValues[2] != v3 || u.intValues[3] != v4) {
            u.intValues[0] = v1;
            u.intValues[1] = v2;
            u.intValues[2] = v3;
            u.intValues[3] = v4;
            
            glUniform(u.uniform, u.intValues, 1);
        } 
    }

    @Override
    public void setUniform(String name, int[] vals) {
        if (vals == null)
            throw new NullPointerException("Values array cannot be null");
        
        UniformBinding u = verifyUniform(name, VALID_INT_ANY);
        if (u == null)
            return; // ignore unsupported uniforms
        
        GlslUniform uniform = u.uniform.uniform;
        int primitiveCount = uniform.getType().getPrimitiveCount();
        if (vals.length % primitiveCount != 0)
            throw new IllegalArgumentException("Length does not align with primitive count of uniform " 
                                               + name + " with type " + uniform.getType());
        
        int totalElements = vals.length / primitiveCount;
        if (totalElements != uniform.getLength())
            throw new IllegalArgumentException("Number of elements ( " + totalElements + ") does not equal the length of uniform " 
                                               + name + " with " + uniform.getLength() + " elements");
        
        // the int array is of the proper length, we assume that it is
        // laid out properly for the uniform's specific float type
        // - also, we don't verify that the array is equal since we expect 
        // - the array equals check to take too long
        System.arraycopy(vals, 0, u.intValues, 0, vals.length);
        glUniform(u.uniform, u.intValues, totalElements);
    }

    @Override
    public void setUniform(String name, boolean val) {
        UniformBinding u = verifyUniform(name, VALID_BOOL);
        if (u == null)
            return; // ignore unsupported uniforms
        
        int translated = (val ? 1 : 0);
        if (u.intValues[0] != translated) {
            u.intValues[0] = translated;
            glUniform(u.uniform, u.intValues, 1);
        }
    }
    
    @Override
    public void setUniform(String name, boolean[] vals) {
        UniformBinding u = verifyUniform(name, VALID_BOOL);
        if (u == null)
            return; // ignore unsupported uniforms

        GlslUniform uniform = u.uniform.uniform;
        if (uniform.getLength() != vals.length)
            throw new IllegalArgumentException("Number of elements ( " + vals.length + ") does not equal the length of uniform " 
                                               + name + " with " + uniform.getLength() + " elements");

        // convert the boolean array into an integer array
        for (int i = 0; i < vals.length; i++)
            u.intValues[i] = (vals[i] ? 1 : 0);
        glUniform(u.uniform, u.intValues, vals.length);
    }

    @Override
    public void setUniform(String name, Texture texture) {
        if (texture != null) {
            UniformType[] validTypes = null;
            switch(texture.getTarget()) {
            case T_1D: validTypes = VALID_T1D; break;
            case T_2D: validTypes = VALID_T2D; break;
            case T_3D: validTypes = VALID_T3D; break;
            case T_CUBEMAP: validTypes = VALID_TCM; break;
            }

            UniformBinding u = verifyUniform(name, validTypes);
            if (u.uniform.uniform.getType() == UniformType.SHADOW_MAP && texture.getFormat() != TextureFormat.DEPTH)
                throw new IllegalArgumentException("Shadow map uniforms must be depth textures, not: " + texture.getFormat());

            TextureHandle handle = (TextureHandle) resourceManager.getHandle(texture);
            
            if (u.textureValid) {
                int oldTexUnit = u.intValues[0];
                if (textureBindings[oldTexUnit].currentTexture == handle)
                    return; // no change is needed
                
                // remove reference from old texture unit
                textureBindings[oldTexUnit].referenceCount--;
            }
            
            int newTexUnit = -1;
            // search for existing texture to share the binding
            for (int i = 0; i < textureBindings.length; i++) {
                if (textureBindings[i].currentTexture == handle) {
                    newTexUnit = i;
                    break;
                }
            }
            
            if (newTexUnit < 0) {
                // search for a unit that has no references
                for (int i = 0; i < textureBindings.length; i++) {
                    if (textureBindings[i].referenceCount == 0) {
                        newTexUnit = i;
                        break;
                    }
                }
            }
            
            if (newTexUnit >= 0) {
                // found a valid texture unit to bind to, update reference count
                textureBindings[newTexUnit].referenceCount++;
                
                // bind new texture
                glBindTexture(newTexUnit, handle.target, handle);
                textureBindings[newTexUnit].currentTexture = handle;
                
                // set the uniform to point to the texture unit, too
                u.intValues[0] = newTexUnit;
                u.textureValid = true;
                glUniform(u.uniform, u.intValues, 1);
            } else {
                // there is no room, shouldn't happen
                u.textureValid = false;
            }
        } else {
            // uniform must be unbound from the texture
            UniformBinding u = verifyUniform(name, VALID_TEXTURE_ANY);
            if (u == null)
                return;
            
            if (u.textureValid) {
                int oldTexUnit = u.intValues[0];
                textureBindings[oldTexUnit].referenceCount--;
                u.textureValid = false;
            }
        }
    }

    /**
     * Bind the given texture provided by the TextureHandle. If the
     * <tt>handle</tt> is null, unbind the texture currently bound to the given
     * target. <tt>tex</tt> represents the texture unit to bind or unbind the
     * texture on, which starts at 0. If the handle is not null, its target will
     * equal the provided target.
     */
    protected abstract void glBindTexture(int tex, Target target, TextureHandle handle);

    @Override
    public int render(Geometry geom) {
        verifyShader();
        return 0;
    }
    
    @Override
    public void reset() {
        super.reset();
        
        // this also clears our uniform cache, but we don't bother with
        // setting default values, that would be too slow and of little use
        setShader(null);
        
        // unbind all textures and reset reference counts to 0
        for (int i = 0; i < textureBindings.length; i++) {
            if (textureBindings[i].currentTexture != null)
                glBindTexture(i, textureBindings[i].currentTexture.target, null);
            
            textureBindings[i].currentTexture = null;
            textureBindings[i].referenceCount = 0;
        }
    }
}
