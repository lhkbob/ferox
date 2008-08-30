package com.ferox.core.util.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.*;

import com.ferox.core.util.IOUtil;


class Variable {
	String name;
	int hash;
	boolean compressed;
	Object value;
	byte valueType;
	
	public static Variable read(InputStream in, InputChunk chunk) throws IOException {
		Variable var = new Variable();
		var.compressed = IOUtil.readBoolean(in);
		if (var.compressed)
			var.hash = IOUtil.readInt(in);
		else
			var.name = IOUtil.readString(in);
		
		byte type = IOUtil.readByte(in);
		if (type == IOUtil.TYPE_NULL) {
			var.valueType = IOUtil.readByte(in);
			var.value = null;
		} else {
			var.valueType = type;
			switch(var.valueType) {
			case IOUtil.TYPE_BOOLEAN:
				var.value = Byte.valueOf((IOUtil.readBoolean(in) ? (byte)1 : (byte)0));
				break;
			case IOUtil.TYPE_BYTE:
				var.value = Byte.valueOf(IOUtil.readByte(in));
				break;
			case IOUtil.TYPE_SHORT:
				var.value = Short.valueOf(IOUtil.readShort(in));
				break;
			case IOUtil.TYPE_INT: case IOUtil.TYPE_OBJECT:
				var.value = Integer.valueOf(IOUtil.readInt(in));
				break;
			case IOUtil.TYPE_LONG:
				var.value = Long.valueOf(IOUtil.readLong(in));
				break;
			case IOUtil.TYPE_CHAR:
				var.value = Character.valueOf(IOUtil.readChar(in));
				break;
			case IOUtil.TYPE_FLOAT:
				var.value = Float.valueOf(IOUtil.readFloat(in));
				break;
			case IOUtil.TYPE_DOUBLE:
				var.value = Double.valueOf(IOUtil.readDouble(in));
				break;
			case IOUtil.TYPE_STRING:
				var.value = IOUtil.readString(in);
				break;
			case IOUtil.TYPE_BOOLEAN_ARRAY:
				var.value = IOUtil.readBooleanArray(in);
				break;
			case IOUtil.TYPE_BYTE_ARRAY:
				var.value = IOUtil.readByteArray(in);
				break;
			case IOUtil.TYPE_INT_ARRAY:
				var.value = IOUtil.readIntArray(in);
				break;
			case IOUtil.TYPE_SHORT_ARRAY:
				var.value = IOUtil.readShortArray(in);
				break;
			case IOUtil.TYPE_LONG_ARRAY:
				var.value = IOUtil.readLongArray(in);
				break;
			case IOUtil.TYPE_FLOAT_ARRAY:
				var.value = IOUtil.readFloatArray(in);
				break;
			case IOUtil.TYPE_DOUBLE_ARRAY:
				var.value = IOUtil.readDoubleArray(in);
				break;
			case IOUtil.TYPE_CHAR_ARRAY:
				var.value = IOUtil.readCharArray(in);
				break;
			case IOUtil.TYPE_STRING_ARRAY:
				var.value = IOUtil.readStringArray(in);
				break;
			case IOUtil.TYPE_BYTE_BUFFER:
				var.value = IOUtil.readByteBuffer(in);
				break;
			case IOUtil.TYPE_SHORT_BUFFER:
				var.value = IOUtil.readShortBuffer(in);
				break;
			case IOUtil.TYPE_INT_BUFFER:
				var.value = IOUtil.readIntBuffer(in);
				break;
			case IOUtil.TYPE_LONG_BUFFER:
				var.value = IOUtil.readLongBuffer(in);
				break;
			case IOUtil.TYPE_FLOAT_BUFFER:
				var.value = IOUtil.readFloatBuffer(in);
				break;
			case IOUtil.TYPE_DOUBLE_BUFFER:
				var.value = IOUtil.readDoubleBuffer(in);
				break;
			case IOUtil.TYPE_CHAR_BUFFER:
				var.value = IOUtil.readCharBuffer(in);
				break;
			}
		}
		
		return var;
	}
	
	public void write(OutputStream out) throws IOException {		
		IOUtil.write(out, this.compressed);
		
		if (this.compressed) 
			IOUtil.write(out, this.hash);
		 else 
			IOUtil.write(out, this.name);
			
		if (this.value == null) {
			IOUtil.write(out, IOUtil.TYPE_NULL);
			IOUtil.write(out, this.valueType);
		} else {
			IOUtil.write(out, this.valueType);
			switch(this.valueType) {	
			case IOUtil.TYPE_BOOLEAN:
				IOUtil.write(out, ((Number)this.value).byteValue() != 0);
				break;
			case IOUtil.TYPE_BYTE:
				IOUtil.write(out, ((Number)this.value).byteValue());
				break;
			case IOUtil.TYPE_INT:
				IOUtil.write(out, ((Number)this.value).intValue());
				break;
			case IOUtil.TYPE_SHORT:
				IOUtil.write(out, ((Number)this.value).shortValue());
				break;
			case IOUtil.TYPE_LONG:
				IOUtil.write(out, ((Number)this.value).longValue());
				break;
			case IOUtil.TYPE_FLOAT:
				IOUtil.write(out, ((Number)this.value).floatValue());
				break;
			case IOUtil.TYPE_DOUBLE:
				IOUtil.write(out, ((Number)this.value).doubleValue());
				break;
			case IOUtil.TYPE_CHAR:
				IOUtil.write(out, ((Character)this.value).charValue());
				break;
			case IOUtil.TYPE_STRING:
				IOUtil.write(out, (String)this.value);
				break;
			case IOUtil.TYPE_BOOLEAN_ARRAY:
				IOUtil.write(out, (boolean[])this.value);
				break;
			case IOUtil.TYPE_INT_ARRAY:
				IOUtil.write(out, (int[])this.value);
				break;
			case IOUtil.TYPE_SHORT_ARRAY:
				IOUtil.write(out, (short[])this.value);
				break;
			case IOUtil.TYPE_LONG_ARRAY:
				IOUtil.write(out, (long[])this.value);
				break;
			case IOUtil.TYPE_BYTE_ARRAY:
				IOUtil.write(out, (byte[])this.value);
				break;
			case IOUtil.TYPE_CHAR_ARRAY:
				IOUtil.write(out, (char[])this.value);
				break;
			case IOUtil.TYPE_FLOAT_ARRAY:
				IOUtil.write(out, (float[])this.value);
				break;
			case IOUtil.TYPE_DOUBLE_ARRAY:
				IOUtil.write(out, (double[])this.value);
				break;
			case IOUtil.TYPE_STRING_ARRAY:
				IOUtil.write(out, (String[])this.value);
				break;
			case IOUtil.TYPE_BYTE_BUFFER:
				IOUtil.write(out, (ByteBuffer)this.value);
				break;
			case IOUtil.TYPE_INT_BUFFER:
				IOUtil.write(out, (IntBuffer)this.value);
				break;
			case IOUtil.TYPE_SHORT_BUFFER:
				IOUtil.write(out, (ShortBuffer)this.value);
				break;
			case IOUtil.TYPE_LONG_BUFFER:
				IOUtil.write(out, (LongBuffer)this.value);
				break;
			case IOUtil.TYPE_CHAR_BUFFER:
				IOUtil.write(out, (CharBuffer)this.value);
				break;
			case IOUtil.TYPE_FLOAT_BUFFER:
				IOUtil.write(out, (FloatBuffer)this.value);
				break;
			case IOUtil.TYPE_DOUBLE_BUFFER:
				IOUtil.write(out, (DoubleBuffer)this.value);
				break;
			case IOUtil.TYPE_OBJECT:
				IOUtil.write(out, ((Chunk)this.value).id);
				break;
			}
		}
	}	
}