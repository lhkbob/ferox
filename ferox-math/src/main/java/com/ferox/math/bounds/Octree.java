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
package com.ferox.math.bounds;

import com.ferox.math.AxisAlignedBox;
import com.ferox.math.Const;
import com.ferox.math.Functions;
import com.ferox.math.Vector3;
import com.ferox.math.bounds.Frustum.FrustumIntersection;

import java.util.Arrays;

/**
 * <p/>
 * Octree is a SpatialIndex implementation that uses a octree to efficiently cull areas in the viewing frustum
 * that don't have objects within them. This particular implementation is a fully-allocated octree on top of a
 * spatial grid. This means that inserts are almost constant time, and that AABB queries are very fast.
 * <p/>
 * Unless all three dimensions are required to suitably index the space, a {@link QuadTree} will generally
 * perform faster and use less memory.
 *
 * @param <T> The data type stored in the octree
 *
 * @author Michael Ludwig
 */
public class Octree<T> implements SpatialIndex<T> {
    private static final int POS_X = 0x1;
    private static final int POS_Y = 0x2;
    private static final int POS_Z = 0x4;

    // complete octree nodes, keyed by hashed node ids packed into bits
    // - values are the number of children in each node
    private final int[] octree;
    private final int depth;

    // grid of leaf nodes
    private final Cell[] spatialHash;
    private final int maxCellDimension;

    private double widthScaleFactor;
    private double heightScaleFactor;
    private double depthScaleFactor;

    private double widthOffset;
    private double heightOffset;
    private double depthOffset;

    private final AxisAlignedBox rootBounds;

    // items in the octree
    private Object[] elements;
    private int[] queryIds;
    private double[] aabbs;
    private int size;

    private int queryIdCounter;

    /**
     * Construct a new Octree that has X, Y, and Z dimensions of 100, and an estimated object size of 2 units,
     * which allows the tree to have a depth of 6.
     */
    public Octree() {
        this(100, 2.0);
    }

    /**
     * <p/>
     * Construct a new Octree that has X, Y, and Z dimensions equal to <var>sideLength</var>, and the depth of
     * the tree is controlled by the estimated object size, <var>objSize</var>. <var>objSize</var> should be
     * the approximate dimension of the average object contained in this index. If it is too big or too small,
     * query performance may suffer.
     *
     * @param sideLength The side length of the root bounds of the octree
     * @param objSize    The estimated object size
     */
    public Octree(double sideLength, double objSize) {
        this(new AxisAlignedBox(new Vector3(-sideLength / 2.0, -sideLength / 2.0, -sideLength / 2.0),
                                new Vector3(sideLength / 2.0, sideLength / 2.0, sideLength / 2.0)),
             Functions.log2((int) Math.ceil(sideLength / objSize)));
    }

    /**
     * <p/>
     * Construct a new Octree with the given root bounds, <var>aabb</var> and tree depth. The depth of the
     * tree and the X, Y, and Z dimensions of the root bounds determine the size the leaf octree nodes. If
     * objects are significantly larger than this, they will be contained in multiple nodes and it may hurt
     * performance.
     * <p/>
     * <p/>
     * The root bounds are copied so future changes to <var>aabb</var> will not affect this tree.
     *
     * @param aabb  The root bounds of the tree
     * @param depth The depth of the tree
     *
     * @throws NullPointerException if aabb is null
     */
    public Octree(@Const AxisAlignedBox aabb, int depth) {
        this.depth = depth;
        rootBounds = new AxisAlignedBox();

        maxCellDimension = 1 << (depth - 1);
        spatialHash = new Cell[maxCellDimension * maxCellDimension * maxCellDimension];
        elements = new Object[8];
        queryIds = new int[8];
        aabbs = new double[48];
        size = 0;
        queryIdCounter = 0;

        setExtent(aabb);

        int numNodes = getLevelOffset(depth);
        octree = new int[numNodes];

        // mark quadtree leaves with negative indices, so that indices
        // can be computed lazily later
        int leafOffset = getLevelOffset(depth - 1);
        Arrays.fill(octree, leafOffset, octree.length, -1);
    }

    @Override
    @Const
    public AxisAlignedBox getExtent() {
        return rootBounds;
    }

    @Override
    public void setExtent(AxisAlignedBox bounds) {
        if (size > 0) {
            throw new IllegalStateException("Index is not empty");
        }

        rootBounds.set(bounds);

        widthScaleFactor = maxCellDimension / (rootBounds.max.x - rootBounds.min.x);
        heightScaleFactor = maxCellDimension / (rootBounds.max.y - rootBounds.min.y);
        depthScaleFactor = maxCellDimension / (rootBounds.max.z - rootBounds.min.z);

        widthOffset = -rootBounds.min.x;
        heightOffset = -rootBounds.min.y;
        depthOffset = -rootBounds.min.z;
    }

    @Override
    public boolean remove(T element) {
        if (element == null) {
            throw new NullPointerException("Item cannot be null");
        }

        int item = -1;
        for (int i = 0; i < size; i++) {
            if (elements[i] == element) {
                item = i;
                break;
            }
        }

        if (item >= 0) {
            // item is in the tree, so remove it
            if (removeItem(item)) {
                // the old item has been swapped with the tail, so we need to
                // update references to the tail
                updateItemIndex(size - 1, item);
            }
            size--;
            return true;
        } else {
            return false;
        }
    }

    /*
     * Update cell references to oldIndex to point to toIndex (e.g. when an
     * element has been swapped because original value for toIndex was removed)
     */
    private void updateItemIndex(int oldIndex, int toIndex) {
        // do an aabb query using the last known aabb state so that we
        // limit the number of cells considered
        Vector3 t = new Vector3();
        int minX = hashCellX(t.set(aabbs, toIndex * 6));
        int minY = hashCellY(t); // t still holds the min vector
        int minZ = hashCellZ(t);
        int maxX = hashCellX(t.set(aabbs, toIndex * 6 + 3));
        int maxY = hashCellY(t); // t still holds the max vector
        int maxZ = hashCellZ(t);

        Cell cell;
        for (int z = minZ; z <= maxZ; z++) {
            for (int y = minY; y <= maxY; y++) {
                for (int x = minX; x <= maxX; x++) {
                    cell = spatialHash[hash(x, y, z)];
                    if (cell != null) {
                        // iterate through keys and search for old one
                        for (int i = 0; i < cell.size; i++) {
                            if (cell.keys[i] == oldIndex) {
                                cell.keys[i] = toIndex;
                            }
                        }
                    }
                }
            }
        }
    }

    /*
     * Remove the given item index from the elements bag, clean up cell
     * references and update the quadtree. Does not update size
     */
    private boolean removeItem(int index) {
        // do an aabb query using the last known aabb state so that we
        // limit the number of cells considered
        Vector3 t = new Vector3();
        int minX = hashCellX(t.set(aabbs, index * 6));
        int minY = hashCellY(t); // t still holds the min vector
        int minZ = hashCellZ(t);
        int maxX = hashCellX(t.set(aabbs, index * 6 + 3));
        int maxY = hashCellY(t); // t still holds the max vector
        int maxZ = hashCellZ(t);

        Cell cell;
        for (int z = minZ; z <= maxZ; z++) {
            for (int y = minY; y <= maxY; y++) {
                for (int x = minX; x <= maxX; x++) {
                    cell = spatialHash[hash(x, y, z)];
                    if (cell != null) {
                        // remove cell's reference to index, and update quadtree
                        // counts
                        cell.remove(this, index);
                    }
                }
            }
        }

        // swap the last element with this one if it's not already the last item
        if (index < size - 1) {
            int swap = size - 1;
            elements[index] = elements[swap];
            queryIds[index] = queryIds[swap];
            System.arraycopy(aabbs, swap * 6, aabbs, index * 6, 6);

            // must also null the old element index since that won't get
            // iterated over during a non-fast clear anymore
            elements[swap] = null;
            return true;
        } else {
            // return false to signal that no further clean up is necessary
            elements[index] = null; // to help gc
            return false;
        }
    }

    @Override
    public boolean add(T element, @Const AxisAlignedBox bounds) {
        if (element == null) {
            throw new NullPointerException("Item cannot be null");
        }
        if (bounds == null) {
            throw new NullPointerException("Item bounds cannot be null");
        }

        if (!rootBounds.contains(bounds)) {
            return false; // skip the element
        }

        // add the item to the item list
        int itemIndex = size;
        if (itemIndex == elements.length) {
            // grow items
            int newSize = (int) (itemIndex * 1.5);
            elements = Arrays.copyOf(elements, newSize);
            queryIds = Arrays.copyOf(queryIds, newSize);
            aabbs = Arrays.copyOf(aabbs, newSize * 6);
        }
        elements[itemIndex] = element;
        queryIds[itemIndex] = 0;

        bounds.min.get(aabbs, itemIndex * 6);
        bounds.max.get(aabbs, itemIndex * 6 + 3);

        size++;

        // we know these hashes will be within the valid cells, because the
        // object is fully contained in the root bounds
        int minX = hashCellX(bounds.min);
        int minY = hashCellY(bounds.min);
        int minZ = hashCellZ(bounds.min);
        int maxX = hashCellX(bounds.max);
        int maxY = hashCellY(bounds.max);
        int maxZ = hashCellZ(bounds.max);

        int hash;
        Cell cell;
        for (int z = minZ; z <= maxZ; z++) {
            for (int y = minY; y <= maxY; y++) {
                for (int x = minX; x <= maxX; x++) {
                    hash = hash(x, y, z);
                    cell = spatialHash[hash];
                    if (cell == null) {
                        int qtIndex = getQuadLeafFromCell(x, y, z);
                        cell = new Cell(qtIndex);
                        spatialHash[hash] = cell;

                        // also update the quad tree to point to this cell
                        qtIndex += getLevelOffset(depth - 1);
                        if (octree[qtIndex] != -1) {
                            throw new IllegalArgumentException("Quadtree leaf should not have any index");
                        }
                        octree[qtIndex] = hash;
                    }
                    cell.add(this, itemIndex);
                }
            }
        }
        return true;
    }

    @Override
    public void clear() {
        clear(false);
    }

    @Override
    public void clear(boolean fast) {
        // fill quadtree counts with 0s, but only up to the leaf nodes because
        // they hold cell indices, which don't change
        int leafStartIndex = getLevelOffset(depth - 1);
        Arrays.fill(octree, 0, leafStartIndex, 0);

        // clear spatial hash
        Cell c;
        int leafOffset = getLevelOffset(depth - 1);
        for (int i = 0; i < spatialHash.length; i++) {
            c = spatialHash[i];
            if (c != null) {
                c.lifetime++;

                // check lifetime to help with GC'ing
                if (c.lifetime > Cell.MAX_LIFETIME && c.size == 0) {
                    // clear cell so that its contents get GC'ed
                    spatialHash[i] = null;
                    octree[leafOffset + c.octreeIndex] = -1;
                }

                // only need to reset the size variable
                c.size = 0;
            }
        }

        // empty global item bag
        if (!fast) {
            // must null elements for gc purposes, we do the entire array in
            // case elements got trapped at the end during a previous fast clear
            Arrays.fill(elements, null);
        }
        size = 0;
        queryIdCounter = 0;
    }

    public static long intersectionCount = 0;
    public static long maxCellCount = 0;
    public static long usedCellCount = 0;

    @Override
    @SuppressWarnings("unchecked")
    public void query(IntersectionCallback<T> callback) {
        if (callback == null) {
            throw new NullPointerException("Callback cannot be null");
        }

        AxisAlignedBox ba = new AxisAlignedBox();
        AxisAlignedBox bb = new AxisAlignedBox();
        Vector3 minIntersect = new Vector3();

        intersectionCount = 0;
        maxCellCount = 0;
        usedCellCount = 0;

        // iterate over all cells
        Cell cell;
        for (int cellZ = 0; cellZ < maxCellDimension; cellZ++) {
            for (int cellY = 0; cellY < maxCellDimension; cellY++) {
                for (int cellX = 0; cellX < maxCellDimension; cellX++) {
                    maxCellCount++;
                    cell = spatialHash[hash(cellX, cellY, cellZ)];
                    if (cell != null) {
                        usedCellCount++;

                        // do an N^2 iteration over items within cell
                        for (int a = 0; a < cell.size; a++) {
                            updateBounds(ba, cell.keys[a]);

                            for (int b = a + 1; b < cell.size; b++) {
                                intersectionCount++;
                                updateBounds(bb, cell.keys[b]);

                                if (ba.intersects(bb)) {
                                    // to remove duplicate checks we enforce that
                                    // the intersection geometry is in the minimum cell
                                    minIntersect
                                            .set(Math.max(ba.min.x, bb.min.x), Math.max(ba.min.y, bb.min.y),
                                                 Math.max(ba.min.z, bb.min.z));
                                    if (hashCellX(minIntersect) != cellX ||
                                        hashCellY(minIntersect) != cellY ||
                                        hashCellZ(minIntersect) != cellZ) {
                                        continue;
                                    }

                                    // report intersection
                                    callback.process((T) elements[cell.keys[a]], ba,
                                                     (T) elements[cell.keys[b]], bb);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void query(@Const AxisAlignedBox bounds, QueryCallback<T> callback) {
        if (bounds == null) {
            throw new NullPointerException("Bounds cannot be null");
        }
        if (callback == null) {
            throw new NullPointerException("Callback cannot be null");
        }

        // hash x/z of bounds and do spatial hash query over intersecting cells
        int minX = Math.max(0, hashCellX(bounds.min));
        int minY = Math.max(0, hashCellY(bounds.min));
        int minZ = Math.max(0, hashCellZ(bounds.min));
        int maxX = Math.min(maxCellDimension - 1, hashCellX(bounds.max));
        int maxY = Math.min(maxCellDimension - 1, hashCellY(bounds.max));
        int maxZ = Math.min(maxCellDimension - 1, hashCellZ(bounds.max));

        int query = ++queryIdCounter;
        AxisAlignedBox itemBounds = new AxisAlignedBox();

        Cell cell;
        int item;
        for (int z = minZ; z <= maxZ; z++) {
            for (int y = minY; y <= maxY; y++) {
                for (int x = minX; x <= maxX; x++) {
                    cell = spatialHash[hash(x, y, z)];
                    if (cell != null) {
                        for (int i = 0; i < cell.size; i++) {
                            item = cell.keys[i];

                            // check query id, since the item could have crossed cell bounds
                            // - this is valid in a single threaded situation
                            if (queryIds[item] != query) {
                                updateBounds(itemBounds, item);
                                if (bounds.intersects(itemBounds)) {
                                    // we have an intersection, invoke the callback
                                    callback.process((T) elements[cell.keys[i]], itemBounds);
                                }

                                // record we've visited this item so other cells don't
                                // attempt intersection checks
                                queryIds[item] = query;
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void query(Frustum f, QueryCallback<T> callback) {
        if (f == null) {
            throw new NullPointerException("Frustum cannot be null");
        }
        if (callback == null) {
            throw new NullPointerException("Callback cannot be null");
        }

        // start at root quadtree and walk the tree to compute intersections,
        // building in place an aabb for testing.
        query(0, 0, new AxisAlignedBox(rootBounds), ++queryIdCounter, f, new PlaneState(), false, callback,
              new AxisAlignedBox());
    }

    @SuppressWarnings("unchecked")
    private void query(int level, int index, AxisAlignedBox nodeBounds, int query, Frustum f,
                       PlaneState planeState, boolean insideGuaranteed, QueryCallback<T> callback,
                       AxisAlignedBox itemBounds) {
        // we assume that this node has items and nodeBounds has been updated to
        // equal this node. we still have to check if the node intersects the frustum
        if (!insideGuaranteed) {
            FrustumIntersection test = f.intersects(nodeBounds, planeState);
            if (test == FrustumIntersection.OUTSIDE) {
                // node and it's children do not intersect, escape now
                return;
            } else if (test == FrustumIntersection.INSIDE) {
                // all children nodes and items are guaranteed inside as well
                insideGuaranteed = true;
            }
        }

        // at this point the node is within the frustum, so we have to visit
        // its children if they have items underneath them

        // save planestate
        int state = planeState.get();

        if (level == depth - 1) {
            // we are at a leaf node, to visit children, we process items in the
            // linked cell, we assume that the cell index is non-negative since
            // we had to have passed a >0 check to get here
            int item;
            Cell cell = spatialHash[octree[getLevelOffset(level) + index]];
            // process items
            for (int i = 0; i < cell.size; i++) {
                item = cell.keys[i];

                // check query id, since the item could have crossed cell bounds
                // - this is valid in a single threaded situation
                if (queryIds[item] != query) {
                    updateBounds(itemBounds, item);
                    if (insideGuaranteed ||
                        f.intersects(itemBounds, planeState) != FrustumIntersection.OUTSIDE) {
                        // we have an intersection, invoke the callback
                        callback.process((T) elements[cell.keys[i]], itemBounds);
                    }

                    // record we've visited this item so other cells don't
                    // attempt intersection checks
                    queryIds[item] = query;

                    // restore planestate for next item
                    planeState.set(state);
                }
            }
        } else {
            int childOffset = getLevelOffset(level + 1);
            int childIndex;
            // visit children and check counts directly
            for (int i = 0; i < 8; i++) {
                childIndex = getChildIndex(index, i);
                if (octree[childOffset + childIndex] > 0) {
                    // visit child
                    toChildBounds(i, nodeBounds);
                    query(level + 1, childIndex, nodeBounds, query, f, planeState, insideGuaranteed, callback,
                          itemBounds);
                    restoreParentBounds(i, nodeBounds);

                    // restore planestate for this node
                    planeState.set(state);
                }
            }
        }
    }

    private static boolean inPositiveX(int index) {
        return (index & POS_X) != 0;
    }

    private static boolean inPositiveY(int index) {
        return (index & POS_Y) != 0;
    }

    private static boolean inPositiveZ(int index) {
        return (index & POS_Z) != 0;
    }

    private void toChildBounds(int child, AxisAlignedBox bounds) {
        if (inPositiveX(child)) {
            // new min x is the center of the node
            bounds.min.x = (bounds.min.x + bounds.max.x) / 2.0;
        } else {
            // new max x is the center of the node
            bounds.max.x = (bounds.min.x + bounds.max.x) / 2.0;
        }

        if (inPositiveY(child)) {
            // new min y is the center of the node
            bounds.min.y = (bounds.min.y + bounds.max.y) / 2.0;
        } else {
            // new max y is the center of the node
            bounds.max.y = (bounds.min.y + bounds.max.y) / 2.0;
        }

        if (inPositiveZ(child)) {
            // new min z is the center of the node
            bounds.min.z = (bounds.min.z + bounds.max.z) / 2.0;
        } else {
            // new max z is the center of the node
            bounds.max.z = (bounds.min.z + bounds.max.z) / 2.0;
        }
    }

    private void restoreParentBounds(int child, AxisAlignedBox bounds) {
        if (inPositiveX(child)) {
            // new min x = min x - distance from min to max = 2 * min - max
            bounds.min.x = 2 * bounds.min.x - bounds.max.x;
        } else {
            // new max x = max x + distance from min to max = 2 * max - min
            bounds.max.x = 2 * bounds.max.x - bounds.min.x;
        }

        if (inPositiveY(child)) {
            // new min y = min y - distance from min to max = 2 * min - max
            bounds.min.y = 2 * bounds.min.y - bounds.max.y;
        } else {
            // new max y = max y + distance from min to max = 2 * max - min
            bounds.max.y = 2 * bounds.max.y - bounds.min.y;
        }

        if (inPositiveZ(child)) {
            // new min z = min z - distance from min to max = 2 * min - max
            bounds.min.z = 2 * bounds.min.z - bounds.max.z;
        } else {
            // new max z = max z + distance from min to max = 2 * max - min
            bounds.max.z = 2 * bounds.max.z - bounds.min.z;
        }
    }

    protected int hashCellX(@Const Vector3 v) {
        // we add widthOffset to the coordinate value to get values into a positive-only range
        return clamp((int) ((v.x + widthOffset) * widthScaleFactor));
    }

    protected int hashCellY(@Const Vector3 v) {
        return clamp((int) ((v.y + heightOffset) * heightScaleFactor));
    }

    protected int hashCellZ(@Const Vector3 v) {
        return clamp((int) ((v.z + depthOffset) * depthScaleFactor));
    }

    protected int hash(int cellX, int cellY, int cellZ) {
        return cellX + maxCellDimension * cellY +
               maxCellDimension * maxCellDimension * cellZ;
    }

    protected int clamp(int discrete) {
        return Math.max(0, Math.min(maxCellDimension - 1, discrete));
    }

    private void updateBounds(AxisAlignedBox bounds, int index) {
        int realIndex = index * 6;
        bounds.min.set(aabbs, realIndex);
        bounds.max.set(aabbs, realIndex + 3);
    }

    private int getQuadLeafFromCell(int cellX, int cellY, int cellZ) {
        // compute the center point of the cell, to use in a tree search,
        // must also subtract away offsets to get into the root bounds space
        double px = (cellX + 0.5) / widthScaleFactor - widthOffset;
        double py = (cellY + 0.5) / heightScaleFactor - heightOffset;
        double pz = (cellZ + 0.5) / depthScaleFactor - depthOffset;

        double minx = rootBounds.min.x;
        double miny = rootBounds.min.y;
        double minz = rootBounds.min.z;
        double maxx = rootBounds.max.x;
        double maxy = rootBounds.max.y;
        double maxz = rootBounds.max.z;

        // the center of the node
        double cx = (minx + maxx) * 0.5;
        double cy = (miny + maxy) * 0.5;
        double cz = (minz + maxz) * 0.5;

        int child;
        int index = 0;
        for (int i = 0; i < depth - 1; i++) {
            child = 0;
            // if px > cx then the cell is in the right/positive x half of this node
            if (px >= cx) {
                child |= POS_X;
                // next node's min x is the current center x
                minx = cx;
            } else {
                // next node's max x is the current center x
                maxx = cx;
            }

            if (py >= cy) {
                child |= POS_Y;
                // next node's min y is the current center y
                miny = cy;
            } else {
                maxy = cy;
            }

            if (pz >= cz) {
                child |= POS_Z;
                // next node's min z is the current center z
                minz = cz;
            } else {
                maxz = cz;
            }

            // compute new center for next node
            cx = (minx + maxx) * 0.5;
            cy = (miny + maxy) * 0.5;
            cz = (minz + maxz) * 0.5;

            index = getChildIndex(index, child);
        }

        return index;
    }

    private int getLevelOffset(int level) {
        // compute the index offset into the quadtree array for the given level
        if (level == 0) {
            // level = 0 is the root, which is the first element in the array
            return 0;
        } else {
            // finite sum of the geometric series: 1 + 8 + 32 + ...
            //  1. S = 8^0 + 8^1 + 8^2 + ... + 8^level-1
            //  2. 8S = 8^1 + ... + 8^(level)
            //  3. 8S = (S - 1) + 8^(level)
            //  4. 7S = 8^(level) - 1
            //  5. S = (8^(level) - 1) / 7
            //  6. S = (2^(3level) - 1) / 7
            return ((1 << (3 * level)) - 1) / 7;
        }
    }

    private int getChildIndex(int parentIndex, int child) {
        // shift parent index left 3 bits, and OR in the child, this assumes:
        // - parentIndex does not have its level offset added to it
        // - child is between 0 and 7 (i.e. 3 bits required)
        return (parentIndex << 3) | child;
    }

    private int getParentIndex(int childIndex) {
        // shift child index right 3 bits, this assumes:
        // - child does not have its level offset added to it
        return (childIndex >> 3);
    }

    private static class Cell {
        private static final int INCREMENT = 4;
        private static final int MAX_LIFETIME = 15;

        private int[] keys;

        private int size;

        private int lifetime;

        // this is the parent index of the octree index that actually holds
        // this cell, because the leaves don't store count information
        private final int octreeIndex;

        private Cell(int octLeaf) {
            octreeIndex = octLeaf;
            keys = new int[INCREMENT];
            size = 0;
            lifetime = 0;
        }

        public void add(Octree<?> tree, int item) {
            if (size == keys.length - 1) {
                // increase size
                keys = Arrays.copyOf(keys, keys.length + INCREMENT);
            }
            keys[size] = item;
            size++;

            // update octree counts by 1
            updateTree(tree, 1);
        }

        private void updateTree(Octree<?> tree, int val) {
            int index = octreeIndex;
            // it's depth-2 because depth-1 is the leaf level, but we skip that one
            for (int l = tree.depth - 2; l >= 0; l--) {
                index = tree.getParentIndex(index);
                tree.octree[tree.getLevelOffset(l) + index] += val;
            }
        }

        public void remove(Octree<?> tree, int item) {
            for (int i = 0; i < size; i++) {
                // search for the item to remove
                if (keys[i] == item) {
                    keys[i] = keys[size - 1];
                    size--;

                    // decrement octree counts by 1
                    updateTree(tree, -1);
                    break;
                }
            }
        }
    }
}
