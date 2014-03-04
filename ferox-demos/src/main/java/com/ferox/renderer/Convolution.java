package com.ferox.renderer;

import com.ferox.math.Const;
import com.ferox.math.Vector3;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class Convolution {
    public static interface ConvolutionFunction {
        public double convolve(@Const Vector3 wIn, @Const Vector3 wOut);
    }

    // wIn is the normal vector
    public static class AshikhminDiffuse implements ConvolutionFunction {
        @Override
        public double convolve(@Const Vector3 wIn, @Const Vector3 wOut) {
            double pd = wIn.dot(wOut);
            if (pd > 0) {
                // NOTE this does not include the (1 - vn/2)^5 term that depends on the viewing angle to normal
                pd = pd * (28.0 / (23.0 * Math.PI) * (1.0 - Math.pow(1.0 - pd / 2.0, 5.0)));
            } else {
                pd = 0; // ignore it
            }
            return pd;
        }
    }

    // wIn is the reflection vector
    public static class AshikhminIsotropicSpecular implements ConvolutionFunction {
        private final double exponent;

        public AshikhminIsotropicSpecular(double exp) {
            exponent = exp;
        }

        @Override
        public double convolve(@Const Vector3 wIn, @Const Vector3 wOut) {
            double ps = wIn.dot(wOut);
            if (ps > 0) {
                // NOTE this does not include the sqrt() of the tangent and bitangent exponents, or
                // the division by (N*V).
                // FIXME this does not approximate fresnel or the (H*L) division
                ps = Math.pow(ps, exponent);
            } else {
                ps = 0; // ignore it
            }
            return ps;
        }
    }

    private final ConvolutionFunction func;
    private final int side;

    public Convolution(ConvolutionFunction func, int side) {
        this.func = func;
        this.side = side;
    }

    public float[][] convolve(float[][] envMap, int side) {
        float[][] k = new float[6][this.side * this.side * 3];

        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            System.out.println("Starting face " + i);
            threads.add(integrateFace(i, k, envMap, side));
        }

        while (!threads.isEmpty()) {
            Thread t = threads.get(threads.size() - 1);
            try {
                t.join();
            } catch (InterruptedException e) {
                // do nothing
            }
            if (!t.isAlive()) {
                System.out.println("Face " + (threads.size() - 1) + " completed");
                threads.remove(threads.size() - 1);
            }
        }
        return k;
    }

    private Thread integrateFace(final int face, final float[][] convolutionStore, final float[][] envData,
                                 final int envSide) {
        Thread t = new Thread("integrate-face-" + face) {
            @Override
            public void run() {
                Vector3 wIn = new Vector3();
                for (int y = 0; y < side; y++) {
                    for (int x = 0; x < side; x++) {
                        EnvironmentMap.toVector(face, x, y, side, side, wIn);
                        Vector3 value = convolveTexel(wIn, envData, envSide);
                        value.get(convolutionStore[face], 3 * y * side + 3 * x);
                    }
                }
            }
        };
        t.start();
        return t;
    }

    private Vector3 convolveTexel(@Const Vector3 wIn, float[][] envMap, int side) {
        Vector3 wOut = new Vector3();
        Vector3 color = new Vector3();
        Vector3 total = new Vector3();

        for (int i = 0; i < 6; i++) {
            for (int y = 0; y < side; y++) {
                for (int x = 0; x < side; x++) {
                    EnvironmentMap.toVector(i, x, y, side, side, wOut);
                    double weight = func.convolve(wIn, wOut);
                    if (weight > 0) {
                        color.set(envMap[i], y * side * 3 + x * 3);
                        total.addScaled(weight * EnvironmentMap.texelCoordSolidAngle(x, y, side), color);
                    }
                }
            }
        }
        return total;
    }
}
