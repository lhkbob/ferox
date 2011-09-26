package com.ferox.entity;

/**
 * IndexedDataStore is a generic data storage interface representing packed,
 * random-access data storage for a property of a Component. All Components of
 * the same type in the same EntitySystem will have their properties share the
 * IndexedDataStores so that iteration will have much better cache locality, and
 * will avoid the reorganization caused by Java's garbage collector.
 * 
 * @author Michael Ludwig
 */
public interface IndexedDataStore {
    /**
     * Resize the internal data store of this IndexedDataStore so that it holds
     * enough space for the given number of Properties that will use this data
     * store.
     * 
     * @param size The size, in number of properties
     */
    public void resize(int size);
    
    /**
     * @return The number of properties that can fit into this IndexedDataStore
     */
    public int size();

    /**
     * <p>
     * Copy <tt>len</tt> property values starting at <tt>srcOffset</tt> from
     * this IndexedDataStore into <tt>dest</tt>, placing the first property's
     * values at <tt>destOffset</tt>. Both <tt>srcOffset</tt> and
     * <tt>destOffset</tt> are in units of property, and not any underlying
     * array.
     * </p>
     * <p>
     * An exception should be thrown if the destination IndexedDataStore is not
     * of the same type, or is not compatible with this IndexedDataStore.
     * </p>
     * 
     * @param srcOffset The offset of the first property to copy
     * @param len The number of properties to copy
     * @param dest The destination IndexedDataStore
     * @param destOffset The offset into dest to place the first property's
     *            values
     * @throws IndexOutOfBoundsException if the offsets or lengths cause
     *             out-of-bounds exceptions
     */
    public void copy(int srcOffset, int len, IndexedDataStore dest, int destOffset);

    /**
     * <p>
     * Rearrange the property values in this IndexedDataStore to reflect the
     * re-ordering of the Components in <tt>newToOldMap</tt>. The Components in
     * <tt>newToOldMap</tt> in the range of <tt>from</tt> (inclusive) to
     * <tt>to</tt> (exclusive), are the now valid components and will not be
     * null.
     * </p>
     * <p>
     * Their 'new' index is the index in the array holding the component, and
     * the 'old' index is the actual index reported by the component. Property
     * values must be moved around internally so that values for a component at
     * its old index will be stored at the new index instead.
     * </p>
     * 
     * @param newToOldMap An array map from the new index of each component to
     *            their old index
     * @param from The valid starting index into newToOldMap
     * @param to The ending index of newToOldMap, exclusive
     */
    public void update(Component[] newToOldMap, int from, int to);
}
