package com.ferox.scene;

import com.ferox.math.Color4f;
import com.ferox.math.Vector3f;
import com.ferox.entity.AbstractComponent;

public class DirectionLight extends AbstractComponent<DirectionLight> {
    private final Vector3f direction;
    private final Color4f color;

    public DirectionLight(Color4f color, Vector3f direction) {
        super(DirectionLight.class);
        
        this.direction = new Vector3f();
        this.color = new Color4f();
        
        setDirection(direction);
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
    
    public void setDirection(Vector3f direction) {
        if (direction == null)
            throw new NullPointerException("Direction cannot be null");
        this.direction.set(direction);
    }
}
