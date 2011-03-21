package com.ferox.renderer;

/**
 * DisplayMode is an immutable object describing screen dimensions for a monitor
 * display. Currently it only contains width and height, as the bit depth and
 * other aspects are controlled by other properties for the OnscreenSurface.
 * DisplayModes can be used to change screen resolutions.
 * 
 * @author Michael Ludwig
 */
public class DisplayMode {
    private final int width;
    private final int height;

    /**
     * Create a new DisplayMode with the given screen dimensions, measured in
     * pixels.
     * 
     * @param width The screen width
     * @param height The screen height
     * @throws IllegalArgumentException if width or height is less than 1
     */
    public DisplayMode(int width, int height) {
        if (width < 1 || height < 1)
            throw new IllegalArgumentException("Invalid dimensions: " + width + ", " + height);
        this.width = width;
        this.height = height;
    }

    /**
     * Get the width of the screen in pixels.
     * 
     * @return The screen width dimension
     */
    public int getWidth() {
        return width;
    }

    /**
     * Get the height of the screen in pixels.
     * 
     * @return The screen height dimension
     */
    public int getHeight() {
        return height;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DisplayMode))
            return false;
        DisplayMode d = (DisplayMode) o;
        return d.width == width && d.height == height;
    }

    @Override
    public int hashCode() {
        return (37 * height) ^ (17 * width);
    }

    @Override
    public String toString() {
        return "[DisplayMode width=" + width + ", height=" + height + "]";
    }
}
