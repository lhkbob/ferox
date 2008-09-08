package com.ferox.core.renderer;

import java.util.ArrayList;

import com.ferox.core.scene.InfluenceLeaf;
import com.ferox.core.states.*;
import com.ferox.core.states.atoms.AlphaState;
import com.ferox.core.states.atoms.BlendState;
import com.ferox.core.states.atoms.DrawMode;
import com.ferox.core.states.atoms.ZBuffer;
import com.ferox.core.states.atoms.BlendState.BlendFactor;
import com.ferox.core.states.atoms.BlendState.BlendFunction;
import com.ferox.core.states.atoms.DrawMode.DrawFace;
import com.ferox.core.states.atoms.DrawMode.DrawStyle;
import com.ferox.core.states.atoms.DrawMode.Winding;
import com.ferox.core.states.manager.Geometry;

/**
 * A Bin implementation that is used to render all RenderAtoms present in a given view of a scene.  It is recommended
 * to only subclass this class when necessary or to use a custom transparent queue.  It is backed by an array.
 * For subclasses, the array is always 2 greater than the capacity of the bin because there is a null element in the
 * first index and the index after the last valid RenderAtom.  This is to make iterating and apply states easier
 * since you can use array access to always get an appropriate previous or next atom.
 * 
 * @author Michael Ludwig
 *
 */
public class RenderAtomBin implements Bin<RenderAtom> {	
	//FIXME: turn this into some flexible structure that can handle any default type of atom
	private static ZBuffer defaultZ = new ZBuffer();
	private static DrawMode defaultDraw = new DrawMode();
	private static AlphaState defaultAlpha = new AlphaState();
	private static BlendState defaultBlend = new BlendState();
	
	private static final int DEFAULT_INCREMENT = 10;
	
	private RenderAtomBin transparent;
	private ArrayList<InfluenceLeaf> influences;
	protected RenderAtom[] bin;
	protected int count;
		
	/**
	 * Creates a queue with the given capacity.  You shouldn't need to instantiate these
	 * directly, instead let SpatialTree take care of it.
	 */
	public RenderAtomBin(int startingSize) {
		this(startingSize, false);
	}
	
	/**
	 * transparent should be true if this bin is intended for use as a transparent RenderAtom bin
	 */
	protected RenderAtomBin(int startingSize, boolean transparent) {
		this.count = 0;
		this.bin = new RenderAtom[startingSize + 2];
		this.influences = new ArrayList<InfluenceLeaf>();
		if (!transparent)
			this.transparent = new TransparentAtomBin(startingSize);
	}
	
	/**
	 * The capacity of the RenderQueue, if more atoms are added beyond this, the bin's backing array will grow.
	 */
	public int capacity() {
		return this.bin.length - 2;
	}
	
	/**
	 * Current number of atoms in the bin.
	 */
	public int itemCount() {
		return this.count;
	}
	
	/**
	 * Must be called between renderings of a scene if the view has changed.
	 */
	public void clear() {
		this.count = 0;
		this.influences.clear();
		if (this.transparent != null)
			this.transparent.clear();
	}
	
	public void ensureCapacity(int size) {
		if (this.transparent != null) {
			this.transparent.ensureCapacity(size);					                			         
		}
		if (size + 2 > this.bin.length) {
			RenderAtom[] temp = new RenderAtom[size + 2];
			if (this.bin != null)
				System.arraycopy(this.bin, 1, temp, 1, Math.min(this.bin.length - 2, size));
			this.bin = temp;
		} 
	}
	
	/**
	 * Don't call directly, instead use addRenderAtom(), which handles statistics tracking and 
	 * transparent render atoms.
	 */
	public void add(RenderAtom item) {
		if (this.count == this.bin.length - 2) {
			this.ensureCapacity(this.count + DEFAULT_INCREMENT);
		}

		this.bin[1 + this.count] = item;
		this.count++;
	}
	
	/**
	 * Adds the atom to the queue, growing the queue if needed and updates the FrameStatistics for
	 * the total scene.  It doesn't check for duplicate atoms, if the same atom is added (won't happen
	 * if the bin is properly cleared between renderings of a scene and if you allow the SpatialTree to
	 * handle everything), results are undefined (but most likely result in a double render).
	 */
	public void addRenderAtom(RenderAtom atom, RenderManager manager) {
		if (atom.getStateLink() == null)
			return;
		Geometry g = atom.getGeometry();
		if (g == null)
			return;
		
		if (this.transparent != null && atom.isTransparent()) {
			this.transparent.addRenderAtom(atom, manager);
		} else {
			this.add(atom);
			atom.stateSortedIndex = atom.getStateLink().getSortIndex();
			atom.states = atom.getStateLink().getMergedStates();
			
			this.addStatistics(g, manager);
		}
	}
	
	/**
	 * Utility method for subclasses to update the given RenderManager's statistics.
	 */
	protected final void addStatistics(Geometry g, RenderManager manager) {
		manager.getFrameStatistics().add(1, g.getVertices().getNumElements(), g.getPolygonCount());
	}
	
	/**
	 * Add an InfluenceLeaf.  Should not be called directly, it is called by InfluenceLeaves that intersect
	 * the viewing frustum.  An added InfluenceLeaf will apply its attached state to all influenced atoms that
	 * will be rendered.
	 */
	public void addInfluenceLeaf(InfluenceLeaf leaf) {
		if (leaf.getState() == null)
			return;
		
		this.influences.add(leaf);
		if (this.transparent != null)
			this.transparent.addInfluenceLeaf(leaf);
	}
	
	/**
	 * Renders the bin with all of the atoms currently inside it, this should not be called
	 * directly by the program, but instead by RenderPass.
	 */
	public void renderAtoms(RenderManager manager, RenderPass pass) {
		if (this.count > 0) {
			this.bin[0] = null;
			this.bin[1 + this.count] = null;
			
			defaultZ.setZBufferWriteEnabled(true);
			defaultZ.setDepthTest(FragmentTest.LEQUAL);
			
			defaultDraw.setWinding(Winding.COUNTER_CLOCKWISE);
			defaultDraw.setBackMode(DrawStyle.FILLED);
			defaultDraw.setFrontMode(DrawStyle.FILLED);
			defaultDraw.setDrawFace(DrawFace.FRONT);
			
			defaultAlpha.setAlphaEnabled(false);
			defaultAlpha.setAlphaRefValue(1f);
			defaultAlpha.setAlphaTest(FragmentTest.GEQUAL);
			
			defaultBlend.setBlendEnabled(false);
			defaultBlend.setBlendFunction(BlendFunction.ADD);
			defaultBlend.setSourceBlendFactor(BlendFactor.SRC_ALPHA);
			defaultBlend.setDestBlendFactor(BlendFactor.ONE_MINUS_SRC_ALPHA);
			
			this.renderAll(manager, pass, pass.getRenderAtomMask(), defaultZ, defaultDraw, defaultAlpha, defaultBlend);
		}
		
		if (this.transparent != null) {
			this.transparent.optimize();
			this.transparent.renderAtoms(manager, pass);
		}
	}
	
	/**
	 * Subclasses can override this method to provide more unique rendering of atoms.  They must obey pass's 
	 * RenderAtomMask and they must apply and restore the default states passed in.  They are allowed to modify
	 * the values of the default states to get desired affects (each frame the defaults are restored).
	 */
	protected void renderAll(RenderManager manager, RenderPass pass, RenderAtomMask mask, ZBuffer zbuff, DrawMode draw, AlphaState alpha, BlendState blend) {
		RenderAtom prev = null;
		
		zbuff.applyState(manager, NullUnit.get());
		draw.applyState(manager, NullUnit.get());
		alpha.applyState(manager, NullUnit.get());
		blend.applyState(manager, NullUnit.get());
		RenderAtom curr;
		RenderContext context = manager.getRenderContext();
		
		for (int i = 1; i < this.count + 1; i++) {
			curr = this.bin[i];
			if (mask == null || mask.isValidForRender(curr, manager, pass)) {
				this.renderAtom(prev, curr, context, manager, pass);
				prev = curr;
			}
		}
		context.clearSpatialStates();
		RenderAtom.applyStates(prev, null, manager, pass);
		
		zbuff.restoreState(manager, NullUnit.get());
		draw.restoreState(manager, NullUnit.get());
		alpha.restoreState(manager, NullUnit.get());
		blend.restoreState(manager, NullUnit.get());
	}
	
	/**
	 * Utility method to render curr, given that prev was the last rendered atom.  This method computes the 
	 * correct influences and applies any states and renders the geomtry to the context.
	 */
	protected final void renderAtom(RenderAtom prev, RenderAtom curr, RenderContext context, RenderManager manager, RenderPass pass) {
		context.beginAtom(curr);
		RenderAtom.applyStates(prev, curr, manager, pass);
		
		int s = this.influences.size();
		InfluenceLeaf l;
		for (int u = 0; u < s; u++) {
			l = this.influences.get(u);
			if (l.influencesSpatialLeaf(curr.getSpatialLink()))
				context.addSpatialState(l.getState());
		}
		
		context.endAtom(curr);
	}
	
	/**
	 * Re-orders the bin using the stateSortedIndex of the atom's attached StateLeaf.  You will get inefficient results
	 * if the atoms in the bin have state leaves from multiply trees or if the tree's update() method isn't called
	 * each frame.
	 */
	public void optimize() {
		if (this.count > 1) {
			quickSort(this.bin, 1, this.count);
		}
	}
	
	private static void quickSort(RenderAtom[] x, int off, int len) {
		int m = off + (len >> 1);
		if (len > 7) {
			int l = off;
			int n = off + len - 1;
			if (len > 40) {
				int s = len / 8;
				l = med3(x, l, l+s, l+2*s);
				m = med3(x, m-s, m, m+s);
				n = med3(x, n-2*s, n-s, n);
			}
			m = med3(x, l, m, n);
		}
		int v = x[m].stateSortedIndex ;
		
		int a = off, b = a, c = off+len-1, d = c;
		while(true) {
			while (b <= c && x[b].stateSortedIndex <= v) {
				if (x[b].stateSortedIndex == v)
					swap(x, a++, b);
				b++;
			}
			while (c >= b&& x[c].stateSortedIndex >=v) {
				if (x[c].stateSortedIndex  == v)
					swap(x, c, d--);
				c--;
			}
			if (b > c)
				break;
			swap(x, b++, c--);
		}
		
		int s, n = off + len;
		s = Math.min(a-off, b-a); vecswap(x, off, b-s, s);
		s = Math.min(d-c, n-d-1); vecswap(x, b, n-s,s);
		
		if ((s = b-a) > 1)
			quickSort(x, off, s);
		if ((s = d-c) > 1)
			quickSort(x, n-s, s);
	}
	
	private static void vecswap(RenderAtom x[], int a, int b, int n) {
		for (int i = 0; i < n; i++, a++, b++)
			swap(x, a, b);
	}
	
	private static void swap(RenderAtom[] x, int a, int b) {
		RenderAtom t = x[a];
		x[a] = x[b];
		x[b] = t;
	}
	
	private static int med3(RenderAtom[] x, int a, int b, int c) {
		return (x[a].stateSortedIndex < x[b].stateSortedIndex  ? (x[b].stateSortedIndex  < x[c].stateSortedIndex  ? b: x[a].stateSortedIndex  < x[c].stateSortedIndex  ? c: a) : (x[b].stateSortedIndex  > x[c].stateSortedIndex  ? b: x[a].stateSortedIndex  > x[c].stateSortedIndex  ? c : a));
	}
}
