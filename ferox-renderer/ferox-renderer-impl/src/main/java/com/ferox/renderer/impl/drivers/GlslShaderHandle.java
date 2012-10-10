/*
 * Ferox, a graphics and game library in Java
 *
 * Copyright (c) 2012, Michael Ludwig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright notice,
 *         this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright notice,
 *         this list of conditions and the following disclaimer in the
 *         documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.ferox.renderer.impl.drivers;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import com.ferox.resource.GlslShader.AttributeType;
import com.ferox.resource.GlslShader.ShaderType;
import com.ferox.resource.GlslUniform;

/**
 * GlslShaderHandle is the handle type that represents the persisted state of a
 * GlslShader, and is used by any {@link AbstractGlslShaderResourceDriver}.
 * 
 * @author Michael Ludwig
 */
public class GlslShaderHandle {
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

    public int programID;

    public GlslShaderHandle() {
        attributes = new HashMap<String, Attribute>();
        uniforms = new HashMap<String, Uniform>();
        shaders = new EnumMap<ShaderType, Integer>(ShaderType.class);
        shaderSource = new EnumMap<ShaderType, String>(ShaderType.class);

        programID = 0;
    }
}
