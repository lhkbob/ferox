package com.ferox.util;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Utility class implementing a fast map-like interface where the access pattern 
 * reuses the same key in continuous blocks.
 * 
 * @author Michael Ludwig
 */
public class FastMap<K, V> {
	private static class FrequentKeyCache {
		private WeakReference<Object> frequentKey;
		private int frequentIndex;
		
		private WeakHashMap<Object, Integer> indexMap;
		private int indexCounter;
		
		public FrequentKeyCache() {
			frequentKey = null;
			frequentIndex = -1;
			
			indexMap = new WeakHashMap<Object, Integer>();
			indexCounter = 0;
		}
		
		private int getId(Object key) {
			if (frequentKey != null && frequentKey.get() == key)
				return frequentIndex;

			synchronized(indexMap) {
				Integer id = indexMap.get(key);
				if (id == null) {
					id = indexCounter++;
					indexMap.put(key, id);
				}

				frequentKey = new WeakReference<Object>(key);
				frequentIndex = id;
				return id;
			}
		}
	}
	
	private static final Map<Class<?>, FrequentKeyCache> keyTypes = new HashMap<Class<?>, FrequentKeyCache>();

	private Object[] values;
	private FrequentKeyCache cache;

	/**
	 * Create a new FastMap that will be used for the given key type.
	 * 
	 * @param keyClass The key type
	 */
	public FastMap(Class<K> keyClass) {
		if (keyClass == null)
			throw new NullPointerException("Must specify non-null key class");
		cache = keyTypes.get(keyClass);
		if (cache == null) {
			cache = new FrequentKeyCache();
			keyTypes.put(keyClass, cache);
		}
		
		values = new Object[0];
	}

	/**
	 * <p>
	 * Add the given key-value pair to this FastMap. If the key previously had a
	 * value in this map, then the new value will replace the old.
	 * </p>
	 * <p>
	 * Does nothing if the key is null.
	 * </p>
	 * 
	 * @param key The key associated with the given value
	 * @param value The value stored for the given key, can be null
	 */
	public void put(K key, V value) {
		if (key == null)
			return;

		int id = cache.getId(key);
		if (id >= values.length)
			values = Arrays.copyOf(values, id + 1);
		values[id] = value;
	}

	/**
	 * <p>
	 * Return the value previously associated with the given key. If the key is
	 * null or if a key-value pair does not exist within this map for the given
	 * key, null is returned.
	 * </p>
	 * 
	 * @param key The key whose value will be fetched
	 * @return Value stored for the key, or null
	 */
	@SuppressWarnings("unchecked")
	public V get(K key) {
		if (key == null)
			return null;

		int id = cache.getId(key);
		if (id >= values.length)
			return null;
		return (V) values[id];
	}
}
