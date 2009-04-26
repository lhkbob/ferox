package com.ferox.renderer;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Comparator;
import java.util.IdentityHashMap;

import javax.management.relation.Role;

import com.ferox.effect.Effect;
import com.ferox.scene.Appearance;

/**
 * This is a complex render queue that tracks Appearances. It maintains a set of
 * appearances that it has ordered in an optimal manner (based on an optional
 * priority of state types).
 * 
 * If it detects changes to any appearances that will be used for a frame, or if
 * there are new appearances that haven't been cached, it will re-order the
 * appearances. If these operations are not necessary, the render atoms can be
 * quickly sorted based on their appearance's computed sort index.
 * 
 * The caching of appearances within the render queue doesn't use weak
 * references, so a call to clearInternalResources is necessary. The render
 * queue data for each RenderAtom processed does use weak references so it is
 * not necessary to set each atom's queue data to null when an Appearance should
 * be garbage collected.
 * 
 * @author Michael Ludwig
 * 
 */
public class StateSortingRenderQueue extends BasicRenderQueue {
	/*
	 * Holds onto sortIndex and the 'version' of caching. If the version is old,
	 * then we have to update the sortIndex before doing a fast sort.
	 */
	private static class StateRenderQueueData {
		int sortIndex;
		int lastSortKey;
		WeakReference<Appearance> lastAppearance;
		int version;
	}

	/* Sort a list of appearances to group them as best as possible by priority. */
	private static class AppearanceSorter implements Comparator<Appearance> {
		private final Role[] sortPriority;

		public AppearanceSorter(Role[] sortPriority) {
			this.sortPriority = sortPriority;
		}

		@Override
		public int compare(Appearance o1, Appearance o2) {
			int h1, h2;
			Effect s1, s2;

			for (int i = 0; i < sortPriority.length; i++) {
				s1 = o1.getState(sortPriority[i]);
				s2 = o2.getState(sortPriority[i]);
				if (s1 != null && s2 != null) {
					h1 = s1.hashCode();
					h2 = s2.hashCode();
					if (h1 < h2) {
						return -1;
					} else if (h1 > h2) {
						return 1;
					}
				} else if (s1 != null) {
					return 1;
				} else if (s2 != null) {
					return -1;
				}
			}
			return 0;
		}

	}

	private int queueVersion; // increased after every re-cache
	private final IdentityHashMap<Appearance, Integer> sortIndexMap;
	private boolean reCache;

	private final AppearanceSorter appSorter;

	/**
	 * Create a sorting render atom queue that uses a priority of: Texture,
	 * Material, DrawMode, AlphaTest, StencilTest, BlendMode, DepthTest
	 */
	public StateSortingRenderQueue() {
		this(null);
	}

	/**
	 * Create a sorting render atom queue that uses the given priority. If the
	 * array is null, a default one is constructed.
	 * 
	 * @param sortPriority Sort priority to use when sorting by effect
	 * @throws NullPointerException if any element in sortPriority is null
	 */
	public StateSortingRenderQueue(Role[] sortPriority) {
		if (sortPriority == null) { // use a default sort order
			sortPriority = new Role[] { Role.SHADER, Role.TEXTURE,
							Role.MATERIAL, Role.BLEND_MODE,
							Role.POLYGON_DRAW_STYLE, Role.LINE_DRAW_STYLE,
							Role.POINT_DRAW_STYLE, Role.STENCIL_TEST,
							Role.DEPTH_TEST, Role.ALPHA_TEST };
		} else {
			// validate the list
			for (int i = 0; i < sortPriority.length; i++) {
				if (sortPriority[i] == null) {
					throw new NullPointerException(
									"It is invalid to pass in a null class for sortPriority");
				}
			}
		}

		queueVersion = 0;
		sortIndexMap = new IdentityHashMap<Appearance, Integer>();
		reCache = true;

		appSorter = new AppearanceSorter(sortPriority);
	}

	@Override
	public void clearInternalResources() {
		super.clearInternalResources();
		sortIndexMap.clear();
	}

	@Override
	public void add(RenderAtom atom) {
		if (atom == null) {
			return;
		}
		super.add(atom);

		Appearance a = atom.getAppearance();
		int sortKey = (a == null ? 0 : a.sortKey());
		StateRenderQueueData data = (StateRenderQueueData) atom
						.getRenderQueueData(this);

		if (data != null && !reCache) {
			// validate the atom's appearance against the old
			Appearance old = (data.lastAppearance == null ? null
							: data.lastAppearance.get());
			if (a != old || data.version != queueVersion) {
				// new appearance or old atom, so we need to update it
				updateRenderAtom(data, a, sortKey);
			} else if (sortKey != data.lastSortKey) {
				// update sort key and invalidate all sort indices
				data.lastSortKey = sortKey;
				reCache = true;
			} // else nothing has changed for the system and atom
		} else {
			if (data == null) {
				data = new StateRenderQueueData();
				atom.setRenderQueueData(this, data);
			}
			updateRenderAtom(data, a, sortKey);
		}
	}

	// Looks for the new index, and updates the data version
	private void updateRenderAtom(StateRenderQueueData data, Appearance newApp,
					int sortKey) {
		// update the appearance fields
		data.lastAppearance = (newApp == null ? null
						: new WeakReference<Appearance>(newApp));
		data.lastSortKey = sortKey;

		// now try to find the index
		if (!reCache) {
			Integer newIndex = (newApp == null ? Integer.valueOf(0)
							: sortIndexMap.get(newApp));
			if (newIndex != null) {
				// update with the new index
				data.sortIndex = newIndex.intValue();
				data.version = queueVersion; // update the version for later
			} else {
				// unknown appearance, flag a recache
				reCache = true;
			}
		}
	}

	@Override
	protected void optimizeOrder(View view, RenderAtom[] atoms, int count) {
		if (reCache) {
			cacheSortIndex(atoms, count);
			reCache = false;
		}
		if (count > 1) {
			quickSort(atoms, 0, count);
		}
	}

	private void cacheSortIndex(RenderAtom[] atoms, int count) {
		queueVersion++;
		IdentityHashMap<Appearance, Boolean> cache = new IdentityHashMap<Appearance, Boolean>();

		Appearance a;
		StateRenderQueueData data;
		for (int i = 0; i < count; i++) {
			data = (StateRenderQueueData) atoms[i].getRenderQueueData(this);
			data.version = queueVersion; // update the version for cached atoms
			a = (data.lastAppearance == null ? null : data.lastAppearance.get());
			if (a != null) {
				cache.put(a, true);
			}
		}

		Appearance[] cacheArray = cache.keySet().toArray(
						new Appearance[cache.keySet().size()]);
		Arrays.sort(cacheArray, appSorter);

		sortIndexMap.clear();
		for (int i = 0; i < cacheArray.length; i++) {
			sortIndexMap.put(cacheArray[i], i);
		}

		for (int i = 0; i < count; i++) {
			data = (StateRenderQueueData) atoms[i].getRenderQueueData(this);
			a = (data.lastAppearance == null ? null : data.lastAppearance.get());
			if (a != null) {
				data.sortIndex = sortIndexMap.get(a);
			} else {
				data.sortIndex = 0;
			}
		}
	}

	private void quickSort(RenderAtom[] x, int off, int len) {
		int m = off + (len >> 1);
		if (len > 7) {
			int l = off;
			int n = off + len - 1;
			if (len > 40) {
				int s = len / 8;
				l = med3(x, l, l + s, l + 2 * s);
				m = med3(x, m - s, m, m + s);
				n = med3(x, n - 2 * s, n - s, n);
			}
			m = med3(x, l, m, n);
		}
		int v = sortIndex(x[m]);

		int a = off, b = a, c = off + len - 1, d = c;
		int s; // temp variable
		while (true) {
			while (b <= c && (s = sortIndex(x[b])) <= v) {
				if (s == v) {
					swap(x, a++, b);
				}
				b++;
			}
			while (c >= b && (s = sortIndex(x[c])) >= v) {
				if (s == v) {
					swap(x, c, d--);
				}
				c--;
			}
			if (b > c) {
				break;
			}
			swap(x, b++, c--);
		}

		int n = off + len;
		s = Math.min(a - off, b - a);
		vecswap(x, off, b - s, s);
		s = Math.min(d - c, n - d - 1);
		vecswap(x, b, n - s, s);

		if ((s = b - a) > 1) {
			quickSort(x, off, s);
		}
		if ((s = d - c) > 1) {
			quickSort(x, n - s, s);
		}
	}

	private int sortIndex(RenderAtom a) {
		return ((StateRenderQueueData) a.getRenderQueueData(this)).sortIndex;
	}

	private int med3(RenderAtom[] x, int a, int b, int c) {
		int sa = sortIndex(x[a]);
		int sb = sortIndex(x[b]);
		int sc = sortIndex(x[c]);

		return (sa < sb ? (sb < sc ? b : (sa < sc ? c : a)) : (sb > sc ? b
						: (sa > sc ? c : a)));
	}

	private static void vecswap(RenderAtom x[], int a, int b, int n) {
		for (int i = 0; i < n; i++, a++, b++) {
			swap(x, a, b);
		}
	}

	private static void swap(RenderAtom[] x, int a, int b) {
		RenderAtom t = x[a];
		x[a] = x[b];
		x[b] = t;
	}
}
