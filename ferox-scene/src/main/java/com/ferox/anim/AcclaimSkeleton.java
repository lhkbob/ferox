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
package com.ferox.anim;

import com.ferox.math.Matrix3;
import com.ferox.math.Matrix4;
import com.ferox.math.Vector3;
import com.ferox.math.Vector4;
import com.lhkbob.entreri.Entity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AcclaimSkeleton {
    private String name;
    private ASFRoot root;
    private List<ASFBone> boneData;
    private Map<Object, List<ASFBone>> hierarchy; // key is Bone or Root instance

    public String getName() {
        return name;
    }

    public Skeleton addSkeleton(Entity entity) {
        Skeleton skeleton = entity.add(Skeleton.class).getData();

        Bone rootBone = createRootBone();
        skeleton.addBone(rootBone);
        skeleton.setRootBone(rootBone);

        addBones(root, rootBone, skeleton);

        return skeleton;
    }

    public SkeletonAnimation loadAnimation(InputStream acmFile, double frameRate) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(acmFile));

        SkeletonAnimation animation = new SkeletonAnimation();
        KeyFrame.Builder currentKeyFrame = null;
        boolean inDegrees = true; // assume this is the default just like in ASF

        String line;
        while ((line = in.readLine()) != null) {
            line = line.replaceAll("#.*", "").trim();
            if (line.isEmpty()) {
                continue;
            }

            String[] tokens = line.split("\\s+");

            if (tokens[0].equalsIgnoreCase(":degrees")) {
                inDegrees = true;
            } else if (tokens[0].equalsIgnoreCase(":radians")) {
                inDegrees = false;
            } else if (tokens[0].startsWith(":")) {
                // ignore
            } else if (tokens.length == 1) {
                try {
                    int frame = Integer.parseInt(tokens[0]);
                    if (currentKeyFrame != null && (frame - 1) % 5 == 0) {
                        animation.addKeyFrame(currentKeyFrame.build());
                    }
                    currentKeyFrame = new KeyFrame.Builder(frameRate * (frame - 1));
                } catch (NumberFormatException nfe) {
                    throw new IOException("Expected single integer on line: " + line);
                }
            } else {
                if (tokens[0].equals("root")) {
                    double[] dofValues = new double[root.order.length];
                    if (dofValues.length != tokens.length - 1) {
                        throw new IOException("Incorrect number of motion values");
                    }

                    for (int i = 0; i < dofValues.length; i++) {
                        dofValues[i] = Double.parseDouble(tokens[i + 1]);
                        if (inDegrees && (root.order[i] == DoF.RX || root.order[i] == DoF.RY ||
                                          root.order[i] == DoF.RZ)) {
                            dofValues[i] = Math.toRadians(dofValues[i]);
                        }
                    }

                    currentKeyFrame.setBone("root", buildRoot(root.order, dofValues));
                } else {
                    ASFBone bone = getBone(tokens[0]);
                    if (bone == null) {
                        throw new IOException("Bone with name: " + tokens[0] + " is undefined");
                    }

                    double[] dofValues = new double[bone.motionDoF.length];
                    if (dofValues.length != tokens.length - 1) {
                        throw new IOException("Incorrect number of motion values");
                    }

                    for (int i = 0; i < dofValues.length; i++) {
                        dofValues[i] = Double.parseDouble(tokens[i + 1]);
                        if (inDegrees && (bone.motionDoF[i] == DoF.RX || bone.motionDoF[i] == DoF.RY ||
                                          bone.motionDoF[i] == DoF.RZ)) {
                            dofValues[i] = Math.toRadians(dofValues[i]);
                        }
                    }

                    Matrix4 localBasis = new Matrix4().setIdentity().setUpper(bone.childToParentBasis);
                    Matrix4 motion = new Matrix4().setIdentity()
                                                  .setUpper(buildBasis(bone.motionDoF, dofValues));
                    Matrix4 translate = new Matrix4().setIdentity().setCol(3, new Vector4(
                            bone.localDirection.x * bone.length, bone.localDirection.y * bone.length,
                            bone.localDirection.z * bone.length, 1));

                    localBasis.mul(motion).mul(translate);
                    currentKeyFrame.setBone(bone.name, localBasis);
                }
            }
        }

        if (currentKeyFrame != null) {
            animation.addKeyFrame(currentKeyFrame.build());
        }
        return animation;
    }

    private void addBones(Object asfParent, Bone parent, Skeleton skeleton) {
        List<ASFBone> children = hierarchy.get(asfParent);
        if (children != null) {
            for (ASFBone bone : children) {
                Bone childBone = createBone(bone);
                skeleton.addBone(childBone);
                skeleton.connect(parent, childBone);

                addBones(bone, childBone, skeleton);
            }
        }
    }

    private Bone createBone(ASFBone bone) {
        Matrix4 translate = new Matrix4().setIdentity();
        translate.m03 = bone.localDirection.x * bone.length;
        translate.m13 = bone.localDirection.y * bone.length;
        translate.m23 = bone.localDirection.z * bone.length;

        Matrix4 rotate = new Matrix4().setIdentity().setUpper(bone.childToParentBasis);
        rotate.mul(translate);

        Bone newBone = new Bone(bone.name);
        newBone.setRelativeBoneTransform(rotate);
        return newBone;
    }

    private void computeLocalSpace() {
        List<ASFBone> rootChildren = hierarchy.get(root);
        if (rootChildren != null) {
            Matrix3 inverseParent;
            if (root.axis != null) {
                inverseParent = buildBasis(root.axis, root.presetOrientation).inverse();
            } else {
                inverseParent = new Matrix3().setIdentity();
            }

            for (ASFBone child : rootChildren) {
                computeLocalSpace(child, inverseParent);
            }
        }
    }

    private void computeLocalSpace(ASFBone bone, Matrix3 inverseParent) {
        Matrix3 boneGlobalBasis = buildBasis(bone.axis, bone.axisRotation);
        bone.childToParentBasis = new Matrix3().mul(inverseParent, boneGlobalBasis);

        // this now holds the inverse of the bone's global basis
        bone.localDirection = new Vector3().mul(boneGlobalBasis.inverse(), bone.direction);
        bone.localDirection.normalize();

        List<ASFBone> children = hierarchy.get(bone);
        if (children != null) {
            for (ASFBone child : children) {
                computeLocalSpace(child, boneGlobalBasis);
            }
        }
    }

    private Bone createRootBone() {
        Bone rootBone = new Bone("root");
        Matrix4 m = rootBone.getRelativeBoneTransform();
        m.setIdentity();

        if (root.axis != null) {
            m.setUpper(buildBasis(root.axis, root.presetOrientation));
        }

        if (root.presetPosition != null) {
            m.m03 = root.presetPosition.x;
            m.m13 = root.presetPosition.y;
            m.m23 = root.presetPosition.z;
        }
        rootBone.setRelativeBoneTransform(m);
        return rootBone;
    }

    private Matrix3 buildBasis(DoF[] axis, Vector3 values) {
        return buildBasis(axis, new double[] { values.x, values.y, values.z });
    }

    private Matrix4 buildRoot(DoF[] order, double[] values) {
        //        Matrix3 temp = new Matrix3();
        Matrix4 root = new Matrix4().setIdentity();

        // FIXME this is brittle, and odd, because it expects the rotations to
        // be transposed and reversed like in buildBasis(), but the translation
        // must be at the end, even though the  provided order is different.

        // What they give: TX TY TZ RX RY RZ
        // So what the expected translation would be: RZ RY RX TZ TY TX
        // What works: TX TY TZ RZ RY RX
        //
        // Perhaps the site is incorrect, and you just use the order values
        // but construct it using the same axis definition for presetOrientation, etc.?
        // seems plausible
        root.setUpper(buildBasis(new DoF[] { order[3], order[4], order[5] },
                                 new double[] { values[3], values[4], values[5] }));
        root.m03 = values[0];
        root.m13 = values[1];
        root.m23 = values[2];

        //        Matrix4 dof = new Matrix4();
        //
        //        // FIXME order?
        //        for (int i = 0; i < order.length; i++) {
        //            dof.setIdentity();
        //            switch (order[i]) {
        //            case RX:
        //                temp.rotateX(values[i]);
        //                dof.setUpper(temp);
        //                break;
        //            case RY:
        //                temp.rotateY(values[i]);
        //                dof.setUpper(temp);
        //                break;
        //            case RZ:
        //                temp.rotateZ(values[i]);
        //                dof.setUpper(temp);
        //                break;
        //            case TX:
        //                dof.m03 = values[i];
        //                break;
        //            case TY:
        //                dof.m13 = values[i];
        //                break;
        //            case TZ:
        //                dof.m23 = values[i];
        //                break;
        //            default:
        //                throw new IllegalStateException("Bad degree of freedom: " + order[i]);
        //            }
        //
        //            root.mul(dof);
        //        }

        return root;
    }

    private Matrix3 buildBasis(DoF[] axis, double[] values) {
        Matrix3 basis = new Matrix3().setIdentity();
        Matrix3 axisAngle = new Matrix3();

        for (int i = axis.length - 1; i >= 0; i--) {
            switch (axis[i]) {
            case RX:
                axisAngle.rotateX(values[i]);
                break;
            case RY:
                axisAngle.rotateY(values[i]);
                break;
            case RZ:
                axisAngle.rotateZ(values[i]);
                break;
            default:
                throw new IllegalStateException("Bad degree of freedom: " + axis[i]);
            }

            basis.mul(axisAngle);
        }

        return basis;
    }

    public void load(InputStream asf) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(asf));

        // state tracking while progressively loading
        FileSection section = null;

        // when in units
        boolean anglesInDegrees = true; // degrees is default

        // when in bone data
        ASFBone currentBone = null;
        List<Double> minLimits = null;
        List<Double> maxLimits = null;

        // when in hierarchy
        boolean buildingHierarchy = false;

        String line;
        while ((line = in.readLine()) != null) {
            line = line.replaceAll("#.*", "").trim();
            if (line.isEmpty()) {
                continue;
            }

            String[] tokens = line.split("\\s+");

            if (tokens[0].equalsIgnoreCase(":version")) {
                if (tokens.length != 2) {
                    throw new IOException("Malformed :version token: " + line);
                }
                if (!tokens[1].equals("1.10") && !tokens[1].equals("1.1")) {
                    throw new IOException("Unsupported version: " + tokens[1]);
                }
                section = FileSection.VERSION;
            } else if (tokens[0].equalsIgnoreCase(":name")) {
                if (tokens.length != 2) {
                    throw new IOException("Malformed :name token: " + line);
                }
                name = tokens[1];
                section = FileSection.NAME;
            } else if (tokens[0].equalsIgnoreCase(":units")) {
                section = FileSection.UNITS;
            } else if (tokens[0].equalsIgnoreCase(":documentation")) {
                section = FileSection.DOCUMENTATION;
            } else if (tokens[0].equalsIgnoreCase(":root")) {
                root = new ASFRoot();
                section = FileSection.ROOT;
            } else if (tokens[0].equalsIgnoreCase(":bonedata")) {
                boneData = new ArrayList<ASFBone>();
                section = FileSection.BONE_DATA;
            } else if (tokens[0].equalsIgnoreCase(":hierarchy")) {
                section = FileSection.HIERARCHY;
            } else if (tokens[0].equalsIgnoreCase(":skin")) {
                section = FileSection.SKIN;
            } else if (section == FileSection.UNITS) {
                if (tokens[0].equalsIgnoreCase("angle")) {
                    if (tokens.length != 2) {
                        throw new IOException("Malformed angle token: " + line);
                    }
                    if (!tokens[1].equalsIgnoreCase("deg") && !tokens[1].equalsIgnoreCase("rad")) {
                        throw new IOException("Invalid angle unit: " + tokens[1]);
                    }
                    anglesInDegrees = tokens[1].equalsIgnoreCase("deg");
                }
            } else if (section == FileSection.ROOT) {
                if (tokens[0].equals("order")) {
                    root.order = new DoF[tokens.length - 1];

                    for (int i = 0; i < root.order.length; i++) {
                        root.order[i] = DoF.byToken(tokens[i + 1]);
                    }
                } else if (tokens[0].equals("axis")) {
                    if (tokens.length != 2 || tokens[1].length() != 3) {
                        throw new IOException("Unexpected axis format: " + line);
                    }

                    root.axis = new DoF[3];
                    String axisOrder = tokens[1].toLowerCase();
                    for (int i = 0; i < 3; i++) {
                        if (axisOrder.charAt(i) == 'x') {
                            root.axis[i] = DoF.RX;
                        } else if (axisOrder.charAt(i) == 'y') {
                            root.axis[i] = DoF.RY;
                        } else if (axisOrder.charAt(i) == 'z') {
                            root.axis[i] = DoF.RZ;
                        } else {
                            throw new IOException("Unknown axis token: " + tokens[1]);
                        }
                    }
                } else if (tokens[0].equals("position")) {
                    if (tokens.length != 4) {
                        throw new IOException("Badly formed position specifier: " + line);
                    }
                    root.presetPosition = new Vector3(Double.parseDouble(tokens[1]),
                                                      Double.parseDouble(tokens[2]),
                                                      Double.parseDouble(tokens[3]));
                } else if (tokens[0].equals("orientation")) {
                    if (tokens.length != 4) {
                        throw new IOException("Badly formed orientation specifier: " + line);
                    }
                    root.presetOrientation = new Vector3(Double.parseDouble(tokens[1]),
                                                         Double.parseDouble(tokens[2]),
                                                         Double.parseDouble(tokens[3]));
                    toRadians(root.presetOrientation, anglesInDegrees);
                }
            } else if (section == FileSection.BONE_DATA) {
                if (currentBone != null) {
                    if (tokens[0].equals("name")) {
                        if (tokens.length != 2) {
                            throw new IOException("Malformed name token: " + line);
                        }
                        currentBone.name = tokens[1];
                    } else if (tokens[0].equals("direction")) {
                        if (tokens.length != 4) {
                            throw new IOException("Badly formed direction specifier: " + line);
                        }
                        currentBone.direction = new Vector3(Double.parseDouble(tokens[1]),
                                                            Double.parseDouble(tokens[2]),
                                                            Double.parseDouble(tokens[3]));
                        currentBone.direction.normalize();
                    } else if (tokens[0].equals("length")) {
                        if (tokens.length != 2) {
                            throw new IOException("Malformed length token: " + line);
                        }
                        currentBone.length = Double.parseDouble(tokens[1]);
                    } else if (tokens[0].equals("axis")) {
                        if (tokens.length != 5 || tokens[4].length() != 3) {
                            throw new IOException("Malformed axis token: " + line);
                        }

                        currentBone.axis = new DoF[3];
                        currentBone.axisRotation = new Vector3(Double.parseDouble(tokens[1]),
                                                               Double.parseDouble(tokens[2]),
                                                               Double.parseDouble(tokens[3]));
                        toRadians(currentBone.axisRotation, anglesInDegrees);

                        String axisOrder = tokens[4].toLowerCase();
                        for (int i = 0; i < 3; i++) {
                            if (axisOrder.charAt(i) == 'x') {
                                currentBone.axis[i] = DoF.RX;
                            } else if (axisOrder.charAt(i) == 'y') {
                                currentBone.axis[i] = DoF.RY;
                            } else if (axisOrder.charAt(i) == 'z') {
                                currentBone.axis[i] = DoF.RZ;
                            } else {
                                throw new IOException("Unknown axis token: " + tokens[1]);
                            }
                        }
                    } else if (tokens[0].equals("dof")) {
                        currentBone.motionDoF = new DoF[tokens.length - 1];

                        for (int i = 0; i < currentBone.motionDoF.length; i++) {
                            currentBone.motionDoF[i] = DoF.byToken(tokens[i + 1]);
                        }
                    } else if (tokens[0].equals("limits")) {
                        minLimits = new ArrayList<Double>();
                        maxLimits = new ArrayList<Double>();

                        String min, max;
                        if (tokens.length == 3) {
                            // tokens are ['limits', '(number', 'number)']
                            min = tokens[1].substring(1);
                            max = tokens[2].substring(0, tokens[2].length() - 1);
                        } else if (tokens.length == 2) {
                            // tokens are ['limits', '(number,number)']
                            String[] split = tokens[1].split(",");
                            min = split[0].substring(1);
                            max = split[1].substring(0, split[1].length() - 1);
                        } else {
                            throw new IOException("Malformed limits token: " + line);
                        }

                        parseLimits(min, max, minLimits, maxLimits);
                    } else if (tokens[0].startsWith("(")) {
                        if (minLimits == null) {
                            throw new IOException("Unexpected limit specifier: " + line);
                        }

                        String min, max;
                        if (tokens.length == 2) {
                            // tokens are ['(number', 'number)']
                            min = tokens[0].substring(1);
                            max = tokens[1].substring(0, tokens[1].length() - 1);
                        } else if (tokens.length == 1) {
                            // tokens are ['(number,number)']
                            String[] split = tokens[0].split(",");
                            min = split[0].substring(1);
                            max = split[1].substring(0, split[1].length() - 1);
                        } else {
                            throw new IOException("Malformed limits token: " + line);
                        }

                        parseLimits(min, max, minLimits, maxLimits);
                    } else if (tokens[0].equals("end")) {
                        // process accumulated limits text
                        if (currentBone.motionDoF != null) {
                            currentBone.minLimits = new double[currentBone.motionDoF.length];
                            currentBone.maxLimits = new double[currentBone.motionDoF.length];

                            if (minLimits != null) {
                                if (minLimits.size() != currentBone.motionDoF.length) {
                                    throw new IOException(
                                            "Bone does not have same limit count as degrees of freedom");
                                }

                                for (int i = 0; i < currentBone.motionDoF.length; i++) {
                                    if (anglesInDegrees && currentBone.motionDoF[i].isAngle()) {
                                        currentBone.minLimits[i] = Math.toRadians(minLimits.get(i));
                                        currentBone.maxLimits[i] = Math.toRadians(maxLimits.get(i));
                                    } else {
                                        currentBone.minLimits[i] = minLimits.get(i);
                                        currentBone.maxLimits[i] = maxLimits.get(i);
                                    }
                                }
                            } else {
                                // fill in infinite limits
                                for (int i = 0; i < currentBone.motionDoF.length; i++) {
                                    currentBone.minLimits[i] = Double.NEGATIVE_INFINITY;
                                    currentBone.maxLimits[i] = Double.POSITIVE_INFINITY;
                                }
                            }
                        }

                        boneData.add(currentBone);
                        currentBone = null;
                    }
                } else {
                    if (tokens[0].equals("begin")) {
                        currentBone = new ASFBone();
                        minLimits = null;
                        maxLimits = null;
                    }
                }
            } else if (section == FileSection.HIERARCHY) {
                if (!buildingHierarchy) {
                    if (tokens[0].equals("begin")) {
                        hierarchy = new HashMap<Object, List<ASFBone>>();
                        buildingHierarchy = true;
                    }
                } else {
                    if (tokens[0].equals("end")) {
                        // process the completed hierarchy to compute local
                        // transforms for each bone
                        computeLocalSpace();
                        buildingHierarchy = false;
                    } else {
                        // assume tokens represent hierarchy
                        if (tokens.length < 2) {
                            throw new IOException("Bad hierarchy definition: " + line);
                        }

                        Object parent = (tokens[0].equals("root") ? root : getBone(tokens[0]));
                        if (parent == null) {
                            throw new IOException("Parent bone does not exist: " + tokens[0]);
                        }
                        List<ASFBone> children = new ArrayList<ASFBone>();
                        for (int i = 1; i < tokens.length; i++) {
                            ASFBone child = getBone(tokens[i]);
                            if (child == null) {
                                throw new IOException("Child bone does not exist: " + tokens[i]);
                            }
                            children.add(child);
                        }

                        hierarchy.put(parent, children);
                    }
                }
            }
        }
    }

    private ASFBone getBone(String name) {
        for (ASFBone b : boneData) {
            if (b.name.equals(name)) {
                return b;
            }
        }
        return null;
    }

    private void toRadians(Vector3 angles, boolean inDegrees) {
        if (inDegrees) {
            angles.x = Math.toRadians(angles.x);
            angles.y = Math.toRadians(angles.y);
            angles.z = Math.toRadians(angles.z);
        }
        // else already in radians
    }

    private void parseLimits(String min, String max, List<Double> minLimits, List<Double> maxLimits) {
        if (min.equals("-inf")) {
            minLimits.add(Double.NEGATIVE_INFINITY);
        } else if (min.equals("inf")) {
            minLimits.add(Double.POSITIVE_INFINITY);
        } else {
            minLimits.add(Double.parseDouble(min));
        }

        if (max.equals("-inf")) {
            maxLimits.add(Double.NEGATIVE_INFINITY);
        } else if (max.equals("inf")) {
            maxLimits.add(Double.POSITIVE_INFINITY);
        } else {
            maxLimits.add(Double.parseDouble(max));
        }
    }

    private static enum FileSection {
        VERSION,
        NAME,
        UNITS,
        DOCUMENTATION,
        ROOT,
        BONE_DATA,
        HIERARCHY,
        SKIN
    }

    private static enum DoF {
        TX(false),
        TY(false),
        TZ(false),
        RX(true),
        RY(true),
        RZ(true),
        L(false);

        private final boolean angle;

        private DoF(boolean angle) {
            this.angle = angle;
        }

        public static DoF byToken(String token) throws IOException {
            for (DoF d : values()) {
                if (d.name().equalsIgnoreCase(token)) {
                    return d;
                }
            }
            throw new IOException("Unsupported/unknown DoF token: " + token);
        }

        public boolean isAngle() {
            return angle;
        }
    }

    private static class ASFRoot {
        private DoF[] order; // will not contain L
        private DoF[] axis; // will only contain RX, RY, and RZ
        private Vector3 presetPosition;
        private Vector3 presetOrientation;
    }

    private static class ASFBone {
        private String name;
        private Vector3 direction; // FIXME I think this might be in global as well
        private double length;

        private Vector3 axisRotation; // rotations about global axis
        private DoF[] axis; // axis order in above vector

        private Matrix3 childToParentBasis;
        private Vector3 localDirection; // direction converted to bone's coordinate frame

        private DoF[] motionDoF; // DoF's present in AMC files, won't be TX, TY, or TZ
        private double[] minLimits;
        private double[] maxLimits;
    }
}
