package com.ferox.resource;

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

    public boolean isInterpolated();

    public int getWidth();

    public int getHeight();

    public int getDepth();

    // FIXME this might change
    public int getRenderTargets();

    public WrapMode getWrapMode();

    public int getBaseMipmap();

    public int getMaxMipmap();

    public BaseFormat getFormat();

    public DataType getDataType();
}
