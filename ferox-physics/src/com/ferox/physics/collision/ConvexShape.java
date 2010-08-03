package com.ferox.physics.collision;

import com.ferox.math.AxisAlignedBox;
import com.ferox.math.ReadOnlyVector3f;
import com.ferox.math.Vector3f;

public interface ConvexShape {

    public AxisAlignedBox getBounds();
    
    public Vector3f computeSupport(ReadOnlyVector3f v, Vector3f result);
}
