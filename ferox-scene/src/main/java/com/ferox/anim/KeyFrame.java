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

import com.ferox.math.Const;
import com.ferox.math.Matrix4;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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
