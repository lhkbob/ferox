package com.ferox.resource.builder;

import com.ferox.math.ColorRGB;
import com.ferox.math.Const;
import com.ferox.math.Vector4;

/**
 *
 */
public interface TextureBuilder<T extends TextureBuilder<T>> extends SamplerBuilder<T> {

    public T anisotropy(double v);

    public T borderColor(@Const ColorRGB color);

    public T borderColor(@Const Vector4 color);

    public static interface BasicColorData<I> {
        public I fromUnsignedNormalized(byte[] data);

        public I fromUnsignedNormalized(short[] data);

        public I fromUnsignedNormalized(int[] data);

        public I fromUnsigned(byte[] data);

        public I fromUnsigned(short[] data);

        public I fromUnsigned(int[] data);

        public I from(float[] data);

        public I fromHalfFloats(short[] data);
    }

    public static interface RGBData<I> extends BasicColorData<I> {
        public I fromPackedFloats(int[] data);
    }

    public static interface CompressedRGBData<I> extends RGBData<I> {
        public I fromDXT1(byte[] data);
    }

    public static interface CompressedRGBAData<I> extends BasicColorData<I> {
        public I fromDXT1(byte[] data);

        public I fromDXT3(byte[] data);

        public I fromDXT5(byte[] data);
    }

    public static interface ARGBData<I> {
        public I from(byte[] data);

        public I fromPackedBytes(int[] data);
    }
}
