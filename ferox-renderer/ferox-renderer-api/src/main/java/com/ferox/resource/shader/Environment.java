package com.ferox.resource.shader;

import java.util.HashMap;
import java.util.Map;

public class Environment {
    private final Environment parent;
    private final Map<String, Type> variables;

    private final boolean inLoop;
    private final boolean inFragmentShader;
    private final Type requiredReturnType;

    public Environment(boolean inFragmentShader) {
        this(inFragmentShader, new HashMap<String, Type>());
    }

    public Environment(boolean inFragmentShader, Map<String, Type> initialState) {
        this.parent = null;
        this.inFragmentShader = inFragmentShader;
        requiredReturnType = PrimitiveType.VOID;
        inLoop = false;
        variables = new HashMap<String, Type>(initialState);
    }

    private Environment(Environment parent, boolean inFragmentShader, boolean inLoop,
                        Type requiredReturnType, Map<String, Type> newState) {
        this.parent = parent;
        this.inFragmentShader = inFragmentShader;
        this.requiredReturnType = requiredReturnType;
        this.inLoop = inLoop;
        variables = newState;
    }

    public boolean inLoop() {
        return inLoop;
    }

    public Type getRequiredReturnType() {
        return requiredReturnType;
    }

    public boolean inFragmentShader() {
        return inFragmentShader;
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

    public Environment getRoot() {
        Environment p = this;
        while (p.parent != null) {
            p = p.parent;
        }
        return p;
    }

    public boolean declare(Type type, String name) {
        if (variables.get(name) != null) {
            return false;
        } else {
            variables.put(name, type);
            return true;
        }
    }

    public Environment functionScope(Type returnType, Map<String, Type> variables) {
        return new Environment(getRoot(), inFragmentShader, false, returnType,
                               new HashMap<String, Type>(variables));
    }

    public Environment newScope(boolean forLoop) {
        Map<String, Type> newState = new HashMap<String, Type>();
        return new Environment(this, inFragmentShader, forLoop || inLoop,
                               requiredReturnType, newState);
    }
}
