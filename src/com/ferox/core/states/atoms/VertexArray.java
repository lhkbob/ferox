package com.ferox.core.states.atoms;

import com.ferox.core.states.StateAtom;
import com.ferox.core.states.StateUnit;
import com.ferox.core.states.manager.Geometry;
import com.ferox.core.util.io.Chunkable;
import com.ferox.core.util.io.ChunkableInstantiator;
import com.ferox.core.util.io.InputChunk;
import com.ferox.core.util.io.OutputChunk;

public class VertexArray extends StateAtom implements ChunkableInstantiator {
	public static final int MAX_SUPPORTED_TEXUNITS = 32;
	
	public static enum VertexArrayTarget {
		VERTEX(false), NORMAL(false), INDEX(false), TEXCOORD(true), ATTRIB(true);
		
		private boolean numeric;
		private VertexArrayTarget(boolean numeric) {
			this.numeric = numeric;
		}
		
		public boolean isNumeric() {
			return this.numeric;
		}
	}
	
	public static class VertexArrayUnit implements StateUnit {
		private static VertexArrayUnit[] texCache;
		private static VertexArrayUnit[] attCache;
		private static VertexArrayUnit vt = new VertexArrayUnit(VertexArrayTarget.VERTEX, 0);
		private static VertexArrayUnit nm = new VertexArrayUnit(VertexArrayTarget.NORMAL, 0);
		private static VertexArrayUnit id = new VertexArrayUnit(VertexArrayTarget.INDEX, 0);
		
		private int unit;
		private VertexArrayTarget target;
		
		public VertexArrayUnit(VertexArrayTarget target, int unit) {
			this.target = target;
			if (!target.isNumeric())
				unit = -1;
			this.unit = Math.max(0, unit);
		}
		
		public VertexArrayTarget getTarget() {
			return this.target;
		}
		
		public int getUnit() {
			return this.unit;
		}
		
		public int ordinal() {
			return this.target.ordinal() + this.unit;
		}
		
		public static VertexArrayUnit get(VertexArrayTarget target, int unit) throws NullPointerException {
			if (target == null)
				throw new NullPointerException();
			switch(target) {
			case VERTEX: return vt;
			case NORMAL: return nm;
			case INDEX: return id;
			case TEXCOORD:
				texCache = updateCache(target, unit, texCache);
				return texCache[unit];
			case ATTRIB:
				attCache = updateCache(target, unit, attCache);
				return attCache[unit];
			default: 
				return null;
			}
		}
		
		private static VertexArrayUnit[] updateCache(VertexArrayTarget target, int unit, VertexArrayUnit[] cache) {
			unit = Math.max(0, unit);
			
			if (cache == null || cache.length <= unit) {
				VertexArrayUnit[] temp = new VertexArrayUnit[unit + 1];
				if (cache != null) 
					System.arraycopy(cache, 0, temp, 0, cache.length);
				cache = temp;
			}
			if (cache[unit] == null)
				cache[unit] = new VertexArrayUnit(target, unit);
			return cache;
		}
	}
	
	
	private BufferData data;
	private int elementSize;
	private int offset;
	private int stride;
	
	private int oldDataCapacity;
	private int numElements;
	private int accessor;
	
	public VertexArray(BufferData data) {
		this(data, 1);
	}
	
	public VertexArray(BufferData data, int elementSize) {
		this(data, elementSize, 0, 0);
	}
	
	public VertexArray(BufferData data, int elementSize, int offset, int stride) throws NullPointerException {
		super();
		if (data == null)
			throw new NullPointerException("BufferData must be non-null");
		this.data = data;
		this.elementSize = Math.max(1, elementSize);
		this.offset = Math.max(0, offset);
		this.stride = Math.max(0, stride);
		this.accessor = this.elementSize + this.stride;
	}
	
	private VertexArray() { }
	
	public BufferData getBufferData() {
		return this.data;
	}
	
	public int getOffset() {
		return this.offset;
	}
	
	public int getStride() {
		return this.stride;
	}
	
	public int getElementSize() {
		return this.elementSize;
	}
	
	public int getNumElements() {
		if (this.oldDataCapacity != this.data.getCapacity()) {
			this.oldDataCapacity = this.data.getCapacity();
			this.numElements = (this.oldDataCapacity - this.offset) / (this.elementSize + this.stride);
		}
		return this.numElements;
	}
	
	public int getIndex(int element) {
		if (this.accessor == 1)
			return this.offset + element;
		switch(this.accessor) {
		case 2:
			return this.offset + (element << 1);
		case 4:
			return this.offset + (element << 2);
		case 8:
			return this.offset + (element << 3);
		case 16:
			return this.offset + (element << 4);
		default:
			return this.offset + element * this.accessor;
		}
	}

	public Class<? extends Chunkable> getChunkableClass() {
		return VertexArray.class;
	}

	public Chunkable newInstance() {
		return new VertexArray();
	}
	
	@Override
	public void readChunk(InputChunk in) {
		super.readChunk(in);
		
		this.data = (BufferData)in.getObject("data");
		int[] temp = in.getIntArray("params");
		this.elementSize = temp[0];
		this.offset = temp[1];
		this.stride = temp[2];
		
		this.accessor = this.elementSize + this.stride;
		this.oldDataCapacity = -1;
	}
	
	@Override
	public void writeChunk(OutputChunk out) {
		super.writeChunk(out);
		
		out.setObject("data", this.data);
		int[] temp = new int[] {this.elementSize, this.offset, this.stride};
		out.setIntArray("params", temp);
	}

	@Override
	public Class<? extends StateAtom> getAtomType() {
		return VertexArray.class;
	}

	@Override
	public boolean isValidUnit(StateUnit unit) {
		if (!(unit instanceof VertexArrayUnit))
			return false;
		VertexArrayUnit v = (VertexArrayUnit)unit;
		switch(v.getTarget()) {
		case TEXCOORD:
			if (Geometry.getMaxTextureCoordinates() >= 0)
				return v.getUnit() < Geometry.getMaxTextureCoordinates();
			return true;
		case ATTRIB:
			if (Geometry.getMaxVertexAttributes() >= 0)
				return v.getUnit() < Geometry.getMaxVertexAttributes();
			return true;
		default:
			return true;
		}
	}
}
