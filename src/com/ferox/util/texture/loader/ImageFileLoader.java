package com.ferox.util.texture.loader;

import java.io.IOException;
import java.io.InputStream;

import com.ferox.resource.texture.TextureImage;

/**
 * ImageFileLoader is a simple interface that provides loading capabilities for
 * various types of image data, to then convert them into TextureImages.
 * 
 * To keep the interface simple, it only deals in streams, if a file format
 * cannot be determined using just streams, then this interface is not suitable
 * for that type.
 * 
 * @author Michael Ludwig
 * 
 */
public interface ImageFileLoader {
	/**
	 * Process the data from the given stream and return a TextureImage
	 * representing its contents.
	 * 
	 * Return null if the stream doesn't represent an image of any supported
	 * format. If it is an image of the expected type, but is otherwise invalid
	 * or unsupported, then throw an exception.
	 * 
	 * If null is returned, the stream should not have its position modified.
	 * 
	 * Throw an IOException if there are any problems.
	 */
	public TextureImage readImage(InputStream stream) throws IOException;
}
