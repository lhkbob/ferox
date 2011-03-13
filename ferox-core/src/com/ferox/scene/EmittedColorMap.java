package com.ferox.scene;

import com.ferox.resource.Texture;

public class EmittedColorMap extends TextureMap<EmittedColorMap> {
    public EmittedColorMap(Texture emitted) {
        super(emitted);
    }
    
    public EmittedColorMap(EmittedColorMap clone) {
        super(clone);
    }

    @Override
    protected void validate(Texture tex) {
        // do nothing
    }
}
