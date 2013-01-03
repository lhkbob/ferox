package com.ferox.scene.controller.ffp;

import com.ferox.math.Const;
import com.ferox.math.Matrix4;

public interface RenderState extends State {
    public void add(@Const Matrix4 transform);
}
