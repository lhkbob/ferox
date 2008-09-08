package com.ferox.core.scene;

import java.nio.FloatBuffer;

import org.openmali.vecmath.*;

import com.ferox.core.util.io.Chunkable;
import com.ferox.core.util.io.InputChunk;
import com.ferox.core.util.io.OutputChunk;

public class Transform implements Chunkable {
	private static final Transform IDENTITY = new Transform();
	private static final Vector3f temp = new Vector3f();
	
	private final Matrix3f rot;
	private final Vector3f trans;
	private final Vector3f scale;
	
	public Transform() {
		this.rot = new Matrix3f();
		this.rot.setIdentity();
		this.trans = new Vector3f();
		this.scale = new Vector3f(1f, 1f, 1f);
	}
	
	public Transform(Vector3f trans) {
		this();
		this.setTranslation(trans);
	}
	
	public Transform(Vector3f trans, Vector3f scale) {
		this();
		this.setTranslation(trans);
		this.setScale(scale);
	}
	
	public Transform(Vector3f trans, Matrix3f rot) {
		this();
		this.setTranslation(trans);
		this.setRotation(rot);
	}
	
	public Transform(Vector3f trans, Matrix3f rot, Vector3f scale) {
		this();
		this.setTranslation(trans);
		this.setScale(scale);
		this.setRotation(rot);
	}
	
	public Vector3f getTranslation() {
		return this.trans;
	}
	
	public Vector3f getScale() {
		return this.scale;
	}
	
	public Matrix3f getRotation() {
		return this.rot;
	}
	
	public void setTranslation(Vector3f t) {
		if (t == null)
			this.trans.set(0f, 0f, 0f);
		else
			this.trans.set(t);
	}
	
	public void setTranslation(float x, float y, float z) {
		this.trans.set(x, y, z);
	}
	
	public void setScale(Vector3f s) {
		if (s == null)
			this.scale.set(1f, 1f, 1f);
		else
			this.scale.set(s);
	}
	
	public void setScale(float sx, float sy, float sz) {
		this.scale.set(sx, sy, sz);
	}
	
	public void setRotation(Matrix3f rot) {
		if (rot == null)
			this.rot.setIdentity();
		else
			this.rot.set(rot);
	}
	
	public void setRotation(Quat4f rot) {
		if (rot == null)
			this.rot.setIdentity();
		else
			this.rot.set(rot);
	}
	
	public void setRotation(AxisAngle4f rot) {
		if (rot == null)
			this.rot.setIdentity();
		else
			this.rot.set(rot);
	}
	
	public Quat4f getRotation(Quat4f rot) {
		if (rot == null)
			rot = new Quat4f();
		rot.set(this.rot);
		return rot;
	}
	
	public AxisAngle4f getRotation(AxisAngle4f rot) {
		if (rot == null)
			rot = new AxisAngle4f();
		rot.set(this.rot);
		return rot;
	}
	
	public Transform mul(Transform t1, Transform t2) throws NullPointerException {
		if (t1 == null || t2 == null)
			throw new NullPointerException("Can't multiply null transforms");
		t1.transform(t2.trans, temp);
		this.trans.set(temp);
		this.scale.set(t1.scale.x * t2.scale.x, t1.scale.y * t2.scale.y, t1.scale.z * t2.scale.z);
		this.rot.mul(t1.rot, t2.rot);
		return this;
	}
	
	public void inverse() {
		this.inverse(this);
	}
	
	public Transform inverse(Transform t) throws NullPointerException {
		if (t == null)
			throw new NullPointerException("Can't inverse a null transform");
		this.inverseMul(t, Transform.IDENTITY);
		return this;
	}
	
	public Transform inverseMul(Transform ti, Transform tn) throws NullPointerException {
		if (ti == null || tn == null)
			throw new NullPointerException("Can't inverse multiply null transforms");
		
		ti.inverseTransform(tn.trans, temp);
		this.trans.set(temp);
		this.scale.set(tn.scale.x / ti.scale.x , tn.scale.y / ti.scale.y, tn.scale.z / ti.scale.z);
		this.rot.mulTransposeLeft(ti.rot, tn.rot);
		return this;
	}
	
	public void transform(Vector3f t) {
		this.transform(t, t);
	}
	
	public Vector3f transform(Vector3f t, Vector3f result) {
		if (t == null)
			throw new NullPointerException("Can't transform a null vector");
		if (result == null)
			result = new Vector3f();
		if (result == this.trans || result == this.scale)
			throw new IllegalArgumentException("Can't use this transform's vectors as a result");
		
		result.set(t.x * this.scale.x, t.y * this.scale.y, t.z * this.scale.z);
		this.rot.transform(result);
		result.add(this.trans);
		
		return result;
	}
	
	public void inverseTransform(Vector3f t) {
		this.inverseTransform(t, t);
	}
	
	public Vector3f inverseTransform(Vector3f t, Vector3f result) throws NullPointerException, IllegalArgumentException {
		if (t == null)
			throw new NullPointerException("Can't transform a null vector");
		if (result == null)
			result = new Vector3f();
		if (result == this.trans || result == this.scale)
			throw new IllegalArgumentException("Can't use this transform's vectors as a result");
		
		result.sub(t, this.trans);
		result.set(this.rot.m00 * result.x + this.rot.m10 * result.y + this.rot.m20 * result.z, 
				   this.rot.m01 * result.x + this.rot.m11 * result.y + this.rot.m21 * result.z,
				   this.rot.m02 * result.x + this.rot.m12 * result.y + this.rot.m22 * result.z);
		result.set(result.x / this.scale.x, result.y / this.scale.y, result.z / this.scale.z);
			
		return result;
	}
	
	public void set(Transform t) {
		if (t == null)
			this.setIdentity();
		else {
			this.rot.set(t.rot);
			this.scale.set(t.scale);
			this.trans.set(t.trans);
		}
	}
	
	public void setIdentity() {
		this.rot.setIdentity();
		this.scale.set(1f, 1f, 1f);
		this.trans.set(0f, 0f, 0f);
	}

	public void getOpenGLMatrix(FloatBuffer matrix) {
		int pos = matrix.position();
		
		matrix.put(pos + 0, this.rot.m00 * this.scale.x);
		matrix.put(pos + 1, this.rot.m10 * this.scale.x);
		matrix.put(pos + 2, this.rot.m20 * this.scale.x);
		matrix.put(pos + 3, 0f);
		
		matrix.put(pos + 4, this.rot.m01 * this.scale.y);
		matrix.put(pos + 5, this.rot.m11 * this.scale.y);
		matrix.put(pos + 6, this.rot.m21 * this.scale.y);
		matrix.put(pos + 7, 0f);
		
		matrix.put(pos + 8, this.rot.m02 * this.scale.z);
		matrix.put(pos + 9, this.rot.m12 * this.scale.z);
		matrix.put(pos + 10, this.rot.m22 * this.scale.z);
		matrix.put(pos + 11, 0f);
		
		matrix.put(pos + 12, this.trans.x);
		matrix.put(pos + 13, this.trans.y);
		matrix.put(pos + 14, this.trans.z);
		matrix.put(pos + 15, 1f);
	}
	
	public void getOpenGLMatrix(float[] matrix, int offset) {
		matrix[offset + 0] = this.rot.m00 * this.scale.x;
		matrix[offset + 1] = this.rot.m10 * this.scale.x;
		matrix[offset + 2] = this.rot.m20 * this.scale.x;
		matrix[offset + 3] = 0f;
		
		matrix[offset + 4] = this.rot.m01 * this.scale.y;
		matrix[offset + 5] = this.rot.m11 * this.scale.y;
		matrix[offset + 6] = this.rot.m21 * this.scale.y;
		matrix[offset + 7] = 0f;
		
		matrix[offset + 8] = this.rot.m02 * this.scale.z;
		matrix[offset + 9] = this.rot.m12 * this.scale.z;
		matrix[offset + 10] = this.rot.m22 * this.scale.z;
		matrix[offset + 11] = 0f;
		
		matrix[offset + 12] = this.trans.x;
		matrix[offset + 13] = this.trans.y;
		matrix[offset + 14] = this.trans.z;
		matrix[offset + 15] = 1f;
	}
	
	public static void getOpenGLMatrix(Matrix4f matrix, FloatBuffer ogl) {
		int pos = ogl.position();
		
		ogl.put(pos + 0, matrix.m00);
		ogl.put(pos + 1, matrix.m10);
		ogl.put(pos + 2, matrix.m20);
		ogl.put(pos + 3, matrix.m30);
		
		ogl.put(pos + 4, matrix.m01);
		ogl.put(pos + 5, matrix.m11);
		ogl.put(pos + 6, matrix.m21);
		ogl.put(pos + 7, matrix.m31);
		
		ogl.put(pos + 8, matrix.m02);
		ogl.put(pos + 9, matrix.m12);
		ogl.put(pos + 10, matrix.m22);
		ogl.put(pos + 11, matrix.m32);
		
		ogl.put(pos + 12, matrix.m03);
		ogl.put(pos + 13, matrix.m13);
		ogl.put(pos + 14, matrix.m23);
		ogl.put(pos + 15, matrix.m33);
	}
	
	public static void getOpenGLMatrix(Matrix4f matrix, float[] ogl, int offset) {
		ogl[offset + 0] =  matrix.m00;
		ogl[offset + 1] =  matrix.m10;
		ogl[offset + 2] =  matrix.m20;
		ogl[offset + 3] =  matrix.m30;
		
		ogl[offset + 4] =  matrix.m01;
		ogl[offset + 5] =  matrix.m11;
		ogl[offset + 6] =  matrix.m21;
		ogl[offset + 7] =  matrix.m31;
		
		ogl[offset + 8] =  matrix.m02;
		ogl[offset + 9] =  matrix.m12;
		ogl[offset + 10] =  matrix.m22;
		ogl[offset + 11] =  matrix.m32;
		
		ogl[offset + 12] =  matrix.m03;
		ogl[offset + 13] =  matrix.m13;
		ogl[offset + 14] =  matrix.m23;
		ogl[offset + 15] =  matrix.m33;	
	}
	
	public void readChunk(InputChunk in) {
		this.trans.set(in.getFloatArray("trans"));
		this.scale.set(in.getFloatArray("scale"));
		AxisAngle4f aa = new AxisAngle4f();
		aa.set(in.getFloatArray("rot"));
		this.rot.set(aa);
	}

	public void writeChunk(OutputChunk out) {
		float[] t = new float[3];
		this.trans.get(t);
		out.setFloatArray("trans", t);
		float[] s = new float[3];
		this.scale.get(s);
		out.setFloatArray("scale", s);
		float[] m = new float[4];
		AxisAngle4f aa = new AxisAngle4f();
		aa.set(this.rot);
		aa.get(m);
		out.setFloatArray("rot", m);
	}
	
	public String toString() {
		AxisAngle4f a = new AxisAngle4f();
		a.set(this.rot);
		return "Transform: " + this.trans + " " + this.scale + " " + a;
	}
}
