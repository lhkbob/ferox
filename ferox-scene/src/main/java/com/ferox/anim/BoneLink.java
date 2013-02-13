package com.ferox.anim;

import com.ferox.scene.Transform;
import com.lhkbob.entreri.ComponentData;
import com.lhkbob.entreri.Requires;
import com.lhkbob.entreri.property.ObjectProperty;

@Requires(Transform.class)
public class BoneLink extends ComponentData<BoneLink> {
    private ObjectProperty<Bone> linkedBone;

    private BoneLink() {}

    public Bone getLinkedBone() {
        return linkedBone.get(getIndex());
    }

    public BoneLink setLinkedBone(Bone bone) {
        linkedBone.set(bone, getIndex());
        return this;
    }
}
