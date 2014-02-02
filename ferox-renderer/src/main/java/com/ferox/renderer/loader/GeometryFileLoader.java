package com.ferox.renderer.loader;

import com.ferox.renderer.Framework;
import com.ferox.renderer.geom.Geometry;

import java.io.IOException;
import java.io.InputStream;

/**
 *
 */
public interface GeometryFileLoader {
    public Geometry read(Framework framework, InputStream in) throws IOException;
}
