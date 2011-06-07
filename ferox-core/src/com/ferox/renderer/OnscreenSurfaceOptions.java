package com.ferox.renderer;

/**
 * OnscreenSurfaceOptions represents the set of configurable parameters used to
 * create an OnscreenSurface. These parameters are requests to the Framework and
 * may be ignored, but this is unlikely. Each OnscreenSurfaceOptions instance is
 * immutable, the setters available return new instances that match the calling
 * instance except for the new parameter value.
 * 
 * @author Michael Ludwig
 */
public final class OnscreenSurfaceOptions {
    /**
     * The format for the depth component of the surface fragment.
     */
    public static enum DepthFormat {
        /**
         * Use 16 bits to store depth information.
         */
        DEPTH_16BIT,
        /**
         * Use 24 bits to store depth information.
         */
        DEPTH_24BIT,
        /**
         * Use 32 bits to store depth information.
         */
        DEPTH_32BIT,
        /**
         * There should be no depth buffer.
         */
        NONE,
        /**
         * Depth buffer exists but doesn't match specified enum values
         */
        UNKNOWN
    }

    /**
     * The format for the stencil buffer of the surface.
     */
    public static enum StencilFormat {
        /**
         * Use 16 bits for each fragment.
         */
        STENCIL_16BIT,
        /**
         * Use 8 bits for each fragment.
         */
        STENCIL_8BIT,
        /**
         * Use 4 bits for each fragment.
         */
        STENCIL_4BIT,
        /**
         * Use only 1 bit for each fragment.
         */
        STENCIL_1BIT,
        /**
         * There shouldn't be any stencil buffer.
         */
        NONE,
        /**
         * Stencil buffer exists but doesn't match specified enum values
         */
        UNKNOWN
    }

    /**
     * The type of fullscreen anti-aliasing to apply to the surface.
     */
    public static enum AntiAliasMode {
        /**
         * Two samples per-pixel multisampling.
         */
        TWO_X,
        /**
         * Four samples per-pixel multisampling.
         */
        FOUR_X,
        /**
         * Eight samples per-pixel multisampling.
         */
        EIGHT_X,
        /**
         * No fullscreen anti-aliasing is performed.
         */
        NONE,
        /**
         * Anti-aliasing is performed with a method that doesn't match known
         * enum values.
         */
        UNKNOWN
    }
    
    private final boolean undecorated;
    private final boolean resizable;
    
    private final int width;
    private final int height;
    
    private final int x;
    private final int y;
    
    private final DisplayMode fullMode;
    
    private final DepthFormat depth;
    private final AntiAliasMode aa;
    private final StencilFormat stencil;

    /**
     * Create a new OnscreenSurfaceOptions with the following default
     * parameters:
     * <ul>
     * <li>{@link #getDepthFormat()} returns DEPTH_24BIT</li>
     * <li>{@link #getAntiAliasMode()} returns NONE</li>
     * <li>{@link #getStencilFormat()} returns NONE</li>
     * <li>{@link #isUndecorated()} returns false</li>
     * <li>{@link #isResizable()} returns false</li>
     * <li>{@link #getX()} returns 0</li>
     * <li>{@link #getY()} returns 0</li>
     * <li>{@link #getWidth()} returns 600</li>
     * <li>{@link #getHeight()} returns 600</li>
     * <li>{@link #getFullscreenMode()} returns null</li>
     * </ul>
     */
    public OnscreenSurfaceOptions() {
        this(DepthFormat.DEPTH_24BIT, AntiAliasMode.NONE, StencilFormat.NONE, 
             false, false, 0, 0, 600, 600, null);
    }
    
    private OnscreenSurfaceOptions(DepthFormat d, AntiAliasMode a, StencilFormat s, 
                                   boolean undecorated, boolean resizable, int x, int y, 
                                   int width, int height, DisplayMode ff) {
        if (d == null || a == null || s == null)
            throw new NullPointerException("Format options cannot be null");
        if (width < 1 || height < 1)
            throw new IllegalArgumentException("Window dimensions must be at least 1");
        
        this.undecorated = undecorated;
        this.resizable = resizable;
        this.width = width;
        this.height = height;
        this.x = x;
        this.y = y;
        
        fullMode = ff;
        depth = d;
        aa = a;
        stencil = s;
    }

    /**
     * Set the requested initial width of the surface, when it's windowed.
     * 
     * @param width The new width
     * @return A new OnscreenSurfaceOptions equivalent to this one, except with
     *         the new width
     * @throws IllegalArgumentException if width < 1
     */
    public OnscreenSurfaceOptions setWidth(int width) {
        return new OnscreenSurfaceOptions(depth, aa, stencil, undecorated, resizable, x, y, 
                                          width, height, fullMode);
    }
    
    /**
     * Set the requested initial height of the surface, when it's windowed.
     * 
     * @param height The new height
     * @return A new OnscreenSurfaceOptions equivalent to this one, except with
     *         the new height
     * @throws IllegalArgumentException if height < 1
     */
    public OnscreenSurfaceOptions setHeight(int height) {
        return new OnscreenSurfaceOptions(depth, aa, stencil, undecorated, resizable, x, y, 
                                          width, height, fullMode);
    }
    
    /**
     * Set the requested initial x coordinate of the surface, when it's windowed.
     * 
     * @param x The new x value
     * @return A new OnscreenSurfaceOptions equivalent to this one, except with
     *         the new x value
     */
    public OnscreenSurfaceOptions setX(int x) {
        return new OnscreenSurfaceOptions(depth, aa, stencil, undecorated, resizable, x, y, 
                                          width, height, fullMode);
    }
    
    /**
     * Set the requested initial y coordinate of the surface, when it's windowed.
     * 
     * @param y The new y value
     * @return A new OnscreenSurfaceOptions equivalent to this one, except with
     *         the new y value
     */
    public OnscreenSurfaceOptions setY(int y) {
        return new OnscreenSurfaceOptions(depth, aa, stencil, undecorated, resizable, x, y, 
                                          width, height, fullMode);
    }

    /**
     * Set the requested DepthFormat of the surface.
     * 
     * @param depth The new DepthFormat
     * @return A new OnscreenSurfaceOptions equivalent to this one, except with
     *         the new format
     * @throws NullPointerException if depth is null
     */
    public OnscreenSurfaceOptions setDepthFormat(DepthFormat depth) {
        return new OnscreenSurfaceOptions(depth, aa, stencil, undecorated, resizable, x, y, 
                                          width, height, fullMode);
    }
    
    /**
     * Set the requested StencilFormat of the surface.
     * 
     * @param stencil The new StencilFormat
     * @return A new OnscreenSurfaceOptions equivalent to this one, except with
     *         the new format
     * @throws NullPointerException if stencil is null
     */
    public OnscreenSurfaceOptions setStencilFormat(StencilFormat stencil) {
        return new OnscreenSurfaceOptions(depth, aa, stencil, undecorated, resizable, x, y, 
                                          width, height, fullMode);
    }

    /**
     * Set the requested AntiAliasMode of the surface.
     * 
     * @param aa The new AntiAliasMode
     * @return A new OnscreenSurfaceOptions equivalent to this one, except with
     *         the new mode
     * @throws NullPointerException if aa is null
     */
    public OnscreenSurfaceOptions setAntiAliasMode(AntiAliasMode aa) {
        return new OnscreenSurfaceOptions(depth, aa, stencil, undecorated, resizable, x, y, 
                                          width, height, fullMode);
    }

    /**
     * Set the whether or not the surface is undecorated when it's windowed.
     * 
     * @param undecorated True if the surface has no window decorations
     * @return A new OnscreenSurfaceOptions equivalent to this one, except with
     *         the new undecorated value
     */
    public OnscreenSurfaceOptions setUndecorated(boolean undecorated) {
        return new OnscreenSurfaceOptions(depth, aa, stencil, undecorated, resizable, x, y, 
                                          width, height, fullMode);
    }
    
    /**
     * Set the whether or not the surface is user resizable when it's windowed.
     * 
     * @param resizable True if the surface cannot be resized
     * @return A new OnscreenSurfaceOptions equivalent to this one, except with
     *         the new resizable value
     */
    public OnscreenSurfaceOptions setResizable(boolean resizable) {
        return new OnscreenSurfaceOptions(depth, aa, stencil, undecorated, resizable, x, y, 
                                          width, height, fullMode);
    }

    /**
     * Set the requested DisplayMode for the surface. If this is null, the
     * surface will initially be windowed. If it is not null, the surface will
     * begin in fullscreen mode using a supported DisplayMode that matches the
     * requested mode as closely as possible
     * 
     * @param fullMode The new fullscreen DisplayMode or null
     * @return A new OnscreenSurfaceOptions equivalent to this one, except with
     *         the new DisplayMode
     */
    public OnscreenSurfaceOptions setFullscreenMode(DisplayMode fullMode) {
        return new OnscreenSurfaceOptions(depth, aa, stencil, undecorated, resizable, x, y, 
                                          width, height, fullMode);
    }
    
    /**
     * @return The initial x coordinate of the OnscreenSurface when windowed
     */
    public int getX() { return x; }
    
    /**
     * @return The initial y coordinate of the OnscreenSurface when windowed
     */
    public int getY() { return y; }
    
    /**
     * @return Whether or not the window is undecorated
     */
    public boolean isUndecorated() { return undecorated; }
    
    /**
     * @return Whether or not the window is user-resizable
     */
    public boolean isResizable() { return resizable; }

    /**
     * @return The requested DisplayMode. If null then the surface will
     *         initially be windowed, else it will be fullscreen with a
     *         supported DisplayMode closest to the requested
     */
    public DisplayMode getFullscreenMode() { return fullMode; }
    
    /**
     * @return The requested DepthFormat for the depth buffer
     */
    public DepthFormat getDepthFormat() { return depth; }
    
    /**
     * @return The requested StencilFormat for the stencil buffer
     */
    public StencilFormat getStencilFormat() { return stencil; }
    
    /**
     * @return The requested AntiAliasMode for the buffers
     */
    public AntiAliasMode getAntiAliasMode() { return aa; }
    
    /**
     * @return The initial width of the surface when windowed
     */
    public int getWidth() { return width; }
    
    /**
     * @return The initial height of the surface when windowed
     */
    public int getHeight() { return height; }
}
