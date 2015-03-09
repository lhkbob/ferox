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

import com.ferox.math.Const;
import com.ferox.math.Vector3;
import com.ferox.math.Vector4;

import java.util.*;

/**
 *
 */
public final class Mesh implements Iterable<Triangle> {
    private static final int PAGE_SIZE = 8;
    private static final int PAGE_DATA_START = 1;
    private static final int PAGE_NEXT = PAGE_SIZE - 1;

    private static final Triangle.Vertex[] VERTICES = Triangle.Vertex.values();

    // empty value in connection vertex map if the index is less than nextSet (after nextSet all values are invalid)
    private static final int INVALID = ~0;

    private final int attrCount;
    private final Map<String, Integer> attrIndices;
    private final int[] attrElementSize;

    float[] vertices; // 9 x size of triangles array
    final float[][] attrs; // 3 x elementSize x size of tri array
    private int[] connectionVertexMap; // 3 x size of tri array
    private Triangle[] triangles;

    private int[] connectionSets;

    private Queue<Integer> emptyTriangles;
    private Queue<Integer> emptyPages;

    private int nextTriangle;
    private int nextSet;

    // Mesh is not a thread safe structure, so it's okay to have it hold onto temp objects
    // used for per-triangle computations
    final Vector3 temp1 = new Vector3();
    final Vector3 temp2 = new Vector3();

    Mesh(Map<String, Integer> attributes) {
        attrCount = attributes.size();

        attrElementSize = new int[attrCount];
        Map<String, Integer> indices = new HashMap<>();
        int index = 0;
        for (Map.Entry<String, Integer> e : attributes.entrySet()) {
            indices.put(e.getKey(), index);
            attrElementSize[index] = e.getValue();
            index++;
        }

        attrIndices = Collections.unmodifiableMap(indices);

        vertices = new float[9];
        attrs = new float[attrCount][];
        for (int i = 0; i < attrCount; i++) {
            attrs[i] = new float[3 * attrElementSize[i]];
        }
        connectionVertexMap = new int[3];
        triangles = new Triangle[1];

        connectionSets = new int[PAGE_SIZE];
        emptyPages = null;
        emptyTriangles = null;

        nextTriangle = 0;
        nextSet = 0;
    }

    public static MeshBuilder newBuilder() {
        return new MeshBuilder();
    }

    public void optimize() {

    }

    public void ensureCapacity(int size) {
        if (size > triangles.length) {
            setDataCapacity(size);
        }
    }

    private void setDataCapacity(int size) {
        triangles = Arrays.copyOf(triangles, size);
        vertices = Arrays.copyOf(vertices, size * 9);
        connectionVertexMap = Arrays.copyOf(connectionVertexMap, size * 3);
        for (int i = 0; i < attrCount; i++) {
            attrs[i] = Arrays.copyOf(attrs[i], size * 3 * attrElementSize[i]);
        }
    }

    public Triangle addTriangle() {
        // get index of new triangle, either reusing storage or appending
        int triIndex;
        if (emptyTriangles == null) {
            triIndex = nextTriangle++;
        } else {
            triIndex = emptyTriangles.poll();
            if (emptyTriangles.isEmpty()) {
                emptyTriangles = null;
            }
        }

        // grow backing data if necessary
        if (triIndex >= triangles.length) {
            setDataCapacity(2 * triIndex);
        }

        // initialize all fields of the triangle to meaningful data
        Arrays.fill(vertices, triIndex * 9, (triIndex + 1) * 9, 0f);
        for (int i = 0; i < attrCount; i++) {
            int size = attrElementSize[i];
            Arrays.fill(attrs[i], triIndex * 3 * size, (triIndex + 1) * 3 * size, 0f);
        }
        Arrays.fill(connectionVertexMap, triIndex * 3, (triIndex + 1) * 3, INVALID);

        Triangle t = new Triangle(this, triIndex);
        triangles[triIndex] = t;
        return t;
    }

    private void validateTriangle(Triangle t) {
        if (t.mesh != this) {
            throw new IllegalArgumentException("Triangle does not belong to this mesh");
        }
        if (t.index >= triangles.length || triangles[t.index] != t) {
            throw new IllegalArgumentException("Triangle does not belong to this mesh"); // already removed
        }
    }

    public void removeTriangle(Triangle t) {
        validateTriangle(t);

        detachVertexUnsafe(t, Triangle.Vertex.A);
        detachVertexUnsafe(t, Triangle.Vertex.B);
        detachVertexUnsafe(t, Triangle.Vertex.C);

        triangles[t.index] = null;
        if (emptyTriangles == null) {
            emptyTriangles = new ArrayDeque<>();
        }
        emptyTriangles.add(t.index);
        // don't bother clearing attribute state, it will be overwritten when a new triangle is added to it
    }

    public void detachVertex(Triangle t, Triangle.Vertex v) {
        validateTriangle(t);
        detachVertexUnsafe(t, v);
    }

    private boolean isPageValid(int page) {
        return page >= 0 && page < connectionSets.length - PAGE_SIZE && connectionSets[page] != INVALID;
    }

    private void detachVertexUnsafe(Triangle t, Triangle.Vertex v) {
        int id = toIndex(t, v);
        int set = connectionVertexMap[id];
        connectionVertexMap[id] = INVALID;

        // iterate through linked pages until the vertex is found
        int priorPage = INVALID;
        while (isPageValid(set)) {
            int nextPage = connectionSets[set + PAGE_NEXT];

            for (int i = set + PAGE_DATA_START; i < set + PAGE_NEXT; i++) {
                if (connectionSets[i] == id) {
                    // found it so remove and possibly clean up the page
                    connectionSets[i] = INVALID;
                    connectionSets[set]--; // decrement size

                    if (connectionSets[set] == 0) {
                        // remove the page since nothing else is in it (it is possible that there is only
                        // 1 total vertex remaining in the other pages of the set, in which case we could
                        // clean it up, but that is a lot of book-keeping)
                        removePage(set, priorPage, nextPage);
                    } // else at least 2 vertices still connected together after the removal
                    return;
                }
            }
            priorPage = set;
            set = nextPage;
        }
    }

    private void removePage(int page, int priorPage, int nextPage) {
        connectionSets[page] = INVALID;
        if (emptyPages == null) {
            emptyPages = new ArrayDeque<>();
        }
        emptyPages.add(page);
        if (isPageValid(priorPage)) {
            connectionSets[priorPage + PAGE_NEXT] = nextPage;
        }
    }

    public void attachVertex(Triangle t1, Triangle.Vertex v1, Triangle t2, Triangle.Vertex v2) {
        validateTriangle(t1);
        validateTriangle(t2);

        if (t1.index == t2.index && v1 == v2) {
            // this is the same vertex of the same triangle, don't bother changing the system, since
            // the set connected to a vertex always excludes itself
            return;
        }

        int id1 = toIndex(t1, v1);
        int id2 = toIndex(t2, v2);
        int set1 = connectionVertexMap[id1];
        int set2 = connectionVertexMap[id2];

        if (isPageValid(set1) && isPageValid(set2)) {
            // need to merge two sets together, one page has its elements removed while simultaneously
            // adding them to the other set's pages
            int[] tempPage = new int[PAGE_SIZE];
            while (isPageValid(set1)) {
                // copy page data into temporary storage and reclaim page immediately
                System.arraycopy(connectionSets, set1, tempPage, 0, PAGE_SIZE);
                removePage(set1, INVALID, INVALID); // don't bother patching list, they're all going away
                for (int i = 1; i < PAGE_NEXT; i++) {
                    if (tempPage[i] != INVALID) {
                        // add to set2
                        addVertexToSet(set2, tempPage[i]);
                    }
                }
                set1 = tempPage[PAGE_NEXT];
            }
        } else if (isPageValid(set2)) {
            // need to add t1:v1 to set2, make sure that t1 is not already in set2
            addVertexToSet(set2, id1);
        } else if (isPageValid(set1)) {
            // need to add t2:v2 to set1, make sure that t2 is not already in set1
            addVertexToSet(set1, id2);
        } else {
            // need to make a new set page and add id1 and id2 to it
            int newPage = newPage();
            connectionSets[newPage] = 2;
            connectionSets[newPage + PAGE_DATA_START] = id1;
            connectionSets[newPage + PAGE_DATA_START + 1] = id2;

            connectionVertexMap[id1] = newPage;
            connectionVertexMap[id2] = newPage;
        }
    }

    int triangleFromVertex(int id) {
        return id / 3;
    }

    int toIndex(Triangle t, Triangle.Vertex v) {
        return t.index * 3 + v.ordinal();
    }

    int elementSize(int attrIndex) {
        return attrElementSize[attrIndex];
    }

    public int getAttributeSize(String name) {
        return elementSize(getAttributeIndex(name));
    }

    private void addVertexToSet(int set, int id) {
        int insertIndex = Integer.MAX_VALUE;
        int insertPage = INVALID;

        int priorPage = INVALID;
        while (isPageValid(set)) {
            for (int i = set + PAGE_DATA_START; i < set + PAGE_NEXT; i++) {
                if (connectionSets[i] == INVALID) {
                    // record lowest open index for later assignment
                    insertIndex = Math.min(insertIndex, i);
                    insertPage = set;
                } else {
                    // check if id is present
                    if (connectionSets[i] == id) {
                        return; // already in the set don't add it
                    }
                }
            }
            // move to next page even if we found an index, because we search entire set for
            // invalid triangle connections
            priorPage = set;
            set = connectionSets[set + PAGE_NEXT];
        }

        if (isPageValid(insertPage)) {
            // record id into the selected page
            connectionSets[insertIndex] = id;
            connectionSets[insertPage]++;
        } else {
            // current pages are filled up so add a new page and link prior page to it
            int newPage = newPage();
            connectionSets[priorPage + PAGE_NEXT] = newPage;
            connectionSets[newPage] = 1; // only 1 vertex
            connectionSets[newPage + PAGE_DATA_START] = id;
        }

        connectionVertexMap[id] = set;
    }

    private int newPage() {
        int pageId;
        if (emptyPages == null) {
            pageId = PAGE_SIZE * (nextSet++);
        } else {
            pageId = emptyPages.poll();
            if (emptyPages.isEmpty()) {
                emptyPages = null;
            }
        }

        // initialize data structure for page
        Arrays.fill(connectionSets, pageId + PAGE_DATA_START, pageId + PAGE_SIZE, INVALID);
        connectionSets[pageId] = 0; // valid but empty
        return pageId;
    }

    @Override
    public Iterator<Triangle> iterator() {
        return new MeshTriangleIterator();
    }

    public TriangleIterator fastIterator() {
        return new FastMeshTriangleIterator();
    }

    public Map<Triangle, Triangle.Vertex> getConnected(Triangle t, Triangle.Vertex v) {
        validateTriangle(t);
        int id = toIndex(t, v);
        int set = connectionVertexMap[id];

        Map<Triangle, Triangle.Vertex> neighbors = new HashMap<>();
        while (isPageValid(set)) {
            for (int i = set + PAGE_DATA_START; i < PAGE_NEXT; i++) {
                if (connectionSets[i] != INVALID && connectionSets[i] != id) {
                    int nId = triangleFromVertex(connectionSets[i]);
                    int vId = connectionSets[i] - nId * 3;
                    neighbors.put(triangles[nId], VERTICES[vId]);
                }
            }
            set = connectionSets[set + PAGE_NEXT];
        }

        return neighbors;
    }

    public int getAttributeIndex(String name) {
        return attrIndices.get(name);
    }

    public Set<String> getAttributes() {
        return attrIndices.keySet();
    }

    private class MeshTriangleIterator implements Iterator<Triangle> {
        private int index;
        private boolean advanced;

        private MeshTriangleIterator() {
            index = -1;
            advanced = false;
        }

        @Override
        public boolean hasNext() {
            if (!advanced) {
                advance();
            }
            return index < triangles.length;
        }

        @Override
        public Triangle next() {
            if (!advanced) {
                advance();
            }
            if (index >= triangles.length) {
                throw new NoSuchElementException();
            }
            advanced = false;
            return triangles[index];
        }

        private void advance() {
            do {
                index++;
            } while (index < triangles.length && triangles[index] == null);
            advanced = true;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Removal isn't supported");
        }
    }

    private class FastMeshTriangleIterator implements TriangleIterator {
        private final Vector3 pos;
        private final Vector4[] attr;

        private int index;

        private FastMeshTriangleIterator() {
            index = -1;
            pos = new Vector3();
            attr = new Vector4[attrElementSize.length];
            for (int i = 0; i < attr.length; i++) {
                attr[i] = new Vector4();
            }
        }

        @Override
        public boolean next() {
            do {
                index++;
            } while (index < triangles.length && triangles[index] == null);
            return index < triangles.length;
        }

        @Override
        @Const
        public Vector3 getPosition(Triangle.Vertex v) {
            return pos.set(vertices, index * 9 + v.ordinal());
        }

        @Override
        @Const
        public Vector4 getAttribute(String name, Triangle.Vertex v) {
            return getAttribute(getAttributeIndex(name), v);
        }

        @Override
        @Const
        public Vector4 getAttribute(int attrIndex, Triangle.Vertex v) {
            Vector4 r = attr[attrIndex];
            int size = attrElementSize[attrIndex];
            int baseIndex = index * size * 3 + v.ordinal();
            switch (size) {
            case 4:
                r.w = attrs[attrIndex][baseIndex + 3];
            case 3:
                r.z = attrs[attrIndex][baseIndex + 2];
            case 2:
                r.y = attrs[attrIndex][baseIndex + 1];
            case 1:
                r.x = attrs[attrIndex][baseIndex];
            }
            return r;
        }

        @Override
        public Set<String> getAttributes() {
            return Mesh.this.getAttributes();
        }

        @Override
        public int getAttributeIndex(String name) {
            return Mesh.this.getAttributeIndex(name);
        }

        @Override
        public int getAttributeSize(String name) {
            return Mesh.this.getAttributeSize(name);
        }
    }


    // IDEAS FOR EFFICIENT REPRESENTATION:
    // 1. triangles are 9 floats packed together, + parallel arrays for generic attrs
    // 2. each triangle has its id as index into packed array
    // 3. kd tree stores triangle index with vertex mask in upper 2 bits for the leaf nodes
    // 4. a kd branch node needs to know its axis, its split position (float), and the location of its
    //    two children. PBRT optimizes this because it recursively builds left and then right trees,
    //    so it knows that IF left child exists, then its the immediate next node in list of nodes, and can
    //    then use 29 bits for right child index (2 bits for axis, 1 bit for left presence, 29 for right).
    //5. With that, we're at 8 bytes per node. That's pretty good.  If we consider a 10,000,000 triangle model,
    // that 10,000,000 leaf nodes and 2^23 intermediate nodes (maybe 2^24), either way that's probably another
    // 10,000,000 so we're looking at 20mil * 8 bytes = 160mil ~160 MB to store the tree into a packed int array.
    // that's pretty reasonable IMO


    // Node data structure:
    // - 2 bits for state (x, y, z axis or leaf)
    // when not a leaf
    // - 1 bit for left/right child when not a leaf
    // - 29 bits for right child index
    // - 32 bits for floating point split value
    //
    // when a leaf
    // - 2 bits for vertex encoding
    // - 28 bits for triangle index
    // - 32 bits for floating point location


    // FIXME a question to address with this, element per vertex instead of the entire triangle is how
    // would we implement ray intersection queries quickly, and would it still support triangle-triangle
    // intersection queries?
    //
    // PBRT actually has two kd tree implementations, one that stores point data (kdtree.h) and one that
    // stores triangle primitives (kdtreeaccel.h). the structure of the accelerator seems less optimized
    // or compact, but I haven't analyzed if that is mandatory if it has to hold triangles.
    //
    // An important point about storing triangles completely is that it may not be possible to get a clean
    // split where each triangle is in the left or right split. This requires that bisected triangles
    // get stored at the intermediate node and not at the leafs, or it splits the triangles (not good).
    //
    // I bet you could still get a good fitted overestimate about the size of a kd tree if you stored
    // N ids at intermediate nodes. Worst case, you have an intermediate node per triangle somehow. Otherwise
    // you have some nodes put at the same intermediate node, or stored in the leafs (where multiple
    // tris exist in each leaf). You can allocate once and then trim to fit when you're done.
    // Storing a tri into a node is cheaper than allocating a whole new node for it, since the node header
    // is not required. When you make a split, do you always have to consume a triangle? Yes, otherwise
    // you produce nodes that don't have any triangles and then you have no limit to hoe deep the
    // tree could get. The code would need to be smart though and see if it reached the very end of the
    // geometry and not attempt to allocate another level, or if it reached the end of levels, just
    // add it to the leaf (but don't compute size based on 2^level, since that's not optimal)
}
