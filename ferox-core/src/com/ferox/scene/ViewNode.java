package com.ferox.scene;

import com.ferox.entity.AbstractComponent;
import com.ferox.math.Frustum;
import com.ferox.renderer.OnscreenSurface;
import com.ferox.renderer.Surface;
import com.ferox.scene.controller.ViewNodeController;

/**
 * <p>
 * ViewNode is a Component that specifies the viewing point and projection
 * information necessary for rendering a scene into a Surface. ViewNodes
 * are linked with a Surface that represents the target for any rendering
 * that should occur from the ViewNode's perspective. Each ViewNode contains a
 * single {@link Frustum} that stores the location, orientation and projection
 * information to use when rendering.
 * </p>
 * <p>
 * The constructors provided are designed for the common use cases of
 * perspective projections that are centered on the Surface, or of
 * orthographic projections that span the Surface (useful for UI's and 2D
 * pixel work). Each ViewNode has an auto-update policy that defaults to true.
 * When it is true, a {@link ViewNodeController} will update its frustum and
 * viewport to match changes to the Surface's dimensions while keeping
 * them relatively equivalent.
 * </p>
 * <p>
 * Initially each Frustum (and thus ViewNode) uses a location of (0, 0, 0) with
 * a right-handed coordinate system pointing down the negative z-axis. A
 * ViewNode can be positioned manually by updating the orientation of its
 * associated Frustum. Alternatively, Entities which are both
 * {@link SceneElement}s and ViewNodes will by updated by a ViewNodeController
 * so that the frustum's orientation matches the transformation of the Entity's
 * SceneElement.
 * </p>
 * 
 * @see ViewNodeController
 * @author Michael Ludwig
 */
public final class ViewNode extends AbstractComponent<ViewNode> {
    private Surface surface;
    private boolean matchSurfaceDim;
    
    private int top;
    private int bottom;
    private int left;
    private int right;
    
    private final Frustum frustum;

    /**
     * Create a ViewNode linked with the given Surface. Its initial
     * Frustum will use a field-of-view of 60 degrees, a near distance of .1 and
     * a far distance of 100. The aspect ratio will match that of the
     * Surface.
     * 
     * @param surface The Surface initially attached to this ViewNode
     */
    public ViewNode(Surface surface) {
        this(surface, 60f, .1f, 100f);
    }

    /**
     * Create a ViewNode linked with the given Surface, that uses the
     * given values for field-of-view, near distance and far distance. The
     * aspect ratio will match that of the Surface.
     * 
     * @param surface The Surface initially attached to this ViewNode
     * @param fov Field-of-view in degrees of the perspective projection of the
     *            frustum
     * @param znear Distance from camera location to near clipping plane
     * @param zfar Distance from camera location to far clipping plane
     * @throws IllegalArgumentException if znear >= zfar, or if znear <= 0, or
     *             if fov is outside of (0, 180]
     * @throws NullPointerException if surface is null
     */
    public ViewNode(Surface surface, float fov, float znear, float zfar) {
        super(ViewNode.class);
        setRenderSurface(surface);
        setAutoUpdateViewport(true);
        setViewport(0, surface.getWidth(), 0, surface.getHeight());
        
        frustum = new Frustum(fov, surface.getWidth() / (float) surface.getHeight(), znear, zfar);
    }

    /**
     * Create a ViewNode linked with the given Surface, that uses the
     * given values for near and far clipping plane distance. Unlike the other
     * constructors, the frustum is configured to be an orthographic frustum.
     * The frustum boundaries are configured to be from (0, 0) to (width,
     * height).
     * 
     * @param surface The Surface initially attached to this ViewNode
     * @param znear The near clipping plane distance
     * @param zfar The far clipping plane distance
     * @throws IllegalArgumentException if znear >= zfar
     * @throws NullPointerException if surface is null
     */
    public ViewNode(Surface surface, float znear, float zfar) {
        super(ViewNode.class);
        setRenderSurface(surface);
        setAutoUpdateViewport(true);
        setViewport(0, surface.getWidth(), 0, surface.getHeight());
        
        frustum = new Frustum(true, 0f, surface.getWidth(), 0f, surface.getHeight(), znear, zfar);
    }

    /**
     * <p>
     * Set the dimensions of the view port used by this ViewNode when rendering
     * content into its associated {@link #getRenderSurface() Surface}.
     * The values specified are in the pixel-space of the surface, which means
     * that <tt>left</tt> and <tt>right</tt> can range from 0 to the surface's
     * width, and <tt>bottom</tt> and <tt>top</tt> can range from 0 to the
     * surface's height.
     * </p>
     * <p>
     * The coordinate (0, 0) represents the surface's lower-left coordinate, and
     * (width, height) represents the upper-right. If
     * {@link #getAutoUpdateViewport()} returns true, these values will be
     * modified by a {@link ViewNodeController} to maintain the ViewNode's
     * relative position within the Surface.
     * </p>
     * 
     * @param left The location of the left edge of the viewport, measured in
     *            pixels from the left edge of the surface
     * @param right The location of the right edge of the viewport, measured in
     *            pixels from the left edge of the surface
     * @param bottom The location of the bottom edge of the viewport, measured
     *            in pixels from the bottom of the surface
     * @param top The location of the top edge of the viewport, measured in
     *            pixels from the bottom of the surface
     * @throws IllegalArgumentException if any dimension is less than 0, or if
     *             the dimensions extend beyond the size of the Surface,
     *             or if left > right or if bottom > top
     */
    public void setViewport(int left, int right, int bottom, int top) {
        if (left < 0 || left > surface.getWidth())
            throw new IllegalArgumentException("left dimension outside of surface range: " + left);
        if (right < 0 || right > surface.getWidth())
            throw new IllegalArgumentException("right dimension outside of surface range: " + right);
        if (bottom < 0 || bottom > surface.getHeight())
            throw new IllegalArgumentException("bottom dimension outside of surface range: " + bottom);
        if (top < 0 || top > surface.getHeight())
            throw new IllegalArgumentException("top dimension outside of surface range: " + top);
        
        if (left > right)
            throw new IllegalArgumentException("left must be less than or equal to right (left = " + left + ", right = " + right + ")");
        if (bottom > top)
            throw new IllegalArgumentException("bottom must be less than or equal to top (bottom = " + bottom + ", top = " + top + ")");
        
        this.left = left;
        this.right = right;
        this.top = top;
        this.bottom = bottom;
    }

    /**
     * @return The location of the left edge of the viewport, in pixels measured
     *         from the left of the surface.
     */
    public int getLeft() {
        return left;
    }
    
    /**
     * @return The location of the right edge of the viewport, in pixels measured
     *         from the left of the surface.
     */
    public int getRight() {
        return right;
    }
    
    /**
     * @return The location of the bottom edge of the viewport, in pixels measured
     *         from the bottom of the surface.
     */
    public int getBottom() {
        return bottom;
    }
    
    /**
     * @return The location of the top edge of the viewport, in pixels measured
     *         from the top of the surface.
     */
    public int getTop() {
        return top;
    }
    
    /**
     * <p>
     * Return the Frustum instance that represents how a rendered image should
     * be projected onto the {@link #getRenderSurface() Surface of this
     * ViewNode}. The Frustum may be modified to change how things are rendered.
     * </p>
     * <p>
     * However, if an Entity with this ViewNode is also a SceneElement a
     * ViewNodeController may overwrite changes to the Frustum's orientation.
     * Similarly, if {@link #getAutoUpdateViewport()} returns true, the aspect
     * ratio or frustum dimensions may be changed to match changes in the linked
     * Surface's dimensions.
     * </p>
     * 
     * @return The Frustum representing the projection for this ViewNode
     */
    public Frustum getFrustum() {
        return frustum;
    }

    /**
     * Return the Surface that this ViewNode is linked to. This will not
     * be null.
     * 
     * @return The Surface of this ViewNode
     */
    public Surface getRenderSurface() {
        return surface;
    }

    /**
     * Set the Surface that this ViewNode is linked to. If
     * {@link #getAutoUpdateViewport()} returns true, this may cause the Frustum
     * to be updated if the given surface's dimensions differ from the previous
     * surface. This can also occur if the surface is a resizable {@link OnscreenSurface}
     * 
     * @param surface The Surface that this
     */
    public void setRenderSurface(Surface surface) {
        this.surface = surface;
    }

    /**
     * <p>
     * Return whether or not a {@link ViewNodeController} should update the
     * ViewNode's viewport to match changes in its linked Surface. If this
     * returns false, the viewport will remain in a fixed position (barring
     * other calls to {@link #setViewport(int, int, int, int)}), regardless of
     * changes to the Surface. If it returns true, the pixel dimensions of
     * the viewport will be updated to place the new viewport in the same
     * relative position within the surface.
     * </p>
     * <p>
     * Additionally, the Frustum will be modified to reflect the changes within
     * the viewport. Depending on the type of Frustum, there can be two
     * modifications:
     * <ol>
     * <li>If the Frustum is orthographic, the frustum region is modified so that
     * it scales by the same amount as the changes in viewport dimensions.</li>
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
     * @return True if the ViewNode is updated to reflect surface dimension
     *         changes.
     */
    public boolean getAutoUpdateViewport() {
        return matchSurfaceDim;
    }

    /**
     * Set whether or not this ViewNode's viewport should be updated
     * by a {@link ViewNodeController} to reflect changes in its linked
     * Surface's dimensions. See {@link #getAutoUpdateViewport()} for
     * details of what is updated.
     * 
     * @param matchDim True if ViewNode should automatically match changes to
     *            the surface's dimensions
     */
    public void setAutoUpdateViewport(boolean matchDim) {
        matchSurfaceDim = matchDim;
    }
}
