/*
 * Ferox, a graphics and game library in Java
 *
 * Copyright (c) 2012, Michael Ludwig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright notice,
 *         this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright notice,
 *         this list of conditions and the following disclaimer in the
 *         documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.ferox.renderer;

import com.ferox.resource.Texture;
import com.ferox.resource.Texture.Target;

/**
 * <p>
 * A TextureSurface represents a surface that renders its content directly into
 * multiple, usable textures. This can be used for a variety of effects,
 * including dynamic reflections, deferred lighting, and other full screen
 * effects. A TextureSurface's textures are implicitly updated each time the
 * surface is rendered into so they reflect the newly rendered contents. It is
 * still necessary to update those textures when the texture parameters have
 * changed.
 * <p>
 * TextureSurfaces support multiple color buffer attachments, although the exact
 * number is dependent on hardware, and the requested number when the surface
 * was created. They can also have a depth texture that stores the surface's
 * depth information.
 * <p>
 * A TextureSurface requires a 2D plane of image data to render into. However,
 * Texture has the concept of a layer to support targets such as T_CUBEMAP.
 * Additionally, 3D textures have a single layer but are composed of multiple
 * planes of 2D data. The TextureSurface has two parameters:
 * {@link #getActiveLayer() the active layer}, and
 * {@link #getActiveDepthPlane() the active depth plane}. The active layer
 * selects which layer of every Texture in the surface will be rendered into,
 * and the active depth plane selects the depth plane from the active layer to
 * finally limit the rendering into a 2D plane. These parameters are defaults
 * used when {@link Context#setSurface(Surface) using a surface}. They can be
 * explicitly specified with {@link Context#setSurface(TextureSurface, int)}.
 * <p>
 * Attached textures have a limited ability to be updated. The created Textures
 * will not be mipmapped, so each layer will only have a single mipmap level.
 * Although it's possible to assign new Buffer image data to these levels,
 * subsequent renderings into the TextureSurface will overwrite the data.
 * Texture parameters are still updateable. The textures cannot be disposed of
 * by the Framework until the TextureSurface has been destroyed (an exception is
 * thrown by {@link HardwareAccessLayer#dispose(com.ferox.resource.Resource)}).
 * The textures are still usable after the surface has been destroyed, they will
 * just no longer be rendered into by the TextureSurface.
 * 
 * @author Michael Ludwig
 */
public interface TextureSurface extends Surface {
    /**
     * @return The depth of the Textures of this TextureSurface
     */
    public int getDepth();

    /**
     * @return The number of layers present in each of the Textures of this
     *         TextureSurface
     */
    public int getNumLayers();

    /**
     * @return The active layer that is rendered into unless the TextureSurface
     *         is activated with
     *         {@link HardwareAccessLayer#setActiveSurface(TextureSurface, int)}
     */
    public int getActiveLayer();

    /**
     * @return The active depth plane that is rendered into unless the
     *         TextureSurface is activated with
     *         {@link HardwareAccessLayer#setActiveSurface(TextureSurface, int)}
     */
    public int getActiveDepthPlane();

    /**
     * @return The options used to create this Surface. Any changes to the
     *         returned options will not be reflected by the Surface or future
     *         calls to this method.
     */
    public TextureSurfaceOptions getOptions();

    /**
     * Set the active, or default, Mipmap layer that is rendered into when
     * {@link Framework#queue(Surface, RenderPass)} is used, instead of the
     * queue action that allows the depth plane and layer to be overridden. The
     * layer must be between 0 and the number of layers present in this
     * surface's textures. Because every target except T_CUBEMAP has only one
     * layer, this is meaningful only when a TextureSurface is rendering into a
     * cube map.
     * 
     * @param layer The new default layer that's rendered into
     * @throws IllegalArgumentException if layer < 0 or layer >= maximum layers
     *             present
     */
    public void setActiveLayer(int layer);

    /**
     * Set the active, or default, depth plane that is rendered into when
     * {@link Framework#queue(Surface, RenderPass)} is used, instead of the
     * queue action that allows the depth plane and layer to be overridden. The
     * depth value must be between 0 and the maximum depth of the texture's
     * within this TextureSurface. Because every target except T_3D has a max
     * depth of 1, this is meaningful only when a TextureSurface is rendering
     * into a 3D texture.
     * 
     * @param depth The new default depth plane that's to be rendered into
     * @throws IllegalArgumentException if depth < 0 or depth >= max allowed
     *             depth
     */
    public void setActiveDepthPlane(int depth);

    /**
     * Get the Texture handle for the given color buffer. A texture will exist
     * if target is within [0, getNumColorBuffers() - 1]. The Buffer data
     * associated with the texture is null, but internally the Framework will
     * update the low-level texture image each time the surface is rendered.
     * 
     * @param buffer The requested color buffer
     * @return The Texture associated with buffer
     * @throws IndexOutOfBoundsException if buffer < 0 or >=
     *             {@link #getNumColorBuffers()}
     */
    public Texture getColorBuffer(int buffer);

    /**
     * Return the number of color buffers usable by this surface. This can be a
     * a number from 0 to the maximum supported number of color buffers on the
     * current hardware. A value of 0 implies that the TextureSurface does not
     * store any color information.
     * 
     * @return The number of color buffers (and thus Textures storing color
     *         data)
     */
    public int getNumColorBuffers();

    /**
     * Return the Texture image that stores the depth information that is
     * rendered into this TextureSurface. The format of this Texture will be
     * DEPTH. A null texture implies that there is no texture handle associated
     * with the depth data for the surface, but depth testing is still available
     * while rendering into the color buffers.
     * 
     * @return The Texture holding the depth information
     */
    public Texture getDepthBuffer();

    /**
     * Return the texture target that every Texture used with this
     * TextureSurface will be.
     * 
     * @return The Target used for all Textures in this surface
     */
    public Target getTarget();
}
