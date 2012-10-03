package com.ferox.renderer;

import java.util.Arrays;

import com.ferox.resource.Texture.Target;
import com.ferox.resource.TextureFormat;

/**
 * TextureSurfaceOptions represents the set of configurable parameters used to
 * create a TextureSurface. These parameters are requests to the Framework and
 * may be ignored, but this is unlikely. Each TextureSurfaceOptions instance is
 * immutable, the setters available return new instances that match the calling
 * instance except for the new parameter value.
 * 
 * @author Michael Ludwig
 */
public final class TextureSurfaceOptions {
    private final Target textureTarget;

    private final int width;
    private final int height;
    private final int depth;

    private final TextureFormat[] colorTargetFormats;
    private final boolean useDepthTexture;

    private final int activeLayer;
    private final int activeDepthPlane;

    /**
     * Create a new TextureSurfaceOptions with the initial values:
     * <ul>
     * <li>{@link #getTarget()} returns T_2D</li>
     * <li>{@link #getNumColorBuffers()} returns 1, and has a TextureFormat of
     * RGB</li>
     * <li>{@link #getWidth()} returns 1</li>
     * <li>{@link #getHeight()} returns 1</li>
     * <li>{@link #getDepth()} returns 1</li>
     * <li>{@link #hasDepthTexture()} returns false</li>
     * <li>{@link #getActiveLayer()} returns 0</li>
     * <li>{@link #getActiveDepthPlane()} returns 0</li>
     * </ul>
     */
    public TextureSurfaceOptions() {
        this(Target.T_2D, new TextureFormat[] {TextureFormat.RGB}, 1, 1, 1, false, 0, 0);
    }

    private TextureSurfaceOptions(Target target, TextureFormat[] colorFormats, int width,
                                  int height, int depth, boolean useDepthTexture,
                                  int activeLayer, int activeDepth) {
        if (target == null) {
            throw new NullPointerException("Target cannot be null");
        }
        if (colorFormats == null) {
            colorFormats = new TextureFormat[0];
        }
        for (int i = 0; i < colorFormats.length; i++) {
            if (colorFormats[i] == null) {
                throw new NullPointerException("TextureFormat cannot be null, at index: " + i);
            }
            if (colorFormats[i] == TextureFormat.DEPTH) {
                throw new IllegalArgumentException("TextureFormat cannot be DEPTH, at index: " + i);
            }
        }

        if (width < 1 || height < 1 || depth < 1) {
            throw new IllegalArgumentException("Invalid dimensions: " + width + ", " + height + ", " + depth);
        }
        if (activeLayer < 0) {
            throw new IllegalArgumentException("Active layer must be at least 0");
        }
        if (activeDepth < 0) {
            throw new IllegalArgumentException("Active depth slice must be at least 0");
        }

        this.width = width;
        this.height = height;
        this.depth = depth;

        this.useDepthTexture = useDepthTexture;
        this.activeLayer = activeLayer;

        activeDepthPlane = activeDepth;

        textureTarget = target;
        colorTargetFormats = colorFormats;
    }

    /**
     * Set the requested Target that defines which dimensions and the number of
     * layers of the Textures created by any TextureSurface using these options.
     * The target primarily refers to the target of the color buffers, as color
     * textures support more targets than depth textures, and Framework
     * implementations may need to use a different target when creating depth
     * textures.
     * 
     * @param textureTarget The new Target
     * @return A new TextureSurfaceOptions identical to this, except with the
     *         new target
     * @throws NullPointerException if textureTarget is null
     */
    public TextureSurfaceOptions setTarget(Target textureTarget) {
        return new TextureSurfaceOptions(textureTarget,
                                         colorTargetFormats,
                                         width,
                                         height,
                                         depth,
                                         useDepthTexture,
                                         activeLayer,
                                         activeDepthPlane);
    }

    /**
     * Set the requested number of color buffers and the TextureFormats for each
     * of the buffers. Certain graphical hardware is capable of rendering into
     * multiple color buffers using GLSL shaders. In many cases, only one color
     * buffer is required. When created, a TextureSurface will have one Texture
     * for each of the requested formats (assuming it supports multiple
     * buffers). A null array is equivalent to a 0-length array.
     * 
     * @param formats The new array of formats for the color buffers
     * @return A new TextureSurfaceOptions identical to this, except with the
     *         new width
     * @throws NullPointerException if formats contains any null element
     * @throws IllegalArgumentException if formats contains the DEPTH format
     */
    public TextureSurfaceOptions setColorBufferFormats(TextureFormat... formats) {
        TextureFormat[] copy = (formats == null ? null : Arrays.copyOf(formats,
                                                                       formats.length));
        return new TextureSurfaceOptions(textureTarget,
                                         copy,
                                         width,
                                         height,
                                         depth,
                                         useDepthTexture,
                                         activeLayer,
                                         activeDepthPlane);
    }

    /**
     * Set the width size of the TextureSurface and any Textures it may need to
     * create.
     * 
     * @param width The new width
     * @return A new TextureSurfaceOptions identical to this, except with the
     *         new width
     * @throws IllegalArgumentException if width < 1
     */
    public TextureSurfaceOptions setWidth(int width) {
        return new TextureSurfaceOptions(textureTarget,
                                         colorTargetFormats,
                                         width,
                                         height,
                                         depth,
                                         useDepthTexture,
                                         activeLayer,
                                         activeDepthPlane);
    }

    /**
     * Set the height size of the TextureSurface and any Textures it may need to
     * create. The height will be ignored when the target is T_1D.
     * 
     * @param height The new height
     * @return A new TextureSurfaceOptions identical to this, except with the
     *         new height
     * @throws IllegalArgumentException if height < 1
     */
    public TextureSurfaceOptions setHeight(int height) {
        return new TextureSurfaceOptions(textureTarget,
                                         colorTargetFormats,
                                         width,
                                         height,
                                         depth,
                                         useDepthTexture,
                                         activeLayer,
                                         activeDepthPlane);
    }

    /**
     * Set the depth size of the TextureSurface and any Textures it may need to
     * create. The depth will be ignored when the target isn't T_3D.
     * 
     * @param depth The new depth
     * @return A new TextureSurfaceOptions identical to this, except with the
     *         new depth
     * @throws IllegalArgumentException if depth < 1
     */
    public TextureSurfaceOptions setDepth(int depth) {
        return new TextureSurfaceOptions(textureTarget,
                                         colorTargetFormats,
                                         width,
                                         height,
                                         depth,
                                         useDepthTexture,
                                         activeLayer,
                                         activeDepthPlane);
    }

    /**
     * Set whether or not the created TextureSurface will have a Texture that
     * stores the rendered depth information, in addition to any Textures used
     * to store the color buffer data. Not all Targets support depth textures,
     * so this could cause surface creation to fail.
     * 
     * @param useDepthTexture True if a depth texture should be created
     * @return A new TextureSurfaceOptions identical to this, except with the
     *         new depth texture parameter
     */
    public TextureSurfaceOptions setUseDepthTexture(boolean useDepthTexture) {
        return new TextureSurfaceOptions(textureTarget,
                                         colorTargetFormats,
                                         width,
                                         height,
                                         depth,
                                         useDepthTexture,
                                         activeLayer,
                                         activeDepthPlane);
    }

    /**
     * Set the initial layer that will be rendered into if the default
     * {@link Framework#queue(Surface, RenderPass)} is used.
     * 
     * @param activeLayer The active layer
     * @return A new TextureSurfaceOptions identical to this, except with the
     *         new active layer
     * @throws IllegalArgumentException if activeLayer < 0
     */
    public TextureSurfaceOptions setActiveLayer(int activeLayer) {
        return new TextureSurfaceOptions(textureTarget,
                                         colorTargetFormats,
                                         width,
                                         height,
                                         depth,
                                         useDepthTexture,
                                         activeLayer,
                                         activeDepthPlane);
    }

    /**
     * Set the initial depth slice that will be rendered into if the target is
     * T_3D and the default {@link Framework#queue(Surface, RenderPass)} is
     * used.
     * 
     * @param activeDepthPlane The active depth
     * @return A new TextureSurfaceOptions identical to this, except with the
     *         new active depth slice
     * @throws IllegalArgumentException if activeDepthPlane < 0
     */
    public TextureSurfaceOptions setActiveDepthPlane(int activeDepthPlane) {
        return new TextureSurfaceOptions(textureTarget,
                                         colorTargetFormats,
                                         width,
                                         height,
                                         depth,
                                         useDepthTexture,
                                         activeLayer,
                                         activeDepthPlane);
    }

    /**
     * @return The requested Target of the Textures used by the created
     *         TextureSurface
     */
    public Target getTarget() {
        return textureTarget;
    }

    /**
     * The requested TextureFormat of the given color buffer index. New graphics
     * hardware can support rendering into multiple color buffers at the same
     * time.
     * 
     * @param colorBuffer The color buffer index
     * @return The color buffer's requested TextureFormat
     * @throws IndexOutOfBoundsException if colorBuffer < 0 or >=
     *             {@link #getNumColorBuffers()}
     */
    public TextureFormat getColorBufferFormat(int colorBuffer) {
        return colorTargetFormats[colorBuffer];
    }

    /**
     * @return The number of requested color buffers
     */
    public int getNumColorBuffers() {
        return colorTargetFormats.length;
    }

    /**
     * @return True if depth information is stored in a usable depth Texture, or
     *         false if depth information can be stored in a non-external source
     */
    public boolean hasDepthTexture() {
        return useDepthTexture;
    }

    /**
     * @return The width for Textures for the created TextureSurface
     */
    public int getWidth() {
        return width;
    }

    /**
     * @return The height for Textures of surfaces with a Target of T_2D, T_3D
     *         or T_CUBEMAP
     */
    public int getHeight() {
        return height;
    }

    /**
     * @return The depth for Textures of surfaces with a Target of T_3D
     */
    public int getDepth() {
        return depth;
    }

    /**
     * @return The initial active layer for surfaces with multiple layers (e.g.
     *         cube maps)
     */
    public int getActiveLayer() {
        return activeLayer;
    }

    /**
     * @return The initial active depth layer for 3D textures
     */
    public int getActiveDepthPlane() {
        return activeDepthPlane;
    }
}
