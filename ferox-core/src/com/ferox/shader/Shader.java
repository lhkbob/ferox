package com.ferox.shader;

/**
 * <p>
 * Shader represents a very abstract concept. When rendering a Geometry, there
 * are many possible ways that the Geometry can be converted into colored
 * pixels. Some Shader implementations are highly configurable, based on a fixed
 * collection of states and parameters (e.g. FixedFunctionShader) or are
 * completely customizable and programmable (e.g. GlslShader).
 * </p>
 * <p>
 * Theoretically a Shader may be even more abstract than just generating pixels
 * for a Geometry. With more advanced hardware, there are render-to-vertex and
 * transform feedback capabilities that could be represented as a Shader.
 * </p>
 * <p>
 * Currently there are two concrete implementations of Shader:
 * FixedFunctionShader and GlslShader. Both of these extend OpenGlShader. The
 * functionality of an OpenGl-like system is fairly representative of the
 * capabilities allowed with any graphics hardware. It is also an API that's
 * cross platform, making it suitable for a Java implementation. Even so, it's
 * possible that a Shader could be designed around DirectX or even ray-tracing
 * (of course, the Framework implementation would have to support it, too).
 * </p>
 * 
 * @author Michael Ludwig
 */
public interface Shader {
}
