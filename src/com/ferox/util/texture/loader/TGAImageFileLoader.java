package com.ferox.util.texture.loader;

import java.io.IOException;
import java.io.InputStream;

import com.ferox.resource.texture.TextureImage;

/**
 * An implementation of ImageFileLoader that relies on TGATexture to load .tga
 * files.
 * 
 * @author Michael Ludwig
 * 
 */
public class TGAImageFileLoader implements ImageFileLoader {
	@Override
	public TextureImage readImage(InputStream stream) throws IOException {
		if (TGATexture.isTGATexture(stream)) {
			return TGATexture.readTexture(stream);
		} else {
			return null;
		}
	}
}
