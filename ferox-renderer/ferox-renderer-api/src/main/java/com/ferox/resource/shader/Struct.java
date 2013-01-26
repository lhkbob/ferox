package com.ferox.resource.shader;

import java.util.Map;

public interface Struct extends Type, GlslElement {
    public Map<String, Type> getFields();
}
