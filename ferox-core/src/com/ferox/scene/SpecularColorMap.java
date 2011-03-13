package com.ferox.scene;

import com.ferox.resource.Texture;

public class SpecularColorMap extends TextureMap<SpecularColorMap> {
    public SpecularColorMap(Texture spec) {
        super(spec);
    }
    
    public SpecularColorMap(SpecularColorMap clone) {
        super(clone);
    }

    @Override
    protected void validate(Texture tex) {
        // do nothing
    }
}
