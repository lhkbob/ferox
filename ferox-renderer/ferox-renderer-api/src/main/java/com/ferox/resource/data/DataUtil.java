package com.ferox.resource.data;

/**
 *
 */
public final class DataUtil {
    public static final long INT_MASK = 0xffffffffL;
    public static final long SHORT_MASK = 0xffffL;
    public static final long BYTE_MASK = 0xffL;

    public static final long MAX_UNSIGNED_INT = 4294967295L;
    public static final long MAX_UNSIGNED_SHORT = 65535L;
    public static final long MAX_UNSIGNED_BYTE = 255L;

    public static final int MAX_DEPTH_INT_VALUE = 16777215;

    public static long unsignedIntToLong(int v) {
        return (INT_MASK & (long) v);
    }

    public static long unsignedShortToLong(short v) {
        return (SHORT_MASK & v);
    }

    public static long unsignedByteToLong(byte b) {
        return (BYTE_MASK & b);
    }

    public static int longToUnsignedInt(long v) {
        return (int) (INT_MASK & v);
    }

    public static short longToUnsignedShort(long v) {
        return (short) (SHORT_MASK & v);
    }

    public static byte longToUnsignedByte(long v) {
        return (byte) (BYTE_MASK & v);
    }

    public static double normalizeUnsignedInt(int v) {
        return unsignedIntToLong(v) / (double) MAX_UNSIGNED_INT;
    }

    public static double normalizeUnsignedShort(short v) {
        return unsignedShortToLong(v) / (double) MAX_UNSIGNED_SHORT;
    }

    public static double normalizeUnsignedByte(byte v) {
        return unsignedByteToLong(v) / (double) MAX_UNSIGNED_BYTE;
    }

    public static int unnormalizeUnsignedInt(double v) {
        return longToUnsignedInt((long) (v * MAX_UNSIGNED_INT));
    }

    public static short unnormalizeUnsignedShort(double v) {
        return longToUnsignedShort((long) (v * MAX_UNSIGNED_SHORT));
    }

    public static byte unnormalizeUnsignedByte(double v) {
        return longToUnsignedByte((long) (v * MAX_UNSIGNED_BYTE));
    }

    public static double normalizeByte(byte v) {
        return (2.0 * v + 1.0) / (double) MAX_UNSIGNED_BYTE;
    }

    public static double normalizeShort(short v) {
        return (2.0 * v + 1.0) / (double) MAX_UNSIGNED_SHORT;
    }

    public static double normalizeInt(int v) {
        return (2.0 * v + 1.0) / (double) MAX_UNSIGNED_INT;
    }

    public static byte unnormalizeByte(double v) {
        return (byte) ((MAX_UNSIGNED_BYTE * v - 1.0) / 2.0);
    }

    public static short unnormalizeShort(double v) {
        return (short) ((MAX_UNSIGNED_SHORT * v - 1.0) / 2.0);
    }

    public static int unnormalizeInt(double v) {
        return (int) ((MAX_UNSIGNED_INT * v - 1.0) / 2.0);
    }

    public static byte getWord(int value, int word) {
        // word = 0 -> shift = 24
        // word = 1 -> shift = 16
        // word = 2 -> shift = 8
        // word = 3 -> shift  = 0
        // Thus, this is big endian, where the MSB is word 0
        int shift = (3 - word) << 3;
        return (byte) ((value >> shift) & 0xff);
    }

    public static int setWord(int value, int word, byte wordValue) {
        int shift = (3 - word) << 3;
        return value | (wordValue << shift);
    }

    public static double getDepth(int depthStencil) {
        // 24 bits of depth are stored in the 3 most sig. bytes
        int unsignedDepth = (depthStencil >> 8) & 0xffffff;
        return unsignedDepth / (double) MAX_DEPTH_INT_VALUE;
    }

    public static int setDepth(int depthStencil, double newDepth) {
        int unsignedDepth = (int) (newDepth * MAX_DEPTH_INT_VALUE);
        return depthStencil | (unsignedDepth << 8);
    }

    public static byte getStencil(int depthStencil) {
        // 8 bits of stencil are stored in the LSB
        return getWord(depthStencil, 3);
    }

    public static int setStencil(int depthStencil, byte newStencil) {
        return setWord(depthStencil, 3, newStencil);
    }

    public static double getPackedFloatR(int rgb) {
        // red is stored in bits 0 - 10
        int mantissa = (0x3f & rgb) & 0x7ff; // mantissa is right 6 bits
        int exponent = ((0x7c0 & rgb) >> 5) & 0x7ff; // mantissa is left 5 bits

        if (exponent == 0) {
            if (mantissa == 0) {
                return 0.0;
            } else {
                return 2e-14 * mantissa / 64.0;
            }
        } else if (exponent == 31) {
            if (mantissa == 0) {
                return Double.POSITIVE_INFINITY;
            } else {
                return Double.NaN;
            }
        } else {
            return Math.pow(2.0, exponent - 15.0) * (1.0 + mantissa / 64.0);
        }
    }

    public static int setPackedFloatR(int rgb, double newValue) {

    }

    public static double getPackedFloatG(int rgb) {
        // green is stored in bits 11 - 21
    }

    public static int setPackedFloatG(int rgb, double newValue) {

    }

    public static double getPackedFloatB(int rgb) {
        // blue is stored in bits 22 - 31
    }

    public static int setPackedFloatB(int rgb, double newValue) {

    }
}
