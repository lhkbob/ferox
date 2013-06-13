package com.ferox.renderer;

/**
 *
 */
public interface Sampler {
    public static enum WrapMode {
        CLAMP,
        CLAMP_TO_BORDER,
        MIRROR,
        REPEAT
    }

    public static enum BaseFormat {
        DEPTH,
        DEPTH_STENCIL,
        R,
        RG,
        RGB,
        RGBA,
        COMPRESSED_RGB,
        COMPRESSED_RGBA
    }

    public static interface RenderTarget {
        public Sampler getSampler();
    }

    public boolean isInterpolated();

    public int getWidth();

    public int getHeight();

    public int getDepth();

    public WrapMode getWrapMode();

    public int getBaseMipmap();

    public int getMaxMipmap();

    public BaseFormat getFormat();

    public DataType getDataType();
}
