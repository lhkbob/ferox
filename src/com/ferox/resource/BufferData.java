package com.ferox.resource;

/**
 * <p>
 * BufferData represents an abstract block of data. Internally it relies on
 * primitive arrays of floats, ints, shorts, or bytes to store the actual data.
 * A null array is allowed, but it must have a special interpretation by the
 * Renderer that needs to use BufferData's.
 * </p>
 * <p>
 * BufferData itself is not a Resource, but instead many resources will rely on
 * BufferData to perform type and size checking for it. To make things simpler,
 * a BufferData's type and capacity cannot be changed after it is constructed.
 * Only the contents of the buffer data may be modified or reassigned.
 * </p>
 * <p>
 * If a BufferData is constructed with a null array, and an associated resource
 * is updated before a non-null array is assigned, then the Renderer should
 * allocate space without changing any of the values (which could be old
 * garbage).
 * </p>
 * <p>
 * If a BufferData is later set to have a null array, and an associated resource
 * is updated, Renderers should not modify already allocated space. Essentially,
 * a null data array implies Renderers must make sure the space is there, but
 * otherwise not change the contents.
 * </p>
 * <p>
 * Because of this, care should be given when using null arrays and BufferDatas.
 * One reasonably safe scenario is to update a resource using a BufferData, and
 * then on a success, nullify the BufferData's array to clear up client memory.
 * It is necessary to reload the array before any more updates.
 * </p>
 * 
 * @author Michael Ludwig
 */
public class BufferData {
	public static final int BYTESIZE_BYTE = 1;
	public static final int BYTESIZE_SHORT = 2;
	public static final int BYTESIZE_INT = 4;
	public static final int BYTESIZE_FLOAT = 4;

	/**
	 * <p>
	 * DataType represents one of the data types common to game-related graphics
	 * applications. It does not allow for double, long or char types because
	 * they are not likely to be used in games to represent large blocks of
	 * data.
	 * </p>
	 * <p>
	 * DataType allows for the distinction between signed and unsigned types. If
	 * a type is unsigned, the primitives are not treated as two's complement
	 * integers.
	 * </p>
	 */
	public static enum DataType {
		/** Use float[] for the data. */
		FLOAT(float[].class, BYTESIZE_FLOAT, false),
		/** Use signed int[] for the data. */
		INT(int[].class, BYTESIZE_INT, false),
		/** Use signed short[] for the data. */
		SHORT(short[].class, BYTESIZE_SHORT, false),
		/** Use signed byte[] for the data. */
		BYTE(byte[].class, BYTESIZE_BYTE, false),
		/** Used int[] for the data, bit pattern treated as unsigned. */
		UNSIGNED_INT(int[].class, BYTESIZE_INT, true),
		/** Use short[] for the data, bit pattern treated as unsigned. */
		UNSIGNED_SHORT(short[].class, BYTESIZE_SHORT, true),
		/** Use byte[] for the data, bit pattern treated as unsigned. */
		UNSIGNED_BYTE(byte[].class, BYTESIZE_BYTE, true);

		private Class<?> classType;
		private int byteSize;
		private boolean unsigned;

		private DataType(Class<?> type, int byteSize, boolean unsigned) {
			classType = type;
			this.byteSize = byteSize;
			this.unsigned = unsigned;
		}

		/**
		 * Return true if the object is a primitive array of the corresponding
		 * type to this DataType.
		 * 
		 * @param instance Object to test validity on
		 * @return Whether or not instance matches this DataType
		 */
		public boolean isValid(Object instance) {
			return (instance == null || classType.isInstance(instance));
		}

		/**
		 * Get the number of bytes in each of this type's primitives.
		 * 
		 * @return The number of bytes in each primitive element
		 */
		public int getByteSize() {
			return byteSize;
		}

		/**
		 * Return true if the primitives should be treated as unsigned.
		 * 
		 * @return Whether or not primitives are signed or unsigned
		 */
		public boolean isUnsigned() {
			return unsigned;
		}

		/**
		 * <p>
		 * Determine the DataType based on the given object and whether or not
		 * is unsigned. Returns null if array is null or if the object is not a
		 * valid primitive array.
		 * </p>
		 * <p>
		 * unsigned is ignored for float[] arrays. For int[], short[], and
		 * byte[] arrays, unsigned distinguishes between returning X or
		 * UNSIGNED_X.
		 * </p>
		 * 
		 * @param array Object to detect its DataType
		 * @param unsigned Whether or not to treat array as unsigned, if
		 *            applicable
		 * @return The matching data type, or null if array doesn't match
		 */
		public static DataType getDataType(Object array, boolean unsigned) {
			if (array == null)
				return null;

			if (array instanceof float[])
				return DataType.FLOAT;
			else if (unsigned) {
				if (array instanceof int[])
					return DataType.UNSIGNED_INT;
				else if (array instanceof short[])
					return DataType.UNSIGNED_SHORT;
				else if (array instanceof byte[])
					return DataType.UNSIGNED_BYTE;
			} else if (array instanceof int[])
				return DataType.INT;
			else if (array instanceof short[])
				return DataType.SHORT;
			else if (array instanceof byte[])
				return DataType.BYTE;

			return null;
		}
	}

	private final int capacity;
	private final DataType type;
	private Object data;

	/**
	 * <p>
	 * Create a BufferData object wrapping the given primitive array. data must
	 * not be null and be a valid primitive array (byte[], short[], int[], or
	 * float[]).
	 * </p>
	 * <p>
	 * This constructor identifies the capacity as the length of the array, and
	 * determines the DataType as per getDataType(data, unsigned).
	 * </p>
	 * 
	 * @param data The primitive array used by this BufferData
	 * @param unsigned Whether or not this data is to be unsigned
	 * @throws NullPointerException if data is null
	 * @throws IllegalArgumentException if data doesn't match a DataType
	 */
	public BufferData(Object data, boolean unsigned) {
		if (data == null)
			throw new NullPointerException(
					"Constructor expects a non-null primitive array");

		type = DataType.getDataType(data, unsigned);
		if (type == null)
			throw new IllegalArgumentException(
					"Data must be a valid data type, according to DataType: "
							+ data);

		this.data = data;
		capacity = capacity(data);
	}

	/**
	 * Create a BufferData object with a null data array. Initialize it with the
	 * given capacity and type.
	 * 
	 * @param capacity Number of primitives that this BufferData will hold
	 * @param type The DataType to use for this BufferData
	 * @throws NullPointerException if type is null
	 * @throws IllegalArgumentException if capacity < 0
	 */
	public BufferData(int capacity, DataType type) {
		if (type == null)
			throw new NullPointerException("Must specify a non-null DataType");
		if (capacity < 0)
			throw new IllegalArgumentException(
					"Must specifiy a capacity >= 0, not: " + capacity);

		this.capacity = capacity;
		this.type = type;
		data = null;
	}

	/**
	 * Return the data type that represents the array type for this BufferData.
	 * If the returned type is INT or UNSIGNED_INT, getData() is an int[]. If
	 * it's SHORT or UNSIGNED_SHORT, it's a short[]. If it's BYTE or
	 * UNSIGNED_BYTE, it's a byte[]. If it's FLAOT, then the data is a float[].
	 * 
	 * @return The DataType that matches this BufferData's data array
	 */
	public DataType getType() {
		return type;
	}

	/**
	 * Return the capacity of this buffer data. If getData() is not null, it
	 * will have a length of getCapacity(). If it is null, the Renderer must
	 * allocate space of the given capacity to be filled later.
	 * 
	 * @return The length of the data array, if it's not null
	 */
	public int getCapacity() {
		return capacity;
	}

	/**
	 * Return the primitive array that is holding the data of this BufferData.
	 * This return value may be null. If it is not null, it is guaranteed to be
	 * a short[], int[], float[] or byte[].
	 * 
	 * @return The data array currently stored, if not null, it's length is
	 *         getCapacity()
	 */
	public Object getData() {
		return data;
	}

	/**
	 * Set the primitive array object to be used by this BufferData. If obj is
	 * not null, the object must be a primitive array matching this BufferData's
	 * type. It must also have a length that equals the capacity of the buffer
	 * data. If obj is null, the original array reference is cleared but the
	 * type and capacity remain unchanged.
	 * 
	 * @param obj The new data array to use for this BufferData, may be null
	 * @throws IllegalArgumentException if obj doesn't have a matching type or
	 *             size (only matters when obj != null)
	 */
	public void setData(Object obj) throws IllegalArgumentException {
		if (obj != null) {
			DataType t = DataType.getDataType(obj, type.isUnsigned());
			if (t != type)
				throw new IllegalArgumentException(
						"Data object does not match BufferData's type.  Expected: "
								+ type + " but was: " + t);
			int size = capacity(obj);
			if (size != capacity)
				throw new IllegalArgumentException(
						"Data object must have a length matching this BufferData's capacity. Expected: "
								+ capacity + " but was: " + size);
		}
		// obj is valid, so we can assign it now
		data = obj;
	}

	private static int capacity(Object array) {
		if (array instanceof float[])
			return ((float[]) array).length;
		else if (array instanceof int[])
			return ((int[]) array).length;
		else if (array instanceof short[])
			return ((short[]) array).length;
		else if (array instanceof byte[])
			return ((byte[]) array).length;

		return 0;
	}
}
