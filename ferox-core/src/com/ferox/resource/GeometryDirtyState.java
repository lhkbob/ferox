package com.ferox.resource;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * GeometryDirtyState is the DirtyState that is used by Geometry and returned by
 * calls to {@link Geometry#getDirtyState()}.
 * 
 * @author Michael Ludwig
 */
public class GeometryDirtyState implements DirtyState<GeometryDirtyState> {
    /**
     * <p>
     * BufferRange represents an offset into a VectorBuffer or array and a
     * positive length. All affected elements lie within the range [offset,
     * offset + length]. BufferRange makes no guarantees that its 'range' lies
     * within a valid section of an array. It may be that its length extends
     * beyond the last actual index.
     * </p>
     * <p>
     * BufferRanges are immutable objects.
     * </p>
     */
    public static class BufferRange {
        private final int offset;
        private final int len;

        /**
         * Create a new BufferRange that uses the given offset and length. The
         * specified offset is clamped to be at least 0, however the length
         * cannot be less than 1.
         * 
         * @param offset The offset to use, will be clamped to be >= 0
         * @param len The length of this range
         * @throws IllegalArgumentException if len < 1
         */
        public BufferRange(int offset, int len) {
            if (len < 1)
                throw new IllegalArgumentException("Length of range must be at least 1: " + len);
            this.offset = Math.max(0, offset);
            this.len = len;
        }
        
        /**
         * @return The offset of this BufferRange
         */
        public int getOffset() {
            return offset;
        }
        
        /**
         * @return The length of this BufferRange
         */
        public int getLength() {
            return len;
        }
    }
    
    private final Set<String> newAttributes;
    private final Set<String> delAttributes;
    
    private final Map<String, BufferRange> dirtyAttributes;
    
    private final BufferRange dirtyIndices;
    
    /**
     * Create a new GeometryDirtyState that initially has nothing marked as
     * dirty.
     */
    public GeometryDirtyState() {
        newAttributes = new HashSet<String>();
        delAttributes = new HashSet<String>();
        
        dirtyAttributes = new HashMap<String, BufferRange>();
        dirtyIndices = null;
    }
    
    private GeometryDirtyState(Set<String> newAttr, Set<String> delAttr, 
                               Map<String, BufferRange> dirtyAttr, BufferRange dirtyIndices) {
        newAttributes = newAttr;
        delAttributes = delAttr;
        dirtyAttributes = dirtyAttr;
        
        this.dirtyIndices = dirtyIndices;
    }

    /**
     * @return The set of attribute names that have been added to the Geometry.
     *         This is an immutable Set
     */
    public Set<String> getAddedAttributes() {
        return Collections.unmodifiableSet(newAttributes);
    }

    /**
     * @return The set of attribute names that have been removed from the
     *         Geometry. This is an immutable Set
     */
    public Set<String> getRemovedAttributes() {
        return Collections.unmodifiableSet(delAttributes);
    }

    /**
     * @return The map containing modified attribute information. Each key
     *         represents an attribute that's been modified, the associated
     *         BufferRange are the values that are dirty
     */
    public Map<String, BufferRange> getModifiedAttributes() {
        return Collections.unmodifiableMap(dirtyAttributes);
    }

    /**
     * @return The BufferRange of dirty indices, a null return value signals
     *         that the indices are not dirty
     */
    public BufferRange getModifiedIndices() {
        return dirtyIndices;
    }

    /**
     * Return a new GeometryDirtyState that is equivalent to this
     * GeometryDirtyState except that the BufferRange returned by
     * {@link #getModifiedIndices()} is union'ed with the range represented by
     * the given offset and length.
     * 
     * @param offset The offset of dirty indices to include
     * @param length The length of the dirty indices range, must be at least 1
     * @return A GeometryDirtyState that includes the updated indices
     * @throws IllegalArgumentException if length < 1
     */
    public GeometryDirtyState updateIndices(int offset, int length) {
        BufferRange di = merge(offset, length, dirtyIndices);
        return new GeometryDirtyState(newAttributes, delAttributes, dirtyAttributes, di);
    }

    /**
     * <p>
     * Return a new GeometryDirtyState that is equivalent to this
     * GeometryDirtyState except that the BufferRange stored at <tt>name</tt> in
     * the Map returned by {@link #getModifiedAttributes()} has been updated to
     * include the range represented by the given offset and length.
     * </p>
     * <p>
     * If <tt>newA</tt> is true, then the attribute name is also included in the
     * returned GeometryDirtyState's added attribute set. If the name was
     * previously included in the removed attribute set, then it will no longer
     * be present there.
     * </p>
     * 
     * @param name The name of the vertex attribute to update
     * @param offset The offset of dirty vertex information
     * @param length The length of the dirty vertex data
     * @param newA True if the attribute name is also 'new' to the Geometry
     * @return A GeometryDirtyState that reflects the changes described above
     * @throws IllegalArgumentException if length < 1
     * @throws NullPointerException if name is null
     */
    public GeometryDirtyState updateAttribute(String name, int offset, int length, boolean newA) {
        if (name == null)
            throw new NullPointerException("Cannot update a null attribute name");
        BufferRange da = merge(offset, length, dirtyAttributes.get(name));
        
        Map<String, BufferRange> attrs = new HashMap<String, BufferRange>(dirtyAttributes);
        attrs.put(name, da);
        
        Set<String> newAttrs = newAttributes;
        Set<String> delAttrs = delAttributes;
        if (newA) {
            if (delAttrs.contains(name)) {
                // not actually new, probably had a removeAttribute() and then setAttribute()
                // called with the same name before an update
                delAttrs = new HashSet<String>(delAttrs);
                delAttrs.remove(name);
            } else {
                newAttrs = new HashSet<String>(newAttrs);
                newAttrs.add(name);
            }
        }
        
        return new GeometryDirtyState(newAttrs, delAttrs, attrs, dirtyIndices);
    }

    /**
     * Return a new GeometryDirtyState that is equivalent to this
     * GeometryDirtyState except that the given attribute name is included in
     * the returned dirty state's set of removed attributes. If the name was
     * previously present in the added attributes or modified attributes, then
     * it is not included in those sets for the returned GeometryDirtyState.
     * 
     * @param name The vertex attribute that has been removed
     * @return A GeometryDirtyState as described above
     * @throws NullPointerException if name is null
     */
    public GeometryDirtyState removeAttribute(String name) {
        if (name == null)
            throw new NullPointerException("Cannot remove a null attribute name");
        
        Map<String, BufferRange> dirtyAttrs = dirtyAttributes;
        Set<String> newAttrs = newAttributes;
        
        Set<String> delAttrs = new HashSet<String>(delAttributes);
        delAttrs.add(name);
        
        if (newAttrs.contains(name)) {
            newAttrs = new HashSet<String>(newAttrs);
            newAttrs.remove(name);
        }
        
        if (dirtyAttrs.containsKey(name)) {
            dirtyAttrs = new HashMap<String, BufferRange>(dirtyAttrs);
            dirtyAttrs.remove(name);
        }
        
        return new GeometryDirtyState(newAttrs, delAttrs, dirtyAttrs, dirtyIndices);
    }
    
    @Override
    public GeometryDirtyState merge(GeometryDirtyState geom) {
        if (geom == null)
            return this;
        
        Set<String> newAttrs = new HashSet<String>(newAttributes);
        // cannot include 'removed' attributes that happened afterwards
        for (String d: geom.newAttributes) {
            if (!delAttributes.contains(d))
                newAttrs.add(d);
        }
        
        Map<String, BufferRange> dirtyAttrs = new HashMap<String, BufferRange>(dirtyAttributes);
        for (String name: geom.dirtyAttributes.keySet()) {
            BufferRange b1 = dirtyAttrs.get(name);
            BufferRange b2 = geom.dirtyAttributes.get(name);
            
            // cannot include 'removed' attributes
            if (!delAttributes.contains(name))
                dirtyAttrs.put(name, merge(b2.offset, b2.len, b1));
        }
        
        Set<String> delAttrs = new HashSet<String>(delAttributes);
        // must only include attributes that haven't been re-added
        for (String d: geom.delAttributes) {
            if (!newAttrs.contains(d) && !dirtyAttrs.containsKey(d))
                delAttrs.add(d);
        }
        
        BufferRange di = (dirtyIndices == null ? geom.dirtyIndices 
                                               : merge(dirtyIndices.offset, dirtyIndices.len, geom.dirtyIndices));
        return new GeometryDirtyState(newAttrs, delAttrs, dirtyAttrs, di);
    }
    
    private BufferRange merge(int offset, int length, BufferRange old) {
        if (old != null) {
            int nO = Math.min(offset, old.offset);
            int nM = Math.max(offset + length, old.offset + old.len);
            return new BufferRange(nO, nM - nO);
        } else
            return new BufferRange(offset, length);
    }
}