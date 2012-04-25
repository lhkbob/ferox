package com.ferox.util.texture;

import java.io.IOException;
import java.io.InputStream;

import com.ferox.resource.Texture;

/**
 * An implementation of ImageFileLoader that relies on DDSTexture to load .dds
 * files.
 * 
 * @author Michael Ludwig
 */
public class DDSImageFileLoader implements ImageFileLoader {
    @Override
    public Texture readImage(InputStream stream) throws IOException {
        if (DDSTexture.isDDSTexture(stream))
            return DDSTexture.readTexture(stream);
        else
            return null;
    }
}