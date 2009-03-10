package com.ferox.math;

import org.openmali.vecmath.Vector3f;

import com.ferox.renderer.View;
import com.ferox.renderer.View.FrustumIntersection;

/**
 * A BoundVolume represents a partitioning of space into what's outside and 
 * what's inside the BoundVolume.  Its main purpose is used for efficient view
 * culling and picking, as well as spatial queries.
 * 
 * @author Michael Ludwig
 *
 */
public interface BoundVolume {
	/** Clone this BoundVolume into result.  If result is not the same type, or is null, create a new instance of the correct type
	 * and return that. */
	public BoundVolume clone(BoundVolume result);
	
	/** Grow this BoundVolume to completely enclose the given BoundVolume. Should throw an exception if it doesn't know how to
	 * enclose the volume. Do nothing if toEnclose is null, or if it has no vertices, preserving previous bounds. */
	public void enclose(BoundVolume toEnclose) throws UnsupportedOperationException;
	
	/** Enclose the given set of vertices.  For most cases, one should use Boundable.getBounds() because the boundable
	 * may cache computations for faster results, but this method can be used by Boundables or to compute an enclosure
	 * from scratch. Do nothing if vertices is null. */
	public void enclose(Boundable vertices);
	
	/** Test for intersection between this and another BoundVolume.  Should throw an exception if it can't
	 * compute an intersection. Return false if other is null. */
	public boolean intersects(BoundVolume other) throws UnsupportedOperationException;
	
	/** Apply the given transform to this BoundVolume.  Do nothing if trans is null. */
	public void applyTransform(Transform trans);
	
	/** Test this BoundVolume against the view frustum.  Should fail if the view is null.
	 * Implementations can assume that the view's world cache's are up-to-date. */
	public FrustumIntersection testFrustum(View view) throws NullPointerException;
	
	/** Compute the farthest extent of this volume along the given direction vector and store it in result.  
	 * If reverse is true, instead find the extent in the opposite direction of dir.
	 * Fail if dir is null.  If result is null, create a new instance, should return result. */
	public Vector3f getExtent(Vector3f dir, boolean reverse, Vector3f result) throws NullPointerException;
		
	/** Compute the enclosure of this and toEnclose and store it in result.  If result is of the wrong type or is null, create a new
	 * instance of the correct type and return that. Store itself in result if toEnclose is null. */
	public BoundVolume enclose(BoundVolume toEnclose, BoundVolume result);
	
	/** Store the transformation of this bound volume into result.  If result is of the wrong type or is null, create a new
	 * instance of the correct type and return that.  Store itself in result if trans is null. */
	public BoundVolume applyTransform(Transform trans, BoundVolume result);
	
	/** Store the enclosure of this and vertices into result.  If result is the wrong type or is null, create a new
	 * instance of the correct type and return that.  Store itself in result if vertices is null. */
	public BoundVolume enclose(Boundable vertices, BoundVolume result);
}
