/*
 * Ferox, a graphics and game library in Java
 *
 * Copyright (c) 2012, Michael Ludwig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright notice,
 *         this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright notice,
 *         this list of conditions and the following disclaimer in the
 *         documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.ferox.renderer.loader;

import com.ferox.renderer.*;
import com.ferox.renderer.builder.*;

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.image.*;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * <p/>
 * TextureIO provides functionality to load image files as Texture objects. It provides some utilities to
 * generate a Texture from a BufferedImage.
 * <p/>
 * By default, the DDS, TGA and ImageIO image file loaders are registered.
 *
 * @author Michael Ludwig
 */
public final class TextureLoader {
    private static final List<ImageFileLoader> loaders = new ArrayList<>();

    // register some default loaders
    static {
        registerLoader(new ImageIOImageFileLoader());
        registerLoader(new DDSImageFileLoader());
        registerLoader(new RadianceImageLoader());
    }

    private TextureLoader() {
    }

    /**
     * <p/>
     * Register the given loader, so that it can be used in subsequent readTexture() calls. The newer loaders
     * are favored when resolving conflicts between loaders that are capable of loading the same file.
     * <p/>
     * Does nothing if e is null. If e has already been registered, then e becomes the "newest" with regards
     * to resolving conflicts.
     *
     * @param e An ImageFileLoader to register for use
     */
    public static void registerLoader(ImageFileLoader e) {
        synchronized (loaders) {
            if (e != null) {
                int index = loaders.indexOf(e);
                if (index >= 0) {
                    loaders.remove(index);
                }
                loaders.add(e);
            }
        }
    }

    /**
     * Remove the given loader. Does nothing if it's null or was never registered. After a call to this
     * method, that loader instance will not be used in calls to readTexture().
     *
     * @param e An ImageFileLoader that should no longer be used
     */
    public static void unregisterLoader(ImageFileLoader e) {
        synchronized (loaders) {
            if (e != null) {
                loaders.remove(e);
            }
        }
    }

    /**
     * Read the texture from the given file, functions identically to readTexture(stream).
     *
     * @param framework The Framework using the created texture
     * @param file      The File to read a texture from
     *
     * @return The read Texture
     *
     * @throws IOException if the file can't be read, if it's unsupported, etc.
     */
    // FIXME returning builders does not let you configure the texture parameters, we need a better option
    // consider the geometry approach, where we don't load directly to the GPU resource?
    public static Builder<? extends Sampler> readTexture(Framework framework, File file) throws IOException {
        if (file == null) {
            throw new IOException("Cannot load a texture image from a null file");
        }

        try (InputStream stream = new FileInputStream(file)) {
            return readTexture(framework, stream);
        }
    }

    /**
     * Read the texture from the given URL, functions identically to readTexture(stream).
     *
     * @param framework The Framework using the created texture
     * @param url       The URL representing the Texture
     *
     * @return The read Texture
     *
     * @throws IOException if the texture couldn't be read, if it's unsupported or invalid, etc.
     */
    public static Builder<? extends Sampler> readTexture(Framework framework, URL url) throws IOException {
        if (url == null) {
            throw new IOException("Cannot read from a null URL");
        }
        try (InputStream urlStream = url.openStream()) {
            return readTexture(framework, urlStream);
        }
    }

    /**
     * <p/>
     * Read a texture from the given stream. This assumes that the texture begins with the next bytes read
     * from the stream, and that the stream is already opened.
     * <p/>
     * For standard images (e.g. jpg, png, gif, etc.) Texture2D or Texture1D is used. If the file has a height
     * of 1 pixel then it is a Texture1D otherwise it's 2D. DDS formats contain the type of texture within it,
     * such as 1D, 2D, cube map, or array and the Sampler implementation is appropriately selected.
     * <p/>
     * This method does not close the stream, in case it's to be used later on.
     *
     * @param framework The Framework using the created texture
     * @param stream    The InputStream to read the texture from
     *
     * @return The read Texture, will be a Texture1D, Texture2D, TextureCubeMap or Texture3D (2d images use
     * Texture2D by default, see convertToRectangle())
     *
     * @throws IOException if the stream can't be read from, it represents an invalid or unsupported texture
     *                     type, etc.
     */
    public static Builder<? extends Sampler> readTexture(Framework framework, InputStream stream)
            throws IOException {
        // make sure we're buffered
        BufferedInputStream in;
        if (stream instanceof BufferedInputStream) {
            in = (BufferedInputStream) stream;
        } else {
            in = new BufferedInputStream(stream);
        }

        // load the file
        Builder<? extends Sampler> t;

        synchronized (loaders) {
            for (int i = loaders.size() - 1; i >= 0; i--) {
                t = loaders.get(i).read(framework, in);
                if (t != null) {
                    return t; // we've loaded it
                }
            }
        }

        throw new IOException("Unable to load the given texture, no registered loader with support");
    }

    /**
     * Utility function to convert the given BufferedImage into a Texture with the target of T_1D. This can be
     * a slower operation because it has to redraw the buffered image to make sure it has an appropriate
     * raster and color model.
     *
     * @param framework The Framework using the created texture
     * @param image     The BufferedImage to convert into a Texture
     *
     * @return The converted Texture with a T_1D target
     *
     * @throws NullPointerException     if image is null
     * @throws IllegalArgumentException if image doesn't have a height of 1
     */
    public static Builder<Texture1D> createTexture1D(Framework framework, BufferedImage image) {
        if (image == null) {
            throw new NullPointerException("Cannot convert a null BufferedImage");
        }
        if (image.getHeight() != 1) {
            throw new IllegalArgumentException("A BufferedImage can only be converted to a Texture1D with height == 1, not: " +
                                               image.getHeight());
        }

        // draw the image into a known color model
        RasterImage im = new RasterImage(image.getType(), image.getWidth(), 1);
        BufferedImage formatted = new BufferedImage(im.colorModel, im.data, false, null);
        Graphics2D g2 = formatted.createGraphics();
        g2.drawImage(formatted, 0, 0, null);
        g2.dispose();

        Texture1DBuilder b = framework.newTexture1D();
        b.length(image.getWidth());
        ImageData<? extends TextureBuilder.BasicColorData> i;
        if (im.format == Sampler.TexelFormat.RGBA) {
            i = b.rgba();
        } else if (im.format == Sampler.TexelFormat.RGB) {
            i = b.rgb();
        } else {
            i = b.r();
        }

        if (im.type.equals(DataType.BYTE)) {
            byte[] rd = ((DataBufferByte) formatted.getRaster().getDataBuffer()).getData();
            i.mipmap(0).fromUnsignedNormalized(rd);
        } else { // assumes ShortBuffer
            short[] rd = ((DataBufferUShort) formatted.getRaster().getDataBuffer()).getData();
            i.mipmap(0).fromUnsignedNormalized(rd);
        }

        return b;
    }

    /**
     * Utility function to convert the given BufferedImage into a Texture with a target of T_2D. This can be a
     * slower operation because it has to redraw the buffered image to make sure it has an appropriate raster
     * and color model. It also flips the converted image to match the coordinate system of Texture (origin at
     * the lower left).
     *
     * @param framework The Framework that creates the texture
     * @param image     The BufferedImage to convert into a Texture2D
     *
     * @return The converted buffered image as a Texture2D
     *
     * @throws NullPointerException if image is null
     */
    public static Builder<Texture2D> createTexture2D(Framework framework, BufferedImage image) {
        if (image == null) {
            throw new NullPointerException("Cannot convert a null BufferedImage");
        }

        // draw the image into a known color model
        RasterImage im = new RasterImage(image.getType(), image.getWidth(), image.getHeight());
        BufferedImage formatted = new BufferedImage(im.colorModel, im.data, false, null);
        Graphics2D g2 = formatted.createGraphics();

        // flip the image along the y-axis to match OpenGL's coordinate system
        AffineTransform t = AffineTransform.getScaleInstance(1, -1);
        t.concatenate(AffineTransform.getTranslateInstance(0, -image.getHeight()));
        g2.drawImage(image, t, null);
        g2.dispose();

        Texture2DBuilder b = framework.newTexture2D();
        b.width(image.getWidth()).height(image.getHeight());
        ImageData<? extends TextureBuilder.BasicColorData> i;
        if (im.format == Sampler.TexelFormat.RGBA) {
            i = b.rgba();
        } else if (im.format == Sampler.TexelFormat.RGB) {
            i = b.rgb();
        } else {
            i = b.r();
        }

        if (im.type.equals(DataType.BYTE)) {
            byte[] rd = ((DataBufferByte) formatted.getRaster().getDataBuffer()).getData();
            i.mipmap(0).fromUnsignedNormalized(rd);
        } else { // assumes ShortBuffer
            short[] rd = ((DataBufferUShort) formatted.getRaster().getDataBuffer()).getData();
            i.mipmap(0).fromUnsignedNormalized(rd);
        }

        return b;
    }

    /**
     * <p/>
     * Like createTexture2D(), except it identifies 6 faces from the cube map as if the cube were unfolded to
     * lie flat on a rectangle (so the rectangular image must have room for 4 cube faces along its width, and
     * 3 faces on its height).  The image is laid out like so:
     * <p/>
     * <pre>
     * •----•----•----•----•
     * | -- | NZ | -- | -- |
     * •----•----•----•----•
     * | NX | NY | PX | PY |
     * •----•----•----•----•
     * | -- | PZ | -- | -- |
     * •----•----•----•----•
     * </pre>
     * <p/>
     * <p/>
     * Because of this, the specified image must have an aspect ration of 4/3 for the creation to work. Other
     * than this, the creation process functions like createTexture2D().
     *
     * @param framework The Framework that creates the texture
     * @param image     The BufferedImage to interpret as a cube map
     *
     * @return The converted image as a TextureCubeMap
     *
     * @throws NullPointerException     if image is null
     * @throws IllegalArgumentException if image.getWidth() / 4 != image.getHeight() / 3
     */
    public static Builder<TextureCubeMap> createTextureCubeMap(Framework framework, BufferedImage image) {
        if (image == null) {
            throw new NullPointerException("Cannot create a cube map from a null BufferedImage");
        }

        int side = image.getWidth() / 4;
        if (side * 4 != image.getWidth() || side * 3 != image.getHeight()) {
            throw new IllegalArgumentException("Base image doesn't have the 4x3 aspect ration necessary for a cube map");
        }

        RasterImage im = new RasterImage(image.getType(), side, side);
        BufferedImage formatted = new BufferedImage(im.colorModel, im.data, false, null);

        TextureCubeMapBuilder b = framework.newTextureCubeMap();
        b.side(side);
        CubeImageData<? extends TextureBuilder.BasicColorData> i;
        if (im.format == Sampler.TexelFormat.RGBA) {
            i = b.rgba();
        } else if (im.format == Sampler.TexelFormat.RGB) {
            i = b.rgb();
        } else {
            i = b.r();
        }

        createCubeMapFace(image, formatted, i.positiveX(0), 0, im);
        createCubeMapFace(image, formatted, i.positiveY(0), 1, im);
        createCubeMapFace(image, formatted, i.positiveZ(0), 2, im);
        createCubeMapFace(image, formatted, i.negativeX(0), 3, im);
        createCubeMapFace(image, formatted, i.negativeY(0), 4, im);
        createCubeMapFace(image, formatted, i.negativeZ(0), 5, im);

        return b;
    }

    /*
     * Internal method that redraws fullImage into faceStore, and then copies
     * out the raster data into a new BufferData. A copy is made since it is
     * assumed that faceStore is re-used for each cube face.
     */
    private static void createCubeMapFace(BufferedImage fullImage, BufferedImage faceStore,
                                          TextureBuilder.BasicColorData target, int face, RasterImage im) {
        Graphics2D g2 = faceStore.createGraphics();
        AffineTransform t = AffineTransform.getScaleInstance(1, 1);

        // setup up a transform that adjusts the fullImage into the correct
        // space for a single face
        switch (face) {
        case 0: // PX
            t.concatenate(AffineTransform.getTranslateInstance(.5 * faceStore.getWidth(),
                                                               .5 * faceStore.getHeight()));
            t.concatenate(AffineTransform.getScaleInstance(-1, 1));
            t.concatenate(AffineTransform.getRotateInstance(-Math.PI / 2));
            t.concatenate(AffineTransform.getTranslateInstance(-.5 * faceStore.getWidth(),
                                                               -.5 * faceStore.getHeight()));
            t.concatenate(AffineTransform.getTranslateInstance(-2 * faceStore.getWidth(),
                                                               -1 * faceStore.getHeight()));
            break;
        case 1: // PY
            t.concatenate(AffineTransform.getTranslateInstance(.5 * faceStore.getWidth(),
                                                               .5 * faceStore.getHeight()));
            t.concatenate(AffineTransform.getScaleInstance(-1, 1));
            t.concatenate(AffineTransform.getRotateInstance(0));
            t.concatenate(AffineTransform.getTranslateInstance(-.5 * faceStore.getWidth(),
                                                               -.5 * faceStore.getHeight()));
            t.concatenate(AffineTransform.getTranslateInstance(-3 * faceStore.getWidth(),
                                                               -1 * faceStore.getHeight()));
            break;
        case 2: // PZ
            t.concatenate(AffineTransform.getTranslateInstance(.5 * faceStore.getWidth(),
                                                               .5 * faceStore.getHeight()));
            t.concatenate(AffineTransform.getScaleInstance(-1, 1));
            t.concatenate(AffineTransform.getRotateInstance(-Math.PI));
            t.concatenate(AffineTransform.getTranslateInstance(-.5 * faceStore.getWidth(),
                                                               -.5 * faceStore.getHeight()));
            t.concatenate(AffineTransform.getTranslateInstance(-1 * faceStore.getWidth(),
                                                               -2 * faceStore.getHeight()));
            break;
        case 3: // NX
            t.concatenate(AffineTransform.getTranslateInstance(.5 * faceStore.getWidth(),
                                                               .5 * faceStore.getHeight()));
            t.concatenate(AffineTransform.getScaleInstance(-1, 1));
            t.concatenate(AffineTransform.getRotateInstance(Math.PI / 2));
            t.concatenate(AffineTransform.getTranslateInstance(-.5 * faceStore.getWidth(),
                                                               -.5 * faceStore.getHeight()));
            t.concatenate(AffineTransform.getTranslateInstance(0, -1 * faceStore.getHeight()));
            break;
        case 4: // NY
            t.concatenate(AffineTransform.getTranslateInstance(.5 * faceStore.getWidth(),
                                                               .5 * faceStore.getHeight()));
            t.concatenate(AffineTransform.getScaleInstance(1, -1));
            t.concatenate(AffineTransform.getRotateInstance(0));
            t.concatenate(AffineTransform.getTranslateInstance(-.5 * faceStore.getWidth(),
                                                               -.5 * faceStore.getHeight()));
            t.concatenate(AffineTransform.getTranslateInstance(-1 * faceStore.getWidth(),
                                                               -1 * faceStore.getHeight()));
            break;
        case 5: // NZ
            t.concatenate(AffineTransform.getTranslateInstance(.5 * faceStore.getWidth(),
                                                               .5 * faceStore.getHeight()));
            t.concatenate(AffineTransform.getScaleInstance(1, 1));
            t.concatenate(AffineTransform.getRotateInstance(0));
            t.concatenate(AffineTransform.getTranslateInstance(-.5 * faceStore.getWidth(),
                                                               -.5 * faceStore.getHeight()));
            t.concatenate(AffineTransform.getTranslateInstance(-1 * faceStore.getWidth(), 0));
            break;
        }

        g2.drawImage(fullImage, t, null);
        g2.dispose();

        if (im.type.equals(DataType.BYTE)) {
            byte[] rd = ((DataBufferByte) faceStore.getRaster().getDataBuffer()).getData();
            target.fromUnsignedNormalized(rd);
        } else { // assumes ShortBuffer
            short[] rd = ((DataBufferUShort) faceStore.getRaster().getDataBuffer()).getData();
            target.fromUnsignedNormalized(rd);
        }
    }

    /*
     * Utility class used to identify a format and type based on the
     * BufferedImage. It also creates a color model and raster suitable for
     * creating new buffered images.
     */
    private static class RasterImage {
        // Ferox specific variables
        Sampler.TexelFormat format;
        DataType type;

        // BufferImage specific variables
        ColorModel colorModel;
        WritableRaster data;

        public RasterImage(int imageType, int width, int height) {
            switch (imageType) {
            case BufferedImage.TYPE_3BYTE_BGR:
            case BufferedImage.TYPE_INT_BGR:
            case BufferedImage.TYPE_INT_RGB:
            case BufferedImage.TYPE_USHORT_555_RGB:
            case BufferedImage.TYPE_USHORT_565_RGB:
                colorModel = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_LINEAR_RGB),
                                                     new int[] { 8, 8, 8, 0 }, false, false,
                                                     Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
                data = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, width, height, 3, null);

                type = DataType.BYTE;
                format = Sampler.TexelFormat.RGB;
                break;
            case BufferedImage.TYPE_USHORT_GRAY:
                colorModel = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY),
                                                     new int[] { 16 }, false, false, Transparency.OPAQUE,
                                                     DataBuffer.TYPE_USHORT);
                data = Raster.createInterleavedRaster(DataBuffer.TYPE_USHORT, width, height, 1, null);

                type = DataType.SHORT;
                format = Sampler.TexelFormat.R;
                break;
            case BufferedImage.TYPE_BYTE_GRAY:
                colorModel = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY),
                                                     new int[] { 8 }, false, false, Transparency.OPAQUE,
                                                     DataBuffer.TYPE_BYTE);
                data = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, width, height, 1, null);

                type = DataType.BYTE;
                format = Sampler.TexelFormat.R;
                break;
            default:
                colorModel = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_LINEAR_RGB),
                                                     new int[] { 8, 8, 8, 8 }, true, false,
                                                     Transparency.TRANSLUCENT, DataBuffer.TYPE_BYTE);
                data = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, width, height, 4, null);

                type = DataType.BYTE;
                format = Sampler.TexelFormat.RGBA;
                break;
            }
        }
    }
}
