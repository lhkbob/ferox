package com.ferox.anim;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.ferox.math.Const;
import com.ferox.math.Matrix4;

public class KeyFrame {
    private final double keyTime;
    private final Map<String, Matrix4> boneTransforms;

    private KeyFrame(double time, Map<String, Matrix4> transforms) {
        keyTime = time;
        boneTransforms = Collections.unmodifiableMap(transforms);
    }

    public double getFrameTime() {
        return keyTime;
    }

    @Const
    public Matrix4 getBoneTransform(String name) {
        return boneTransforms.get(name);
    }

    public Map<String, Matrix4> getBoneTransforms() {
        return boneTransforms;
    }

    public static Builder newKeyFrame(double keyTime) {
        return new Builder(keyTime);
    }

    public static class Builder {
        private final double keyTime;
        private final Map<String, Matrix4> boneTransforms;

        public Builder(double keyTime) {
            this.keyTime = keyTime;
            boneTransforms = new HashMap<String, Matrix4>();
        }

        public Builder setBone(String name, @Const Matrix4 transform) {
            boneTransforms.put(name, new Matrix4(transform));
            return this;
        }

        public KeyFrame build() {
            return new KeyFrame(keyTime, new HashMap<String, Matrix4>(boneTransforms));
        }
    }
}
