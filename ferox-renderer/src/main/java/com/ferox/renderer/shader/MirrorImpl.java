package com.ferox.renderer.shader;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 *
 */
class MirrorImpl<T extends Mirror> implements InvocationHandler {
    final String name;
    final Class<T> type;

    private MirrorImpl(Class<T> type, String name) {
        this.name = name;
        this.type = type;
    }

    public static <T extends Mirror> T createNodeOutput(Class<T> type, String outputName, Node source) {
        return create(type, new NodeOutputMirror<>(type, outputName, source));
    }

    public static <T extends Mirror> T createUniform(Class<T> type, String name) {
        return create(type, new UniformMirror<>(type, name));
    }

    public static <T extends Mirror> T createConstant(Class<T> type, String name, Object value) {
        return create(type, new ConstantMirror<>(type, name, value));
    }

    public static <T extends Mirror> T createVertexInput(Class<T> type, String name) {
        return create(type, new ShaderInputMirror<>(type, null, name));
    }

    public static <T extends Mirror> T createFragmentInput(T vertex) {
        MirrorImpl<T> impl = getImpl(vertex);
        return create(impl.type, new ShaderInputMirror<>(impl.type, vertex, impl.name));
    }

    public static <T extends Mirror> T createShaderOutput(T output) {
        MirrorImpl<T> impl = getImpl(output);
        return create(impl.type, new ShaderOutputMirror<>(impl.type, output, impl.name));
    }

    public static <T extends Mirror> T createShaderOutput(T output, String name) {
        MirrorImpl<T> impl = getImpl(output);
        return create(impl.type, new ShaderOutputMirror<>(impl.type, output, name));
    }

    @SuppressWarnings("unchecked")
    public static <T extends Mirror> MirrorImpl<T> getImpl(T m) {
        return (MirrorImpl<T>) Proxy.getInvocationHandler(m);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Mirror> T create(Class<T> type, MirrorImpl handler) {
        // FIXME validate mirror definition: must have a @TypeName annotation and one of @BuiltIn or @Struct
        return (T) Proxy
                .newProxyInstance(MirrorImpl.class.getClassLoader(), new Class<?>[] { type }, handler);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // Mirror doesn't define any methods so we just need to forward Object's methods to the handler
        switch (method.getName()) {
        case "toString":
            return toString();
        case "equals":
            if (!(args[0] instanceof Mirror) || !Proxy.isProxyClass(args[0].getClass())) {
                return false;
            }
            MirrorImpl<?> arg = getImpl((Mirror) args[0]);
            return equals(arg);
        case "hashCode":
            return hashCode();
        default:
            // Proxy only dispatches equals, toString, and hashCode, and Mirror doesn't define anymore
            // so this can only happen if someone maliciously or unintentionally adds a method to a custom
            // mirror type
            throw new UnsupportedOperationException("Unexpected and unsupported method call: " + method);
        }
    }

    public String getTypeName() {
        Mirror.TypeName t = type.getAnnotation(Mirror.TypeName.class);
        return t.value();
    }

    public Class<T> getMirrorType() {
        return type;
    }

    public static class NodeOutputMirror<T extends Mirror> extends MirrorImpl<T> {
        final Node source;

        private NodeOutputMirror(Class<T> type, String name, Node source) {
            super(type, name);
            this.source = source;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof NodeOutputMirror)) {
                return false;
            }
            NodeOutputMirror<?> m = (NodeOutputMirror<?>) o;
            return m.type.equals(type) && m.source.equals(source) && m.name.equals(name);
        }

        @Override
        public int hashCode() {
            int h = 17;
            h += 31 * h + type.hashCode();
            h += 31 * h + source.hashCode();
            h += 31 * h + name.hashCode();
            return h;
        }

        @Override
        public String toString() {
            return "node value (" + getTypeName() + " " + name + ")";
        }
    }

    public static class UniformMirror<T extends Mirror> extends MirrorImpl<T> {
        private UniformMirror(Class<T> type, String name) {
            super(type, name);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof UniformMirror)) {
                return false;
            }
            UniformMirror<?> m = (UniformMirror<?>) o;
            return m.type.equals(type) && m.name.equals(name);
        }

        @Override
        public int hashCode() {
            int h = 17;
            h += 31 * h + type.hashCode();
            h += 31 * h + name.hashCode();
            return h;
        }

        @Override
        public String toString() {
            return "uniform (" + getTypeName() + " " + name + ")";
        }
    }

    public static class ConstantMirror<T extends Mirror> extends MirrorImpl<T> {
        final Object value;

        private ConstantMirror(Class<T> type, String name, Object value) {
            super(type, name);
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ConstantMirror)) {
                return false;
            }
            ConstantMirror<?> m = (ConstantMirror<?>) o;
            return m.type.equals(type) && m.value.equals(value) && m.name.equals(name);
        }

        @Override
        public int hashCode() {
            int h = 17;
            h += 31 * h + type.hashCode();
            h += 31 * h + value.hashCode();
            h += 31 * h + name.hashCode();
            return h;
        }

        @Override
        public String toString() {
            return "constant (" + name + " " + value.toString() + ")";
        }
    }

    public static class ShaderInputMirror<T extends Mirror> extends MirrorImpl<T> {
        // if null, this represents a vertex input, otherwise it's the output mirror from the prior stage
        // for the corresponding linked output attribute
        final T source;

        private ShaderInputMirror(Class<T> type, T source, String name) {
            super(type, name);
            this.source = source;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ShaderInputMirror)) {
                return false;
            }
            ShaderInputMirror<?> m = (ShaderInputMirror<?>) o;
            return m.type.equals(type) && m.name.equals(name) &&
                   (source == null ? m.source == null : source.equals(m.source));
        }

        @Override
        public int hashCode() {
            int h = 17;
            h += 31 * h + type.hashCode();
            if (source != null) {
                h += 31 * h + source.hashCode();
            }
            h += 31 * h + name.hashCode();
            return h;
        }

        @Override
        public String toString() {
            if (source == null) {
                return "shader input (" + getTypeName() + " " + name + ")";
            } else {
                return "shader input (" + source + ")";
            }
        }
    }

    public static class ShaderOutputMirror<T extends Mirror> extends MirrorImpl<T> {
        final T value;

        private ShaderOutputMirror(Class<T> type, T value, String name) {
            super(type, name);
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ShaderOutputMirror)) {
                return false;
            }
            ShaderOutputMirror<?> m = (ShaderOutputMirror<?>) o;
            return m.value.equals(value) && m.name.equals(name);
        }

        @Override
        public int hashCode() {
            int h = 17;
            h += 31 * h + value.hashCode();
            h += 31 * h + name.hashCode();
            return h;
        }

        @Override
        public String toString() {
            return "shader output (" + value + ")";
        }
    }
}
