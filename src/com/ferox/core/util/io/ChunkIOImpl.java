package com.ferox.core.util.io;

import com.ferox.core.util.io.Chunk.VariableType;


public interface ChunkIOImpl {
	public Variable createVariable(String name, VariableType type);
	public String getBaseClassname();
}
