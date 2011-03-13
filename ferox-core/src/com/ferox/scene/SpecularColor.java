package com.ferox.scene;

import com.ferox.math.ReadOnlyColor3f;

public final class SpecularColor extends ColorComponent<SpecularColor> {
    public SpecularColor(ReadOnlyColor3f color) {
        super(color);
    }
    
    public SpecularColor(SpecularColor clone) {
        super(clone);
    }
}
