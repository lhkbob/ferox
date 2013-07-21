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

import com.ferox.scene.Transform;
import com.lhkbob.entreri.ComponentData;
import com.lhkbob.entreri.Requires;
import com.lhkbob.entreri.property.ObjectProperty;

import java.util.*;

@Requires(Transform.class)
public class Skeleton extends ComponentData<Skeleton> {
    // FIXME for templating to work correctly, this will need a custom
    // factory that clones properly
    private ObjectProperty<Map<String, Bone>> bones;
    private ObjectProperty<Map<Bone, List<Bone>>> parentToChildHierarchy;
    private ObjectProperty<Map<Bone, Bone>> childToParentHierarchy;

    private ObjectProperty<Bone> root;

    private Skeleton() {
    }

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
