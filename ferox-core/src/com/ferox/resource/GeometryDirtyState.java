package com.ferox.resource;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class GeometryDirtyState implements DirtyState<GeometryDirtyState> {
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
	
	public GeometryDirtyState() {
		newAttributes = new HashSet<String>();
		delAttributes = new HashSet<String>();
		
		dirtyAttributes = new HashMap<String, BufferRange>();
		dirtyIndices = null;
	}
	
	public GeometryDirtyState(Set<String> newAttr, Set<String> delAttr, 
							  Map<String, BufferRange> dirtyAttr, BufferRange dirtyIndices) {
		newAttributes = newAttr;
		delAttributes = delAttr;
		dirtyAttributes = dirtyAttr;
		
		this.dirtyIndices = dirtyIndices;
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
	
	public GeometryDirtyState updateIndices(int offset, int length) {
		BufferRange di = merge(offset, length, dirtyIndices);
		return new GeometryDirtyState(newAttributes, delAttributes, dirtyAttributes, di);
	}
	
	public GeometryDirtyState updateAttribute(String name, int offset, int length, boolean newA) {
		if (name == null)
			throw new NullPointerException("Cannot update a null attribute name");
		BufferRange da = merge(offset, length, dirtyAttributes.get(name));
		
		Map<String, BufferRange> attrs = new HashMap<String, BufferRange>(dirtyAttributes);
		attrs.put(name, da);
		
		Set<String> newAttrs = newAttributes;
		Set<String> delAttrs = delAttributes;
		if (newA) {
			if (delAttrs.contains(name)) {
				delAttrs = new HashSet<String>(delAttrs);
				delAttrs.remove(name);
			}
			
			newAttrs = new HashSet<String>(newAttrs);
			newAttrs.add(name);
		}
		
		return new GeometryDirtyState(newAttrs, delAttrs, attrs, dirtyIndices);
	}
	
	public GeometryDirtyState removeAttribute(String name) {
		if (name == null)
			throw new NullPointerException("Cannot remove a null attribute name");
		
		Map<String, BufferRange> dirtyAttrs = dirtyAttributes;
		Set<String> newAttrs = newAttributes;
		
		Set<String> delAttrs = new HashSet<String>(delAttributes);
		delAttrs.add(name);
		
		if (newAttrs.contains(name)) {
			newAttrs = new HashSet<String>(newAttrs);
			newAttrs.remove(name);
		}
		
		if (dirtyAttrs.containsKey(name)) {
			dirtyAttrs = new HashMap<String, BufferRange>(dirtyAttrs);
			dirtyAttrs.remove(name);
		}
		
		return new GeometryDirtyState(newAttrs, delAttrs, dirtyAttrs, dirtyIndices);
	}
	
	@Override
	public GeometryDirtyState merge(GeometryDirtyState geom) {
		if (geom == null)
			return this;
		
		Set<String> newAttrs = new HashSet<String>(newAttributes);
		newAttrs.addAll(geom.newAttributes);
		
		Set<String> delAttrs = new HashSet<String>(delAttributes);
		delAttrs.addAll(geom.delAttributes);
		
		Map<String, BufferRange> dirtyAttrs = new HashMap<String, BufferRange>(dirtyAttributes);
		for (String name: geom.dirtyAttributes.keySet()) {
			BufferRange b1 = dirtyAttrs.get(name);
			BufferRange b2 = geom.dirtyAttributes.get(name);
			
			dirtyAttrs.put(name, merge(b2.offset, b2.len, b1));
		}
		
		BufferRange di = (dirtyIndices == null ? geom.dirtyIndices 
											   : merge(dirtyIndices.offset, dirtyIndices.len, geom.dirtyIndices));
		return new GeometryDirtyState(newAttrs, delAttrs, dirtyAttrs, di);
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