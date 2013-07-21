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

public class Bone {
    private final Matrix4 relativeBoneTransform;
    // FIXME should this actually be global? or just relative to the entity's transform
    // keep it global for now, we can make skinning work around that (on GPU or CPU),
    // and it works a lot better with setting bones to be physics objects
    private final Matrix4 globalBoneTransform;
    private final String name;

    public Bone(String name) {
        this.name = name;
        relativeBoneTransform = new Matrix4().setIdentity();
        globalBoneTransform = new Matrix4();
    }

    @Const
    public Matrix4 getRelativeBoneTransform() {
        return relativeBoneTransform;
    }

    public void setRelativeBoneTransform(@Const Matrix4 relativeBoneTransform) {
        this.relativeBoneTransform.set(relativeBoneTransform);
    }

    @Const
    public Matrix4 getGlobalBoneTransform() {
        return globalBoneTransform;
    }

    public void setGlobalBoneTransform(@Const Matrix4 globalBoneTransform) {
        this.globalBoneTransform.set(globalBoneTransform);
    }

    public String getName() {
        return name;
    }
}
