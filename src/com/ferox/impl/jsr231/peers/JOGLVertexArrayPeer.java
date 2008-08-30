package com.ferox.impl.jsr231.peers;

import java.nio.Buffer;

import javax.media.opengl.GL;

import com.ferox.core.states.StateManager;
import com.ferox.core.states.StateUnit;
import com.ferox.core.states.atoms.BufferData;
import com.ferox.core.states.atoms.VertexArray;
import com.ferox.core.states.atoms.BufferData.BufferTarget;
import com.ferox.core.states.atoms.BufferData.DataType;
import com.ferox.core.states.atoms.VertexArray.VertexArrayTarget;
import com.ferox.core.states.atoms.VertexArray.VertexArrayUnit;
import com.ferox.core.states.manager.Geometry;
import com.ferox.core.states.manager.Geometry.InterleavedType;
import com.ferox.core.util.FeroxException;
import com.ferox.impl.jsr231.JOGLRenderContext;

public class JOGLVertexArrayPeer extends SimplePeer<VertexArray, NoRecord> {
	private static int cachePos;
	private static int cacheLimit;
	
	private VertexArrayUnit unit;
	private boolean interleavedActive;
	
	public JOGLVertexArrayPeer(JOGLRenderContext context) {
		super(context);
	}
	
	private static int getGLTypeEnum(DataType type) {
		switch(type) {
		case DOUBLE: return GL.GL_DOUBLE;
		case FLOAT: return GL.GL_FLOAT;
		case INT: return GL.GL_INT;
		case SHORT: return GL.GL_SHORT;
		case UNSIGNED_BYTE: return GL.GL_UNSIGNED_BYTE;
		case UNSIGNED_INT: return GL.GL_UNSIGNED_INT;
		case UNSIGNED_SHORT: return GL.GL_UNSIGNED_SHORT;
		default:
			throw new FeroxException("Illegal buffer data type");
		}
	}
	
	private static int getGLInterleaved(InterleavedType inter) {
		switch(inter) {
		case N3F_V3F: return GL.GL_N3F_V3F;
		case T2F_N3F_V3F: return GL.GL_T2F_N3F_V3F;
		case T2F_V3F: return GL.GL_T2F_V3F;
		default:
			return 0;
		}
	}
	
	protected void applyState(VertexArray prevA, NoRecord prevR, VertexArray nextA, NoRecord nextR, GL gl) {
		if (!this.interleavedActive) {
			BufferData data = nextA.getBufferData();
			
			if (prevA == null || prevA.getBufferData() != nextA.getBufferData())
				data.applyState(context.getRenderManager(), (this.unit.getTarget() == VertexArrayTarget.INDEX ? BufferTarget.ELEMENT_BUFFER : BufferTarget.ARRAY_BUFFER));
					
			if (data.isVBO()) {
				switch(this.unit.getTarget()) {
				case VERTEX:
					if (prevA == null)
						gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
					gl.glVertexPointer(nextA.getElementSize(), getGLTypeEnum(data.getDataType()), nextA.getStride() * data.getByteSize(), nextA.getOffset() * data.getByteSize());
					break;
				case NORMAL:
					if (prevA == null)
						gl.glEnableClientState(GL.GL_NORMAL_ARRAY);
					gl.glNormalPointer(getGLTypeEnum(data.getDataType()), nextA.getStride() * data.getByteSize(), nextA.getOffset() * data.getByteSize());
					break;
				case INDEX:
					// TODO: figure out index locking
					break;
				case ATTRIB:
					if (prevA == null)
						gl.glEnableVertexAttribArray(this.unit.getUnit());
					gl.glVertexAttribPointer(this.unit.getUnit(), nextA.getElementSize(), getGLTypeEnum(data.getDataType()), false, nextA.getStride() * data.getByteSize(), nextA.getOffset() * data.getByteSize());
					break;
				case TEXCOORD:
					gl.glClientActiveTexture(GL.GL_TEXTURE0 + this.unit.getUnit());
					if (prevA == null)
						gl.glEnableClientState(GL.GL_TEXTURE_COORD_ARRAY);
					gl.glTexCoordPointer(nextA.getElementSize(), getGLTypeEnum(data.getDataType()), nextA.getStride() * data.getByteSize(), nextA.getOffset() * data.getByteSize());
					break;
				}
			} else {
				Buffer bd = data.getData();
				if (bd == null) 
					throw new FeroxException("Can't have a non-vbo buffer data that has a null backing buffer");
				
				cachePos = bd.position();
				cacheLimit = bd.limit();
				bd.limit(data.getCapacity());
				bd.position(nextA.getOffset());
				
				switch(this.unit.getTarget()) {
				case VERTEX:
					if (prevA == null)
						gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
					gl.glVertexPointer(nextA.getElementSize(), getGLTypeEnum(data.getDataType()), nextA.getStride() * data.getByteSize(), bd);
					break;
				case NORMAL:
					if (prevA == null)
						gl.glEnableClientState(GL.GL_NORMAL_ARRAY);
					gl.glNormalPointer(getGLTypeEnum(data.getDataType()), nextA.getStride() * data.getByteSize(), bd);
					break;
				case INDEX:
					// TODO: figure out index locking
					break;
				case ATTRIB:
					if (prevA == null)
						gl.glEnableVertexAttribArray(this.unit.getUnit());
					gl.glVertexAttribPointer(this.unit.getUnit(), nextA.getElementSize(), getGLTypeEnum(data.getDataType()), false, nextA.getStride() * data.getByteSize(), bd);
					break;
				case TEXCOORD:
					gl.glClientActiveTexture(GL.GL_TEXTURE0 + this.unit.getUnit());
					if (prevA == null)
						gl.glEnableClientState(GL.GL_TEXTURE_COORD_ARRAY);
					gl.glTexCoordPointer(nextA.getElementSize(), getGLTypeEnum(data.getDataType()), nextA.getStride() * data.getByteSize(), bd);
					break;
				}
				
				bd.position(cachePos);
				bd.limit(cacheLimit);
			}
		}
	}

	public void disableManager(Geometry manager) {
		if (this.interleavedActive) {
			this.interleavedActive = false;
			GL gl = this.context.getGL();
			
			manager.getVertices().getBufferData().restoreState(this.context.getRenderManager(), BufferTarget.ARRAY_BUFFER);
			
			switch(manager.getInterleavedType()) {
			case N3F_V3F:
				gl.glDisableClientState(GL.GL_NORMAL_ARRAY);
				gl.glDisableClientState(GL.GL_VERTEX_ARRAY);
				break;
			case T2F_N3F_V3F:
				gl.glDisableClientState(GL.GL_NORMAL_ARRAY);
			case T2F_V3F:
				gl.glClientActiveTexture(GL.GL_TEXTURE0 + manager.getInterleavedTextureUnit());
				gl.glDisableClientState(GL.GL_TEXTURE_COORD_ARRAY);
				gl.glDisableClientState(GL.GL_VERTEX_ARRAY);
				break;
			}
			
			if (manager.getIndices() != null) {
				this.setUnit(VertexArrayUnit.get(VertexArrayTarget.INDEX, 0));
				this.restoreState(manager.getIndices(), null);
			}
		}
	}
	
	public void prepareManager(StateManager manager, StateManager previous) {
		this.prepareManager((Geometry)manager, (Geometry)previous);
	}
	
	private void prepareManager(Geometry manager, Geometry previous) {
		if (previous != manager) {
			if (manager.isDataInterleaved()) {
				GL gl = this.context.getGL();
				
				if (previous != null) {
					switch(previous.getInterleavedType()) {
					case T2F_N3F_V3F:
					case T2F_V3F:
						if (manager.getInterleavedTextureUnit() >= 0 && manager.getInterleavedTextureUnit() != previous.getInterleavedTextureUnit()) {
							gl.glClientActiveTexture(GL.GL_TEXTURE0 + previous.getInterleavedTextureUnit());
							gl.glDisableClientState(GL.GL_TEXTURE_COORD_ARRAY);
						}
					default:
						break;
					}
				}
				manager.getVertices().getBufferData().applyState(this.context.getRenderManager(), BufferTarget.ARRAY_BUFFER);
				
				if (manager.getInterleavedTextureUnit() >= 0) 
					gl.glClientActiveTexture(GL.GL_TEXTURE0 + manager.getInterleavedTextureUnit());
				
				if (manager.getVertices().getBufferData().isVBO()) 
					gl.glInterleavedArrays(getGLInterleaved(manager.getInterleavedType()), 0, 0);
				else {
					Buffer bd = manager.getVertices().getBufferData().getData();
					cachePos = bd.position();
					cacheLimit = bd.limit();
					bd.clear();
					
					gl.glInterleavedArrays(getGLInterleaved(manager.getInterleavedType()), 0, bd);

					bd.position(cachePos);
					bd.limit(cacheLimit);
				}
				this.interleavedActive = false;
				this.setUnit(VertexArrayUnit.get(VertexArrayTarget.INDEX, 0));
				VertexArray ind = manager.getIndices();
				VertexArray pInd = (previous != null ? previous.getIndices() : null);
				if (pInd != ind) {
					if (pInd != null && ind == null)
						this.restoreState(pInd, null);
					else if (ind != null)
						this.applyState(pInd, null, ind, null);
				}
				
				this.interleavedActive = true;
			} else {
				if (previous != null && previous.isDataInterleaved()) {
					this.interleavedActive = true;
					this.disableManager(previous);
				}
				this.interleavedActive = false;
			}
		}
	}

	protected void restoreState(VertexArray cleanA, NoRecord cleanR, GL gl) {
		if (!this.interleavedActive) {
			cleanA.getBufferData().restoreState(context.getRenderManager(), (this.unit.getTarget() == VertexArrayTarget.INDEX ? BufferTarget.ELEMENT_BUFFER : BufferTarget.ARRAY_BUFFER));
			
			switch(this.unit.getTarget()) {
			case VERTEX:
				gl.glDisableClientState(GL.GL_VERTEX_ARRAY);
				break;
			case NORMAL:
				gl.glDisableClientState(GL.GL_NORMAL_ARRAY);
				break;
			case INDEX:
				// FIXME: figure out index locking
				break;
			case ATTRIB:
				gl.glDisableVertexAttribArray(this.unit.getUnit());
				break;
			case TEXCOORD:
				gl.glClientActiveTexture(GL.GL_TEXTURE0 + this.unit.getUnit());
				gl.glDisableClientState(GL.GL_TEXTURE_COORD_ARRAY);		
				break;
			}
		}
	}

	public void setUnit(StateUnit unit) {
		this.unit = (VertexArrayUnit)unit;
	}
}
