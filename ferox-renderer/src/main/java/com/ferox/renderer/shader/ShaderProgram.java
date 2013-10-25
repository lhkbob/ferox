package com.ferox.renderer.shader;

import com.ferox.renderer.Framework;
import com.ferox.renderer.Shader;
import com.ferox.renderer.builder.ShaderBuilder;

import java.util.Collections;
import java.util.Set;

/**
 *
 */
public class ShaderProgram {
    private final String vertexShader;
    private final String fragmentShader;

    private final Set<String> uniforms;
    private final Set<String> attributes;
    private final Set<String> outputs;

    ShaderProgram(String vertexShader, String fragmentShader, Set<String> uniforms, Set<String> attributes,
                  Set<String> outputs) {
        this.vertexShader = vertexShader;
        this.fragmentShader = fragmentShader;

        this.uniforms = Collections.unmodifiableSet(uniforms);
        this.attributes = Collections.unmodifiableSet(attributes);
        this.outputs = Collections.unmodifiableSet(outputs);
    }

    public Shader build(Framework framework) {
        ShaderBuilder sb = framework.newShader();
        configure(sb);
        return sb.build();
    }

    public void configure(ShaderBuilder builder) {
        builder.withVertexShader(vertexShader).withGeometryShader(null).withFragmentShader(fragmentShader)
               .requestBinding(outputs.toArray(new String[outputs.size()]));
    }

    public String getVertexCode() {
        return vertexShader;
    }

    public String getFragmentCode() {
        return fragmentShader;
    }

    public Set<String> getUniformNames() {
        return uniforms;
    }

    public Set<String> getAttributeNames() {
        return attributes;
    }

    public Set<String> getFragmentOutputNames() {
        return outputs;
    }
}
