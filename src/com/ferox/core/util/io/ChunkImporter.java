package com.ferox.core.util.io;

import java.io.IOException;

public interface ChunkImporter extends ChunkIOImpl {
	public void beginRead(IOManager manager) throws IOException;
	public InputChunk getPrimaryChunk() throws IOException;
	public void readHeader() throws IOException;
	public void readChunks() throws IOException;
	public void endRead() throws IOException;
}
