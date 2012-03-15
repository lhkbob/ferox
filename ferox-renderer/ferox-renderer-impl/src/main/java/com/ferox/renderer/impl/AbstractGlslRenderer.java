package com.ferox.renderer.impl;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.ferox.math.ColorRGB;
import com.ferox.math.Const;
import com.ferox.math.Matrix3;
import com.ferox.math.Matrix4;
import com.ferox.math.Vector3;
import com.ferox.math.Vector4;
import com.ferox.renderer.GlslRenderer;
import com.ferox.renderer.RenderCapabilities;
import com.ferox.renderer.Renderer;
import com.ferox.renderer.impl.ResourceManager.LockToken;
import com.ferox.renderer.impl.drivers.GlslShaderHandle;
import com.ferox.renderer.impl.drivers.GlslShaderHandle.Attribute;
import com.ferox.renderer.impl.drivers.GlslShaderHandle.Uniform;
import com.ferox.resource.GlslShader;
import com.ferox.resource.GlslShader.AttributeType;
import com.ferox.resource.GlslUniform;
import com.ferox.resource.GlslUniform.UniformType;
import com.ferox.resource.Resource.Status;
import com.ferox.resource.Texture;
import com.ferox.resource.Texture.Target;
import com.ferox.resource.TextureFormat;
import com.ferox.resource.VertexAttribute;
import com.ferox.resource.VertexBufferObject;

/**
 * <p>
 * The AbstractGlslRenderer is an abstract implementation of
 * {@link GlslRenderer}. It uses a {@link RendererDelegate} to handle
 * implementing the methods exposed by {@link Renderer}. The
 * AbstractGlslRenderer tracks the current state, and when necessary, delegate
 * to protected abstract methods which have the responsibility of actually
 * making OpenGL calls.
 * </p>
 * <p>
 * It makes a best-effort attempt to preserve the texture, vertex attribute, and
 * shader state when resource deadlocks must be resolved. It is possible that a
 * texture must be unbound or will have its data changed based on the actions of
 * another render task. Additionally, the preserving a shader's state when it
 * undergoes concurrent updates is quite complicated, so it is possible that it
 * cannot be preserved if uniforms or attributes are changed or reordered in the
 * shader definition.
 * </p>
 * 
 * @author Michael Ludwig
 */
public abstract class AbstractGlslRenderer extends AbstractRenderer implements GlslRenderer {
    protected class VertexAttributeBinding implements LockListener<VertexBufferObject> {
        // Used to handle relocking/unlocking
        public final int attributeIndex;

        public LockToken<? extends VertexBufferObject> lock;

        public int offset;
        public int stride;
        public int elementSize;

        private VertexAttributeBinding(int index) {
            attributeIndex = index;
        }

        @Override
        public boolean onRelock(LockToken<? extends VertexBufferObject> token) {
            if (token != lock)
                throw new IllegalStateException("Resource locks have been confused");

            if (token.getResourceHandle() == null || token.getResourceHandle().getStatus() != Status.READY) {
                // Resource has been removed, so reset the lock
                lock = null;
                
                // search for attribute binding for this vertex-attribute so we update
                // attribute state to record the unbinding of the vbo
                boolean found = false;
                for (AttributeBinding attr: attributeBindings.values()) {
                    for (int i = 0; i < attr.columnVBOs.length; i++) {
                        if (attr.columnVBOs[i] == VertexAttributeBinding.this) {
                            // found it, so now bind a default value of (0, 0, 0, 0)
                            bindAttribute(attr, i, attr.attr.type.getRowCount(), 0, 0, 0, 0);
                            found = true;
                            break;
                        }
                    }
                    
                    if (found)
                        break;
                }
                
                return false;
            } else {
                // Re-bind the VBO, this is restoring the expected state
                // so we don't need to tamper with the attribute binding record
                bindArrayVbo(lock.getResource(), lock.getResourceHandle(), null);
                glEnableAttribute(attributeIndex, true);
                glAttributePointer(attributeIndex, lock.getResourceHandle(), offset, stride, elementSize);

                return true;
            }
        }

        @Override
        public boolean onForceUnlock(LockToken<? extends VertexBufferObject> token) {
            if (token != lock)
                throw new IllegalStateException("Resource locks have been confused");

            glEnableAttribute(attributeIndex, false); // Disabling is the only way to unbind the attr
            unbindArrayVboMaybe(lock.getResource());

            return true;
        }
    }

    protected class TextureBinding implements LockListener<Texture> {
        public LockToken<? extends Texture> lock;
        public int referenceCount;
        
        public final int texUnit;

        public TextureBinding(int unit) {
            texUnit = unit;
            referenceCount = 0;
        }

        @Override
        public boolean onRelock(LockToken<? extends Texture> token) {
            if (token != lock)
                throw new IllegalStateException("Resource locks have become confused");
            
            if (token.getResourceHandle() == null || token.getResourceHandle().getStatus() != Status.READY) {
                // Texture got screwed up while we were unlocked so don't bind anything, and
                // tell the resource manager to unlock
                lock = null;
                referenceCount = 0;
                
                // search through uniform binding state and clear the record that it
                // was a valid texture binding
                for (UniformBinding u: uniformBindings.values()) {
                    if (u.isTextureBinding && u.intValues.get(0) == texUnit) {
                        // this uniform points to this unit so invalidate its state
                        // but don't make calls that depend on the shader
                        u.isTextureBinding = false;
                        u.valuesValid = false;
                    }
                }

                return false;
            } else {
                // Re-enable and bind the texture, the uniform will still
                // point to this texture slot and the texture could not have
                // changed targets so we're okay on validity
                glBindTexture(texUnit, token.getResource().getTarget(), token.getResourceHandle());
                return true;
            }
        }
        
        @Override
        public boolean onForceUnlock(LockToken<? extends Texture> token) {
            if (token != lock)
                throw new IllegalStateException("Resource locks have been confused");
            
            // Disable and unbind the texture
            glBindTexture(texUnit, token.getResource().getTarget(), null);
            return true;
        }
    }
        
    protected class ShaderBinding implements LockListener<GlslShader> {
        public LockToken<? extends GlslShader> lock;
        
        @Override
        public boolean onRelock(LockToken<? extends GlslShader> token) {
            if (token != lock)
                throw new IllegalStateException("Resource locks have been confused");
            
            if (token.getResourceHandle() == null || token.getResourceHandle().getStatus() != Status.READY) {
                // Resource has been removed, so reset the lock
                lock = null;
                
                // since we're not using the shader anymore, reset other state too
                // we assume that glsl locks go last so we can assume any locked vbos and textures
                // have been relocked or left unlocked and we call the reset method safely
                resetAttributeAndTextureBindings();
                attributeBindings = null;
                uniformBindings = null;
                
                return false;
            } else {
                // Re-bind the program
                GlslShaderHandle handle = (GlslShaderHandle) token.getResourceHandle();
                glUseProgram(handle);
                
                // Update uniform bindings to match new shader details
                generateNewUniformBindings(handle);
                
                // Update attribute bindings to match new shader details
                generateNewAttributeBindings(handle);
                
                return true;
            }
        }

        @Override
        public boolean onForceUnlock(LockToken<? extends GlslShader> token) {
            if (token != lock)
                throw new IllegalStateException("Resource locks have been confused");
            
            glUseProgram(null);
            return true;
        }
        
        // Destructively create new attribute bindings for the GlslRenderer,
        // this replaces the old attributeBindings map.
        private void generateNewAttributeBindings(GlslShaderHandle handle) {
            Map<String, AttributeBinding> newAttrs = new HashMap<String, AttributeBinding>();
            for (Attribute a: handle.attributes.values()) {
                AttributeBinding oldBinding = attributeBindings.get(a.name);
                if (oldBinding != null && a.type == oldBinding.attr.type) {
                    // attribute is compatible
                    if (oldBinding.attr == a) {
                        // no change in the shader variable, so reuse binding
                        newAttrs.put(a.name, oldBinding);
                    } else {
                        // create a new binding
                        // - if the attribute only stored a single value, it can be pushed now
                        // - if it uses VBOs the logic gets a little more complicated
                        // - no change needs to be made if the index lines up
                        if (oldBinding.attr.index == a.index) {
                            // clone oldBinding and that's all we need to do, since
                            // OpenGL should have remembered the vertex attribute state by index
                            newAttrs.put(a.name, new AttributeBinding(a, oldBinding));
                        } else {
                            for (int i = 0; i < oldBinding.columnValuesValid.length; i++) {
                                if (oldBinding.columnValuesValid[i]) {
                                    // push the attribute value to the new va index
                                    int rowCount = oldBinding.attr.type.getRowCount();
                                    glAttributeValue(oldBinding.attr.index + i, rowCount, 
                                                     (rowCount >= 1 ? oldBinding.columnValues.get(i * rowCount + 0) : 0f),
                                                     (rowCount >= 2 ? oldBinding.columnValues.get(i * rowCount + 1) : 0f), 
                                                     (rowCount >= 3 ? oldBinding.columnValues.get(i * rowCount + 2) : 0f), 
                                                     (rowCount >= 4 ? oldBinding.columnValues.get(i * rowCount + 3) : 0f));
                                } else if (oldBinding.columnVBOs[i].lock != null) {
                                    // unlock any vbo for an attribute that was moved to a new index
                                    // we have to unlock it from the old one
                                    // - we will NOT relock the VBOs onto the new index, however, because that
                                    //   would require us to lock a resource while in the relock phase (not okay)
                                    // - this isn't the end of the world, we're already accepting the possibility
                                    //   that the shader can be in an inconsistent state so this is just another 
                                    //   situation to be aware of.
                                    glEnableAttribute(oldBinding.columnVBOs[i].attributeIndex, false);
                                    unbindArrayVboMaybe(oldBinding.columnVBOs[i].lock.getResource());
                                    resourceManager.unlock(oldBinding.columnVBOs[i].lock);
                                    oldBinding.columnVBOs[i].lock = null;
                                }
                            }
                            
                            // create a completely new binding
                            // - note that the column values will not match OpenGL state, but this isn't that bad
                            //   because it just means the next state push will go through no matter what, and
                            //   most likely it will be different state anyway
                            newAttrs.put(a.name, new AttributeBinding(a));
                        }
                    }
                    
                    // remove old binding from attributeBindings so we can iterate
                    // over old attributes that must be cleaned up properly later
                    attributeBindings.remove(a.name);
                } else {
                    // completely new attribute is needed
                    newAttrs.put(a.name, new AttributeBinding(a));
                }
            }
            
            // Reset attribute state for any attribute bindings that are being removed
            for (AttributeBinding ab: attributeBindings.values()) {
                for (int i = 0; i < ab.columnVBOs.length; i++) {
                    if (ab.columnVBOs[i].lock != null) {
                        // attribute that no longer exists or is invalid and has a 
                        // VBO that must be unbound
                        glEnableAttribute(ab.columnVBOs[i].attributeIndex, false);
                        unbindArrayVboMaybe(ab.columnVBOs[i].lock.getResource());
                        resourceManager.unlock(ab.columnVBOs[i].lock);
                        ab.columnVBOs[i].lock = null;
                    }
                }
            }

            attributeBindings = newAttrs;
        }
        
        // Destructively create new uniform bindings for the GlslRenderer,
        // this replaces the old uniformBindings map.
        private void generateNewUniformBindings(GlslShaderHandle handle) {
            Map<String, UniformBinding> newUniforms = new HashMap<String, UniformBinding>();
            for (Uniform u: handle.uniforms.values()) {
                UniformBinding oldBinding = uniformBindings.get(u.name);
                if (oldBinding != null && u.uniform.getType() == oldBinding.uniform.uniform.getType()
                    && u.uniform.getLength() == oldBinding.uniform.uniform.getLength()) {
                    // uniform is compatible
                    if (oldBinding.uniform == u) {
                        // no change in the shader variable, so reuse binding
                        newUniforms.put(u.name, oldBinding);
                    } else {
                        // push value to the shader and create a new binding
                        if (oldBinding.valuesValid) {
                            if (oldBinding.floatValues != null) {
                                oldBinding.floatValues.rewind();
                                glUniform(u, oldBinding.floatValues,
                                          oldBinding.floatValues.capacity() / u.uniform.getType().getPrimitiveCount());
                            } else { // oldBinding.intValues != null
                                oldBinding.intValues.rewind();
                                glUniform(u, oldBinding.intValues,
                                          oldBinding.intValues.capacity() / u.uniform.getType().getPrimitiveCount());
                            }
                        }
                        newUniforms.put(u.name, new UniformBinding(u, oldBinding));
                    }
                    
                    // remove old binding from uniformBindings so we can iterate
                    // over old uniforms that must be cleaned up properly later
                    uniformBindings.remove(u.name);
                } else {
                    // completely new uniform binding is needed
                    newUniforms.put(u.name, new UniformBinding(u));
                }
            }
            
            // Iterate through the UniformBindings left to clean up any referenced textures
            for (UniformBinding ub: uniformBindings.values()) {
                if (ub.isTextureBinding && ub.valuesValid) {
                    int texUnit = ub.intValues.get(0);
                    textureBindings[texUnit].referenceCount--;
                    if (textureBindings[texUnit].referenceCount <= 0) {
                        // unbind and unlock texture
                        glBindTexture(texUnit, textureBindings[texUnit].lock.getResource().getTarget(), null);
                        resourceManager.unlock(textureBindings[texUnit].lock);
                        textureBindings[texUnit].lock = null;
                    }
                }
            }
            
            uniformBindings = newUniforms;
        }
    }
    
    protected class AttributeBinding {
        public final Attribute attr;

        public final VertexAttributeBinding[] columnVBOs;
        public final FloatBuffer columnValues; // column-major if multiple columns exist
        public final boolean[] columnValuesValid;

        public AttributeBinding(Attribute attr, AttributeBinding oldBinding) {
            this.attr = attr;
            columnVBOs = oldBinding.columnVBOs;
            columnValues = oldBinding.columnValues;
            columnValuesValid = oldBinding.columnValuesValid;
        }
        
        public AttributeBinding(Attribute attr) {
            this.attr = attr;
            columnVBOs = new VertexAttributeBinding[attr.type.getColumnCount()];
            for (int i = 0; i < columnVBOs.length; i++)
                columnVBOs[i] = genericAttributeStates[attr.index + i];
            
            columnValues = BufferUtil.newFloatBuffer(attr.type.getColumnCount() * attr.type.getRowCount());
            columnValuesValid = new boolean[attr.type.getColumnCount()];
        }
    }

    protected class UniformBinding {
        public final Uniform uniform;

        public final FloatBuffer floatValues;
        public final IntBuffer intValues;
        
        public boolean valuesValid;
        public boolean isTextureBinding;

        public UniformBinding(Uniform uniform, UniformBinding old) {
            this.uniform = uniform;
            
            floatValues = old.floatValues;
            intValues = old.intValues;
            valuesValid = old.valuesValid;
            isTextureBinding = old.isTextureBinding;
        }
        
        public UniformBinding(Uniform uniform) {
            this.uniform = uniform;
            isTextureBinding = false;
            valuesValid = false;

            int length = uniform.uniform.getLength() * uniform.uniform.getType().getPrimitiveCount();
            switch(uniform.uniform.getType()) {
            case FLOAT: case FLOAT_MAT2: case FLOAT_MAT3: case FLOAT_MAT4: 
            case FLOAT_VEC2: case FLOAT_VEC3: case FLOAT_VEC4: 
                floatValues = BufferUtil.newFloatBuffer(length);
                intValues = null;
                break;
            case INT: case INT_VEC2: case INT_VEC3: case INT_VEC4: case BOOL:
            case SHADOW_MAP: case TEXTURE_1D: case TEXTURE_2D: case TEXTURE_3D: case TEXTURE_CUBEMAP:
                intValues = BufferUtil.newIntBuffer(length);
                floatValues = null;
                break;
            default:
                intValues = null;
                floatValues = null;
                break;
            }
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


    protected final ShaderBinding shaderBinding;

    protected Map<String, AttributeBinding> attributeBindings;
    protected Map<String, UniformBinding> uniformBindings;
    protected TextureBinding[] textureBindings; // "final" after first activate()

    // vertex attribute handling
    protected VertexAttributeBinding[] genericAttributeStates; // "final" after first activate()
    protected VertexBufferObject arrayVboBinding = null;
    protected int activeArrayVbos = 0;

    // cached float arrays to use for matrix uniform equality checks
    private final float[] tempFloatMat3;
    private final float[] tempFloatMat4;

    public AbstractGlslRenderer(RendererDelegate delegate) {
        super(delegate);

        shaderBinding = new ShaderBinding();
        
        tempFloatMat3 = new float[9];
        tempFloatMat4 = new float[16];
    }

    @Override
    public void activate(AbstractSurface surface, OpenGLContext context, ResourceManager manager) {
        super.activate(surface, context, manager);
        
        if (textureBindings == null) {
            RenderCapabilities caps = surface.getFramework().getCapabilities();
            int numTextures = Math.max(caps.getMaxVertexShaderTextures(), caps.getMaxFragmentShaderTextures());
            textureBindings = new TextureBinding[numTextures];
            for (int i = 0; i < numTextures; i++)
                textureBindings[i] = new TextureBinding(i);
        }
        
        if (genericAttributeStates == null) {
            int numAttrs = surface.getFramework().getCapabilities().getMaxVertexAttributes();
            genericAttributeStates = new VertexAttributeBinding[numAttrs];
            for (int i = 0; i < numAttrs; i++)
                genericAttributeStates[i] = new VertexAttributeBinding(i);
        }
    }
    
    protected void resetAttributeAndTextureBindings() {
        // unbind all textures and reset reference counts to 0
        for (int i = 0; i < textureBindings.length; i++) {
            if (textureBindings[i].lock != null) {
                glBindTexture(i, textureBindings[i].lock.getResource().getTarget(), null);
                resourceManager.unlock(textureBindings[i].lock);
            }

            textureBindings[i].lock = null;
            textureBindings[i].referenceCount = 0;
        }
        
        // unbind all vertex attributes
        for (int i = 0; i < genericAttributeStates.length; i++) {
            if (genericAttributeStates[i].lock != null) {
                glEnableAttribute(i, false);
                unbindArrayVboMaybe(genericAttributeStates[i].lock.getResource());
                resourceManager.unlock(genericAttributeStates[i].lock);
            }
            
            genericAttributeStates[i].lock = null;
        }
    }

    @Override
    public void setShader(GlslShader shader) {
        if (shaderBinding.lock == null || shaderBinding.lock.getResource() != shader) {
            // reset the extra state as a safety precaution
            resetAttributeAndTextureBindings();
            
            LockToken<? extends GlslShader> oldLock = null;
            if (shaderBinding.lock != null) {
                oldLock = shaderBinding.lock;
                resourceManager.unlock(shaderBinding.lock);
                shaderBinding.lock = null;
            }
            
            if (shader != null) {
                LockToken<? extends GlslShader> newLock = resourceManager.lock(context, shader, shaderBinding);
                if (newLock != null && (newLock.getResourceHandle() == null 
                                        || newLock.getResourceHandle().getStatus() != Status.READY)) {
                    // shader can't be used
                    resourceManager.unlock(newLock);
                    newLock = null;
                }
                shaderBinding.lock = newLock;
            }
            
            if (shaderBinding.lock != null) {
                // we have a valid shader, so use it and rebuild attribute/uniform bindings
                GlslShaderHandle handle = (GlslShaderHandle) shaderBinding.lock.getResourceHandle();
                glUseProgram(handle);
                
                // fill in the attribute bindings
                attributeBindings = new HashMap<String, AttributeBinding>();
                for (Entry<String, Attribute> a: handle.attributes.entrySet()) {
                    attributeBindings.put(a.getKey(), new AttributeBinding(a.getValue()));
                }

                // fill in the uniform bindings
                uniformBindings = new HashMap<String, UniformBinding>();
                for (Entry<String, Uniform> u: handle.uniforms.entrySet()) {
                    uniformBindings.put(u.getKey(), new UniformBinding(u.getValue()));
                }
            } else {
                // no valid shader
                if (oldLock != null)
                    glUseProgram(null);
                
                attributeBindings = null;
                uniformBindings = null;
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
        if (shaderBinding.lock == null)
            return Collections.emptyMap();

        Attribute attr;
        Map<String, AttributeType> attrs = new HashMap<String, AttributeType>();
        for (Entry<String, AttributeBinding> a: attributeBindings.entrySet()) {
            attr = a.getValue().attr;
            attrs.put(attr.name, attr.type);
        }

        return Collections.unmodifiableMap(attrs);
    }

    @Override
    public Map<String, GlslUniform> getUniforms() {
        if (shaderBinding.lock == null)
            return Collections.emptyMap();

        Uniform uniform;
        Map<String, GlslUniform> uniforms = new HashMap<String, GlslUniform>();
        for (Entry<String, UniformBinding> u: uniformBindings.entrySet()) {
            uniform = u.getValue().uniform;
            uniforms.put(uniform.name, uniform.uniform);
        }

        return Collections.unmodifiableMap(uniforms);
    }

    @Override
    public void bindAttribute(String glslAttrName, VertexAttribute attr) {
        bindAttribute(glslAttrName, 0, attr);
    }

    @Override
    public void bindAttribute(String glslAttrName, int column, VertexAttribute attr) {
        if (attr != null) {
            AttributeBinding a = verifyAttribute(glslAttrName, attr.getElementSize(), column + 1);
            if (a == null)
                return; // ignore call for unsupported attribute types

            VertexAttributeBinding state = a.columnVBOs[column];
            boolean accessDiffers = (state.offset != attr.getOffset() ||
                                     state.stride != attr.getStride() ||
                                     state.elementSize != attr.getElementSize());
            if (state.lock == null || state.lock.getResource() != attr.getData() || accessDiffers) {
                VertexBufferObject oldVbo = (state.lock == null ? null : state.lock.getResource());
                if (state.lock != null && oldVbo != attr.getData()) {
                    // unlock the old vbo
                    resourceManager.unlock(state.lock);
                    state.lock = null;
                }

                if (state.lock == null) {
                    // lock the new vbo
                    LockToken<? extends VertexBufferObject> newLock = resourceManager.lock(context, attr.getData(), state);
                    if (newLock != null && (newLock.getResourceHandle() == null 
                        || newLock.getResourceHandle().getStatus() != Status.READY)) {
                        // VBO isn't ready so unlock it
                        resourceManager.unlock(newLock);
                        newLock = null;
                    } else {
                        // VBO is ready or wasn't locked, either way state.lock should equal newLock
                        state.lock = newLock;
                    }
                }

                if (state.lock != null) {
                    // At this point, state.lock is the lock for the new VBO (or possibly old VBO)
                    state.elementSize = attr.getElementSize();
                    state.offset = attr.getOffset();
                    state.stride = attr.getStride();

                    bindArrayVbo(attr.getData(), state.lock.getResourceHandle(), oldVbo);

                    if (oldVbo == null)
                        glEnableAttribute(state.attributeIndex, true);
                    glAttributePointer(state.attributeIndex, state.lock.getResourceHandle(), state.offset, state.stride, state.elementSize);
                    a.columnValuesValid[column] = false;
                } else if (oldVbo != null) {
                    // Since there was an old vbo we need clean some things up
                    glEnableAttribute(state.attributeIndex, false);
                    unbindArrayVboMaybe(oldVbo);

                    // set a good default attribute value
                    bindAttribute(a, column, a.attr.type.getRowCount(), 0f, 0f, 0f, 0f);
                }
            }
        } else {
            // Since we don't have a row count to use, we fudge the verifyAttribute method here
            if (glslAttrName == null)
                throw new NullPointerException("GLSL attribute name cannot be null");
            
            AttributeBinding a = (attributeBindings != null ? attributeBindings.get(glslAttrName) : null);
            if (a != null) {
                if (a.attr.type.getColumnCount() <= column)
                    throw new IllegalArgumentException("GLSL attribute with a type of " + a.attr.type + " cannot use " + (column + 1) + " columns");
                
                // The attribute is meant to be unbound
                VertexAttributeBinding state = a.columnVBOs[column];
                if (state.lock != null) {
                    // disable the attribute
                    glEnableAttribute(state.attributeIndex, false);
                    // possibly unbind it from the array vbo
                    unbindArrayVboMaybe(state.lock.getResource());

                    // unlock it
                    resourceManager.unlock(state.lock);
                    state.lock = null;

                    // set a good default attribute value
                    bindAttribute(a, column, a.attr.type.getRowCount(), 0f, 0f, 0f, 0f);
                }
            }
        }
    }

    private AttributeBinding verifyAttribute(String glslAttrName, int rowCount, int colCount) {
        if (glslAttrName == null)
            throw new NullPointerException("GLSL attribute name cannot be null");
        if (colCount <= 0)
            throw new IllegalArgumentException("Column must be at least 0");
        
        AttributeBinding a = (attributeBindings != null ? attributeBindings.get(glslAttrName) : null);
        if (a != null) {
            if (a.attr.type == AttributeType.UNSUPPORTED) {
                // no useful binding
                a = null;
            } else {
                if (a.attr.type.getColumnCount() < colCount)
                    throw new IllegalArgumentException("GLSL attribute with a type of " + a.attr.type + " cannot use " + colCount + " columns");
                if (a.attr.type.getRowCount() != rowCount)
                    throw new IllegalArgumentException("GLSL attribute with a type of " + a.attr.type + " cannot use " + rowCount + " rows");
            }
        }
        return a;
    }

    private void bindAttribute(AttributeBinding a, int col, int rowCount, float v1, float v2, float v3, float v4) {
        if (a == null)
            return; // unsupported or unknown binding, ignore at this stage

        if (a.columnVBOs[col].lock != null) {
            // there was a previously bound vertex attribute, so unbind it
            glEnableAttribute(a.attr.index + col, false);
            unbindArrayVboMaybe(a.columnVBOs[col].lock.getResource());
            resourceManager.unlock(a.columnVBOs[col].lock);
            a.columnVBOs[col].lock = null;
        }
        
        int index = col * rowCount;
        boolean pushChanges = !a.columnValuesValid[col];
        switch(rowCount) {
        case 4:
            pushChanges = pushChanges || a.columnValues.get(index + 3) != v4;
            a.columnValues.put(index + 3, v4);
        case 3:
            pushChanges = pushChanges || a.columnValues.get(index + 2) != v3;
            a.columnValues.put(index + 2, v3);
        case 2:
            pushChanges = pushChanges || a.columnValues.get(index + 1) != v2;
            a.columnValues.put(index + 1, v2);
        case 1:
            pushChanges = pushChanges || a.columnValues.get(index) != v1;
            a.columnValues.put(index, v1);
        }
        
        a.columnValuesValid[col] = true;
        if (pushChanges)
            glAttributeValue(a.attr.index + col, rowCount, v1, v2, v3, v4);
    }

    /**
     * Set the generic vertex attribute at attr to the given vector marked by
     * v1, v2, v3, and v4. Depending on rowCount, certain vector values can be
     * ignored (i.e. if rowCount is 3, v4 is meaningless).
     */
    protected abstract void glAttributeValue(int attr, int rowCount, float v1, float v2, float v3, float v4);

    @Override
    public void bindAttribute(String glslAttrName, float val) {
        AttributeBinding ab = verifyAttribute(glslAttrName, 1, 1);
        bindAttribute(ab, 0, 1, val, -1f, -1f, -1f);
    }

    @Override
    public void bindAttribute(String glslAttrName, float v1, float v2) {
        AttributeBinding ab = verifyAttribute(glslAttrName, 2, 1);
        bindAttribute(ab, 0, 2, v1, v2, -1f, -1f);
    }

    @Override
    public void bindAttribute(String glslAttrName, @Const Vector3 v) {
        AttributeBinding ab = verifyAttribute(glslAttrName, 3, 1);
        bindAttribute(ab, 0, 3, (float) v.x, (float) v.y, (float) v.z, -1f);
    }

    @Override
    public void bindAttribute(String glslAttrName, @Const Vector4 v) {
        AttributeBinding ab = verifyAttribute(glslAttrName, 4, 1);
        bindAttribute(ab, 0, 4, (float) v.x, (float) v.y, (float) v.z, (float) v.w);
    }

    @Override
    public void bindAttribute(String glslAttrName, @Const Matrix3 v) {
        AttributeBinding ab = verifyAttribute(glslAttrName, 3, 3);

        bindAttribute(ab, 0, 3, (float) v.get(0, 0), (float) v.get(1, 0), (float) v.get(2, 0), -1f);
        bindAttribute(ab, 1, 3, (float) v.get(0, 1), (float) v.get(1, 1), (float) v.get(2, 1), -1f);
        bindAttribute(ab, 2, 3, (float) v.get(0, 2), (float) v.get(1, 2), (float) v.get(2, 2), -1f);
    }

    @Override
    public void bindAttribute(String glslAttrName, @Const Matrix4 v) {
        AttributeBinding ab = verifyAttribute(glslAttrName, 4, 4);

        bindAttribute(ab, 0, 4, (float) v.get(0, 0), (float) v.get(1, 0), (float) v.get(2, 0), (float) v.get(3, 0));
        bindAttribute(ab, 1, 4, (float) v.get(0, 1), (float) v.get(1, 1), (float) v.get(2, 1), (float) v.get(3, 1));
        bindAttribute(ab, 2, 4, (float) v.get(0, 2), (float) v.get(1, 2), (float) v.get(2, 2), (float) v.get(3, 2));
        bindAttribute(ab, 3, 4, (float) v.get(0, 3), (float) v.get(1, 3), (float) v.get(2, 3), (float) v.get(3, 3));
    }

    private UniformBinding verifyUniform(String name, UniformType[] validTypes) {
        if (name == null)
            throw new NullPointerException("Uniform name cannot be null");
        UniformBinding u = (uniformBindings != null ? uniformBindings.get(name) : null);
        if (u == null)
            return null;

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

        if (!u.valuesValid || u.floatValues.get(0) != val) {
            u.floatValues.put(0, val);
            u.valuesValid = true;
            
            u.floatValues.rewind();
            glUniform(u.uniform, u.floatValues, 1);
        }
    }

    @Override
    public void setUniform(String name, float v1, float v2) {
        UniformBinding u = verifyUniform(name, VALID_FLOAT2);
        if (u == null)
            return; // ignore unsupported uniforms
        if (!u.valuesValid || u.floatValues.get(0) != v1 || u.floatValues.get(1) != v2) {
            u.floatValues.put(0, v1);
            u.floatValues.put(1, v2);
            u.valuesValid = true;
            
            u.floatValues.rewind();
            glUniform(u.uniform, u.floatValues, 1);
        }
    }

    /**
     * Set the given uniform's values. The uniform could have any of the FLOAT_
     * types and could possibly be an array. The buffer will have been rewound
     * already.
     */
    protected abstract void glUniform(Uniform u, FloatBuffer values, int count);

    @Override
    public void setUniform(String name, float v1, float v2, float v3) {
        UniformBinding u = verifyUniform(name, VALID_FLOAT3);
        if (u == null)
            return; // ignore unsupported uniforms

        if (!u.valuesValid || u.floatValues.get(0) != v1 || u.floatValues.get(1) != v2 || u.floatValues.get(2) != v3) {
            u.floatValues.put(0, v1);
            u.floatValues.put(1, v2);
            u.floatValues.put(2, v3);
            u.valuesValid = true;
            
            u.floatValues.rewind();
            glUniform(u.uniform, u.floatValues, 1);
        }
    }

    @Override
    public void setUniform(String name, float v1, float v2, float v3, float v4) {
        UniformBinding u = verifyUniform(name, VALID_FLOAT4);
        if (u == null)
            return; // ignore unsupported uniforms

        if (!u.valuesValid || u.floatValues.get(0) != v1 || u.floatValues.get(1) != v2 || u.floatValues.get(2) != v3 || u.floatValues.get(3) != v4) {
            u.floatValues.put(0, v1);
            u.floatValues.put(1, v2);
            u.floatValues.put(2, v3);
            u.floatValues.put(3, v4);
            u.valuesValid = true;

            u.floatValues.rewind();
            glUniform(u.uniform, u.floatValues, 1);
        }
    }

    @Override
    public void setUniform(String name, @Const Matrix3 val) {
        if (val == null)
            throw new NullPointerException("Matrix cannot be null");
        UniformBinding u = verifyUniform(name, VALID_FLOAT_MAT3);
        if (u == null)
            return; // ignore unsupported uniforms

        val.get(tempFloatMat3, 0, false);
        boolean eq = true;
        if (u.valuesValid) {
            for (int i = 0; i < tempFloatMat3.length; i++) {
                if (u.floatValues.get(i) != tempFloatMat3[i]) {
                    eq = false;
                    break;
                }
            }
        }
        
        if (!u.valuesValid || !eq) {
            u.floatValues.rewind(); // must rewind first since we can't absolute bulk put
            u.floatValues.put(tempFloatMat3);
            u.valuesValid = true;
            
            u.floatValues.rewind();
            glUniform(u.uniform, u.floatValues, 1);
        }
    }

    @Override
    public void setUniform(String name, @Const Matrix4 val) {
        if (val == null)
            throw new NullPointerException("Matrix cannot be null");
        UniformBinding u = verifyUniform(name, VALID_FLOAT_MAT4);
        if (u == null)
            return; // ignore unsupported uniforms

        val.get(tempFloatMat4, 0, false);
        boolean eq = true;
        if (u.valuesValid) {
            for (int i = 0; i < tempFloatMat4.length; i++) {
                if (u.floatValues.get(i) != tempFloatMat4[i]) {
                    eq = false;
                    break;
                }
            }
        }
        
        if (!u.valuesValid || !eq) {
            u.floatValues.rewind(); // must rewind first since we can't absolute bulk put
            u.floatValues.put(tempFloatMat4);
            u.valuesValid = true;
            
            u.floatValues.rewind();
            glUniform(u.uniform, u.floatValues, 1);
        }
    }

    @Override
    public void setUniform(String name, @Const Vector3 v) {
        if (v == null)
            throw new NullPointerException("Vector cannot be null");
        setUniform(name, (float) v.x, (float) v.y, (float) v.z);
    }

    @Override
    public void setUniform(String name, @Const Vector4 v) {
        if (v == null)
            throw new NullPointerException("Vector cannot be null");
        setUniform(name, (float) v.x, (float) v.y, (float) v.z, (float) v.w);
    }
    
    @Override
    public void setUniform(String name, @Const ColorRGB color) {
        setUniform(name, color, false);
    }

    @Override
    public void setUniform(String name, @Const ColorRGB color, boolean isHDR) {
        if (color == null)
            throw new NullPointerException("Color cannot be null");
        
        if (isHDR)
            setUniform(name, (float) color.redHDR(), (float) color.greenHDR(), (float) color.blueHDR());
        else
            setUniform(name, (float) color.red(), (float) color.green(), (float) color.blue());
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
        u.floatValues.rewind(); // must rewind because there is no absolute bulk put
        u.floatValues.put(vals);
        u.valuesValid = true;
        
        u.floatValues.rewind();
        glUniform(u.uniform, u.floatValues, totalElements);
    }

    @Override
    public void setUniform(String name, int val) {
        UniformBinding u = verifyUniform(name, VALID_INT);
        if (u == null)
            return; // ignore unsupported uniforms

        if (!u.valuesValid || u.intValues.get(0) != val) {
            u.intValues.put(0, val);
            u.valuesValid = true;

            u.intValues.rewind();
            glUniform(u.uniform, u.intValues, 1);
        }
    }

    /**
     * Set the given uniform's values. The uniform could have any of the INT_
     * types, the BOOL type or any of the texture sampler types, and could
     * possibly be an array.
     */
    protected abstract void glUniform(Uniform u, IntBuffer values, int count);

    @Override
    public void setUniform(String name, int v1, int v2) {
        UniformBinding u = verifyUniform(name, VALID_INT2);
        if (u == null)
            return; // ignore unsupported uniforms

        if (!u.valuesValid || u.intValues.get(0) != v1 || u.intValues.get(1) != v2) {
            u.intValues.put(0, v1);
            u.intValues.put(1, v2);
            u.valuesValid = true;
            
            u.intValues.rewind();
            glUniform(u.uniform, u.intValues, 1);
        }
    }

    @Override
    public void setUniform(String name, int v1, int v2, int v3) {
        UniformBinding u = verifyUniform(name, VALID_INT3);
        if (u == null)
            return; // ignore unsupported uniforms

        if (!u.valuesValid || u.intValues.get(0) != v1 || u.intValues.get(1) != v2 || u.intValues.get(2) != v3) {
            u.intValues.put(0, v1);
            u.intValues.put(1, v2);
            u.intValues.put(2, v3);
            u.valuesValid = true;

            u.intValues.rewind();
            glUniform(u.uniform, u.intValues, 1);
        }
    }

    @Override
    public void setUniform(String name, int v1, int v2, int v3, int v4) {
        UniformBinding u = verifyUniform(name, VALID_INT4);
        if (u == null)
            return; // ignore unsupported uniforms

        if (!u.valuesValid || u.intValues.get(0) != v1 || u.intValues.get(1) != v2 || u.intValues.get(2) != v3 || u.intValues.get(3) != v4) {
            u.intValues.put(0, v1);
            u.intValues.put(1, v2);
            u.intValues.put(2, v3);
            u.intValues.put(3, v4);
            u.valuesValid = true;
            
            u.intValues.rewind();
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
        u.intValues.rewind(); // must rewind because there is no absolute bulk put
        u.intValues.put(vals);
        u.valuesValid = true;
        
        u.intValues.rewind();
        glUniform(u.uniform, u.intValues, totalElements);
    }

    @Override
    public void setUniform(String name, boolean val) {
        UniformBinding u = verifyUniform(name, VALID_BOOL);
        if (u == null)
            return; // ignore unsupported uniforms

        int translated = (val ? 1 : 0);
        if (!u.valuesValid || u.intValues.get(0) != translated) {
            u.intValues.put(0, translated);
            u.valuesValid = true;
            
            u.intValues.rewind();
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
            u.intValues.put(i, (vals[i] ? 1 : 0));
        u.valuesValid = true;
        
        u.intValues.rewind();
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
            if (u == null)
                return; // ignore unsupported uniforms
            if (u.uniform.uniform.getType() == UniformType.SHADOW_MAP && texture.getFormat() != TextureFormat.DEPTH)
                throw new IllegalArgumentException("Shadow map uniforms must be depth textures, not: " + texture.getFormat());

            int oldUnit = -1;
            Target oldTarget = null;
            if (u.isTextureBinding) {
                // if u.isTextureBinding is true, we can assume that lock is not null
                TextureBinding oldBinding = textureBindings[u.intValues.get(0)];
                if (oldBinding.lock.getResource() == texture)
                    return; // no change is needed
                
                // remove uniform's reference to tex unit, since we'll be
                // changing the uniform's binding
                oldBinding.referenceCount--;
                if (oldBinding.referenceCount == 0) {
                    // remember bind point for later, we might need to unbind
                    // the texture if the new texture doesn't just overwrite
                    oldUnit = oldBinding.texUnit;
                    oldTarget = oldBinding.lock.getResource().getTarget();
                    
                    // unlock old texture
                    resourceManager.unlock(oldBinding.lock);
                    oldBinding.lock = null;
                }
            }
            
            // search for an existing texture to share the binding
            int newUnit = -1;
            int firstEmpty = -1;
            for (int i = 0; i < textureBindings.length; i++) {
                if (textureBindings[i].lock != null && textureBindings[i].lock.getResource() == texture) {
                    newUnit = i;
                    break;
                } else if (textureBindings[i].lock == null && firstEmpty < 0)
                    firstEmpty = i;
            }
            
            boolean needsBind = false;
            if (newUnit < 0) {
                // use the first found empty unit if there is one
                if (firstEmpty >= 0) {
                    // must lock the texture to the unit
                    LockToken<? extends Texture> newLock = resourceManager.lock(context, texture, textureBindings[firstEmpty]);
                    if (newLock != null && (newLock.getResourceHandle() == null
                                            || newLock.getResourceHandle().getStatus() != Status.READY)) {
                        // texture is unusable
                        resourceManager.unlock(newLock);
                        newLock = null;
                    }
                    
                    if (newLock != null) {
                        textureBindings[firstEmpty].lock = newLock;
                        newUnit = firstEmpty;
                        needsBind = true;
                    }
                }
            }
            
            Target newTarget = (newUnit >= 0 && textureBindings[newUnit].lock != null ? textureBindings[newUnit].lock.getResource().getTarget() : null);
            if ((oldTarget != null && oldTarget != newTarget) || (oldUnit >= 0 && oldUnit != newUnit)) {
                // unbind old texture
                glBindTexture(oldUnit, oldTarget, null);
            }
            
            if (newUnit >= 0) {
                // found a bind point
                if (needsBind)
                    glBindTexture(newUnit, newTarget, textureBindings[newUnit].lock.getResourceHandle());
                textureBindings[newUnit].referenceCount++;
                
                u.intValues.put(0, newUnit);
                u.isTextureBinding = true;
                u.valuesValid = true;
                
                u.intValues.rewind();
                glUniform(u.uniform, u.intValues, 1);
            } else {
                // no bind point because no available texture unit could be found
                u.isTextureBinding = false;
                u.valuesValid = false;
            }
        } else {
            // uniform must be unbound from the texture
            UniformBinding u = verifyUniform(name, VALID_TEXTURE_ANY);
            if (u == null)
                return;

            if (u.isTextureBinding) {
                int oldTexUnit = u.intValues.get(0);
                textureBindings[oldTexUnit].referenceCount--;
                
                if (textureBindings[oldTexUnit].referenceCount == 0) {
                    // unlock texture too
                    glBindTexture(oldTexUnit, textureBindings[oldTexUnit].lock.getResource().getTarget(), null);
                    resourceManager.unlock(textureBindings[oldTexUnit].lock);
                    textureBindings[oldTexUnit].lock = null;
                }
                
                u.isTextureBinding = false;
                u.valuesValid = false;
            }
        }
    }

    /**
     * Bind the given texture provided by the ResourceHandle. If the
     * <tt>handle</tt> is null, unbind the texture currently bound to the given
     * target. <tt>tex</tt> represents the texture unit to bind or unbind the
     * texture on, which starts at 0. If the handle is not null, its target will
     * equal the provided target.
     */
    protected abstract void glBindTexture(int tex, Target target, ResourceHandle handle);

    /**
     * Enable the given generic vertex attribute to read in data from an
     * attribute pointer as last assigned by glAttributePointer().
     */
    protected abstract void glEnableAttribute(int attr, boolean enable);

    /**
     * Bind the given resource handle as the array vbo. If null, unbind the
     * array vbo.
     */
    protected abstract void glBindArrayVbo(ResourceHandle handle);

    /**
     * Invoke OpenGL commands to set the given attribute pointer. The resource
     * will have already been bound using glBindArrayVbo. If this is for a
     * texture coordinate, glActiveClientTexture will already have been called.
     */
    protected abstract void glAttributePointer(int attr, ResourceHandle handle, int offset, int stride, int elementSize);

    private void bindArrayVbo(VertexBufferObject vbo, ResourceHandle handle, VertexBufferObject oldVboOnSlot) {
        if (vbo != arrayVboBinding) {
            glBindArrayVbo(handle);
            activeArrayVbos = 0;
            arrayVboBinding = vbo;

            // If we have to bind the vbo, then the last vbo bound to the slot doesn't 
            // matter, since it wasn't counted in the activeArrayVbos counter
            oldVboOnSlot = null;
        }

        // Only update the count if the new vbo isn't replacing itself in the same slot
        if (oldVboOnSlot != vbo)
            activeArrayVbos++;
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

    @Override
    public void reset() {
        super.reset();

        // This unbinds the shader handle, all textures and vertex attributes.
        // It clears the uniform and attribute cached values, but does not
        // assign default values to them.
        setShader(null);
    }
}
