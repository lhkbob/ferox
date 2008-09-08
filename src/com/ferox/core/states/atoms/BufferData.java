package com.ferox.core.states.atoms;

import java.nio.*;

import com.ferox.core.states.StateAtom;
import com.ferox.core.states.StateUnit;
import com.ferox.core.util.BufferUtil;
import com.ferox.core.util.FeroxException;
import com.ferox.core.util.io.Chunkable;
import com.ferox.core.util.io.ChunkableInstantiator;
import com.ferox.core.util.io.InputChunk;
import com.ferox.core.util.io.OutputChunk;

public class BufferData extends StateAtom implements ChunkableInstantiator {
	public static enum DataType {
		FLOAT(FloatBuffer.class, BufferUtil.BYTESIZE_FLOAT, false), 
		DOUBLE(DoubleBuffer.class, BufferUtil.BYTESIZE_DOUBLE, false), 
		SHORT(ShortBuffer.class, BufferUtil.BYTESIZE_SHORT, false), 
		INT(IntBuffer.class, BufferUtil.BYTESIZE_INT, false), 
		BYTE(ByteBuffer.class, BufferUtil.BYTESIZE_BYTE, false),
		UNSIGNED_INT(IntBuffer.class, BufferUtil.BYTESIZE_INT, true),
		UNSIGNED_BYTE(ByteBuffer.class, BufferUtil.BYTESIZE_BYTE, true), 
		UNSIGNED_SHORT(ShortBuffer.class, BufferUtil.BYTESIZE_SHORT, true);
		
		private Class<? extends Buffer> buffer; private int byteSize; private boolean unsigned;
		private DataType(Class<? extends Buffer> buffer, int byteSize, boolean unsigned) {
			this.buffer = buffer; this.byteSize = byteSize; this.unsigned = unsigned;
		}
		
		public int getByteSize() {
			return this.byteSize;
		}
		public boolean isTypeValid(Buffer b) {
			if (b == null)
				return false;
			return this.buffer.isAssignableFrom(b.getClass());
		}
		public boolean isUnsignedData() {
			return this.unsigned;
		}
	}
	
	public static enum BufferTarget implements StateUnit {
		ARRAY_BUFFER, ELEMENT_BUFFER, PIXEL_READ_BUFFER, PIXEL_WRITE_BUFFER
	}
	
	public static enum UsageHint {
		STATIC, STREAM
	}
	
	private Buffer data;
	private DataType dataType;
	private int capacity;
	private int byteSize;
	private UsageHint hint;
	private BufferTarget primaryTarget;
	private boolean isVBO;
	
	public BufferData(Buffer data, DataType dataType, int capacity) {
		this(data, dataType, capacity, BufferTarget.ARRAY_BUFFER, false);
	}
	
	public BufferData(Buffer data, DataType dataType, int capacity, BufferTarget expectedTarget) {
		this(data, dataType, capacity, expectedTarget, true);
	}
	
	public BufferData(Buffer data, DataType dataType, int capacity, BufferTarget expectedTarget, boolean isVBO) {
		this(data, dataType, capacity, expectedTarget, isVBO, UsageHint.STATIC);
	}
	
	public BufferData(Buffer data, DataType dataType, int capacity, BufferTarget expectedTarget, boolean isVBO, UsageHint usageHint) {
		super();
		this.hint = usageHint;
		this.primaryTarget = expectedTarget;
		this.setBufferData(data, dataType, capacity);
		this.setVBO(isVBO);
	}
	
	private BufferData() { }
	
	public BufferTarget getPrimaryTarget() {
		return this.primaryTarget;
	}
	
	public UsageHint getUsageHint() {
		return this.hint;
	}
	
	public void setBufferData(Buffer data, DataType dataType, int capacity) throws FeroxException {
		Buffer oldData = this.data;
		DataType oldType = this.dataType;
		int oldCapacity = this.capacity;
		int oldByteSize = this.byteSize;
		
		try {
			if (!BufferData.isBufferValid(data, dataType, capacity)) 
				throw new FeroxException("Can't create an invalid buffer");
			this.data = data;
			this.dataType = dataType;
			this.byteSize = dataType.getByteSize();
			this.capacity = capacity;
		} catch (RuntimeException e) {
			this.data = oldData;
			this.dataType = oldType;
			this.capacity = oldCapacity;
			this.byteSize = oldByteSize;
			throw e;
		}
	}
	
	public void clearClientMemory() {
		this.setBufferData(null, this.dataType, this.capacity);
	}
	
	public void setVBO(boolean isVBO) {
		if (isVBO != this.isVBO) {
			this.isVBO = isVBO;
		}
	}
	
	public boolean isVBO() {
		return this.isVBO;
	}
	
	public int getCapacity() {
		return this.capacity;
	}
	
	public Buffer getData() {
		return this.data;
	}
	
	public int getByteSize() {
		return this.byteSize;
	}
	
	public DataType getDataType() {
		return this.dataType;
	}
	
	public boolean isDataInClientMemory() {
		return this.data != null;
	}
	
	public static boolean isBufferValid(Buffer data, DataType dataType, int capacity) {
		if (capacity <= 0)
			return false;
		if (data == null)
			return true;
		if (data.capacity() != capacity)
			return false;
		return dataType.isTypeValid(data);
	}
	
	@Override
	public void readChunk(InputChunk in) {
		super.readChunk(in);
		
		this.data = null;
		this.dataType = in.getEnum("dataType", DataType.class);
		this.data = in.getBuffer("data");
		this.capacity = in.getInt("capacity");
		this.hint = in.getEnum("usageHint", UsageHint.class);
		this.isVBO = in.getBoolean("isVBO");
		this.primaryTarget = in.getEnum("target", BufferTarget.class);
		this.byteSize = this.dataType.getByteSize();
	}
	
	@Override
	public void writeChunk(OutputChunk output) {
		super.writeChunk(output);
		
		output.setEnum("dataType", this.dataType);
		output.setInt("capacity", this.capacity);
		output.setEnum("usageHint", this.hint);
		output.setBoolean("isVBO", this.isVBO);
		output.setBuffer("data", this.data);
		output.setEnum("target", this.primaryTarget);
	}

	public Class<? extends Chunkable> getChunkableClass() {
		return BufferData.class;
	}

	public Chunkable newInstance() {
		return new BufferData();
	}

	@Override
	public Class<BufferData> getAtomType() {
		return BufferData.class;
	}

	@Override
	public boolean isValidUnit(StateUnit unit) {
		return unit instanceof BufferTarget;
	}
}
