package com.ferox.core.util.io.binary;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

import com.ferox.core.util.IOUtil;
import com.ferox.core.util.io.ChunkExporter;
import com.ferox.core.util.io.Chunkable;
import com.ferox.core.util.io.IOManager;
import com.ferox.core.util.io.OutputChunk;
import com.ferox.core.util.io.Variable;
import com.ferox.core.util.io.Chunk.VariableType;

public class BinaryExporter implements ChunkExporter {
	private HashMap<IOManager, Integer> managerIds;
	private HashMap<Class<? extends Chunkable>, Integer> typeIds;
	private int primaryChunkId;
	private IOManager manager;
	
	public void beginWrite(IOManager manager) {
		this.managerIds = new HashMap<IOManager, Integer>();
		this.typeIds = new HashMap<Class<? extends Chunkable>, Integer>();
		this.manager = manager;
		this.primaryChunkId = 0;
		this.managerIds.put(manager, 0);
	}

	public void endWrite() {
		this.managerIds = null;
		this.typeIds = null;
		this.primaryChunkId = 0;
		this.manager = null;
	}

	public void setPrimaryChunk(OutputChunk out) {
		this.primaryChunkId = out.getID();
	}

	public void writeChunks(Set<OutputChunk> chunks) throws IOException {
		OutputStream out = this.manager.getOutputStream();
		IOUtil.write(out, chunks.size());
		IOUtil.write(out, this.primaryChunkId);
		
		Collection<Variable> vars;
		for (OutputChunk c: chunks) {
			System.out.println("WRITE CHUNK: " + c.getID() + " " + this.typeIds.get(c.getChunkType()) + " " + c.getChunkType().getName());
			IOUtil.write(out, this.typeIds.get(c.getChunkType()));
			IOUtil.write(out, c.getID());
			
			vars = c.getVariables();
			IOUtil.write(out, vars.size());
			for (Variable v: vars) 
				v.write(out, this.manager);
		}
	}

	public void writeHeader(Set<Class<? extends Chunkable>> allTypes, Set<IOManager> referencedManagers) throws IOException {
		OutputStream out = this.manager.getOutputStream();
		
		int managerCounter = 1;
		IOUtil.write(out, referencedManagers.size());
		for (IOManager iom: referencedManagers) {
			this.managerIds.put(iom, managerCounter);
			String relPath = this.manager.computeRelativePath(iom.getCanonicalResourcePath());
			IOUtil.write(out, managerCounter);
			IOUtil.write(out, relPath);
			IOUtil.write(out, iom.getImpl().getBaseClassname());
			System.out.println("REFERENCE MANAGER: " + managerCounter + " " + relPath + " " + iom.getImpl().getBaseClassname());
			managerCounter++;
		}
		
		int typeCounter = 0;
		IOUtil.write(out, allTypes.size());
		for (Class<? extends Chunkable> type: allTypes) {
			this.typeIds.put(type, typeCounter);
			IOUtil.write(out, typeCounter);
			IOUtil.write(out, type.getName());
			System.out.println("CHUNK TYPE: " + typeCounter + " " + type.getName() + " " + type.getName());
			typeCounter++;
		}
	}

	public Variable createVariable(String name, VariableType type) {
		return new BinaryVariable(name, type);
	}

	public String getBaseClassname() {
		return this.getClass().getPackage().getName() + ".Binary";
	}
	
	public int getManagerId(IOManager man) {
		return this.managerIds.get(man);
	}
	
	public int getTypeId(Class<? extends Chunkable> type) {
		return this.typeIds.get(type);
	}
}
