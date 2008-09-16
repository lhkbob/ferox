package com.ferox.core.util.io;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

import com.ferox.core.states.atoms.BufferData;
import com.ferox.core.states.atoms.GLSLShaderObject;
import com.ferox.core.states.atoms.VertexArray;
import com.ferox.core.states.manager.Geometry;
import com.ferox.core.util.IOUtil;

public class IOManager {
	private static final byte MAGIC_NUMBER = 57;
	private static final HashMap<Class<?>, ChunkableInstantiator> cInstance = new HashMap<Class<?>, ChunkableInstantiator>();

	private static class ChunkType {
		int id;
		String qualifiedName;
	}
	
	static {
		BufferData d = new BufferData(null, BufferData.DataType.FLOAT, 3);
		VertexArray a = new VertexArray(d, 3);
		Geometry g = new Geometry(a, null, Geometry.PolygonType.TRIANGLES);
		GLSLShaderObject o = new GLSLShaderObject(new String[] {""}, GLSLShaderObject.GLSLType.VERTEX);
		
		IOManager.registerInstantiator(d);
		IOManager.registerInstantiator(a);
		IOManager.registerInstantiator(g);
		IOManager.registerInstantiator(o);
	}
		
	private HashMap<Class<?>, ChunkType> classToTypesMap;
	private HashMap<Integer, ChunkType> idToTypesMap;
	
	private HashMap<Chunkable, Chunk> objectToChunkMap; // used for writing
	private HashMap<Integer, Chunk> idToChunkMap; // used for reading
	private int chunkIdCounter;
	private int chunkTypeCounter;
	
	private File ioFile;
	private boolean forOutput;
	private Stack<Chunkable> refStack;
	
	private IOManager(File file, boolean forOutput) {
		this.ioFile = file;
		this.forOutput = forOutput;
		
		this.refStack = new Stack<Chunkable>();
		this.chunkIdCounter = 0;
		this.chunkTypeCounter = 0;
		
		this.classToTypesMap = new HashMap<Class<?>, ChunkType>();
		this.idToTypesMap = new HashMap<Integer, ChunkType>();
		
		if (this.forOutput)
			this.objectToChunkMap = new HashMap<Chunkable, Chunk>();
		else
			this.idToChunkMap = new HashMap<Integer, Chunk>();
	}
	
	public static void registerInstantiator(ChunkableInstantiator c) {
		Class<?> t = c.getChunkableClass();
		cInstance.put(t, c);
	}
	
	public static void unregisterInstantiator(Class<? extends Chunkable> c) {
		cInstance.remove(c);
	}
	
	public static void write(File file, Chunkable chunk) throws IOException {
		if (file == null || chunk == null)
			throw new NullPointerException("Can't write a null chunk, or to a null file");
		if (file.isDirectory())
			throw new IOException("Can't write to a directory");
		
		write(new FileOutputStream(file), file, chunk);
	}
	
	public static void write(OutputStream stream, Chunkable chunk) throws IOException {
		write(stream, null, chunk);
	}
	
	public static void write(OutputStream stream, File file, Chunkable chunk) throws IOException {
		try {
			IOManager manager = new IOManager(file, true);
			
			// compile every chunks variables
			OutputChunk head = manager.addChunk(chunk);
			manager.pushChunkableOnRefStack(chunk);
			chunk.writeChunk(head);
			manager.popChunkableOffRefStack();
			
			// prune any chunks that may no longer be referenced, in which case they can
			// be removed since we don't need to save them
			OutputChunk[] allChunks = new OutputChunk[manager.objectToChunkMap.size()];
			Iterator<Entry<Chunkable, Chunk>> it = manager.objectToChunkMap.entrySet().iterator();
			int i = 0;
			while (it.hasNext()) {
				allChunks[i] = (OutputChunk)it.next().getValue();
				i++;
			}
			
			OutputChunk[] pruned = pruneChunks(allChunks);
			ChunkType[] prunedTypes = pruneTypes(manager.idToTypesMap, pruned);
			
			// write the file
					
			// fill the out buffer
			IOUtil.write(stream, MAGIC_NUMBER);
			IOUtil.write(stream, prunedTypes.length);
			IOUtil.write(stream, pruned.length);
			for (i = 0; i < prunedTypes.length; i++) {
				IOUtil.write(stream, prunedTypes[i].qualifiedName);
				IOUtil.write(stream, prunedTypes[i].id);
			}
			
			for (i = 0; i < pruned.length; i++) {
				IOUtil.write(stream, pruned[i].type);
				IOUtil.write(stream, pruned[i].id);
				IOUtil.write(stream, pruned[i].getNumVariables());
				pruned[i].serialize(stream);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new IOException("Exception occurred during writing: " + e.toString());
		} finally {
			// cleanup
			if (stream != null) {
				stream.flush();
				stream.close();
			}
		}
	}
	
	public static Chunkable read(File file) throws IOException {
		// validate the file
		if (file == null)
			throw new NullPointerException("Can't read from a null file object");
		if (!file.canRead() || file.isDirectory() || !file.exists())
			throw new IOException("Can't read from the given file");
		
		return read(new FileInputStream(file), file);
	}
	
	public static Chunkable read(InputStream stream) throws IOException {
		return read(stream, null);
	}
	
	public static Chunkable read(InputStream stream, File location) throws IOException {
		try {
			// grab the header for total file size and last minute validation
			if (IOUtil.readByte(stream) != MAGIC_NUMBER)
				throw new IOException("Cannot read from a file with the wrong format");

			// load the data into memory
			IOManager manager = new IOManager(location, false);
			int numTypes = IOUtil.readInt(stream);
			int numChunks = IOUtil.readInt(stream);
			
			// instantiate all of the types
			for (int i = 0; i < numTypes; i++) {
				ChunkType t = new ChunkType();
				t.qualifiedName = IOUtil.readString(stream);
				t.id = IOUtil.readInt(stream);
								
				manager.idToTypesMap.put(t.id, t);
				manager.classToTypesMap.put(Class.forName(t.qualifiedName), t);
			}
			
			// parse the chunks, so that ids exist and offsets are placed
			// correctly
			InputChunk[] loadedChunks = new InputChunk[numChunks];
			for (int i = 0; i < numChunks; i++) {
				int type = IOUtil.readInt(stream);
				int id = IOUtil.readInt(stream);
				int numVars = IOUtil.readInt(stream);
				loadedChunks[i] = new InputChunk(manager, id, type, numVars);
				
				manager.idToChunkMap.put(loadedChunks[i].id, loadedChunks[i]);
				loadedChunks[i].unserialize(stream);
			}
			
			// build the object within the file
			return manager.getChunk(0).getRepresentedObject();
		} catch (Exception e) {
			e.printStackTrace();
			throw new IOException("Exception occurred during reading: " + e.getMessage());
		} finally {
			// cleanup
			if (stream != null)
				stream.close();
		}
	}
	
	private static ChunkType[] pruneTypes(HashMap<Integer, ChunkType> types, OutputChunk[] pruned) {
		ArrayList<ChunkType> p = new ArrayList<ChunkType>();
		for (int i = 0; i < pruned.length; i++) {
			ChunkType t = types.get(pruned[i].type);
			if (t != null) {
				p.add(t);
				types.remove(pruned[i].type);
			}
		}
		
		return p.toArray(new ChunkType[p.size()]);
	}
	
	private static OutputChunk[] pruneChunks(OutputChunk[] allChunks) {
		int zeroCount;
		int nonNullCount = allChunks.length;
		do {
			zeroCount = 0;
			for (int i = 0; i < allChunks.length; i++) {
				if (allChunks[i] != null)
					allChunks[i].updateReferences();
			}
			for (int i = 0; i < allChunks.length; i++) {
				if (allChunks[i] != null && allChunks[i].getRefCount() == 0 && allChunks[i].id != 0) {
					zeroCount++;
					nonNullCount--;
					allChunks[i] = null;
				}
			}
		} while (zeroCount > 0);
		
		OutputChunk[] pruned = allChunks;
		if (nonNullCount < allChunks.length) {
			// need to remove some
			pruned = new OutputChunk[nonNullCount];
			nonNullCount = 0;
			for (int i = 0; i < allChunks.length; i++) {
				if (allChunks[i] != null) {
					pruned[nonNullCount++] = allChunks[i];
				}
			}
		}
		
		// sort pruned chunks so that chunks with the most references come first
		Arrays.sort(pruned, new Comparator<OutputChunk>() {
			public int compare(OutputChunk arg0, OutputChunk arg1) {
				return arg1.getRefCount() - arg0.getRefCount();
			}
		});
		
		return pruned;
	}
	
	/**
	 * The File that is being read or written to.
	 */
	public File getFile() {
		return this.ioFile;
	}
	
	/**
	 * Utility method to compute the canonical path string, treating relativePath as a relative
	 * file path with respect to getFile()'s canonical path.  Returns null if the canonical path
	 * can't be generated.
	 */
	public String computeCanonicalPath(String relativePath) throws IOException {
		if (this.ioFile == null)
			return new File(relativePath).getCanonicalPath();
		
		File can = new File(this.ioFile.getCanonicalPath());
		if (can.getParentFile() != null && can.getParent() != null && !can.getParent().equals("")) {
			return new File(can.getParentFile().getCanonicalPath() + File.separator + relativePath).getCanonicalPath();
		} else
			return new File(relativePath).getCanonicalPath();
	}
	
	/**
	 * Computes the relative path between getFile()'s path and the given path.  If the given
	 * path is relative, path is converted to a canonical path as per the rules in File.
	 * Returns null if the canonical path conversion fails.
	 */
	public String computeRelativePath(String path) throws IOException {
		if (this.ioFile == null)
			return new File(path).getCanonicalPath();
		
		String fullFilePath = this.ioFile.getCanonicalPath();
		File fullPathFile = new File(path).getCanonicalFile();
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
	
	public Chunkable newInstance(int type) throws IOException {
		Class<?> t = null;
		try {
			t = Class.forName(this.idToTypesMap.get(type).qualifiedName);
		} catch (ClassNotFoundException e) {
			throw new IOException("Unable to load class resource for: " + this.idToTypesMap.get(type).qualifiedName);
		}
		Chunkable f;
		if (cInstance.containsKey(t)) {
				f = cInstance.get(t).newInstance();
				if (f == null || !f.getClass().isAssignableFrom(t))
					throw new IOException("Improper use ChunkableInstanciator, it must return a non-null instance that's a subclass of " + t.getName());
		} else
			try {
				f = (Chunkable)t.newInstance();
			} catch (InstantiationException e) {
				throw new IOException("Unable to create a new default instance of " + t.getName() + ", " + e.getMessage());
			} catch (IllegalAccessException e) {
				throw new IOException("Unable to create a new default instance of " + t.getName() + ", " + e.getMessage());
			}
		return f;		
	}
	
	public Chunk getChunk(int id) {
		if (this.forOutput)
			throw new UnsupportedOperationException("Method available only when reading a file");
		
		Chunk c = this.idToChunkMap.get(id);
		if (c != null)
			return c;
		throw new ValueAccessException("Object not present to read");
	}
	
	public OutputChunk addChunk(Chunkable chunk) {
		if (!this.forOutput)
			throw new UnsupportedOperationException("Method available only when writing a file");
	
		OutputChunk c = (OutputChunk)this.objectToChunkMap.get(chunk);
		if (c != null)
			return c;
		
		Class<? extends Chunkable> type = chunk.getClass();
		int newId = this.chunkIdCounter++;
		if (!this.classToTypesMap.containsKey(type)) {
			ChunkType t = new ChunkType();
			t.id = this.chunkTypeCounter++;
			t.qualifiedName = type.getName();
			this.classToTypesMap.put(type, t);
			this.idToTypesMap.put(t.id, t);
		}
		int typeId = this.classToTypesMap.get(type).id;
		
		c = new OutputChunk(this, newId, typeId, chunk);
		this.objectToChunkMap.put(chunk, c);
		
		return c;
	}
	
	public void pushChunkableOnRefStack(Chunkable chunk) {
		if (this.refStack.contains(chunk))
			throw new ValueAccessException("Adding chunkable creates a cyclical reference");
		this.refStack.push(chunk);
	}
	
	public void popChunkableOffRefStack() {
		if (this.refStack.size() > 0)
			this.refStack.pop();
	}
}
