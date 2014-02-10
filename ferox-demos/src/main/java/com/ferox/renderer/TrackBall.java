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
