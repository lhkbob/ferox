package com.ferox.util;

import java.util.Arrays;

/**
 * Provides a useful implementation of the Bag data structure. Much like an
 * ArrayList, except that it features constant time removal of indexes. This is
 * because the Bag is allowed to modify elements' relative orderings, unlike in
 * the List structure. Also, this implementation of the Bag uses '==' to
 * determine equality instead of an object's equals() method.
 * 
 * @author Michael Ludwig
 * @param <T> The element type stored in this bag
 */
public class Bag<T> {
	public static final int DEFAULT_CAPACITY = 8;
	public static final float DEFAULT_FACTOR = 2f;
	public static final int DEFAULT_GROWTH = 0;
	
	private Object[] elements;
	private int size;
	private int maxFastClearSize;
	
	private float factor;
	private int growth;
	
	/**
	 * Create a new Bag with the default capacity, factor and growth.
	 */
	public Bag() {
		this(DEFAULT_CAPACITY);
	}
	
	/**
	 * Create a new Bag with the given initial capacity, but with default factor
	 * and growth.
	 * 
	 * @param capacity The initial capacity
	 * @throws IllegalArgumentException if capacity < 0
	 */
	public Bag(int capacity) {
		this(capacity, DEFAULT_FACTOR, DEFAULT_GROWTH);
	}
	
	/**
	 * Create a new Bag with the given initial capacity, factor and growth.
	 * Factor specifies the constant multiplier used when expanding the
	 * underlying array. Growth specifies the constant added to the capacity
	 * when expanding the array.
	 * 
	 * @param capacity Initial capacity
	 * @param factor Capacity multiplier
	 * @param growth Constant capacity growth
	 * @throws IllegalArgumentException if capacity < 0, if factor < 1, if
	 *             growth < 0
	 */
	public Bag(int capacity, float factor, int growth) {
		if (capacity < 0)
			throw new IllegalArgumentException("Cannot have negative capacity");
		if (factor < 1f)
			throw new IllegalArgumentException("Cannot have a factor < 1");
		if (growth < 0)
			throw new IllegalArgumentException("Cannot have a growth < 0");
		
		elements = new Object[capacity];
		size = 0;
		maxFastClearSize = 0;
		
		this.factor = factor;
		this.growth = growth;
	}

	/**
	 * Provide access to the underlying array used by this Bag. This must be
	 * considered read-only. Any modifications could break the contract of the
	 * Bag. Also, only the values in indices 0 to (size() - 1) are valid.
	 * 
	 * @return Underlying array of this Bag
	 */
	@SuppressWarnings("unchecked")
	public T[] elements() {
		return (T[]) elements;
	}
	
	/**
	 * Add an item to the Bag, possibly growing its capacity.
	 * 
	 * @param item The item to add
	 */
	public void add(T item) {
		set(size, item);
	}
	
	/**
	 * Remove the item at the given index and return it. To be efficient, the
	 * last item replaces the value at index thus avoiding an expensive shift.
	 * 
	 * @param index The index to remove
	 * @return The value formerly at index
	 * @throws IndexOutOfBoundsException if index is < 0 or >= size()
	 */
	@SuppressWarnings("unchecked")
	public T remove(int index) {
		if (index < 0 || index >= size)
			throw new IndexOutOfBoundsException("Index must be in [0, " + (size - 1) + "]");
		
		Object e = elements[index];
		elements[index] = elements[size];
		elements[size--] = null;
		
		return (T) e;
	}
	
	/**
	 * Remove the first occurrence of item from this Bag, starting at index 0.
	 * If the item is not found, then false is returned, signaling that the
	 * remove failed. If true is found, it may be possible for more references
	 * to that item to be contained later in the Bag.
	 * 
	 * @param item The item to be removed
	 * @return True if a reference was found and removed
	 */
	public boolean remove(T item) {
		int index = indexOf(item);
		return (index >= 0 ? remove(index) != null : false);
	}
	
	/**
	 * Return the value at the given index, which may be null if null values
	 * were added to the Bag.
	 * 
	 * @param index The index whose value is requested
	 * @return The T at index
	 * @throws IndexOutOfBoundsException if index < 0 or >= size()
	 */
	@SuppressWarnings("unchecked")
	public T get(int index) {
		if (index < 0 || index >= size)
			throw new IndexOutOfBoundsException("Index must be in [0, " + (size - 1) + "]");
		return (T) elements[index];
	}
	
	/**
	 * <p>
	 * Set the value to be stored at the given index. If index is equal to the
	 * current size(), then the size of the Bag is increased and the value is
	 * added to the bag. This method will return the value formerly at index. A
	 * null return value implies that the previous value was null, or that the
	 * Bag increased in size.
	 * </p>
	 * <p>
	 * Note that the assigned index is not permanent. The Bag will re-order
	 * items as others are removed, so the index may later point to another
	 * item.
	 * </p>
	 * 
	 * @param index The index whose value will be assigned
	 * @param item The item to be stored at index
	 * @return The value formerly at index
	 * @throws IndexOutOfBoundsException if index < 0 or index > size()
	 */
	@SuppressWarnings("unchecked")
	public T set(int index, T item) {
		if (index < 0 || index > size)
			throw new IndexOutOfBoundsException("Index must be in [0, " + size + "]");
		
		if (item != null) {
			if (index == size) {
				// potentially grow the array
				if (size == elements.length)
					capacity((int) (size * factor + growth));
				elements[size++] = item;
				return null;
			} else {
				Object e = elements[index];
				elements[index] = item;
				return (T) e;
			}
		}
		
		return null;
	}
	
	/**
	 * Determine the first index that holds a reference to item. This uses '=='
	 * to determine equality, and not the equals() method. Later indices that
	 * might reference item are only detectable after item has been removed, or
	 * if the Bag is re-ordered.
	 * 
	 * @param item The item to search for
	 * @return The current index of the 1st reference to item, or -1 if it
	 *         wasn't found
	 */
	public int indexOf(T item) {
		if (item != null) {
			for (int i = 0; i < size; i++) {
				if (elements[i] == item)
					return i;
			}
		}
		return -1;
	}
	
	/**
	 * Return true if item is contained in this Bag.
	 * 
	 * @param item The item to check for
	 * @return True if indexOf(item) >= 0
	 */
	public boolean contains(T item) {
		return indexOf(item) >= 0;
	}
	
	public void clear(boolean fast) {
		if (fast) {
			maxFastClearSize = Math.max(size, maxFastClearSize);
			size = 0;
		} else {
			Arrays.fill(elements, 0, Math.max(maxFastClearSize, size), null);
			maxFastClearSize = 0;
			size = 0;
		}
	}
	
	/**
	 * Reduce the Bag's capacity to fit its current size exactly.
	 */
	public void trim() {
		capacity(size);
	}
	
	/**
	 * @return The current size of the Bag
	 */
	public int size() {
		return size;
	}
	
	/**
	 * @return The capacity of the Bag, or the max size before a new backing
	 *         array must be created
	 */
	public int capacity() {
		return elements.length;
	}
	
	/**
	 * Adjust the capacity of the Bag. This can be used to grow or reduce the
	 * capacity as desired. If the new capacity is less than the current size,
	 * then some items will be removed from the Bag.
	 * 
	 * @param capacity The new capacity
	 * @throws IllegalArgumentException if capacity < 0
	 */
	public void capacity(int capacity) {
		if (capacity < 0)
			throw new IllegalArgumentException("Cannot have a negative capacity");
		
		Object[] newElements = new Object[capacity];
		System.arraycopy(elements, 0, newElements, 0, Math.min(capacity, elements.length));
		elements = newElements;
		
		size = Math.min(capacity, size);
		maxFastClearSize = Math.min(capacity, maxFastClearSize);
	}
}
