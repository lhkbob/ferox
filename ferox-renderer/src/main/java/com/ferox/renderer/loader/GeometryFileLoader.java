package com.ferox.renderer.loader;

import com.ferox.renderer.Framework;
import com.ferox.renderer.geom.Geometry;

import java.io.BufferedInputStream;
import java.io.IOException;

/**
 *
 */
public interface GeometryFileLoader {
    public Geometry read(Framework framework, BufferedInputStream in) throws IOException;
}
