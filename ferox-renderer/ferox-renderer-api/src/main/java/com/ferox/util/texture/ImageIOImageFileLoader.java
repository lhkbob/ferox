package com.ferox.util.texture.loader;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import com.ferox.resource.Texture;

/**
 * An ImageFileLoader that uses ImageIO to load files in gif, png, or jpg files
 * (this depends on the ImageIO loaders present on a given system).
 * 
 * @author Michael Ludwig
 */
public class ImageIOImageFileLoader implements ImageFileLoader {
    @Override
    public Texture readImage(InputStream stream) throws IOException {
        // I'm assuming that read() will restore the stream's position
        // if no reader is found

        BufferedImage b = ImageIO.read(stream);
        if (b != null) {
            if (b.getHeight() == 1)
                return TextureLoader.createTexture1D(b);
            else
                return TextureLoader.createTexture2D(b);
        } else
            return null;
    }

}
