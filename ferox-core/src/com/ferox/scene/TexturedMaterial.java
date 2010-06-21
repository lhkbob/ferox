package com.ferox.scene;

import com.ferox.resource.Texture;
import com.ferox.entity.AbstractComponent;

public final class TexturedMaterial extends AbstractComponent<TexturedMaterial> {
    private Texture primarySurface;
    private Texture decalSurface;
    
    public TexturedMaterial(Texture primary) {
        this(primary, null);
    }
    
    public TexturedMaterial(Texture primary, Texture decal) {
        super(TexturedMaterial.class);
        
        setPrimaryTexture(primary);
        setDecalTexture(decal);
    }
    
    public void setPrimaryTexture(Texture image) {
        primarySurface = image;
    }
    
    public Texture getPrimaryTexture() {
        return primarySurface;
    }
    
    public void setDecalTexture(Texture image) {
        decalSurface = image;
    }
    
    public Texture getDecalTexture() {
        return decalSurface;
    }
}
