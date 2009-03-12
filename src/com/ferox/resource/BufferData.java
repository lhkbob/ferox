package com.ferox.resource;


/** BufferData represents an abstract block of data.  Internally
 * it relies on primitive arrays of floats, ints, shorts, or bytes
 * to store the actual data.  A null array is allowed, but it must have
 * a special interpretation by the Renderer that needs to use BufferData's.
 * 
 * BufferData itself is not a Resource, but instead many resources will
 * rely on BufferData to perform type and size checking for it.  To make 
 * things simpler, a BufferData's type and capacity cannot be changed after
 * it is constructed.  Only the contents of the buffer data may be modified
 * or reassigned.
 * 
 * If a BufferData is constructed with a null array, and an associated resource
 * is updated before a non-null array is assigned, then the Renderer should
 * allocate space without changing any of the values (which could be old garbage).
 * 
 * If a BufferData is later set to have a null array, and an associated resource
 * is updated, Renderers should not modify already allocated space.  Essentially,
 * a null data array implies Renderers must make sure the space is there, but otherwise
 * not change the contents.
 * 
 * Because of this, care should be given when using null arrays and BufferDatas.
 * One reasonably safe scenario is to update a resource using a BufferData, and then
 * on a success, nullify the BufferData's array to clear up client memory.  It is
 * necessary to reload the array before any more updates.
 * 
 * @author Michael Ludwig
 *
 */
public class BufferData {
	public static final int BYTESIZE_BYTE = 1;
	public static final int BYTESIZE_SHORT = 2;
	public static final int BYTESIZE_INT = 4;
	public static final int BYTESIZE_FLOAT = 4;
	
	/** DataType represents one of the data types common to game-related 
	 * graphics applications.  It does not allow for double, long or char types
	 * because they are not likely to be used in games to represent large blocks
	 * of data.
	 * 
	 * DataType allows for the distinction between signed and unsigned types.  If
	 * a type is unsigned, the primitives are not treated as two's complement integers. */
	public static enum DataType {
		FLOAT(float[].class, BYTESIZE_FLOAT, false), 
		INT(int[].class, BYTESIZE_INT, false), 
		SHORT(short[].class, BYTESIZE_SHORT, false), 
		BYTE(byte[].class, BYTESIZE_BYTE, false),
		
		UNSIGNED_INT(int[].class, BYTESIZE_INT, true), 
		UNSIGNED_SHORT(short[].class, BYTESIZE_SHORT, true),
		UNSIGNED_BYTE(byte[].class, BYTESIZE_BYTE, true);
		
		private Class<?> classType; private int byteSize; private boolean unsigned;
		private DataType(Class<?> type, int byteSize, boolean unsigned) {
			this.classType = type; this.byteSize = byteSize; this.unsigned = unsigned;
		}
		
		/** Return true if the object is a primitive array of the 
		 * corresponding type to this DataType. */
		public boolean isValid(Object instance) {
			return (instance == null || this.classType.isInstance(instance));
		}
		
		/** Get the number of bytes in each of this type's primitives. */
		public int getByteSize() {
			return this.byteSize;
		}
		
		/** Return true if the primitives should be treated as unsigned. */
		public boolean isUnsigned() {
			return this.unsigned;
		}
		
		/** Determine the DataType based on the given object and whether
		 * or not is unsigned.  Returns null if array is null or if the
		 * object is not a valid primitive array.
		 * 
		 * unsigned is ignored for float[] arrays.  For int[], short[], and
		 * byte[] arrays, unsigned distinguishes between returning 
		 * X or UNSIGNED_X. */
		public static DataType getDataType(Object array, boolean unsigned) {
			if (array == null)
				return null;
			
			if (array instanceof float[]) {
				return DataType.FLOAT;
			} else if (unsigned) {
				if (array instanceof int[])
					return DataType.UNSIGNED_INT;
				else if (array instanceof short[])
					return DataType.UNSIGNED_SHORT;
				else if (array instanceof byte[])
					return DataType.UNSIGNED_BYTE;
			} else {
				if (array instanceof int[])
					return DataType.INT;
				else if (array instanceof short[])
					return DataType.SHORT;
				else if (array instanceof byte[])
					return DataType.BYTE;
			}
			
			return null;
		}
	}
	
	private int capacity;
	private DataType type;
	private Object data;
	
	/** Create a BufferData object wrapping the given primitive array.
	 * data must not be null and be a valid primitive array (byte[], short[],
	 * int[], or float[]).
	 * 
	 * This constructor identifies the capacity as the length of the array,
	 * and determines the DataType as per getDataType(data, unsigned).
	 * 
	 * Throw an exception if data is null, or if data isn't a valid array. */
	public BufferData(Object data, boolean unsigned) throws NullPointerException, IllegalArgumentException {
		if (data == null)
			throw new NullPointerException("Constructor expects a non-null primitive array");
		
		this.type = DataType.getDataType(data, unsigned);
		if (this.type == null)
			throw new IllegalArgumentException("Data must be a valid data type, according to DataType: " + data);
		
		this.data = data;
		this.capacity = capacity(data);
	}
	
	/** Create a BufferData object with a null data array.  Initialize
	 * it with the given capacity and type.
	 * 
	 * Throw an exception if capacity < 0, or if type is null. */
	public BufferData(int capacity, DataType type) throws NullPointerException, IllegalArgumentException {
		if (type == null)
			throw new NullPointerException("Must specify a non-null DataType");
		if (capacity < 0)
			throw new IllegalArgumentException("Must specifiy a capacity >= 0, not: " + capacity);
		
		this.capacity = capacity;
		this.type = type;
		this.data = null;
	}
	
	/** Return the data type that represents the array type
	 * for this BufferData.  If the returned type is
	 * INT or UNSIGNED_INT, getData() is an int[].
	 * If it's SHORT or UNSIGNED_SHORT, it's a short[].
	 * If it's BYTE or UNSIGNED_BYTE, it's a byte[].
	 * If it's FLAOT, then the data is a float[]. */
	public DataType getType() {
		return this.type;
	}
	
	/** Return the capacity of this buffer data.  If getData()
	 * is not null, it will have a length of getCapacity().  If it
	 * is null, the Renderer must allocate space of the given capacity
	 * to be filled later. */
	public int getCapacity() {
		return this.capacity;
	}
	
	/** Return the primitive array that is holding the data of this
	 * BufferData.  This return value may be null.  If it is not null,
	 * it is guaranteed to be a short[], int[], float[] or byte[]. */
	public Object getData() {
		return this.data;
	}
	
	/** Set the primitive array object to be used by this BufferData.
	 * If obj is not null, the object must be a primitive array matching
	 * this BufferData's type.  It must also have a length that equals the
	 * capacity of the buffer data.  If obj is null, the original array reference
	 * is cleared but the type and capacity remain unchanged.
	 * 
	 * Throws an exception if obj isn't null and doesn't have a matching DataType
	 * or if its array length doesn't match the capacity. */
	public void setData(Object obj) throws IllegalArgumentException {
		if (obj != null) {
			DataType t = DataType.getDataType(obj, this.type.isUnsigned());
			if (t != this.type)
				throw new IllegalArgumentException("Data object does not match BufferData's type.  Expected: " + this.type + " but was: " + t);
			int size = capacity(obj);
			if (size != this.capacity)
				throw new IllegalArgumentException("Data object must have a length matching this BufferData's capacity. Expected: " + this.capacity + " but was: " + size);
		}
		// obj is valid, so we can assign it now
		this.data = obj;
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
