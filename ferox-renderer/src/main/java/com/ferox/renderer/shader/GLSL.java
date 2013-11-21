package com.ferox.renderer.shader;

import com.ferox.math.*;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public final class GLSL {
    private final int glslVersion;

    private final Map<String, Mirror> fragmentOutputs;
    private Mirror.Vec4 vertexPosition;

    private int constantNameCounter;
    private int nodeNameCounter;

    private GLSL(int version) {
        glslVersion = version;
        constantNameCounter = 0;
        nodeNameCounter = 0;

        fragmentOutputs = new HashMap<>();
        vertexPosition = null;
    }

    public Mirror.Mat3 wrap(@Const Matrix3 value) {
        return wrap("constMat3_" + (constantNameCounter++), value);
    }

    public Mirror.Mat3 wrap(String name, @Const Matrix3 value) {
        return MirrorImpl.createConstant(Mirror.Mat3.class, name, value.clone());
    }

    public Mirror.Mat4 wrap(@Const Matrix4 m) {
        return wrap("constMat4_" + (constantNameCounter++), m);
    }

    public Mirror.Mat4 wrap(String name, @Const Matrix4 value) {
        return MirrorImpl.createConstant(Mirror.Mat4.class, name, value.clone());
    }

    public Mirror.Vec3 wrap(@Const Vector3 value) {
        return wrap("constVec3_" + (constantNameCounter++), value);
    }

    public Mirror.Vec3 wrap(String name, @Const Vector3 value) {
        return MirrorImpl.createConstant(Mirror.Vec3.class, name, value.clone());
    }

    public Mirror.Vec4 wrap(@Const Vector4 value) {
        return wrap("constVec4_" + (constantNameCounter++), value);
    }

    public Mirror.Vec4 wrap(String name, @Const Vector4 value) {
        return MirrorImpl.createConstant(Mirror.Vec4.class, name, value.clone());
    }

    public Mirror.Bool wrap(boolean value) {
        return wrap("constBool_" + (constantNameCounter++), value);
    }

    public Mirror.Bool wrap(String name, boolean value) {
        return MirrorImpl.createConstant(Mirror.Bool.class, name, value);
    }

    public Mirror.Float wrap(float value) {
        return wrap("constFloat_" + (constantNameCounter++), value);
    }

    public Mirror.Float wrap(String name, float value) {
        return MirrorImpl.createConstant(Mirror.Float.class, name, value);
    }

    public Mirror.Int wrap(int value) {
        return wrap("constInt_" + (constantNameCounter++), value);
    }

    public Mirror.Int wrap(String name, int value) {
        return MirrorImpl.createConstant(Mirror.Int.class, name, value);
    }

    public Mirror.UInt wrap(long value) {
        return wrap("constUInt_" + (constantNameCounter++), value);
    }

    public Mirror.UInt wrap(String name, long value) {
        return MirrorImpl.createConstant(Mirror.UInt.class, name, value);
    }

    // FIXME add other primitive vecs and matrices, and vec2 and mat2

    public <T extends Mirror> T uniform(String name, Class<T> cls) {
        return MirrorImpl.createUniform(cls, name);
    }

    public <T extends Mirror> T attribute(String name, Class<T> cls) {
        return MirrorImpl.createVertexInput(cls, name);
    }

    public <T extends Node> T node(Class<T> cls) {
        return node("node_" + (nodeNameCounter++), cls);
    }

    public <T extends Node> T node(String name, Class<T> cls) {
        return NodeImpl.createNode(cls, name);
    }

    public void setPosition(Mirror.Vec4 position) {
        vertexPosition = position;
    }

    public void setFragmentColor(Mirror.Vec4 color) {
        if (glslVersion < 330) {
            setFragmentOutput("gl_FragColor", color);
        } else {
            setFragmentOutput("color", color);
        }
    }

    public void setFragmentOutput(String name, Mirror output) {
        fragmentOutputs.put(name, output);
    }

    public ShaderProgram generate() {
        return ShaderAccumulator.generate(glslVersion, vertexPosition, fragmentOutputs);
    }

    public static GLSL newShader(int version) {
        return new GLSL(version);
    }
}
