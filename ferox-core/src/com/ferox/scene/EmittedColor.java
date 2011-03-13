package com.ferox.scene;

import com.ferox.math.ReadOnlyColor3f;

public final class EmittedColor extends ColorComponent<EmittedColor> {
    public EmittedColor(ReadOnlyColor3f color) {
        super(color);
    }
    
    public EmittedColor(EmittedColor clone) {
        super(clone);
    }
}
