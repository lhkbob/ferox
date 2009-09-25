package com.ferox.scene.fx.impl.fixed;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import com.ferox.scene.Shape;
import com.ferox.scene.fx.Appearance;
import com.ferox.util.Bag;

public class AppearanceSorter {
	private final FixedFunctionSceneCompositor compositor;
	
	private final Sorter sorter;
	private int[] indices;
	
	public AppearanceSorter(FixedFunctionSceneCompositor compositor) {
		this.compositor = compositor;
		sorter = new Sorter();
	}
	
	public void sort(Bag<Shape> shapes) {
		if (indices == null || indices.length < shapes.size())
			indices = new int[shapes.size()];
		
		SortedSet<Appearance> apps = new TreeSet<Appearance>(sorter);
		int ct = shapes.size();
		for (int i = 0; i < ct; i++)
			apps.add(shapes.get(i).getAppearance());
		List<Appearance> ordered = new ArrayList<Appearance>(apps);
		for (int i = 0; i < ct; i++)
			indices[i] = ordered.indexOf(shapes.get(i).getAppearance());
		
		sort(indices, shapes);
	}
	
	private void sort(int[] indices, Bag<Shape> parallel) {
		Object[] e = parallel.elements();
		quickSort(indices, e, 0, parallel.size());
	}
	
	// Sort both k and x, based on the keys in k, using the quick sort algorithm
	private static void quickSort(int[] k, Object[] x, int off, int len) {
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

		return (sa < sb ? (sb < sc ? b : (sa < sc ? c : a)) : (sb > sc ? b : (sa > sc ? c : a)));
	}

	private static void vecswap(int[] k, Object x[], int a, int b, int n) {
		for (int i = 0; i < n; i++, a++, b++)
			swap(k, x, a, b);
	}

	private static void swap(int[] k, Object[] x, int a, int b) {
		Object t = x[a];
		x[a] = x[b];
		x[b] = t;

		int v = k[a];
		k[a] = k[b];
		k[b] = v;
	}
	
	private class Sorter implements Comparator<Appearance> {
		@Override
		public int compare(Appearance o1, Appearance o2) {
			FixedFunctionAppearance a1 = compositor.get(o1);
			FixedFunctionAppearance a2 = compositor.get(o2);

			if (a1 == a2)
				return 0;
			if (a1.isLightingEnabled() != a2.isLightingEnabled())
				return a1.isLightingEnabled() ? 1 : -1;
			if (a1.isShadowReceiver() != a2.isShadowReceiver())
				return a1.isShadowReceiver() ? 1 : -1;

			return a1.hashCode() - a2.hashCode();
		}
	}
}
