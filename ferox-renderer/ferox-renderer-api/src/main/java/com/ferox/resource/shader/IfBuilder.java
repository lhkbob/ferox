package com.ferox.resource.shader;

public interface IfBuilder {
    public IfBuilder then(Statement... body);

    public Statement fi();

    public Statement else_(Statement... body);
}
