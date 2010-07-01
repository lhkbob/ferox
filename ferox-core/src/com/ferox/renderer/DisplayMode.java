package com.ferox.renderer;

public class DisplayMode {
    private final int width;
    private final int height;
    
    public DisplayMode(int width, int height) {
        if (width < 1 || height < 1)
            throw new IllegalArgumentException("Invalid dimensions: " + width + ", " + height);
        this.width = width;
        this.height = height;
    }
    
    public int getWidth() {
        return width;
    }
    
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
