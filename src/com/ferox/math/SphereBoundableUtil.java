package com.ferox.math;

import org.openmali.vecmath.Vector3f;

/**
 * A utility class to compute enclosing BoundSphere's for Boundables. This is to
 * be used for Boundables when they implement their getBounds() method. All that
 * is necessary is for them to correctly implement their getVertex() and
 * getVertexCount() methods.
 * 
 * @author Michael Ludwig
 * 
 */
public class SphereBoundableUtil {
	private static final float radiusEpsilon = 1.00001f;

	/**
	 * Utility method to store enclose vertices in a BoundSphere, using the
	 * recurse minimum algorithm. Does nothing if box or vertices is null, or if
	 * vertices has a vertex count of 0.
	 * 
	 * @param vertices The Boundable whose bounding sphere will be computed
	 * @param sphere The BoundSphere who will hold the computed results
	 */
	public static void getBounds(Boundable vertices, BoundSphere sphere) {
		if (vertices == null || vertices.getVertexCount() == 0)
			return;

		float[] points = new float[vertices.getVertexCount() * 3];
		fillPointsArray(points, vertices);

		// thanks to JME for the algorithm, adapted to use float[]
		recurseMini(sphere, points, points.length / 3, 0, 0);
	}

	private static void recurseMini(BoundSphere sphere, float[] points, int p,
			int b, int ap) {
		Vector3f tempA = SphereBoundableUtil.tempA.get();
		Vector3f tempB = SphereBoundableUtil.tempB.get();
		Vector3f tempC = SphereBoundableUtil.tempC.get();
		Vector3f tempD = SphereBoundableUtil.tempD.get();

		Vector3f center = sphere.getCenter();

		switch (b) {
		case 0:
			sphere.setRadius(0f);
			sphere.getCenter().set(0f, 0f, 0f);
			break;
		case 1:
			sphere.setRadius(1f - radiusEpsilon);
			populateFromArray(center, points, ap - 1);
			break;
		case 2:
			populateFromArray(tempA, points, ap - 1);
			populateFromArray(tempB, points, ap - 2);

			setSphere(sphere, tempA, tempB);
			break;
		case 3:
			populateFromArray(tempA, points, ap - 1);
			populateFromArray(tempB, points, ap - 2);
			populateFromArray(tempC, points, ap - 3);

			setSphere(sphere, tempA, tempB, tempC);
			break;
		case 4:
			populateFromArray(tempA, points, ap - 1);
			populateFromArray(tempB, points, ap - 2);
			populateFromArray(tempC, points, ap - 3);
			populateFromArray(tempD, points, ap - 4);

			setSphere(sphere, tempA, tempB, tempC, tempD);
			return;
		}
		for (int i = 0; i < p; i++) {
			populateFromArray(tempA, points, i + ap);
			float d = ((tempA.x - center.x) * (tempA.x - center.x)
					+ (tempA.y - center.y) * (tempA.y - center.y) + (tempA.z - center.z)
					* (tempA.z - center.z));
			if (d - (sphere.getRadius() * sphere.getRadius()) > radiusEpsilon - 1f) {
				for (int j = i; j > 0; j--) {
					populateFromArray(tempB, points, j + ap);
					populateFromArray(tempC, points, j - 1 + ap);
					setInArray(tempC, points, j + ap);
					setInArray(tempB, points, j - 1 + ap);
				}
				recurseMini(sphere, points, i, b + 1, ap + 1);
			}
		}
	}

	private static void populateFromArray(Vector3f p, float[] points, int index) {
		p.x = points[index * 3];
		p.y = points[index * 3 + 1];
		p.z = points[index * 3 + 2];
	}

	private static void setInArray(Vector3f p, float[] points, int index) {
		if (p == null) {
			points[index * 3] = 0f;
			points[index * 3 + 1] = 0f;
			points[index * 3 + 2] = 0f;
		} else {
			points[index * 3] = p.x;
			points[index * 3 + 1] = p.y;
			points[index * 3 + 2] = p.z;
		}
	}

	private static void setSphere(BoundSphere sphere, Vector3f o, Vector3f a,
			Vector3f b, Vector3f c) {
		Vector3f tA = SphereBoundableUtil.tA.get();
		Vector3f tB = SphereBoundableUtil.tB.get();
		Vector3f tC = SphereBoundableUtil.tC.get();
		Vector3f tD = SphereBoundableUtil.tD.get();
		Vector3f cross = SphereBoundableUtil.cross.get();

		tA.sub(a, o);
		tB.sub(b, o);
		tC.sub(c, o);

		float denom = 2.0f * (tA.x * (tB.y * tC.z - tC.y * tB.z) - tB.x
				* (tA.y * tC.z - tC.y * tA.z) + tC.x
				* (tA.y * tB.z - tB.y * tA.z));
		if (denom == 0) {
			sphere.getCenter().set(0f, 0f, 0f);
			sphere.setRadius(0f);
		} else {
			cross.cross(tA, tB);
			cross.scale(tC.lengthSquared());
			tD.cross(tC, tA);
			tD.scale(tB.lengthSquared());
			cross.add(tD);
			tD.cross(tB, tC);
			tD.scale(tA.lengthSquared());
			cross.add(tD);
			cross.scale(1f / denom);

			sphere.setRadius(cross.length() * radiusEpsilon);
			sphere.getCenter().add(o, cross);
		}
	}

	private static void setSphere(BoundSphere sphere, Vector3f o, Vector3f a,
			Vector3f b) {
		Vector3f tA = SphereBoundableUtil.tA.get();
		Vector3f tB = SphereBoundableUtil.tB.get();
		Vector3f tC = SphereBoundableUtil.tC.get();
		Vector3f tD = SphereBoundableUtil.tD.get();
		Vector3f cross = SphereBoundableUtil.cross.get();

		tA.sub(a, o);
		tB.sub(b, o);
		cross.cross(tA, tB);

		float denom = 2f * cross.lengthSquared();

		if (denom == 0) {
			sphere.getCenter().set(0f, 0f, 0f);
			sphere.setRadius(0f);
		} else {
			tC.cross(cross, tA);
			tC.scale(tB.lengthSquared());
			tD.cross(tB, cross);
			tD.scale(tA.lengthSquared());
			tC.add(tD);
			tC.scale(1f / denom);

			sphere.setRadius(tC.length() * radiusEpsilon);
			sphere.getCenter().add(o, tC);
		}
	}

	private static void setSphere(BoundSphere sphere, Vector3f o, Vector3f a) {
		sphere.setRadius((float) Math.sqrt(((a.x - o.x) * (a.x - o.x)
				+ (a.y - o.y) * (a.y - o.y) + (a.z - o.z) * (a.z - o.z)) / 4f)
				+ radiusEpsilon - 1f);

		Vector3f center = sphere.getCenter();
		center.scale(.5f, o);
		center.scaleAdd(.5f, a, center);
	}

	private static void fillPointsArray(float[] points, Boundable verts) {
		int vertexCount = verts.getVertexCount();

		for (int i = 0; i < vertexCount; i++) {
			points[i * 3] = verts.getVertex(i, 0);
			points[i * 3 + 1] = verts.getVertex(i, 1);
			points[i * 3 + 2] = verts.getVertex(i, 2);
		}
	}

	// used in recurseMini
	private static final ThreadLocal<Vector3f> tempA = new ThreadLocal<Vector3f>() {
		@Override
		protected Vector3f initialValue() {
			return new Vector3f();
		}
	};
	private static final ThreadLocal<Vector3f> tempB = new ThreadLocal<Vector3f>() {
		@Override
		protected Vector3f initialValue() {
			return new Vector3f();
		}
	};
	private static final ThreadLocal<Vector3f> tempC = new ThreadLocal<Vector3f>() {
		@Override
		protected Vector3f initialValue() {
			return new Vector3f();
		}
	};
	private static final ThreadLocal<Vector3f> tempD = new ThreadLocal<Vector3f>() {
		@Override
		protected Vector3f initialValue() {
			return new Vector3f();
		}
	};

	// used exclusively in setSphere methods
	private static final ThreadLocal<Vector3f> tA = new ThreadLocal<Vector3f>() {
		@Override
		protected Vector3f initialValue() {
			return new Vector3f();
		}
	};
	private static final ThreadLocal<Vector3f> tB = new ThreadLocal<Vector3f>() {
		@Override
		protected Vector3f initialValue() {
			return new Vector3f();
		}
	};
	private static final ThreadLocal<Vector3f> tC = new ThreadLocal<Vector3f>() {
		@Override
		protected Vector3f initialValue() {
			return new Vector3f();
		}
	};
	private static final ThreadLocal<Vector3f> tD = new ThreadLocal<Vector3f>() {
		@Override
		protected Vector3f initialValue() {
			return new Vector3f();
		}
	};

	// used in setSphere
	private static final ThreadLocal<Vector3f> cross = new ThreadLocal<Vector3f>() {
		@Override
		protected Vector3f initialValue() {
			return new Vector3f();
		}
	};
}
