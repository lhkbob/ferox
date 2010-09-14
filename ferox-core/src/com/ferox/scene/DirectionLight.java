package com.ferox.scene;

import com.ferox.entity.AbstractComponent;
import com.ferox.math.Color4f;
import com.ferox.math.ReadOnlyVector3f;
import com.ferox.math.Vector3f;

public class DirectionLight extends AbstractComponent<DirectionLight> {
    private final Vector3f direction;
    private final Color4f color;

    public DirectionLight(Color4f color, ReadOnlyVector3f direction) {
        super(DirectionLight.class);
        
        this.direction = new Vector3f(direction);
        this.color = new Color4f();
        
        setColor(color);
    }
    
    public Color4f getColor() {
        return color;
    }
    
    public void setColor(Color4f color) {
        if (color == null)
            throw new NullPointerException("Color cannot be null");
        this.color.set(color);
    }
    
    public Vector3f getDirection() {
        return direction;
    }
}
