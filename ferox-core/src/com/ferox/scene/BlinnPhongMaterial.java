package com.ferox.scene;

import com.ferox.entity.TypedComponent;

public final class BlinnPhongMaterial extends TypedComponent<BlinnPhongMaterial> {
    private float shininess;
    
    public BlinnPhongMaterial() {
        this(1f);
    }
    
    public BlinnPhongMaterial(float shininess) {
        super(null, false);
        setShininess(shininess);
    }
    public BlinnPhongMaterial(BlinnPhongMaterial clone) {
        super(clone, true);
        shininess = clone.shininess;
    }
    
    public void setShininess(float shiny) {
        if (shiny < 0f)
            throw new IllegalArgumentException("Shininess must be positive, not: " + shiny);
        shininess = shiny;
    }
    
    public float getShininess() {
        return shininess;
    }
}
