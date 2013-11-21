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

        for (Map.Entry<String, Mirror> output : fragmentOutputs.entrySet()) {
            Mirror wrappedOutput = MirrorImpl.createShaderOutput(output.getValue(), false);
            sortForFragmentShader(wrappedOutput, fragmentStage, pushedToVertex);
            fragmentStage.add(wrappedOutput, output.getKey()); // force variable name to be output.getKey()
        }

        // these mirrors are already wrapped in the shader output mirror type
        for (Mirror vertOut : pushedToVertex) {
            sortForVertexShader(vertOut, vertexStage);
        }
        Mirror wrappedPosition = MirrorImpl.createShaderOutput(position, false);
        sortForVertexShader(wrappedPosition, vertexStage);
        vertexStage.add(wrappedPosition, "gl_Position"); // force variable name to be gl_Position


        String vertexGLSL = toGLSL(version, vertexStage, true);
        String fragmentGLSL = toGLSL(version, fragmentStage, false);

        // FIXME we may want this to be a map from Mirror to String because of the name mangling performed
        // in the generated source, so we can have the programmer hold onto the mirror and then use that to
        // query for the actual attr or uniform name
        Set<String> uniforms = new HashSet<>();
        uniforms.addAll(getMirrorNames(MirrorImpl.UniformMirror.class, vertexStage));
        uniforms.addAll(getMirrorNames(MirrorImpl.UniformMirror.class, fragmentStage));
        Set<String> attributes = getMirrorNames(MirrorImpl.VertexInputMirror.class, vertexStage);

        return new ShaderProgram(vertexGLSL, fragmentGLSL, uniforms, attributes, fragmentOutputs.keySet());
    }

    private static String toGLSL(int version, Stage shader, boolean vertexShader) {
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
        if (vertexShader) {
            // shader inputs
            for (Mirror input : shader.ordering) {
                MirrorImpl<?> im = MirrorImpl.getImpl(input);
                if (im instanceof MirrorImpl.VertexInputMirror) {
                    finalShader.append("in ").append(im.getTypeName()).append(" ")
                               .append(shader.getName(input)).append(";\n");
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
        } else {
            // shader inputs
            for (Mirror input : shader.ordering) {
                MirrorImpl<?> im = MirrorImpl.getImpl(input);
                if (im instanceof MirrorImpl.ShaderOutputMirror &&
                    ((MirrorImpl.ShaderOutputMirror<?>) im).fromVertexStage) {
                    finalShader.append("in ").append(im.getTypeName()).append(" ")
                               .append(shader.getName(input)).append(";\n");
                }
            }
            finalShader.append('\n');

            // shader outputs
            for (Mirror output : shader.ordering) {
                MirrorImpl<?> im = MirrorImpl.getImpl(output);
                if (im instanceof MirrorImpl.ShaderOutputMirror &&
                    !((MirrorImpl.ShaderOutputMirror<?>) im).fromVertexStage) {
                    String name = shader.getName(output);
                    if (!name.startsWith("gl_")) {
                        finalShader.append("out ").append(im.getTypeName()).append(" ").append(name)
                                   .append(";\n");
                    }

                    bodyOutputs.add(output);
                }
            }
            finalShader.append('\n');
        }

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
                // declare output variables and then invoke function on its own line
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
                       .append(shader.getName(im.output)).append(";\n");
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

    private static void sortForFragmentShader(Mirror m, Stage stage, LinkedHashSet<Mirror> forVertexStage) {
        if (!stage.ordering.contains(m)) {
            // FIXME I don't like wastefully creating this when it is meaingless
            Mirror wrapped = MirrorImpl.createShaderOutput(m, true);
            if (forVertexStage.contains(wrapped)) {
                return;
            }

            NodeImpl<?> n = fromMirror(m);
            if (n != null) {
                if (n.forceToVertexShader) {
                    // wrap the mirror with a fragment input reference
                    forVertexStage.add(wrapped);
                    m = wrapped;
                } else {
                    // visit incoming edges
                    for (Mirror in : n.assignedInputs.values()) {
                        sortForFragmentShader(in, stage, forVertexStage);
                    }
                }
            } else {
                // m is a simple mirror, but check if it's a vertex attribute and configure a pass-through
                MirrorImpl<?> im = MirrorImpl.getImpl(m);
                if (im instanceof MirrorImpl.VertexInputMirror) {
                    // wrap the mirror with a fragment input reference
                    // FIXME this is actually a problem because we can't update the fragment output variable
                    // to wrap the correct and expected shader output var we just created.  Thus the name
                    // gets added for the wrapper, but then the assignment code doesn't know how to get that name
                    //
                    // We could make something where we dynamically rebuild the connections as we walk the graph
                    // to form a new graph that has the proper type and instance relationships; this problem
                    // will show up when a function in the fragment shader takes in a vertex attr as well
                    // or when a node is forced into the fragment shader
                    //
                    // The other way is to lazily create the names, and if we can't find the name for a mirror
                    // we try to look up using various shader output wrappers. This could work but it seems
                    // less well specified.
                    forVertexStage.add(wrapped);
                    m = wrapped;
                } else if (im instanceof MirrorImpl.ShaderOutputMirror) {
                    // depend on the wrapped mirror only if this is not an input mirror from the vertex stage
                    MirrorImpl.ShaderOutputMirror<?> w = (MirrorImpl.ShaderOutputMirror<?>) im;
                    if (!w.fromVertexStage) {
                        sortForFragmentShader(w.output, stage, forVertexStage);
                    }
                }
            }

            stage.add(m);
        }
    }

    private static void sortForVertexShader(Mirror m, Stage stage) {
        if (!stage.ordering.contains(m)) {
            NodeImpl<?> n = fromMirror(m);
            if (n != null) {
                // visit incoming edges
                for (Mirror in : n.assignedInputs.values()) {
                    sortForVertexShader(in, stage);
                }
            } else {
                // m is a simple mirror, but check if it's a fragment attribute and configure wrapped
                MirrorImpl<?> im = MirrorImpl.getImpl(m);
                if (im instanceof MirrorImpl.ShaderOutputMirror) {
                    // depend on the wrapped mirror
                    sortForVertexShader(((MirrorImpl.ShaderOutputMirror<?>) im).output, stage);
                }
            }
            stage.add(m);
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
            prefixes.put(MirrorImpl.VertexInputMirror.class, "v");

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

            String uniquePostfix = getUniquePostfix(m);
            Map<Mirror, String> store;
            if (node != null) {
                if (name == null) {
                    name = NAME_PREFIX.get(im.getClass()) + node.name + "_" + im.name + "_" + uniquePostfix;
                }
                store = localNames;
            } else {
                if (name == null) {
                    name = NAME_PREFIX.get(im.getClass()) + im.name + "_" + uniquePostfix;
                }
                store = globalNames;
            }

            store.put(m, name);
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
            Mirror wrap = MirrorImpl.createShaderOutput(m, true);
            n = globalNames.get(wrap);
            if (n != null) {
                globalNames.put(m, n);
                return n;
            }

            // otherwise it's an unused node output mirror
            n = NAME_PREFIX.get(MirrorImpl.NodeOutputMirror.class) + "Unused_" + MirrorImpl.getImpl(m).name +
                "_" + getUniquePostfix(m);
            localNames.put(m, n);
            return n;
        }

        private String getUniquePostfix(Mirror m) {
            int hash = 0xffff & ((m.hashCode() >> 16) ^ m.hashCode());
            return Integer.toHexString(hash);
        }
    }
}
