package com.ferox.scene;

import com.ferox.math.Color4f;
import com.ferox.entity.AbstractComponent;

public final class BlinnPhongLightingModel extends AbstractComponent<BlinnPhongLightingModel> {
    
    private static final Color4f DEFAULT_SPEC = new Color4f(0f, 0f, 0f, 1f);
    private static final Color4f DEFAULT_AMB = new Color4f(.2f, .2f, .2f, 1f);
    private static final float DEFAULT_SHININESS = 1f;
    
    private final Color4f ambient;
    private final Color4f diffuse;
    private final Color4f specular;
    
    private float shininess;
    
    public BlinnPhongLightingModel(Color4f diff) {
        this(diff, DEFAULT_SPEC);
    }
    
    public BlinnPhongLightingModel(Color4f diff, Color4f spec) {
        this(diff, spec, DEFAULT_SHININESS);
    }
    
    public BlinnPhongLightingModel(Color4f diff, float shininess) {
        this(diff, DEFAULT_SPEC, shininess);
    }
    
    public BlinnPhongLightingModel(Color4f diff, Color4f specular, float shininess) {
        this(DEFAULT_AMB, diff, specular, shininess);
    }
    
    public BlinnPhongLightingModel(Color4f ambient, Color4f diffuse, Color4f specular, float shininess) {
        super(BlinnPhongLightingModel.class);
        
        this.ambient = new Color4f(ambient);
        this.diffuse = new Color4f(diffuse);
        this.specular = new Color4f(specular);
        
        setShininess(shininess);
    }
    
    public Color4f getAmbient() {
        return ambient;
    }
    
    public Color4f getDiffuse() {
        return diffuse;
    }
    
    public Color4f getSpecular() {
        return specular;
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
