package com.ferox.renderer.impl2.drivers;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import com.ferox.resource.GlslShader.AttributeType;
import com.ferox.resource.GlslShader.ShaderType;
import com.ferox.resource.GlslUniform;

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
    
    public GlslShaderHandle(int id) {
        super(id);
        
        attributes = new HashMap<String, Attribute>();
        uniforms = new HashMap<String, Uniform>();
        shaders = new EnumMap<ShaderType, Integer>(ShaderType.class);
    }
}
