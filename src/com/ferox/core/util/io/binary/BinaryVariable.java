package com.ferox.core.util.io.binary;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

import com.ferox.core.util.IOUtil;
import com.ferox.core.util.io.Chunk;
import com.ferox.core.util.io.IOManager;
import com.ferox.core.util.io.Variable;
import com.ferox.core.util.io.VariableAccessException;
import com.ferox.core.util.io.Chunk.VariableType;

public class BinaryVariable extends Variable {

	public BinaryVariable(String name, VariableType type) {
		super(name, type);
	}
	
	public BinaryVariable(String name, VariableType type, int hash) {
		super(name, type, hash);
	}

	public static BinaryVariable readVariable(InputStream in, IOManager manager) throws IOException {
		boolean compressed = IOUtil.readBoolean(in);
		BinaryVariable var = null;
		int hash = 0;
		String name = "";
		if (compressed)
			hash = IOUtil.readInt(in);
		else {
			name = IOUtil.readString(in);
			hash = Chunk.getCompressedName(name);
		}
		
		byte type = IOUtil.readByte(in);
		VariableType realType = null;
		if (type < 0)
			realType = getTypeEnum(IOUtil.readByte(in));
		else
			realType = getTypeEnum(type);
		var = new BinaryVariable(name, realType, hash);
		var.setNameCompressed(compressed);
		
		if (type < 0)
			var.setValue(null);
		else
			var.read(in, manager);
		return var;
	}
	
	private static VariableType getTypeEnum(byte type) {
		VariableType[] v = VariableType.values();
		for (int i = 0; i < v.length; i++) {
			if (v[i].getTypeCode() == type)
				return v[i];
		}
		return null;
	}
	
	@Override
	public void read(InputStream in, IOManager manager) throws IOException {
		switch(this.getType()) {
		case BOOLEAN:
			this.setValue(Boolean.valueOf(IOUtil.readBoolean(in)));
			break;
		case BYTE:
			this.setValue(Byte.valueOf(IOUtil.readByte(in)));
			break;
		case SHORT:
			this.setValue(Short.valueOf(IOUtil.readShort(in)));
			break;
		case INT:
			this.setValue(Integer.valueOf(IOUtil.readInt(in)));
			break;
		case LONG:
			this.setValue(Long.valueOf(IOUtil.readLong(in)));
			break;
		case CHAR:
			this.setValue(Character.valueOf(IOUtil.readChar(in)));
			break;
		case FLOAT:
			this.setValue(Float.valueOf(IOUtil.readFloat(in)));
			break;
		case DOUBLE:
			this.setValue(Double.valueOf(IOUtil.readDouble(in)));
			break;
		case STRING:
			this.setValue(IOUtil.readString(in));
			break;
		case BOOLEAN_ARRAY:
			this.setValue(IOUtil.readBooleanArray(in));
			break;
		case BYTE_ARRAY:
			this.setValue(IOUtil.readByteArray(in));
			break;
		case INT_ARRAY:
			this.setValue(IOUtil.readIntArray(in));
			break;
		case SHORT_ARRAY:
			this.setValue(IOUtil.readShortArray(in));
			break;
		case LONG_ARRAY:
			this.setValue(IOUtil.readLongArray(in));
			break;
		case FLOAT_ARRAY:
			this.setValue(IOUtil.readFloatArray(in));
			break;
		case DOUBLE_ARRAY:
			this.setValue(IOUtil.readDoubleArray(in));
			break;
		case CHAR_ARRAY:
			this.setValue(IOUtil.readCharArray(in));
			break;
		case STRING_ARRAY:
			this.setValue(IOUtil.readStringArray(in));
			break;
		case BYTE_BUFFER:
			this.setValue(IOUtil.readByteBuffer(in));
			break;
		case SHORT_BUFFER:
			this.setValue(IOUtil.readShortBuffer(in));
			break;
		case INT_BUFFER:
			this.setValue(IOUtil.readIntBuffer(in));
			break;
		case LONG_BUFFER:
			this.setValue(IOUtil.readLongBuffer(in));
			break;
		case FLOAT_BUFFER:
			this.setValue(IOUtil.readFloatBuffer(in));
			break;
		case DOUBLE_BUFFER:
			this.setValue(IOUtil.readDoubleBuffer(in));
			break;
		case CHAR_BUFFER:
			this.setValue(IOUtil.readCharBuffer(in));
			break;
		case CHUNK:
			this.setValue(readChunk(in, manager));
			break;
		case CHUNK_ARRAY:
			Chunk[] vals = new Chunk[IOUtil.readInt(in)];
			for (int i = 0; i < vals.length; i++) 
				vals[i] = readChunk(in, manager);
			this.setValue(vals);
			break;
		}
	}

	@Override
	public void write(OutputStream out, IOManager manager) throws IOException {
		IOUtil.write(out, this.isNameCompressed());
		
		if (this.isNameCompressed()) {
			IOUtil.write(out, this.getCompressedName());
		} else 
			IOUtil.write(out, this.getName());
			
		Object value = this.getValue();
		VariableType type = this.getType();
		if (value == null) {
			IOUtil.write(out, (byte)-1);
			IOUtil.write(out, type.getTypeCode());
		} else {
			IOUtil.write(out, type.getTypeCode());
			switch(type) {	
			case BOOLEAN:
				IOUtil.write(out, ((Boolean)value).booleanValue());
				break;
			case BYTE:
				IOUtil.write(out, ((Number)value).byteValue());
				break;
			case INT:
				IOUtil.write(out, ((Number)value).intValue());
				break;
			case SHORT:
				IOUtil.write(out, ((Number)value).shortValue());
				break;
			case LONG:
				IOUtil.write(out, ((Number)value).longValue());
				break;
			case FLOAT:
				IOUtil.write(out, ((Number)value).floatValue());
				break;
			case DOUBLE:
				IOUtil.write(out, ((Number)value).doubleValue());
				break;
			case CHAR:
				IOUtil.write(out, ((Character)value).charValue());
				break;
			case STRING:
				IOUtil.write(out, (String)value);
				break;
			case BOOLEAN_ARRAY:
				IOUtil.write(out, (boolean[])value);
				break;
			case INT_ARRAY:
				IOUtil.write(out, (int[])value);
				break;
			case SHORT_ARRAY:
				IOUtil.write(out, (short[])value);
				break;
			case LONG_ARRAY:
				IOUtil.write(out, (long[])value);
				break;
			case BYTE_ARRAY:
				IOUtil.write(out, (byte[])value);
				break;
			case CHAR_ARRAY:
				IOUtil.write(out, (char[])value);
				break;
			case FLOAT_ARRAY:
				IOUtil.write(out, (float[])value);
				break;
			case DOUBLE_ARRAY:
				IOUtil.write(out, (double[])value);
				break;
			case STRING_ARRAY:
				IOUtil.write(out, (String[])value);
				break;
			case BYTE_BUFFER:
				IOUtil.write(out, (ByteBuffer)value);
				break;
			case INT_BUFFER:
				IOUtil.write(out, (IntBuffer)value);
				break;
			case SHORT_BUFFER:
				IOUtil.write(out, (ShortBuffer)value);
				break;
			case LONG_BUFFER:
				IOUtil.write(out, (LongBuffer)value);
				break;
			case CHAR_BUFFER:
				IOUtil.write(out, (CharBuffer)value);
				break;
			case FLOAT_BUFFER:
				IOUtil.write(out, (FloatBuffer)value);
				break;
			case DOUBLE_BUFFER:
				IOUtil.write(out, (DoubleBuffer)value);
				break;
			case CHUNK: 
				writeChunk(out, manager, (Chunk)value);
				break; 
			case CHUNK_ARRAY:
				Chunk[] vals = (Chunk[])value;
				IOUtil.write(out, vals.length);
				for (int i = 0; i < vals.length; i++) 
					writeChunk(out, manager, vals[i]);
				break;
			}
		}
	}

	private static Chunk readChunk(InputStream in, IOManager manager) throws IOException {
		BinaryImporter imp = (BinaryImporter)manager.getImpl();
		IOManager cMan = imp.getManager(IOUtil.readInt(in));
		int id = IOUtil.readInt(in);
		int type = 0;
		if (cMan == manager)
			type = IOUtil.readInt(in);
		Chunk c;
		try {
			c = manager.getChunk(cMan, id);
		} catch (VariableAccessException vae) {
			if (cMan == manager)
				c = manager.newChunk(id, imp.getType(type));
			else
				throw vae;
		}
		return c;
	}
	
	private static void writeChunk(OutputStream out, IOManager manager, Chunk chunk) throws IOException {
		BinaryExporter exp = (BinaryExporter)manager.getImpl();
		IOUtil.write(out, exp.getManagerId(chunk.getIOManager()));
		IOUtil.write(out, chunk.getID());
		if (chunk.getIOManager() == manager)
			IOUtil.write(out, exp.getTypeId(chunk.getChunkType()));
	}
}
