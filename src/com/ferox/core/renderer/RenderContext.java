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
import com.ferox.core.states.atoms.TextureData.TextureCompression;
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
	
	/**
	 * Register the given type of atom and return the dynamic type.  All state atoms and state managers
	 * call this in their constructor.  If an atom type has already been registered, it returns the previously
	 * assigned value.
	 */
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
	public void beginAtom(RenderAtom atom) throws NullPointerException, FeroxException {
		if (this.currAtom != null)
			throw new FeroxException("Can't call beginAtom() after a previous beginAtom() call if endAtom() hasn't been called");
		if (atom == null)
			throw new NullPointerException("Can't call beginAtom() with a null atom");
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
		if (atom != this.currAtom || this.currAtom == null)
			throw new FeroxException("Can't call endAtom() with a different RenderAtom or not after beginAtom()");
		if (this.dynamicRecord != null)
			for (int i = 0; i < this.dynamicRecord.length; i++)
				if (this.dynamicRecord[i] != null)
					this.dynamicRecord[i].apply(this.manager);
		this.pushModelTransform(atom.getSpatialLink().getWorldTransform());
		this.renderGeometry(atom.getGeometry());
		this.popModelTransform();
		this.currAtom = null;
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

	/**
	 * Update the texture data on the graphics card for the given texture and region, using the given slice of 
	 * the buffer in as source data.  The region must be contained completely within the actual textures dimensions
	 * at the given mipmap level (level >= 0).  It will throw an exception if the region is too large, if the 
	 * mipmap level doesn't exist in the texture, or if the slice extends beyond the buffer's capacity, or if 
	 * the slice isn't sized to fit the entire texture.  The buffer to update must be of the same format and type
	 * of the texture (which might be compressed data).  Because of some driver issues, it may be necessary to 
	 * reallocate the texture on the graphics card, which is why the source buffer must be sized for the texture.
	 * It is then recommended to modify the in-memory nio buffer of the texture (in the given region) and then call
	 * this method with that buffer, so that in the event of a reallocation instead of an update, you will not
	 * lose your texture image.
	 * 
	 * The depth and z offset values of the region are ignored.
	 */
	public void setTextureRegion(Texture2D data, Block region, int level, Buffer in, Slice slice) throws IllegalArgumentException {
		validateAll(data.getDataType(), data.getDataFormat(), data.getNumMipmaps(),
					data.getWidth(level), data.getHeight(level), 1, 
					region.getXOffset(), region.getYOffset(), 0, region.getWidth(), region.getHeight(), 1, level,
					in.capacity(), getBufferDataType(in), slice);
		TextureData prev = (TextureData)this.push(data, NumericUnit.get(0), TextureData.class);
		this.setTextureData(data, region, null, level, in, slice);
		this.pop(data, prev, NumericUnit.get(0));
	}
	/**
	 * Equivalent to setTextureRegion(Texture2D, ... Buffer) but uses the BufferData as a source.  If the source BufferData
	 * is not a vbo or if pixel buffer objects aren't supported, it is identical to the previous method (using the returned
	 * nio buffer), otherwise it uses pixel buffers (not pbuffers) to use data transfer directly on the graphics
	 * card (using the graphics card vbo data as a source).
	 */
	public void setTextureRegion(Texture2D data, Block region, int level, BufferData in, Slice slice) throws IllegalArgumentException {
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
	
	/**
	 * As before with a Texture2D argument, but the depth and z offset values must be valid.
	 */
	public void setTextureRegion(Texture3D data, Block region, int level, Buffer in, Slice slice) throws IllegalArgumentException {
		validateAll(data.getDataType(), data.getDataFormat(), data.getNumMipmaps(),
					data.getWidth(level), data.getHeight(level), data.getDepth(level), 
					region.getXOffset(), region.getYOffset(), region.getZOffset(), region.getWidth(), region.getHeight(), region.getDepth(), level,
					in.capacity(), getBufferDataType(in), slice);
		TextureData prev = (TextureData)this.push(data, NumericUnit.get(0), TextureData.class);
		this.setTextureData(data, region, null, level, in, slice);
		this.pop(data, prev, NumericUnit.get(0));
	}
	/**
	 * See the Texture3D version with a Buffer and the Texture2D version with a BufferData.
	 */
	public void setTextureRegion(Texture3D data, Block region, int level, BufferData in, Slice slice) throws IllegalArgumentException {
		validateAll(data.getDataType(), data.getDataFormat(), data.getNumMipmaps(),
					data.getWidth(level), data.getHeight(level), data.getDepth(level), 
					region.getXOffset(), region.getYOffset(), region.getZOffset(), region.getWidth(), region.getHeight(), region.getDepth(), level,
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
	
	/**
	 * Just like the Texture2D call except that it takes a Face value to choose which face of the cube map to 
	 * update.  Validation then works on the size of that 2D texture face.
	 */
	public void setTextureRegion(TextureCubeMap data, Block region, Face face, int level, Buffer in, Slice slice) throws IllegalArgumentException {
		validateAll(data.getDataType(), data.getDataFormat(), data.getNumMipmaps(),
					data.getSideLength(level), data.getSideLength(level), 1, 
					region.getXOffset(), region.getYOffset(), 0, region.getWidth(), region.getHeight(), 1, level,
					in.capacity(), getBufferDataType(in), slice);
		TextureData prev = (TextureData)this.push(data, NumericUnit.get(0), TextureData.class);
		this.setTextureData(data, region, face, level, in, slice);
		this.pop(data, prev, NumericUnit.get(0));
	}
	/**
	 * See previous setTextureRegion docs.
	 */
	public void setTextureRegion(TextureCubeMap data, Block region, Face face, int level, BufferData in, Slice slice) throws IllegalArgumentException {
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
	
	/**
	 * Implementations of a RenderContext must implement this method to set given region of the texture data and mipmap level.  Face is only 
	 * non-null when a cube map is involved.  If the Buffer in is null, then the transfer should use pixel buffers to transfer the 
	 * data.  Both the texture and possibly the pixel buffer will already have been applied to the 0th unit.
	 */
	protected abstract void setTextureData(TextureData data, Block region, Face face, int level, Buffer in, Slice slice);
	
	/**
	 * Fetch the texture data from the graphics card into the given buffer for the given level.  It fetches
	 * the entire image data, so the slice of the buffer must be fitted to the texture.  And, of course, the
	 * slice must be contained within the entire buffer's capacity.  The fetched texture will be in the same
	 * original texture format and type.
	 */
	public void getTexture(Texture2D data, int level, Buffer out, Slice slice) throws IllegalArgumentException {
		TextureFormat f = getServerCompressedFormat(data.getDataFormat(), data.getDataCompression());
		TextureType t = (TextureData.isServerCompressed(f, data.getDataCompression()) ? TextureType.UNSIGNED_BYTE : data.getDataType());
		validateAll(t, f, data.getNumMipmaps(),
					data.getWidth(level), data.getHeight(level), 1,
					0, 0, 0, data.getWidth(level), data.getHeight(level), 1, level,
					out.capacity(), getBufferDataType(out), slice);
		TextureData prev = (TextureData)this.push(data, NumericUnit.get(0), TextureData.class);
		this.getTextureData(data, null, level, out, slice);
		this.pop(data, prev, NumericUnit.get(0));
	}
	/**
	 * Same as getTexture() but stores the texture data into the buffer data.  If isVBO is true for BufferData
	 * it will store it on the graphics card.  If pixel buffers are available, there will be a direct transfer
	 * and the in-memory data will not be updated.  If isVBO is true, and pixel buffers aren't supported, and
	 * the client buffer has been set to null, then this will be a no-op.
	 */
	public void getTexture(Texture2D data, int level, BufferData out, Slice slice) throws IllegalArgumentException {
		TextureFormat f = getServerCompressedFormat(data.getDataFormat(), data.getDataCompression());
		TextureType t = (TextureData.isServerCompressed(f, data.getDataCompression()) ? TextureType.UNSIGNED_BYTE : data.getDataType());
		validateAll(t, f, data.getNumMipmaps(),
					data.getWidth(level), data.getHeight(level), 1,
					0, 0, 0, data.getWidth(level), data.getHeight(level), 1, level,
					out.getCapacity(), out.getDataType(), slice);
		TextureData prev = (TextureData)this.push(data, NumericUnit.get(0), TextureData.class);
		BufferData pBD = (BufferData)this.push(out, BufferTarget.PIXEL_READ_BUFFER, BufferData.class);
		if (out.isVBO() && RenderManager.getSystemCapabilities().arePixelBuffersSupported())
			this.getTextureData(data, null, level, null, slice);
		else if (out.getData() != null) {
			this.getTextureData(data, null, level, out.getData(), slice);
			out.update(this.getRenderManager());
		}
		this.pop(out, pBD, BufferTarget.PIXEL_READ_BUFFER);
		this.pop(data, prev, NumericUnit.get(0));
	}
	
	/**
	 * As getTexture(Textur2D ...)
	 */
	public void getTexture(Texture3D data, int level, Buffer out, Slice slice) throws IllegalArgumentException {
		TextureFormat f = getServerCompressedFormat(data.getDataFormat(), data.getDataCompression());
		TextureType t = (TextureData.isServerCompressed(f, data.getDataCompression()) ? TextureType.UNSIGNED_BYTE : data.getDataType());
		validateAll(t, f, data.getNumMipmaps(),
					data.getWidth(level), data.getHeight(level), data.getDepth(level),
					0, 0, 0, data.getWidth(level), data.getHeight(level), data.getDepth(level), level,
					out.capacity(), getBufferDataType(out), slice);
		TextureData prev = (TextureData)this.push(data, NumericUnit.get(0), TextureData.class);
		this.getTextureData(data, null, level, out, slice);
		this.pop(data, prev, NumericUnit.get(0));
	}
	/**
	 * As getTexture(Texture2D ...)
	 */
	public void getTexture(Texture3D data, int level, BufferData out, Slice slice) throws IllegalArgumentException {
		TextureFormat f = getServerCompressedFormat(data.getDataFormat(), data.getDataCompression());
		TextureType t = (TextureData.isServerCompressed(f, data.getDataCompression()) ? TextureType.UNSIGNED_BYTE : data.getDataType());
		validateAll(t, f, data.getNumMipmaps(),
					data.getWidth(level), data.getHeight(level), data.getDepth(level),
					0, 0, 0, data.getWidth(level), data.getHeight(level), data.getDepth(level), level,
					out.getCapacity(), out.getDataType(), slice);
		TextureData prev = (TextureData)this.push(data, NumericUnit.get(0), TextureData.class);
		BufferData pBD = (BufferData)this.push(out, BufferTarget.PIXEL_READ_BUFFER, BufferData.class);
		if (out.isVBO() && RenderManager.getSystemCapabilities().arePixelBuffersSupported())
			this.getTextureData(data, null, level, null, slice);
		else if (out.getData() != null) {
			this.getTextureData(data, null, level, out.getData(), slice);
			out.update(this.getRenderManager());
		}
		this.pop(out, pBD, BufferTarget.PIXEL_READ_BUFFER);
		this.pop(data, prev, NumericUnit.get(0));
	}
	/**
	 * As getTexture(Textur2D ...) but you pick a cube map face to fetch.
	 */
	public void getTexture(TextureCubeMap data, Face face, int level, Buffer out, Slice slice) throws IllegalArgumentException {
		TextureFormat f = getServerCompressedFormat(data.getDataFormat(), data.getDataCompression());
		TextureType t = (TextureData.isServerCompressed(f, data.getDataCompression()) ? TextureType.UNSIGNED_BYTE : data.getDataType());
		validateAll(t, f, data.getNumMipmaps(),
					data.getSideLength(level), data.getSideLength(level), 1,
					0, 0, 0, data.getSideLength(level), data.getSideLength(level), 1, level,
					out.capacity(), getBufferDataType(out), slice);
		TextureData prev = (TextureData)this.push(data, NumericUnit.get(0), TextureData.class);
		this.getTextureData(data, face, level, out, slice);
		this.pop(data, prev, NumericUnit.get(0));
	}
	/**
	 * As getTexture(Textur2D ...) but you pick a cube map face to fetch.
	 */
	public void getTexture(TextureCubeMap data, Face face, int level, BufferData out, Slice slice) throws IllegalArgumentException {
		TextureFormat f = getServerCompressedFormat(data.getDataFormat(), data.getDataCompression());
		TextureType t = (TextureData.isServerCompressed(f, data.getDataCompression()) ? TextureType.UNSIGNED_BYTE : data.getDataType());
		validateAll(t, f, data.getNumMipmaps(),
					data.getSideLength(level), data.getSideLength(level), 1,
					0, 0, 0, data.getSideLength(level), data.getSideLength(level), 1, level,
					out.getCapacity(), out.getDataType(), slice);
		TextureData prev = (TextureData)this.push(data, NumericUnit.get(0), TextureData.class);
		BufferData pBD = (BufferData)this.push(out, BufferTarget.PIXEL_READ_BUFFER, BufferData.class);
		if (out.isVBO() && RenderManager.getSystemCapabilities().arePixelBuffersSupported())
			this.getTextureData(data, face, level, null, slice);
		else if (out.getData() != null) {
			this.getTextureData(data, face, level, out.getData(), slice);
			out.update(this.getRenderManager());
		}
		this.pop(out, pBD, BufferTarget.PIXEL_READ_BUFFER);
		this.pop(data, prev, NumericUnit.get(0));
	}
	
	/**
	 * Implementations of RenderContext must fetch the data of the texture from the graphics card (and face
	 * if the texture is a cube map) at the given mipmap level into the buffer.  If the buffer is null,
	 * it should do a pixel buffer transfer.  The texture data and buffer are already applied.
	 */
	protected abstract void getTextureData(TextureData data, Face face, int level, Buffer out, Slice slice);
	
	/**
	 * Copy the current contents of the active drawing buffer into the texture, given the region.  The region
	 * must be contained within the whole texture's dimensions.  Fails if the region, offset by the screen
	 * fetch coordinates (sx, sy) goes off screen.  This is an operation on the graphics card, no memory
	 * is transfered back to the in-memory buffer of the texture.
	 */
	public void copyFramePixels(Texture2D data, Block region, int level, int sx, int sy) throws IllegalArgumentException {
		if (TextureData.isServerCompressed(data.getDataFormat(), data.getDataCompression()))
			throw new IllegalArgumentException("copyFramePixels doesn't support compressed textures");
		validateMipmap(level, data.getNumMipmaps());
		validateRegion(region.getXOffset(), region.getYOffset(), 0, region.getWidth(), region.getHeight(), 1, data.getWidth(level), data.getHeight(level), 1);
		validateRegion(sx, sy, 0, region.getWidth(), region.getHeight(), 1, this.getContextWidth(), this.getContextHeight(), 1);
		TextureData prev = (TextureData)this.push(data, NumericUnit.get(0), TextureData.class);
		this.copyTextureData(data, region, null, level, sx, sy);
		this.pop(data, prev, NumericUnit.get(0));
	}
	
	/**
	 * As copyFramePixels(Texture2D ...) except that in essence it copies the framebuffer into a slice of
	 * the 3D texture (as specified by the region).  The depth value is ignored, and is set to 1 for the region.
	 */
	public void copyFramePixels(Texture3D data, Block region, int level, int sx, int sy) throws IllegalArgumentException {
		if (TextureData.isServerCompressed(data.getDataFormat(), data.getDataCompression()))
			throw new IllegalArgumentException("copyFramePixels doesn't support compressed textures");
		validateMipmap(level, data.getNumMipmaps());
		validateRegion(region.getXOffset(), region.getYOffset(), region.getZOffset(), region.getWidth(), region.getHeight(), 1, data.getWidth(level), data.getHeight(level), data.getDepth(level));
		validateRegion(sx, sy, 0, region.getWidth(), region.getHeight(), 1, this.getContextWidth(), this.getContextHeight(), 1);
		TextureData prev = (TextureData)this.push(data, NumericUnit.get(0), TextureData.class);
		this.copyTextureData(data, region, null, level, sx, sy);
		this.pop(data, prev, NumericUnit.get(0));
	}
	
	/**
	 * As copyFramePixels(Texture2D ...) except that the face argument chooses one of the 2d faces of the cube
	 * map to update
	 */
	public void copyFramePixels(TextureCubeMap data, Block region, Face face, int level, int sx, int sy) throws IllegalArgumentException {
		if (TextureData.isServerCompressed(data.getDataFormat(), data.getDataCompression()))
			throw new IllegalArgumentException("copyFramePixels doesn't support compressed textures");
		validateMipmap(level, data.getNumMipmaps());
		validateRegion(region.getXOffset(), region.getYOffset(), 0, region.getWidth(), region.getHeight(), 1, data.getSideLength(level), data.getSideLength(level), 0);
		validateRegion(sx, sy, 0, region.getWidth(), region.getHeight(), 1, this.getContextWidth(), this.getContextHeight(), 1);
		TextureData prev = (TextureData)this.push(data, NumericUnit.get(0), TextureData.class);
		this.copyTextureData(data, region, face, level, sx, sy);
		this.pop(data, prev, NumericUnit.get(0));
	}
	
	/**
	 * Implementations of RenderContext should override this to perform an actual copy operation from the active
	 * drawing buffer to the given texture.  It should update region in the texture at the mipmap level, using the frame pixels
	 * starting at (sx, sy).  Face is only valid/non-null when data is a cube map.
	 */
	protected abstract void copyTextureData(TextureData data, Block region, Face face, int level, int sx, int sy);
	
	/**
	 * Read the current state of the frame buffer into the given buffer.  The pixels are grabbed from the 2D
	 * region specified by region (depth and zoffset are ignored).  The pixel data is converted to the 
	 * given type and format.  The in buffer's slice must be contained by the buffer and have the correct size
	 * and type for an equivalently formatted texture.
	 */
	public void readFramePixels(Buffer in, Slice slice, TextureType type, TextureFormat format, Block region) throws IllegalArgumentException {
		if (!format.isTypeCompatible(type) || format.isClientCompressed())
			throw new IllegalArgumentException("Invalid texture type and format");
		validateTypePrimitive(type, getBufferDataType(in));
		validateRegion(region.getXOffset(), region.getYOffset(), 0, region.getWidth(), region.getHeight(), 1, this.getContextWidth(), this.getContextHeight(), 1);
		validateBuffer(format.getBufferSize(type, region.getWidth(), region.getHeight()), in.capacity(), slice);
		
		this.readPixels(in, slice, type, format, region);
	}
	/**
	 * As readFramePixels(Buffer ...) except that if the BufferData is a vbo and pixel buffers are supported, then
	 * it does a transfer completely on the graphics card (much faster).  Otherwise, it reads the pixel data into
	 * the BufferData's backing buffer (if not null) and then updates the BufferData if necessary.
	 */
	public void readFramePixels(BufferData in, Slice slice, TextureType type, TextureFormat format, Block region) throws IllegalArgumentException {
		if (!format.isTypeCompatible(type) || format.isClientCompressed())
			throw new IllegalArgumentException("Invalid texture type and format");
		validateTypePrimitive(type, in.getDataType());
		validateRegion(region.getXOffset(), region.getYOffset(), 0, region.getWidth(), region.getHeight(), 1, this.getContextWidth(), this.getContextHeight(), 1);
		validateBuffer(format.getBufferSize(type, region.getWidth(), region.getHeight()), in.getCapacity(), slice);
		
		BufferData pBD = (BufferData)this.push(in, BufferTarget.PIXEL_READ_BUFFER, BufferData.class);
		if (in.isVBO() && RenderManager.getSystemCapabilities().arePixelBuffersSupported())
			this.readPixels(null, slice, type, format, region);
		else if (in.getData() != null) {
			this.readPixels(in.getData(), slice, type, format, region);
			in.update(this.getRenderManager());
		}
		this.pop(in, pBD, BufferTarget.PIXEL_READ_BUFFER);
	}
	
	/**
	 * Implementations of RenderContext must implement this method to read pixels (from the region) into the given buffer, and have
	 * the internal context format it given the type and format.  If buffer is null, then use the already bound
	 * pixel buffer (but it still must use the slice).
	 */
	protected abstract void readPixels(Buffer in, Slice slice, TextureType type, TextureFormat format, Block region);
	
	private static TextureFormat getServerCompressedFormat(TextureFormat f, TextureCompression c) {
		switch(c) {
		case DXT1:
			if (f.getNumComponents() == 3)
				return TextureFormat.COMPRESSED_RGB_DXT1;
			else
				return TextureFormat.COMPRESSED_RGBA_DXT1;
		case DXT3:
			return TextureFormat.COMPRESSED_RGBA_DXT3;
		case DXT5:
			return TextureFormat.COMPRESSED_RGBA_DXT5;
		default:
			return f;
		}
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
	
	/**
	 * Whether or not this RenderContext has been initialized.  After this returns true, the render context must
	 * have computed and gathered a valid set of SystemCapabilities and have a valid underlying context.
	 */
	public abstract boolean isInitialized();
	
	/**
	 * Sets the active state manager that was used to apply atoms of the given type.  Shouldn't be called
	 * directly, instead use StateManager's apply().
	 */
	public void setActiveStateManager(StateManager man, Class<? extends StateAtom> type) throws NullPointerException {
		if (type == null)
			throw new NullPointerException("Can't have a null type");
		int index = (man != null ? man.getDynamicType() : registerStateAtomType(type));
		this.setActiveStateManager(man, index);
	}
	
	/**
	 * Sets the active state manager that was used to apply atoms with the given dynamic type.  Shouldn't be
	 * called directly.
	 */
	public void setActiveStateManager(StateManager man, int type) throws NullPointerException {
		if (man != null && man.getDynamicType() != type)
			throw new IllegalArgumentException("A non-null state manager's dynamic type doesn't agree with the specified type");
		if (this.stateRecord == null)
			this.stateRecord = new StateManager[StateManager.NUM_CORE_STATES];
		if (this.stateRecord.length <= type) {
			StateManager[] temp = new StateManager[type + 1];
			System.arraycopy(this.stateRecord, 0, temp, 0, this.stateRecord.length);
			this.stateRecord = temp;
		}
		this.stateRecord[type] = man;
	}
	
	/**
	 * Get the active state manager for the given type, which can't be null.
	 */
	public StateManager getActiveStateManager(Class<? extends StateAtom> type) throws NullPointerException {
		if (type == null)
			throw new NullPointerException("Can't have a null type");
		int index = registerStateAtomType(type);
		return this.getActiveStateManager(index);
	}
	
	/**
	 * Get the active state manager for the given dynamic type (>= 0).
	 */
	public StateManager getActiveStateManager(int dynamicType) {
		if (this.stateRecord == null || dynamicType >= this.stateRecord.length)
			return null;
		return this.stateRecord[dynamicType];
	}
	
	/**
	 * Set the active state atom for the given class and unit.  This and its variant shouldn't be 
	 * called directly, instead use the StateAtom's apply() method or restore() method (or RenderPass's
	 * applyState() method).
	 */
	public void setActiveStateAtom(StateAtom atom, Class<? extends StateAtom> type, StateUnit unit) throws NullPointerException {
		if (type == null)
			throw new NullPointerException("Can't have a null type");
		if (unit == null)
			throw new NullPointerException("Can't have a null unit");
		
		this.setActiveStateAtom(atom, registerStateAtomType(type), unit.ordinal());
	}
	
	/**
	 * Set the active state atom for the given type and ordinal value (as returned by a StateUnit, >= 0).
	 */
	public void setActiveStateAtom(StateAtom atom, int type, int ordinal) throws IllegalArgumentException {
		if (atom != null && atom.getDynamicType() != type)
			throw new IllegalArgumentException("A non-null state atom must have the same dynamic type");
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
	
	/**
	 * Get the last applied state atom that is of the given atom type and at the state unit.
	 * The StateUnit must be valid for the given type of atom.  Null implies that no previous 
	 * state atom for that type has been applied.
	 */
	public StateAtom getActiveStateAtom(Class<? extends StateAtom> type, StateUnit unit) throws NullPointerException {
		if (type == null)
			throw new NullPointerException("Can't have a null type");
		if (unit == null)
			throw new NullPointerException("Can't have a null unit");
			
		return this.getActiveStateAtom(registerStateAtomType(type), unit.ordinal());
	}
	
	/**
	 * Get the last applied state atom that has the given dynamic type and ordinal ->
	 * number returned by the appropriate StateUnit.  Null implies that no previous 
	 * state atom for that type has been applied.
	 */
	public StateAtom getActiveStateAtom(int dynamicType, int ordinal) {
		if (this.atomRecord == null || this.atomRecord.length <= dynamicType || this.atomRecord[dynamicType] == null || this.atomRecord[dynamicType].stateRecord.length <= ordinal)
			return null;
		return this.atomRecord[dynamicType].stateRecord[ordinal];
	}
	
	/**
	 * Get the RenderManager that is attached to this RenderContext
	 */
	public RenderManager getRenderManager() {
		return this.manager;
	}

	/**
	 * Get the default render pass peer suitable for use in a standard double/single buffered
	 * setup
	 */
	public RenderPassPeer<RenderPass> getDefaultRenderPassPeer() {
		return this.defaultPass;
	}
	
	/**
	 * Convenience method that throws an exception if the RenderContext isn't current
	 */
	protected void callValidate() throws FeroxException {
		if (!this.isCurrent())
			throw new FeroxException("Method unavailable when RenderContext isn't current");
	}
}
