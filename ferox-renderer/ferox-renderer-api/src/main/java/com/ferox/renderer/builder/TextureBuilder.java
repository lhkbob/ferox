package com.ferox.renderer.builder;

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

    public static interface BasicColorData {
        public void fromUnsignedNormalized(byte[] data);

        public void fromUnsignedNormalized(short[] data);

        public void fromUnsignedNormalized(int[] data);

        public void fromUnsigned(byte[] data);

        public void fromUnsigned(short[] data);

        public void fromUnsigned(int[] data);

        public void from(float[] data);

        public void from(byte[] data);

        public void from(short[] data);

        public void from(int[] data);

        public void fromHalfFloats(short[] data);
    }

    public static interface RGBData extends BasicColorData {
        public void fromPackedFloats(int[] data);
    }

    public static interface CompressedRGBData extends RGBData {
        public void fromDXT1(byte[] data);
    }

    public static interface CompressedRGBAData extends BasicColorData {
        public void fromDXT1(byte[] data);

        public void fromDXT3(byte[] data);

        public void fromDXT5(byte[] data);
    }

    public static interface ARGBData {
        public void from(byte[] data);

        public void fromPackedBytes(int[] data);
    }
}
