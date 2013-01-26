package com.ferox.resource.shader;

public interface ShaderAccumulator {
    public void addLine(String code);

    public ShaderAccumulator indent();

    public void accumulateFunction(Function f);
}
