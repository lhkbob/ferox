package com.ferox.anim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ferox.scene.Transform;
import com.lhkbob.entreri.ComponentData;
import com.lhkbob.entreri.Requires;
import com.lhkbob.entreri.property.ObjectProperty;

@Requires(Transform.class)
public class Skeleton extends ComponentData<Skeleton> {
    // FIXME for templating to work correctly, this will need a custom
    // factory that clones properly
    private ObjectProperty<Map<String, Bone>> bones;
    private ObjectProperty<Map<Bone, List<Bone>>> parentToChildHierarchy;
    private ObjectProperty<Map<Bone, Bone>> childToParentHierarchy;

    private ObjectProperty<Bone> root;

    private Skeleton() {}

    public Skeleton addBone(Bone bone) {
        Bone old = getBoneMap().put(bone.getName(), bone);
        if (old != null) {
            removeBone(old);
        }
        return this;
    }

    public Collection<Bone> getBones() {
        return getBoneMap().values();
    }

    public Bone getRootBone() {
        return root.get(getIndex());
    }

    public Skeleton setRootBone(Bone bone) {
        if (getBoneMap().get(bone.getName()) != bone) {
            throw new IllegalArgumentException("Bone is not in skeleton");
        }
        root.set(bone, getIndex());
        return this;
    }

    public List<Bone> getChildren(Bone bone) {
        // FIXME read-only
        return getParentToChildMap().get(bone);
    }

    public Skeleton removeBone(Bone bone) {
        Map<String, Bone> boneMap = getBoneMap();
        Map<Bone, Bone> childToParent = getChildToParentMap();
        Map<Bone, List<Bone>> parentToChild = getParentToChildMap();

        // perform check to make sure we're not removing a bone
        // instance with the same name that is replacing this bone
        if (boneMap.get(bone.getName()) == bone) {
            boneMap.remove(bone.getName());
        }

        // remove bone from child-to-parent map

        Bone parent = childToParent.remove(bone);
        if (parent != null) {
            // remove child from its parent's list of child bones
            List<Bone> neighbors = parentToChild.get(parent);
            neighbors.remove(bone);
        }

        // remove all of bone's children from the child-to-parent map,
        // since they no longer have a parent
        List<Bone> children = parentToChild.remove(bone);
        if (children != null) {
            for (int i = 0; i < children.size(); i++) {
                childToParent.remove(children.get(i));
            }
        }

        return this;
    }

    public Skeleton connect(Bone parent, Bone child) {
        Map<String, Bone> boneMap = getBoneMap();
        Map<Bone, Bone> childToParent = getChildToParentMap();
        Map<Bone, List<Bone>> parentToChild = getParentToChildMap();

        if (boneMap.get(parent.getName()) != parent) {
            throw new IllegalArgumentException("Parent bone is not in this skeleton");
        }
        if (boneMap.get(child.getName()) != child) {
            throw new IllegalArgumentException("Child bone is not in this skeleton");
        }

        // disconnect child from its current parent
        Bone oldParent = childToParent.remove(child);
        if (oldParent != null) {
            parentToChild.get(oldParent).remove(child);
        }

        // attach child to the new parent bone
        childToParent.put(child, parent);
        List<Bone> children = parentToChild.get(parent);
        if (children == null) {
            children = new ArrayList<Bone>();
            parentToChild.put(parent, children);
        }
        children.add(child);

        return this;
    }

    public Bone getBone(String name) {
        return getBoneMap().get(name);
    }

    private Map<String, Bone> getBoneMap() {
        Map<String, Bone> boneMap = bones.get(getIndex());
        if (boneMap == null) {
            boneMap = new HashMap<String, Bone>();
            bones.set(boneMap, getIndex());
        }

        return boneMap;
    }

    private Map<Bone, Bone> getChildToParentMap() {
        Map<Bone, Bone> boneMap = childToParentHierarchy.get(getIndex());
        if (boneMap == null) {
            boneMap = new HashMap<Bone, Bone>();
            childToParentHierarchy.set(boneMap, getIndex());
        }

        return boneMap;
    }

    private Map<Bone, List<Bone>> getParentToChildMap() {
        Map<Bone, List<Bone>> boneMap = parentToChildHierarchy.get(getIndex());
        if (boneMap == null) {
            boneMap = new HashMap<Bone, List<Bone>>();
            parentToChildHierarchy.set(boneMap, getIndex());
        }

        return boneMap;
    }
}
