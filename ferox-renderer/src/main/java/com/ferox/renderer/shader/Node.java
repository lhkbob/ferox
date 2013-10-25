package com.ferox.renderer.shader;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;

/**
 *
 */
public interface Node {
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Function {
        // FIXME add variations here, or support with additional annotations for different glsl versions
        // or at least the required version
        String[] glsl();
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Input {
        String name();
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Output {
        String name();
    }

    public Map<String, Class<? extends Mirror>> getInputs();

    public Map<String, Class<? extends Mirror>> getOutputs();

    public Node getInputSource(String input);

    public Mirror getInput(String input);

    public void setInput(String input, Mirror value);

    public Mirror getOutput(String output);

    public void forceToVertexShader();
}
