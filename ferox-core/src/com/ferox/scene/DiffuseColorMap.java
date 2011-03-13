package com.ferox.scene;

import com.ferox.resource.Texture;

public class DiffuseColorMap extends TextureMap<DiffuseColorMap> {
    public DiffuseColorMap(Texture diffuse) {
        super(diffuse);
    }
    
    public DiffuseColorMap(DiffuseColorMap clone) {
        super(clone);
    }

    @Override
    protected void validate(Texture tex) {
        // do nothing
    }
}
