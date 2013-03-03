package com.ferox.anim;

import com.ferox.math.Matrix3;
import com.ferox.math.Matrix4;
import com.ferox.math.Quat4;
import com.ferox.math.Vector3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SkeletonAnimation {
    private final List<KeyFrame> frames;
    private boolean sortDirty;

    public SkeletonAnimation() {
        frames = new ArrayList<KeyFrame>();
    }

    public double getAnimationLength() {
        return (frames.isEmpty() ? 0 : frames.get(frames.size() - 1).getFrameTime());
    }

    public void addKeyFrame(KeyFrame frame) {
        frames.add(frame);
        sortDirty = true;
    }

    public void removeKeyFrame(KeyFrame frame) {
        frames.remove(frame);
    }

    public List<KeyFrame> getKeyFrames() {
        return Collections.unmodifiableList(frames);
    }

    public void updateSkeleton(Skeleton skeleton, double animationTime) {
        if (sortDirty) {
            Collections.sort(frames, new Comparator<KeyFrame>() {
                @Override
                public int compare(KeyFrame o1, KeyFrame o2) {
                    return Double.compare(o1.getFrameTime(), o2.getFrameTime());
                }
            });
            sortDirty = false;
        }

        int startFrame = getStartFrame(animationTime);

        Matrix3 upper = new Matrix3();
        Quat4 aRot = new Quat4();
        Quat4 bRot = new Quat4();
        Vector3 aPos = new Vector3();
        Vector3 bPos = new Vector3();

        for (Bone b : skeleton.getBones()) {
            int boneStartTarget = getStartFrame(startFrame, b.getName());
            int boneEndTarget = getEndFrame(startFrame, b.getName());

            double alpha = 0;
            if (boneStartTarget != boneEndTarget) {
                alpha = (animationTime - frames.get(boneStartTarget).getFrameTime()) /
                        (frames.get(boneEndTarget).getFrameTime() -
                         frames.get(boneStartTarget).getFrameTime());
            }

            Matrix4 aMat = frames.get(boneStartTarget).getBoneTransform(b.getName());
            Matrix4 bMat = frames.get(boneEndTarget).getBoneTransform(b.getName());

            if (aMat == null || bMat == null) {
                continue;
            }

            // rotation
            aRot.set(upper.setUpper(aMat));
            bRot.set(upper.setUpper(bMat));

            aRot.slerp(aRot, bRot, alpha);
            upper.set(aRot);

            // position
            aPos.set(aMat.m03, aMat.m13, aMat.m23);
            bPos.set(bMat.m03, bMat.m13, bMat.m23);

            aPos.scale(1 - alpha).add(bPos.scale(alpha));

            Matrix4 m = b.getRelativeBoneTransform();
            m.setUpper(upper);
            m.m03 = aPos.x;
            m.m13 = aPos.y;
            m.m23 = aPos.z;
            b.setRelativeBoneTransform(m);
        }
    }

    private int getStartFrame(double time) {
        // FIXME do a binary search
        for (int i = 0; i < frames.size() - 1; i++) {
            if (frames.get(i).getFrameTime() <= time &&
                frames.get(i + 1).getFrameTime() > time) {
                return i;
            }
        }
        return frames.size() - 1;
    }

    private int getStartFrame(int bestStartFrame, String bone) {
        for (int i = bestStartFrame; i > 0; i--) {
            if (frames.get(i).getBoneTransform(bone) != null) {
                return i;
            }
        }

        return 0;
    }

    private int getEndFrame(int bestStartFrame, String bone) {
        for (int i = bestStartFrame + 1; i < frames.size(); i++) {
            if (frames.get(i).getBoneTransform(bone) != null) {
                return i;
            }
        }

        return frames.size() - 1;
    }
}
