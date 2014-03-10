package com.ferox.renderer;

import com.ferox.math.Const;
import com.ferox.math.Vector3;

import java.util.ArrayList;
import java.util.List;

import static com.ferox.renderer.EnvironmentMap.*;

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
        private final Vector3 h;

        public AshikhminIsotropicSpecular(double exp) {
            exponent = exp;
            h = new Vector3();
        }

        @Override
        public double convolve(@Const Vector3 wIn, @Const Vector3 wOut) {
            double ps = wIn.dot(wOut);
            if (ps > 0) {
                // NOTE this does not include the sqrt() of the tangent and bitangent exponents, or division by max(N*L, N*V)
                h.add(wIn, wOut).normalize();
                ps = Math.pow(ps, exponent);
            } else {
                ps = 0; // ignore it
            }
            return ps;
        }
    }

    private final ConvolutionFunction func;
    private final int side;
    private final float[][] data;

    public Convolution(ConvolutionFunction func, int side, float[][] data) {
        this.func = func;
        this.side = side;
        this.data = data;
    }

    public void convolve(float[][] envMap, int side) {
        //        float[][] fullData = new float[6][side * side * 3];

        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            System.out.println("Starting face " + i);
            threads.add(integrateFace(i, data, this.side, envMap, side, func));
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

        // downsample to the requested size
        //        double ratio = side / (double) this.side;
        //        for (int i = 0; i < 6; i++) {
        //            for (int y = 0; y < this.side; y++) {
        //                for (int x = 0; x < this.side; x++) {
        //                    // now iterate over the full image data
        //                    for (double y2 = ratio * y; y2 < (y + 1) * ratio; y2 += 1.0) {
        //                        for (double x2 = ratio * x; x2 < (x + 1) * ratio; x2 += 1.0) {
        //                            // linear interpolation
        //                            double ax = x2 - Math.floor(x2);
        //                            double ay = y2 - Math.floor(y2);
        //                            int o1 = (int) (Math.max(0, Math.min(Math.floor(y2), side - 1)) * side * 3 +
        //                                            Math.max(0, Math.min(Math.floor(x2), side - 1)) * 3);
        //                            int o2 = (int) (Math.max(0, Math.min(Math.floor(y2), side - 1)) * side * 3 +
        //                                            Math.max(0, Math.min(Math.floor(x2) + 1, side - 1)) * 3);
        //                            int o3 = (int) (Math.max(0, Math.min(Math.floor(y2) + 1, side - 1)) * side * 3 +
        //                                            Math.max(0, Math.min(Math.floor(x2), side - 1)) * 3);
        //                            int o4 = (int) (Math.max(0, Math.min(Math.floor(y2) + 1, side - 1)) * side * 3 +
        //                                            Math.max(0, Math.min(Math.floor(x2) + 1, side - 1)) * 3);
        //
        //                            data[i][y * this.side * 3 + x * 3] +=
        //                                    (1.0 - ay) * ((1.0 - ax) * fullData[i][o1] + ax * fullData[i][o2]) +
        //                                    ay * ((1.0 - ax) * fullData[i][o3] + ax * fullData[i][o4]);
        //                            data[i][y * this.side * 3 + x * 3 + 1] += //
        //                                    (1.0 - ay) *
        //                                    ((1.0 - ax) * fullData[i][o1 + 1] + ax * fullData[i][o2 + 1]) +
        //                                    ay * ((1.0 - ax) * fullData[i][o3 + 1] + ax * fullData[i][o4 + 1]);
        //                            data[i][y * this.side * 3 + x * 3 + 2] += //
        //                                    (1.0 - ay) *
        //                                    ((1.0 - ax) * fullData[i][o1 + 2] + ax * fullData[i][o2 + 2]) +
        //                                    ay * ((1.0 - ax) * fullData[i][o3 + 2] + ax * fullData[i][o4 + 2]);
        //
        //                        }
        //                    }
        //
        //                    // average
        //                    data[i][y * this.side * 3 + x * 3] /= (ratio * ratio);
        //                    data[i][y * this.side * 3 + x * 3 + 1] /= (ratio * ratio);
        //                    data[i][y * this.side * 3 + x * 3 + 2] /= (ratio * ratio);
        //                }
        //            }
        //        }

        // blend edges together across cube faces

        // pz -> px boundary
        for (int i = 1; i < this.side - 1; i++) {
            int zoffset = i * this.side * 3 + (this.side - 1) * 3; // right edge of image
            int xoffset = i * this.side * 3; // left edge of image

            float avgR = (data[PZ][zoffset] + data[PX][xoffset]) / 2.0f;
            float avgG = (data[PZ][zoffset + 1] + data[PX][xoffset + 1]) / 2.0f;
            float avgB = (data[PZ][zoffset + 2] + data[PX][xoffset + 2]) / 2.0f;

            data[PZ][zoffset] = data[PX][xoffset] = avgR;
            data[PZ][zoffset + 1] = data[PX][xoffset + 1] = avgG;
            data[PZ][zoffset + 2] = data[PX][xoffset + 2] = avgB;
        }

        // pz -> nx boundary
        for (int i = 1; i < this.side - 1; i++) {
            int zoffset = i * this.side * 3; // left edge of image
            int xoffset = i * this.side * 3 + (this.side - 1) * 3; // right edge of image

            float avgR = (data[PZ][zoffset] + data[NX][xoffset]) / 2.0f;
            float avgG = (data[PZ][zoffset + 1] + data[NX][xoffset + 1]) / 2.0f;
            float avgB = (data[PZ][zoffset + 2] + data[NX][xoffset + 2]) / 2.0f;

            data[PZ][zoffset] = data[NX][xoffset] = avgR;
            data[PZ][zoffset + 1] = data[NX][xoffset + 1] = avgG;
            data[PZ][zoffset + 2] = data[NX][xoffset + 2] = avgB;
        }

        // px -> nz boundary
        for (int i = 1; i < this.side - 1; i++) {
            int zoffset = i * this.side * 3; // left edge of image
            int xoffset = i * this.side * 3 + (this.side - 1) * 3; // right edge of image

            float avgR = (data[NZ][zoffset] + data[PX][xoffset]) / 2.0f;
            float avgG = (data[NZ][zoffset + 1] + data[PX][xoffset + 1]) / 2.0f;
            float avgB = (data[NZ][zoffset + 2] + data[PX][xoffset + 2]) / 2.0f;

            data[NZ][zoffset] = data[PX][xoffset] = avgR;
            data[NZ][zoffset + 1] = data[PX][xoffset + 1] = avgG;
            data[NZ][zoffset + 2] = data[PX][xoffset + 2] = avgB;
        }

        // nz -> nx boundary
        for (int i = 1; i < this.side - 1; i++) {
            int zoffset = i * this.side * 3 + (this.side - 1) * 3; // right edge of image
            int xoffset = i * this.side * 3; // left edge of image

            float avgR = (data[NZ][zoffset] + data[NX][xoffset]) / 2.0f;
            float avgG = (data[NZ][zoffset + 1] + data[NX][xoffset + 1]) / 2.0f;
            float avgB = (data[NZ][zoffset + 2] + data[NX][xoffset + 2]) / 2.0f;

            data[NZ][zoffset] = data[NX][xoffset] = avgR;
            data[NZ][zoffset + 1] = data[NX][xoffset + 1] = avgG;
            data[NZ][zoffset + 2] = data[NX][xoffset + 2] = avgB;
        }

        // pz -> py boundary
        for (int i = 1; i < this.side - 1; i++) {
            int zoffset = i * 3; // top edge of image
            int yoffset = (this.side - 1) * this.side * 3 + i * 3; // bottom edge of image

            float avgR = (data[PZ][zoffset] + data[PY][yoffset]) / 2.0f;
            float avgG = (data[PZ][zoffset + 1] + data[PY][yoffset + 1]) / 2.0f;
            float avgB = (data[PZ][zoffset + 2] + data[PY][yoffset + 2]) / 2.0f;

            data[PZ][zoffset] = data[PY][yoffset] = avgR;
            data[PZ][zoffset + 1] = data[PY][yoffset + 1] = avgG;
            data[PZ][zoffset + 2] = data[PY][yoffset + 2] = avgB;
        }

        // nz -> py boundary
        for (int i = 1; i < this.side - 1; i++) {
            int zoffset = i * 3; // top edge of image
            int yoffset = (this.side - i - 1) * 3; // top edge of image, reversed axis

            float avgR = (data[NZ][zoffset] + data[PY][yoffset]) / 2.0f;
            float avgG = (data[NZ][zoffset + 1] + data[PY][yoffset + 1]) / 2.0f;
            float avgB = (data[NZ][zoffset + 2] + data[PY][yoffset + 2]) / 2.0f;

            data[NZ][zoffset] = data[PY][yoffset] = avgR;
            data[NZ][zoffset + 1] = data[PY][yoffset + 1] = avgG;
            data[NZ][zoffset + 2] = data[PY][yoffset + 2] = avgB;
        }

        // px -> py boundary
        for (int i = 1; i < this.side - 1; i++) {
            int xoffset = i * 3; // top edge of image
            int yoffset = (this.side - i - 1) * this.side * 3 +
                          (this.side - 1) * 3; // right edge of image, reversed axis

            float avgR = (data[PX][xoffset] + data[PY][yoffset]) / 2.0f;
            float avgG = (data[PX][xoffset + 1] + data[PY][yoffset + 1]) / 2.0f;
            float avgB = (data[PX][xoffset + 2] + data[PY][yoffset + 2]) / 2.0f;

            data[PX][xoffset] = data[PY][yoffset] = avgR;
            data[PX][xoffset + 1] = data[PY][yoffset + 1] = avgG;
            data[PX][xoffset + 2] = data[PY][yoffset + 2] = avgB;
        }

        // nx -> py boundary
        for (int i = 1; i < this.side - 1; i++) {
            int xoffset = i * 3; // top edge of image
            int yoffset = i * this.side * 3; // left edge of image

            float avgR = (data[NX][xoffset] + data[PY][yoffset]) / 2.0f;
            float avgG = (data[NX][xoffset + 1] + data[PY][yoffset + 1]) / 2.0f;
            float avgB = (data[NX][xoffset + 2] + data[PY][yoffset + 2]) / 2.0f;

            data[NX][xoffset] = data[PY][yoffset] = avgR;
            data[NX][xoffset + 1] = data[PY][yoffset + 1] = avgG;
            data[NX][xoffset + 2] = data[PY][yoffset + 2] = avgB;
        }

        // pz -> ny boundary
        for (int i = 1; i < this.side - 1; i++) {
            int zoffset = (this.side - 1) * this.side * 3 + i * 3; // bottom edge of image
            int yoffset = i * 3; // top edge of image

            float avgR = (data[PZ][zoffset] + data[NY][yoffset]) / 2.0f;
            float avgG = (data[PZ][zoffset + 1] + data[NY][yoffset + 1]) / 2.0f;
            float avgB = (data[PZ][zoffset + 2] + data[NY][yoffset + 2]) / 2.0f;

            data[PZ][zoffset] = data[NY][yoffset] = avgR;
            data[PZ][zoffset + 1] = data[NY][yoffset + 1] = avgG;
            data[PZ][zoffset + 2] = data[NY][yoffset + 2] = avgB;
        }

        // nz -> ny boundary
        for (int i = 1; i < this.side - 1; i++) {
            int zoffset = (this.side - 1) * this.side * 3 + i * 3; // bottom edge of image
            int yoffset = (this.side - 1) * this.side * 3 +
                          (this.side - i - 1) * 3; // bottom edge of image, reversed axis

            float avgR = (data[NZ][zoffset] + data[NY][yoffset]) / 2.0f;
            float avgG = (data[NZ][zoffset + 1] + data[NY][yoffset + 1]) / 2.0f;
            float avgB = (data[NZ][zoffset + 2] + data[NY][yoffset + 2]) / 2.0f;

            data[NZ][zoffset] = data[NY][yoffset] = avgR;
            data[NZ][zoffset + 1] = data[NY][yoffset + 1] = avgG;
            data[NZ][zoffset + 2] = data[NY][yoffset + 2] = avgB;
        }

        // px -> ny boundary
        for (int i = 1; i < this.side - 1; i++) {
            int xoffset = (this.side - 1) * this.side * 3 + i * 3; // bottom edge of image
            int yoffset = i * this.side * 3 + (this.side - 1) * 3; // right edge of image

            float avgR = (data[PX][xoffset] + data[NY][yoffset]) / 2.0f;
            float avgG = (data[PX][xoffset + 1] + data[NY][yoffset + 1]) / 2.0f;
            float avgB = (data[PX][xoffset + 2] + data[NY][yoffset + 2]) / 2.0f;

            data[PX][xoffset] = data[NY][yoffset] = avgR;
            data[PX][xoffset + 1] = data[NY][yoffset + 1] = avgG;
            data[PX][xoffset + 2] = data[NY][yoffset + 2] = avgB;
        }

        // nx -> ny boundary
        for (int i = 1; i < this.side - 1; i++) {
            int xoffset = (this.side - 1) * this.side * 3 + i * 3; // bottom edge of image
            int yoffset = (this.side - i - 1) * this.side * 3; // left edge of image, reversed axis

            float avgR = (data[NX][xoffset] + data[NY][yoffset]) / 2.0f;
            float avgG = (data[NX][xoffset + 1] + data[NY][yoffset + 1]) / 2.0f;
            float avgB = (data[NX][xoffset + 2] + data[NY][yoffset + 2]) / 2.0f;

            data[NX][xoffset] = data[NY][yoffset] = avgR;
            data[NX][xoffset + 1] = data[NY][yoffset + 1] = avgG;
            data[NX][xoffset + 2] = data[NY][yoffset + 2] = avgB;
        }

        // 8 corners get 3 way blending

        // px, py, pz
        {
            int xoffset = 0; // top left corner of image
            int yoffset = (this.side - 1) * this.side * 3 + (this.side - 1) * 3; // bottom-right of image
            int zoffset = (this.side - 1) * 3; // top right of image

            float avgR = (data[PX][xoffset] + data[PY][yoffset] + data[PZ][zoffset]) / 3.0f;
            float avgG = (data[PX][xoffset + 1] + data[PY][yoffset + 1] + data[PZ][zoffset + 1]) / 3.0f;
            float avgB = (data[PX][xoffset + 2] + data[PY][yoffset + 2] + data[PZ][zoffset + 2]) / 3.0f;

            data[PX][xoffset] = data[PY][yoffset] = data[PZ][zoffset] = avgR;
            data[PX][xoffset + 1] = data[PY][yoffset + 1] = data[PZ][zoffset + 1] = avgG;
            data[PX][xoffset + 2] = data[PY][yoffset + 2] = data[PZ][zoffset + 2] = avgB;
        }

        // nx, py, pz
        {
            int xoffset = (this.side - 1) * 3; // top right corner of image
            int yoffset = (this.side - 1) * this.side * 3; // bottom left of image
            int zoffset = 0; // top left of image

            float avgR = (data[NX][xoffset] + data[PY][yoffset] + data[PZ][zoffset]) / 3.0f;
            float avgG = (data[NX][xoffset + 1] + data[PY][yoffset + 1] + data[PZ][zoffset + 1]) / 3.0f;
            float avgB = (data[NX][xoffset + 2] + data[PY][yoffset + 2] + data[PZ][zoffset + 2]) / 3.0f;

            data[NX][xoffset] = data[PY][yoffset] = data[PZ][zoffset] = avgR;
            data[NX][xoffset + 1] = data[PY][yoffset + 1] = data[PZ][zoffset + 1] = avgG;
            data[NX][xoffset + 2] = data[PY][yoffset + 2] = data[PZ][zoffset + 2] = avgB;
        }

        // px, py, nz
        {
            int xoffset = (this.side - 1) * 3; // top right corner of image
            int yoffset = (this.side - 1) * 3; // top right of image
            int zoffset = 0; // top left of image

            float avgR = (data[PX][xoffset] + data[PY][yoffset] + data[NZ][zoffset]) / 3.0f;
            float avgG = (data[PX][xoffset + 1] + data[PY][yoffset + 1] + data[NZ][zoffset + 1]) / 3.0f;
            float avgB = (data[PX][xoffset + 2] + data[PY][yoffset + 2] + data[NZ][zoffset + 2]) / 3.0f;

            data[PX][xoffset] = data[PY][yoffset] = data[NZ][zoffset] = avgR;
            data[PX][xoffset + 1] = data[PY][yoffset + 1] = data[NZ][zoffset + 1] = avgG;
            data[PX][xoffset + 2] = data[PY][yoffset + 2] = data[NZ][zoffset + 2] = avgB;
        }

        // nx, py, nz
        {
            int xoffset = 0; // top left corner of image
            int yoffset = 0; // top left of image
            int zoffset = (this.side - 1) * 3; // top right of image

            float avgR = (data[NX][xoffset] + data[PY][yoffset] + data[NZ][zoffset]) / 3.0f;
            float avgG = (data[NX][xoffset + 1] + data[PY][yoffset + 1] + data[NZ][zoffset + 1]) / 3.0f;
            float avgB = (data[NX][xoffset + 2] + data[PY][yoffset + 2] + data[NZ][zoffset + 2]) / 3.0f;

            data[NX][xoffset] = data[PY][yoffset] = data[NZ][zoffset] = avgR;
            data[NX][xoffset + 1] = data[PY][yoffset + 1] = data[NZ][zoffset + 1] = avgG;
            data[NX][xoffset + 2] = data[PY][yoffset + 2] = data[NZ][zoffset + 2] = avgB;
        }

        // px, ny, pz
        {
            int xoffset = (this.side - 1) * this.side * 3; // bottom left corner of image
            int yoffset = (this.side - 1) * 3; // top right of image
            int zoffset = (this.side - 1) * this.side * 3 + (this.side - 1) * 3; // bottom right of image

            float avgR = (data[PX][xoffset] + data[NY][yoffset] + data[PZ][zoffset]) / 3.0f;
            float avgG = (data[PX][xoffset + 1] + data[NY][yoffset + 1] + data[PZ][zoffset + 1]) / 3.0f;
            float avgB = (data[PX][xoffset + 2] + data[NY][yoffset + 2] + data[PZ][zoffset + 2]) / 3.0f;

            data[PX][xoffset] = data[NY][yoffset] = data[PZ][zoffset] = avgR;
            data[PX][xoffset + 1] = data[NY][yoffset + 1] = data[PZ][zoffset + 1] = avgG;
            data[PX][xoffset + 2] = data[NY][yoffset + 2] = data[PZ][zoffset + 2] = avgB;
        }

        // nx, ny, pz
        {
            int xoffset =
                    (this.side - 1) * this.side * 3 + (this.side - 1) * 3; // bottom right corner of image
            int yoffset = 0; // top left of image
            int zoffset = (this.side - 1) * this.side * 3; // bottom left of image

            float avgR = (data[NX][xoffset] + data[NY][yoffset] + data[PZ][zoffset]) / 3.0f;
            float avgG = (data[NX][xoffset + 1] + data[NY][yoffset + 1] + data[PZ][zoffset + 1]) / 3.0f;
            float avgB = (data[NX][xoffset + 2] + data[NY][yoffset + 2] + data[PZ][zoffset + 2]) / 3.0f;

            data[NX][xoffset] = data[NY][yoffset] = data[PZ][zoffset] = avgR;
            data[NX][xoffset + 1] = data[NY][yoffset + 1] = data[PZ][zoffset + 1] = avgG;
            data[NX][xoffset + 2] = data[NY][yoffset + 2] = data[PZ][zoffset + 2] = avgB;
        }

        // px, ny, nz
        {
            int xoffset =
                    (this.side - 1) * this.side * 3 + (this.side - 1) * 3; // bottom right corner of image
            int yoffset = (this.side - 1) * this.side * 3 + (this.side - 1) * 3; // bottom right of image
            int zoffset = (this.side - 1) * this.side * 3; // bottom left of image

            float avgR = (data[PX][xoffset] + data[NY][yoffset] + data[NZ][zoffset]) / 3.0f;
            float avgG = (data[PX][xoffset + 1] + data[NY][yoffset + 1] + data[NZ][zoffset + 1]) / 3.0f;
            float avgB = (data[PX][xoffset + 2] + data[NY][yoffset + 2] + data[NZ][zoffset + 2]) / 3.0f;

            data[PX][xoffset] = data[NY][yoffset] = data[NZ][zoffset] = avgR;
            data[PX][xoffset + 1] = data[NY][yoffset + 1] = data[NZ][zoffset + 1] = avgG;
            data[PX][xoffset + 2] = data[NY][yoffset + 2] = data[NZ][zoffset + 2] = avgB;
        }

        // nx, ny, nz
        {
            int xoffset = (this.side - 1) * this.side * 3; // bottom left corner of image
            int yoffset = (this.side - 1) * this.side * 3; // bottom left of image
            int zoffset = (this.side - 1) * this.side * 3 + (this.side - 1) * 3; // bottom right of image

            float avgR = (data[NX][xoffset] + data[NY][yoffset] + data[NZ][zoffset]) / 3.0f;
            float avgG = (data[NX][xoffset + 1] + data[NY][yoffset + 1] + data[NZ][zoffset + 1]) / 3.0f;
            float avgB = (data[NX][xoffset + 2] + data[NY][yoffset + 2] + data[NZ][zoffset + 2]) / 3.0f;

            data[NX][xoffset] = data[NY][yoffset] = data[NZ][zoffset] = avgR;
            data[NX][xoffset + 1] = data[NY][yoffset + 1] = data[NZ][zoffset + 1] = avgG;
            data[NX][xoffset + 2] = data[NY][yoffset + 2] = data[NZ][zoffset + 2] = avgB;
        }
    }

    private static Thread integrateFace(final int face, final float[][] data, final int dataSide,
                                        final float[][] envData, final int envSide,
                                        final ConvolutionFunction func) {
        Thread t = new Thread("integrate-face-" + face) {
            @Override
            public void run() {
                Vector3 wIn = new Vector3();
                for (int y = 0; y < dataSide; y++) {
                    for (int x = 0; x < dataSide; x++) {
                        EnvironmentMap.toVector(face, x, y, dataSide, dataSide, wIn);
                        Vector3 value = convolveTexel(wIn, envData, envSide, func);
                        value.get(data[face], 3 * y * dataSide + 3 * x);
                    }
                }
            }
        };
        t.start();
        return t;
    }

    private static Vector3 convolveTexel(@Const Vector3 wIn, float[][] envMap, int side,
                                         ConvolutionFunction func) {
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
