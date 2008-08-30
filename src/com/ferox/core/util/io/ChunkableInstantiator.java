package com.ferox.core.util.io;


public interface ChunkableInstantiator {
	public Class<? extends Chunkable> getChunkableClass();
	public Chunkable newInstance();
}
