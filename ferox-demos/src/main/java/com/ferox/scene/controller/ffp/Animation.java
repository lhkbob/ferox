package com.ferox.scene.controller.ffp;

import com.ferox.math.Const;
import com.ferox.math.Vector3;
import com.ferox.math.entreri.Vector3Property;
import com.lhkbob.entreri.Component;
import com.lhkbob.entreri.property.DoubleProperty;
import com.lhkbob.entreri.property.SharedInstance;

/**
 *
 */
public interface Animation extends Component {
    @DoubleProperty.DefaultDouble(1)
    public double getLifetime();

    public void setLifetime(double lt);

    @Const
    @SharedInstance
    @Vector3Property.DefaultVector3(x = 0, y = 0, z = 0)
    public Vector3 getDirection();

    public void setDirection(@Const Vector3 dir);
}
