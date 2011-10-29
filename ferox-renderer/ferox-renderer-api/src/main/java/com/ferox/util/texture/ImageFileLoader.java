package com.ferox.util.texture;

import java.io.IOException;
import java.io.InputStream;

import com.ferox.resource.Texture;

/**
 * <p>
 * ImageFileLoader is a simple interface that provides loading capabilities for
 * various types of image data, to then convert them into TextureImages.
 * </p>
 * <p>
 * To keep the interface simple, it only deals in streams, if a file format
 * cannot be determined using just streams, then this interface is not suitable
 * for that type.
 * </p>
 * 
 * @author Michael Ludwig
 */
public interface ImageFileLoader {
    /**
     * <p>
     * Process the data from the given stream and return a Texture
     * representing its contents.
     * </p>
     * <p>
     * Return null if the stream doesn't represent an image of any supported
     * format. If it is an image of the expected type, but is otherwise invalid
     * or unsupported, then throw an exception.
     * </p>
     * <p>
     * If null is returned, the stream should not have its position modified. <br>
     * <i>The stream should not be closed</i>
     * </p>
     * 
     * @param stream The InputStream to attempt to read an image from
     * @return The read Texture, or null if this stream doesn't match a
     *         supported format
     * @throws IOException if there are any problems reading the texture
     */
    public Texture readImage(InputStream stream) throws IOException;
}
