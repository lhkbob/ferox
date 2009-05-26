package com.ferox.renderer;

import com.ferox.resource.TextureImage;
import com.ferox.resource.TextureImage.TextureTarget;

/**
 * <p>
 * A TextureSurface represents a surface that renders its content directly into
 * multiple, usable textures. This can be used for a variety of effects,
 * including dynamic reflections, deferred lighting, and other full screen
 * effects.
 * </p>
 * <p>
 * TextureSurfaces support multiple color buffer attachments, although the exact
 * number is dependent on hardware, and the requested number when the surface
 * was created. There can also have a depth texture that stores the surface's
 * depth information.
 * </p>
 * <p>
 * Multiple texture surfaces may reference the same texture image, such as one
 * surface attached to each face of a cube map. It is recommended that only one
 * surface modifies a 2D layer of a texture at one time.
 * </p>
 * <p>
 * Attached textures should not be used within scenes rendered onto the
 * offscreen render surface. Undefined results will occur if this is the case.
 * </p>
 * <p>
 * Attached textures should be deemed immutable. Any changes made to them will
 * be ignored, and update requests to the Framework will throw exceptions.
 * </p>
 * 
 * @author Michael Ludwig
 */
public interface TextureSurface extends RenderSurface {
	/**
	 * <p>
	 * Get the TextureImage handle for the given color target. If no texture
	 * exists for the given target, null is returned. A texture will exist if
	 * target is within [0, getNumColorTargets() - 1]; unless there are no color
	 * targets, in which case null is always returned.
	 * </p>
	 * <p>
	 * The BufferData associated with the texture is null, but internally the
	 * Framework will update the low-level texture image each time the surface is
	 * rendered.
	 * </p>
	 * 
	 * @param target The requested target
	 * @return The TextureImage associated with target
	 */
	public TextureImage getColorBuffer(int target);

	/**
	 * Return the number of color targets in use by the surface. There will be a
	 * non-null TextureImage for each color target, 0 through numTargets-1 (as
	 * returned by getColorBuffer(target)).
	 * 
	 * @return The number of color targets (and thus TextureImages for coloring)
	 */
	public int getNumColorTargets();

	/**
	 * <p>
	 * Just like getColorBuffer(), but for storing depth information. Another
	 * difference is that they will always be at most one depth target, unlike
	 * colors which can have multiple textures rendered into.
	 * </p>
	 * <p>
	 * The format of this TextureImage will be DEPTH.
	 * </p>
	 * 
	 * @return The TextureImage holding the depth information
	 */
	public TextureImage getDepthBuffer();

	/**
	 * Return the texture target that every TextureImage for the color buffers
	 * will be. This will return null if there are no color buffers.
	 * 
	 * @return The TextureTarget used for all color images
	 */
	public TextureTarget getColorTarget();

	/**
	 * Return the texture target that the depth buffer TextureImage will be.
	 * This will return null if there is no depth buffer.
	 * 
	 * @return The TextureTarget for the depth image
	 */
	public TextureTarget getDepthTarget();

	/**
	 * <p>
	 * Return the "layer" that will be updated by the texture surface. If
	 * getTarget() is T_1D, T_2D, or T_RECT there is only one possible layer, so
	 * 0 is returned.
	 * </p>
	 * <p>
	 * If getColorTarget() is T_CUBEMAP, this value represents one of the six
	 * possible faces of the cube (see the constants defined in TextureCubeMap).
	 * If getColorTarget() is T_3D, this value represents the z index used to
	 * access one of the 3D textures 2D layers.
	 * </p>
	 * <p>
	 * getDepthTarget() is not considered, since the depth texture will always
	 * be a 1D, 2D or rectangular texture (cubemaps and 3d textures aren't
	 * supported).
	 * </p>
	 * <p>
	 * The texture images used by TextureSurface aren't mipmapped, so this value
	 * must <i>not</i> be confused with a texture's mipmap layer.
	 * </p>
	 * 
	 * @return The image layer used to update the TextureImage (e.g. cube face
	 *         for TextureCubeMaps)
	 */
	public int getLayer();
}
