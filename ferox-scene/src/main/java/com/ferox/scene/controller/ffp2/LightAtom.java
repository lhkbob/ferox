package com.ferox.scene.controller.ffp2;

import com.ferox.math.Color3f;
import com.ferox.math.Vector3f;
import com.ferox.math.Vector4f;

public class LightAtom {
    public final Color3f color;
    public final Vector4f position; // or direction for direction lights
    public final Vector3f spotlightDirection;
    public float cutoffAngle;
    
    public LightAtom() {
        color = new Color3f();
        position = new Vector4f();
        spotlightDirection = new Vector3f();
    }
}
