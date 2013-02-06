package com.ferox.resource.shader;

public interface ShaderAccumulator {
    public void addLine(String code);

    public void pushIndent();

    public void popIndent();

    public void accumulateFunction(Function f);

    public void accumulateStruct(Struct s);

    public ShaderAccumulator getGlobalDeclarationAccumulator();

    public ShaderAccumulator getMainAccumulator();
}
