/**
 * 
 */
package com.ferox.math.bounds;

/**
 * PlaneState keeps track of the intermediate test results of a BoundVolume's
 * testFrustum() method. It can be used to reduce the number of plane checks
 * during the testing of a hierarchy of BoundVolumes.
 * 
 * @author Michael Ludwig
 */
public class PlaneState {
	private int planeBits;
	
	/**
	 * <p>
	 * Return true if the given plane is required for testing during a
	 * testFrustum() call.
	 * </p>
	 * <p>
	 * If false is returned, it can be assumed that a higher BoundVolume in a
	 * hierarchy was completely on the inside of the plane and so further checks
	 * are unnecessary.
	 * </p>
	 * 
	 * @param plane The plane to test (assumed to be in [0, 5]).
	 * @return True if the plane should be tested
	 */
	public boolean isTestRequired(int plane) {
		return ((1 << plane) & planeBits) == 0;
	}
	
	/**
	 * <p>
	 * Set whether or not the given plane must be tested during subsequent
	 * testFrustum() calls on a BoundVolume. A value of false should only be
	 * assigned if it's known that a BoundVolume is guaranteed to be on the
	 * inside of the given plane. If not, incorrect values may be returned from
	 * testFrustum().
	 * </p>
	 * 
	 * @param plane The plane to assign the required boolean to
	 * @param required Whether or not the given plane requires testing
	 */
	public void setTestRequired(int plane, boolean required) {
		if (required)
			planeBits &= ~(1 << plane);
		else
			planeBits |= (1 << plane);
	}
	
	/**
	 * Adjust this PlaneState to use the given bit set of frustum planes. If a 1
	 * is stored at a given bit, then that plane does not require testing. Any
	 * bits assigned outside of 0 through 5 will be ignored.
	 * 
	 * @param planeBits The bitwise OR set of planes
	 */
	public void set(int planeBits) {
		this.planeBits = planeBits;
	}
	
	/** 
	 * @return The current plane bit set used for this PlaneState
	 */
	public int get() {
		return planeBits;
	}
	
	/**
	 * Reset this PlaneState so that all planes require testing.
	 */
	public void reset() {
		planeBits = 0;
	}
}