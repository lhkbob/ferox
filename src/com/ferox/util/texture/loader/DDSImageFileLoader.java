package com.ferox.util.texture.loader;

import java.io.IOException;
import java.io.InputStream;

import com.ferox.resource.texture.TextureImage;

/**
 * An implementation of ImageFileLoader that relies on DDSTexture to load .dds
 * files.
 * 
 * @author Michael Ludwig
 * 
 */
public class DDSImageFileLoader implements ImageFileLoader {
	@Override
	public TextureImage readImage(InputStream stream) throws IOException {
		if (DDSTexture.isDDSTexture(stream))
			return DDSTexture.readTexture(stream);
		else
			return null;
	}
}
