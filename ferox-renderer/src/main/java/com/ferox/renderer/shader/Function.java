package com.ferox.renderer.shader;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
class Function {
    private final String name;

    // special parameter not in the function signature, and is 'return'ed
    private final Parameter returnedParameter;

    private final Parameter[] parameters;

    // FIXME parameter substitution?
    private final String[] body;

    private final Class<? extends Node> symbol;

    public Function(NodeImpl<?> impl) {
        this.symbol = impl.type;

        Node.Function funcDef = symbol.getAnnotation(Node.Function.class);

        body = funcDef.glsl();
        name = symbol.getSimpleName().toLowerCase(); // FIXME guarantee uniqueness

        List<Parameter> params = new ArrayList<>();
        for (String input : impl.validInputs.keySet()) {
            params.add(new Parameter(Qualifier.IN, impl.validInputs.get(input), input));
        }
        if (impl.validOutputs.size() > 1) {
            for (String output : impl.validOutputs.keySet()) {
                params.add(new Parameter(Qualifier.OUT, impl.validOutputs.get(output), output));
            }
            returnedParameter = null;
        } else if (impl.validOutputs.size() == 1) {
            String output = impl.validOutputs.keySet().iterator().next();
            returnedParameter = new Parameter(Qualifier.OUT, impl.validOutputs.get(output), output);
        } else {
            // void method FIXME is this really allowed?
            returnedParameter = null;
        }

        parameters = params.toArray(new Parameter[params.size()]);
    }

    public String getName() {
        return name;
    }

    public List<String> getParameters() {
        List<String> p = new ArrayList<>();
        for (Parameter param : parameters) {
            p.add(param.name);
        }
        return p;
    }

    public void appendFunction(StringBuilder sb) {
        if (returnedParameter == null) {
            sb.append("void ");
        } else {
            sb.append(returnedParameter.type.getAnnotation(Mirror.TypeName.class).value()).append(' ');
        }
        sb.append(name).append('(');
        boolean first = true;
        for (Parameter p : parameters) {
            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }
            sb.append(p.qualifier.toString().toLowerCase()).append(' ')
              .append(p.type.getAnnotation(Mirror.TypeName.class).value()).append(' ').append(p.name);
        }
        sb.append(") {\n");

        for (String b : body) {
            sb.append("   ").append(b).append('\n');
        }

        if (returnedParameter != null) {
            sb.append("   return ").append(returnedParameter.name).append(";\n");
        }
        sb.append("}\n");
    }

    @Override
    public int hashCode() {
        return symbol.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Function)) {
            return false;
        }
        return ((Function) o).symbol.equals(symbol);
    }

    private static class Parameter {
        final String name;
        final Qualifier qualifier;
        final Class<? extends Mirror> type;

        public Parameter(Qualifier qualifier, Class<? extends Mirror> type, String name) {
            this.name = name;
            this.qualifier = qualifier;
            this.type = type;
        }
    }
}
