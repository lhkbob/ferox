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
