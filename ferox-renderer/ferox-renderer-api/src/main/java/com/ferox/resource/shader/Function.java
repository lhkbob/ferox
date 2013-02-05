package com.ferox.resource.shader;

import java.util.Map;

public interface Function extends GlslElement {
    public Expression call(Expression... parameters);

    public String getName();

    public Map<String, Type> getParameters();

    public Type[] getParameterTypes();

    public Type getReturnType();
}
