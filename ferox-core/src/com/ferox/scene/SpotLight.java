package com.ferox.scene;

import com.ferox.entity.AbstractComponent;
import com.ferox.math.Color4f;
import com.ferox.math.ReadOnlyVector3f;
import com.ferox.math.Vector3f;

public class SpotLight extends AbstractComponent<SpotLight> {
    private final Vector3f direction;
    private final Vector3f position;
    
    private final Color4f color;
    
    private float cutoffAngle;

    public SpotLight(Color4f color, ReadOnlyVector3f position) {
        this(color, position, 180f, new Vector3f(0f, 0f, 1f));
    }
    
    public SpotLight(Color4f color, ReadOnlyVector3f position, float cutoffAngle, ReadOnlyVector3f direction) {
        super(SpotLight.class);
        
        this.direction = new Vector3f(direction);
        this.position = new Vector3f(position);
        this.color = new Color4f(color);
        
        setCutoffAngle(cutoffAngle);
    }
    
    public Vector3f getPosition() {
        return position;
    }
    
    public float getCutoffAngle() {
        return cutoffAngle;
    }
    
    public void setCutoffAngle(float angle) {
        if ((angle < 0 || angle > 90) && angle != 180f)
            throw new IllegalArgumentException("Illegal cutoff angle, must be in [0, 90] or 180, not: " + angle);
        cutoffAngle = angle;
    }
    
    public Color4f getColor() {
        return color;
    }
    
    public Vector3f getDirection() {
        return direction;
    }
}
