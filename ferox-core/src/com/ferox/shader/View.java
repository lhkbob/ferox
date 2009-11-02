package com.ferox.shader;

import com.ferox.math.Frustum;
import com.ferox.math.Matrix3f;
import com.ferox.math.Matrix4f;
import com.ferox.math.Transform;
import com.ferox.math.Vector3f;

/**
 * <p>
 * View represents the camera that a scene is rendered through. It holds onto
 * the type and specifics of the projection, and its location and orientation.
 * The visible region of a View is a 3D frustum. It provides functionality to
 * get the planes of the frustum in world space.
 * </p>
 * <p>
 * The View uses a right-handed coordinate system. With the default
 * configuration of the View, having an up vector of <0, 1, 0> and a direction
 * of <0, 0, -1>, the world coordinate space is as follows:
 * <ul>
 * <li>Positive X Axis: Horizontal, towards the right edge of the monitor.</li>
 * <li>Positive Y Axis: Vertical, towards the top of the monitor.</li>
 * <li>Positive Z Axis: Directly out of the monitor.</li>
 * </ul>
 * Keep in mind that a positive x change in view space causes the object to move
 * to the left, because the initial world direction is <0, 0, -1>.
 * </p>
 * 
 * @author Michael Ludwig
 */
public class View {
	private static final ThreadLocal<Vector3f> l = new ThreadLocal<Vector3f>() {
		@Override
		protected Vector3f initialValue() {
			return new Vector3f();
		}
	};

	private final Matrix4f projection;
	private final Frustum frustum;

	private float viewLeft;
	private float viewRight;
	private float viewTop;
	private float viewBottom;

	private final Vector3f location;
	private final Vector3f up;
	private final Vector3f direction;

	private final Transform viewTrans;

	/**
	 * Construct a view located at the origin, looking down the negative z-axis.
	 * Sets the projection as setPerspective(60f, 1f, 1f, 100f).
	 */
	public View() {
		projection = new Matrix4f();
		viewTrans = new Transform();
		frustum = new Frustum(60f, 1f, 1f, 100f);

		// share the frustum's vectors
		location = frustum.getLocation();
		up = frustum.getUp();
		direction = frustum.getDirection();
		
		setViewPort(0f, 1f, 0f, 1f);
	}

	/**
	 * <p>
	 * Return the transform that converts from "world" coordinates into view
	 * coordinates.
	 * </p>
	 * <p>
	 * This instance should not be modified. This will be stale if the location,
	 * direction, and up vectors are changed without a subsequent call to
	 * updateView().
	 * </p>
	 * 
	 * @return The Transform instance going from world coordinates to view
	 *         coordinates
	 */
	public Transform getViewTransform() {
		return viewTrans;
	}
	
	/**
	 * <p>
	 * Return the Frustum that represents the viewing projection for this View.
	 * </p>
	 * <p>
	 * This instance can be modified to assign new frustum values or change the
	 * projection mode from orthogonal to perspective. The Frustum's location,
	 * direction and up vectors are shared by this View. These vectors will be
	 * re-shared after a call to updateView() if the frustum's are rebound.
	 * </p>
	 * <p>
	 * The frustum planes stored by the returned Frustum will be stale if
	 * location, direction and up vectors are changed without a subsequent call
	 * to updateView() or by calling the Frustum's updateFrustumPlanes() method.
	 * </p>
	 * 
	 * @return The Frustum instance used by this View
	 */
	public Frustum getFrustum() {
		return frustum;
	}

	/**
	 * <p>
	 * Update the Transform returned by getViewTransform() and update this
	 * View's Frustum object to reflect changes in the View's location,
	 * direction and up vectors. After a call to this method, getViewTransform()
	 * will not be stale, and the frustum planes within getFrustum() will be
	 * up-to-date.
	 * <p>
	 * This must be called before the view is used by a render pass in the
	 * renderer.
	 * </p>
	 */
	public void updateView() {
		// restore frustum references, and auto update the planes,
		// will also ortho-normalize direction and up
		frustum.setOrientation(location, direction, up);
		Vector3f left = up.cross(direction, View.l.get());

		// update viewTrans to the basis vectors and new location
		Matrix3f m = viewTrans.getRotation();
		m.setCol(0, left).setCol(1, up).setCol(2, direction);

		viewTrans.setTranslation(location);
		viewTrans.setScale(1f);

		// invert the world transform to get the view transform
		viewTrans.inverse();
		
		// store the projection matrix
		frustum.getProjectionMatrix(projection);
	}

	/**
	 * Get the location vector of this view, in world space.
	 * 
	 * @return The location of the view
	 */
	public Vector3f getLocation() {
		return location;
	}

	/**
	 * Get the location vector of this view, in world space. If loc is null,
	 * this view's location is set to the origin.
	 * 
	 * @param loc New location for the view, null == origin
	 */
	public void setLocation(Vector3f loc) {
		if (loc == null)
			location.set(0f, 0f, 0f);
		else
			location.set(loc);
	}

	/**
	 * Get the up vector of this view, in world space. Together up and direction
	 * form a right-handed coordinate system.
	 * 
	 * @return The up vector of this view
	 */
	public Vector3f getUp() {
		return up;
	}

	/**
	 * Set the up vector of this view, in world space. If up is null, up is set
	 * to <0, 1, 0>.
	 * 
	 * @param up The new up vector for this view, null == <0, 1, 0>
	 */
	public void setUp(Vector3f up) {
		if (up == null)
			this.up.set(0f, 1f, 0f);
		else
			this.up.set(up);
	}

	/**
	 * <p>
	 * Get the direction vector of this view, in world space. Together up and
	 * direction form a right-handed coordinate system.
	 * </p>
	 * 
	 * @return The current direction that this camera is pointing
	 */
	public Vector3f getDirection() {
		return direction;
	}

	/**
	 * Set the direction vector of this view, in world space. If direction is
	 * null, direction is set to <0, 0, -1>.
	 * 
	 * @param dir The new direction for the view, null == <0, 0, -1>
	 */
	public void setDirection(Vector3f dir) {
		if (dir == null)
			direction.set(0f, 0f, -1f);
		else
			direction.set(dir);
	}

	/**
	 * Returns the projection matrix for this view. Modifying the values stored
	 * within will produce undefined results when rendering.
	 * 
	 * @return The projection matrix for this view
	 */
	public Matrix4f getProjectionMatrix() {
		return projection;
	}

	/**
	 * Get the left edge of the viewport, as a fraction of the width measured
	 * from the left side of the frame. Default value is 0.
	 * 
	 * @return Left edge of the viewport
	 */
	public float getViewLeft() {
		return viewLeft;
	}

	/**
	 * Get the right edge of the viewport, as a fraction of the width measured
	 * from the left side of the frame. Default value is 1.
	 * 
	 * @return Right edge of the viewport
	 */
	public float getViewRight() {
		return viewRight;
	}

	/**
	 * Get the top edge of the viewport, as a fraction of the height measured
	 * from the bottom side of the frame. Default value is 1.
	 * 
	 * @return Top edge of the viewport
	 */
	public float getViewTop() {
		return viewTop;
	}

	/**
	 * Get the bottom edge of the viewport, as a fraction of the height measured
	 * from the bottom side of the frame. Default value is 0.
	 * 
	 * @return Bottom edge of the viewport
	 */
	public float getViewBottom() {
		return viewBottom;
	}

	/**
	 * Set the dimensions of the viewport, as fractions of the width and height
	 * of a frame. Left and right are measured from the left edge, top and
	 * bottom are measured from the bottom edge.
	 * 
	 * @param left The left edge of the viewport
	 * @param right The right edge of the viewport
	 * @param bottom The bottom edge of the viewport
	 * @param top The bottom edge of the viewport
	 * @throws IllegalArgumentException if any value is outside of [0, 1], or if
	 *             top < bottom, or if left > right.
	 */
	public void setViewPort(float left, float right, float bottom, float top) {
		if (left < 0 || left > 1)
			throw new IllegalArgumentException("Illegal value for left viewport edge: " + left);
		if (right < 0 || right > 1)
			throw new IllegalArgumentException("Illegal value for right viewport edge: " + right);
		if (bottom < 0 || bottom > 1)
			throw new IllegalArgumentException("Illegal value for bottom viewport edge: " + bottom);
		if (top < 0 || top > 1)
			throw new IllegalArgumentException("Illegal value for top viewport edge: " + top);
		if (left > right || bottom > top)
			throw new IllegalArgumentException("Viewport edges are invalid: " + left + " " + right + " x " + bottom + " " + top);
		viewBottom = bottom;
		viewLeft = left;
		viewRight = right;
		viewTop = top;
	}
}
