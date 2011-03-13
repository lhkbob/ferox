package com.ferox.scene;

import com.ferox.math.ReadOnlyColor3f;

public final class DiffuseColor extends ColorComponent<DiffuseColor> {
    public DiffuseColor(ReadOnlyColor3f color) {
        super(color);
    }
    
    public DiffuseColor(DiffuseColor clone) {
        super(clone);
    }
}
