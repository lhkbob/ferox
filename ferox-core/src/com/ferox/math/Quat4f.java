package com.ferox.math;

public class Quat4f implements Cloneable {
    public float x;
    public float y;
    public float z;
    public float w;
    
    public Quat4f add(Quat4f q) {
        return add(q, this);
    }
    
    public Quat4f add(Quat4f q, Quat4f result) {
        return add(q.x, q.y, q.z, q.w, result);
    }
    
    public Quat4f add(float x, float y, float z, float w, Quat4f result) {
        if (result == null)
            result = new Quat4f();
        return result.set(this.x + x, this.y + y, this.z + z, this.w + w);
    }
    
    public Quat4f sub(Quat4f q) {
        return sub(q, this);
    }
    
    public Quat4f sub(Quat4f q, Quat4f result) {
        return sub(q.x, q.y, q.z, q.w, result);
    }
    
    public Quat4f sub(float x, float y, float z, float w, Quat4f result) {
        if (result == null)
            result = new Quat4f();
        return result.set(this.x - x, this.y - y, this.z - z, this.w - w);
    }
    
    public Quat4f mul(Quat4f q) {
        return mul(q, this);
    }
    
    public Quat4f mul(Quat4f q, Quat4f result) {
        return mul(q.x, q.y, q.z, q.w, result);
    }
    
    public Quat4f mul(float x, float y, float z, float w, Quat4f result) {
        if (result == null)
            result = new Quat4f();
        return result.set(this.w * x + this.x * w + this.y * z - this.z * y,
                          this.w * y + this.y * w + this.z * x - this.x * z,
                          this.w * z + this.z * w + this.x * y - this.y * x,
                          this.w * w - this.x * x - this.y * y - this.z * z);
    }
    
    public Quat4f mul(Vector3f v) {
        return mul(v, this);
    }
    
    public Quat4f mul(Vector3f v, Quat4f result) {
        if (result == null)
            result = new Quat4f();
        
        return result.set(w * v.x + y * v.z - z * v.y,
                          w * v.y + z * v.x - x * v.z,
                          w * v.z + x * v.y - y * v.x,
                          -x * v.x - y * v.y - z * v.z);
    }
    
    public Vector3f rotate(Vector3f v, Vector3f result) {
        // FIXME: generates garbage
        Quat4f q = mul(v, null).mul(inverse(null));
        if (result == null)
            return new Vector3f(q.x, q.y, q.z);
        else
            return result.set(q.x, q.y, q.z);
    }
    
    public Quat4f scale(float s) {
        return scale(s, this);
    }
    
    public Quat4f scale(float s, Quat4f result) {
        if (result == null)
            result = new Quat4f();
        return result.set(s * x, s * y, s * z, s * w);
    }
    
    public Quat4f normalize() {
        return normalize(this);
    }
    
    public Quat4f normalize(Quat4f result) {
        return scale(1f / length(), result);
    }
    
    public float length() {
        return (float) Math.sqrt(lengthSquared());
    }
    
    public float lengthSquared() {
        return x * x + y * y + z * z + w * w;
    }
    
    public float dot(Quat4f q) {
        return x * q.x + y * q.y + z * q.z + w * q.w;
    }
    
    public float dot(float x, float y, float z, float w) {
        return this.x * x + this.y * y +  this.z * z + this.w * w;
    }
    
    public float getAxisAngle() {
        return (float) (2 * Math.acos(w));
    }
    
    public Vector3f getAxis() {
        return getAxis(null);
    }
    
    public Vector3f getAxis(Vector3f result) {
        if (result == null)
            result = new Vector3f();
        
        float s2 = 1f - w * w;
        if (s2 < .0001f)
            return result.set(1f, 0f, 0f); // arbitrary
        
        float s = (float) Math.sqrt(s2);
        return result.set(x / s, y / s, z / s);
    }
    
    public float angle(Quat4f q) {
        return angle(q.x, q.y, q.z, q.w);
    }
    
    public float angle(float x, float y, float z, float w) {
        float s = (float) Math.sqrt(lengthSquared() * (x * x + y * y + z * z + w * w));
        if (s == 0f)
            throw new ArithmeticException("Undefined angle between two quaternions");
        
        return (float) Math.acos(dot(x, y, z, w) / s);
    }
    
    public Quat4f inverse() {
        return inverse(this);
    }
    
    public Quat4f inverse(Quat4f result) {
        if (result == null)
            result = new Quat4f();
        return result.set(-x, -y, -z, w);
    }
    
    public Quat4f slerp(Quat4f q, float t) {
        return slerp(q, t, this);
    }
    
    public Quat4f slerp(Quat4f q, float t, Quat4f result) {
        if (result == null)
            result = new Quat4f();
        
        float theta = angle(q);
        if (theta != 0f) {
            float d = 1f / (float) Math.sin(theta);
            float s0 = (float) Math.sin((1 - t) * theta);
            float s1 = (float) Math.sin(t * theta);
            
            if (dot(q) < 0) // long angle case, see http://en.wikipedia.org/wiki/Slerp
                s1 *= -1;
            
            return result.set((x * s0 + q.x * s1) * d,
                              (y * s0 + q.y * s1) * d,
                              (z * s0 + q.z * s1) * d,
                              (w * s0 + q.w * s1) * d);
        } else
            return result.set(this);
    }
    
    public Quat4f setIdentity() {
        return set(0f, 0f, 0f, 1f);
    }
                                
    public Quat4f setAxisAngle(Vector3f axis, float angle) {
        float d = axis.length();
        float s = (float) Math.sin(.5f * angle) / d;
        return set(axis.x * s, axis.y * s, axis.z * s, (float) Math.cos(.5f * angle));
    }
    
    // yaw = angle around y
    // pitch = angle around x
    // roll = angle around z
    public Quat4f setEuler(float yaw, float pitch, float roll) {
        float cosYaw = (float) Math.cos(.5f * yaw);
        float sinYaw = (float) Math.sin(.5f * yaw);
        
        float cosPitch = (float) Math.cos(.5f * pitch);
        float sinPitch = (float) Math.sin(.5f * pitch);
        
        float cosRoll = (float) Math.cos(.5f * roll);
        float sinRoll = (float) Math.sin(.5f * roll);
        
        return set(cosRoll * sinPitch * cosYaw + sinRoll * cosPitch * sinYaw,
                   cosRoll * cosPitch * sinYaw - sinRoll * sinPitch * cosYaw,
                   sinRoll * cosPitch * cosYaw - cosRoll * sinPitch * sinYaw,
                   cosRoll * cosPitch * cosYaw + sinRoll * sinPitch * sinYaw);
    }
    
    // FIXME: figure out what quatRotate, and the vector mul operations are for
    // FIXME: add the shortestArcQuat(vec, vec) to the Vector3f class
    // FIXME: document
    // FIXME: tuple4f/3f?
    
    public Quat4f set(Matrix3f e) {
        float trace = e.m00 + e.m11 + e.m22;
        if (trace > 0f) {
            float s = (float) Math.sqrt(trace + 1f);
            w = .5f * s;
            s = .5f / s;
            
            x = (e.m21 - e.m12) * s;
            y = (e.m02 - e.m20) * s;
            z = (e.m10 - e.m01) * s;
        } else {
            int i = (e.m00 < e.m11 ? (e.m11 < e.m22 ? 2 : 1)
                                   : (e.m00 < e.m22 ? 2 : 0));
            int j = (i + 1) % 3;
            int k = (i + 2) % 3;
            
            float s = (float) Math.sqrt(e.get(i, i) - e.get(j, j) - e.get(k, k) + 1f);
            set(i, .5f * s);
            s = .5f / s;
            
            set(3, (e.get(k, j) - e.get(j, k)) * s);
            set(j, (e.get(j, i) + e.get(i, j)) * s);
            set(k, (e.get(k, i) + e.get(i, k)) * s);
        }
        
        return this;
    }
    
    @Override
    public Quat4f clone() {
        try {
            return (Quat4f) super.clone();
        } catch (CloneNotSupportedException e) {
            // shouldn't happen since Quat4f implements Cloneable
            throw new UnsupportedOperationException(e);
        }
    }

    /**
     * Get the given component from this quaternion; index must be 0 (x), 1 (y),
     * 2 (z), or 3 (w)
     * 
     * @param index The quaternion component to retrieve
     * @return The component at the given index
     * @throws IndexOutOfBoundsException if index is invalid
     */
    public float get(int index) {
        switch (index) {
        case 0:
            return x;
        case 1:
            return y;
        case 2:
            return z;
        case 3:
            return w;
        default:
            throw new IndexOutOfBoundsException("Index must be in [0, 3]");
        }
    }

    /**
     * Store the four component values of this quaternion into vals, starting at
     * offset. The components should be placed consecutively, ordered x, y, z,
     * and w. It is assumed that the array has at least four positions
     * available, starting at offset.
     * 
     * @param vals Array to store this quaternion in
     * @param offset First array index to hold the x value
     * @throws NullPointerException if vals is null
     * @throws ArrayIndexOutOfBoundsException if there isn't enough room to
     *             store this vector at offset
     */
    public void get(float[] vals, int offset) {
        vals[offset] = x;
        vals[offset + 1] = y;
        vals[offset + 2] = z;
        vals[offset + 3] = w;
    }

    /**
     * Set the quaternion coordinate at index to the given value. index must be
     * one of 0 (x), 1 (y), 2 (z), or 3 (w).
     * 
     * @param index Coordinate to modify
     * @param val New value for coordinate
     * @return This quaternion
     * @throws IndexOutOfBoundsException if index is invalid
     */
    public Quat4f set(int index, float val) {
        switch (index) {
        case 0:
            x = val;
            break;
        case 1:
            y = val;
            break;
        case 2:
            z = val;
            break;
        case 3:
            w = val;
            break;
        default:
            throw new IndexOutOfBoundsException("Index must be in [0, 3]");
        }

        return this;
    }

    /**
     * Set the x, y, z, and w values of this Quat4f to the values held in q.
     * 
     * @param q Quaternion to be copied into this
     * @return This quaternion
     * @throws NullPointerException if q is null
     */
    public Quat4f set(Quat4f q) {
        return set(q.x, q.y, q.z, q.w);
    }

    /**
     * Set the x, y, z, and w values of this Quat4f to the given four
     * coordinates.
     * 
     * @param x New x coordinate
     * @param y New y coordinate
     * @param z New z coordinate
     * @param w New w coordinate
     * @return This quaternion
     */
    public Quat4f set(float x, float y, float z, float w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;

        return this;
    }

    /**
     * Set the x, y, z and w values of this Quat4f to the four values held
     * within the vals array, starting at offset.
     * 
     * @param vals Array to take 4 component values from
     * @param offset Index of the x coordinate
     * @return This quaternion
     * @throws ArrayIndexOutOfBoundsException if vals doesn't have four values
     *             starting at offset
     */
    public Quat4f set(float[] vals, int offset) {
        return set(vals[offset], vals[offset + 1], vals[offset + 2], vals[offset + 3]);
    }

    @Override
    public String toString() {
        return "[" + x + ", " + y + ", " + z + ", " + w + "]";
    }

    @Override
    public int hashCode() {
        int result = 17;

        result += 31 * result + Float.floatToIntBits(x);
        result += 31 * result + Float.floatToIntBits(y);
        result += 31 * result + Float.floatToIntBits(z);
        result += 31 * result + Float.floatToIntBits(w);

        return result;
    }

    @Override
    public boolean equals(Object v) {
        // this conditional correctly handles null values
        if (!(v instanceof Quat4f))
            return false;
        else
            return equals((Quat4f) v);
    }

    /**
     * Return true if these two quaternions are numerically equal. Returns false if
     * v is null.
     * 
     * @param v Vector to test equality with
     * @return True if these quaternions are numerically equal
     */
    public boolean equals(Vector4f v) {
        return v != null && x == v.x && y == v.y && z == v.z && w == v.w;
    }

    /**
     * Determine if these two quaternions are equal, within an error range of eps.
     * Returns false if q is null.
     * 
     * @param q Quaternion to check approximate equality to
     * @param eps Error tolerance of each component
     * @return True if all component values are within eps of the corresponding
     *         component of q
     */
    public boolean epsilonEquals(Quat4f q, float eps) {
        if (q == null)
            return false;

        float tx = Math.abs(x - q.x);
        float ty = Math.abs(y - q.y);
        float tz = Math.abs(z - q.z);
        float tw = Math.abs(w - q.w);

        return tx <= eps && ty <= eps && tz <= eps && tw <= eps;
    }
}
