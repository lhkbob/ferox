package com.ferox.core.states;

import java.util.Arrays;
import java.util.Comparator;

import com.ferox.core.renderer.Bin;
import com.ferox.core.renderer.RenderContext;
import com.ferox.core.scene.states.Fog;
import com.ferox.core.scene.states.Light;
import com.ferox.core.states.atoms.*;

public class StateBin implements Bin<StateLeaf> {
	public static final Comparator<StateLeaf> comparer = new Comparator<StateLeaf>() {
		private final boolean hasState(StateManager[] states, int type) {
			return type < states.length && states[type] != null;
		}
		
		public int compare(StateLeaf sl1, StateLeaf sl2) {
			StateManager[] s1 = sl1.getMergedStates();
			StateManager[] s2 = sl2.getMergedStates();
			int type, h1, h2;
			boolean t1, t2;
			
			for (int i = 0; i < sortPriority.length; i++) {
				type = RenderContext.registerStateAtomType(sortPriority[i]);
				t1 = this.hasState(s1, type);
				t2 = this.hasState(s2, type);
				
				if (t1 && t2) {
					h1 = s1[type].getSortingIdentifier();
					h2 = s2[type].getSortingIdentifier();
					if (h1 < h2)
						return -1;
					else if (h1 > h2)
						return 1;
				} else if (t1) {
					return 1;
				} else if (t2) {
					return -1;
				}
			}
			return 0;
		}
	};
	
	private static Class<? extends StateAtom>[] sortPriority = (Class<? extends StateAtom>[])new Class[] {
		GLSLShaderProgram.class, VertexArray.class, Texture.class, AlphaState.class,
		Light.class, StencilState.class, Material.class, DrawMode.class,
		ZBuffer.class, Fog.class, BlendState.class
	};
	
	private static final int DEFAULT_INCREMENT = 10;
	
	private StateLeaf[] bin;
	private int count;
	
	public StateBin(int startingSize) {
		this.count = 0;
		this.bin = new StateLeaf[startingSize + 2];
	}

	public static int[] getStateSortingPriority() {
		int[] t = new int[sortPriority.length];
		System.arraycopy(sortPriority, 0, t, 0, sortPriority.length);
		return t;
	}
	
	public static void setStateSortingPriority(Class<? extends StateAtom>[] priorities) {
		if (priorities == null || Arrays.equals(priorities, sortPriority))
			return;
		sortPriority = (Class<? extends StateAtom>[])new Class[priorities.length];
		System.arraycopy(priorities, 0, sortPriority, 0, sortPriority.length);
	}

	/**
	 * The capacity of the RenderQueue, if more atoms are added beyond this, the queue will be
	 * forced to grow.
	 */
	public int capacity() {
		return this.bin.length - 2;
	}
	
	/**
	 * Get the number of atoms currently added to the queue.
	 */
	public int itemCount() {
		return this.count;
	}
	
	/**
	 * Clears the queue for the next frame.
	 */
	public void clear() {
		this.count = 0;
	}
	
	/**
	 * Makes sure that the RenderQueue has at least size elements, growing the queue if needed.
	 */
	public void ensureCapacity(int size) {
		if (size + 2 > this.bin.length) {
			StateLeaf[] temp = new StateLeaf[size + 2];
			if (this.bin != null)
				System.arraycopy(this.bin, 1, temp, 1, Math.min(this.bin.length - 2, size));
			this.bin = temp;
		} 
	}
	
	/**
	 * Adds the atom to the queue, growing the queue if needed and updates the FrameStatistics for
	 * the total scene.  Since each Leaf has a unique SpatialAtom, this should not have been
	 * in the queue before.
	 */
	public void add(StateLeaf item) {
		if (this.count == this.bin.length - 2) {
			this.ensureCapacity(this.count + DEFAULT_INCREMENT);
		}

		this.bin[1 + this.count] = item;
		this.count++;
	}
	
	public void optimize() {
		if (this.count > 1) 
			Arrays.sort(this.bin, 1, this.count + 1, comparer);
		
		for (int i = 1; i < this.count + 1; i++) {
			updateManagers(this.bin[i].getMergedStates());
			this.bin[i].sortIndex = i;
		}
	}
	
	private static void updateManagers(StateManager[] merged) {
		for (int i = 0; i < merged.length; i++) {
			if (merged[i] != null) {
				System.out.print(merged[i].getClass().getSimpleName() + "." + Integer.toHexString(merged[i].hashCode()) + " | ");
				merged[i].update();
			}
		}
		System.out.println("");
	}
}
