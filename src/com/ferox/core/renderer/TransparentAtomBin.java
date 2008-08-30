package com.ferox.core.renderer;


import org.openmali.vecmath.Matrix3f;
import org.openmali.vecmath.Vector3f;

import com.ferox.core.scene.Plane;
import com.ferox.core.scene.Transform;
import com.ferox.core.scene.View;
import com.ferox.core.scene.bounds.AxisAlignedBox;
import com.ferox.core.scene.bounds.BoundingVolume;
import com.ferox.core.states.FragmentTest;
import com.ferox.core.states.NullUnit;
import com.ferox.core.states.StateManager;
import com.ferox.core.states.atoms.AlphaState;
import com.ferox.core.states.atoms.BlendState;
import com.ferox.core.states.atoms.DrawMode;
import com.ferox.core.states.atoms.ZBuffer;
import com.ferox.core.states.atoms.DrawMode.DrawFace;
import com.ferox.core.states.manager.DrawModeManager;


/**
 * Default implementation of a RenderAtomBin that handles blended RenderAtoms.  To generate correct results, it
 * splits the view frustum into bands and assigns the visible atoms into one (or more) bands.  These bands are
 * rendered from back to front, in a limited depth peeling algorithm by using custom clip planes.  Because it
 * uses clipping planes, the first two clip planes are reserved for its use and will give incorrect results if
 * you expect those planes to be something else.
 * 
 * @author Michael Ludwig
 *
 */
public class TransparentAtomBin extends RenderAtomBin {
	private static class Band {
		int[] indices;
		int indexCount;
		
		float znear;
		float zfar;
		
		float maxZNear;
		float minZFar;
		
		public float getSliceDepth() {
			float depth = this.zfar - this.znear;
			if (this.znear > 50f)
				return depth;
			if (this.znear > 15f)
				return depth / 2f;
			if (this.indexCount > 5)
				return depth;
			if (this.indexCount > 2)
				return depth / 2f;
			return depth / 4f;
		}
	}
	
	private static final float BAND_DEPTH = 10f;
	
	private Band[] bands;
	private BoundingVolume drawnRegion;
	private Plane near, far;
	
	public TransparentAtomBin(int startingSize) {
		super(startingSize, true);
		
		this.drawnRegion = new AxisAlignedBox();
		this.near = new Plane();
		this.far = new Plane();
	}

	@Override
	public void clear() {
		super.clear();
		this.drawnRegion = null;
	}
	
	@Override
	public void addRenderAtom(RenderAtom atom, RenderManager manager) {
		BoundingVolume b = atom.getSpatialLink().getWorldBounds();
		if (this.drawnRegion == null)
			this.drawnRegion = b.clone(this.drawnRegion);
		else
			this.drawnRegion.enclose(b);
		super.addRenderAtom(atom, manager);
	}
	
	// Instantiate and assign band regions given the extents of the view frustum
	private void initializeBands(float znear, float zfar) {
		int numBands = (int)Math.ceil((zfar - znear) / BAND_DEPTH);
		
		if (this.bands == null || this.bands.length != numBands) {
			Band[] temp = new Band[numBands];
			if (this.bands != null)
				System.arraycopy(this.bands, 0, temp, 0, Math.min(this.bands.length, temp.length));
			this.bands = temp;
		}
		
		float step = (zfar - znear) / numBands;
		Band band;
		for (int i = 0; i < numBands; i++) {
			band = this.bands[i];
			if (band == null) {
				band = new Band();
				this.bands[i] = band;
			}
			
			band.indexCount = 0;
			band.zfar = zfar;
			band.znear = zfar - step;
			band.maxZNear = band.zfar;
			band.minZFar = band.znear;
			
			zfar = band.znear;
		}
	}
	
	// Assign each RenderAtom in this bin to at least one band.
	private void assignBands(Vector3f camDir, Vector3f camPos, float znear, float zfar) {
		int farIndex;
		int nearIndex;
		float atomFar;
		float atomNear;
		Vector3f ext = new Vector3f();
		AxisAlignedBox b = new AxisAlignedBox();
		
		float defStep = (zfar - znear) / this.bands.length;
		
		for (int i = 1; i < this.count + 1; i++) {
			// Use an AxisAlignedBox because it gives better depth estimation than a sphere tends to.
			this.bin[i].getGeometry().getBoundingVolume(b);
			b.applyTransform(this.bin[i].getSpatialLink().getWorldTransform());
			
			b.getFurthestExtent(camDir, ext);
			ext.sub(camPos);
			atomFar = Math.min(zfar, camDir.dot(ext));
			b.getClosestExtent(camDir, ext);
			ext.sub(camPos);
			atomNear = Math.max(znear, camDir.dot(ext));
						
			nearIndex = Math.min(this.bands.length - 1, (int)Math.ceil((zfar - atomNear) / defStep));
			farIndex = Math.max(0, (int)Math.floor((zfar - atomFar) / defStep));
			
			// correct farIndex
			while (farIndex < this.bands.length && atomFar < this.bands[farIndex].znear)
				farIndex += 1;
			// correct nearIndex
			while (nearIndex >= 0 && atomNear > this.bands[nearIndex].zfar)
				nearIndex -= 1;
			
			farIndex = Math.min(this.bands.length - 1, farIndex);
			nearIndex = Math.max(0, nearIndex);
			for (int u = farIndex; u <= nearIndex; u++) 
				addIndex(this.bands[u], i);
		}
	}
	
	// Add the given index into the bin to the given band.
	private static void addIndex(Band band, int index) {
		band.indexCount++;
		
		if (band.indices == null || band.indices.length <= band.indexCount) {
			int[] temp = new int[band.indexCount];
			if (band.indices != null)
				System.arraycopy(band.indices, 0, temp, 0, band.indices.length);
			band.indices = temp;
		}
		band.indices[band.indexCount - 1] = index;
	}
	
	@Override
	protected void renderAll(RenderManager manager, RenderPass pass, RenderAtomMask mask, ZBuffer zbuff, DrawMode draw, AlphaState alpha, BlendState blend) {
		// Update the defaults
		draw.setDrawFace(DrawFace.FRONT_AND_BACK);
		alpha.setAlphaEnabled(true);
		
		boolean store = pass.isStateManagerMasked(BlendState.class);
		pass.setStateManagerMasked(BlendState.class, true);
		// Render opaque pixels in the blended geometry
		super.renderAll(manager, pass, mask, zbuff, draw, alpha, blend);
		pass.setStateManagerMasked(BlendState.class, store);
		
		View view = pass.getView();
		Transform w = view.getWorldTransform();
		Matrix3f b = w.getRotation();
		
		Vector3f camDir = new Vector3f(-b.m20, -b.m21, -b.m22);
		camDir.normalize();
		Vector3f camPos = w.getTranslation();
		Vector3f diff = new Vector3f();
		
		this.drawnRegion.getFurthestExtent(camDir, diff);
		diff.sub(camPos);
		float zfar = Math.min(view.getFrustumFar(), camDir.dot(diff));
		
		this.drawnRegion.getClosestExtent(camDir, diff);
		diff.sub(camPos);
		float znear = Math.max(view.getFrustumNear(), camDir.dot(diff));
		
		this.initializeBands(znear, zfar);
		this.assignBands(camDir, camPos, znear, zfar);
		
		// set defaults for blended geometry
		alpha.setAlphaTest(FragmentTest.LESS);
		blend.setBlendEnabled(true);
		zbuff.setZBufferWriteEnabled(true);
		
		RenderContext context = manager.getRenderContext();
		zbuff.applyState(manager, NullUnit.get());
		alpha.applyState(manager, NullUnit.get());
		blend.applyState(manager, NullUnit.get());
		
		// turn on clipping to simulate depth peeling
		context.enableUserClipPlane(0);
		context.enableUserClipPlane(1);
		
		RenderAtom curr, prev;
		float sliceDepth;
		boolean setFace = false;
		int dmT = RenderContext.registerStateAtomType(DrawMode.class);
		int numSlices;
		float step;
		Band band;
		StateManager[] states;
		
		for (int i = 0; i < this.bands.length; i++) {
			band = this.bands[i];
			znear = band.znear;
			zfar = band.zfar;
			
			sliceDepth = this.bands[i].getSliceDepth();
			
			numSlices = (int)Math.ceil((zfar - znear) / sliceDepth);
			step = (zfar - znear) / numSlices;
			znear = zfar - step;

			for (int s = 0; s < numSlices; s++) {
				near.setPlane(0, 0, -1, -znear);
				far.setPlane(0, 0, 1, zfar);
					
				near.transformLocal(w);
				far.transformLocal(w);
			
				context.setUserClipPlane(near, 0);
				context.setUserClipPlane(far, 1);
				
				draw.setDrawFace(DrawFace.BACK);
				draw.applyState(manager, NullUnit.get());
				prev = null;
				for (int p = 0; p < band.indexCount; p++) {
					curr = this.bin[band.indices[p]];
					if (mask == null || mask.isValidForRender(curr, manager, pass)) {
						states = curr.getCachedStates();
						if (states.length > dmT && states[dmT] != null) {
							DrawMode d = ((DrawModeManager)states[dmT]).getStateAtom();
							if (d != null && d.getDrawFace() == DrawFace.FRONT)
								continue;
							else if (d != null && d.getDrawFace() == DrawFace.FRONT_AND_BACK) {
								d.setDrawFace(DrawFace.BACK);
								setFace = true;
							}
						}
						this.renderAtom(curr, prev, context, manager, pass);
						this.addStatistics(curr.getGeometry(), manager);
						if (setFace) {
							((DrawModeManager)states[dmT]).getStateAtom().setDrawFace(DrawFace.FRONT_AND_BACK);
							setFace = false;
						}
					}
					prev = curr;
				}
				context.clearSpatialStates();
				RenderAtom.applyStates(prev, null, manager, pass);
				draw.restoreState(manager, NullUnit.get());
				
				draw.setDrawFace(DrawFace.FRONT);
				draw.applyState(manager, NullUnit.get());
				prev = null;
				for (int p = 0; p < band.indexCount; p++) {
					curr = this.bin[band.indices[p]];
					states = curr.getCachedStates();
					if (mask == null || mask.isValidForRender(curr, manager, pass)) {
						if (states.length > dmT && states[dmT] != null) {
							DrawMode d = ((DrawModeManager)states[dmT]).getStateAtom();
							if (d != null && d.getDrawFace() == DrawFace.BACK)
								continue;
							else if (d != null && d.getDrawFace() == DrawFace.FRONT_AND_BACK) {
								d.setDrawFace(DrawFace.FRONT);
								setFace = true;
							}
						}
						this.renderAtom(curr, prev, context, manager, pass);
						this.addStatistics(curr.getGeometry(), manager);
						if (setFace) {
							((DrawModeManager)states[dmT]).getStateAtom().setDrawFace(DrawFace.FRONT_AND_BACK);
							setFace = false;
						}
					}
					prev = curr;
				}
				context.clearSpatialStates();
				RenderAtom.applyStates(prev, null, manager, pass);
				draw.restoreState(manager, NullUnit.get());
				
				zfar = znear;
				znear = zfar - step;
			}
		}
		
		context.disableUserClipPlane(0);
		context.disableUserClipPlane(1);
		
		zbuff.restoreState(manager, NullUnit.get());
		alpha.restoreState(manager, NullUnit.get());
		blend.restoreState(manager, NullUnit.get());
	} 	
}
