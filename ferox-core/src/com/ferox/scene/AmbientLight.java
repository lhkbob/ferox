package com.ferox.scene;

import com.ferox.entity.AbstractComponent;
import com.ferox.math.Color4f;

public class AmbientLight extends AbstractComponent<AmbientLight>{
    private final Color4f color;
    
    public AmbientLight(Color4f color) {
        super(AmbientLight.class);
        this.color = new Color4f(color);
    }
    
    public Color4f getColor() {
        return color;
    }
}
