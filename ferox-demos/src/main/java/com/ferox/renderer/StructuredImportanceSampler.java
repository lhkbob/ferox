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

import com.ferox.math.Vector3;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

/**
 *
 */
public class StructuredImportanceSampler {
    private final float[][] env;
    private final float[][] solidAngles;
    private final int side;

    private final int numThresholds;
    private final int[][] thresholdAssignments;
    private final List<ThresholdHierarchy> thresholds;

    private final double sa0; // proportional to T^2

    public StructuredImportanceSampler(float[][] env, float[][] solidAngles, int side, int numThresholds) {
        this.env = env;
        this.solidAngles = solidAngles;
        this.side = side;

        this.numThresholds = numThresholds;

        thresholdAssignments = new int[6][side * side];
        thresholds = new ArrayList<>();
        sa0 = 0.01;

        assignThresholds();
        computeConnectedComponents();
    }

    private void assignThresholds() {
        Vector3 rad = new Vector3();
        double sum = 0.0;
        double sqSum = 0.0;
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < env[i].length; j += 3) {
                rad.set(env[i], j);
                double dsa = solidAngles[i][j / 3];
                sum += dsa * rad.length();
                sqSum += dsa * dsa * rad.lengthSquared();
            }
        }
        double mean = sum / (6 * side * side);
        double stdDev = Math.sqrt(sqSum / (6 * side * side) - mean * mean);
        System.out.println("STD DEV: " + stdDev);

        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < thresholdAssignments[i].length; j++) {
                rad.set(env[i], j * 3);
                int t = Math.min((int) Math.floor(rad.length() * solidAngles[i][j] / stdDev),
                                 numThresholds - 1);
                thresholdAssignments[i][j] = t;
            }
        }

        thresholds.clear();
        for (int i = 0; i < numThresholds; i++) {
            ThresholdHierarchy t = new ThresholdHierarchy(i);
            t.assign();
            thresholds.add(t);
        }
        System.out.println("Assign thresholds completed");
    }

    private void computeConnectedComponents() {
        for (ThresholdHierarchy t : thresholds) {
            t.label();
        }
        for (ThresholdHierarchy t : thresholds) {
            t.computeHierarchy();
        }
        System.out.println("Connected components completed");
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

    private void assignSampleCounts(int totalSamples) {
        Vector3 rad = new Vector3();
        double sum = 0.0;
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < env[i].length; j += 3) {
                rad.set(env[i], j);
                double dsa = solidAngles[i][j / 3];
                sum += dsa * rad.length();
            }
        }

        double maxWeight = sum * Math.pow(sa0, 0.25);
        for (int i = thresholds.size() - 1; i >= 0; i--) {
            thresholds.get(i).computeSampleCounts(maxWeight, totalSamples);
        }
        System.out.println("Sample counts completed");

        int total = 0;
        for (ThresholdHierarchy h : thresholds) {
            for (Integer ct : h.componentSamples.values()) {
                total += ct;
            }
        }
        System.out.println("Sample total: " + total);
    }

    private void computeStrata() {
        for (int i = thresholds.size() - 1; i >= 0; i--) {
            thresholds.get(i).computeStrata();
        }
        System.out.println("Strata centers completed");
    }

    public List<EnvironmentMap.Sample> computeSamples(int numSamples) {
        assignSampleCounts(numSamples);
        computeStrata();

        // all centers from higher levels have been added to threshold 0, which has one component labeled 0
        Set<Center> allCenters = thresholds.get(0).strata.get(0);

        Map<Center, Vector3> integral = new HashMap<>();
        for (Center c : allCenters) {
            integral.put(c, new Vector3());
        }

        Vector3 rad = new Vector3();
        for (int i = 0; i < 6; i++) {
            for (int y = 0; y < side; y++) {
                for (int x = 0; x < side; x++) {
                    rad.set(env[i], 3 * y * side + 3 * x);
                    double dsa = solidAngles[i][y * side + x];
                    Center closest = Center.minDistance(allCenters, i, x, y);
                    // if a face has 0 centers in it, then this will return null
                    if (closest != null) {
                        integral.get(closest).addScaled(dsa, rad);
                    }
                }
            }
        }

        List<EnvironmentMap.Sample> samples = new ArrayList<>();
        for (Map.Entry<Center, Vector3> e : integral.entrySet()) {
            Center c = e.getKey();
            Vector3 d = new Vector3();
            EnvironmentMap.toVector(c.face, c.x, c.y, side, side, d);
            samples.add(new EnvironmentMap.Sample(c.face, c.x, c.y, d, e.getValue()));
        }
        Collections.sort(samples);
        System.out.println("Samples completed");
        return samples;
    }

    private class ThresholdHierarchy {
        final int threshold;
        final boolean[][] assignment;
        final int[][] components;

        // key is component label in this threshold level, value contains all component labels of next higher
        // threshold that are contained within the key component region
        // - key set holds complete component name set. components without children have an empty bit set
        final Map<Integer, BitSet> componentChildren;
        final Map<Integer, Integer> componentSamples;
        final Map<Integer, Set<Center>> strata;

        ThresholdHierarchy(int threshold) {
            this.threshold = threshold;
            assignment = new boolean[6][side * side];
            components = new int[6][side * side];

            componentChildren = new HashMap<>();
            componentSamples = new HashMap<>();
            strata = new HashMap<>();
        }

        Set<Center> computeStrataForComponent(int component) {
            Set<Center> centers = new HashSet<>();

            BitSet children = componentChildren.get(component);
            for (int k = children.nextSetBit(0); k >= 0; k = children.nextSetBit(k + 1)) {
                centers.addAll(thresholds.get(threshold + 1).strata.get(k));
            }

            int sampleCount = componentSamples.get(component);
            if (sampleCount == 0) {
                return centers;
            }

            if (centers.isEmpty()) {
                // add arbitrary point
                for (int i = 0; i < 6; i++) {
                    for (int j = 0; j < components[i].length; j++) {
                        if (components[i][j] == component) {
                            // make sure it's not in a higher threshold
                            if (threshold >= thresholds.size() - 1 ||
                                !thresholds.get(threshold + 1).assignment[i][j]) {
                                // and the center
                                centers.add(new Center(i, j % side, j / side));
                                break;
                            }
                        }
                    }
                    // no need to check other faces if we found one
                    if (!centers.isEmpty()) {
                        break;
                    }
                }

                sampleCount--;
            }

            // a component is restricted to a single cube face, and centers size > 0
            int face = centers.iterator().next().face;

            while (sampleCount > 0) {
                double maxD2 = 0.0;
                int maxX = -1;
                int maxY = -1;

                for (int y = 0; y < side; y++) {
                    for (int x = 0; x < side; x++) {
                        // only consider pixel if we're in the same component and it wasn't already
                        // added to a stratum from a higher threshold
                        if (components[face][y * side + x] == component &&
                            (threshold >= thresholds.size() - 1 ||
                             !thresholds.get(threshold + 1).assignment[face][y * side + x])) {
                            Center closest = Center.minDistance(centers, face, x, y);
                            double d2 = closest.distance(x, y);
                            if (d2 > maxD2) {
                                maxD2 = d2;
                                maxX = x;
                                maxY = y;
                            }
                        }
                    }
                }

                Center max = new Center(face, maxX, maxY);
                centers.add(max);
                sampleCount--;
            }
            return centers;
        }

        void computeStrata() {
            strata.clear();
            for (Integer c : componentChildren.keySet()) {
                strata.put(c, computeStrataForComponent(c));
            }
        }

        void computeSampleCounts(double maxWeight, int totalSamples) {
            // requires that child sample counts have already been computed
            Vector3 rad = new Vector3();

            Map<Integer, Double> sumL = new HashMap<>();
            Map<Integer, Double> sumSA = new HashMap<>();
            Map<Integer, Integer> counts = new HashMap<>();

            for (int i = 0; i < 6; i++) {
                for (int j = 0; j < env[i].length; j += 3) {
                    rad.set(env[i], j);
                    double dsa = solidAngles[i][j / 3];

                    int c = components[i][j / 3];
                    if (c >= 0) {
                        if (sumL.containsKey(c)) {
                            sumL.put(c, sumL.get(c) + dsa * rad.length());
                            sumSA.put(c, sumSA.get(c) + dsa);
                        } else {
                            sumL.put(c, dsa * rad.length());
                            sumSA.put(c, dsa);
                        }

                        if (threshold >= thresholds.size() - 1 ||
                            !thresholds.get(threshold + 1).assignment[i][j / 3]) {
                            if (counts.containsKey(c)) {
                                counts.put(c, counts.get(c) + 1);
                            } else {
                                counts.put(c, 1);
                            }
                        }
                    }
                }
            }

            componentSamples.clear();
            int totalForThreshold = 0;
            for (Integer c : componentChildren.keySet()) {
                int totalForComponent = (int) Math.floor(totalSamples * sumL.get(c) *
                                                         Math.pow(Math.min(sumSA.get(c), sa0), 0.25) /
                                                         maxWeight);
                BitSet current = new BitSet();
                BitSet next = new BitSet();
                current.or(componentChildren.get(c));
                for (int t = threshold + 1; t < thresholds.size(); t++) {
                    for (int k = current.nextSetBit(0); k >= 0; k = current.nextSetBit(k + 1)) {
                        totalForComponent -= thresholds.get(t).componentSamples.get(k);
                        next.or(thresholds.get(t).componentChildren.get(k));
                    }
                    current.clear();
                    current.or(next);
                    next.clear();
                }

                if (totalForComponent < 0) {
                    // we can get negatives at the lower thresholds because our level's count is
                    // artificially reduced because we clamp the solid angle contribution
                    totalForComponent = 0;
                }

                // we are not in a position to sample a texel multiple times, so tweak the counts a bit
                int minTexels = (counts.containsKey(c) ? counts.get(c) : 0);
                totalForComponent = Math.min(totalForComponent, minTexels);

                totalForThreshold += totalForComponent;
                componentSamples.put(c, totalForComponent);
            }
            System.out.println("Threshold " + threshold + " has " + totalForThreshold + " samples");
        }

        void assign() {
            if (threshold == 0) {
                // base layer, everything is a member
                for (int i = 0; i < 6; i++) {
                    Arrays.fill(assignment[i], true);
                }
            } else {
                for (int i = 0; i < 6; i++) {
                    for (int j = 0; j < assignment[i].length; j++) {
                        assignment[i][j] = thresholdAssignments[i][j] >= threshold;
                    }
                }
            }
        }

        void computeHierarchy() {
            // this assumes that threshold+1 has already been labeled
            if (threshold < thresholds.size() - 1) {
                if (threshold == 0) {
                    // root, so every component in the next level is a child to the single component
                    BitSet root = componentChildren.get(0);
                    for (Integer v : thresholds.get(threshold + 1).componentChildren.keySet()) {
                        root.set(v);
                    }
                } else {
                    ThresholdHierarchy child = thresholds.get(threshold + 1);
                    for (int i = 0; i < 6; i++) {
                        for (int y = 0; y < side; y++) {
                            for (int x = 0; x < side; x++) {
                                int currentComponent = components[i][y * side + x];
                                int childComponent = child.components[i][y * side + x];

                                if (currentComponent >= 0 && childComponent >= 0) {
                                    componentChildren.get(currentComponent).set(childComponent);
                                }
                            }
                        }
                    }
                }
            } // else leaf so don't have any children
        }

        void label() {
            if (threshold == 0) {
                // there is one group, labeled as 0
                for (int i = 0; i < 6; i++) {
                    Arrays.fill(components[i], 0);
                }
                componentChildren.clear();
                componentChildren.put(0, new BitSet());
            } else {
                // identify connected components (note that we don't connect around the edges of the cube map
                int labelCounter = 0;
                Map<Integer, BitSet> labelEquivalences = new HashMap<>(); // key = min label, value = all other equiv. labels
                Map<Integer, Integer> reverseLabels = new HashMap<>(); // key = label, value = min equiv. label
                for (int i = 0; i < 6; i++) {
                    for (int y = 0; y < side; y++) {
                        for (int x = 0; x < side; x++) {
                            if (assignment[i][y * side + x]) {
                                boolean leftSame = (x > 0 && assignment[i][y * side + x - 1]);
                                boolean botSame = (y > 0 && assignment[i][(y - 1) * side + x]);
                                if (leftSame && botSame) {
                                    // both values match, take smallest label and mark the two neighbor labels as the same
                                    int leftLabel = components[i][y * side + x - 1];
                                    int botLabel = components[i][(y - 1) * side + x];

                                    int minLabel = Math.min(leftLabel, botLabel);
                                    int maxLabel = Math.max(leftLabel, botLabel);

                                    components[i][y * side + x] = minLabel;

                                    BitSet minEquivs = labelEquivalences.get(minLabel);
                                    if (minEquivs == null) {
                                        minEquivs = new BitSet();
                                        labelEquivalences.put(minLabel, minEquivs);
                                    }

                                    // mark max as equivalent to minimum
                                    minEquivs.set(maxLabel);
                                    reverseLabels.put(maxLabel, minLabel);

                                    // propagate label merge to all labels that thought max had been the minimum
                                    BitSet maxEquivs = labelEquivalences.remove(maxLabel);
                                    if (maxEquivs != null) {
                                        for (int k = maxEquivs.nextSetBit(0); k >= 0;
                                             k = maxEquivs.nextSetBit(k + 1)) {
                                            minEquivs.set(k);
                                            reverseLabels.put(k, minLabel);
                                        }
                                    }
                                } else if (leftSame) {
                                    // take left label
                                    components[i][y * side + x] = components[i][y * side + x - 1];
                                } else if (botSame) {
                                    // take bottom label
                                    components[i][y * side + x] = components[i][(y - 1) * side + x];
                                } else {
                                    // new label
                                    components[i][y * side + x] = labelCounter;
                                    reverseLabels.put(labelCounter, labelCounter);
                                    labelCounter++;
                                }
                            } else {
                                components[i][y * side + x] = -1;
                            }
                        }
                    }
                }
                // update equivalent labels to the minimum
                componentChildren.clear();
                for (int i = 0; i < 6; i++) {
                    for (int j = 0; j < components[i].length; j++) {
                        if (components[i][j] >= 0) {
                            components[i][j] = reverseLabels.get(components[i][j]);
                            if (!componentChildren.containsKey(components[i][j])) {
                                componentChildren.put(components[i][j], new BitSet());
                            }
                        }
                    }
                }
            }
        }
    }

    private static class Center {
        final int x;
        final int y;
        final int face;

        public Center(int face, int x, int y) {
            this.face = face;
            this.x = x;
            this.y = y;
        }

        double distance(int x, int y) {
            return (x - this.x) * (x - this.x) + (y - this.y) * (y - this.y);
        }

        static Center minDistance(Set<Center> centers, int face, int x, int y) {
            double minD2 = Double.POSITIVE_INFINITY;
            Center min = null;
            for (Center c : centers) {
                if (c.face == face) {
                    double d2 = c.distance(x, y);
                    if (min == null || d2 < minD2) {
                        minD2 = d2;
                        min = c;
                    }
                }
            }
            return min;
        }

        @Override
        public String toString() {
            return "[" + face + " - " + x + ", " + y + "]";
        }
    }
}
