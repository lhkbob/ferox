package com.ferox.math.bounds;

import java.util.Arrays;

import com.ferox.math.AxisAlignedBox;
import com.ferox.math.Const;
import com.ferox.math.Functions;
import com.ferox.math.Vector3;
import com.ferox.math.bounds.Frustum.FrustumIntersection;

/**
 * <p>
 * QuadTree is a SpatialIndex implementation that uses a quadtree to efficiently
 * cull areas in the viewing frustum that don't have objects within them. This
 * particular implementation is a fully-allocated quadtree on top of a spatial
 * grid. This means that inserts are almost constant time, and that AABB queries
 * are very fast.
 * </p>
 * <p>
 * The quadtree is extend to three dimensions by having the 2D quadtree defined
 * in the XZ plane, and imposing a minimum and maximum Y value for every object
 * within the quadtree. This means the quadtree is well suited to 3D games that
 * predominantly 2D in their logic (i.e. an RTS).
 * </p>
 * 
 * @author Michael Ludwig
 * @param <T> The data type stored in the quadtree
 */
public class QuadTree<T> implements SpatialIndex<T> {
    private static final int POS_X = 0x1;
    private static final int POS_Y = 0x2;
    
    // complete quadtree nodes, keyed by hashed node ids packed into bits
    // - values are the number of children in each node
    private final int[] quadtree;
    private final int depth;
    
    // grid of leaf nodes
    private final Cell[] spatialHash;
    private final int maxCellDimension;
    
    private final double widthScaleFactor;
    private final double heightScaleFactor;
    
    private final double widthOffset;
    private final double heightOffset;
    
    private final AxisAlignedBox rootBounds;
    
    // items in the quadtree
    private Object[] elements;
    private int[] queryIds;
    private double[] aabbs;
    private int size;
    
    private int queryIdCounter;
    
    /**
     * Construct a new QuadTree that has X and Z dimensions of 100, and an
     * estimated object size of 2 units, which allows the tree to have a depth
     * of 6.
     */
    public QuadTree() {
        this(100, 2.0);
    }
    
    /**
     * <p>
     * Construct a new QuadTree that has X and Z dimensions equal to
     * <tt>sideLength</tt>, and the depth of the tree is controlled by the
     * estimated object size, <tt>objSize</tt>. <tt>objSize</tt> should be the
     * approximate dimension of the average object contained in this index. If
     * it is too big or too small, query performance may suffer.
     * </p>
     * <p>
     * The height of the root bounds is estimated as 20 times the object size.
     * </p>
     * 
     * @param sideLength The side length of the root bounds of the quadtree
     * @param objSize The estimated object size
     */
    public QuadTree(double sideLength, double objSize) {
        this(new AxisAlignedBox(new Vector3(-sideLength / 2.0, -10 * objSize, -sideLength / 2.0),
                                new Vector3(sideLength / 2.0, 10 * objSize, sideLength / 2.0)), 
             Functions.log2((int) Math.ceil(sideLength / objSize)));
    }
    
    /**
     * <P>
     * Construct a new QuadTree with the given root bounds, <tt>aabb</tt> and
     * tree depth. The depth of the tree and the X and Z dimensions of the root
     * bounds determine the size the leaf quadtree nodes. If objects are
     * significantly larger than this, they will be contained in multiple nodes
     * and it may hurt performance.
     * </p>
     * <p>
     * The root bounds are copied so future changes to <tt>aabb</tt> will not
     * affect this tree.
     * </p>
     * 
     * @param aabb The root bounds of the tree
     * @param depth The depth of the tree
     * @throws NullPointerException if aabb is null
     */
    public QuadTree(@Const AxisAlignedBox aabb, int depth) {
        if (aabb == null)
            throw new NullPointerException("Root bounds cannot be null");
        
        this.depth = depth;
        rootBounds = aabb.clone();
        
        maxCellDimension = 1 << (depth - 1);
        spatialHash = new Cell[maxCellDimension * maxCellDimension];
        elements = new Object[8];
        queryIds = new int[8];
        aabbs = new double[48];
        size = 0;
        queryIdCounter = 0;
        
        widthScaleFactor = maxCellDimension / (getFirstDimension(aabb.max) - getFirstDimension(aabb.min));
        heightScaleFactor = maxCellDimension / (getSecondDimension(aabb.max) - getSecondDimension(aabb.min));
        
        widthOffset =  -getFirstDimension(aabb.min);
        heightOffset = -getSecondDimension(aabb.min);
        
        int numNodes = getLevelOffset(depth);
        quadtree = new int[numNodes];
        
        // mark quadtree leaves with negative indices, so that indices
        // can be computed lazily later
        int leafOffset = getLevelOffset(depth - 1);
        Arrays.fill(quadtree, leafOffset, quadtree.length, -1);
    }
    
    @Override
    public boolean remove(T element) {
        if (element == null)
            throw new NullPointerException("Item cannot be null");
        
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
     * Update cell references to oldIndex to point to toIndex (e.g.
     * when an element has been swapped because original value for toIndex
     * was removed)
     */
    private void updateItemIndex(int oldIndex, int toIndex) {
        // do an aabb query using the last known aabb state so that we
        // limit the number of cells considered
        Vector3 t = new Vector3();
        int minX = hashCellX(t.set(aabbs, toIndex * 6));
        int minY = hashCellY(t); // t still holds the min vector
        int maxX = hashCellX(t.set(aabbs, toIndex * 6 + 3));
        int maxY = hashCellY(t); // t still holds the max vector
        
        Cell cell;
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                cell = spatialHash[hash(x, y)];
                if (cell != null) {
                    // iterate through keys and search for old one
                    for (int i = 0; i < cell.size; i++) {
                        if (cell.keys[i] == oldIndex)
                            cell.keys[i] = toIndex;
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
        int maxX = hashCellX(t.set(aabbs, index * 6 + 3));
        int maxY = hashCellY(t); // t still holds the max vector
        
        Cell cell;
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                cell = spatialHash[hash(x, y)];
                if (cell != null) {
                    // remove cell's reference to index, and update quadtree
                    // counts
                    cell.remove(this, index);
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
        if (element == null)
            throw new NullPointerException("Item cannot be null");
        if (bounds == null)
            throw new NullPointerException("Item bounds cannot be null");
        
        if (!rootBounds.contains(bounds))
            return false; // skip the element
        
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
        int maxX = hashCellX(bounds.max);
        int maxY = hashCellY(bounds.max); // FIXME if exactly on far edge of max, we get out of bounds
        
        int hash;
        Cell cell;
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                hash = hash(x, y);
                cell = spatialHash[hash];
                if (cell == null) {
                    int qtIndex = getQuadLeafFromCell(x, y);
                    cell = new Cell(this, qtIndex);
                    spatialHash[hash] = cell;
                    
                    // also update the quad tree to point to this cell
                    qtIndex += getLevelOffset(depth - 1);
                    if (quadtree[qtIndex] != -1)
                        throw new IllegalArgumentException("Quadtree leaf should not have any index");
                    quadtree[qtIndex] = hash;
                }
                cell.add(this, itemIndex, bounds);
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
        Arrays.fill(quadtree, 0, leafStartIndex, 0);
        
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
                    quadtree[leafOffset + c.quadTreeIndex] = -1;
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
        if (callback == null)
            throw new NullPointerException("Callback cannot be null");
        
        AxisAlignedBox ba = new AxisAlignedBox();
        AxisAlignedBox bb = new AxisAlignedBox();
        Vector3 minIntersect = new Vector3();
        
        intersectionCount = 0;
        maxCellCount = 0;
        usedCellCount = 0;
        
        // iterate over all cells
        Cell cell;
        for (int cellY = 0; cellY < maxCellDimension; cellY++) {
            for (int cellX = 0; cellX < maxCellDimension; cellX++) {
                maxCellCount++;
                cell = spatialHash[hash(cellX, cellY)];
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
                                minIntersect.set(Math.max(ba.min.x, bb.min.x),
                                                 Math.max(ba.min.y, bb.min.y),
                                                 Math.max(ba.min.z, bb.min.z));
                                if (hashCellX(minIntersect) != cellX || hashCellY(minIntersect) != cellY) {
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
    
    @Override
    @SuppressWarnings("unchecked")
    public void query(@Const AxisAlignedBox bounds, QueryCallback<T> callback) {
        if (bounds == null)
            throw new NullPointerException("Bounds cannot be null");
        if (callback == null)
            throw new NullPointerException("Callback cannot be null");
        
        // hash x/z of bounds and do spatial hash query over intersecting cells
        int minX = Math.max(0, hashCellX(bounds.min));
        int minY = Math.max(0, hashCellY(bounds.min));
        int maxX = Math.min(maxCellDimension - 1, hashCellX(bounds.max));
        int maxY = Math.min(maxCellDimension - 1, hashCellY(bounds.max));
        
        int query = ++queryIdCounter;
        AxisAlignedBox itemBounds = new AxisAlignedBox();
        
        Cell cell;
        int item;
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                cell = spatialHash[hash(x, y)];
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
    
    @Override
    public void query(Frustum f, QueryCallback<T> callback) {
        if (f == null)
            throw new NullPointerException("Frustum cannot be null");
        if (callback == null)
            throw new NullPointerException("Callback cannot be null");
        
        // start at root quadtree and walk the tree to compute intersections,
        // building in place an aabb for testing.
        query(0, 0, new AxisAlignedBox(rootBounds), ++queryIdCounter, 
              f, new PlaneState(), false, callback, 
              new AxisAlignedBox());
    }
    
    @SuppressWarnings("unchecked")
    private void query(int level, int index, AxisAlignedBox nodeBounds, int query, Frustum f, PlaneState planeState, 
                       boolean insideGuaranteed, QueryCallback<T> callback, AxisAlignedBox itemBounds) {
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
            Cell cell = spatialHash[quadtree[getLevelOffset(level) + index]];
            // process items
            for (int i = 0; i < cell.size; i++) {
                item = cell.keys[i];

                // check query id, since the item could have crossed cell bounds
                // - this is valid in a single threaded situation
                if (queryIds[item] != query) {
                    updateBounds(itemBounds, item);
                    if (insideGuaranteed || f.intersects(itemBounds, planeState) != FrustumIntersection.OUTSIDE) {
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
            for (int i = 0; i < 4; i++) {
                childIndex = getChildIndex(index, i);
                if (quadtree[childOffset + childIndex] > 0) {
                    // visit child
                    toChildBounds(i, nodeBounds);
                    query(level + 1, childIndex, nodeBounds, query, f, planeState, insideGuaranteed, callback, itemBounds);
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
    
    private void toChildBounds(int child, AxisAlignedBox bounds) {
        if (inPositiveX(child)) {
            // new min x is the center of the node
            setFirstDimension(bounds.min, (getFirstDimension(bounds.min) + getFirstDimension(bounds.max)) / 2.0);
        } else {
            // new max x is the center of the node
            setFirstDimension(bounds.max, (getFirstDimension(bounds.min) + getFirstDimension(bounds.max)) / 2.0);
        }
        
        if (inPositiveY(child)) {
            // new min y is the center of the node
            setSecondDimension(bounds.min, (getSecondDimension(bounds.min) + getSecondDimension(bounds.max)) / 2.0);
        } else {
            // new max y is the center of the node
            setSecondDimension(bounds.max, (getSecondDimension(bounds.min) + getSecondDimension(bounds.max)) / 2.0);
        }
    }
    
    private void restoreParentBounds(int child, AxisAlignedBox bounds) {
        if (inPositiveX(child)) {
            // new min x = min x - distance from min to max = 2 * min - max
            setFirstDimension(bounds.min, 2 * getFirstDimension(bounds.min) - getFirstDimension(bounds.max));
        } else {
            // new max x = max x + distance from min to max = 2 * max - min
            setFirstDimension(bounds.max, 2 * getFirstDimension(bounds.max) - getFirstDimension(bounds.min));
        }
        
        if (inPositiveY(child)) {
            // new min y = min y - distance from min to max = 2 * min - max
            setSecondDimension(bounds.min, 2 * getSecondDimension(bounds.min) - getSecondDimension(bounds.max));
        } else {
            // new max y = max y + distance from min to max = 2 * max - min
            setSecondDimension(bounds.max, 2 * getSecondDimension(bounds.max) - getSecondDimension(bounds.min));
        }
    }
    
    protected void setFirstDimension(Vector3 v, double d) {
        v.x = d;
    }
    
    protected void setSecondDimension(Vector3 v, double d) {
        v.z = d;
    }
    
    protected double getFirstDimension(@Const Vector3 v) {
        return v.x;
    }
    
    protected double getSecondDimension(@Const Vector3 v) {
        return v.z;
    }
    
    protected int hashCellX(@Const Vector3 v) {
        // we add widthOffset to the coordinate value to get values into a positive-only range
        return (int) ((getFirstDimension(v) + widthOffset) * widthScaleFactor);
    }
    
    protected int hashCellY(@Const Vector3 v) {
        return (int) ((getSecondDimension(v) + heightOffset) * heightScaleFactor);
    }
    
    protected int hash(int cellX, int cellY) {
        return cellX + maxCellDimension * cellY;
    }
    
    private void updateBounds(AxisAlignedBox bounds, int index) {
        int realIndex = index * 6;
        bounds.min.set(aabbs, realIndex);
        bounds.max.set(aabbs, realIndex + 3);
    }
    
    private int getQuadLeafFromCell(int cellX, int cellY) {
        // compute the center point of the cell, to use in a tree search,
        // must also subtract away offsets to get into the root bounds space
        double px = (cellX + 0.5) / widthScaleFactor - widthOffset;
        double py = (cellY + 0.5) / heightScaleFactor - heightOffset;
        
        double minx = getFirstDimension(rootBounds.min);
        double miny = getSecondDimension(rootBounds.min);
        double maxx = getFirstDimension(rootBounds.max);
        double maxy = getSecondDimension(rootBounds.max);
        
        // the center of the node
        double cx = (minx + maxx) * 0.5;
        double cy = (miny + maxy) * 0.5;
        
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
            
            // compute new center for next node
            cx = (minx + maxx) * 0.5;
            cy = (miny + maxy) * 0.5;

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
            // finite sum of the geometric series: 1 + 4 + 16 + ...
            //  1. S = 4^0 + 4^1 + 4^2 + ... + 4^level-1
            //  2. 4S = 4^1 + ... + 4^(level)
            //  3. 4S = (S - 1) + 4^(level)
            //  4. 3S = 4^(level) - 1
            //  5. S = (4^(level) - 1) / 3
            //  6. S = (2^(2level) - 1) / 3
            return ((1 << (level << 1)) - 1) / 3;
        }
    }
    
    private int getChildIndex(int parentIndex, int child) {
        // shift parent index left 2 bits, and OR in the child, this assumes:
        // - parentIndex does not have its level offset added to it
        // - child is between 0 and 3 (i.e. 2 bits required)
        return (parentIndex << 2) | child;
    }
    
    private int getParentIndex(int childIndex) {
        // shift child index right 2 bits, this assumes:
        // - child does not have its level offset added to it
        return (childIndex >> 2);
    }
    
    private static class Cell {
        private static final int INCREMENT = 4;
        private static final int MAX_LIFETIME = 15;
        
        private int[] keys;
        
        private int size;
        
        private int lifetime;
        
        // this is the parent index of the quadtree index that actually holds
        // this cell, because the leaves don't store count information
        private final int quadTreeIndex;
        
        private Cell(QuadTree<?> tree, int quadLeaf) {
            quadTreeIndex = quadLeaf;
            keys = new int[INCREMENT];
            size = 0;
            lifetime = 0;
        }
        
        public void add(QuadTree<?> tree, int item, @Const AxisAlignedBox bounds) {
            if (size == keys.length - 1) {
                // increase size
                keys = Arrays.copyOf(keys, keys.length + INCREMENT);
            }
            keys[size] = item;
            size++;
            
            // update quadtree counts by 1
            updateTree(tree, 1);
        }
        
        private void updateTree(QuadTree<?> tree, int val) {
            int index = quadTreeIndex;
            // it's depth-2 because depth-1 is the leaf level, but we skip that one
            for (int l = tree.depth - 2; l >= 0; l--) {
                index = tree.getParentIndex(index);
                tree.quadtree[tree.getLevelOffset(l) + index] += val;
            }
        }
        
        public void remove(QuadTree<?> tree, int item) {
            for (int i = 0; i < size; i++) {
                // search for the item to remove
                if (keys[i] == item) {
                    keys[i] = keys[size - 1];
                    size--;
                    
                    // decrement quadtree counts by 1
                    updateTree(tree, -1);
                    break;
                }
            }
        }
    }
}
