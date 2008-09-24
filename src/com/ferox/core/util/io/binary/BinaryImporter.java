package com.ferox.core.util.io.binary;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

import com.ferox.core.util.IOUtil;
import com.ferox.core.util.io.ChunkImporter;
import com.ferox.core.util.io.Chunkable;
import com.ferox.core.util.io.IOManager;
import com.ferox.core.util.io.InputChunk;
import com.ferox.core.util.io.Variable;
import com.ferox.core.util.io.VariableAccessException;
import com.ferox.core.util.io.Chunk.VariableType;

public class BinaryImporter implements ChunkImporter {
	private HashMap<Integer, IOManager> reffedManagers;
	private HashMap<Integer, Class<? extends Chunkable>> types;
	private int primaryChunkId;
	private IOManager manager;
	
	public void beginRead(IOManager manager) {
		this.manager = manager;
		this.reffedManagers = new HashMap<Integer, IOManager>();
		this.types = new HashMap<Integer, Class<? extends Chunkable>>();
		this.primaryChunkId = 0;
		this.reffedManagers.put(0, this.manager);
	}

	public void endRead() {
		this.manager = null;
		this.reffedManagers = null;
		this.types = null;
		this.primaryChunkId = 0;
	}

	public InputChunk getPrimaryChunk() {
		return (InputChunk)this.manager.getChunk(this.manager, this.primaryChunkId);
	}

	public void readChunks() throws IOException {
		InputStream in = this.manager.getInputStream();
		int chunkCount = IOUtil.readInt(in);
		this.primaryChunkId = IOUtil.readInt(in);
		
		int type, id, varCount;
		InputChunk nc;
		ArrayList<Variable> vars;
		for (int i = 0; i < chunkCount; i++) {
			type = IOUtil.readInt(in);
			id = IOUtil.readInt(in);
			try {
				nc = (InputChunk)this.manager.getChunk(this.manager, id);
			} catch (VariableAccessException vae) {
				nc = this.manager.newChunk(id, this.types.get(type));
			}
			
			varCount = IOUtil.readInt(in);
			vars = new ArrayList<Variable>(varCount);
			for (int u = 0; u < varCount; u++)
				vars.add(BinaryVariable.readVariable(in, this.manager));
			nc.setVariables(vars);
		}
	}

	public void readHeader() throws IOException {
		InputStream in = this.manager.getInputStream();
		
		int managerSize = IOUtil.readInt(in);
		if (managerSize > 0) {
			String path, impl;
			int id;
			for (int i = 0; i < managerSize; i++) {
				id = IOUtil.readInt(in);
				path = this.manager.computeCanonicalPath(IOUtil.readString(in));
				impl = IOUtil.readString(in) + "Importer";
				try {
					this.reffedManagers.put(id, IOManager.read(new File(path), (ChunkImporter)Class.forName(impl).newInstance(), !this.manager.getIgnoreCache(), this.manager.isManagerCached()));
				} catch (Exception e) {
					throw new IOException("Error loading referenced file: " + path, e);
				}
			}
		}
		
		int typeSize = IOUtil.readInt(in);
		if (typeSize > 0) {
			String name;
			int id;
			for (int i = 0; i < typeSize; i++) {
				id = IOUtil.readInt(in);
				name = IOUtil.readString(in);
				try {
					this.types.put(id, (Class<? extends Chunkable>)Class.forName(name));
				} catch (Exception e) {
					throw new IOException("Error loading class type: " + name, e);
				}
			}
		}
	}
	
	public String getBaseClassname() {
		return this.getClass().getPackage().getName() + ".Binary";
	}

	public Variable createVariable(String name, VariableType type) {
		return new BinaryVariable(name, type);
	}

	public IOManager getManager(int id) {
		return this.reffedManagers.get(id);
	}
	
	public Class<? extends Chunkable> getType(int id) {
		return this.types.get(id);
	}
}
