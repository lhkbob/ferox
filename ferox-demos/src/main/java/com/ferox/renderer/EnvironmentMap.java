package com.ferox.renderer;

import com.ferox.math.Const;
import com.ferox.math.Vector3;
import com.ferox.renderer.builder.CubeImageData;
import com.ferox.renderer.builder.TextureBuilder;
import com.ferox.renderer.builder.TextureCubeMapBuilder;
import com.ferox.renderer.loader.RadianceImageLoader;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 */
public class EnvironmentMap {
    private static final int DIR_SIDE = 32;

    private final RadianceImageLoader.Image cross;
    private final int side;
    private final float[][] env;

    private final float[][] diff;

    private final List<Vector3> samples;

    public EnvironmentMap(File verticalCrossHDR) throws IOException {
        try (BufferedInputStream stream = new BufferedInputStream(new FileInputStream(verticalCrossHDR))) {
            cross = new RadianceImageLoader().read(stream);
        }

        side = cross.width / 3;
        env = new float[6][side * side * 3];

        diff = new float[6][DIR_SIDE * DIR_SIDE * 3];
        samples = new ArrayList<>();

        convertCross();
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < env[i].length; j++) {
                env[i][j] /= 10000.0; // FIXME WHY???? is it 256*256? definitely not 256*256*6, is it 32 * 32 * 6?
            }
        }
        computeDiffuseIrradiance();
        computeSamples();
    }

    private void computeSamples() {
        List<Sample> withIntensity = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            for (int y = 0; y < side; y++) {
                for (int x = 0; x < side; x++) {
                    Vector3 n = new Vector3();
                    n.set(env[i], 3 * y * side + 3 * x);
                    double intensity = n.length();
                    toVector(i, x, y, side, side, n);
                    withIntensity.add(new Sample(n, intensity));
                }
            }
        }

        Collections.sort(withIntensity);
        System.out.println(withIntensity.get(0).intensity);
        System.out.println(withIntensity.get(withIntensity.size() - 1).intensity);
        for (Sample s : withIntensity) {
            samples.add(s.direction);
        }
    }

    public List<Vector3> getSamples() {
        return samples;
    }

    public TextureCubeMap createEnvironmentMap(Framework framework) {
        TextureCubeMapBuilder cmb = framework.newTextureCubeMap();
        cmb.side(side).interpolated();
        CubeImageData<? extends TextureBuilder.BasicColorData> data = cmb.rgb();
        data.positiveX(0).from(env[0]);
        data.positiveY(0).from(env[1]);
        data.positiveZ(0).from(env[2]);
        data.negativeX(0).from(env[3]);
        data.negativeY(0).from(env[4]);
        data.negativeZ(0).from(env[5]);
        return cmb.build();
    }

    public TextureCubeMap createDiffuseMap(Framework framework) {
        TextureCubeMapBuilder cmb = framework.newTextureCubeMap();
        cmb.side(DIR_SIDE).interpolated();
        CubeImageData<? extends TextureBuilder.BasicColorData> data = cmb.rgb();
        data.positiveX(0).from(diff[0]);
        data.positiveY(0).from(diff[1]);
        data.positiveZ(0).from(diff[2]);
        data.negativeX(0).from(diff[3]);
        data.negativeY(0).from(diff[4]);
        data.negativeZ(0).from(diff[5]);
        return cmb.build();
    }

    private void computeDiffuseIrradiance() {
        maxIrradiance = 0.0f;
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            System.out.println("Starting face " + i);
            threads.add(integrateFace(i));
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

        System.out.println("Tonemapping, max irradiance = " + maxIrradiance);
        //        for (int i = 0; i < 6; i++) {
        //            float[] d = diff[i];
        //            for (int j = 0; j < d.length; j++) {
        //                d[j] = d[j] / maxIrradiance;
        //            }
        //        }
    }

    private static class Sample implements Comparable<Sample> {
        private final Vector3 direction;
        private final double intensity;

        public Sample(Vector3 direction, double intensity) {
            this.direction = direction;
            this.intensity = intensity;
        }

        @Override
        public int compareTo(Sample o) {
            return Double.compare(intensity, o.intensity);
        }
    }

    private float maxIrradiance = 0.0f;

    private Thread integrateFace(final int face) {
        Thread t = new Thread("integrate-face-" + face) {
            @Override
            public void run() {
                Vector3 normal = new Vector3();
                float faceMax = 0;
                for (int y = 0; y < DIR_SIDE; y++) {
                    for (int x = 0; x < DIR_SIDE; x++) {
                        toVector(face, x, y, DIR_SIDE, DIR_SIDE, normal);
                        Vector3 irradiance = integrate(normal);
                        irradiance.get(diff[face], 3 * y * DIR_SIDE + 3 * x);

                        float irradLen = (float) irradiance.length();
                        if (irradLen > faceMax) {
                            faceMax = irradLen;
                        }
                    }
                }

                synchronized (EnvironmentMap.this) {
                    if (faceMax > maxIrradiance) {
                        maxIrradiance = faceMax;
                    }
                }
            }
        };
        t.start();
        return t;
    }

    private Vector3 integrate(@Const Vector3 normal) {
        Vector3 light = new Vector3();
        Vector3 color = new Vector3();
        Vector3 irradiance = new Vector3();
        for (int i = 0; i < 6; i++) {
            for (int y = 0; y < side; y++) {
                for (int x = 0; x < side; x++) {
                    color.set(env[i], 3 * y * side + 3 * x);//.scale(1.0 / 393216);
                    toVector(i, x, y, side, side, light);
                    double pd = normal.dot(light);
                    if (pd > 0) {
                        // NOTE this does not include the (1 - vn/2)^5 term that depends on the viewing angle to normal
                        pd = 28.0 / (23.0 * Math.PI) * (1.0 - Math.pow(1.0 - pd / 2.0, 5.0));
                        irradiance.addScaled(pd, color);
                    }
                }
            }
        }
        return irradiance;
    }

    public static void toVector(int face, int tx, int ty, int w, int h, Vector3 v) {
        // technically these are sc / |ma| and tc / |ma| but we assert
        // that |ma| = 1
        float sc = 2f * tx / (float) w - 1f;
        float tc = 2f * ty / (float) h - 1f;
        switch (face) {
        case 0: // px
            v.set(1.0, -tc, -sc);
            break;
        case 1: // py
            v.set(sc, 1.0, tc);
            break;
        case 2: // pz
            v.set(sc, -tc, 1.0);
            break;
        case 3: // nx
            v.set(-1.0, -tc, sc);
            break;
        case 4: // ny
            v.set(sc, -1.0, -tc);
            break;
        case 5: // nz
            v.set(-sc, -tc, -1.0);
            break;
        }
        v.normalize();
    }

    private void convertCross() {
        // px
        for (int y = 0; y < side; y++) {
            for (int x = 0; x < side; x++) {
                int crossX = 3 * side - x - 1;
                int crossY = 3 * side - y - 1;

                int faceOffset = 3 * y * side + 3 * x;
                int crossOffset = 3 * crossY * cross.width + 3 * crossX;
                env[0][faceOffset] = cross.data[crossOffset];
                env[0][faceOffset + 1] = cross.data[crossOffset + 1];
                env[0][faceOffset + 2] = cross.data[crossOffset + 2];
            }
        }

        // py
        for (int y = 0; y < side; y++) {
            for (int x = 0; x < side; x++) {
                int crossX = side + x; // shifted over 1 column
                int crossY = 3 * side + y; // flip y axis

                int faceOffset = 3 * y * side + 3 * x;
                int crossOffset = 3 * crossY * cross.width + 3 * crossX;
                env[1][faceOffset] = cross.data[crossOffset];
                env[1][faceOffset + 1] = cross.data[crossOffset + 1];
                env[1][faceOffset + 2] = cross.data[crossOffset + 2];
            }
        }

        // pz
        for (int y = 0; y < side; y++) {
            for (int x = 0; x < side; x++) {
                int crossX = side + x;
                int crossY = y;

                int faceOffset = 3 * y * side + 3 * x;
                int crossOffset = 3 * crossY * cross.width + 3 * crossX;
                env[2][faceOffset] = cross.data[crossOffset];
                env[2][faceOffset + 1] = cross.data[crossOffset + 1];
                env[2][faceOffset + 2] = cross.data[crossOffset + 2];
            }
        }

        // nx
        for (int y = 0; y < side; y++) {
            for (int x = 0; x < side; x++) {
                int crossX = side - x - 1;
                int crossY = 3 * side - y - 1;

                int faceOffset = 3 * y * side + 3 * x;
                int crossOffset = 3 * crossY * cross.width + 3 * crossX;
                env[3][faceOffset] = cross.data[crossOffset];
                env[3][faceOffset + 1] = cross.data[crossOffset + 1];
                env[3][faceOffset + 2] = cross.data[crossOffset + 2];
            }
        }

        // ny
        for (int y = 0; y < side; y++) {
            for (int x = 0; x < side; x++) {
                int crossX = 2 * side - x - 1;
                int crossY = 2 * side - y - 1;

                int faceOffset = 3 * y * side + 3 * x;
                int crossOffset = 3 * crossY * cross.width + 3 * crossX;
                env[4][faceOffset] = cross.data[crossOffset];
                env[4][faceOffset + 1] = cross.data[crossOffset + 1];
                env[4][faceOffset + 2] = cross.data[crossOffset + 2];
            }
        }

        // nz
        for (int y = 0; y < side; y++) {
            for (int x = 0; x < side; x++) {
                int crossX = 2 * side - x - 1;
                int crossY = 3 * side - y - 1;

                int faceOffset = 3 * y * side + 3 * x;
                int crossOffset = 3 * crossY * cross.width + 3 * crossX;
                env[5][faceOffset] = cross.data[crossOffset];
                env[5][faceOffset + 1] = cross.data[crossOffset + 1];
                env[5][faceOffset + 2] = cross.data[crossOffset + 2];
            }
        }
    }
}
