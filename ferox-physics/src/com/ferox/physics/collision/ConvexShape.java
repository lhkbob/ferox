package com.ferox.physics.collision;

import com.ferox.math.Vector3f;
import com.ferox.math.bounds.AxisAlignedBox;

public interface ConvexShape {

    public AxisAlignedBox getBounds();
    
    public Vector3f computeSupport(Vector3f v, Vector3f result);
}
