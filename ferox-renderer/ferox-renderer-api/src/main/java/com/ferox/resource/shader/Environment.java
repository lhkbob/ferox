package com.ferox.resource.shader;

import java.util.HashMap;
import java.util.Map;

public class Environment {
    private final Environment parent;
    private final Map<String, Type> variables;

    public Environment() {
        this(new HashMap<String, Type>());
    }

    public Environment(Map<String, Type> initialState) {
        this.parent = null;
        variables = new HashMap<String, Type>(initialState);
    }

    private Environment(Environment parent, Map<String, Type> newState) {
        this.parent = parent;
        variables = newState;
    }

    public Type getVariable(String name) {
        Type inScope = variables.get(name);
        if (inScope == null && parent != null) {
            return parent.getVariable(name);
        }
        return inScope;
    }

    public Environment getParent() {
        return parent;
    }

    public Environment declare(Type type, String name) {
        Map<String, Type> newState = new HashMap<String, Type>(variables);
        newState.put(name, type);
        return new Environment(this, newState);
    }

    public Environment newScope() {
        Map<String, Type> newState = new HashMap<String, Type>(variables);
        return new Environment(this, newState);
    }
}
