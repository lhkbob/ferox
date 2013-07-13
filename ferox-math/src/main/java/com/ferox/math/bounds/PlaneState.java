/*
 * Ferox, a graphics and game library in Java
 *
 * Copyright (c) 2012, Michael Ludwig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright notice,
 *         this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright notice,
 *         this list of conditions and the following disclaimer in the
 *         documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.ferox.math.bounds;

/**
 * PlaneState keeps track of the intermediate test results of a BoundVolume's testFrustum() method. It can be
 * used to reduce the number of plane checks during the testing of a hierarchy of BoundVolumes.
 *
 * @author Michael Ludwig
 */
public class PlaneState {
    private static final int PLANE_MASK = 0x3f;

    private int planeBits;

    /**
     * @return True if any plane returns true from {@link #isTestRequired(int)}
     */
    public boolean getTestsRequired() {
        return (planeBits & PLANE_MASK) != PLANE_MASK;
    }

    /**
     * <p/>
     * Return true if the given plane is required for testing during a testFrustum() call.
     * <p/>
     * If false is returned, it can be assumed that a higher BoundVolume in a hierarchy was completely on the
     * inside of the plane and so further checks are unnecessary.
     *
     * @param plane The plane to test (assumed to be in [0, 5]).
     *
     * @return True if the plane should be tested
     */
    public boolean isTestRequired(int plane) {
        return ((1 << plane) & planeBits) == 0;
    }

    /**
     * <p/>
     * Set whether or not the given plane must be tested during subsequent testFrustum() calls on a
     * BoundVolume. A value of false should only be assigned if it's known that a BoundVolume is guaranteed to
     * be on the inside of the given plane. If not, incorrect values may be returned from testFrustum().
     *
     * @param plane    The plane to assign the required boolean to
     * @param required Whether or not the given plane requires testing
     */
    public void setTestRequired(int plane, boolean required) {
        if (required) {
            planeBits &= ~(1 << plane);
        } else {
            planeBits |= (1 << plane);
        }
    }

    /**
     * Adjust this PlaneState to use the given bit set of frustum planes. If a 1 is stored at a given bit,
     * then that plane does not require testing. Any bits assigned outside of 0 through 5 will be ignored.
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