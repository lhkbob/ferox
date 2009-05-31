package com.ferox.renderer;

import com.ferox.effect.Effect;
import com.ferox.effect.EffectSet;
import com.ferox.effect.EffectType;

/**
 * EffectSortingRenderQueue can be used to order queued RenderAtoms by the
 * Effects stored in their EffectSet. When rendering opaque atoms, this is the
 * recommended queue because it can greatly reduce the number of low-level
 * operations because similar render atoms can be grouped and rendered in bulk.
 * 
 * @author Michael Ludwig
 */
public class EffectSortingRenderQueue extends BasicRenderQueue {
	private int[] effectSortKeys;

	@Override
	protected void optimizeOrder(View view, RenderAtom[] atoms, int raCount) {
		if (raCount > 1) {
			// make sure we have enough indices
			if (effectSortKeys == null || effectSortKeys.length < raCount)
				effectSortKeys = new int[raCount];

			for (int i = 0; i < raCount; i++)
				effectSortKeys[i] = computeSortKey(atoms[i]);

			quickSort(effectSortKeys, atoms, 0, raCount);
		}
	}

	/* Compute the sorting key for the given render atom. */
	private int computeSortKey(RenderAtom atom) {
		EffectSet set = atom.getEffects();

		if (set != null) {
			Effect e;
			int key = 0;

			set.reset();
			while ((e = set.next()) != null) {
				key ^= typeKey(System.identityHashCode(e), e.getType());
			}

			// store it for later
			return key;
		} else
			return 0;
	}

	/*
	 * Return the bit key for hash and type based on a hardcoded bit layout
	 * pattern for the different types.
	 */
	private int typeKey(int hash, EffectType t) {
		int comp;

		switch (t) {
		case TEXTURE:
		case ALPHA:
		case DEPTH:
		case COLOR_MASK:
		case STENCIL:
		case LIGHT:
		case LINE:
		case POINT:
		case POLYGON:
			comp = (hash & 0xff);
			break;
		default:
			comp = (hash & 0xf);
			break;
		}

		switch (t) {
		case SHADER:
			// bits 28 - 31
			return comp << 28;
		case TEXTURE:
			// bits 20 - 27
			return comp << 20;
		case LIGHT:
			// bits 12 - 19
			return comp << 12;
		case BLEND:
		case MATERIAL:
		case GLOBAL_LIGHTING:
		case FOG:
			// bits 8 - 11
			return comp << 8;
		default:
			// bits 0 - 7
			return comp;
		}
	}

	// Sort both k and x, based on the keys in k, using the quick sort algorithm
	private static void quickSort(int[] k, RenderAtom[] x, int off, int len) {
		int m = off + (len >> 1);
		if (len > 7) {
			int l = off;
			int n = off + len - 1;
			if (len > 40) {
				int s = len / 8;
				l = med3(k, l, l + s, l + 2 * s);
				m = med3(k, m - s, m, m + s);
				n = med3(k, n - 2 * s, n - s, n);
			}
			m = med3(k, l, m, n);
		}
		int v = k[m];

		int a = off, b = a, c = off + len - 1, d = c;
		int s; // temp variable
		while (true) {
			while (b <= c && (s = k[b]) <= v) {
				if (s == v)
					swap(k, x, a++, b);
				b++;
			}
			while (c >= b && (s = k[c]) >= v) {
				if (s == v)
					swap(k, x, c, d--);
				c--;
			}
			if (b > c)
				break;
			swap(k, x, b++, c--);
		}

		int n = off + len;
		s = Math.min(a - off, b - a);
		vecswap(k, x, off, b - s, s);
		s = Math.min(d - c, n - d - 1);
		vecswap(k, x, b, n - s, s);

		if ((s = b - a) > 1)
			quickSort(k, x, off, s);
		if ((s = d - c) > 1)
			quickSort(k, x, n - s, s);
	}

	private static int med3(int[] k, int a, int b, int c) {
		int sa = k[a];
		int sb = k[b];
		int sc = k[c];

		return (sa < sb ? (sb < sc ? b : (sa < sc ? c : a)) : (sb > sc ? b
			: (sa > sc ? c : a)));
	}

	private static void vecswap(int[] k, RenderAtom x[], int a, int b, int n) {
		for (int i = 0; i < n; i++, a++, b++)
			swap(k, x, a, b);
	}

	private static void swap(int[] k, RenderAtom[] x, int a, int b) {
		RenderAtom t = x[a];
		x[a] = x[b];
		x[b] = t;

		int v = k[a];
		k[a] = k[b];
		k[b] = v;
	}
}
