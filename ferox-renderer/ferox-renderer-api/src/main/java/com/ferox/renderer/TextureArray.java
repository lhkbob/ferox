package com.ferox.renderer;

/**
 * The TextureArray interface is a decorating interface for specific texture types that are arrays of images.
 * All texture arrays have common properties regardless of the underlying texture type of each image. They
 * expose a render target for each image, in addition to the default render target that refers to the first or
 * 0th image. Texture arrays also support one additional texture coordinate besides however many supported by
 * the base texture type, which is used to designate the specific image in the array.
 * <p/>
 * Texture types that implement TextureArray should extend from the texture interface type of each image
 * within the array. As an example, {@link Texture2DArray} extends from {@link Texture2D}. This allows the 2D
 * texture array to be used anywhere in the code that expects a two-dimensional image.
 *
 * @author Michael Ludwig
 */
public interface TextureArray {
    /**
     * @return The number of images contained in the array
     */
    public int getImageCount();

    /**
     * Get the render target that will render directly into the specific image. {@code image} must be between
     * 0 and {@code getImageCount() - 1}.
     *
     * @param image The image to render into
     *
     * @return The render target for the image
     *
     * @throws IndexOutOfBoundsException if image is less than 0 or greater than the number of render targets
     */
    public Sampler.RenderTarget getRenderTarget(int image);
}
