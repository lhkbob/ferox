package com.ferox.renderer;

import java.util.Arrays;
import java.util.Comparator;

import org.openmali.vecmath.Vector3f;

/**
 * Extends BasicRenderQueue to allow for two different types of depth sorting:
 * front-to-back (useful for opaque render atoms), or back-to-front (necessary
 * for correct blending of transparent atoms).
 * 
 * @author Michael Ludwig
 * 
 */
public class DepthSortingRenderQueue extends BasicRenderQueue {
	private static class DepthSorter implements Comparator<RenderAtom> {
		private Vector3f pointOfReference;
		private final boolean forwardBack;

		public DepthSorter(boolean forwardBack) {
			this.forwardBack = forwardBack;
		}

		public int compare(RenderAtom ra1, RenderAtom ra2) {
			Vector3f t1 = ra1.getTransform().getTranslation();
			Vector3f t2 = ra2.getTransform().getTranslation();

			float d = distanceSquared(pointOfReference, t1)
							- distanceSquared(pointOfReference, t2);

			if (forwardBack) {
				if (d < 0) {
					return -1;
				} else if (d > 0) {
					return 1;
				} else {
					return 0;
				}
			} else {
				if (d < 0) {
					return 1;
				} else if (d > 0) {
					return -1;
				} else {
					return 0;
				}
			}
		}

		private static float distanceSquared(Vector3f v1, Vector3f v2) {
			return (v1.x - v2.x) * (v1.x - v2.x) + (v1.y - v2.y)
							* (v1.y - v2.y) + (v1.z - v2.z) * (v1.z - v2.z);
		}
	}

	private final DepthSorter sorter;

	/**
	 * Create a RenderQueue with the given type of sorting.
	 * 
	 * @param forwardBack True if atoms are sorted so that closer atoms are
	 *            rendered first
	 */
	public DepthSortingRenderQueue(boolean forwardBack) {
		sorter = new DepthSorter(forwardBack);
	}

	@Override
	protected void optimizeOrder(View view, RenderAtom[] atoms, int count) {
		sorter.pointOfReference = view.getLocation();
		Arrays.sort(atoms, 0, count, sorter);
		sorter.pointOfReference = null;
	}
}
