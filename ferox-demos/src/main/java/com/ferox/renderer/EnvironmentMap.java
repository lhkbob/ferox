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
import com.ferox.renderer.builder.*;
import com.ferox.renderer.loader.RadianceImageLoader;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 */
public class EnvironmentMap {
    public static final double[] SPEC_EXP = { -1.0,
                                              1.0,
                                              10.0,
                                              20.0,
                                              50.0,
                                              150.0,
                                              300.0,
                                              600.0,
                                              1200.0,
                                              2400.0,
                                              5000.0,
                                              8000.0,
                                              10000.0,
                                              -1.0 };
    public static final int[] SPEC_SIDE = { 64, 64, 64, 64, 64, 64, 64, 128, 128, 128, 256, 256, 256, -1 };
    public static final int SPEC_COUNT = SPEC_EXP.length;

    public static final int PX = 0;
    public static final int PY = 1;
    public static final int PZ = 2;
    public static final int NX = 3;
    public static final int NY = 4;
    public static final int NZ = 5;

    private static final int DIR_SIDE = 32;

    private final int side;
    private final float[][] env;

    private final float[][] diff;
    private final float[][][] spec;

    private final transient List<Sample> samples;

    private EnvironmentMap(int side) {
        this.side = side;
        //        int numMips = (int) Math.floor(Math.log(side) / Math.log(2)) + 1;

        env = new float[6][side * side * 3];
        diff = new float[6][DIR_SIDE * DIR_SIDE * 3];
        spec = new float[SPEC_COUNT][6][];
        for (int m = 0; m < SPEC_COUNT; m++) {
            //            int s = Math.max(side >> m, 1);
            int s = (SPEC_SIDE[m] < 0 ? side : SPEC_SIDE[m]);

            for (int i = 0; i < 6; i++) {
                spec[m][i] = new float[s * s * 3];
            }
        }

        samples = new ArrayList<>();
    }

    public static void main(String[] args) throws IOException {
        File in = new File("/Users/mludwig/Desktop/Congress_hall_default_zvc_scaled.hdr");
        EnvironmentMap toCache = createFromCubeMap(in);

        File out = new File(in.getParent() + File.separator +
                            in.getName().substring(0, in.getName().length() - 4) + ".env2");
        toCache.write(out);
    }

    public static EnvironmentMap createFromCubeMap(File verticalCrossHDR) throws IOException {
        RadianceImageLoader.Image cross;
        try (BufferedInputStream stream = new BufferedInputStream(new FileInputStream(verticalCrossHDR))) {
            cross = new RadianceImageLoader().read(stream);
        }

        EnvironmentMap map = new EnvironmentMap(cross.width / 3);
        convertCross(map.side, map.env, cross);

        map.computeDiffuseIrradiance();
        map.computeSpecularIrradiance();
        map.computeSamples();
        return map;
    }

    public static EnvironmentMap loadFromFile(File cachedData) throws IOException {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(cachedData)))) {
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

            int dirSide = in.readInt();
            if (dirSide != DIR_SIDE) {
                throw new IOException("Unexpected diffuse irradiance size: " + dirSide);
            }
            for (int i = 0; i < 6; i++) {
                for (int j = 0; j < map.diff[i].length; j++) {
                    map.diff[i][j] = in.readFloat();
                }
            }

            ct = 0;
            for (int m = 0; m < map.spec.length; m++) {
                for (int i = 0; i < 6; i++) {
                    for (int j = 0; j < map.spec[m][i].length; j++) {
                        map.spec[m][i][j] = in.readFloat();
                        ct++;
                    }
                }
            }
            System.out.println("Read " + ct + " floats for spec");

            map.computeSamples();
            return map;
        }
    }

    public void write(File data) throws IOException {
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(data)))) {
            out.writeInt(side);
            int ct = 0;
            for (int i = 0; i < 6; i++) {
                for (int j = 0; j < env[i].length; j++) {
                    out.writeFloat(env[i][j]);
                    ct++;
                }
            }
            System.out.println("Wrote " + ct + " floats for env");

            out.writeInt(DIR_SIDE);
            for (int i = 0; i < 6; i++) {
                for (int j = 0; j < diff[i].length; j++) {
                    out.writeFloat(diff[i][j]);
                }
            }

            ct++;
            for (int m = 0; m < spec.length; m++) {
                for (int i = 0; i < 6; i++) {
                    for (int j = 0; j < spec[m][i].length; j++) {
                        out.writeFloat(spec[m][i][j]);
                        ct++;
                    }
                }
            }
            System.out.println("Wrote " + ct + " floats for spec");
        }
    }

    private static float[] scale(float[] env, int side, int newSide) {
        float[] data = new float[newSide * newSide * 3];

        double ratio = side / (double) newSide;
        for (int y = 0; y < newSide; y++) {
            for (int x = 0; x < newSide; x++) {
                // now iterate over the full image data
                for (double y2 = ratio * y; y2 < (y + 1) * ratio; y2 += 1.0) {
                    for (double x2 = ratio * x; x2 < (x + 1) * ratio; x2 += 1.0) {
                        // linear interpolation
                        double ax = x2 - Math.floor(x2);
                        double ay = y2 - Math.floor(y2);
                        int o1 = (int) (Math.max(0, Math.min(Math.floor(y2), side - 1)) * side * 3 +
                                        Math.max(0, Math.min(Math.floor(x2), side - 1)) * 3);
                        int o2 = (int) (Math.max(0, Math.min(Math.floor(y2), side - 1)) * side * 3 +
                                        Math.max(0, Math.min(Math.floor(x2) + 1, side - 1)) * 3);
                        int o3 = (int) (Math.max(0, Math.min(Math.floor(y2) + 1, side - 1)) * side * 3 +
                                        Math.max(0, Math.min(Math.floor(x2), side - 1)) * 3);
                        int o4 = (int) (Math.max(0, Math.min(Math.floor(y2) + 1, side - 1)) * side * 3 +
                                        Math.max(0, Math.min(Math.floor(x2) + 1, side - 1)) * 3);

                        data[y * newSide * 3 + x * 3] += (1.0 - ay) * ((1.0 - ax) * env[o1] + ax * env[o2]) +
                                                         ay * ((1.0 - ax) * env[o3] + ax * env[o4]);
                        data[y * newSide * 3 + x * 3 + 1] += //
                                (1.0 - ay) * ((1.0 - ax) * env[o1 + 1] + ax * env[o2 + 1]) +
                                ay * ((1.0 - ax) * env[o3 + 1] + ax * env[o4 + 1]);
                        data[y * newSide * 3 + x * 3 + 2] += //
                                (1.0 - ay) * ((1.0 - ax) * env[o1 + 2] + ax * env[o2 + 2]) +
                                ay * ((1.0 - ax) * env[o3 + 2] + ax * env[o4 + 2]);

                    }
                }

                // average
                data[y * newSide * 3 + x * 3] /= (ratio * ratio);
                data[y * newSide * 3 + x * 3 + 1] /= (ratio * ratio);
                data[y * newSide * 3 + x * 3 + 2] /= (ratio * ratio);
            }
        }

        return data;
    }

    public Texture2D[] createDualParaboloidMap(Framework framework) {
        float[] posZ = new float[4 * side * side * 3];
        float[] negZ = new float[4 * side * side * 3];

        Vector3 negDir = new Vector3();
        Vector3 posDir = new Vector3();

        Vector3 color = new Vector3();

        double minZ = 10.0;
        float b = 1.0f; //1.2f;
        for (int y = 0; y < 2 * side; y++) {
            for (int x = 0; x < 2 * side; x++) {
                double s = x / (2.0 * side);
                double t = y / (2.0 * side);

                //                double alpha = 4 * b * b * ((s - 0.5) * (s - 0.5) + (y - 0.5) * (y - 0.5));
                //
                //                double zNeg = (1.0 - alpha) / (alpha + 1.0);
                //                double zPos = (alpha - 1.0) / (alpha + 1.0);
                //
                //                negDir.set(2.0 * b * (s - 0.5) * (1.0 - zNeg), 2.0 * b * (t - 0.5) * (1.0 - zNeg), zNeg)
                //                      .normalize();
                //                posDir.set(-2.0 * b * (s - 0.5) * (1.0 + zPos), -2.0 * b * (t - 0.5) * (1.0 + zPos), zPos)
                //                      .normalize();

                double rx = 2.0 * s - 1.0;
                double ry = 2.0 * t - 1.0;
                double zPos = 0.5 - 0.5 * (rx * rx + ry * ry);
                double zNeg = -zPos;

                posDir.set(rx, ry, zPos).normalize();
                negDir.set(rx, ry, zNeg).normalize();

                minZ = Math.min(minZ, Math.min(Math.abs(negDir.z), Math.abs(posDir.z)));

                //                color.set(negDir);
                sample(negDir, color);
                color.get(negZ, 2 * side * y * 3 + x * 3);

                sample(posDir, color);
                //                color.set(posDir);
                color.get(posZ, 2 * side * y * 3 + x * 3);
            }
        }
        System.out.println("MIN Z: " + minZ);

        Texture2DBuilder pos = framework.newTexture2D();
        pos.width(2 * side).height(2 * side).wrap(Sampler.WrapMode.CLAMP).interpolated();
        ImageData<? extends TextureBuilder.BasicColorData> posData = pos.rgb();

        posData.mipmap(0).from(posZ);
        int numMips = (int) Math.floor(Math.log(2 * side) / Math.log(2)) + 1;
        for (int i = 1; i < numMips; i++) {
            int s = Math.max(1, (2 * side) >> i);
            posData.mipmap(i).from(scale(posZ, 2 * side, s));
        }

        Texture2DBuilder neg = framework.newTexture2D();
        neg.width(2 * side).height(2 * side).wrap(Sampler.WrapMode.CLAMP).interpolated();
        ImageData<? extends TextureBuilder.BasicColorData> negData = neg.rgb();

        negData.mipmap(0).from(negZ);
        for (int i = 1; i < numMips; i++) {
            int s = Math.max(1, (2 * side) >> i);
            negData.mipmap(i).from(scale(negZ, 2 * side, s));
        }

        return new Texture2D[] { neg.build(), pos.build() };
    }

    public TextureCubeMap createEnvironmentMap(Framework framework) {
        TextureCubeMapBuilder cmb = framework.newTextureCubeMap();
        cmb.side(side).wrap(Sampler.WrapMode.CLAMP).interpolated();
        CubeImageData<? extends TextureBuilder.BasicColorData> data = cmb.rgb();

        // FIXME this really needs to be a well labeled method call in sampler
        int numMips = (int) Math.floor(Math.log(side) / Math.log(2)) + 1;

        data.positiveX(0).from(env[PX]);
        data.positiveY(0).from(env[PY]);
        data.positiveZ(0).from(env[PZ]);
        data.negativeX(0).from(env[NX]);
        data.negativeY(0).from(env[NY]);
        data.negativeZ(0).from(env[NZ]);

        for (int i = 1; i < numMips; i++) {
            int s = Math.max(1, side >> i);
            data.positiveX(i).from(scale(env[PX], side, s));
            data.positiveY(i).from(scale(env[PY], side, s));
            data.positiveZ(i).from(scale(env[PZ], side, s));
            data.negativeX(i).from(scale(env[NX], side, s));
            data.negativeY(i).from(scale(env[NY], side, s));
            data.negativeZ(i).from(scale(env[NZ], side, s));
        }
        return cmb.build();
    }

    public TextureCubeMap createDiffuseMap(Framework framework) {
        TextureCubeMapBuilder cmb = framework.newTextureCubeMap();
        cmb.side(DIR_SIDE).wrap(Sampler.WrapMode.CLAMP).interpolated();
        CubeImageData<? extends TextureBuilder.BasicColorData> data = cmb.rgb();
        data.positiveX(0).from(diff[PX]);
        data.positiveY(0).from(diff[PY]);
        data.positiveZ(0).from(diff[PZ]);
        data.negativeX(0).from(diff[NX]);
        data.negativeY(0).from(diff[NY]);
        data.negativeZ(0).from(diff[NZ]);
        return cmb.build();
    }

    public TextureCubeMap[] createSpecularMaps(Framework framework) {
        TextureCubeMap[] cubes = new TextureCubeMap[SPEC_COUNT];
        for (int m = 0; m < SPEC_COUNT; m++) {
            TextureCubeMapBuilder cmb = framework.newTextureCubeMap();
            int s = (SPEC_SIDE[m] < 0 ? side : SPEC_SIDE[m]);
            cmb.side(s).wrap(Sampler.WrapMode.CLAMP).interpolated();
            CubeImageData<? extends TextureBuilder.BasicColorData> data = cmb.rgb();
            data.positiveX(0).from(spec[m][PX]);
            data.positiveY(0).from(spec[m][PY]);
            data.positiveZ(0).from(spec[m][PZ]);
            data.negativeX(0).from(spec[m][NX]);
            data.negativeY(0).from(spec[m][NY]);
            data.negativeZ(0).from(spec[m][NZ]);
            cubes[m] = cmb.build();
        }

        return cubes;
    }

    public List<Sample> getSamples() {
        return samples;
    }

    private void computeSamples() {
        samples.clear();
        for (int i = 0; i < 6; i++) {
            for (int y = 0; y < side; y++) {
                for (int x = 0; x < side; x++) {
                    Vector3 r = new Vector3();
                    r.set(env[i], y * side * 3 + x * 3);
                    r.scale(texelCoordSolidAngle(x, y, side));

                    Vector3 d = new Vector3();
                    toVector(i, x, y, side, side, d);
                    samples.add(new Sample(i, x, y, d, r));
                }
            }
        }

        Collections.sort(samples);
        Collections.reverse(samples);
    }

    private void computeDiffuseIrradiance() {
        Convolution conv = new Convolution(new Convolution.AshikhminDiffuse(), DIR_SIDE, diff);
        conv.convolve(env, side);
    }

    private void computeSpecularIrradiance() {
        for (int i = 1; i < spec.length - 1; i++) {
            //            double exp = 10 * Math.pow(4, spec.length - i - 1) - 9;
            //            double exp = 50 * (spec.length - i - 1);
            double exp = SPEC_EXP[i];
            //            int side = Math.max(this.side >> i, 1);
            int s = SPEC_SIDE[i];
            System.out.printf("level %d, side = %d, exp = %.2f\n", i, s, exp);
            Convolution conv = new Convolution(new Convolution.AshikhminIsotropicSpecular(exp), s, spec[i]);
            conv.convolve(env, this.side);
        }

        // compute environment map scaled by solid angle, which approximates the highest specularity
        float[][] scaled = spec[SPEC_COUNT - 1];
        for (int i = 0; i < 6; i++) {
            for (int y = 0; y < side; y++) {
                for (int x = 0; x < side; x++) {
                    float dsa = (float) texelCoordSolidAngle(x, y, side);
                    scaled[i][y * side * 3 + x * 3] = dsa * env[i][y * side * 3 + x * 3];
                    scaled[i][y * side * 3 + x * 3 + 1] = dsa * env[i][y * side * 3 + x * 3 + 1];
                    scaled[i][y * side * 3 + x * 3 + 2] = dsa * env[i][y * side * 3 + x * 3 + 2];
                }
            }
        }

        // duplicate 0th level from 1st
        for (int i = 0; i < 6; i++) {
            System.arraycopy(spec[1][i], 0, spec[0][i], 0, spec[1][i].length);
        }
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

    public static void toVector(int face, int tx, int ty, int w, int h, Vector3 v) {
        // technically these are sc / |ma| and tc / |ma| but we assert
        // that |ma| = 1
        float sc = 2f * (tx + 0.5f) / (float) w - 1f;
        float tc = 2f * (ty + 0.5f) / (float) h - 1f;
        switch (face) {
        case PX: // px
            v.set(1.0, -tc, -sc);
            break;
        case PY: // py
            v.set(sc, 1.0, tc);
            break;
        case PZ: // pz
            v.set(sc, -tc, 1.0);
            break;
        case NX: // nx
            v.set(-1.0, -tc, sc);
            break;
        case NY: // ny
            v.set(sc, -1.0, -tc);
            break;
        case NZ: // nz
            v.set(-sc, -tc, -1.0);
            break;
        }
        v.normalize();
    }

    private void sample(@Const Vector3 coord, Vector3 out) {
        double sc, tc, ma;
        int face;

        if (Math.abs(coord.x) > Math.abs(coord.y) && Math.abs(coord.x) > Math.abs(coord.z)) {
            if (coord.x >= 0.0) {
                ma = coord.x;
                sc = -coord.z;
                tc = -coord.y;
                face = PX;
            } else {
                ma = -coord.x;
                sc = coord.z;
                tc = -coord.y;
                face = NX;
            }
        } else if (Math.abs(coord.y) > Math.abs(coord.x) && Math.abs(coord.y) > Math.abs(coord.z)) {
            if (coord.y >= 0) {
                ma = coord.y;
                sc = coord.x;
                tc = coord.z;
                face = PY;
            } else {
                ma = -coord.y;
                sc = coord.x;
                tc = -coord.z;
                face = NY;
            }
        } else {
            if (coord.z >= 0) {
                ma = coord.z;
                sc = coord.x;
                tc = -coord.y;
                face = PZ;
            } else {
                ma = -coord.z;
                sc = -coord.x;
                tc = -coord.y;
                face = NZ;
            }
        }

        double s = Math.max(0.0, Math.min(0.5 * (sc / ma + 1.0) * side, side - 2));
        double t = Math.max(0.0, Math.min(0.5 * (tc / ma + 1.0) * side, side - 2));

        int x = (int) Math.floor(s);
        int y = (int) Math.floor(t);
        s = s - x;
        t = t - y;

        int o1 = y * side * 3 + x * 3;
        int o2 = y * side * 3 + (x + 1) * 3;
        int o3 = (y + 1) * side * 3 + x * 3;
        int o4 = (y + 1) * side * 3 + (x + 1) * 3;

        out.set((1 - t) * ((1 - s) * env[face][o1] + s * env[face][o2]) +
                t * ((1 - s) * env[face][o3] + s * env[face][o4]),
                (1 - t) * ((1 - s) * env[face][o1 + 1] + s * env[face][o2 + 1]) +
                t * ((1 - s) * env[face][o3 + 1] + s * env[face][o4 + 1]),
                (1 - t) * ((1 - s) * env[face][o1 + 2] + s * env[face][o2 + 2]) +
                t * ((1 - s) * env[face][o3 + 2] + s * env[face][o4 + 2])
               );
    }

    // from AMD CubeMapGen
    private static double areaElement(double x, double y) {
        return Math.atan2(x * y, Math.sqrt(x * x + y * y + 1));
    }

    public static double texelCoordSolidAngle(int tx, int ty, int size) {
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

    private static void convertCross(int side, float[][] env, RadianceImageLoader.Image cross) {
        // px
        for (int y = 0; y < side; y++) {
            for (int x = 0; x < side; x++) {
                int crossX = 3 * side - x - 1;
                int crossY = 3 * side - y - 1;

                int faceOffset = 3 * y * side + 3 * x;
                int crossOffset = 3 * crossY * cross.width + 3 * crossX;
                env[PX][faceOffset] = cross.data[crossOffset];
                env[PX][faceOffset + 1] = cross.data[crossOffset + 1];
                env[PX][faceOffset + 2] = cross.data[crossOffset + 2];
            }
        }

        // py
        for (int y = 0; y < side; y++) {
            for (int x = 0; x < side; x++) {
                int crossX = side + x; // shifted over 1 column
                int crossY = 3 * side + y; // flip y axis

                int faceOffset = 3 * y * side + 3 * x;
                int crossOffset = 3 * crossY * cross.width + 3 * crossX;
                env[PY][faceOffset] = cross.data[crossOffset];
                env[PY][faceOffset + 1] = cross.data[crossOffset + 1];
                env[PY][faceOffset + 2] = cross.data[crossOffset + 2];
            }
        }

        // pz
        for (int y = 0; y < side; y++) {
            for (int x = 0; x < side; x++) {
                int crossX = side + x;
                int crossY = y;

                int faceOffset = 3 * y * side + 3 * x;
                int crossOffset = 3 * crossY * cross.width + 3 * crossX;
                env[PZ][faceOffset] = cross.data[crossOffset];
                env[PZ][faceOffset + 1] = cross.data[crossOffset + 1];
                env[PZ][faceOffset + 2] = cross.data[crossOffset + 2];
            }
        }

        // nx
        for (int y = 0; y < side; y++) {
            for (int x = 0; x < side; x++) {
                int crossX = side - x - 1;
                int crossY = 3 * side - y - 1;

                int faceOffset = 3 * y * side + 3 * x;
                int crossOffset = 3 * crossY * cross.width + 3 * crossX;
                env[NX][faceOffset] = cross.data[crossOffset];
                env[NX][faceOffset + 1] = cross.data[crossOffset + 1];
                env[NX][faceOffset + 2] = cross.data[crossOffset + 2];
            }
        }

        // ny
        for (int y = 0; y < side; y++) {
            for (int x = 0; x < side; x++) {
                int crossX = side + x;
                int crossY = side + y;

                int faceOffset = 3 * y * side + 3 * x;
                int crossOffset = 3 * crossY * cross.width + 3 * crossX;
                env[NY][faceOffset] = cross.data[crossOffset];
                env[NY][faceOffset + 1] = cross.data[crossOffset + 1];
                env[NY][faceOffset + 2] = cross.data[crossOffset + 2];
            }
        }

        // nz
        for (int y = 0; y < side; y++) {
            for (int x = 0; x < side; x++) {
                int crossX = 2 * side - x - 1;
                int crossY = 3 * side - y - 1;

                int faceOffset = 3 * y * side + 3 * x;
                int crossOffset = 3 * crossY * cross.width + 3 * crossX;
                env[NZ][faceOffset] = cross.data[crossOffset];
                env[NZ][faceOffset + 1] = cross.data[crossOffset + 1];
                env[NZ][faceOffset + 2] = cross.data[crossOffset + 2];
            }
        }
    }
}
