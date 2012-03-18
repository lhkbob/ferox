package com.ferox.math.bounds;

import java.util.HashSet;
import java.util.Set;

import com.ferox.math.Const;
import com.ferox.math.Vector3;
import com.ferox.math.bounds.Frustum.FrustumIntersection;
import com.ferox.util.Bag;

/**
 * <p>
 * Octree is a SpatialIndex implementation that represents the octree data
 * structure. The octree partitions space into an organized hierarchy of cubes.
 * A cube node within the octree can be evenly split into 8 nested cubes which
 * partition its space.
 * </p>
 * <p>
 * This octree only stores items as deep as possible before they overlap an edge
 * of a node. This means that updates and removals are very fast, but many items
 * can be placed at the top of the tree, reducing the efficiency of queries.
 * </p>
 * 
 * @author Michael Ludwig
 * @param <T> Class type of items contained by the Octree
 */
@SuppressWarnings("unchecked")
public class Octree<T> implements SpatialIndex<T> {
    public static final int DEFAULT_MAX_DEPTH = 6;
    
    private int queryNumber;

    private Node<T> root;
    private final AxisAlignedBox rootBounds; // all other node bounds implicit from this
    
    private final Set<Node<T>> pendingRemovals;

    /**
     * Create a new Octree with dimensions of 100 and an initial max depth of 6.
     */
    public Octree() {
        this(100.0);
    }

    /**
     * Create a new Octree with the given side dimension of its root cube, and an
     * initial max depth of 6. As the octree expands to enclose more nodes, its
     * depth and dimensions can increase.
     * 
     * @param side The desired side length for the root node
     * @throws IllegalArgumentException if side is less than 0
     */
    public Octree(double side) {
        this(side, DEFAULT_MAX_DEPTH);
    }

    /**
     * Create an Octree with the given side dimension of its root cube and the
     * specified initial max depth. As the octree expands to enclose more nodes,
     * its depth and dimensions can increase.
     * 
     * @param side The desired side length for the root node
     * @param initialMaxDepth The initial max depth of the tree
     * @throws IllegalArgumentException if side is less than 0, or if
     *             initialMaxDepth < 1
     */
    public Octree(double side, int initialMaxDepth) {
        this(new AxisAlignedBox(new Vector3(-side / 2, -side / 2, -side / 2), 
                                new Vector3(side / 2, side / 2, side / 2)),
             initialMaxDepth);
    }

    /**
     * Create an Octree with the given bounds for its root node and an initial
     * max tree depth of 6. As the octree expands to enclose more nodes, its
     * depth and dimensions can increase.
     * 
     * @param extents The desired root node bounds
     * @throws NullPointerException if extents is null
     */
    public Octree(@Const AxisAlignedBox extents) {
        this(extents, DEFAULT_MAX_DEPTH);
    }

    /**
     * Create an Octree with the given bounds for its root node and the
     * specified initial max depth. As the octree expands to enclose more nodes,
     * its depth and dimensions can increase.
     * 
     * @param extents The desired root node bounds
     * @param initialMaxDepth The initial max depth of the tree
     * @throws NullPointerException if extents is null
     * @throws IllegalArgumentException if initialMaxDepth < 1
     */
    public Octree(@Const AxisAlignedBox extents, int initialMaxDepth) {
        if (extents == null)
            throw new NullPointerException("Extents cannot be null");
        if (initialMaxDepth <= 0)
            throw new IllegalArgumentException("Initial max depth must be at least 1, not: " + initialMaxDepth);
        if (extents.max.x < extents.min.x ||
            extents.max.y < extents.min.y ||
            extents.max.z < extents.min.z)
            throw new IllegalArgumentException("Invalid root bounds: " + extents);
        
        rootBounds = new AxisAlignedBox(extents);
        root = new Node<T>(null, initialMaxDepth - 1);
        pendingRemovals = new HashSet<Node<T>>();
        
        queryNumber = 0;
    }
    
    @Override
    public Object add(T item, @Const AxisAlignedBox bounds) {
        if (item == null)
            throw new NullPointerException("Item cannot be null");
        
        if (inOctree(bounds)) {
            Key<T> newKey = new Key<T>(item, bounds, this);
            newKey.addToRoot();

            return newKey;
        } else {
            // can't fit in tree
            return null;
        }
    }

    @Override
    public void remove(T item, Object key) {
        if (item == null)
            throw new NullPointerException("Item cannot be null");
        if (key == null)
            throw new NullPointerException("Key cannot be null");
        
        if (key instanceof Key) {
            Key<T> ok = (Key<T>) key;
            if (ok.owner == this && ok.data == item) {
                // key is valid, now remove it
                ok.removeFromRoot();
                return;
            }
        }

        // else key is invalid
        throw new IllegalArgumentException("Invalid key: " + key);
    }

    @Override
    public boolean update(T item, @Const AxisAlignedBox bounds, Object key) {
        if (item == null)
            throw new NullPointerException("Item cannot be null");
        if (key == null)
            throw new NullPointerException("Key cannot be null");
        
        if (key instanceof Key) {
            Key<T> ok = (Key<T>) key;
            if (ok.owner == this && ok.data == item) {
                // key is valid
                if (inOctree(bounds)) {
                    // item is still in the octree, so update it
                    ok.update(bounds);
                    return true;
                } else {
                    // item has moved outside of the tree, so remove it
                    ok.removeFromRoot();
                    return false;
                }
            }
        }
        
        // else key is invalid
        throw new IllegalArgumentException("Invalid key: " + key);
    }
    
    @Override
    public void query(@Const AxisAlignedBox volume, QueryCallback<T> callback) {
        if (volume == null)
            throw new NullPointerException("Query volume cannot be null");
        if (callback == null)
            throw new NullPointerException("QueryCallback cannot be null");
        
        // prune tree before query to make it the most efficient possible
        pruneTree();
        
        root.visitIntersecting(volume, new AabbQueryCallback<T>(callback, ++queryNumber), rootBounds, false);
    }

    @Override
    public void query(Frustum f, QueryCallback<T> callback) {
        if (f == null)
            throw new NullPointerException("Frustum cannot be null");
        if (callback == null)
            throw new NullPointerException("QueryCallback cannot be null");
        
        // prune tree before query to make it the most efficient possible
        pruneTree();
        
        // set up plane state to be shared by callback and query
        PlaneState planeState = new PlaneState();
        FrustumIntersection rootTest = rootBounds.intersects(f, planeState);
        
        if (rootTest != FrustumIntersection.OUTSIDE)
            root.visitFrustum(f, new FrustumQueryCallback<T>(callback, planeState, ++queryNumber), rootBounds, planeState);
    }
    
    /*
     * Internal helper functions for traversing and manipulating the tree
     */
    
    private void pruneTree() {
        if (pendingRemovals.size() == 0)
            return; // no work needs to be done
        
        for (Node<T> n: pendingRemovals) {
            if (n.isRemovable())
                n.detach();
        }
        pendingRemovals.clear();
    }
    
    private boolean inOctree(@Const AxisAlignedBox dataBounds) {
        if (dataBounds == null)
            throw new NullPointerException("Bounds cannot be null");
        return rootBounds.contains(dataBounds);
    }
    
    /*
     * Bit flags that describe where in the octree grid a
     * child node exists. If POS_a is true, then the child
     * is in the positive half-space of the parent node for
     * that axis.
     */
    private static final int POS_X = 0x1;
    private static final int POS_Y = 0x2;
    private static final int POS_Z = 0x4;
    
    private static boolean inPositiveX(int index) {
        return (index & POS_X) != 0;
    }
    private static boolean inPositiveY(int index) {
        return (index & POS_Y) != 0;
    }
    private static boolean inPositiveZ(int index) {
        return (index & POS_Z) != 0;
    }
    private static int getChildIndex(@Const AxisAlignedBox nodeBounds, @Const Vector3 pointInNode) {
        int index = 0;
        if ((nodeBounds.min.x + nodeBounds.max.x) < 2 * pointInNode.x)
            index |= POS_X;
        if ((nodeBounds.min.y + nodeBounds.max.y) < 2 * pointInNode.y)
            index |= POS_Y;
        if ((nodeBounds.min.z + nodeBounds.max.z) < 2 * pointInNode.z)
            index |= POS_Z;
        return index;
    }
    
    private static void updateForChild(int child, AxisAlignedBox bounds) {
        if (inPositiveX(child))
            bounds.min.x = (bounds.min.x + bounds.max.x) / 2;
        else
            bounds.max.x = (bounds.min.x + bounds.max.x) / 2;
        
        if (inPositiveY(child))
            bounds.min.y = (bounds.min.y + bounds.max.y) / 2;
        else
            bounds.max.y = (bounds.min.y + bounds.max.y) / 2;
        
        if (inPositiveZ(child))
            bounds.min.z = (bounds.min.z + bounds.max.z) / 2;
        else
            bounds.max.z = (bounds.min.z + bounds.max.z) / 2;
    }
    
    private static void updateForParent(int child, AxisAlignedBox bounds) {
        if (inPositiveX(child))
            bounds.min.x = 2 * bounds.min.x - bounds.max.x;
        else
            bounds.max.x = 2 * bounds.max.x - bounds.min.x;
        
        if (inPositiveY(child))
            bounds.min.y = 2 * bounds.min.y - bounds.max.y;
        else
            bounds.max.y = 2 * bounds.max.y - bounds.min.y;
        
        if (inPositiveZ(child))
            bounds.min.z = 2 * bounds.min.z - bounds.max.z;
        else
            bounds.max.z = 2 * bounds.max.z - bounds.min.z;
    }
    
    private static class Node<T> {
        Node<T>[] children; // length 8, or null
        Bag<Key<T>> items;
        
        Node<T> parent;
        final int depth;
        
        public Node(Node<T> parent, int depth) {
            this.parent = parent;
            this.depth = depth;
        }
        
        public boolean isRemovable() {
            if (items != null && items.size() > 0)
                return false;
            if (children != null) {
                for (int i = 0; i < 8; i++) {
                    if (children[i] != null)
                        return false;
                }
            }
            
            return true;
        }
        
        public boolean isLeaf() {
            return depth == 0;
        }
        
        public int add(Key<T> key) {
            if (items == null)
                items = new Bag<Key<T>>();

            items.add(key);
            return items.size() - 1;
        }
        
        public void remove(Key<T> key, int index) {
            if (items == null)
                return;
            
            if (index < 0)
                index = items.indexOf(key);
            items.remove(index);
            
            if (isRemovable())
                key.owner.pendingRemovals.add(this);
            
            if (items.size() != index)
                items.get(index).nodeIndex = index; // just in case
        }
        
        public void detach() {
            if (parent != null) {
                boolean hasChildren = false;
                for (int i = 0; i < 8; i++) {
                    if (parent.children[i] == this)
                        parent.children[i] = null;
                    else if (parent.children[i] != null)
                        hasChildren = true;
                }
                
                if (!hasChildren && (parent.items == null || parent.items.size() == 0)) 
                    parent.detach();
            }
        }
        
        public Node<T> getChild(int index, boolean create) {
            if (isLeaf())
                return null;
            
            // return child if it exists
            if (children != null && children[index] != null)
                return children[index];
            
            if (create) {
                // need a new child node
                if (children == null)
                    children = new Node[8];

                children[index] = new Node<T>(this, depth - 1);
                return children[index];
            } else
                return null;
        }
        
        /**
         * Invokes the callback on this Node and its children (and children's
         * children ...) for all nodes that contain the query bounds. Assumes
         * that this node contains query.
         * 
         * @param query The aabb query
         * @param callback The callback to run on all nodes containing query
         * @param nodeBounds The bounds of this node
         * @param createChildren True if child nodes can be created if they'd be
         *            contained in the query
         */
        public void visitContaining(@Const AxisAlignedBox query, NodeCallback<AxisAlignedBox, T> callback, AxisAlignedBox nodeBounds, boolean createChildren) {
            // since we assume this node contains query, run the callback right away
            callback.visit(query, this, nodeBounds);
            
            // now visit children, if we have any
            if (!isLeaf() && (children != null || createChildren)) {
                int minChildIndex = getChildIndex(nodeBounds, query.min);
                int maxChildIndex = getChildIndex(nodeBounds, query.max);
                
                if (minChildIndex == maxChildIndex) {
                    // query fits within a single child, so we can go deeper
                    Node<T> child = getChild(minChildIndex, createChildren);
                    if (child == null)
                        return;
                    
                    // resize the extent and update the center, visit then revert
                    updateForChild(minChildIndex, nodeBounds);
                    child.visitContaining(query, callback, nodeBounds, createChildren);
                    updateForParent(minChildIndex, nodeBounds);
                } // else query spans multiple children so no more nodes contain it
            }
        }
        
        /**
         * Invokes the callback on this Node and its children (and children's
         * children ...) for all nodes that intersect the query bounds. Assumes
         * that this node intersects the query bounds.
         * 
         * @param query The aabb query
         * @param callback The callback to run on all nodes containing query
         * @param nodeBounds The bounds of this node
         * @param createChildren True if child nodes can be created if they'd be
         *            contained in the query
         */
        public void visitIntersecting(@Const AxisAlignedBox query, NodeCallback<AxisAlignedBox, T> callback, AxisAlignedBox nodeBounds, boolean createChildren) {
            // visit the callback
            callback.visit(query, this, nodeBounds);
            
            if (!isLeaf() && (children != null || createChildren)) {
                int minIndex = getChildIndex(nodeBounds, query.min);
                int maxIndex = getChildIndex(nodeBounds, query.max);
                int constraint = ~(minIndex ^ maxIndex);
                int childMatch = minIndex & constraint; // note that maxIndex & constraint would work too
                
                for (int i = 0; i < 8; i++) {
                    if ((i & constraint) == childMatch) {
                        Node<T> child = getChild(i, createChildren);
                        if (child == null)
                            continue;
                        
                        // resize center to pass to the child, visit and revert
                        updateForChild(i, nodeBounds);
                        child.visitIntersecting(query, callback, nodeBounds, createChildren);
                        updateForParent(i, nodeBounds);
                    }
                }
            }
        }

        /**
         * Invokes the callback on this Node and its children (and children's
         * children ...) for all nodes that intersect the query bounds. Assumes
         * that this node intersects the query bounds.
         * 
         * @param f The frustum query
         * @param callback The callback to run
         * @param nodeBounds The bounds of the this Node
         */
        public void visitFrustum(Frustum f, NodeCallback<Frustum, T> callback, AxisAlignedBox nodeBounds, PlaneState ps) {
            // visit the callback
            callback.visit(f, this, nodeBounds);
            
            if (children != null) {
                // save plane state
                int save = ps.get();
                
                // now check children
                for (int i = 0; i < 8; i++) {
                    if (children[i] != null) {
                        // update bounds for the current node
                        updateForChild(i, nodeBounds);

                        // use the plane state and frustum intersection
                        FrustumIntersection test = nodeBounds.intersects(f, ps);
                        if (test != FrustumIntersection.OUTSIDE)
                            children[i].visitFrustum(f, callback, nodeBounds, ps);
                        
                        // restore planestate and bounds
                        updateForParent(i, nodeBounds);
                        ps.set(save);
                    }
                }
            }
        }
    }
    
    private static class Key<T>  {
        final Octree<T> owner;
        final T data;
        
        Node<T> parent;
        int nodeIndex;
        
        final AxisAlignedBox bounds;
        int queryNumber;
        
        public Key(T data, @Const AxisAlignedBox bounds, Octree<T> owner) {
            this.data = data;
            this.owner = owner;
            this.bounds = new AxisAlignedBox(bounds);
        }
        
        public void update(@Const AxisAlignedBox newBounds) {
            // regular update within the tree, do a remove and then a re-add
            if (!bounds.equals(newBounds)) {
                removeFromRoot();
                bounds.set(newBounds);
                addToRoot();
            }
        }
        
        public void removeFromRoot() {
            parent.remove(this, nodeIndex);
            parent = null;
        }
        
        public void addToRoot() {
            DeepestNodeCallback<T> toAdd = new DeepestNodeCallback<T>();
            owner.root.visitContaining(bounds, toAdd, owner.rootBounds, true);
            parent = toAdd.deepestNode;
            nodeIndex = parent.add(this);
        }
    }
    
    /*
     * Callbacks for traversing the node tree in various ways
     */
    
    private static interface NodeCallback<Q, T> {
        public void visit(Q query, Node<T> node, AxisAlignedBox nodeBounds);
    }
    
    private static class DeepestNodeCallback<T> implements NodeCallback<AxisAlignedBox, T> {
        Node<T> deepestNode;
        
        @Override
        public void visit(@Const AxisAlignedBox query, Node<T> node, AxisAlignedBox nodeBounds) {
            if (deepestNode == null || node.depth < deepestNode.depth)
                deepestNode = node;
        }
    }
    
    private static class AabbQueryCallback<T> implements NodeCallback<AxisAlignedBox, T> {
        final QueryCallback<T> callback;
        final int query;
        
        public AabbQueryCallback(QueryCallback<T> callback, int query) {
            this.callback = callback;
            this.query = query;
        }
        
        @Override
        public void visit(@Const AxisAlignedBox query, Node<T> node, AxisAlignedBox nodeBounds) {
            if (node.items != null) {
                Key<T> key;
                int count = node.items.size();
                for (int i = 0; i < count; i++) {
                    key = node.items.get(i);
                    if (key.queryNumber == this.query)
                        continue;
                    key.queryNumber = this.query;
                    
                    if (query.intersects(key.bounds))
                        callback.process(key.data, key.bounds);
                }
            }
        }
    }
    
    private static class FrustumQueryCallback<T> implements NodeCallback<Frustum, T> {
        final PlaneState ps;
        final QueryCallback<T> callback;
        final int query;
        
        public FrustumQueryCallback(QueryCallback<T> callback, PlaneState ps, int query) {
            this.ps = ps;
            this.callback = callback;
            this.query = query;
        }
        
        @Override
        public void visit(Frustum query, Node<T> node, AxisAlignedBox nodeBounds) {
            if (node.items != null) {
                // save old plane state to restore after each child
                int save = ps.get();
                
                Key<T> key;
                int ct = node.items.size();
                for (int i = 0; i < ct; i++) {
                    key = node.items.get(i);
                    if (key.queryNumber == this.query)
                        continue;
                    key.queryNumber = this.query;
                    
                    if (key.bounds.intersects(query, ps) != FrustumIntersection.OUTSIDE)
                        callback.process(key.data, key.bounds);
                    
                    // restore plane state
                    ps.set(save);
                }
            }
        }
    }
}
