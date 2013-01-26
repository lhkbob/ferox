package com.ferox.resource.shader;

import java.util.Map;

public interface Function {
    public Expression call(Expression... parameters);

    public String getName();

    public Map<String, Type> getParameters();

    public Type getReturnType();
}
