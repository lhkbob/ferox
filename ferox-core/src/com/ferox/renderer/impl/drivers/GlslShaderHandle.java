package com.ferox.renderer.impl.drivers;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import com.ferox.renderer.impl.ResourceHandle;
import com.ferox.resource.GlslShader;
import com.ferox.resource.GlslShader.AttributeType;
import com.ferox.resource.GlslShader.ShaderType;
import com.ferox.resource.GlslUniform;

/**
 * GlslShaderHandle is a concrete subclass of ResourceHandle that represents the
 * persisted state of a GlslShader, and is used by any
 * {@link AbstractGlslShaderResourceDriver} when they manage GlslShaders.
 * 
 * @author Michael Ludwig
 */
public class GlslShaderHandle extends ResourceHandle {
    public static class Uniform {
        public final String name;
        public final int index;
        
        public final GlslUniform uniform;
        
        public Uniform(GlslUniform uniform, int index) {
            this.uniform = uniform;
            this.index = index;
            this.name = uniform.getName();
        }
    }
    
    public static class Attribute {
        public final String name;
        public final int index;
        public final AttributeType type;
        
        public Attribute(String name, AttributeType type, int index) {
            this.name = name;
            this.index = index;
            this.type = type;
        }
    }
    
    public final Map<String, Attribute> attributes;
    public final Map<String, Uniform> uniforms;
    public final EnumMap<ShaderType, Integer> shaders;
    public final EnumMap<ShaderType, String> shaderSource;
    
    public final int programID;
    
    public GlslShaderHandle(GlslShader shader, int programID) {
        super(shader);
        
        this.programID = programID;
        
        attributes = new HashMap<String, Attribute>();
        uniforms = new HashMap<String, Uniform>();
        shaders = new EnumMap<ShaderType, Integer>(ShaderType.class);
        shaderSource = new EnumMap<ShaderType, String>(ShaderType.class);
    }
}