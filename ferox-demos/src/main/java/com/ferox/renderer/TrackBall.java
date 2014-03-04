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
package com.ferox.renderer;

import com.ferox.math.*;

/**
 *
 */
public class TrackBall {
    private final static Matrix3 IDENT = new Matrix3().setIdentity();

    private final boolean reverse;
    private final Vector3 lastPos;

    private final Matrix3 transform;

    public TrackBall(boolean reverse) {
        this.reverse = reverse;
        lastPos = new Vector3();
        transform = new Matrix3().setIdentity();
    }

    public void startDrag(double nx, double ny) {
        lastPos.set(nx, ny, 0.0);
        double nz = 1.0 - nx * nx - ny * ny;
        if (nz > 0) {
            lastPos.z = Math.sqrt(nz);
        } else {
            lastPos.normalize();
        }
    }

    public void drag(double nx, double ny) {
        drag(nx, ny, IDENT);
    }

    public void drag(double nx, double ny, @Const Matrix3 view) {
        Vector3 current = new Vector3(nx, ny, 0);
        double nz = 1.0 - nx * nx - ny * ny;
        if (nz > 0) {
            current.z = Math.sqrt(nz);
        } else {
            current.normalize();
        }

        Vector3 axis = new Vector3().cross(lastPos, current);
        axis.mul(view, axis);
        double angle = Math.asin(axis.length());
        if (reverse) {
            angle *= -1;
        }

        Matrix3 rot = new Matrix3().set(new Quat4().setAxisAngle(axis, angle));
        transform.mul(rot, transform);
        lastPos.set(current);
    }

    public Matrix3 getRotation() {
        return transform;
    }

    public Matrix4 getTransform() {
        return new Matrix4().setIdentity().setUpper(transform);
    }
}
