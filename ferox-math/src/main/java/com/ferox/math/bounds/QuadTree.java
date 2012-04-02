package com.ferox.math.bounds;

import java.util.Arrays;

import com.ferox.math.Const;
import com.ferox.math.Vector3;
import com.ferox.math.bounds.Frustum.FrustumIntersection;

public class QuadTree<T> {//implements SpatialIndex<T> {
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
    
    public QuadTree() {
        this(100, 40);
    }
    
    public QuadTree(double sideLength, double height) {
        this(sideLength, height, 8);
    }
    
    public QuadTree(double sideLength, double height, int depth) {
        this(new AxisAlignedBox(new Vector3(-sideLength / 2.0, -height / 2.0, -sideLength / 2.0),
                                new Vector3(sideLength / 2.0, height / 2.0, sideLength / 2.0)), depth);
    }
    
//    public QuadTree(@Const AxisAlignedBox aabb, double objSize) {
//        
//    }
    
    public QuadTree(@Const AxisAlignedBox aabb, int depth) {
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
    
    public boolean add(T element, @Const AxisAlignedBox bounds) {
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
        int maxY = hashCellY(bounds.max);
        
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
    
    public void clear() {
        // fill quadtree counts with 0s, but only up to the leaf nodes because
        // they hold cell indices, which don't change
        int leafStartIndex = getLevelOffset(depth - 1);
        Arrays.fill(quadtree, 0, leafStartIndex, 0);
        
        // clear spatial hash
        int clearCount = 0;
        int totalCount = 0;
        Cell c;
        int leafOffset = getLevelOffset(depth - 1);
        for (int i = 0; i < spatialHash.length; i++) {
            c = spatialHash[i];
            if (c != null) {
                totalCount++;
                c.lifetime++;
                
                // check lifetime to help with GC'ing
                if (c.lifetime > Cell.MAX_LIFETIME && c.size == 0) {
                    // clear cell so that its contents get GC'ed
                    spatialHash[i] = null;
                    quadtree[leafOffset + c.quadTreeIndex] = -1;
                    clearCount++;
                }
                
                // only need to reset the size variable
                c.size = 0;
            }
        }
        
        // empty global item bag
        size = 0;
        queryIdCounter = 0;
        
        System.out.println("cleared " + clearCount + " of " + totalCount + " of possible " + spatialHash.length);
    }
    
    @SuppressWarnings("unchecked")
    public void query(@Const AxisAlignedBox bounds, QueryCallback<T> callback) {
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
    
    public void query(Frustum f, QueryCallback<T> callback) {
        // start at root quadtree and walk the tree to compute intersections,
        // building in place an aabb for testing.
        AxisAlignedBox root = new AxisAlignedBox(rootBounds);
        AxisAlignedBox item = new AxisAlignedBox();
        
        query(f, callback, ++queryIdCounter, 0, 0, root, item, new PlaneState(), false);
    }
    
    // FIXME: these arguments to be ordered better
    @SuppressWarnings("unchecked")
    private void query(Frustum f, QueryCallback<T> callback, int query, int level, int index, AxisAlignedBox nodeBounds, 
                       AxisAlignedBox itemBounds, PlaneState planeState, boolean insideGuaranteed) {
        // we assume that this node has items and nodeBounds has been updated to
        // equal this node. we still have to check if the node intersects the frustum
        if (!insideGuaranteed) {
            FrustumIntersection test = nodeBounds.intersects(f, planeState);
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
                    if (insideGuaranteed || itemBounds.intersects(f, planeState) != FrustumIntersection.OUTSIDE) {
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
                    query(f, callback, query, level + 1, childIndex, nodeBounds, itemBounds, planeState, insideGuaranteed);
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
    
    public void updateBounds(AxisAlignedBox bounds, int index) {
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
//        private double[] aabbs;
        
        private int size;
        
        private int lifetime;
        
        // this is the parent index of the quadtree index that actually holds
        // this cell, because the leaves don't store count information
        private final int quadTreeIndex;
        
        private Cell(QuadTree<?> tree, int quadLeaf) {
            quadTreeIndex = quadLeaf; //tree.getParentIndex(quadLeaf);
//            aabbs = new double[INCREMENT * 6];
            keys = new int[INCREMENT];
            size = 0;
            lifetime = 0;
        }
        
        public void add(QuadTree<?> tree, int item, @Const AxisAlignedBox bounds) {
            if (size == keys.length - 1) {
                // increase size
                keys = Arrays.copyOf(keys, keys.length + INCREMENT);
//                aabbs = Arrays.copyOf(aabbs, aabbs.length + INCREMENT * 6);
            }
            keys[size] = item;
//            bounds.min.get(aabbs, size * 6);
//            bounds.max.get(aabbs, size * 6 + 3);
            size++;
            
            // update quadtree counts by 1
            updateTree(tree, 1);
        }
        
        /*public void updateBounds(AxisAlignedBox bounds, int index) {
            int realIndex = index * 6;
            bounds.min.set(aabbs, realIndex);
            bounds.max.set(aabbs, realIndex + 3);
        }*/
        
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
//                    System.arraycopy(aabbs, (size - 1) * 6, aabbs, i * 6, 6);
                    size--;
                    
                    // decrement quadtree counts by 1
                    updateTree(tree, -1);
                    break;
                }
            }
        }
    }
}
