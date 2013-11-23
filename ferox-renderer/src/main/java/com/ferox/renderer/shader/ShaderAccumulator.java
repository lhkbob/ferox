package com.ferox.renderer.shader;

import java.util.*;

/**
 *
 */
class ShaderAccumulator {

    static ShaderProgram generate(int version, Mirror.Vec4 position, Map<String, Mirror> fragmentOutputs) {
        Stage fragmentStage = new Stage();
        Stage vertexStage = new Stage();
        LinkedHashSet<Mirror> pushedToVertex = new LinkedHashSet<>();

        Set<Mirror> visited = new HashSet<>();
        for (Map.Entry<String, Mirror> output : fragmentOutputs.entrySet()) {
            Mirror wrappedOutput = MirrorImpl.createShaderOutput(output.getValue(), output.getKey());
            sortForFragmentShader(wrappedOutput, fragmentStage, visited, pushedToVertex);
            fragmentStage.add(wrappedOutput, output.getKey()); // force variable name to be value.getKey()
        }

        // these mirrors are already wrapped in the shader value mirror type
        visited.clear();
        for (Mirror vertOut : pushedToVertex) {
            sortForVertexShader(vertOut, vertexStage, visited);
        }
        Mirror wrappedPosition = MirrorImpl.createShaderOutput(position, "gl_Position");
        sortForVertexShader(wrappedPosition, vertexStage, visited);
        vertexStage.add(wrappedPosition, "gl_Position"); // force variable to be gl_Position

        String vertexGLSL = toGLSL(version, vertexStage);
        String fragmentGLSL = toGLSL(version, fragmentStage);

        // FIXME we may want this to be a map from Mirror to String because of the name mangling performed
        // in the generated source, so we can have the programmer hold onto the mirror and then use that to
        // query for the actual attr or uniform name
        Set<String> uniforms = new HashSet<>();
        uniforms.addAll(getMirrorNames(MirrorImpl.UniformMirror.class, vertexStage));
        uniforms.addAll(getMirrorNames(MirrorImpl.UniformMirror.class, fragmentStage));
        Set<String> attributes = getMirrorNames(MirrorImpl.ShaderInputMirror.class, vertexStage);

        return new ShaderProgram(vertexGLSL, fragmentGLSL, uniforms, attributes, fragmentOutputs.keySet());
    }

    private static String toGLSL(int version, Stage shader) {
        StringBuilder finalShader = new StringBuilder();
        finalShader.append("#version ").append(version).append("\n\n");

        // FIXME handle struct type definitions

        // constants block
        for (Mirror constant : shader.ordering) {
            MirrorImpl<?> im = MirrorImpl.getImpl(constant);
            if (im instanceof MirrorImpl.ConstantMirror) {
                finalShader.append("const ").append(im.getTypeName()).append(" ")
                           .append(shader.getName(constant)).append(" = ")
                           .append(((MirrorImpl.ConstantMirror<?>) im).value).append(";\n");
            }
        }
        finalShader.append('\n');

        // uniforms block
        for (Mirror uniform : shader.ordering) {
            MirrorImpl<?> im = MirrorImpl.getImpl(uniform);
            if (im instanceof MirrorImpl.UniformMirror) {
                finalShader.append("uniform ").append(im.getTypeName()).append(" ")
                           .append(shader.getName(uniform)).append(";\n");
            }
        }
        finalShader.append('\n');

        List<Mirror> bodyOutputs = new ArrayList<>(); // will only hold ShaderOutputMirrors
        // shader inputs
        for (Mirror input : shader.ordering) {
            MirrorImpl<?> im = MirrorImpl.getImpl(input);
            if (im instanceof MirrorImpl.ShaderInputMirror) {
                finalShader.append("in ").append(im.getTypeName()).append(" ").append(shader.getName(input))
                           .append(";\n");
            }
        }
        finalShader.append('\n');

        // shader outputs
        for (Mirror output : shader.ordering) {
            MirrorImpl<?> im = MirrorImpl.getImpl(output);
            if (im instanceof MirrorImpl.ShaderOutputMirror) {
                String name = shader.getName(output);
                if (!name.startsWith("gl_")) {
                    finalShader.append("out ").append(im.getTypeName()).append(" ").append(name)
                               .append(";\n");
                }

                bodyOutputs.add(output);
            }
        }
        finalShader.append('\n');

        // function declarations
        Map<Class<? extends Node>, Function> declaredFunctions = new HashMap<>();
        List<NodeImpl<?>> functionCalls = new ArrayList<>();
        for (Mirror m : shader.ordering) {
            NodeImpl<?> node = fromMirror(m);
            if (node != null) {
                // append the function definition if necessary
                if (!declaredFunctions.containsKey(node.type)) {
                    Function f = new Function(node);
                    declaredFunctions.put(node.type, f);

                    f.appendFunction(finalShader);
                    finalShader.append('\n');
                }

                if (!functionCalls.contains(node)) {
                    functionCalls.add(node);
                }
            }
        }
        finalShader.append('\n');

        // main body
        finalShader.append("void main() {\n");

        // invoke functions and declare local variables for function outputs
        for (NodeImpl<?> call : functionCalls) {
            Function f = declaredFunctions.get(call.type);
            if (call.outputs.size() == 1) {
                // assign returned value directly to local variable
                Mirror returned = call.outputs.values().iterator().next();

                finalShader.append("   ").append(MirrorImpl.getImpl(returned).getTypeName()).append(' ')
                           .append(shader.getName(returned)).append(" = ").append(f.getName()).append('(');

                boolean first = true;
                for (String param : f.getParameters()) {
                    if (first) {
                        first = false;
                    } else {
                        finalShader.append(", ");
                    }
                    Mirror input = call.assignedInputs.get(param);
                    finalShader.append(shader.getName(input));
                }
                finalShader.append(");\n");
            } else {
                // declare value variables and then invoke function on its own line
                for (Mirror output : call.outputs.values()) {
                    finalShader.append("   ").append(MirrorImpl.getImpl(output).getTypeName()).append(' ')
                               .append(shader.getName(output)).append(";");
                }

                finalShader.append("   ").append(f.getName()).append('(');
                boolean first = true;
                for (String param : f.getParameters()) {
                    if (first) {
                        first = false;
                    } else {
                        finalShader.append(", ");
                    }
                    Mirror input = call.assignedInputs.get(param);
                    finalShader.append(shader.getName(input));
                }
                finalShader.append(");\n");
            }

            finalShader.append('\n');
        }

        // assign values to all remaining outputs
        for (Mirror output : bodyOutputs) {
            MirrorImpl.ShaderOutputMirror<?> im = (MirrorImpl.ShaderOutputMirror<?>) MirrorImpl
                    .getImpl(output);

            // variable already declared
            finalShader.append("   ").append(shader.getName(output)).append(" = ")
                       .append(shader.getName(im.value)).append(";\n");
        }
        finalShader.append("}\n");

        return finalShader.toString();
    }

    private static Set<String> getMirrorNames(Class<?> type, Stage stage) {
        Set<String> mirrors = new HashSet<>();
        for (Mirror m : stage.globalNames.keySet()) {
            MirrorImpl<?> im = MirrorImpl.getImpl(m);
            if (type.isInstance(im)) {
                mirrors.add(stage.globalNames.get(m));
            }
        }
        return mirrors;
    }

    private static void sortForFragmentShader(Mirror m, Stage stage, Set<Mirror> visited,
                                              LinkedHashSet<Mirror> forVertexStage) {
        if (!visited.contains(m)) {
            NodeImpl<?> n = fromMirror(m);
            if (n != null) {
                if (n.forceToVertexShader) {
                    // wrap the mirror with a fragment input reference
                    Mirror vertex = MirrorImpl.createShaderOutput(m);
                    forVertexStage.add(vertex);
                    stage.add(MirrorImpl.createFragmentInput(m));
                } else {
                    // visit incoming edges
                    for (Mirror in : n.assignedInputs.values()) {
                        sortForFragmentShader(in, stage, visited, forVertexStage);
                    }
                    stage.add(m);
                }
            } else {
                // m is a simple mirror, but check if it's a vertex attribute and configure a pass-through
                MirrorImpl<?> im = MirrorImpl.getImpl(m);
                if (im instanceof MirrorImpl.ShaderInputMirror) {
                    // the only scenario where we visit a shader input is if the user attached a vertex input
                    // mirror to the fragment node, since we don't visit a fragment input node that gets
                    // pushed to the vertex shader output
                    MirrorImpl.ShaderInputMirror<?> si = (MirrorImpl.ShaderInputMirror<?>) im;

                    // to handle it correctly, we record it as both an output for the vertex stage
                    // and then as an input here
                    Mirror vertex = MirrorImpl.createShaderOutput(m);
                    forVertexStage.add(vertex);
                    stage.add(MirrorImpl.createFragmentInput(m));
                } else if (im instanceof MirrorImpl.ShaderOutputMirror) {
                    // shader output mirrors are not visited directly, so this can only have come from
                    // one of the declared fragment outputs.
                    // we must visit it to ensure the value's dependencies are met
                    MirrorImpl.ShaderOutputMirror<?> w = (MirrorImpl.ShaderOutputMirror<?>) im;
                    sortForFragmentShader(w.value, stage, visited, forVertexStage);
                } else {
                    // regular mirror of some flavor, like a constant or uniform
                    stage.add(m);
                }
            }

            // add m to visited regardless of whether or not it ended up in the stage ordering
            visited.add(m);
        }
    }

    private static void sortForVertexShader(Mirror m, Stage stage, Set<Mirror> visited) {
        if (!visited.contains(m)) {
            NodeImpl<?> n = fromMirror(m);
            if (n != null) {
                // visit incoming edges
                for (Mirror in : n.assignedInputs.values()) {
                    sortForVertexShader(in, stage, visited);
                }
            } else {
                // m is a simple mirror, but check if it's a fragment attribute and configure wrapped
                MirrorImpl<?> im = MirrorImpl.getImpl(m);
                if (im instanceof MirrorImpl.ShaderOutputMirror) {
                    // depend on the wrapped mirror
                    sortForVertexShader(((MirrorImpl.ShaderOutputMirror<?>) im).value, stage, visited);
                }
            }
            stage.add(m);
            visited.add(m);
        }
    }

    private static NodeImpl<?> fromMirror(Mirror m) {
        MirrorImpl<?> im = MirrorImpl.getImpl(m);
        if (im instanceof MirrorImpl.NodeOutputMirror) {
            return NodeImpl.getImpl(((MirrorImpl.NodeOutputMirror) im).source);
        } else {
            return null;
        }
    }

    private static class Stage {
        private static final Map<Class<? extends MirrorImpl>, String> NAME_PREFIX;

        static {
            Map<Class<? extends MirrorImpl>, String> prefixes = new HashMap<>();
            prefixes.put(MirrorImpl.UniformMirror.class, "u");
            prefixes.put(MirrorImpl.ConstantMirror.class, "c");

            // 'v' is the valid prefix for vertex inputs, and we use special logic for fragment inputs
            // to recompute the actual name based on the associated vertex output
            prefixes.put(MirrorImpl.ShaderInputMirror.class, "v");

            // 'f' is the valid prefix for auto-generated names because gl_Position and fragment outputs
            // have explicit names
            prefixes.put(MirrorImpl.ShaderOutputMirror.class, "f");
            prefixes.put(MirrorImpl.NodeOutputMirror.class, "l");
            NAME_PREFIX = Collections.unmodifiableMap(prefixes);
        }

        final LinkedHashSet<Mirror> ordering;

        final Map<Mirror, String> globalNames;
        final Map<Mirror, String> localNames;

        final Set<Function> functions;

        public Stage() {
            ordering = new LinkedHashSet<>();
            globalNames = new HashMap<>();
            localNames = new HashMap<>();
            functions = new HashSet<>();
        }

        public <T extends Mirror> void add(T m, String name) {
            ordering.add(m);

            MirrorImpl<T> im = MirrorImpl.getImpl(m);
            NodeImpl<?> node = fromMirror(m);

            Map<Mirror, String> store = (node != null ? localNames : globalNames);
            if (name == null) {
                name = computeName(im);
            }

            store.put(m, name);
        }

        private static <T extends Mirror> String computeName(MirrorImpl<T> m) {
            if (m instanceof MirrorImpl.ShaderInputMirror) {
                // check for source
                MirrorImpl.ShaderInputMirror<T> si = (MirrorImpl.ShaderInputMirror<T>) m;
                if (si.source != null) {
                    return computeName(MirrorImpl.getImpl(si.source));
                }
            }

            // otherwise build the name
            return NAME_PREFIX.get(m.getClass()) + m.name + "_" + getUniquePostfix(m);
        }

        public <T extends Mirror> void add(T m) {
            add(m, null);
        }

        public String getName(Mirror m) {
            String n = localNames.get(m);
            if (n != null) {
                return n;
            }
            n = globalNames.get(m);
            if (n != null) {
                return n;
            }

            // now check if it's an input to the fragment stage
            for (Mirror in : globalNames.keySet()) {
                MirrorImpl<?> inImpl = MirrorImpl.getImpl(in);
                if (inImpl instanceof MirrorImpl.ShaderInputMirror) {
                    MirrorImpl.ShaderInputMirror<?> si = (MirrorImpl.ShaderInputMirror<?>) inImpl;
                    if (si.source != null && si.source.equals(m)) {
                        // it is indeed an input
                        n = globalNames.get(in);
                        globalNames.put(m, n); // make sure we don't search more than once
                        return n;
                    }
                }
            }

            // otherwise it's an unused node value mirror
            n = NAME_PREFIX.get(MirrorImpl.NodeOutputMirror.class) + "Unused_" + MirrorImpl.getImpl(m).name +
                "_" + getUniquePostfix(m);
            localNames.put(m, n);
            return n;
        }

        private static String getUniquePostfix(Object m) {
            int hash = 0xffff & ((m.hashCode() >> 16) ^ m.hashCode());
            return Integer.toHexString(hash);
        }
    }
}
