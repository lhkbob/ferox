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
package com.ferox.math.mesh;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public final class MeshBuilder {
    private final Map<String, Integer> attrs;
    private int size;

    MeshBuilder() {
        attrs = new HashMap<>();
        size = -1;
    }

    public MeshBuilder addAttribute(String name, int elementSize) {
        if (elementSize < 1 || elementSize > 4)
            throw new IllegalArgumentException("Element size must be 1, 2, 3, or 4");
        if (name == null)
            throw new NullPointerException("Name cannot be null");
        attrs.put(name, elementSize);
        return this;
    }

    public MeshBuilder addAttributes(Mesh mesh) {
        for (String name: mesh.getAttributes()) {
            addAttribute(name, mesh.getAttributeSize(name));
        }
        return this;
    }

    public MeshBuilder addAttributes(TriangleIterator tris) {
        for (String name: tris.getAttributes()) {
            addAttribute(name, tris.getAttributeSize(name));
        }
        return this;
    }

    public MeshBuilder ensureSize(int count) {
        size = count;
        return this;
    }

    public Mesh build() {
        Mesh m = new Mesh(attrs);
        if (size > 0)
            m.ensureCapacity(size);
        return m;
    }
}
