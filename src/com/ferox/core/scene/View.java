package com.ferox.core.scene;

import org.openmali.vecmath.Matrix3f;
import org.openmali.vecmath.Matrix4f;
import org.openmali.vecmath.Vector3f;

import com.ferox.core.util.FeroxException;
import com.ferox.core.util.io.Chunkable;
import com.ferox.core.util.io.InputChunk;
import com.ferox.core.util.io.OutputChunk;

/**
 * View represents the viewport for a given Scene.  It must be linked to a ViewNode that is
 * contained within that Scene as well in order to render the Scene.  View represents the lens 
 * or projection and 2D drawing rectangle while ViewNode remembers where it's located in the world.
 * View also has some utilities for frustum culling used by Boundables.
 * 
 * @author Michael Ludwig
 *
 */
public class View implements Chunkable {
	public static final int MAX_FRUSTUM_PLANES = 6;
	
	public static final int NEAR_PLANE = 0;
	public static final int RIGHT_PLANE = 1;
	public static final int TOP_PLANE = 2;
	public static final int BOTTOM_PLANE = 3;
	public static final int FAR_PLANE = 5;
	public static final int LEFT_PLANE = 4;
	
	public static final int NEAR_PLANE_MASK = (1 << NEAR_PLANE);
	public static final int RIGHT_PLANE_MASK = (1 << RIGHT_PLANE);
	public static final int TOP_PLANE_MASK = (1 << TOP_PLANE);
	public static final int BOTTOM_PLANE_MASK = (1 << BOTTOM_PLANE);
	public static final int FAR_PLANE_MASK = (1 << FAR_PLANE);
	public static final int LEFT_PLANE_MASK = (1 << LEFT_PLANE);
	
	public static final int OUTSIDE = 0;
	public static final int INSIDE = 1;
	public static final int INTERSECT = 2;
	
	// World values
	private final Vector3f location;
	private final Vector3f up;
	private final Vector3f direction;
	private final Vector3f u;
	private final Vector3f s;
	
	private final Transform worldTransform;
	private final Transform inverseTransform;
	private Plane[] worldPlanes;
	private boolean updateView;


	// Camera values
	private boolean useOrtho;
	private float frustumLeft;
	private float frustumRight;
	private float frustumTop;
	private float frustumBottom;
	private float frustumNear;
	private float frustumFar;
	private Plane[] cameraPlanes;
	private Matrix4f projectionMatrix;
	private boolean updateProjection;
	
	// Used to setup viewport
	private float viewLeft;
	private float viewRight;
	private float viewTop;
	private float viewBottom;
	
	// For bounds ...
	private int planeState;
	private SphereClassifier classifier;
	
	/**
	 * Creates a view with default settings perspective projection,
	 * setFrustum(-.5f, .5f, 1f, 1f, 1f, 2f).
	 */
	public View() {
		this.projectionMatrix = new Matrix4f();
		this.cameraPlanes = new Plane[6];
		for (int i = 0; i < MAX_FRUSTUM_PLANES; i++)
			this.cameraPlanes[i] = new Plane();
		
		this.worldPlanes = new Plane[6];
		for (int i = 0; i < MAX_FRUSTUM_PLANES; i++)
			this.worldPlanes[i] = new Plane();
		this.worldTransform = new Transform();
		this.inverseTransform = new Transform();
		
		this.location = new Vector3f();
		this.direction = new Vector3f(0f, 0f, 1f);
		this.u = new Vector3f();
		this.up = new Vector3f(0f, 1f, 0f);
		this.s = new Vector3f();
		
		this.setViewBottom(0f);
		this.setViewTop(1f);
		this.setViewLeft(0f);
		this.setViewRight(1f);
		
		this.setPerspective(60f, 1f, .1f, 100f);
	}
	
	public Vector3f getLocation() {
		return this.location;
	}
	
	public Vector3f getDirection() {
		return this.direction;
	}
	
	public Vector3f getUp() {
		return this.up;
	}
	
	public int getPlaneState() {
		return this.planeState;
	}
	
	public void setLocation(Vector3f location) {
		if (location == null)
			this.location.set(0f, 0f, 0f);
		else
			this.location.set(location);
		this.updateView = true;
	}
	
	public void setDirection(Vector3f direction) {
		if (direction == null)
			this.direction.set(0f, 0f, 1f);
		else
			this.direction.set(direction);
		this.updateView = true;
	}
	
	public void setUp(Vector3f up) {
		if (up == null)
			this.up.set(0f, 1f, 0f);
		else
			this.up.set(up);
		this.updateView = true;
	}
	
	public void setPlaneState(int state) {
		this.planeState = state;
	}
	
	/**
	 * Classifies the given sphere centered at (x,y,z) with the given radius.  The coordinates are
	 * assumed to be in this View's coordinates.
	 */
	public int classify(float x, float y, float z, float radius, int planeState) {
		return this.classifier.classify(x, y, z, radius, planeState, this);
	}

	/**
	 * Update the values returned by getCachedViewTransform() and getCachedWorldFrustumPlanex().
	 */
	public void updateWorldValues() {
		this.computeViewMatrix();
		if (this.updateProjection)
			this.computeProjectionMatrix();
		this.computeWorldPlanes();
	}
	
	private void computeViewMatrix() {
		this.inverseTransform.setIdentity();
		
		this.direction.normalize();
		this.up.normalize();
		this.s.cross(this.direction, this.up);
		this.s.normalize();
		this.u.cross(this.s, this.direction);
		this.u.normalize();
		
		Matrix3f m = this.inverseTransform.getRotation();
		m.setRow(0, this.s.x, this.s.y, this.s.z);
		m.setRow(1, this.u.x, this.u.y, this.u.z);
		m.setRow(2, -this.direction.x, -this.direction.y, -this.direction.z);
		
		this.worldTransform.setIdentity();
		this.worldTransform.setTranslation(-this.location.x, -this.location.y, -this.location.z);
		this.inverseTransform.mul(this.inverseTransform, this.worldTransform);
		this.worldTransform.inverse(this.inverseTransform);
		
		this.updateView = false;
	}
	
	/**
	 * Returns thof the view node's world transform at the time of the last updateViewNodeDependentCache()
	 * call.
	 */
	public Transform getWorldTransform() {
		if (this.updateView)
			this.computeViewMatrix();
		return this.worldTransform;
	}
	
	public Transform getInverseWorldTransform() {
		if (this.updateView)
			this.computeViewMatrix();
		return this.inverseTransform;
	}
	
	/**
	 * Get all 6 cached world frustum planes.  These will be valid to use during the rendering of a frame, or if
	 * the camera's view node hasn't moved in world space.
	 */
	public Plane[] getWorldFrustumPlanes() {
		if (this.updateView) {
			if (this.updateProjection)
				this.computeProjectionMatrix();
			this.computeWorldPlanes();
		}
		return this.worldPlanes;
	}
	
	/**
	 * Get the given cached world plane.  This will be valid during the rendering of the frame, or if
	 * the camera's view node hasn't moved in world space.
	 */
	public Plane getWorldFrustumPlane(int plane) {
		if (this.updateView) {
			if (this.updateProjection)
				this.computeProjectionMatrix();
			this.computeWorldPlanes();
		}
		return this.worldPlanes[plane];
	}
	
	/**
	 * Get the projection matrix that will be used for the projection transform in openGL.
	 * It is not recommended to modify the returned matrix's values.
	 */
	public Matrix4f getProjectionMatrix() {
		if (this.updateProjection) 
			this.computeProjectionMatrix();
		return this.projectionMatrix;
	}

	/**
	 * Set the frustum values to create a projection matrix that represents the given
	 * perspective.  zNear and zFar are used for frustumNear() and frustumFar(), so they
	 * should be valid.  fov is an angle in degrees measuring the field of view in the 
	 * y axis.  aspect is the ratio of width over height.
	 */
	public void setPerspective(float fov, float aspect, float zNear, float zFar) {
		float h = (float)Math.tan(Math.toRadians(fov)) * zNear * .5f;
        float w = h * aspect;
        this.frustumLeft = -w;
        this.frustumRight = w;
        this.frustumBottom = -h;
        this.frustumTop = h;
        this.frustumNear = zNear;
        this.frustumFar = zFar;
        
        this.updateProjection = true;
	}
	
	/**
	 * Convenience method to set all the faces of the frustum.
	 */
	public void setFrustum(float left, float right, float top, float bottom, float zNear, float zFar) {
		this.frustumLeft = left;
		this.frustumRight = right;
		this.frustumTop = top;
		this.frustumBottom = bottom;
		this.frustumNear = zNear;
		this.frustumFar = zFar;
		
		this.updateProjection = true;
	}
	
	/**
	 * Whether or not this View uses a ortho/parallel projection instead of a perspective one.
	 */
	public boolean isOrthoProjection() {
		return this.useOrtho;
	}

	/**
	 * Set this view to use a ortho projection.
	 */
	public void setUseOrthoProjection(boolean useOrtho) {
		this.useOrtho = useOrtho;
		this.updateProjection = true;
	}
	
	private void computeWorldPlanes() {
		if (this.updateProjection)
			this.computeProjectionMatrix();
		if (this.updateView)
			this.computeViewMatrix();
		for (int i = 0; i < this.worldPlanes.length; i++) {
			if (this.worldPlanes[i] == null)
				this.worldPlanes[i] = new Plane();
			this.cameraPlanes[i].transform(this.worldTransform, this.worldPlanes[i]);
			this.worldPlanes[i].normalize();
		}
	}
	
	/**
	 * Get all 6 frustum planes, don't modify contents.
	 */
	public Plane[] getCameraFrustumPlanes() {
		if (this.updateProjection)
			this.computeProjectionMatrix();
		return this.cameraPlanes;
	}
	
	/**
	 * Get the corresponding plane of the view frustum.  Do not modify the normal or constant.
	 */
	public Plane getCameraFrustumPlane(int plane) {
		if (this.updateProjection)
			this.computeProjectionMatrix();
		return this.cameraPlanes[plane];
	}
	
	/**
	 * Get the left edge of the near view frustum plane in camera space coords.
	 */
	public float getFrustumLeft() {
		return this.frustumLeft;
	}

	/**
	 * Set the left edge of the near view frustum plane in camera space coords.
	 * The left edge of the far plane is calculated based on near and far distances and the
	 * projection type.
	 */
	public void setFrustumLeft(float frustumLeft) {
		this.frustumLeft = frustumLeft;
		this.updateProjection = true;
	}

	/**
	 * Get the right edge of the near view frustum plane in camera space coords.
	 */
	public float getFrustumRight() {
		return this.frustumRight;
	}

	/**
	 * Set the right edge of the near view frustum plane in camera space coords.
	 * The right edge of the far plane is calculated based on near and far distances and the
	 * projection type.  Must be greater than getFrustumLeft().
	 */
	public void setFrustumRight(float frustumRight) {
		this.frustumRight = frustumRight;
		this.updateProjection = true;
	}

	/**
	 * Get the top edge of the near view frustum plane in camera space coords.
	 */
	public float getFrustumTop() {
		return this.frustumTop;
	}

	/**
	 * Set the top edge of the near view frustum plane in camera space coords.
	 * The top edge of the far plane is calculated based on near and far distances and the
	 * projection type.
	 */
	public void setFrustumTop(float frustumTop) {
		this.frustumTop = frustumTop;
		this.updateProjection = true;
	}

	/**
	 * Get the bottom edge of the near view frustum plane in camera space coords.
	 */
	public float getFrustumBottom() {
		return this.frustumBottom;
	}

	/**
	 * Set the bottom edge of the near view frustum plane in camera space coords.
	 * The bottom edge of the far plane is calculated based on near and far distances and the
	 * projection type.  Must be less than getFrustumTop().
	 */
	public void setFrustumBottom(float frustumBottom) {
		this.frustumBottom = frustumBottom;
		this.updateProjection = true;
	}

	/**
	 * Get the distance along the negative z-axis to the near view frustum plane.
	 */
	public float getFrustumNear() {
		return this.frustumNear;
	}

	/**
	 * Set the distance along the negative z-axis to the near view frustum plane.
	 * Must be less than getFrustumFar() and if useOrthoProjection() is false, then
	 * this must be greater than 0, it only checks when computing the matrix, however.
	 */
	public void setFrustumNear(float frustumNear) {
		this.frustumNear = frustumNear;
		this.updateProjection = true;
	}

	/**
	 * Get the distance along the negative z-axis to the far view frustum plane.
	 */
	public float getFrustumFar() {
		return this.frustumFar;
	}

	/**
	 * Set the distance along the negative z-axis to the far view frustum plane.
	 * Must be greater than getFrustumNear().
	 */
	public void setFrustumFar(float frustumFar) {
		this.frustumFar = frustumFar;	
		this.updateProjection = true;
	}

	/**
	 * Get the proportional location of the view's left edge in a canvas.  
	 * By default it is 0.
	 */
	public float getViewLeft() {
		return this.viewLeft;
	}

	/**
	 * Set the proportional location of the view's left edge, between 0 and 1 
	 * is recommended (outside values give undefined behavior).
	 */
	public void setViewLeft(float viewLeft) {
		this.viewLeft = viewLeft;
		this.updateProjection = true;
	}

	/**
	 * Get the proportional location of the view's right edge (measured from the left side
	 * of the canvas).  By default it is 1.  
	 */
	public float getViewRight() {
		return this.viewRight;
	}

	/**
	 * Set the proportional location of the view's right edge, between 0 and 1 
	 * is recommended (outside values give undefined behavior). Must be greater than getViewLeft().
	 */
	public void setViewRight(float viewRight) {
		this.viewRight = viewRight;
		this.updateProjection = true;
	}

	/**
	 * Get the proportional location of the view's top edge (measured from the bottom side
	 * of the canvas).  By default it is 1.  
	 */
	public float getViewTop() {
		return this.viewTop;
	}

	/**
	 * Set the proportional location of the view's top edge.  Must be greater than getViewBottom().
	 */
	public void setViewTop(float viewTop) {
		this.viewTop = viewTop;
		this.updateProjection = true;
	}

	/**
	 * Get the proportional location of the view's bottom edge (measured from the bottom side
	 * of the canvas).  By default it is 0.  
	 */
	public float getViewBottom() {
		return this.viewBottom;
	}

	/**
	 * Set the proportional location of the view's bottom edge.  
	 */
	public void setViewBottom(float viewBottom) {
		this.viewBottom = viewBottom;
		this.updateProjection = true;
	}
	
	private static void orthoMatrix(float fr, float fl, float ft, float fb, float fn, float ff, Matrix4f out) {
		out.m00 = 2f / (fr - fl);
		out.m11 = 2f / (ft - fb);
		out.m22 = 2f / (fn - ff);
		out.m33 = 1f;
		
		out.m03 = -(fr + fl) / (fr - fl);
		out.m13 = -(ft + fb) / (ft - fb);
		out.m23 = -(ff + fn) / (ff - fn);
	}
	
	private static void projMatrix(float fr, float fl, float ft, float fb, float fn, float ff, Matrix4f out) {
		out.m00 = 2f * fn / (fr - fl);
		out.m11 = 2f * fn / (ft - fb);
		
		out.m02 = (fr + fl) / (fr - fl);
		out.m12 = (ft + fb) / (ft - fb);
		out.m22 = -(ff + fn) / (ff - fn);
		out.m32 = -1f;
		
		out.m23 = -2f * ff * fn / (ff - fn);
	}
	
	/**
	 * Computes the values for the openGL projection matrix and stores them in the matrix returned
	 * by getProjectionMatrix().
	 */
	private void computeProjectionMatrix() {
		this.updateCachedValues();
		this.projectionMatrix.setZero();
		if (this.useOrtho) {
			orthoMatrix(this.frustumRight, this.frustumLeft, this.frustumTop, this.frustumBottom, this.frustumNear, this.frustumFar, this.projectionMatrix);
		} else {
			projMatrix(this.frustumRight, this.frustumLeft, this.frustumTop, this.frustumBottom, this.frustumNear, this.frustumFar, this.projectionMatrix);
		}
		
		this.cameraPlanes[RIGHT_PLANE].getNormal().set(this.projectionMatrix.m30 - this.projectionMatrix.m00,
													   this.projectionMatrix.m31 - this.projectionMatrix.m01,
													   this.projectionMatrix.m32 - this.projectionMatrix.m02);
		this.cameraPlanes[RIGHT_PLANE].setConstant(this.projectionMatrix.m33 - this.projectionMatrix.m03);
		this.cameraPlanes[RIGHT_PLANE].normalize();
		
		this.cameraPlanes[LEFT_PLANE].getNormal().set(this.projectionMatrix.m30 + this.projectionMatrix.m00,
													   this.projectionMatrix.m31 + this.projectionMatrix.m01,
													   this.projectionMatrix.m32 + this.projectionMatrix.m02);
		this.cameraPlanes[LEFT_PLANE].setConstant(this.projectionMatrix.m33 + this.projectionMatrix.m03);
		this.cameraPlanes[LEFT_PLANE].normalize();
		
		this.cameraPlanes[BOTTOM_PLANE].getNormal().set(this.projectionMatrix.m30 + this.projectionMatrix.m10,
													   this.projectionMatrix.m31 + this.projectionMatrix.m11,
													   this.projectionMatrix.m32 + this.projectionMatrix.m12);
		this.cameraPlanes[BOTTOM_PLANE].setConstant(this.projectionMatrix.m33 + this.projectionMatrix.m13);
		this.cameraPlanes[BOTTOM_PLANE].normalize();
		
		this.cameraPlanes[TOP_PLANE].getNormal().set(this.projectionMatrix.m30 - this.projectionMatrix.m10,
													  this.projectionMatrix.m31 - this.projectionMatrix.m11,
													  this.projectionMatrix.m32 - this.projectionMatrix.m12);
		this.cameraPlanes[TOP_PLANE].setConstant(this.projectionMatrix.m33 - this.projectionMatrix.m13);
		this.cameraPlanes[TOP_PLANE].normalize();
		
		this.cameraPlanes[NEAR_PLANE].getNormal().set(this.projectionMatrix.m30 + this.projectionMatrix.m20,
				   									   this.projectionMatrix.m31 + this.projectionMatrix.m21,
				   									   this.projectionMatrix.m32 + this.projectionMatrix.m22);
		this.cameraPlanes[NEAR_PLANE].setConstant(this.projectionMatrix.m33 + this.projectionMatrix.m23);
		this.cameraPlanes[NEAR_PLANE].normalize();
		
		this.cameraPlanes[FAR_PLANE].getNormal().set(this.projectionMatrix.m30 - this.projectionMatrix.m20,
				   									  this.projectionMatrix.m31 - this.projectionMatrix.m21,
				   									  this.projectionMatrix.m32 - this.projectionMatrix.m22);
		this.cameraPlanes[FAR_PLANE].setConstant(this.projectionMatrix.m33 - this.projectionMatrix.m23);
		this.cameraPlanes[FAR_PLANE].normalize();
		this.updateProjection = false;
	}
	
	/**
	 * Updates the cached values for this view.
	 */
	private void updateCachedValues() throws FeroxException {
		if (this.frustumLeft > this.frustumRight)
			throw new FeroxException("Illegal frustum left and right values");
		if (this.frustumBottom > this.frustumTop)
			throw new FeroxException("Illegal frustum top and bottom values");
		if (this.frustumNear > this.frustumFar)
			throw new FeroxException("Illegal frustum near and far values");
		
		if (this.useOrtho) {
			this.classifier = new OrthoSphereClassifier(this);
		} else {
			if (this.frustumNear <= 0)
				throw new FeroxException("Frustum near value can't be <= 0 if View is not using ortho projection");
			if (this.frustumLeft + this.frustumRight == 0 && this.frustumBottom + this.frustumTop == 0)
				this.classifier = new PerspectiveSphereClassifier(this);
			else
				this.classifier = new FrustumSphereClassifier(this);
		}
	}
	
	/**
	 * Interface that classifies spheres based on camera coordinates.
	 */
	private static interface SphereClassifier {
		public int classify(float x, float y, float z, float radius, int planeState, View view);
	}
	
	/**
	 * Classifies spheres, assuming that the given View represents one created by setPerspective().
	 */
	private static class PerspectiveSphereClassifier implements SphereClassifier {
		private View view;
		private float tang;
		private float xFactor;
		private float yFactor;
		private float ratio;
		
		public PerspectiveSphereClassifier(View view) {
			this.view = view;
			
			float fov = 2f * (float)Math.atan(this.view.frustumTop / this.view.frustumNear);
			
			this.ratio = (this.view.frustumRight - this.view.frustumLeft) / (this.view.frustumTop - this.view.frustumBottom);
			this.tang = (float)Math.tan(fov / 2f);
			this.yFactor = 1 / (float)Math.cos(fov / 2);
			this.xFactor = 1 / (float)Math.cos(Math.atan(this.tang * this.ratio));
		}
		
		public int classify(float x, float y, float z, float radius, int planeState, View view) {
			if (radius <= 0)
				return View.INTERSECT;
			
			int res = View.INSIDE;
			z *= -1;
			if (z > this.view.frustumFar + radius || z < this.view.frustumNear - radius) {
				view.setPlaneState(planeState);
				return View.OUTSIDE;
			} 
			if (z > this.view.frustumFar - radius) 
				res = View.INTERSECT;
			else 
				planeState |= FAR_PLANE_MASK;
			if (z < this.view.frustumNear + radius) 
				res = View.INTERSECT;
			else
				planeState |= NEAR_PLANE_MASK;
			
			float d = this.yFactor * radius;
			z *= this.tang;
			if (y > z + d || y < -z - d) {
				view.setPlaneState(planeState);
				return View.OUTSIDE; 
			}
			if (y > z - d) 
				res = View.INTERSECT;
			else
				planeState |= TOP_PLANE_MASK;
			if (y < -z + d)
				res = View.INTERSECT;
			else
				planeState |= BOTTOM_PLANE_MASK;
			
			d = this.xFactor * radius;
			z *= this.ratio;
			if (x > z + d || x < -z - d) {
				view.setPlaneState(planeState);
				return View.OUTSIDE; 
			}
			if (x > z - d) 
				res = View.INTERSECT;
			else
				planeState |= RIGHT_PLANE_MASK;
			if (x < -z + d)
				res = View.INTERSECT;
			else
				planeState |= LEFT_PLANE_MASK;
			
			view.setPlaneState(planeState);
			return res;
		}
	}
	
	/**
	 * Classifier that can use an arbitrary normal frustum, however, it is a tad slower than
	 * PerspectiveSphereClassifier.
	 */
	private static class FrustumSphereClassifier implements SphereClassifier {
		private View view;
		
		private float tangYTop;
		private float tangYBottom;
		private float tangXLeft;
		private float tangXRight;
		
		private float xFactorLeft;
		private float xFactorRight;
		private float yFactorTop;
		private float yFactorBottom;
		
		public FrustumSphereClassifier(View view) {
			this.view = view;
			
			this.tangYTop = (this.view.frustumTop / this.view.frustumNear);
			this.tangYBottom = (this.view.frustumBottom / this.view.frustumNear);
			this.tangXLeft = (this.view.frustumLeft / this.view.frustumNear);
			this.tangXRight = (this.view.frustumRight / this.view.frustumNear);
			
			this.yFactorTop = 1f / (float)Math.cos(Math.atan(this.tangYTop));
			this.yFactorBottom = 1f / (float)Math.cos(Math.atan(this.tangYBottom));
			this.xFactorRight = 1f / (float)Math.cos(Math.atan(this.tangXRight));
			this.xFactorLeft = 1f / (float)Math.cos(Math.atan(this.tangXLeft));
		}
		
		public int classify(float x, float y, float z, float radius, int planeState, View view) {
			if (radius <= 0)
				return View.INTERSECT;
			
			int res = View.INSIDE;
			z *= -1;
			if (z > this.view.frustumFar + radius || z < this.view.frustumNear - radius) {
				view.setPlaneState(planeState);
				return View.OUTSIDE;
			} 
			if (z > this.view.frustumFar - radius) 
				res = View.INTERSECT;
			else 
				planeState |= FAR_PLANE_MASK;
			if (z < this.view.frustumNear + radius)
				res = View.INTERSECT;
			else
				planeState |= NEAR_PLANE_MASK;
			
			float d1 = this.yFactorTop * radius;
			float d2 = this.yFactorBottom * radius;
			float az = this.tangYTop * z;
			float bz = this.tangYBottom * z;
			if (y > az + d1 || y < bz - d2) {
				view.setPlaneState(planeState);
				return View.OUTSIDE; 
			}
			if (y > az - d1) 
				res = View.INTERSECT;
			else
				planeState |= TOP_PLANE_MASK;
			if (y < bz + d2)
				res = View.INTERSECT;
			else
				planeState |= NEAR_PLANE_MASK;
			
			d1 = this.xFactorRight * radius;
			d2 = this.xFactorLeft * radius;
			az *= this.tangXRight * z;
			bz *= this.tangXLeft * z;
			if (x > az + d1 || x < bz - d2) {
				view.setPlaneState(planeState);
				return View.OUTSIDE; 
			}
			if (x > az - d1) 
				res = View.INTERSECT;
			else
				planeState |= RIGHT_PLANE_MASK;
			if (x < bz + d2)
				res = View.INTERSECT;
			else
				planeState |= LEFT_PLANE_MASK;
			view.setPlaneState(planeState);
			return res;
		}
	}
	
	/**
	 * Classifies a sphere based on the view being an ortho projection.
	 */
	private static class OrthoSphereClassifier implements SphereClassifier {
		private View view;
		
		public OrthoSphereClassifier(View view) {
			this.view = view;
		}
		
		public int classify(float x, float y, float z, float radius, int planeState, View view) {
			if (radius <= 0)
				return View.INTERSECT;
			
			int res = View.INSIDE;
			z *= -1;
			if (z > this.view.frustumFar + radius || z < this.view.frustumNear - radius) {
				view.setPlaneState(planeState);
				return View.OUTSIDE;
			} 
			if (z > this.view.frustumFar - radius) 
				res = View.INTERSECT;
			else
				planeState |= FAR_PLANE_MASK;
			if (z < this.view.frustumNear + radius)
				res = View.INTERSECT;
			else
				planeState |= NEAR_PLANE_MASK;
			
			if (y > this.view.frustumTop + radius || y < this.view.frustumBottom - radius) {
				view.setPlaneState(planeState);
				return View.OUTSIDE;
			} 
			if (y > this.view.frustumTop - radius) 
				res = View.INTERSECT;
			else
				planeState |= TOP_PLANE_MASK;
			if (y < this.view.frustumBottom + radius)
				res = View.INTERSECT;
			else
				planeState |= BOTTOM_PLANE_MASK;
			
			if (x > this.view.frustumRight + radius || x < this.view.frustumLeft - radius) {
				view.setPlaneState(planeState);
				return View.OUTSIDE;
			} 
			if (x > this.view.frustumRight - radius) 
				res = View.INTERSECT;
			else
				planeState |= RIGHT_PLANE_MASK;
			if (x < this.view.frustumLeft + radius)
				res = View.INTERSECT;
			else
				planeState |= LEFT_PLANE_MASK;
			view.setPlaneState(planeState);
			return res;
		}
	}

	public void readChunk(InputChunk in) {
		float[] frustum = in.getFloatArray("frustum");
		float[] view = in.getFloatArray("view");
		float[] trans = in.getFloatArray("trans");
		
		this.frustumBottom = frustum[0];
		this.frustumTop = frustum[1];
		this.frustumLeft = frustum[2];
		this.frustumRight = frustum[3];
		this.frustumFar = frustum[4];
		this.frustumNear = frustum[5];
		
		this.viewBottom = view[0];
		this.viewTop = view[1];
		this.viewLeft = view[2];
		this.viewRight = view[3];
		
		this.up.set(trans[0], trans[1], trans[2]);
		this.direction.set(trans[3], trans[4], trans[5]);
		this.location.set(trans[6], trans[7], trans[8]);
	}

	public void writeChunk(OutputChunk out) {
		float[] frustum = new float[] {this.frustumBottom, this.frustumTop,
									   this.frustumLeft, this.frustumRight,
									   this.frustumFar, this.frustumNear};
		float[] view = new float[] {this.viewBottom, this.viewTop,
									this.viewLeft, this.viewRight};
		float[] trans = new float[] {this.up.x, this.up.y, this.up.z,
									 this.direction.x, this.direction.y, this.direction.z,
									 this.location.x, this.location.y, this.location.z};
		out.set("frustum", frustum);
		out.set("view", view);
		out.set("trans", trans);
	}
}