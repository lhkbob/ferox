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
	
    @SuppressWarnings("unchecked")
    private static final HashFunction NATURAL_HASHER = new HashFunction() {
        @Override
        public int hashCode(Object o) {
            return o.hashCode();
        }
    };
	
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
	public E remove(int index) {
		if (index < 0 || index >= size)
			throw new IndexOutOfBoundsException("Index must be in [0, " + (size - 1) + "]");
		
		E e = elements[index];
		elements[index] = elements[--size];
		elements[size] = null;
		
		return e;
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
			E e = elements[index];
			elements[index] = item;
			return e;
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

    /**
     * <p>
     * Sort the Bag using the given HashFunction. The sort is performed by
     * computing the hash's of each value within the Bag and ordering the Bag's
     * elements such that their hash's are in ascending order. The end result is
     * equivalent to:
     * 
     * <pre>
     * sort(new Comparator&lt;E&gt;() {
     *     public int compare(E e1, E e2) {
     *         return hasher.hashCode(e1) - hasher.hashCode(e2);
     *     }
     * });
     * </pre>
     * 
     * </p>
     * <p>
     * However, this sort is often much faster than the above code (orders of
     * magnitude depending on the complexity of the Comparator). This is best
     * used when there's not a precise definition of order. If <tt>hasher</tt>,
     * the "natural" hashing function is used (e.g. each element's
     * {@link Object#hashCode() hashCode()}).
     * </p>
     * 
     * @param hasher The HashFunction that determines the integer hash codes
     *            which impose an ordering on the elements within this Bag
     */
	@SuppressWarnings("unchecked")
    public void sort(HashFunction<E> hasher) {
	    if (hasher == null)
	        hasher = NATURAL_HASHER;
	    
	    int[] keys = new int[size];
	    for (int i = 0; i < size; i++)
	        keys[i] = hasher.hashCode(elements[i]);
	    quickSort(keys, 0, size);
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		if (c == null)
			throw new NullPointerException("Collection can't be null");
		
		if (c instanceof Bag) {
			// do a fast array copy
			Bag<? extends E> bag = (Bag<? extends E>) c;
			int totalSize = size + bag.size;
			
			// grow current array
			if (totalSize > elements.length)
				capacity(totalSize);
			
			// copy over bag's elements
			System.arraycopy(bag.elements, 0, elements, size, bag.size);
			size = totalSize;
		} else {
			// default implementation
			for (E t: c)
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
	
	@Override
	public String toString() {
	    StringBuilder builder = new StringBuilder();
	    builder.append('[');
	    for (int i = 0; i < size; i++) {
	        if (i != 0) {
	            builder.append(','); 
	            builder.append(' ');
	        }
	        builder.append(elements[i].toString());
	    }
	    builder.append(']');
	    
	    return builder.toString();
	}
	
	// use quick sort to sort the elements of the Bag, based off of paired keys stored in x
	private void quickSort(int[] x, int off, int len) {
	    // insertion sort on smallest arrays
	    if (len < 7) {
	        for (int i = off; i < len + off; i++) {
	            for (int j = i; j > off && x[j - 1] > x[j]; j--)
	                swap(x, j, j - 1);
	        }
	        return;
	    }

	    // choose a partition element, v
	    int m = off + (len >> 1);       // small arrays, middle element
	    if (len > 7) {
	        int l = off;
	        int n = off + len - 1;
	        if (len > 40) {        // big arrays, pseudomedian of 9
	            int s = len/8;
	            l = med3(x, l, l + s, l + 2 * s);
	            m = med3(x, m - s, m, m + s);
	            n = med3(x, n - 2 * s, n - s, n);
	        }
	        m = med3(x, l, m, n); // mid-size, med of 3
	    }
	    int v = x[m];

	    int a = off, b = a, c = off + len - 1, d = c;
	    while(true) {
	        while (b <= c && x[b] <= v) {
	            if (v == x[b])
	                swap(x, a++, b);
	            b++;
	        }
	        while (c >= b && x[c] >= v) {
	            if (v == x[c])
	                swap(x, c, d--);
	            c--;
	        }
	        if (b > c)
	            break;
	        swap(x, b++, c--);
	    }

	    // swap partition elements back to middle
	    int s, n = off + len;
	    s = Math.min(a - off, b - a); 
	    vecswap(x, off, b - s, s);
	    s = Math.min(d - c, n - d - 1); 
	    vecswap(x, b, n - s, s);

	    // recursively sort non-partition-elements
	    if ((s = b - a) > 1)
	        quickSort(x, off, s);
	    if ((s = d - c) > 1)
	        quickSort(x, n - s, s);
	}

	// swaps the elements at indices a and b, along with the hashes in x
	private void swap(int[] x, int a, int b) {
	    E t = elements[a];
	    int k = x[a];
	    
	    elements[a] = elements[b];
	    x[a] = x[b];
	    
	    elements[b] = t;
	    x[b] = k;
	}

	// swaps n elements starting at a and b, such that (a,b), (a+1, b+1), etc. are swapped
	private void vecswap(int[] x, int a, int b, int n) {
	    for (int i=0; i<n; i++, a++, b++)
	        swap(x, a, b);
	}

	// returns the index of the median of the three indexed elements
	private static int med3(int x[], int a, int b, int c) {
	    return (x[a] < x[b] ? (x[b] < x[c] ? b : x[a] < x[c] ? c : a) 
	                        : (x[b] > x[c] ? b : x[a] > x[c] ? c : a));
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
			
			if (index == 0 || elements[index - 1] == element) {
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
			if (index == 0)
				throw new IllegalStateException("Must call next() before first calling remove()");
			if (index >= size || element != elements[index] || removed)
				throw new IllegalStateException("Element already removed");
			
			Bag.this.remove(index);
			removed = true;
		}
	}
}
