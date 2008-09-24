package com.ferox.core.util.io;

import java.io.IOException;
import java.util.Set;


public interface ChunkExporter extends ChunkIOImpl {
	public void beginWrite(IOManager manager) throws IOException;
	public void setPrimaryChunk(OutputChunk out) throws IOException;
	public void writeHeader(Set<Class<? extends Chunkable>> allTypes, Set<IOManager> referencedManagers) throws IOException;
	public void writeChunks(Set<OutputChunk> chunks) throws IOException;
	public void endWrite() throws IOException;
}
