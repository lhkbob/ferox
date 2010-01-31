package com.ferox.util;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Provides a useful implementation of the Bag data structure. Much like an
 * ArrayList, except that it features constant time removal of indexes. This is
 * because the Bag is allowed to modify elements' relative orderings, unlike in
 * the List structure. Also, this implementation of the Bag uses '==' to
 * determine equality instead of an object's equals() method.
 * 
 * @author Michael Ludwig
 * @param <E> The element type stored in this bag
 */
public class Bag<E> implements Collection<E>, Iterable<E> {
	public static final int DEFAULT_CAPACITY = 8;
	public static final float DEFAULT_FACTOR = 2f;
	public static final int DEFAULT_GROWTH = 0;
	
	private E[] elements;
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
	@SuppressWarnings("unchecked")
	public Bag(int capacity, float factor, int growth) {
		if (capacity < 0)
			throw new IllegalArgumentException("Cannot have negative capacity");
		if (factor < 1f)
			throw new IllegalArgumentException("Cannot have a factor < 1");
		if (growth < 0)
			throw new IllegalArgumentException("Cannot have a growth < 0");
		
		elements = (E[]) new Object[capacity];
		size = 0;
		maxFastClearSize = 0;
		
		this.factor = factor;
		this.growth = growth;
	}

	/**
	 * Return the Object array that is currently being used by the Bag to hold
	 * onto its elements. This should be considered read-only, and only the
	 * elements at indices 0 to {@link #size()} - 1 are of any meaning.
	 * 
	 * @return The read-only Object[] holding all the elements
	 */
	public Object[] elements() {
		return elements;
	}
	
	/**
	 * Add an item to the Bag, possibly growing its capacity.
	 * 
	 * @param item The item to add
	 * @throws NullPointerException if item is null
	 */
	@Override
	public boolean add(E item) {
		set(size, item);
		return true;
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
	public E remove(int index) {
		if (index < 0 || index >= size)
			throw new IndexOutOfBoundsException("Index must be in [0, " + (size - 1) + "]");
		
		Object e = elements[index];
		elements[index] = elements[--size];
		elements[size] = null;
		
		return (E) e;
	}
	
	/**
	 * Remove the first occurrence of item from this Bag, starting at index 0.
	 * If the item is not found, then false is returned, signaling that the
	 * remove failed. If true is found, it may be possible for more references
	 * to that item to be contained later in the Bag.
	 * 
	 * @param item The item to be removed
	 * @return True if a reference was found and removed
	 * @throws NullPointerException if item is null
	 */
	@Override
	public boolean remove(Object item) {
		if (item == null)
			throw new NullPointerException("Null elements aren't supported");
		
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
	public E get(int index) {
		if (index < 0 || index >= size)
			throw new IndexOutOfBoundsException("Index must be in [0, " + (size - 1) + "]");
		return elements[index];
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
	 * @throws NullPointerException if item is null
	 */
	@SuppressWarnings("unchecked")
	public E set(int index, E item) {
		if (index < 0 || index > size)
			throw new IndexOutOfBoundsException("Index must be in [0, " + size + "]");
		if (item == null)
			throw new NullPointerException("Item cannot be null");
		
		if (index == size) {
			// potentially grow the array
			if (size == elements.length)
				capacity((int) (size * factor + growth));
			elements[size++] = item;
			return null;
		} else {
			Object e = elements[index];
			elements[index] = item;
			return (E) e;
		}
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
	 * @throws NullPointerException if item is null
	 */
	public int indexOf(Object item) {
		if (item == null)
			throw new NullPointerException("Null elements aren't supported");
		
		for (int i = 0; i < size; i++) {
			if (elements[i] == item)
				return i;
		}
		return -1;
	}

	/**
	 * Return true if item is contained in this Bag.
	 * 
	 * @param item The item to check for
	 * @return True if indexOf(item) >= 0
	 * @throws NullPointerException if item is null
	 */
	@Override
	public boolean contains(Object item) {
		return indexOf(item) >= 0;
	}
	
	@Override
	public void clear() {
		clear(false);
	}

	/**
	 * Clear this Bag of all elements. If <tt>fast</tt> is true, then the size
	 * is reset to 0 without clearing any internal references. They will be
	 * overwritten as elements are re-added into the Bag, but otherwise cannot
	 * be garbage collected. Fast clearing is useful when needing a collection
	 * that can be re-filled repeatedly and quickly.
	 * 
	 * @param fast True if references are not cleared
	 */
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
	@Override
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
	@SuppressWarnings("unchecked")
	public void capacity(int capacity) {
		if (capacity < 0)
			throw new IllegalArgumentException("Cannot have a negative capacity");
		
		E[] newElements = (E[]) new Object[capacity];
		System.arraycopy(elements, 0, newElements, 0, Math.min(capacity, elements.length));
		elements = newElements;
		
		size = Math.min(capacity, size);
		maxFastClearSize = Math.min(capacity, maxFastClearSize);
	}

	/**
	 * Sort the Bag using the given Comparator. If the Comparator is null, the
	 * elements are sorted using their natural ordering.
	 * 
	 * @param comparator The Comparator to use when sorting
	 * @throws ClassCastException if comparator is null and the elements in the
	 *             Bag are not Comparable
	 */
	public void sort(Comparator<E> comparator) {
		Arrays.sort(elements, 0, size, comparator);
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		if (c == null)
			throw new NullPointerException("Collection can't be null");
		
		for (E t: c) {
			add(t);
		}
		return true;
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		if (c == null)
			throw new NullPointerException("Collection can't be null");
		
		for (Object o: c) {
			if (!contains(o))
				return false;
		}
		return true;
	}

	@Override
	public boolean isEmpty() {
		return size == 0;
	}

	@Override
	public Iterator<E> iterator() {
		return new BagIterator();
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		if (c == null)
			throw new NullPointerException("Collection cannot be null");
		
		boolean rm = false;
		for (Object o: c) {
			rm |= remove(o);
		}
		return rm;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		boolean rm = false;
		for (int i = size - 1; i >= 0; i++) {
			if (!c.contains(elements[i])) {
				remove(i);
				rm = true;
			}
		}
		return rm;
	}

	@Override
	public Object[] toArray() {
		return Arrays.copyOf(elements, size);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T[] toArray(T[] a) {
		if (a == null)
			throw new NullPointerException("Array cannot be null");
		
		if (a.length < size)
			a = (T[]) Array.newInstance(a.getClass().getComponentType(), size);
		System.arraycopy(elements, 0, a, 0, size);
		if (a.length > size)
			a[size] = null;
		return a;
	}
	
	private class BagIterator implements Iterator<E> {
		private int index;
		private E element;
		private boolean removed;
		
		public BagIterator() {
			index = 0;
			element = null;
			removed = false;
		}
		
		@Override
		public boolean hasNext() {
			return index < size;
		}

		@Override
		public E next() {
			if (!hasNext())
				throw new NoSuchElementException();
			
			if (index < 0 || elements[index] == element) {
				// no element was removed, so advance the index
				element = elements[index++];
			} else {
				// refresh what the current element is
				element = elements[index];
			}
			
			removed = false;
			return element;
		}

		@Override
		public void remove() {
			if (index < 0)
				throw new IllegalStateException("Must call next() before first calling remove()");
			if (index >= size || element != elements[index] || removed)
				throw new IllegalStateException("Element already removed");
			
			Bag.this.remove(index);
			removed = true;
		}
	}
}
