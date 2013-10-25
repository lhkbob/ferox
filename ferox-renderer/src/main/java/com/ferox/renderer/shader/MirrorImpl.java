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

    public MirrorImpl(Class<T> type, String name) {
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
        return create(type, new VertexInputMirror<>(type, name));
    }

    @SuppressWarnings("unchecked")
    public static <T extends Mirror> MirrorImpl<T> getImpl(T m) {
        return (MirrorImpl<T>) Proxy.getInvocationHandler(m);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Mirror> T create(Class<T> type, MirrorImpl handler) {
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
        if (t == null) {
            throw new IllegalStateException(
                    "Mirror type is not annotated with @TypeName, cannot proceed: " + type);
        }
        return t.value();
    }

    public static class NodeOutputMirror<T extends Mirror> extends MirrorImpl<T> {
        final Node source;

        public NodeOutputMirror(Class<T> type, String name, Node source) {
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
            return "node output (" + getTypeName() + " " + name + ")";
        }
    }

    public static class UniformMirror<T extends Mirror> extends MirrorImpl<T> {
        public UniformMirror(Class<T> type, String name) {
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

        public ConstantMirror(Class<T> type, String name, Object value) {
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

    public static class VertexInputMirror<T extends Mirror> extends MirrorImpl<T> {

        public VertexInputMirror(Class<T> type, String name) {
            super(type, name);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof VertexInputMirror)) {
                return false;
            }
            VertexInputMirror<?> m = (VertexInputMirror<?>) o;
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
            return "vertex attribute (" + getTypeName() + " " + name + ")";
        }
    }
}
