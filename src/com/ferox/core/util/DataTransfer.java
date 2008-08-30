package com.ferox.core.util;

import java.nio.Buffer;

import com.ferox.core.states.atoms.BufferData;
import com.ferox.core.states.atoms.Texture2D;
import com.ferox.core.states.atoms.Texture3D;
import com.ferox.core.states.atoms.TextureCubeMap;

public class DataTransfer {
	public static class Slice {
		private int offset;
		private int length;
		
		public Slice(BufferData data) {
			this(0, data.getCapacity());
		}
		
		public Slice(Buffer data) {
			this(0, data.capacity());
		}
		
		public Slice(int offset, int length) {
			this.offset = offset;
			this.length = length;
		}
		
		public int getOffset() {
			return this.offset;
		}
		
		public int getLength() {
			return this.length;
		}
	}
	
	public static class Block {
		private int x, y, z;
		private int width, height, depth;
		
		public Block(Texture2D data) {
			this(0, 0, 0, data.getWidth(), data.getHeight(), 1);
		}
		
		public Block(TextureCubeMap map) {
			this(0, 0, 0, map.getSideLength(), map.getSideLength(), 1);
		}
		
		public Block(Texture3D data) {
			this(0, 0, 0, data.getWidth(), data.getHeight(), data.getDepth());
		}
		
		public Block(int x, int y, int width, int height) {
			this(x, y, 0, width, height, 1);
		}
		
		public Block(int x, int y, int z, int width, int height, int depth) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.width = width;
			this.height = height;
			this.depth = depth;
		}
		
		public int getXOffset() {
			return this.x;
		}
		
		public int getYOffset() {
			return this.y;
		}
		
		public int getZOffset() {
			return this.z;
		}
		
		public int getWidth() {
			return this.width;
		}
		
		public int getHeight() {
			return this.height;
		}
		
		public int getDepth() {
			return this.depth;
		}
	}
}
