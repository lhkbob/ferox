package com.ferox.math.bounds;

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
 * 
 * @author Michael Ludwig
 * @param <T> Class type of items contained by the Octree
 */
public class Octree<T> implements SpatialHierarchy<T> {
    private static final ThreadLocal<PlaneState> PLANE_STATE = new ThreadLocal<PlaneState>() {
        @Override
        protected PlaneState initialValue() { return new PlaneState(); }
    };
    
    private final float deepestChildSize;
    private OctreeNode<T> root;

    /**
     * Create a new Octree that has an empty root node with the given bounds.
     * Its children will then partition those bounds as needed. The created
     * Octree will create children nodes until the diagonal of a child would be
     * smaller than 1. Subsequent changes to <tt>initialExtents</tt> will not be
     * reflected within this Octree.
     * 
     * @param initialExtents The initial bounds for the root node
     * @throws NullPointerException if initialExtents is null
     */
    public Octree(AxisAlignedBox initialExtents) {
        this(initialExtents, 1f);
    }

    /**
     * Create a new Octree that has an empty root node with the given bounds.
     * Its children will then partition those bounds as needed. The created
     * Octree will create children nodes until the diagonal of a child would be
     * smaller than <tt>deepestChildSize</tt>. A negative value for
     * <tt>deepestChildSize</tt> will disable the depth limiting of the Octree;
     * instead, children will be created until an element can be contained in a
     * single node. Subsequent changes to <tt>initialExtents</tt> will not be
     * reflected within this Octree.
     * 
     * @param initialExtents The initial bounds for the root node
     * @param deepestChildSize The smallest valid diagonal for a child node
     * @throws NullPointerException if initialExtents is null
     */
    public Octree(AxisAlignedBox initialExtents, float deepestChildSize) {
        if (initialExtents == null)
            throw new NullPointerException("Initial extents of Octree cannot be null");
        
        this.deepestChildSize = deepestChildSize;
        root = new OctreeNode<T>(null, new AxisAlignedBox(initialExtents));
    }
    
    @Override
    public Object add(T item, AxisAlignedBox bounds) {
        if (item == null)
            throw new NullPointerException("Item cannot be null");
        
        OctreeKey<T> newKey = new OctreeKey<T>(this, item);
        newKey.bounds = bounds;
        newKey.addToNode(findNode(bounds, null));
        return newKey;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public void update(T item, AxisAlignedBox bounds, Object key) {
        if (item == null)
            throw new NullPointerException("Item cannot be null");
        if (key == null)
            throw new NullPointerException("Key cannot be null");
        
        if (key instanceof OctreeKey) {
            OctreeKey ok = (OctreeKey) key;
            if (ok.hierarchy == this && ok.data == item) {
                // key is valid, find new place within tree and update
                ok.bounds = bounds;
                ok.addToNode(findNode(bounds, ((OctreeKey<T>) ok).owner));
                return;
            }
        }
        
        // else key is invalid
        throw new IllegalArgumentException("Invalid key: " + key);
    }
    
    @SuppressWarnings("unchecked")
    private void expandOctree(AxisAlignedBox dataBounds) {
        Vector3f dMin = dataBounds.getMin();
        
        Vector3f extents = new Vector3f();
        Vector3f center = new Vector3f();
        
        AxisAlignedBox newRootBounds;
        OctreeNode<T> newRoot;
        
        OctreeKey<T> e;
        Vector3f rMin, rMax;
        int rootChildIndex, elementSize;
        while(!root.bounds.contains(dataBounds)) {
            // the current root will be placed within the positive 
            // half-plane for any axis if it is even partially above
            // the data bounds, otherwise it is in the negative
            rootChildIndex = 0;
            rMin = root.bounds.getMin();
            rMax = root.bounds.getMax();
            if (rMin.x > dMin.x) {
                rootChildIndex |= POS_X;
                center.x = rMin.x;
            } else
                center.x = rMax.x;
            
            if (rMin.y > dMin.y) {
                rootChildIndex |= POS_Y;
                center.x = rMin.y;
            } else
                center.y = rMax.y;
            
            if (rMin.z > dMin.z) {
                rootChildIndex |= POS_Z;
                center.z = rMin.z;
            } else
                center.z = rMax.z;
            
            rMax.sub(rMin, extents); // get axis lengths of the root
            newRootBounds = new AxisAlignedBox();
            newRootBounds.getMin().set(center.x - extents.x, center.y - extents.y, center.z - extents.z);
            newRootBounds.getMax().set(center.x + extents.x, center.y + extents.y, center.z + extents.z);
            
            newRoot = new OctreeNode<T>(null, newRootBounds);
            newRoot.children = new OctreeNode[8];
            newRoot.children[rootChildIndex] = root;
            root.parent = newRoot;

            elementSize = root.elements.size();
            for (int i = elementSize - 1; i >= 0; i--) {
                e = root.elements.get(i);
                if (e.bounds == null) {
                    // must lift null bound elements up a level
                    e.addToNode(newRoot);
                }
            }
            
            root = newRoot;
        }
    }
    
    private OctreeNode<T> findChild(AxisAlignedBox bounds, OctreeNode<T> node) {
        Vector3f dMin = bounds.getMin();
        Vector3f dMax = bounds.getMax();

        float childDiag = node.bounds.getMin().distance(node.bounds.getMax()) / 2f;
        int minChildIndex, maxChildIndex;
        // this loop stops once the bounds won't fit into a code, or if
        // the child code is too small to exist within this tree
        while(childDiag > deepestChildSize) {
            minChildIndex = node.getChildIndex(dMin);
            maxChildIndex = node.getChildIndex(dMax);

            if (minChildIndex == maxChildIndex) {
                // data bounds fits within a single child so go deeper
                node = node.getChild(minChildIndex);
                childDiag /= 2f;
            } else {
                // data bounds spans multiple children, store in current node
                break;
            }
        }
        
        return node;
    }
    
    private OctreeNode<T> findNode(AxisAlignedBox bounds, OctreeNode<T> current) {
        if (bounds == null)
            return root; // no bounds, use the current root
        if (current == null)
            current = root; // default to root for a new search
        
        while(current != null && !current.bounds.contains(bounds)) {
            // move up parent hierarchy
            current = current.parent;
        }
        
        if (current != null) {
            // found a highest node for the item, attempt to insert it
            return findChild(bounds, current);
        } else {
            // item has moved above the root, grow the octree
            expandOctree(bounds);
            // find a child using the larger root
            return findChild(bounds, root);
        }
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public void remove(T item, Object key) {
        if (item == null)
            throw new NullPointerException("Item cannot be null");
        if (key == null)
            throw new NullPointerException("Key cannot be null");
        
        if (key instanceof OctreeKey) {
            OctreeKey ok = (OctreeKey) key;
            if (ok.hierarchy == this && ok.data == item) {
                // key is valid so quickly remove it
                ok.removeFromNode();
                return;
            }
        }
        
        // else invalid key, so we fail
        throw new IllegalArgumentException("Invalid key: " + key);
    }
    
    @Override
    public Bag<T> query(AxisAlignedBox volume, Bag<T> results) {
        if (volume == null)
            throw new NullPointerException("Volume cannot be null");
        if (results == null)
            results = new Bag<T>();
        
        query(volume, root, results);
        return results;
    }
    
    private void query(AxisAlignedBox volume, OctreeNode<T> node, Bag<T> results) {
        // check all items at current level
        OctreeKey<T> item;
        int size = node.elements.size();
        for (int i = 0; i < size; i++) {
            item = node.elements.get(i);
            if (item.bounds == null || volume.intersects(item.bounds))
                results.add(item.data);
        }

        if (node.children != null) {
            int minIndex = node.getChildIndex(volume.getMin());
            int maxIndex = node.getChildIndex(volume.getMax());
            int constraint = ~(minIndex ^ maxIndex);
            int childMatch = minIndex & constraint; // note that maxIndex & constraint would work too
            
            for (int i = 0; i < 8; i++) {
                if ((i & constraint) == childMatch && node.children[i] != null)
                    query(volume, node.children[i], results);
            }
        }
    }

    @Override
    public Bag<T> query(Frustum frustum, Bag<T> results) {
        if (frustum == null)
            throw new NullPointerException("Frustum cannot be null");
        if (results == null)
            results = new Bag<T>();
        
        PlaneState planeState = PLANE_STATE.get();
        planeState.reset();
        
        FrustumIntersection rootTest = root.bounds.intersects(frustum, planeState);
        if (rootTest != FrustumIntersection.OUTSIDE)
            query(frustum, root, planeState, rootTest == FrustumIntersection.INSIDE, results);
        return results;
    }
    
    private void query(Frustum frustum, OctreeNode<T> node, PlaneState ps, boolean alwaysPass, Bag<T> results) {
        // save old plane state to restore after each child
        int save = ps.get();
        
        // check all items at current level
        OctreeKey<T> key;
        int size = node.elements.size();
        for (int i = 0; i < size; i++) {
            key = node.elements.get(i);
            if (alwaysPass || key.bounds == null || 
                key.bounds.intersects(frustum, ps) != FrustumIntersection.OUTSIDE)
                results.add(key.data);
            ps.set(save); // restore plane state for next iteration
        }
        
        if (node.children != null) {
            // now check children
            for (int i = 0; i < 8; i++) {
                if (node.children[i] != null) {
                    if (alwaysPass) {
                        // always query children since this node is completely inside the frustum
                        query(frustum, node.children[i], ps, true, results);
                    } else {
                        // use the plane state and frustum intersection
                        FrustumIntersection test = node.children[i].bounds.intersects(frustum, ps);
                        if (test != FrustumIntersection.OUTSIDE)
                            query(frustum, node.children[i], ps, test == FrustumIntersection.INSIDE, results);
                        
                        // restore plane state
                        ps.set(save);
                    }
                }
            }
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

    private static class OctreeNode<T> {
        OctreeNode<T>[] children; // 8, can be null or have null elements
        OctreeNode<T> parent; // null when the root
        
        final AxisAlignedBox bounds;
        final Vector3f center;
        final Bag<OctreeKey<T>> elements;
        
        public OctreeNode(OctreeNode<T> parent, AxisAlignedBox aabb) {
            this.parent = parent;
            bounds = aabb;
            center = aabb.getCenter(); // cache this, too, since bounds won't change anymore
            elements = new Bag<OctreeKey<T>>();
        }
        
        public int getChildIndex(Vector3f extent) {
            int index = 0;
            if (center.x < extent.x)
                index |= POS_X;
            if (center.y < extent.y)
                index |= POS_Y;
            if (center.z < extent.z)
                index |= POS_Z;
            return index;
        }
        
        @SuppressWarnings("unchecked")
        public OctreeNode<T> getChild(int index) {
            // allocate child array if needed
            if (children == null)
                children = new OctreeNode[8];
            
            if (children[index] == null) {
                // generate child bounds
                AxisAlignedBox childBounds = new AxisAlignedBox(center, center);
                
                if ((index & POS_X) != 0)
                    childBounds.getMax().x = bounds.getMax().x;
                else
                    childBounds.getMin().x = bounds.getMin().x;
                
                if ((index & POS_Y) != 0)
                    childBounds.getMax().y = bounds.getMax().y;
                else
                    childBounds.getMin().y = bounds.getMin().y;
                
                if ((index & POS_Z) != 0)
                    childBounds.getMax().z = bounds.getMax().z;
                else
                    childBounds.getMin().z = bounds.getMin().z;
                
                // insert new child node
                OctreeNode<T> child = new OctreeNode<T>(this, childBounds);
                children[index] = child;
                return child;
            } else
                return children[index];
        }
        
        public void removeChild(OctreeNode<T> child) {
            // remove child from children array and check if the entire array is empty
            boolean hasChildren = false;
            if (children != null) {
                for (int i = 0; i < 8; i++) {
                    if (children[i] == child)
                        children[i] = null;
                    else if (children[i] != null)
                        hasChildren = true;
                }
            }
            
            if (!hasChildren && elements.size() == 0) {
                // has no more children nodes and no elements of its own,
                // so continue the removal process up to its parent
                if (parent != null)
                    parent.removeChild(this);
            }
        }
    }
    
    private static class OctreeKey<T> {
        final T data;
        AxisAlignedBox bounds;
        
        OctreeNode<T> owner;
        int index;
        
        final Octree<T> hierarchy;
        
        public OctreeKey(Octree<T> hierarchy, T data) {
            this.hierarchy = hierarchy;
            this.data = data;
        }
        
        public void addToNode(OctreeNode<T> node) {
            if (owner == node)
                return; // no change
            
            // insert first before removing in case the new node
            // is a parent of the old node: we don't want that one's
            // removal to clean up the entire chain when we're really just
            // swapping the key
            int oldIndex = (owner == null ? -1 : index);
            OctreeNode<T> oldOwner = owner;
            
            owner = node;
            index = node.elements.size();
            node.elements.add(this);
            
            // now cleanup old owner if needed
            if (oldOwner != null)
                removeFromNode(oldOwner, oldIndex);
        }
        
        public void removeFromNode() {
            removeFromNode(owner, index);
        }
        
        private void removeFromNode(OctreeNode<T> owner, int index) {
            owner.elements.remove(index);
            if (index != owner.elements.size()) {
                // update swapped index
                owner.elements.get(index).index = index;
            }
            
            if (owner.elements.size() == 0 && owner.parent != null) {
                // we need to clean up the owner
                owner.parent.removeChild(owner);
            }
        }
    }
}
