package com.ferox.math;

/**
 * Functions is a static collection of functions that provide additional or more
 * performant alternatives to {@link Math}.
 * 
 * @author Michael Ludwig
 */
public final class Functions {
    private Functions() {}
    
    /**
     * <p>
     * Compute the smallest power-of-two that is greater than or equal to the
     * given integer. If num is less than or equal to 0, 1 is always returned.
     * If num is already a power-of-two, num is returned.
     * </p>
     * <p>
     * This runs in constant time.
     * </p>
     * 
     * @param num The input
     * @return Smallest power-of-two greater than or equal to num
     */
    public static int potCeil(int num) {
        if (num <= 0)
            return 1;
        
        num--;
        num |= (num >> 1);
        num |= (num >> 2);
        num |= (num >> 4);
        num |= (num >> 8);
        num |= (num >> 16);
        num++;
        
        return num;
    }
    
    /**
     * Compute the integer log base 2 of the given number. If num is not a power
     * of two (i.e. its log base 2 is not an integral value), this effectively
     * returns <code>log2(potCeil(num))</code>. If num is less than or equal to
     * 0, results are undefined.
     * 
     * @param num The number to compute the log base 2 of
     * @return The log base 2 of the number, after rounding up to the nearest
     *         power of two
     */
    public static int log2(int num) {
        return 32 - Integer.numberOfLeadingZeros(num - 1);
    }
    
    /**
     * Return true if the given integer is a power of two. This is an efficient,
     * constant time implementation. Numbers less than or equal to 0 will always
     * return false.
     * 
     * @param num The number to check
     * @return True if num is a power of two
     */
    public static boolean isPowerOfTwo(int num) {
        if (num <= 0)
            return false;
        return (num & (num - 1)) == 0;
    }
}
