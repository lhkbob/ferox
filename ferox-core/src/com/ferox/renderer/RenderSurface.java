package com.ferox.renderer;

import com.ferox.math.Color4f;

/**
 * <p>
 * Represents a two dimensional surface that stores a collection of logical and
 * color buffers holding the final rendering from a Framework. These surfaces
 * could be offscreen surfaces, in that they have no visible monitor region
 * associated with their rendered pixels, or they could be a window or
 * fullscreen.
 * </p>
 * <p>
 * The exact representation is dependent on the renderer's implementations,
 * however some possibilities include surfaces that use framebuffer objects to
 * render directly into a set of textures.
 * </p>
 * 
 * @author Michael Ludwig
 */
public interface RenderSurface {
	/**
	 * Return true if this surface has a depth buffer, allowing depth testing to
	 * work correctly.
	 * 
	 * @return True if this surface has a usable depth buffer for depth-testing
	 */
	public boolean hasDepthBuffer();

	/**
	 * Return true if this surface has a stencil buffer, allowing stencil
	 * testing to work correctly.
	 * 
	 * @return True if this surface has a usable stencil buffer for
	 *         stencil-testing
	 */
	public boolean hasStencilBuffer();

	/**
	 * Return true if this surface has one or more color buffers, allowing color
	 * data and color testing to work correctly.
	 * 
	 * @return True if this surface has a usable color buffer for rendering
	 */
	public boolean hasColorBuffer();

	/**
	 * <p>
	 * Return whether or not the surface has been destroyed.
	 * </p>
	 * <p>
	 * Destruction could be because of an explicit call to destroy() or
	 * destroy(surface), or because implementations provide a way for them to be
	 * implicitly destroyed (such as a WindowSurface being closed by the user).
	 * </p>
	 * 
	 * @return True if this surface is no longer usable by its creating
	 *         Framework.
	 */
	public boolean isDestroyed();

	/**
	 * Return the width of the actual drawable area of the surface (doesn't
	 * include any border or frame).
	 * 
	 * @return The width of the drawable area
	 */
	public int getWidth();

	/**
	 * Return the height of the actual drawable area of the surface (doesn't
	 * include any border or frame).
	 * 
	 * @return The height of the drawable area
	 */
	public int getHeight();

	/**
	 * <p>
	 * Get the display options that describe the size and type of surface that
	 * was created. May not be the same as the options requested when the
	 * surface was created.
	 * </p>
	 * <p>
	 * If the options can't be determined right away, return the requested
	 * options until the correct options are determined.
	 * </p>
	 * 
	 * @return The DisplayOptions of this surface
	 */
	public DisplayOptions getDisplayOptions();

	/**
	 * Get the Framework that created this surface.
	 * 
	 * @return The Framework that created this surface, must not be null
	 */
	public Framework getFramework();

	/**
	 * Get the background color to use when the color buffer is cleared for this
	 * surface. A alpha value != 1 may be treated as 1 if the render surface
	 * can't hold onto alpha pixel data. Default should be black.
	 * 
	 * @return Color4f that the surface's color buffer is cleared to
	 */
	public Color4f getClearColor();

	/**
	 * Get the starting depth to use when the depth buffer is cleared. The value
	 * must be within 0 and 1, where 1 represents farthest away. Default should
	 * be 1.
	 * 
	 * @return Depth value that the depth buffer is cleared to, between 0 and 1
	 */
	public float getClearDepth();

	/**
	 * Get the starting stencil value used when the stencil buffer is cleared.
	 * This integer is treated as an unsigned integer. The default should be 0.
	 * 
	 * @return Stencil value that the stencil buffer is cleared to
	 */
	public int getClearStencil();

	/**
	 * Copy color as the background color to use when the color buffer is
	 * cleared for this surface. A non-one alpha value may be treated as one if
	 * the render surface can't hold onto alpha pixel data.
	 * 
	 * @param color New clear color to use, if null use black
	 */
	public void setClearColor(Color4f color);

	/**
	 * Set the starting depth to use when the depth buffer is cleared. The value
	 * must be within 0 and 1, where 1 represents farthest away.
	 * 
	 * @param depth New depth clear value
	 * @throws IllegalArgumentException if depth is < 0 or > 1
	 */
	public void setClearDepth(float depth);

	/**
	 * Set the starting stencil value used when the stencil buffer is cleared.
	 * 
	 * @param stencil New stencil clear value, as an unsigned integer
	 */
	public void setClearStencil(int stencil);
}
