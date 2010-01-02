package com.ferox.util;


@SuppressWarnings("unchecked")
public class IntKeyMap<V> {
	public static final int DEFAULT_CAPACITY = 16;
	public static final float DEFAULT_LOAD_FACTOR = .75f;
	public static final int MAX_CAPACITY = (1 << 30);
	
	private final float loadFactor;
	private int threshold;
	
	private int size;
	private Entry[] table;
	
	public IntKeyMap() {
		this(DEFAULT_CAPACITY);
	}
	
	public IntKeyMap(int initialCapacity) {
		this(initialCapacity, DEFAULT_LOAD_FACTOR);
	}
	
	public IntKeyMap(int initialCapacity, float loadFactor) {
		if (initialCapacity < 0)
            throw new IllegalArgumentException("Illegal initial capacity: " + initialCapacity);
        if (loadFactor <= 0 || Float.isNaN(loadFactor))
            throw new IllegalArgumentException("Illegal load factor: " + loadFactor);

        if (initialCapacity > MAX_CAPACITY)
        	initialCapacity = MAX_CAPACITY;
        
        // Find a power of 2 >= initialCapacity
        int capacity = 1;
        while (capacity < initialCapacity)
            capacity <<= 1;

        this.loadFactor = loadFactor;
        threshold = (int)(capacity * loadFactor);
        table = new Entry[capacity];
	}
	
	public V put(int key, V value) {
		int hash = hash(key);
		int index = indexFor(hash, table.length);
		for (Entry<V> e = table[index]; e != null; e = e.next) {
			if (e.hash == hash && e.key == key) {
				V old = e.value;
				e.value = value;
				return old;
			}
		}
		
		Entry<V> e = table[index];
		table[index] = new Entry<V>(hash, key, value, e);
		if (size++ >= threshold)
			resize(2 * table.length);
		return null;
	}
	
	public V get(int key) {
		Entry<V> e = getEntry(key);
		return (e == null ? null : e.value);
	}
	
	private Entry<V> getEntry(int key) {
		int hash = hash(key);
		for (Entry<V> e = table[indexFor(hash, table.length)]; e != null; e = e.next) {
			if (e.hash == hash && e.key == key)
				return e;
		}
		return null;
	}
	
	public V remove(int key) {
		int hash = hash(key);
        int i = indexFor(hash, table.length);
        Entry<V> prev = table[i];
        Entry<V> e = prev;

        while (e != null) {
            Entry<V> next = e.next;
            if (e.hash == hash && e.key == key) {
                size--;
                if (prev == e)
                    table[i] = next;
                else
                    prev.next = next;
                return e.value;
            }
            prev = e;
            e = next;
        }

        return (e == null ? null : e.value);
	}
	
	private void resize(int newCapacity) {
		Entry[] oldTable = table;
        int oldCapacity = oldTable.length;
        if (oldCapacity == MAX_CAPACITY) {
            threshold = Integer.MAX_VALUE;
            return;
        }

        Entry[] newTable = new Entry[newCapacity];
        transfer(newTable);
        table = newTable;
        threshold = (int)(newCapacity * loadFactor);
	}
	
	private void transfer(Entry[] newTable) {
		Entry[] src = table;
		int newCapacity = newTable.length;
		for (int j = 0; j < src.length; j++) {
			Entry<V> e = src[j];
			if (e != null) {
				src[j] = null;
				do {
					Entry<V> next = e.next;
					int i = indexFor(e.hash, newCapacity);
					e.next = newTable[i];
					newTable[i] = e;
					e = next;
				} while (e != null);
			}
		}
	}
	
	private static int hash(int h) {
        // This function ensures that hashCodes that differ only by
        // constant multiples at each bit position have a bounded
        // number of collisions (approximately 8 at default load factor).
        h ^= (h >>> 20) ^ (h >>> 12);
        return h ^ (h >>> 7) ^ (h >>> 4);
    }

    private static int indexFor(int h, int length) {
        return h & (length-1);
    }
	
	private static class Entry<V> {
		final int key;
		final int hash;
		
		V value;
		Entry<V> next;
		
		public Entry(int h, int k, V v, Entry<V> n) {
			hash = h;
			key = k;
			
			value = v;
			next = n;
		}
	}
	
	// FIXME: the CompressedArray class is not very useful, although it offers faster
	// get time compared to both maps, it's insertions and removals are much too slow
	// This IntKeyMap is faster than the HashMap, for some reason the IdentityHashMap suffers
	// terribly with the Integer keys.  
	// HashMap is as fast with inserts and removals, but it's gets are 2X slower
	// However, it does provide iterators and sets that are very useful 
	//   To make this class completely useful, I'd need to write code to have key sets, entry sets
	//   and iterators (that all support primitive values instead)
	
	// FIXME: I think my resolution is to scrap both these classes and just use HashMaps within
	// the Entity system.  It should be performant enough, and in some cases I may even be able to use
	// id access.
}
