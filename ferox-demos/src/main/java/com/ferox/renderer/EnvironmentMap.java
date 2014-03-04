/*
 * Ferox, a graphics and game library in Java
 *
 * Copyright (c) 2012, Michael Ludwig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright notice,
 *         this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright notice,
 *         this list of conditions and the following disclaimer in the
 *         documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.ferox.renderer;

import com.ferox.math.Const;
import com.ferox.math.Vector3;
import com.ferox.renderer.builder.CubeImageData;
import com.ferox.renderer.builder.TextureBuilder;
import com.ferox.renderer.builder.TextureCubeMapBuilder;
import com.ferox.renderer.loader.RadianceImageLoader;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class EnvironmentMap {
    private static final int SAMPLE_COUNT = 1000;
    private static final int DIR_SIDE = 32;

    private final int side;
    private final float[][] env;
    private final float[][] solidAngle;

    private final float[][] diff;

    private final List<Sample> samples;

    private EnvironmentMap(int side) {
        this.side = side;
        env = new float[6][side * side * 3];

        diff = new float[6][DIR_SIDE * DIR_SIDE * 3];
        solidAngle = new float[6][side * side];
        samples = new ArrayList<>();
    }

    public static BufferedImage resize(BufferedImage image, int width, int height) {
        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TRANSLUCENT);
        Graphics2D g2d = bi.createGraphics();
        g2d.addRenderingHints(new RenderingHints(RenderingHints.KEY_RENDERING,
                                                 RenderingHints.VALUE_RENDER_QUALITY));
        g2d.drawImage(image, 0, 0, width, height, null);
        g2d.dispose();
        return bi;
    }

    public static void main(String[] args) throws IOException {
        File in = new File("/Users/mludwig/Desktop/FordStudio.hdr");
        EnvironmentMap toCache = createFromCubeMap(in);

        File out = new File(in.getParent() + File.separator +
                            in.getName().substring(0, in.getName().length() - 4) + "_" + SAMPLE_COUNT +
                            ".env");
        toCache.write(out);
    }

    public static EnvironmentMap createFromCubeMap(File verticalCrossHDR) throws IOException {
        RadianceImageLoader.Image cross;
        try (BufferedInputStream stream = new BufferedInputStream(new FileInputStream(verticalCrossHDR))) {
            cross = new RadianceImageLoader().read(stream);
        }

        EnvironmentMap map = new EnvironmentMap(cross.width / 3);
        convertCross(map.side, map.env, cross);

        map.computeSolidAngles();
        map.computeDiffuseIrradiance();
        map.computeSamples();
        return map;
    }

    public static EnvironmentMap loadFromFile(File cachedData) throws IOException {
        try (DataInputStream in = new DataInputStream(new FileInputStream(cachedData))) {
            int side = in.readInt();
            System.out.println(side);
            EnvironmentMap map = new EnvironmentMap(side);
            int ct = 0;
            for (int i = 0; i < 6; i++) {
                for (int j = 0; j < map.env[i].length; j++) {
                    map.env[i][j] = in.readFloat();
                    ct++;
                }
            }
            System.out.println("Read " + ct + " floats for env");
            ct = 0;
            for (int i = 0; i < 6; i++) {
                for (int j = 0; j < map.solidAngle[i].length; j++) {
                    map.solidAngle[i][j] = in.readFloat();
                    ct++;
                }
            }
            System.out.println("Read " + ct + " floats for solid angles");

            int dirSide = in.readInt();
            if (dirSide != DIR_SIDE) {
                throw new IOException("Unexpected diffuse irradiance size: " + dirSide);
            }
            for (int i = 0; i < 6; i++) {
                for (int j = 0; j < map.diff[i].length; j++) {
                    map.diff[i][j] = in.readFloat();
                }
            }

            int numSamples = in.readInt();
            for (int i = 0; i < numSamples; i++) {
                int face = in.readInt();
                int x = in.readInt();
                int y = in.readInt();

                double dx = in.readDouble();
                double dy = in.readDouble();
                double dz = in.readDouble();
                double ix = in.readDouble();
                double iy = in.readDouble();
                double iz = in.readDouble();

                map.samples.add(new Sample(face, x, y, new Vector3(dx, dy, dz), new Vector3(ix, iy, iz)));
            }

            return map;
        }
    }

    public void write(File data) throws IOException {
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(data))) {
            out.writeInt(side);
            int ct = 0;
            for (int i = 0; i < 6; i++) {
                for (int j = 0; j < env[i].length; j++) {
                    out.writeFloat(env[i][j]);
                    ct++;
                }
            }
            System.out.println("Wrote " + ct + " floats for env");
            ct = 0;
            for (int i = 0; i < 6; i++) {
                for (int j = 0; j < solidAngle[i].length; j++) {
                    out.writeFloat(solidAngle[i][j]);
                    ct++;
                }
            }
            System.out.println("Wrote " + ct + " floats for solid angles");

            out.writeInt(DIR_SIDE);
            for (int i = 0; i < 6; i++) {
                for (int j = 0; j < diff[i].length; j++) {
                    out.writeFloat(diff[i][j]);
                }
            }

            out.writeInt(samples.size());
            for (Sample s : samples) {
                out.writeInt(s.face);
                out.writeInt(s.x);
                out.writeInt(s.y);

                out.writeDouble(s.direction.x);
                out.writeDouble(s.direction.y);
                out.writeDouble(s.direction.z);

                out.writeDouble(s.illumination.x);
                out.writeDouble(s.illumination.y);
                out.writeDouble(s.illumination.z);
            }
        }
    }

    private void computeSamples() {
        StructuredImportanceSampler sampler = new StructuredImportanceSampler(env, solidAngle, side, 6);
        samples.clear();
        samples.addAll(sampler.computeSamples(SAMPLE_COUNT));
    }

    public List<Sample> getSamples() {
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

    /*
    public TextureCubeMap createSolidAngleMap(Framework framework) {
        TextureCubeMapBuilder cmb = framework.newTextureCubeMap();
        cmb.side(side).interpolated();
        CubeImageData<? extends TextureBuilder.BasicColorData> data = cmb.r();
        data.positiveX(0).from(solidAngle[0]);
        data.positiveY(0).from(solidAngle[1]);
        data.positiveZ(0).from(solidAngle[2]);
        data.negativeX(0).from(solidAngle[3]);
        data.negativeY(0).from(solidAngle[4]);
        data.negativeZ(0).from(solidAngle[5]);
        return cmb.build();
    }
*/
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

        System.out.println("Max irradiance = " + maxIrradiance);
    }

    public static class Sample implements Comparable<Sample> {
        public final Vector3 direction;
        public final Vector3 illumination;

        public final int face;
        public final int x;
        public final int y;

        public Sample(int face, int x, int y, Vector3 direction, Vector3 illumination) {
            this.face = face;
            this.x = x;
            this.y = y;
            this.direction = direction;
            this.illumination = illumination;
        }

        @Override
        public int compareTo(Sample o) {
            return Double.compare(illumination.lengthSquared(), o.illumination.lengthSquared());
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
                    color.set(env[i], 3 * y * side + 3 * x);
                    toVector(i, x, y, side, side, light);
                    double pd = normal.dot(light);
                    if (pd > 0) {
                        double dSA = solidAngle[i][y * side + x];
                        // NOTE this does not include the (1 - vn/2)^5 term that depends on the viewing angle to normal
                        pd = pd * (28.0 / (23.0 * Math.PI) * (1.0 - Math.pow(1.0 - pd / 2.0, 5.0)));
                        irradiance.addScaled(pd * dSA, color);
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

    // from AMD CubeMapGen
    private static double areaElement(double x, double y) {
        return Math.atan2(x * y, Math.sqrt(x * x + y * y + 1));
    }

    private static double texelCoordSolidAngle(int face, int tx, int ty, int size) {
        // scale up to [-1, 1] range (inclusive), offset by 0.5 to point to texel center.
        double u = 2.0 * ((tx + 0.5) / (double) size) - 1.0;
        double v = 2.0 * ((ty + 0.5) / (double) size) - 1.0;

        double invResolution = 1.0 / size;

        // U and V are the -1..1 texture coordinate on the current face.
        // Get projected area for this texel
        double x0 = u - invResolution;
        double y0 = v - invResolution;
        double x1 = u + invResolution;
        double y1 = v + invResolution;

        return areaElement(x0, y0) - areaElement(x0, y1) - areaElement(x1, y0) + areaElement(x1, y1);
    }

    private void computeSolidAngles() {
        for (int i = 0; i < 6; i++) {
            for (int y = 0; y < side; y++) {
                for (int x = 0; x < side; x++) {
                    solidAngle[i][y * side + x] = (float) texelCoordSolidAngle(i, x, y, side);
                }
            }
        }
    }

    private static void convertCross(int side, float[][] env, RadianceImageLoader.Image cross) {
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
                int crossX = side + x;
                int crossY = side + y;

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
