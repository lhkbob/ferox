package com.ferox.renderer;

import java.util.Arrays;

/**
 *
 */
public class Hammersley {
    public static void main(String[] args) {
        System.out.println(Arrays.toString(compute(3)));
    }

    public static double[] compute(int n) {
        double[] result = new double[n * 2];
        double p, u, v;
        int k, kk, pos;

        for (k = 0, pos = 0; k < n; k++) {
            u = 0.0;
            for (p = 0.5, kk = k; kk > 0; p *= 0.5, kk >>= 1) {
                if ((kk & 1) != 0) {
                    u += p;
                }
            }
            v = (k + 0.5) / n;
            result[pos++] = u;
            result[pos++] = v;
        }

        return result;
    }
}
