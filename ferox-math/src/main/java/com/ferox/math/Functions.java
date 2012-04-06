package com.ferox.math;

public final class Functions {
    private Functions() {}
    
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
    
    public static int log2(int num) {
        return 32 - Integer.numberOfLeadingZeros(num - 1);
    }
    
    public static boolean isPowerOfTwo(int num) {
        if (num <= 0)
            return false;
        return (num & (num - 1)) == 0;
    }
    
    public static void main(String[] args) {
        for (int i = -10; i <= 100; i++) {
            int power2 = potCeil(i);
            int log2Orig = log2(i);
            int log2power = log2(power2);
            boolean isPower2Orig = isPowerOfTwo(i);
            boolean isPower2Power = isPowerOfTwo(power2);
            
            System.out.println(i + "(" + isPower2Orig + ") -> " + power2 + "(" + isPower2Power + "), log2: " + log2Orig + " -> " + log2power);
        }
    }
}
