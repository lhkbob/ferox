package com.ferox.util.texture;

import java.awt.Graphics2D;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferUShort;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.ferox.resource.BufferData;
import com.ferox.resource.BufferData.DataType;
import com.ferox.resource.Mipmap;
import com.ferox.resource.Texture;
import com.ferox.resource.Texture.Target;
import com.ferox.resource.TextureFormat;

/**
 * <p>
 * TextureIO provides functionality to load image files as Texture objects.
 * It provides some utilities to generate a Texture from a BufferedImage.
 * </p>
 * <p>
 * By default, the DDS, TGA and ImageIO image file loaders are registered.
 * </p>
 * 
 * @author Michael Ludwig
 */
public class TextureLoader {
    private static final List<ImageFileLoader> loaders = new ArrayList<ImageFileLoader>();

    // register some default loaders
    static {
        registerLoader(new ImageIOImageFileLoader());
        registerLoader(new TGAImageFileLoader());
        registerLoader(new DDSImageFileLoader());
    }

    /**
     * <p>
     * Register the given loader, so that it can be used in subsequent
     * readTexture() calls. The newer loaders are favored when resolving
     * conflicts between loaders that are capable of loading the same file.
     * </p>
     * <p>
     * Does nothing if e is null. If e has already been registered, then e
     * becomes the "newest" with regards to resolving conflicts.
     * </p>
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
     * Remove the given loader. Does nothing if it's null or was never
     * registered. After a call to this method, that loader instance will not be
     * used in calls to readTexture().
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
     * Read the texture from the given file, functions identically to
     * readTexture(stream).
     * 
     * @param file The File to read a texture from
     * @return The read Texture
     * @throws IOException if the file can't be read, if it's unsupported, etc.
     */
    public static Texture readTexture(File file) throws IOException {
        if (file == null) {
            throw new IOException("Cannot load a texture image from a null file");
        }

        InputStream stream = new FileInputStream(file);
        Texture image;
        try {
            image = readTexture(stream);
        } finally {
            stream.close();
        }

        return image;
    }

    /**
     * Read the texture from the given URL, functions identically to
     * readTexture(stream).
     * 
     * @param url The URL representing the Texture
     * @return The read Texture
     * @throws IOException if the texture couldn't be read, if it's unsupported
     *             or invalid, etc.
     */
    public static Texture readTexture(URL url) throws IOException {
        if (url == null) {
            throw new IOException("Cannot read from a null URL");
        }
        InputStream urlStream = url.openStream();
        Texture image;
        try {
            image = readTexture(urlStream);
        } finally {
            urlStream.close();
        }

        return image;
    }

    /**
     * <p>
     * Read a texture from the given stream. This assumes that the texture
     * begins with the next bytes read from the stream, and that the stream is
     * already opened.
     * </p>
     * <p>
     * For standard images (e.g. jpg, png, gif, etc.) Texture2D is used. If a
     * TextureRectangle is desired, use convertToRectangle().
     * </p>
     * <p>
     * This method does not close the stream, in case it's to be used later on.
     * </p>
     * 
     * @param stream The InputStream to read the texture from
     * @return The read Texture, will be a Texture1D, Texture2D,
     *         TextureCubeMap or Texture3D (2d images use Texture2D by default,
     *         see convertToRectangle())
     * @throws IOException if the stream can't be read from, it represents an
     *             invalid or unsupported texture type, etc.
     */
    public static Texture readTexture(InputStream stream) throws IOException {
        try {
            // make sure we're buffered
            if (!(stream instanceof BufferedInputStream)) {
                stream = new BufferedInputStream(stream);
            }

            // load the file
            Texture t;

            synchronized (loaders) {
                for (int i = loaders.size() - 1; i >= 0; i--) {
                    t = loaders.get(i).readImage(stream);
                    if (t != null)
                    {
                        return t; // we've loaded it
                    }
                }
            }

            throw new IOException("Unable to load the given texture, no registered loader with support");
        } catch (Exception io) {
            if (!(io instanceof IOException)) {
                throw new IOException(io);
            } else {
                throw (IOException) io;
            }
        }
    }

    /**
     * Utility function to convert the given BufferedImage into a Texture with
     * the target of T_1D. This can be a slower operation because it has to
     * redraw the buffered image to make sure it has an appropriate raster and
     * color model.
     * 
     * @param image The BufferedImage to convert into a Texture
     * @return The converted Texture with a T_1D target
     * @throws NullPointerException if image is null
     * @throws IllegalArgumentException if image doesn't have a height of 1
     */
    public static Texture createTexture1D(BufferedImage image) {
        if (image == null) {
            throw new NullPointerException("Cannot convert a null BufferedImage");
        }
        if (image.getHeight() != 1) {
            throw new IllegalArgumentException("A BufferedImage can only be converted to a Texture1D with height == 1, not: "
                    + image.getHeight());
        }

        RasterImage im = new RasterImage(image.getType(), image.getWidth(), 1);

        BufferedImage formatted = new BufferedImage(im.colorModel, im.data, false, null);
        Graphics2D g2 = formatted.createGraphics();
        g2.drawImage(formatted, 0, 0, null);
        g2.dispose();

        BufferData data;
        if (im.type.equals(DataType.UNSIGNED_BYTE)) {
            byte[] rd = ((DataBufferByte) formatted.getRaster().getDataBuffer()).getData();
            data = new BufferData(rd);
        } else { // assumes ShortBuffer
            short[] rd = ((DataBufferUShort) formatted.getRaster().getDataBuffer()).getData();
            data = new BufferData(rd);
        }

        return new Texture(Target.T_1D, new Mipmap(data, image.getWidth(), 1, 1, im.format));
    }

    /**
     * Utility function to convert the given BufferedImage into a Texture with a
     * target of T_2D. This can be a slower operation because it has to redraw
     * the buffered image to make sure it has an appropriate raster and color
     * model. It also flips the converted image to match the coordinate system
     * of Texture (origin at the lower left).
     * 
     * @param image The BufferedImage to convert into a Texture2D
     * @return The converted buffered image as a Texture2D
     * @throws NullPointerException if image is null
     */
    public static Texture createTexture2D(BufferedImage image) {
        if (image == null) {
            throw new NullPointerException("Cannot convert a null BufferedImage");
        }

        RasterImage im = new RasterImage(image.getType(), image.getWidth(), image.getHeight());

        BufferedImage formatted = new BufferedImage(im.colorModel, im.data, false, null);
        Graphics2D g2 = formatted.createGraphics();
        AffineTransform t = AffineTransform.getScaleInstance(1, -1);
        t.concatenate(AffineTransform.getTranslateInstance(0, -image.getHeight()));
        g2.drawImage(image, t, null);
        g2.dispose();

        BufferData data;
        if (im.type.equals(DataType.UNSIGNED_BYTE)) {
            byte[] rd = ((DataBufferByte) formatted.getRaster().getDataBuffer()).getData();
            data = new BufferData(rd);
        } else { // assumes ShortBuffer
            short[] rd = ((DataBufferUShort) formatted.getRaster().getDataBuffer()).getData();
            data = new BufferData(rd);
        }

        return new Texture(Target.T_2D, new Mipmap(data, image.getWidth(), image.getHeight(), 1, im.format));
    }

    /**
     * <p>
     * Like createTexture2D(), except it identifies 6 faces from the cube map as
     * if the cube were unfolded to lie flat on a rectangle (so the rectangular
     * image must have room for 4 cube faces along its width, and 3 faces on its
     * height).
     * </p>
     * The image is laid out like so:
     * <pre>
     * �----�----�----�----�
     * | -- | NZ | -- | -- |
     * �----�----�----�----�
     * | NX | NY | PX | PY |
     * �----�----�----�----�
     * | -- | PZ | -- | -- |
     * �----�----�----�----�
     * </pre>
     * <p>
     * Because of this, the specified image must have an aspect ration of 4/3
     * for the creation to work. Other than this, the creation process functions
     * like createTexture2D().
     * </p>
     * 
     * @param image The BufferedImage to interpret as a cube map
     * @return The converted image as a TextureCubeMap
     * @throws NullPointerException if image is null
     * @throws IllegalArgumentException if image.getWidth() / 4 !=
     *             image.getHeight() / 3
     */
    public static Texture createTextureCubeMap(BufferedImage image) {
        if (image == null) {
            throw new NullPointerException("Cannot create a cube map from a null BufferedImage");
        }

        int side = image.getWidth() / 4;
        if (side * 4 != image.getWidth() || side * 3 != image.getHeight()) {
            throw new IllegalArgumentException("Base image doesn't have the 4x3 aspect ration necessary for a cube map");
        }

        RasterImage im = new RasterImage(image.getType(), side, side);
        BufferedImage formatted = new BufferedImage(im.colorModel, im.data, false, null);

        Mipmap px = createCubeMapFace(image, formatted, Texture.PX, im);
        Mipmap py = createCubeMapFace(image, formatted, Texture.PY, im);
        Mipmap pz = createCubeMapFace(image, formatted, Texture.PZ, im);
        Mipmap nx = createCubeMapFace(image, formatted, Texture.NX, im);
        Mipmap ny = createCubeMapFace(image, formatted, Texture.NY, im);
        Mipmap nz = createCubeMapFace(image, formatted, Texture.NZ, im);

        return new Texture(Target.T_CUBEMAP, new Mipmap[] { px, py, pz, nx, ny, nz });
    }

    /*
     * Internal method that redraws fullImage into faceStore, and then copies
     * out the raster data into a new BufferData. A copy is made since it is
     * assumed that faceStore is re-used for each cube face.
     */
    private static Mipmap createCubeMapFace(BufferedImage fullImage, BufferedImage faceStore,
                                            int face, RasterImage im) {
        Graphics2D g2 = faceStore.createGraphics();
        AffineTransform t = AffineTransform.getScaleInstance(1, 1);

        // setup up a transform that adjusts the fullImage into the correct
        // space for a single face
        switch (face) {
        case Texture.PX:
            t.concatenate(AffineTransform.getTranslateInstance(.5 * faceStore.getWidth(), .5 * faceStore.getHeight()));
            t.concatenate(AffineTransform.getScaleInstance(-1, 1));
            t.concatenate(AffineTransform.getRotateInstance(-Math.PI / 2));
            t.concatenate(AffineTransform.getTranslateInstance(-.5 * faceStore.getWidth(), -.5 * faceStore.getHeight()));
            t.concatenate(AffineTransform.getTranslateInstance(-2 * faceStore.getWidth(), -1 * faceStore.getHeight()));
            break;
        case Texture.PY:
            t.concatenate(AffineTransform.getTranslateInstance(.5 * faceStore.getWidth(), .5 * faceStore.getHeight()));
            t.concatenate(AffineTransform.getScaleInstance(-1, 1));
            t.concatenate(AffineTransform.getRotateInstance(0));
            t.concatenate(AffineTransform.getTranslateInstance(-.5 * faceStore.getWidth(), -.5 * faceStore.getHeight()));
            t.concatenate(AffineTransform.getTranslateInstance(-3 * faceStore.getWidth(), -1 * faceStore.getHeight()));
            break;
        case Texture.PZ:
            t.concatenate(AffineTransform.getTranslateInstance(.5 * faceStore.getWidth(), .5 * faceStore.getHeight()));
            t.concatenate(AffineTransform.getScaleInstance(-1, 1));
            t.concatenate(AffineTransform.getRotateInstance(-Math.PI));
            t.concatenate(AffineTransform.getTranslateInstance(-.5 * faceStore.getWidth(), -.5 * faceStore.getHeight()));
            t.concatenate(AffineTransform.getTranslateInstance(-1 * faceStore.getWidth(), -2 * faceStore.getHeight()));
            break;
        case Texture.NX:
            t.concatenate(AffineTransform.getTranslateInstance(.5 * faceStore.getWidth(), .5 * faceStore.getHeight()));
            t.concatenate(AffineTransform.getScaleInstance(-1, 1));
            t.concatenate(AffineTransform.getRotateInstance(Math.PI / 2));
            t.concatenate(AffineTransform.getTranslateInstance(-.5 * faceStore.getWidth(), -.5 * faceStore.getHeight()));
            t.concatenate(AffineTransform.getTranslateInstance(0 * faceStore.getWidth(), -1 * faceStore.getHeight()));
        case Texture.NY:
            t.concatenate(AffineTransform.getTranslateInstance(.5 * faceStore.getWidth(), .5 * faceStore.getHeight()));
            t.concatenate(AffineTransform.getScaleInstance(1, -1));
            t.concatenate(AffineTransform.getRotateInstance(0));
            t.concatenate(AffineTransform.getTranslateInstance(-.5 * faceStore.getWidth(), -.5 * faceStore.getHeight()));
            t.concatenate(AffineTransform.getTranslateInstance(-1 * faceStore.getWidth(), -1 * faceStore.getHeight()));
            break;
        case Texture.NZ:
            t.concatenate(AffineTransform.getTranslateInstance(.5 * faceStore.getWidth(), .5 * faceStore.getHeight()));
            t.concatenate(AffineTransform.getScaleInstance(1, 1));
            t.concatenate(AffineTransform.getRotateInstance(0));
            t.concatenate(AffineTransform.getTranslateInstance(-.5 * faceStore.getWidth(), -.5 * faceStore.getHeight()));
            t.concatenate(AffineTransform.getTranslateInstance(-1 * faceStore.getWidth(), -0 * faceStore.getHeight()));
            break;
        }

        g2.drawImage(fullImage, t, null);
        g2.dispose();

        BufferData data;
        if (im.type.equals(DataType.UNSIGNED_BYTE)) {
            byte[] rd = ((DataBufferByte) faceStore.getRaster().getDataBuffer()).getData();
            data = new BufferData(rd);
        } else { // assumes ShortBuffer
            short[] rd = ((DataBufferUShort) faceStore.getRaster().getDataBuffer()).getData();
            data = new BufferData(rd);
        }

        return new Mipmap(data, faceStore.getWidth(), faceStore.getHeight(), 1, im.format);
    }

    /*
     * Utility class used to identify a format and type based on the
     * BufferedImage. It also creates a color model and raster suitable for
     * creating new buffered images.
     */
    private static class RasterImage {
        // Ferox specific variables
        TextureFormat format;
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
                colorModel = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB),
                                                     new int[] { 8, 8, 8, 0 }, false, false,
                                                     Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
                data = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, width, height, 3, null);

                type = DataType.UNSIGNED_BYTE;
                format = TextureFormat.RGB;
                break;
            case BufferedImage.TYPE_USHORT_GRAY:
                colorModel = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY),
                                                     new int[] { 16 }, false, false,
                                                     Transparency.OPAQUE, DataBuffer.TYPE_USHORT);
                data = Raster.createInterleavedRaster(DataBuffer.TYPE_USHORT, width, height, 1, null);

                type = DataType.UNSIGNED_SHORT;
                format = TextureFormat.R;
                break;
            case BufferedImage.TYPE_BYTE_GRAY:
                colorModel = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY),
                                                     new int[] { 8 }, false, false,
                                                     Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
                data = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, width, height, 1, null);

                type = DataType.UNSIGNED_BYTE;
                format = TextureFormat.R;
                break;
            default:
                colorModel = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB),
                                                     new int[] { 8, 8, 8, 8 }, true, false,
                                                     Transparency.TRANSLUCENT, DataBuffer.TYPE_BYTE);
                data = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, width, height, 4, null);

                type = DataType.UNSIGNED_BYTE;
                format = TextureFormat.RGBA;
                break;
            }
        }
    }
}
