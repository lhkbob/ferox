package com.ferox.core.util.io;


public interface Chunkable {
	public void readChunk(InputChunk in);
	public void writeChunk(OutputChunk out);
}
