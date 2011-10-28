package com.ferox.math.bounds;

import java.util.HashSet;
import java.util.Set;

import com.ferox.math.ReadOnlyVector3f;
import com.ferox.math.Vector3f;
import com.ferox.math.bounds.Frustum.FrustumIntersection;
import com.ferox.util.Bag;

/**
 * <p>
 * Octree is a SpatialHierarchy implementation that represents the octree data
 * structure. The octree partitions space into an organized hierarchy of cubes.
 * A cube node within the octree can be evenly split into 8 nested cubes which
 * partition its space. Many octree implementations restrict the size of the
 * root node, but this implementation can expand as needed to include items.
 * Similarly, this octree supports relatively fast updates for moving objects.
 * </p>
 * <p>
 * This octree provides two variants on its behavior for storing items in its
 * tree. A {@link Strategy#STATIC static octree} only stores items in leaf nodes
 * and allows an item to be contained in multiple nodes. It features very fast
 * query times, especially with intersection pairs, but its updates and removals
 * are slower. A {@link Strategy#DYNAMIC dynamic octree} only stores items as
 * deep as possible before they overlap an edge of a node. This means that
 * updates and removals are very fast, but many items can be placed at the top
 * of the tree, reducing the efficiency of queries (especially intersection pair
 * queries).
 * </p>
 * 
 * @author Michael Ludwig
 * @param <T> Class type of items contained by the Octree
 */
@SuppressWarnings("unchecked")
public class Octree<T> implements SpatialHierarchy<T> {
    /**
     * Strategy specifies the two available strategies for item placement within
     * the octree. See above for more details.
     */
    public static enum Strategy {
        /**
         * Items are restricted to a single parent in the tree. Features fast
         * updates and removals, reasonably fast visibility queries, slower
         * intersection pair queries.
         */
        DYNAMIC,
        /**
         * Items are restricted to leaf nodes and exist in multiple leaves at a
         * time. Features fast queries, but slower updates and removals.
         */
        STATIC
    }
    
    public static final int DEFAULT_MAX_DEPTH = 6;
    
    private int queryNumber;

    private Node<T> root;
    private final AxisAlignedBox rootBounds; // all other node bounds implicit from this
    
    private final Strategy strategy;
    
    private final Bag<Key<T>> nullBounds;
    private final Set<Node<T>> pendingRemovals;

    /**
     * Create a new Octree with dimensions of 100 and an initial max depth of 6.
     * As the octree expands to enclose more nodes, its depth and dimensions can increase.
     * 
     * @param strategy The desired strategy
     * @throws NullPointerException if strategy is null
     */
    public Octree(Strategy strategy) {
        this(strategy, 100f);
    }

    /**
     * Create a new Octree with the given side dimension of its root cube, and an
     * initial max depth of 6. As the octree expands to enclose more nodes, its
     * depth and dimensions can increase.
     * 
     * @param strategy The desired strategy
     * @param side The desired side length for the root node
     * @throws NullPointerException if strategy is null
     * @throws IllegalArgumentException if side is less than 0
     */
    public Octree(Strategy strategy, float side) {
        this(strategy, side, DEFAULT_MAX_DEPTH);
    }

    /**
     * Create an Octree with the given side dimension of its root cube and the
     * specified initial max depth. As the octree expands to enclose more nodes,
     * its depth and dimensions can increase.
     * 
     * @param strategy The desired strategy
     * @param side The desired side length for the root node
     * @param initialMaxDepth The initial max depth of the tree
     * @throws NullPointerException if strategy is null
     * @throws IllegalArgumentException if side is less than 0, or if
     *             initialMaxDepth < 1
     */
    public Octree(Strategy strategy, float side, int initialMaxDepth) {
        this(strategy, new AxisAlignedBox(new Vector3f(-side / 2f, -side / 2f, -side / 2f), 
                                          new Vector3f(side / 2f, side / 2f, side / 2f)),
             initialMaxDepth);
    }

    /**
     * Create an Octree with the given bounds for its root node and an initial
     * max tree depth of 6. As the octree expands to enclose more nodes, its
     * depth and dimensions can increase.
     * 
     * @param strategy The desired strategy
     * @param extents The desired root node bounds
     * @throws NullPointerException if strategy or extents are null
     */
    public Octree(Strategy strategy, AxisAlignedBox extents) {
        this(strategy, extents, DEFAULT_MAX_DEPTH);
    }

    /**
     * Create an Octree with the given bounds for its root node and the
     * specified initial max depth. As the octree expands to enclose more nodes,
     * its depth and dimensions can increase.
     * 
     * @param strategy The desired strategy
     * @param extents The desired root node bounds
     * @param initialMaxDepth The initial max depth of the tree
     * @throws NullPointerException if strategy or extents are null
     * @throws IllegalArgumentException if initialMaxDepth < 1
     */
    public Octree(Strategy strategy, AxisAlignedBox extents, int initialMaxDepth) {
        if (strategy == null)
            throw new NullPointerException("Strategy cannot be null");
        if (extents == null)
            throw new NullPointerException("Extents cannot be null");
        if (initialMaxDepth <= 0)
            throw new IllegalArgumentException("Initial max depth must be at least 1, not: " + initialMaxDepth);
        if (extents.getMax().getX() < extents.getMin().getX() ||
            extents.getMax().getY() < extents.getMin().getY() ||
            extents.getMax().getZ() < extents.getMin().getZ())
            throw new IllegalArgumentException("Invalid root bounds: " + extents);
        
        this.strategy = strategy;
        
        rootBounds = new AxisAlignedBox(extents);
        root = new Node<T>(null, initialMaxDepth - 1);
        nullBounds = new Bag<Key<T>>();
        pendingRemovals = new HashSet<Node<T>>();
        
        queryNumber = 0;
    }
    
    @Override
    public Object add(T item, AxisAlignedBox bounds) {
        if (item == null)
            throw new NullPointerException("Item cannot be null");
        
        expandOctreeMaybe(bounds);
        Key<T> newKey = new Key<T>(item, bounds, this);
        if (bounds == null) {
            newKey.addToNull();
        } else
            newKey.addToRoot();
        
        return newKey;
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
                if (ok.bounds == null)
                    ok.removeFromNull();
                else
                    ok.removeFromRoot();
                return;
            }
        }

        // else key is invalid
        throw new IllegalArgumentException("Invalid key: " + key);
    }

    @Override
    public void update(T item, AxisAlignedBox bounds, Object key) {
        if (item == null)
            throw new NullPointerException("Item cannot be null");
        if (key == null)
            throw new NullPointerException("Key cannot be null");
        
        if (key instanceof Key) {
            Key<T> ok = (Key<T>) key;
            if (ok.owner == this && ok.data == item) {
                // key is valid, so update it
                expandOctreeMaybe(bounds);
                ok.update(bounds);
                return;
            }
        }
        
        // else key is invalid
        throw new IllegalArgumentException("Invalid key: " + key);
    }
    
    @Override
    public void query(AxisAlignedBox volume, QueryCallback<T> callback) {
        if (volume == null)
            throw new NullPointerException("Query volume cannot be null");
        if (callback == null)
            throw new NullPointerException("QueryCallback cannot be null");
        
        // prune tree before query to make it the most efficient possible
        pruneTree();
        
        root.visitIntersecting(volume, new AabbQueryCallback<T>(callback, ++queryNumber), rootBounds, false);
        
        // now report all null bound elements, too
        int count = nullBounds.size();
        for (int i = 0; i < count; i++)
            callback.process(nullBounds.get(i).data);
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
        
        // visit all children with null bounds
        int count = nullBounds.size();
        for (int i = 0; i < count; i++)
            callback.process(nullBounds.get(i).data);
    }

    @Override
    public void query(IntersectionCallback<T> callback) {
        if (callback == null)
            throw new NullPointerException("Callback cannot be null");
        
        // prune tree before query to make it the most efficient possible
        pruneTree();
        
        if (strategy == Strategy.STATIC)
            root.visitIntersecting(new AxisAlignedBox(rootBounds), new IntersectionQueryCallback<T>(callback), rootBounds, false);
        else
            queryIntersectionsDynamic(root, callback);
    }
    
    /*
     * Internal implementations of queries based on the supported strategies
     */
    
    private void queryIntersectionsDynamic(Node<T> node, IntersectionCallback<T> callback) {
        // append all intersections within this node
        detectIntersections(node, node, callback);
        
        // traverse parents
        Node<T> p = node.parent;
        while(p != null) {
            detectIntersections(node, p, callback);
            p = p.parent;
        }
        
        // recurse to children
        if (node.children != null) {
            for (int i = 0; i < node.children.length; i++) {
                if (node.children[i] != null)
                    queryIntersectionsDynamic(node.children[i], callback);
            }
        }
    }
    
    private void detectIntersections(Node<T> n1, Node<T> n2, IntersectionCallback<T> callback) {
        if (n1.items == null || n2.items == null)
            return; // can't have any intersections
        
        Key<T> e1, e2;
        
        // find intersections between n1 and n2's children
        if (n1 == n2) {
            // same node optimizations
            int ct = n1.items.size();
            for (int i = 0; i < ct; i++) {
                e1 = n1.items.get(i);
                for (int j = i + 1; j < ct; j++) {
                    e2 = n1.items.get(j);
                    if (e1.bounds.intersects(e2.bounds))
                        callback.process(e1.data, e2.data);
                }
            }
        } else {
            int ct1 = n1.items.size();
            int ct2 = n2.items.size();

            for (int i = 0; i < ct1; i++) {
                e1 = n1.items.get(i);
                for (int j = 0; j < ct2; j++) {
                    e2 = n2.items.get(j);
                    if (e1.bounds.intersects(e2.bounds))
                        callback.process(e1.data, e2.data);
                }
            }
        }
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
    
    private void expandOctreeMaybe(AxisAlignedBox dataBounds) {
        if (dataBounds == null || rootBounds.contains(dataBounds))
            return;
        
        ReadOnlyVector3f dMin = dataBounds.getMin();
        
        Vector3f extents = new Vector3f();
        Vector3f center = new Vector3f();
        
        Node<T> newRoot;
        
        ReadOnlyVector3f rMin, rMax;
        int rootChildIndex;
        while(!rootBounds.contains(dataBounds)) {
            // the current root will be placed within the positive 
            // half-plane for any axis if it is even partially above
            // the data bounds, otherwise it is in the negative
            rootChildIndex = 0;
            rMin = rootBounds.getMin();
            rMax = rootBounds.getMax();
            if (rMin.getX() > dMin.getX()) {
                rootChildIndex |= POS_X;
                center.setX(rMin.getX());
            } else
                center.setX(rMax.getX());
            
            if (rMin.getY() > dMin.getY()) {
                rootChildIndex |= POS_Y;
                center.setY(rMin.getY());
            } else
                center.setY(rMax.getY());
            
            if (rMin.getZ() > dMin.getZ()) {
                rootChildIndex |= POS_Z;
                center.setZ(rMin.getZ());
            } else
                center.setZ(rMax.getZ());
            
            rMax.sub(rMin, extents); // get axis lengths of the root
            rootBounds.getMin().set(center.getX() - extents.getX(), center.getY() - extents.getY(), center.getZ() - extents.getZ());
            rootBounds.getMax().set(center.getX() + extents.getX(), center.getY() + extents.getY(), center.getZ() + extents.getZ());
            
            newRoot = new Node<T>(null, root.depth + 1);
            newRoot.children = new Node[8];
            newRoot.children[rootChildIndex] = root;
            
            root.parent = newRoot;
            root = newRoot;
        }
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
    private static int getChildIndex(AxisAlignedBox nodeBounds, ReadOnlyVector3f pointInNode) {
        Vector3f min = nodeBounds.getMin();
        Vector3f max = nodeBounds.getMax();
        
        int index = 0;
        if ((min.getX() + max.getX()) < 2f * pointInNode.getX())
            index |= POS_X;
        if ((min.getY() + max.getY()) < 2f * pointInNode.getY())
            index |= POS_Y;
        if ((min.getZ() + max.getZ()) < 2f * pointInNode.getZ())
            index |= POS_Z;
        return index;
    }
    
    private static void updateForChild(int child, AxisAlignedBox bounds) {
        Vector3f min = bounds.getMin();
        Vector3f max = bounds.getMax();
        
        if (inPositiveX(child))
            min.setX((min.getX() + max.getX()) / 2f);
        else
            max.setX((min.getX() + max.getX()) / 2f);
        
        if (inPositiveY(child))
            min.setY((min.getY() + max.getY()) / 2f);
        else
            max.setY((min.getY() + max.getY()) / 2f);
        
        if (inPositiveZ(child))
            min.setZ((min.getZ() + max.getZ()) / 2f);
        else
            max.setZ((min.getZ() + max.getZ()) / 2f);
    }
    
    private static void updateForParent(int child, AxisAlignedBox bounds) {
        Vector3f min = bounds.getMin();
        Vector3f max = bounds.getMax();
        
        if (inPositiveX(child))
            min.setX(2 * min.getX() - max.getX());
        else
            max.setX(2 * max.getX() - min.getX());
        
        if (inPositiveY(child))
            min.setY(2 * min.getY() - max.getY());
        else
            max.setY(2 * max.getY() - min.getY());
        
        if (inPositiveZ(child))
            min.setZ(2 * min.getZ() - max.getZ());
        else
            max.setZ(2 * max.getZ() - min.getZ());
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
            if (index < 0) {
                // wat? FIXME something is buggy here
                return;
            }
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
        public void visitContaining(AxisAlignedBox query, NodeCallback<AxisAlignedBox, T> callback, AxisAlignedBox nodeBounds, boolean createChildren) {
            // since we assume this node contains query, run the callback right away
            callback.visit(query, this, nodeBounds);
            
            // now visit children, if we have any
            if (!isLeaf() && (children != null || createChildren)) {
                int minChildIndex = getChildIndex(nodeBounds, query.getMin());
                int maxChildIndex = getChildIndex(nodeBounds, query.getMax());
                
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
        public void visitIntersecting(AxisAlignedBox query, NodeCallback<AxisAlignedBox, T> callback, AxisAlignedBox nodeBounds, boolean createChildren) {
            // visit the callback
            callback.visit(query, this, nodeBounds);
            
            if (!isLeaf() && (children != null || createChildren)) {
                int minIndex = getChildIndex(nodeBounds, query.getMin());
                int maxIndex = getChildIndex(nodeBounds, query.getMax());
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
        @SuppressWarnings("rawtypes")
        private static final Node MULTI_PARENT = new Node(null, 0);

        final Octree<T> owner;
        final T data;
        
        Node<T> parent;
        int nodeIndex;
        
        AxisAlignedBox bounds;
        int queryNumber;
        
        public Key(T data, AxisAlignedBox bounds, Octree<T> owner) {
            this.data = data;
            this.owner = owner;
            this.bounds = (bounds == null ? null : new AxisAlignedBox(bounds));
        }
        
        public void removeFromNull() {
            owner.nullBounds.remove(nodeIndex);
            if (owner.nullBounds.size() != nodeIndex)
                owner.nullBounds.get(nodeIndex).nodeIndex = nodeIndex;
            nodeIndex = -1;
        }
        
        public void addToNull() {
            owner.nullBounds.add(this);
            nodeIndex = owner.nullBounds.size() - 1;
        }
        
        public void update(AxisAlignedBox newBounds) {
            if (newBounds == null) {
                if (bounds != null) {
                    // bounds switched to null, so remove it from root and add to null list
                    removeFromRoot();
                    addToNull();
                } // else, no real update occurred
            } else {
                if (bounds == null) {
                    // bounds switched from null, do a full add
                    removeFromNull();
                    bounds = new AxisAlignedBox(newBounds);
                    addToRoot();
                } else {
                    // regular update within the tree, do a remove and then a re-add
                    if (!bounds.equals(newBounds)) {
                        removeFromRoot();
                        bounds.set(newBounds);
                        addToRoot();
                    }
                }
            }
        }
        
        public void removeFromRoot() {
            if (parent == MULTI_PARENT || parent == null) {
                StaticUpdateCallback<T> remover = new StaticUpdateCallback<T>(this, false);
                owner.root.visitIntersecting(bounds, remover, owner.rootBounds, false);
            } else
                parent.remove(this, nodeIndex);
            
            parent = null;
        }
        
        public void addToRoot() {
            if (owner.strategy == Strategy.DYNAMIC) {
                DeepestNodeCallback<T> toAdd = new DeepestNodeCallback<T>();
                owner.root.visitContaining(bounds, toAdd, owner.rootBounds, true);
                parent = toAdd.deepestNode;
                nodeIndex = parent.add(this);
            } else {
                // reset parent, so a single node is identified properly
                parent = null;
                
                StaticUpdateCallback<T> adder = new StaticUpdateCallback<T>(this, true);
                owner.root.visitIntersecting(bounds, adder, owner.rootBounds, true);
            }
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
        public void visit(AxisAlignedBox query, Node<T> node, AxisAlignedBox nodeBounds) {
            if (deepestNode == null || node.depth < deepestNode.depth)
                deepestNode = node;
        }
    }
    
    private static class StaticUpdateCallback<T> implements NodeCallback<AxisAlignedBox, T> {
        final Key<T> key;
        final boolean add;
        
        public StaticUpdateCallback(Key<T> key, boolean add) {
            this.key = key;
            this.add = add;
        }
        
        @Override
        public void visit(AxisAlignedBox query, Node<T> node, AxisAlignedBox nodeBounds) {
            if (node.isLeaf()) {
                if (add) {
                    int index = node.add(key);
                    if (key.parent == null) {
                        // so far, we're in a single node so track it
                        key.parent = node;
                        key.nodeIndex = index;
                    } else {
                        // no longer a single node, clear the parent
                        key.parent = Key.MULTI_PARENT;
                    }
                } else
                    node.remove(key, -1);
            }
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
        public void visit(AxisAlignedBox query, Node<T> node, AxisAlignedBox nodeBounds) {
            if (node.items != null) {
                Key<T> key;
                int count = node.items.size();
                for (int i = 0; i < count; i++) {
                    key = node.items.get(i);
                    if (key.queryNumber == this.query)
                        continue;
                    key.queryNumber = this.query;
                    
                    if (query.intersects(key.bounds))
                        callback.process(key.data);
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
                        callback.process(key.data);
                    
                    // restore plane state
                    ps.set(save);
                }
            }
        }
    }
    
    private static class IntersectionQueryCallback<T> implements NodeCallback<AxisAlignedBox, T> {
        final IntersectionCallback<T> callback;
        
        public IntersectionQueryCallback(IntersectionCallback<T> callback) {
            this.callback = callback;
        }
        
        @Override
        public void visit(AxisAlignedBox query, Node<T> node, AxisAlignedBox nodeBounds) {
            if (node.items != null) {
                float nx = nodeBounds.getMin().getX();
                float ny = nodeBounds.getMin().getY();
                float nz = nodeBounds.getMin().getZ();
                
                boolean iPastX, iPastY, iPastZ;
                boolean skipX, skipY, skipZ;
                
                Key<T> ki, kj;
                int ct = node.items.size();
                for (int i = 0; i < ct; i++) {
                    ki = node.items.get(i);
                    iPastX = ki.bounds.getMin().getX() <= nx;
                    iPastY = ki.bounds.getMin().getY() <= ny;
                    iPastZ = ki.bounds.getMin().getZ() <= nz;
                    
                    // check same-node intersections
                    for (int j = i + 1; j < ct; j++) {
                        // FIXME: what happens when we're at the left edge of root bounds?
                        // then we want to still include things
                        kj = node.items.get(j);
                        skipX = iPastX && kj.bounds.getMin().getX() <= nx;
                        skipY = iPastY && kj.bounds.getMin().getY() <= ny;
                        skipZ = iPastZ && kj.bounds.getMin().getZ() <= nz;
                        
                        if (!skipX && !skipY && !skipZ) {
                            // this is the most negative node that both items exist in,
                            // so we can report an intersection w/o worrying about duplicates
                            if (ki.bounds.intersects(kj.bounds))
                                callback.process(ki.data, kj.data);
                        }
                    }
                }
            }
        }
    }
}
