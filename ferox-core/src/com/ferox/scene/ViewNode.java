package com.ferox.scene;

import com.ferox.math.Frustum;
import com.ferox.renderer.RenderSurface;
import com.ferox.renderer.WindowSurface;
import com.ferox.util.entity.AbstractComponent;
import com.ferox.util.entity.Description;

/**
 * <p>
 * ViewNode is a Component that specifies the viewing point and projection
 * information necessary for rendering a scene into a RenderSurface. ViewNodes
 * are linked with a RenderSurface that represents the target for any rendering
 * that should occur from the ViewNode's perspective. Each ViewNode contains a
 * single {@link Frustum} that stores the location, orientation and projection
 * information to use when rendering.
 * </p>
 * <p>
 * The constructors provided are designed for the common use cases of
 * perspective projections that are centered on the RenderSurface, or of
 * orthographic projections that span the RenderSurface (useful for UI's and 2D
 * pixel work). Each ViewNode has an auto-update policy that defaults to true.
 * When it is true, a {@link ViewNodeController} will update the frustum to match
 * changes to the RenderSurface's dimensions while keeping the updated frustum
 * equivalent in form to a frustum created via
 * {@link #ViewNode(RenderSurface, float, float)} or
 * {@link #ViewNode(RenderSurface, float, float, float)}.
 * </p>
 * <p>
 * Initially each Frustum (and thus ViewNode) uses a location of (0, 0, 0) with
 * a right-handed coordinate system pointing down the negative z-axis. A
 * ViewNode can be positioned manually by updating the orientation of its
 * associated Frustum. Alternatively, Entities which are both
 * {@link SceneElement}s and ViewNodes will by updated by a ViewNodeController so
 * that the frustum's orientation matches the transformation of the Entity's
 * SceneElement.
 * </p>
 * 
 * @see ViewNodeController
 * @author Michael Ludwig
 */
@Description("Represents a camera location for rendering")
public final class ViewNode extends AbstractComponent<ViewNode> {
	private RenderSurface surface;
	private boolean matchSurfaceDim;
	
	private final Frustum frustum;

	/**
	 * Create a ViewNode linked with the given RenderSurface. Its initial
	 * Frustum will use a field-of-view of 60 degrees, a near distance of .1 and
	 * a far distance of 100. The aspect ratio will match that of the
	 * RenderSurface.
	 * 
	 * @param surface The RenderSurface initially attached to this ViewNode
	 */
	public ViewNode(RenderSurface surface) {
		this(surface, 60f, .1f, 100f);
	}

	/**
	 * Create a ViewNode linked with the given RenderSurface, that uses the
	 * given values for field-of-view, near distance and far distance. The
	 * aspect ratio will match that of the RenderSurface.
	 * 
	 * @param surface The RenderSurface initially attached to this ViewNode
	 * @param fov Field-of-view in degrees of the perspective projection of the
	 *            frustum
	 * @param znear Distance from camera location to near clipping plane
	 * @param zfar Distance from camera location to far clipping plane
	 * @throws IllegalArgumentException if znear >= zfar, or if znear <= 0, or
	 *             if fov is outside of (0, 180]
	 * @throws NullPointerException if surface is null
	 */
	public ViewNode(RenderSurface surface, float fov, float znear, float zfar) {
		super(ViewNode.class);
		setRenderSurface(surface);
		setAutoUpdateProjection(true);
		
		frustum = new Frustum(fov, surface.getWidth() / (float) surface.getHeight(), znear, zfar);
	}

	/**
	 * Create a ViewNode linked with the given RenderSurface, that uses the
	 * given values for near and far clipping plane distance. Unlike the other
	 * constructors, the frustum is configured to be an orthographic frustum.
	 * The frustum boundaries are configured to be from (0, 0) to (width,
	 * height).
	 * 
	 * @param surface The RenderSurface initially attached to this ViewNode
	 * @param znear The near clipping plane distance
	 * @param zfar The far clipping plane distance
	 * @throws IllegalArgumentException if znear >= zfar
	 * @throws NullPointerException if surface is null
	 */
	public ViewNode(RenderSurface surface, float znear, float zfar) {
		super(ViewNode.class);
		setRenderSurface(surface);
		setAutoUpdateProjection(true);
		
		frustum = new Frustum(true, 0f, surface.getWidth(), 0f, surface.getHeight(), znear, zfar);
	}

	/**
	 * <p>
	 * Return the Frustum instance that represents how a rendered image should
	 * be projected onto the {@link #getRenderSurface() RenderSurface of this
	 * ViewNode}. The Frustum may be modified to change how things are rendered.
	 * </p>
	 * <p>
	 * However, if an Entity with this ViewNode is also a SceneElement a
	 * ViewNodeController may overwrite changes to the Frustum's orientation.
	 * Similarly, if {@link #getAutoUpdateProjection()} returns true, the aspect
	 * ratio or frustum dimensions may be changed to match changes in the linked
	 * RenderSurface's dimensions.
	 * </p>
	 * 
	 * @return The Frustum representing the projection for this ViewNode
	 */
	public Frustum getFrustum() {
		return frustum;
	}

	/**
	 * Return the RenderSurface that this ViewNode is linked to. This will not
	 * be null.
	 * 
	 * @return The RenderSurface of this ViewNode
	 */
	public RenderSurface getRenderSurface() {
		return surface;
	}

	/**
	 * Set the RenderSurface that this ViewNode is linked to. If
	 * {@link #getAutoUpdateProjection()} returns true, this may cause the Frustum
	 * to be updated if the given surface's dimensions differ from the previous
	 * surface. This can also occur if the surface is a {@link WindowSurface}
	 * that is resizable.
	 * 
	 * @param surface The RenderSurface that this
	 */
	public void setRenderSurface(RenderSurface surface) {
		this.surface = surface;
	}

	/**
	 * <p>
	 * Return whether or not a {@link ViewNodeController} should update the
	 * Frustum's projection to match changes in this ViewNode's linked
	 * RenderSurface. If this returns false, any updates to the Frustum's
	 * projection must be done manually. If it returns true, two possible
	 * updates could occur:
	 * <ol>
	 * <li>If the Frustum is orthographic, the frustum region is forced to be
	 * (0, 0) to (width, height), but the z distances are left unmodified.</li>
	 * <li>If the Frustum is perspective, the aspect ratio is forced to be width
	 * / height, but the other perspective parameters are left unmodified.</li>
	 * </ol>
	 * By default this is true, so when complex projections are used, it must be
	 * disabled and any surface resizings must be handled manually.
	 * </p>
	 * <p>
	 * Note that this updating is separate from the location and orientation
	 * updating that can be performed if a ViewNode is added to an Entity that
	 * is also a SceneElement.
	 * </p>
	 * 
	 * @return True if the Frustum is updated to reflect surface dimension
	 *         changes.
	 */
	public boolean getAutoUpdateProjection() {
		return matchSurfaceDim;
	}

	/**
	 * Set whether or not this ViewNode's Frustum should be updated by a
	 * {@link ViewNodeController} to reflect changes in its linked
	 * RenderSurface's dimensions. See {@link #getAutoUpdateProjection()} for
	 * details of what is updated.
	 * 
	 * @param matchDim True if Frustum should automatically match changes to the
	 *            surface's dimensions
	 */
	public void setAutoUpdateProjection(boolean matchDim) {
		matchSurfaceDim = matchDim;
	}
}
