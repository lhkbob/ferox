package com.ferox.core.renderer;

import java.nio.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

import com.ferox.core.scene.Plane;
import com.ferox.core.scene.SpatialState;
import com.ferox.core.scene.Transform;
import com.ferox.core.scene.View;
import com.ferox.core.states.*;
import com.ferox.core.states.atoms.*;
import com.ferox.core.states.atoms.BufferData.BufferTarget;
import com.ferox.core.states.atoms.BufferData.DataType;
import com.ferox.core.states.atoms.TextureCubeMap.Face;
import com.ferox.core.states.atoms.TextureData.TextureFormat;
import com.ferox.core.states.atoms.TextureData.TextureType;
import com.ferox.core.states.manager.Geometry;
import com.ferox.core.system.RenderSurface;
import com.ferox.core.system.SystemCapabilities;
import com.ferox.core.util.FeroxException;
import com.ferox.core.util.DataTransfer.Block;
import com.ferox.core.util.DataTransfer.Slice;
import com.ferox.core.system.DisplayOptions;

/**
 * RenderContext is an abstract class that provides opengl specific code for rendering a scene.  A new type
 * of RenderContext (and set of StateAtomPeers) must be implemented for a desired GL implementation, such as
 * JOGL or LWJGL.  RenderContext provides a number of state management and record keeping utilities that should
 * not be overridden (unless absolutely necessary).  Besides record keeping, it also provides methods to
 * fetch and set data resident on the graphics card.
 * 
 * @author Michael Ludwig
 *
 */
public abstract class RenderContext {	
	private static class StateAtomTracker {
		private StateAtom[] stateRecord;
	}
	
	private static class DynamicState {
		SpatialState state;
		float influence;
	}
	
	private static class DynamicUnit {
		SpatialState next;
		SpatialState previous;
		StateUnit unit;
	}
	
	private static class DynamicStateTracker {
		private static Comparator<DynamicState> influenceSorter = new Comparator<DynamicState>() {
			public int compare(DynamicState ds1, DynamicState ds2) {
				float diff = ds2.influence - ds1.influence;
				if (diff < 0)
					return -1;
				else if (diff > 0)
					return 1;
				else
					return 0;
			}
		};
		DynamicState[] states;
		DynamicUnit[] units;
		int count;
		
		public DynamicStateTracker(StateUnit[] units) {
			this.states = new DynamicState[units.length];
			this.units = new DynamicUnit[units.length];
			for (int i = 0; i < this.units.length; i++) {
				this.units[i] = new DynamicUnit();
				this.units[i].unit = units[i];
				
				this.states[i] = new DynamicState();
			}
		}
		
		public void add(SpatialState state, float influence) {
			if (this.count + 1 > this.states.length) {
				DynamicState[] temp = new DynamicState[this.states.length + 1];
				System.arraycopy(this.states, 0, temp, 0, this.states.length);
				temp[temp.length - 1] = new DynamicState();
				this.states = temp;
			}
			this.states[this.count].state = state;
			this.states[this.count].influence = influence;
			this.count++;
		}
		
		public void apply(RenderManager manager) {
			if (this.count > this.units.length) 
				Arrays.sort(this.states, 0, this.count, influenceSorter);
			for (int i = 0; i < this.count; i++) {
				for (int u = 0; u < this.units.length; u++) {
					if (this.units[u].previous == this.states[i].state) {
						this.units[u].next = this.states[i].state;
						this.states[i].state = null;
						break;
					}
				}
			}
			for (int i = 0; i < this.count; i++) {
				if (this.states[i].state != null) {
					for (int u = 0; u < this.units.length; u++) {
						if (this.units[u].next == null) {
							this.units[u].next = this.states[i].state;
							this.states[i].state = null;
							break;
						}
					}
				}
			}
			for (int u = 0; u < this.units.length; u++) {
				if (this.units[u].next != null && this.units[u].previous != this.units[u].next)
					this.units[u].next.applyState(manager, this.units[u].unit);
				else if (this.units[u].next == null && this.units[u].previous != null)
					this.units[u].previous.restoreState(manager, this.units[u].unit);
				this.units[u].previous = this.units[u].next;
				this.units[u].next = null;
			}
		}
		
		public void restore(RenderManager manager) {
			for (int u = 0; u < this.units.length; u++) {
				if (this.units[u].previous != null)
					this.units[u].previous.restoreState(manager, this.units[u].unit);
				this.units[u].previous = null;
				this.units[u].next = null;
			}
		}
	}
	
	private static HashMap<Class<? extends StateAtom>, Integer> atomTypes = new HashMap<Class<? extends StateAtom>, Integer>();
	private static int aTypeCounter = 0;
	
	RenderManager manager;
	
	private RenderPassPeer<RenderPass> defaultPass; 
	// TODO: add render to texture render passes
	
	private StateManager[] stateRecord;
	private StateAtomTracker[] atomRecord;
	
	private DynamicStateTracker[] dynamicRecord;
	private RenderAtom currAtom;
	
	public static int registerStateAtomType(Class<? extends StateAtom> m) {
		if (atomTypes.containsKey(m))
			return atomTypes.get(m);
		atomTypes.put(m, aTypeCounter);
		aTypeCounter++;
		return aTypeCounter - 1;
	}
	
	/**
	 * Create a RenderContext with the requested DisplayOptions.  This constructor doesn't actually use options,
	 * it is here to force subclasses to take DisplayOptions as arguments.  Subclasses must do there best to 
	 * satisfy the requested options.  They are also responsible for instantiating the correct implementation of 
	 * a RenderSurface.
	 * @param options
	 */
	public RenderContext(DisplayOptions options) {
		this.defaultPass = this.createDefaultRenderPassPeer();
	}
	
	/**
	 * Get the RenderSurface that rendering goes to
	 */
	public abstract RenderSurface getRenderSurface();
	
	/**
	 * Set the projection matrix and the view matrix so that when pushModelTransform is used, it will
	 * correctly place and project objects onto the screen.
	 */
	public abstract void setProjectionViewTransform(View view);
	/**
	 * Set the viewport to the relative locations, left right top and bottom have the same definition as the
	 * values in View.
	 */
	public abstract void setViewport(float left, float right, float top, float bottom);
	
	/**
	 * Push a world space model transform onto the matrix stack.
	 */
	public abstract void pushModelTransform(Transform trans);
	/**
	 * Pop the last pushed model transform off the matrix stack.  Must leave the projection and view matrices intact.
	 */
	public abstract void popModelTransform();
	
	/**
	 * Render the given geometry with any previously applied state and transform.  It can be assumed that the
	 * Geometry will have any necessary state set and enabled (such as vbos) before the call to this method.
	 */
	public abstract void renderGeometry(Geometry geom);
	
	/**
	 * Set the given custom clip plane, i must be greater than or equal to 0.  It is not recommended to use these
	 * because TransparentAtomBin reserves the right to use planes 0 and 1.  The clip plane will have no effect until
	 * it is enabled.
	 */
	public abstract void setUserClipPlane(Plane plane, int i);
	/**
	 * Enable the given custom clip plane.
	 */
	public abstract void enableUserClipPlane(int i);
	/**
	 * Disable the given custom clip plane.
	 */
	public abstract void disableUserClipPlane(int i);
	
	/**
	 * Get the width, in pixels, of the rendering surface or context/canvas.
	 */
	public abstract int getContextWidth();
	/**
	 * Get the height, in pixels, of the rendering surface or context/canvas.
	 */
	public abstract int getContextHeight();
	
	/**
	 * Clear the buffers of the currently enabled draw buffers.  Only clear a buffer if its boolean is true, if 
	 * cleared the buffer should be cleared to the given value.  If an FBO is attached, it should clear those 
	 * buffers instead of the standard back buffer.
	 */
	public abstract void clearBuffers(boolean color, float[] clearColor, boolean depth, float clearDepth, boolean stencil, int clearStencil);
	
	/**
	 * Get the number of auxiliary buffers present (ie GL_AUXi)
	 */
	public abstract int getNumAuxiliaryBuffers();
	/**
	 * Get the maximum number of draw buffers that are present.
	 */
	public abstract int getMaxDrawBuffers();
	
	/**
	 * Method to be called to signal the beginning of a RenderAtom.  At the moment, it resets the dynamic
	 * spatial state record for use with this atom.  Throws an exception if called inside an existing
	 * beginAtom() endAtom() pair of calls.
	 */
	public void beginAtom(RenderAtom atom) {
		if (this.currAtom != null)
			throw new FeroxException("Can't call beginAtom() after a previous beginAtom() call if endAtom() hasn't been called");
		this.currAtom = atom;
		if (this.dynamicRecord != null)
			for (int i = 0; i < this.dynamicRecord.length; i++)
				if (this.dynamicRecord[i] != null)
					this.dynamicRecord[i].count = 0;
	}
	
	/**
	 * Method to signal the end of a RenderAtom.  At the moment, it applies any influenced spatial states,
	 * pushes the atom's world transform, renders its geometry, and pops the world transform back off.  
	 * The rest of the atom's states should have already been applied by the RenderAtomBin.
	 */
	public void endAtom(RenderAtom atom) {
		if (this.dynamicRecord != null)
			for (int i = 0; i < this.dynamicRecord.length; i++)
				if (this.dynamicRecord[i] != null)
					this.dynamicRecord[i].apply(this.manager);
		this.pushModelTransform(atom.getSpatialLink().getWorldTransform());
		this.renderGeometry(atom.getGeometry());
		this.popModelTransform();
	}
	
	/**
	 * Clear the spatial state record, should be called once at the end of a RenderAtomBin's rendering to reset
	 * it for subsequent passes or frames.
	 */
	public void clearSpatialStates() {
		if (this.dynamicRecord != null)
			for (int i = 0; i < this.dynamicRecord.length; i++)
				if (this.dynamicRecord[i] != null)
					this.dynamicRecord[i].restore(this.manager);
	}
	
	/**
	 * Add the given spatial state to the current RenderAtom.  When rendered, the atom will be under
	 * its influence.
	 */
	public void addSpatialState(SpatialState state) {
		int type = state.getDynamicType();
		if (this.dynamicRecord == null || type >= this.dynamicRecord.length) {
			DynamicStateTracker[] temp = new DynamicStateTracker[type + 1];
			if (this.dynamicRecord != null)
				System.arraycopy(this.dynamicRecord, 0, temp, 0, this.dynamicRecord.length);
			this.dynamicRecord = temp;
		}
		
		DynamicStateTracker record = this.dynamicRecord[type];
		if (record == null) {
			record = new DynamicStateTracker(state.availableUnits());
			this.dynamicRecord[type] = record;
		}
		
		record.add(state, state.getInfluence(this.currAtom.getSpatialLink()));
	}
	
	/**
	 * Get the RenderPassPeer impl for the default render pass.
	 */
	public abstract RenderPassPeer<RenderPass> createDefaultRenderPassPeer();
	//public RenderPassImpl<T> getRenderToTextureImpl();
	
	/**
	 * Get the StateAtomPeer implementation instance for the given class of StateAtom and this context
	 */
	public abstract StateAtomPeer getStateAtomPeer(Class<? extends StateAtom> type);
	/**
	 * Get the StateAtomPeer implementation instance for the given StateAtom, should return the same object
	 * as getStateAtomPeer(atom.getAtomType()).
	 */
	public abstract StateAtomPeer getStateAtomPeer(StateAtom atom);

	public void setTextureRegion(Texture2D data, Block region, int level, Buffer in, Slice slice) {
		validateAll(data.getDataType(), data.getDataFormat(), data.getNumMipmaps(),
					data.getWidth(level), data.getHeight(level), 1, 
					region.getXOffset(), region.getYOffset(), 0, region.getWidth(), region.getHeight(), 1, level,
					in.capacity(), getBufferDataType(in), slice);
		TextureData prev = (TextureData)this.push(data, NumericUnit.get(0), TextureData.class);
		this.setTextureData(data, region, null, level, in, slice);
		this.pop(data, prev, NumericUnit.get(0));
	}
	public void setTextureRegion(Texture2D data, Block region, int level, BufferData in, Slice slice) {
		validateAll(data.getDataType(), data.getDataFormat(), data.getNumMipmaps(),
					data.getWidth(level), data.getHeight(level), 1, 
					region.getXOffset(), region.getYOffset(), 0, region.getWidth(), region.getHeight(), 1, level,
					in.getCapacity(), in.getDataType(), slice);
		TextureData prev = (TextureData)this.push(data, NumericUnit.get(0), TextureData.class);
		BufferData pBD = (BufferData)this.push(in, BufferTarget.PIXEL_WRITE_BUFFER, BufferData.class);
		if (in.isVBO() && RenderManager.getSystemCapabilities().arePixelBuffersSupported())
			this.setTextureData(data, region, null, level, null, slice);
		else if (in.getData() != null)
			this.setTextureData(data, region, null, level, in.getData(), slice);
		this.pop(in, pBD, BufferTarget.PIXEL_WRITE_BUFFER);
		this.pop(data, prev, NumericUnit.get(0));
	}
	
	public void setTextureRegion(Texture3D data, Block region, int level, Buffer in, Slice slice) {
		validateAll(data.getDataType(), data.getDataFormat(), data.getNumMipmaps(),
					data.getWidth(level), data.getHeight(level), data.getDepth(level), 
					region.getXOffset(), region.getYOffset(), region.getZOffset(), region.getWidth(), region.getHeight(), region.getDepth(), level,
					in.capacity(), getBufferDataType(in), slice);
		TextureData prev = (TextureData)this.push(data, NumericUnit.get(0), TextureData.class);
		this.setTextureData(data, region, null, level, in, slice);
		this.pop(data, prev, NumericUnit.get(0));
	}
	public void setTextureRegion(Texture3D data, Block region, int level, BufferData in, Slice slice) {
		validateAll(data.getDataType(), data.getDataFormat(), data.getNumMipmaps(),
					data.getWidth(level), data.getHeight(level), data.getDepth(level), 
					region.getXOffset(), region.getYOffset(), region.getZOffset(), region.getWidth(), region.getHeight(), region.getDepth(), level,
					in.getCapacity(), in.getDataType(), slice);
		TextureData prev = (TextureData)this.push(data, NumericUnit.get(0), TextureData.class);
		BufferData pBD = (BufferData)this.push(in, BufferTarget.PIXEL_WRITE_BUFFER, BufferData.class);
		if (in.isVBO() && RenderManager.getSystemCapabilities().arePixelBuffersSupported())
			this.setTextureData(data, region, null, level, null, slice);
		else
			throw new NullPointerException("Can't set data from a non-vbo buffer data that has a null backing buffer");
		this.pop(in, pBD, BufferTarget.PIXEL_WRITE_BUFFER);
		this.pop(data, prev, NumericUnit.get(0));
	}
	
	public void setTextureRegion(TextureCubeMap data, Block region, Face face, int level, Buffer in, Slice slice) {
		validateAll(data.getDataType(), data.getDataFormat(), data.getNumMipmaps(),
					data.getSideLength(level), data.getSideLength(level), 1, 
					region.getXOffset(), region.getYOffset(), 0, region.getWidth(), region.getHeight(), 1, level,
					in.capacity(), getBufferDataType(in), slice);
		TextureData prev = (TextureData)this.push(data, NumericUnit.get(0), TextureData.class);
		this.setTextureData(data, region, face, level, in, slice);
		this.pop(data, prev, NumericUnit.get(0));
	}
	public void setTextureRegion(TextureCubeMap data, Block region, Face face, int level, BufferData in, Slice slice) {
		validateAll(data.getDataType(), data.getDataFormat(), data.getNumMipmaps(),
					data.getSideLength(level), data.getSideLength(level), 1, 
					region.getXOffset(), region.getYOffset(), 0, region.getWidth(), region.getHeight(), 1, level,
					in.getCapacity(), in.getDataType(), slice);
		TextureData prev = (TextureData)this.push(data, NumericUnit.get(0), TextureData.class);
		BufferData pBD = (BufferData)this.push(in, BufferTarget.PIXEL_WRITE_BUFFER, BufferData.class);
		if (in.isVBO() && RenderManager.getSystemCapabilities().arePixelBuffersSupported())
			this.setTextureData(data, region, face, level, null, slice);
		else if (in.getData() != null)
			this.setTextureData(data, region, face, level, in.getData(), slice);
		this.pop(in, pBD, BufferTarget.PIXEL_WRITE_BUFFER);
		this.pop(data, prev, NumericUnit.get(0));
	}
	
	protected abstract void setTextureData(TextureData data, Block region, Face face, int level, Buffer in, Slice slice);
	
	public void getTexture(Texture2D data, int level, Buffer out, Slice slice) {
		TextureFormat f = getServerCompressedFormat(data.getDataFormat());
		TextureType t = (f.isServerCompressed() ? TextureType.UNSIGNED_BYTE : data.getDataType());
		validateAll(t, f, data.getNumMipmaps(),
					data.getWidth(level), data.getHeight(level), 1,
					0, 0, 0, data.getWidth(level), data.getHeight(level), 1, level,
					out.capacity(), getBufferDataType(out), slice);
		TextureData prev = (TextureData)this.push(data, NumericUnit.get(0), TextureData.class);
		this.getTextureData(data, null, level, out, slice);
		this.pop(data, prev, NumericUnit.get(0));
	}
	public void getTexture(Texture2D data, int level, BufferData out, Slice slice) {
		TextureFormat f = getServerCompressedFormat(data.getDataFormat());
		TextureType t = (f.isServerCompressed() ? TextureType.UNSIGNED_BYTE : data.getDataType());
		validateAll(t, f, data.getNumMipmaps(),
					data.getWidth(level), data.getHeight(level), 1,
					0, 0, 0, data.getWidth(level), data.getHeight(level), 1, level,
					out.getCapacity(), out.getDataType(), slice);
		TextureData prev = (TextureData)this.push(data, NumericUnit.get(0), TextureData.class);
		BufferData pBD = (BufferData)this.push(out, BufferTarget.PIXEL_READ_BUFFER, BufferData.class);
		if (out.isVBO() && RenderManager.getSystemCapabilities().arePixelBuffersSupported())
			this.getTextureData(data, null, level, null, slice);
		else if (out.getData() != null)
			this.getTextureData(data, null, level, out.getData(), slice);
		this.pop(out, pBD, BufferTarget.PIXEL_READ_BUFFER);
		this.pop(data, prev, NumericUnit.get(0));
	}
	
	public void getTexture(Texture3D data, int level, Buffer out, Slice slice) {
		TextureFormat f = getServerCompressedFormat(data.getDataFormat());
		TextureType t = (f.isServerCompressed() ? TextureType.UNSIGNED_BYTE : data.getDataType());
		validateAll(t, f, data.getNumMipmaps(),
					data.getWidth(level), data.getHeight(level), data.getDepth(level),
					0, 0, 0, data.getWidth(level), data.getHeight(level), data.getDepth(level), level,
					out.capacity(), getBufferDataType(out), slice);
		TextureData prev = (TextureData)this.push(data, NumericUnit.get(0), TextureData.class);
		this.getTextureData(data, null, level, out, slice);
		this.pop(data, prev, NumericUnit.get(0));
	}
	public void getTexture(Texture3D data, int level, BufferData out, Slice slice) {
		TextureFormat f = getServerCompressedFormat(data.getDataFormat());
		TextureType t = (f.isServerCompressed() ? TextureType.UNSIGNED_BYTE : data.getDataType());
		validateAll(t, f, data.getNumMipmaps(),
					data.getWidth(level), data.getHeight(level), data.getDepth(level),
					0, 0, 0, data.getWidth(level), data.getHeight(level), data.getDepth(level), level,
					out.getCapacity(), out.getDataType(), slice);
		TextureData prev = (TextureData)this.push(data, NumericUnit.get(0), TextureData.class);
		BufferData pBD = (BufferData)this.push(out, BufferTarget.PIXEL_READ_BUFFER, BufferData.class);
		if (out.isVBO() && RenderManager.getSystemCapabilities().arePixelBuffersSupported())
			this.getTextureData(data, null, level, null, slice);
		else if (out.getData() != null)
			this.getTextureData(data, null, level, out.getData(), slice);
		this.pop(out, pBD, BufferTarget.PIXEL_READ_BUFFER);
		this.pop(data, prev, NumericUnit.get(0));
	}
	public void getTexture(TextureCubeMap data, Face face, int level, Buffer out, Slice slice) {
		TextureFormat f = getServerCompressedFormat(data.getDataFormat());
		TextureType t = (f.isServerCompressed() ? TextureType.UNSIGNED_BYTE : data.getDataType());
		validateAll(t, f, data.getNumMipmaps(),
					data.getSideLength(level), data.getSideLength(level), 1,
					0, 0, 0, data.getSideLength(level), data.getSideLength(level), 1, level,
					out.capacity(), getBufferDataType(out), slice);
		TextureData prev = (TextureData)this.push(data, NumericUnit.get(0), TextureData.class);
		this.getTextureData(data, face, level, out, slice);
		this.pop(data, prev, NumericUnit.get(0));
	}
	public void getTexture(TextureCubeMap data, Face face, int level, BufferData out, Slice slice) {
		TextureFormat f = getServerCompressedFormat(data.getDataFormat());
		TextureType t = (f.isServerCompressed() ? TextureType.UNSIGNED_BYTE : data.getDataType());
		validateAll(t, f, data.getNumMipmaps(),
					data.getSideLength(level), data.getSideLength(level), 1,
					0, 0, 0, data.getSideLength(level), data.getSideLength(level), 1, level,
					out.getCapacity(), out.getDataType(), slice);
		TextureData prev = (TextureData)this.push(data, NumericUnit.get(0), TextureData.class);
		BufferData pBD = (BufferData)this.push(out, BufferTarget.PIXEL_READ_BUFFER, BufferData.class);
		if (out.isVBO() && RenderManager.getSystemCapabilities().arePixelBuffersSupported())
			this.getTextureData(data, face, level, null, slice);
		else if (out.getData() != null)
			this.getTextureData(data, face, level, out.getData(), slice);
		this.pop(out, pBD, BufferTarget.PIXEL_READ_BUFFER);
		this.pop(data, prev, NumericUnit.get(0));
	}
	
	protected abstract void getTextureData(TextureData data, Face face, int level, Buffer out, Slice slice);
	
	public void copyFramePixels(Texture2D data, Block region, int level, int sx, int sy) {
		if (data.getDataFormat().isServerCompressed())
			throw new IllegalArgumentException("copyFramePixels doesn't support compressed textures");
		validateMipmap(level, data.getNumMipmaps());
		validateRegion(region.getXOffset(), region.getYOffset(), 0, region.getWidth(), region.getHeight(), 1, data.getWidth(level), data.getHeight(level), 1);
		validateRegion(sx, sy, 0, region.getWidth(), region.getHeight(), 1, this.getContextWidth(), this.getContextHeight(), 1);
		TextureData prev = (TextureData)this.push(data, NumericUnit.get(0), TextureData.class);
		this.copyTextureData(data, region, null, level, sx, sy);
		this.pop(data, prev, NumericUnit.get(0));
	}
	
	public void copyFramePixels(Texture3D data, Block region, int level, int sx, int sy) {
		if (data.getDataFormat().isServerCompressed())
			throw new IllegalArgumentException("copyFramePixels doesn't support compressed textures");
		validateMipmap(level, data.getNumMipmaps());
		validateRegion(region.getXOffset(), region.getYOffset(), region.getZOffset(), region.getWidth(), region.getHeight(), 1, data.getWidth(level), data.getHeight(level), data.getDepth(level));
		validateRegion(sx, sy, 0, region.getWidth(), region.getHeight(), 1, this.getContextWidth(), this.getContextHeight(), 1);
		TextureData prev = (TextureData)this.push(data, NumericUnit.get(0), TextureData.class);
		this.copyTextureData(data, region, null, level, sx, sy);
		this.pop(data, prev, NumericUnit.get(0));
	}
	
	public void copyFramePixels(TextureCubeMap data, Block region, int level, int sx, int sy) {
		if (data.getDataFormat().isServerCompressed())
			throw new IllegalArgumentException("copyFramePixels doesn't support compressed textures");
		validateMipmap(level, data.getNumMipmaps());
		validateRegion(region.getXOffset(), region.getYOffset(), 0, region.getWidth(), region.getHeight(), 1, data.getSideLength(level), data.getSideLength(level), 0);
		validateRegion(sx, sy, 0, region.getWidth(), region.getHeight(), 1, this.getContextWidth(), this.getContextHeight(), 1);
		TextureData prev = (TextureData)this.push(data, NumericUnit.get(0), TextureData.class);
		this.copyTextureData(data, region, null, level, sx, sy);
		this.pop(data, prev, NumericUnit.get(0));
	}
	
	protected abstract void copyTextureData(TextureData data, Block region, Face face, int level, int sx, int sy);
	
	public void readFramePixels(Buffer in, Slice slice, TextureType type, TextureFormat format, Block region) {
		if (!format.isTypeCompatible(type) || format.isServerCompressed())
			throw new IllegalArgumentException("Invalid texture type and format");
		validateTypePrimitive(type, getBufferDataType(in));
		validateRegion(region.getXOffset(), region.getYOffset(), 0, region.getWidth(), region.getHeight(), 1, this.getContextWidth(), this.getContextHeight(), 1);
		validateBuffer(format.getBufferSize(type, region.getWidth(), region.getHeight()), in.capacity(), slice);
		
		this.readPixels(in, slice, type, format, region);
	}
	public void readFramePixels(BufferData in, Slice slice, TextureType type, TextureFormat format, Block region) {
		if (!format.isTypeCompatible(type) || format.isServerCompressed())
			throw new IllegalArgumentException("Invalid texture type and format");
		validateTypePrimitive(type, in.getDataType());
		validateRegion(region.getXOffset(), region.getYOffset(), 0, region.getWidth(), region.getHeight(), 1, this.getContextWidth(), this.getContextHeight(), 1);
		validateBuffer(format.getBufferSize(type, region.getWidth(), region.getHeight()), in.getCapacity(), slice);
		
		BufferData pBD = (BufferData)this.push(in, BufferTarget.PIXEL_READ_BUFFER, BufferData.class);
		if (in.isVBO() && RenderManager.getSystemCapabilities().arePixelBuffersSupported())
			this.readPixels(null, slice, type, format, region);
		else if (in.getData() != null)
			this.readPixels(in.getData(), slice, type, format, region);
		this.pop(in, pBD, BufferTarget.PIXEL_READ_BUFFER);
	}
	
	protected abstract void readPixels(Buffer in, Slice slice, TextureType type, TextureFormat format, Block region);
	
	private static TextureFormat getServerCompressedFormat(TextureFormat f) {
		switch(f) {
		case RGB_DXT1:
			return TextureFormat.COMPRESSED_RGB_DXT1;
		case RGBA_DXT1: case BGRA_DXT1:
			return TextureFormat.COMPRESSED_RGBA_DXT1;
		case RGBA_DXT3:
			return TextureFormat.COMPRESSED_RGBA_DXT3;
		case RGBA_DXT5: case BGRA_DXT5:
			return TextureFormat.COMPRESSED_RGBA_DXT5;
		}
		return f;
	}
	
	private StateAtom push(StateAtom atom, StateUnit unit, Class<? extends StateAtom> type) {
		StateAtom prev = this.getActiveStateAtom(type, unit);
		atom.applyState(this.getRenderManager(), NumericUnit.get(0));
		return prev;
	}
	
	private void pop(StateAtom atom, StateAtom prev, StateUnit unit) {
		if (prev != null)
			prev.applyState(this.getRenderManager(), unit);
		else
			atom.restoreState(this.getRenderManager(), unit);
	}
	
	private static void validateAll(TextureType texType, TextureFormat format, int numMips, int width, int height, int depth, 
								    int sx, int sy, int sz, int sw, int sh, int sd, int level, 
								    int capacity, DataType bType, Slice slice) {
		validateTypePrimitive(texType, bType);
		validateMipmap(level, numMips);
		validateRegion(sx, sy, sz, sw, sh, sd, width, height, depth);
		validateBuffer(format.getBufferSize(texType, width, height, depth), capacity, slice);
	}
	
	private static void validateRegion(int sx, int sy, int sz, int sw, int sh, int sd, int width, int height, int depth) {
		if (sx < 0 || sy < 0 || sz < 0)
			throw new IllegalArgumentException("region must have positive offsets");
		if (sw < 0 || sh < 0 || sd < 0 || width < 0 || height < 0 || depth < 0)
			throw new IllegalArgumentException("region must have positive dimensions");
		if ((sx + sw > width) || (sy + sh) > height || (sz + sd) > depth)
			throw new IllegalArgumentException("region extends beyond texture dimensions");
	}
	
	private static void validateBuffer(int correctSize, int capacity, Slice slice) {
		if (slice.getOffset() < 0 || slice.getOffset() + slice.getLength() > capacity)
			throw new IllegalArgumentException("Slice extends beyond the buffer limits: (" + slice.getOffset() + " - " + (slice.getOffset() + slice.getLength()) + ")");
		if (slice.getLength() != correctSize)
			throw new IllegalArgumentException("Buffer slice doesn't have the correct capacity. Provided " + slice.getLength() + ", required " + correctSize);
	}
	
	private static DataType getBufferDataType(Buffer in) {
		if (in instanceof FloatBuffer)
			return DataType.FLOAT;
		else if (in instanceof DoubleBuffer)
			return DataType.DOUBLE;
		else if (in instanceof ByteBuffer)
			return DataType.BYTE;
		else if (in instanceof IntBuffer)
			return DataType.INT;
		else if (in instanceof ShortBuffer)
			return DataType.SHORT;
		throw new IllegalArgumentException("Invalid buffer primitive type");
	}
	
	private static void validateTypePrimitive(TextureType type, DataType in) {
		boolean valid;
		switch(in) {
		case FLOAT:
			valid = (type == TextureType.FLOAT);
			break;
		case BYTE: case UNSIGNED_BYTE:
			valid = (type == TextureType.UNSIGNED_BYTE);
			break;
		case INT: case UNSIGNED_INT:
			valid = (type == TextureType.UNSIGNED_INT || type == TextureType.PACKED_INT_8888);
			break;
		case SHORT: case UNSIGNED_SHORT:
			valid = (type == TextureType.UNSIGNED_SHORT || type == TextureType.PACKED_SHORT_4444
					 || type == TextureType.PACKED_SHORT_5551 || type == TextureType.PACKED_SHORT_565);
			break;
		default:
			valid = false;
			break;
		}
		
		if (!valid)
			throw new IllegalArgumentException("Incompatible buffer primitive type");
	}
	
	private static void validateMipmap(int level, int provided) {
		if (level < 0 || level >= provided)
			throw new IllegalArgumentException("Invalid mipmap level for the given texture");
	}
	
	/**
	 * Get the current version of this context.  This number must be >= 0, and increase each time the context
	 * is internally re-initialized.
	 */
	public abstract int getContextVersion();
	
	/**
	 * Get the SystemCapabilities of this context, can't return null if isInitialized() returns true.
	 */
	public abstract SystemCapabilities getCapabilities();
	/**
	 * Destroy the internal context resources for this context.
	 */
	public abstract void destroyContext();
	/**
	 * Called by RenderManager to tell the context to begin the process of rendering.  When appropriate, the context
	 * must then call notifyRenderFrame() on this context's manager.
	 */
	public abstract void render();
	/**
	 * Whether or not this RenderContext is current on the calling thread, i.e. that its appropriate to call internal
	 * resources or gl functions.
	 */
	public abstract boolean isCurrent();
	public abstract boolean isInitialized();
	
	public void setActiveStateManager(StateManager man, Class<? extends StateAtom> type) {
		if (type == null)
			throw new NullPointerException("Can't have a null type");
		int index = (man != null ? man.getDynamicType() : registerStateAtomType(type));
		this.setActiveStateManager(man, index);
	}
	
	public void setActiveStateManager(StateManager man, int type) {
		if (this.stateRecord == null)
			this.stateRecord = new StateManager[StateManager.NUM_CORE_STATES];
		if (this.stateRecord.length <= type) {
			StateManager[] temp = new StateManager[type + 1];
			System.arraycopy(this.stateRecord, 0, temp, 0, this.stateRecord.length);
			this.stateRecord = temp;
		}
		this.stateRecord[type] = man;
	}
	
	public StateManager getActiveStateManager(Class<? extends StateAtom> type) {
		if (type == null)
			throw new NullPointerException("Can't have a null type");
		int index = registerStateAtomType(type);
		return this.getActiveStateManager(index);
	}
	
	public StateManager getActiveStateManager(int dynamicType) {
		if (this.stateRecord == null || dynamicType >= this.stateRecord.length)
			return null;
		return this.stateRecord[dynamicType];
	}
	
	public void setActiveStateAtom(StateAtom atom, Class<? extends StateAtom> type, StateUnit unit) {
		if (type == null)
			throw new NullPointerException("Can't have a null type");
		if (unit == null)
			throw new NullPointerException("Can't have a null unit");
		
		this.setActiveStateAtom(atom, registerStateAtomType(type), unit.ordinal());
	}
	
	public void setActiveStateAtom(StateAtom atom, int type, int ordinal) {
		if (this.atomRecord == null)
			this.atomRecord = new StateAtomTracker[StateManager.NUM_CORE_STATES];
		if (this.atomRecord.length <= type) {
			StateAtomTracker[] temp = new StateAtomTracker[type + 1];
			System.arraycopy(this.atomRecord, 0, temp, 0, this.atomRecord.length);
			this.atomRecord = temp;
		}
		
		StateAtomTracker st = this.atomRecord[type];
		if (st == null) {
			st = new StateAtomTracker();
			this.atomRecord[type] = st;
		}
		if (st.stateRecord == null || st.stateRecord.length <= ordinal) {
			StateAtom[] temp = new StateAtom[ordinal + 1];
			if (st.stateRecord != null)
				System.arraycopy(st.stateRecord, 0, temp, 0, st.stateRecord.length);
			st.stateRecord = temp;
		}
		st.stateRecord[ordinal] = atom;
	}
	
	public StateAtom getActiveStateAtom(Class<? extends StateAtom> type, StateUnit unit) {
		if (type == null)
			throw new NullPointerException("Can't have a null type");
		if (unit == null)
			throw new NullPointerException("Can't have a null unit");
			
		return this.getActiveStateAtom(registerStateAtomType(type), unit.ordinal());
	}
	
	public StateAtom getActiveStateAtom(int dynamicType, int ordinal) {
		if (this.atomRecord == null || this.atomRecord.length <= dynamicType || this.atomRecord[dynamicType] == null || this.atomRecord[dynamicType].stateRecord.length <= ordinal)
			return null;
		return this.atomRecord[dynamicType].stateRecord[ordinal];
	}
	
	public RenderManager getRenderManager() {
		return this.manager;
	}

	public RenderPassPeer<RenderPass> getDefaultRenderPassPeer() {
		return this.defaultPass;
	}
	
	protected void callValidate() {
		if (!this.isCurrent())
			throw new FeroxException("Method unavailable when RenderContext isn't current");
	}
}
