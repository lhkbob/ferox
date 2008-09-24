package com.ferox.core.util.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.Map.Entry;

import com.ferox.core.util.io.Chunk.VariableType;


public class IOManager {
	private static HashMap<String, IOManager> allManagers = new HashMap<String, IOManager>();
	
	private ChunkIOImpl impl;
	
	private HashMap<Chunkable, OutputChunk> objToChunk;
	private HashMap<Integer, InputChunk> idToChunk;
	private HashMap<Chunk, Integer> chunkToId;
	
	private int chunkIdCounter;
	private Set<Integer> usedIds;
	private Stack<Chunkable> refStack;
	private boolean forOutput;
	
	private File resource;
	private String canPath;
	private InputStream in;
	private OutputStream out;
	
	private boolean cacheManager;
	private boolean ignoreCache;
	private Chunk primaryChunk;
	
	private IOManager(File resource, boolean forOutput, boolean cache, boolean ignore, ChunkIOImpl impl) {
		this.resource = resource;
		this.chunkIdCounter = 0;
		this.impl = impl;
	
		this.chunkToId = new HashMap<Chunk, Integer>();
		
		if (forOutput) {
			this.objToChunk = new HashMap<Chunkable, OutputChunk>();
			this.refStack = new Stack<Chunkable>();
			this.usedIds = new HashSet<Integer>();
		} else {
			this.idToChunk = new HashMap<Integer, InputChunk>();
		}
		this.forOutput = forOutput;
		
		this.cacheManager = cache;
		this.ignoreCache = ignore;
		
		try {
			this.canPath = this.resource.getCanonicalPath();
		} catch (IOException ioe) {
			this.canPath = this.resource.getAbsolutePath();
		}
	}
	
	public static void clearIOManagerCache() {
		allManagers.clear();
	}
	
	public static IOManager read(File file, ChunkImporter impl) throws IOException, NullPointerException {
		return read(file, impl, false);
	}
	
	public static IOManager read(File file, ChunkImporter impl, boolean useCache) throws IOException, NullPointerException {
		return read(file, impl, useCache, false);
	}
	
	public static IOManager read(File file, ChunkImporter impl, boolean useCache, boolean remember) throws IOException, NullPointerException {
		if (file == null || impl == null)
			throw new NullPointerException("No arguments to read() can be null");
		
		IOManager inM = new IOManager(file, false, remember, !useCache, impl);
		inM.out = null;
		
		try {
			if (useCache && allManagers.containsKey(inM.canPath))
				return allManagers.get(inM.canPath);
			
			inM.in = new FileInputStream(file);
			impl.beginRead(inM);
			impl.readHeader();
			impl.readChunks();
			inM.primaryChunk = impl.getPrimaryChunk();
			impl.endRead();
			inM.primaryChunk.getRepresentedObject();
			
			if (remember)
				allManagers.put(inM.canPath, inM);
			return inM;
		} catch (Exception e) {
			throw new IOException("Exception while reading file: " + file.getPath(), e);
		} finally {
			if (inM.in != null) {
				inM.in.close();
			}
		}
	}
	
	public static IOManager write(File file, Chunkable object, ChunkExporter impl) throws IOException, NullPointerException {
		return write(file, object, impl, false);
	}
	
	public static IOManager write(File file, Chunkable object, ChunkExporter impl, boolean useCache) throws IOException, NullPointerException {
		return write(file, object, impl, false, false);
	}
	
	public static IOManager write(File file, Chunkable object, ChunkExporter impl, boolean useCache, boolean remember) throws IOException, NullPointerException {
		if (file == null || object == null || impl == null)
			throw new NullPointerException("No arguments to write() can be null");
		
		IOManager outM = new IOManager(file, true, remember, !useCache, impl);
		outM.in = null;
		
		try {
			if (useCache && allManagers.containsKey(outM.canPath)) {
				Collection<Integer> usedIds = allManagers.get(outM.canPath).chunkToId.values();
				outM.usedIds.addAll(usedIds);
			}
			
			outM.primaryChunk = outM.newOutputChunk(object);
			
			Set<OutputChunk> pruned = pruneChunks(outM.objToChunk.values());
			Set<Class<? extends Chunkable>> types = pruneTypes(pruned);
			
			Set<IOManager> ref = new HashSet<IOManager>();
			for (OutputChunk c: pruned) {
				ref.addAll(c.getReferencedIOManagers());
			}
			
			outM.out = new FileOutputStream(file);
			impl.beginWrite(outM);
			impl.setPrimaryChunk((OutputChunk)outM.primaryChunk);
			impl.writeHeader(types, ref);
			impl.writeChunks(pruned);
			impl.endWrite();
			
			if (remember)
				allManagers.put(outM.canPath, outM);
			return outM;
		} catch (Exception e) {
			throw new IOException("Exception while writing file: " + file.getPath(), e);
		} finally {
			if (outM.out != null) {
				outM.out.flush();
				outM.out.close();
			}
		}
	}
	
	private static Set<Class<? extends Chunkable>> pruneTypes(Set<OutputChunk> prunedChunks) {
		Set<Class<? extends Chunkable>> types = new HashSet<Class<? extends Chunkable>>();
		for (OutputChunk c: prunedChunks) {
			Class<? extends Chunkable> t = c.getRepresentedObject().getClass();
			if (!types.contains(t))
				types.add(t);
		}
		return types;
	}
	
	private static Set<OutputChunk> pruneChunks(Collection<OutputChunk> allChunks) {
		int zeroCount = Integer.MAX_VALUE;
		Set<OutputChunk> pruned = new HashSet<OutputChunk>();
		pruned.addAll(allChunks);
		
		while (zeroCount > 0) {
			zeroCount = 0;
			for (OutputChunk c: allChunks)
				c.resetReferenceCount();
			for (OutputChunk c: allChunks)
				c.updateReferencedChunks();
			for (OutputChunk c: allChunks) {
				if (c.getReferenceCount() == 0 && c.getID() != 0) {
					zeroCount++;
					pruned.remove(c);
				}
			}
		}
		
		return pruned;
	}
	
	private int getNextId() {
		while(this.usedIds.contains(this.chunkIdCounter)) {
			this.chunkIdCounter++;
		}
		this.usedIds.add(this.chunkIdCounter);
		return this.chunkIdCounter;
	}
	
	public boolean isOutput() {
		return forOutput;
	}

	public boolean isManagerCached() {
		return cacheManager;
	}

	public boolean getIgnoreCache() {
		return ignoreCache;
	}

	public Chunkable getPrimaryObject() {
		return this.primaryChunk.getRepresentedObject();
	}
	
	public File getResource() {
		return this.resource;
	}
	
	public String getCanonicalResourcePath() {
		return this.canPath;
	}
	
	public InputStream getInputStream() throws UnsupportedOperationException {
		if (this.forOutput)
			throw new UnsupportedOperationException("IOManager is for output, only has an outputstream");
		return this.in;
	}
	
	public OutputStream getOutputStream() throws UnsupportedOperationException {
		if (!this.forOutput)
			throw new UnsupportedOperationException("IOManager is for input, only has an inputstream");
		return this.out;
	}
	
	public int getChunkID(Chunk chunk) {
		if (chunk == null)
			return -1;
		if (chunk.getIOManager() != this)
			return chunk.getIOManager().getChunkID(chunk);
		
		Integer id = this.chunkToId.get(chunk);
		if (id == null)
			return -1;
		return id.intValue();
	}
	
	public Chunk getChunk(IOManager owner, int id) throws UnsupportedOperationException, VariableAccessException {
		if (this.forOutput) 
			throw new UnsupportedOperationException("Method only available when reading IOManager");
		if (owner == null) 
			owner = this;
		Chunk found = owner.findChunk(id);
		if (found != null)
			return found;
		else
			throw new VariableAccessException("Chunk id isn't present in given IOManager: " + id);
	}
	
	public InputChunk newChunk(int id, Class<? extends Chunkable> type) throws UnsupportedOperationException {
		if (this.forOutput) 
			throw new UnsupportedOperationException("Method only available when reading IOManager");
		InputChunk c = new InputChunk(this, type);
		this.idToChunk.put(id, c);
		this.chunkToId.put(c, id);
		return c;
	}
	
	private Chunk findChunk(Chunkable object) {
		if (this.forOutput)
			return this.objToChunk.get(object);
		
		Set<Chunk> chunks = this.chunkToId.keySet();
		for (Chunk c: chunks) {
			if (c.getRepresentedObject() == object)
				return c;
		}
		return null;
	}
	
	private Chunk findChunk(int id) {
		if (!this.forOutput) 
			return this.idToChunk.get(id);
		
		Set<Entry<Chunk, Integer>> chunks = this.chunkToId.entrySet();
		for (Entry<Chunk, Integer> e: chunks) {
			if (e.getValue().intValue() == id)
				return e.getKey();
		}
		return null;
	}
	
	private OutputChunk newOutputChunk(Chunkable object) {
		int newId = this.getNextId();
		OutputChunk c = new OutputChunk(this, object.getClass(), object);
		this.objToChunk.put(object, c);
		this.chunkToId.put(c, newId);
		
		this.pushChunkableOnRefStack(object);
		object.writeChunk(c);
		this.popChunkableOffRefStack();
		
		return c;
	}
	
	private OutputChunk reuseOutputChunk(Chunk object) {
		Chunkable obj = object.getRepresentedObject();
		OutputChunk c = new OutputChunk(this, object.getChunkType(), obj);
		int id = object.getID();
		this.objToChunk.put(obj, c);
		this.chunkToId.put(c, id);
		
		this.pushChunkableOnRefStack(obj);
		obj.writeChunk(c);
		this.popChunkableOffRefStack();
		
		return c;
	}
	
	public Chunk getChunk(Chunkable object) throws UnsupportedOperationException {
		if (!this.forOutput)
			throw new UnsupportedOperationException("Method only available when writing IOManager");
		
		if (this.cacheManager) {
			IOManager prev = allManagers.get(this.canPath);
			if (prev != null) {
				Chunk pc = prev.findChunk(object);
				if (pc != null) 
					return this.reuseOutputChunk(pc);
			}
		}
		if (!this.ignoreCache) {
			Collection<IOManager> iom = allManagers.values();
			for (IOManager m: iom) {
				Chunk pc = m.findChunk(object);
				if (pc != null)
					return pc;
			}
		}
		Chunk pc = this.findChunk(object);
		if (pc != null)
			return pc;
		else
			return newOutputChunk(object);
	}
		
	public Chunkable newInstance(Class<? extends Chunkable> type) throws VariableAccessException, NullPointerException {
		if (type == null)
			throw new NullPointerException("Class type can't be null");
		try {
			Constructor<? extends Chunkable> con = type.getDeclaredConstructor(new Class<?>[0]);
			con.setAccessible(true);
			Chunkable c = con.newInstance(new Object[0]);
			con.setAccessible(false);
			return c;
		} catch (Exception e) {
			throw new VariableAccessException("Unable to instantiate an object of class: " + type.getSimpleName(), e);
		}
	}
	
	public Variable newVariable(String name, VariableType type) {
		return this.impl.createVariable(name, type);
	}
	
	/**
	 * Utility method to compute the canonical path string, treating relativePath as a relative
	 * file path with respect to getFile()'s canonical path.  Returns null if the canonical path
	 * can't be generated.
	 */
	public String computeCanonicalPath(String relativePath) {
		try {
			File can = new File(this.resource.getCanonicalPath());
			if (can.getParentFile() != null && can.getParent() != null && !can.getParent().equals("")) {
				return new File(can.getParentFile().getCanonicalPath() + File.separator + relativePath).getCanonicalPath();
			} else
				return new File(relativePath).getCanonicalPath();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Computes the relative path between getFile()'s path and the given path.  If the given
	 * path is relative, path is converted to a canonical path as per the rules in File.
	 * Returns null if the canonical path conversion fails.
	 */
	public String computeRelativePath(String path) {		
		String fullFilePath = null;
		File fullPathFile = null;
		try {
			fullFilePath =this.resource.getCanonicalPath();
			fullPathFile = new File(path).getCanonicalFile();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		File pathParent = fullPathFile.getParentFile();
		String steps = fullPathFile.getName();
		
		while (pathParent != null && fullFilePath.toLowerCase().indexOf(pathParent.getPath().toLowerCase()) < 0) {
			steps = pathParent.getName() + File.separator + steps;
			pathParent = pathParent.getParentFile();
		}
			
		String root = pathParent == null ? "" : pathParent.getPath();
		File fileParent = new File(fullFilePath).getParentFile();
		
		while(fileParent != null && !root.equalsIgnoreCase(fileParent.getPath())) {
			fileParent = fileParent.getParentFile();
			steps = ".." + File.separator + steps;
		}
		
		return steps;
	}
	
	private void pushChunkableOnRefStack(Chunkable chunk) throws UnsupportedOperationException, VariableAccessException {
		if (!this.forOutput)
			throw new UnsupportedOperationException("Method only available when writing IOManager");
		if (this.refStack.contains(chunk))
			throw new VariableAccessException("Adding chunkable creates a cyclical reference");
		this.refStack.push(chunk);
	}
	
	private void popChunkableOffRefStack() throws UnsupportedOperationException {
		if (!this.forOutput)
			throw new UnsupportedOperationException("Method only available when writing IOManager");
		if (this.refStack.size() > 0)
			this.refStack.pop();
	}
	
	public ChunkIOImpl getImpl() {
		return this.impl;
	}
}
