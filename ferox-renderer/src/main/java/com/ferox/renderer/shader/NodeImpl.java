package com.ferox.renderer.shader;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
class NodeImpl<T extends Node> implements InvocationHandler {
    final String name;
    final Class<T> type;

    final Map<String, Class<? extends Mirror>> validInputs;
    final Map<String, Class<? extends Mirror>> validOutputs;

    final Map<String, Mirror> assignedInputs;
    final Map<String, Mirror> outputs;

    boolean forceToVertexShader;

    @SuppressWarnings("unchecked")
    private NodeImpl(Class<T> type, String name) {
        // FIXME perform additional validation, such as making sure all referenced Mirror types are valid,
        // that the Node type is an interface itself, and that it has a @Function annotation
        this.type = type;
        this.name = name;
        // FIXME the valid inputs and outputs need to be made available via static method, when we only have a class

        Map<String, Class<? extends Mirror>> in = new HashMap<>();
        Map<String, Class<? extends Mirror>> out = new HashMap<>();
        for (Method m : type.getMethods()) {
            if (m.getDeclaringClass().equals(Object.class) || m.getDeclaringClass().equals(Node.class)) {
                continue;
            }
            Node.Input input = m.getAnnotation(Node.Input.class);
            Node.Output output = m.getAnnotation(Node.Output.class);

            if (input != null) {
                // should be a void method with a single Mirror parameter, and not marked as an @Output
                if (output != null) {
                    throw new UnsupportedOperationException(
                            "@Input method cannot be annotated with @Output: " + m);
                }
                if (m.getParameterTypes().length != 1 ||
                    !Mirror.class.isAssignableFrom(m.getParameterTypes()[0])) {
                    throw new UnsupportedOperationException(
                            "@Input method must have a single Mirror parameter: " + m);
                }
                if (!m.getReturnType().equals(void.class)) {
                    throw new UnsupportedOperationException("@Input methods must return void: " + m);
                }

                // check for duplicate name
                if (in.containsKey(input.name())) {
                    throw new UnsupportedOperationException("Duplicate input name on method: " + m);
                }

                // record the input
                in.put(input.name(), (Class<? extends Mirror>) m.getParameterTypes()[0]);
            } else if (output != null) {
                // should be a no-arg method that returns a Mirror, and not marked as @Input
                if (m.getParameterTypes().length > 0) {
                    throw new UnsupportedOperationException("@Output methods must take zero arguments: " + m);
                }
                if (!Mirror.class.isAssignableFrom(m.getReturnType())) {
                    throw new UnsupportedOperationException("@Output methods must return a Mirror: " + m);
                }

                // check for duplicate name
                if (out.containsKey(output.name())) {
                    throw new UnsupportedOperationException("Duplicate output name on method: " + m);
                }

                // record the output
                out.put(output.name(), (Class<? extends Mirror>) m.getReturnType());
            } else {
                throw new UnsupportedOperationException("Unable to dynamically implement method: " + m);
            }
        }

        validInputs = Collections.unmodifiableMap(in);
        validOutputs = Collections.unmodifiableMap(out);

        assignedInputs = new HashMap<>();

        // the actual output mirror instances will be completed once we have the wrapped Node proxy
        outputs = new HashMap<>();

        forceToVertexShader = false;
    }

    public Map<String, Class<? extends Mirror>> getInputs() {
        return validInputs;
    }

    public Map<String, Class<? extends Mirror>> getOutputs() {
        return validOutputs;
    }

    // FIXME is this still a useful method? There's no way of distinguishing uniforms vs constants vs attributes
    public Node getInputSource(String input) {
        if (!validInputs.containsKey(input)) {
            throw new IllegalArgumentException("Input is not defined for this node type: " + input);
        }
        Mirror assigned = assignedInputs.get(input);
        if (assigned != null) {
            MirrorImpl<?> impl = MirrorImpl.getImpl(assigned);
            if (impl instanceof MirrorImpl.NodeOutputMirror) {
                return ((MirrorImpl.NodeOutputMirror<?>) impl).source;
            }
        }

        return null;
    }

    public Mirror getInput(String input) {
        if (!validInputs.containsKey(input)) {
            throw new IllegalArgumentException("Input is not defined for this node type: " + input);
        }
        return assignedInputs.get(input);
    }

    public void setInput(String input, Mirror value) {
        if (!validInputs.containsKey(input)) {
            throw new IllegalArgumentException("Input is not defined for this node type: " + input);
        }
        if (!validInputs.get(input).isInstance(value)) {
            throw new IllegalArgumentException(
                    "Input requires a type of " + validInputs.get(input) + ", not a " + value);
        }
        assignedInputs.put(input, value);
    }

    public Mirror getOutput(String output) {
        if (!validOutputs.containsKey(output)) {
            throw new IllegalArgumentException("Output is not defined for this node type: " + output);
        }
        return outputs.get(output);
    }

    public void forceToVertexShader() {
        forceToVertexShader = true;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        switch (method.getName()) {
        case "forceToVertexShader":
            forceToVertexShader();
            return null;
        case "getOutput":
            return getOutput((String) args[0]);
        case "setInput":
            setInput((String) args[0], (Mirror) args[1]);
            return null;
        case "getInput":
            return getInput((String) args[0]);
        case "getInputSource":
            return getInputSource((String) args[0]);
        case "getInputs":
            return getInputs();
        case "getOutputs":
            return getOutputs();
        case "toString":
            return toString();
        case "hashCode":
            return hashCode();
        case "equals":
            if (!(args[0] instanceof Node) || !Proxy.isProxyClass(args[0].getClass())) {
                return false;
            }
            NodeImpl<?> arg = getImpl((Node) args[0]);
            return equals(arg);
        default:
            // handle getters or setters
            Node.Input in = method.getAnnotation(Node.Input.class);
            if (in != null) {
                setInput(in.name(), (Mirror) args[0]);
                return null;
            }
            Node.Output out = method.getAnnotation(Node.Output.class);
            if (out != null) {
                return getOutput(out.name());
            }

            throw new UnsupportedOperationException("Unexpected method call: " + method);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(type.getSimpleName()).append("(inputs: [");
        boolean first = true;
        for (String input : validInputs.keySet()) {
            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }

            sb.append(input).append("=");
            Mirror value = assignedInputs.get(input);
            if (value != null) {
                sb.append(value);
            } else {
                sb.append("unassigned");
            }
        }

        sb.append("], outputs: [");
        first = true;
        for (String output : outputs.keySet()) {
            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }

            sb.append(output).append("=").append(outputs.get(output));
        }
        sb.append("])");
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof NodeImpl)) {
            return false;
        }
        // use instance equality
        return o == this;
    }

    @Override
    public int hashCode() {
        // use instance equality
        return System.identityHashCode(this);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Node> T createNode(Class<T> type, String name) {
        NodeImpl<T> impl = new NodeImpl<>(type, name);
        T node = (T) Proxy.newProxyInstance(NodeImpl.class.getClassLoader(), new Class<?>[] { type }, impl);
        // complete output mirror instances
        for (Map.Entry<String, Class<? extends Mirror>> o : impl.validOutputs.entrySet()) {
            impl.outputs.put(o.getKey(), MirrorImpl.createNodeOutput(o.getValue(), o.getKey(), node));
        }

        return node;
    }

    @SuppressWarnings("unchecked")
    public static <T extends Node> NodeImpl<T> getImpl(T node) {
        return (NodeImpl<T>) Proxy.getInvocationHandler(node);
    }
}
