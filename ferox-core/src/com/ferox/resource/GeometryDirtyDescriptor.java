package com.ferox.resource;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GeometryDirtyDescriptor {
	public static class BufferRange {
		private final int offset;
		private final int len;
		
		public BufferRange(int offset, int len) {
			if (len < 1)
				throw new IllegalArgumentException("Length of range must be at least 1: " + len);
			this.offset = Math.max(0, offset);
			this.len = len;
		}
		
		public int getOffset() {
			return offset;
		}
		
		public int getLength() {
			return len;
		}
	}
	
	private final Set<String> newAttributes;
	private final Set<String> delAttributes;
	
	private final Map<String, BufferRange> dirtyAttributes;
	
	private BufferRange dirtyIndices;
	
	public GeometryDirtyDescriptor() {
		newAttributes = new HashSet<String>();
		delAttributes = new HashSet<String>();
		
		dirtyAttributes = new HashMap<String, BufferRange>();
	}
	
	public Set<String> getAddedAttributes() {
		return Collections.unmodifiableSet(newAttributes);
	}
	
	public Set<String> getRemovedAttributes() {
		return Collections.unmodifiableSet(delAttributes);
	}
	
	public Map<String, BufferRange> getModifiedAttributes() {
		return Collections.unmodifiableMap(dirtyAttributes);
	}
	
	public BufferRange getModifiedIndices() {
		return dirtyIndices;
	}
	
	void markIndicesDirty(int offset, int length) {
		dirtyIndices = merge(offset, length, dirtyIndices);
	}
	
	void markAttributeDirty(String name, int offset, int length) {
		BufferRange old = dirtyAttributes.get(name);
		dirtyAttributes.put(name, merge(offset, length, old));
	}
	
	void notifyAttributeAdded(String name) {
		delAttributes.remove(name);
		newAttributes.add(name);
	}
	
	void notifyAttributeRemoved(String name) {
		newAttributes.remove(name);
		dirtyAttributes.remove(name);
		
		delAttributes.add(name);
	}
	
	private BufferRange merge(int offset, int length, BufferRange old) {
		if (old != null) {
			int nO = Math.min(offset, old.offset);
			int nM = Math.max(offset + length, old.offset + old.len);
			return new BufferRange(nO, nM - nO);
		} else
			return new BufferRange(offset, length);
	}
}