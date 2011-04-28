package com.ferox.renderer.impl.drivers;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import com.ferox.renderer.RenderCapabilities;
import com.ferox.resource.DirtyState;
import com.ferox.resource.GlslShader;
import com.ferox.resource.GlslShader.ShaderType;
import com.ferox.resource.Resource;
import com.ferox.resource.Resource.Status;

public abstract class AbstractGlslShaderResourceDriver implements ResourceDriver {
    protected final boolean glslSupported;
    protected final EnumSet<ShaderType> supportedShaders;
    
    public AbstractGlslShaderResourceDriver(RenderCapabilities caps) {
        glslSupported = caps.hasGlslRenderer();
        supportedShaders = caps.getSupportedShaderTypes();
    }
    
    @Override
    public ResourceHandle init(Resource res) {
        GlslShader shader = (GlslShader) res;
        
        if (!glslSupported) {
            // ideally, frameworks just won't register glsl drivers when
            // they're not supported, but this is here just in case
            
            GlslShaderHandle h = new GlslShaderHandle(-1);
            h.setStatus(Status.UNSUPPORTED);
            h.setStatusMessage("GLSL is not supported on current hardware");
            
            return h;
        }
        
        int id = glCreateProgram();
        GlslShaderHandle h = new GlslShaderHandle(id);
        update(shader, h);
        return h;
    }

    @Override
    public Status update(Resource res, ResourceHandle handle, DirtyState<?> dirtyState) {
        // id is only ever negative if we don't have glsl support
        if (handle.getId() < 0)
            return Status.ERROR;
        
        update((GlslShader) res, (GlslShaderHandle) handle);
        return handle.getStatus();
    }

    @Override
    public void dispose(ResourceHandle handle) {
        if (handle.getId() > 0) {
            GlslShaderHandle h = (GlslShaderHandle) handle;
            
            // detach and delete all shaders first
            for (Integer shader: h.shaders.values()) {
                glDetachShader(shader.intValue(), h.getId());
                glDeleteShader(shader.intValue());
            }
            
            // delete program
            glDeleteProgram(h.getId());
        }
    }
    
    private void update(GlslShader shader, GlslShaderHandle handle) {
        // clear the handle's uniform and attribute maps
        handle.uniforms.clear();
        handle.attributes.clear();
        
        
        // compile shaders and remove any unneeded shaders
        List<String> problems = null;
        ShaderType[] values = ShaderType.values();
        for (int i = 0; i < values.length; i++) {
            ShaderType type = values[i];
            Integer oldShaderId = handle.shaders.get(type);
            String shaderSource = shader.getShader(type);
            
            if (shaderSource == null) {
                // detach any old shader of this type
                if (oldShaderId != null) {
                    glDetachShader(handle.getId(), oldShaderId);
                    glDeleteShader(oldShaderId);
                    handle.shaders.remove(type);
                }
            } else {
                // compile and attach the shader
                int shaderId;
                if (oldShaderId == null) {
                    shaderId = glCreateShader(type);
                    glAttachShader(handle.getId(), shaderId);
                    handle.shaders.put(type, shaderId);
                } else
                    shaderId = oldShaderId.intValue();
                
                String errorLog = glCompileShader(shaderId, shaderSource);
                if (errorLog != null) {
                    if (problems == null)
                        problems = new ArrayList<String>();
                    problems.add("Error compiling " + type + " shader: " + errorLog);
                }
            }
        }
        
        if (problems != null) {
            // shader compiling failed
            handle.setStatus(Status.ERROR);
            handle.setStatusMessage(problems.toString());
            return;
        }
        
        // link the program
        String errorLog = glCompileProgram(handle.getId());
        if (errorLog != null) {
            // program linking failed
            handle.setStatus(Status.ERROR);
            handle.setStatusMessage(errorLog);
            return;
        }
        
        updateUniforms(handle);
        updateAttributes(handle);
        
        handle.setStatus(Status.READY);
        handle.setStatusMessage("");
    }
     
    protected abstract int glCreateProgram();
    
    protected abstract int glCreateShader(ShaderType type);
    
    protected abstract String glCompileShader(int shaderId, String code);
    
    protected abstract void glDeleteShader(int id);
    
    protected abstract void glDeleteProgram(int id);
    
    protected abstract void glAttachShader(int programId, int shaderId);
    
    protected abstract void glDetachShader(int programId, int shaderId);
    
    protected abstract String glCompileProgram(int programId);
    
    protected abstract void updateAttributes(GlslShaderHandle handle);
    
    protected abstract void updateUniforms(GlslShaderHandle handle);
}
