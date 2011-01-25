package com.ferox.math;


public abstract class ReadOnlyColor4f {
    private static final float DEFAULT_FACTOR = .7f;
    
    public Color4f brighter(Color4f result) {
        return brighter(1f / DEFAULT_FACTOR, result);
    }
    
    public Color4f brighter(float factor, Color4f result) {
        if (factor < 1f)
            throw new IllegalArgumentException("Brightening factor must be at least 1, not: " + factor);
        
        if (result == null)
            result = new Color4f();
        
        float minBrightness = factor / (255f * factor - 255f);
        float r = getRedHDR();
        float g = getGreenHDR();
        float b = getBlueHDR();
        
        // if the color is black, then the brighter color must be a gray
        if (r == 0 && g == 0 && b == 0)
           return result.set(minBrightness, minBrightness, minBrightness, getAlpha());

        // check for > 0 here so that non-black colors don't have component creep,
        // this is so that dark blue brightens into a brighter blue, without adding
        // any red or green
        if (r > 0 && r < minBrightness) 
            r = minBrightness;
        if (g > 0 && g < minBrightness) 
            g = minBrightness;
        if (b > 0 && b < minBrightness) 
            b = minBrightness;

        return result.set(r * factor, g * factor, b * factor, getAlpha());
    }
    
    public Color4f darker(Color4f result) {
        return darker(DEFAULT_FACTOR, result);
    }
    
    public Color4f darker(float factor, Color4f result) {
        if (factor <= 0f || factor > 1f)
            throw new IllegalArgumentException("Darkening factor must be in the range (0, 1], not: " + factor);

        if (result == null)
            result = new Color4f();
        return result.set(Math.max(0f, factor * getRedHDR()),
                          Math.max(0f, factor * getGreenHDR()),
                          Math.max(0f, factor * getBlueHDR()),
                          getAlpha());
    }

    public abstract float getRed();
    
    public abstract float getGreen();
    
    public abstract float getBlue();
    
    public abstract float getAlpha();
    
    public abstract float getRedHDR();
    
    public abstract float getGreenHDR();
    
    public abstract float getBlueHDR();
    
    public int getRGBA() {
        byte red = (byte) (getRed() * 255);
        byte green = (byte) (getGreen() * 255);
        byte blue = (byte) (getBlue() * 255);
        byte alpha = (byte) (getAlpha() * 255);
        
        int packed = (red << 24) | (green << 16) | (blue << 8) | (alpha);
        return packed;
    }
    
    public float get(int component) {
        switch(component) {
        case 0:
            return getRed();
        case 1:
            return getGreen();
        case 2:
            return getBlue();
        case 3:
            return getAlpha();
        default:
            throw new IndexOutOfBoundsException("Component must be between 0 and 3, not: " + component);
        }
    }
    
    public float getHDR(int component) {
        switch(component) {
        case 0:
            return getRedHDR();
        case 1:
            return getGreenHDR();
        case 2:
            return getBlueHDR();
        case 3:
            return getAlpha();
        default:
            throw new IndexOutOfBoundsException("Component must be between 0 and 3, not: " + component);
        }
    }
    
    public void get(float[] vals, int offset) {
        vals[offset + 0] = getRed();
        vals[offset + 1] = getGreen();
        vals[offset + 2] = getBlue();
        vals[offset + 3] = getAlpha();
    }
    
    public void getHDR(float[] vals, int offset) {
        vals[offset + 0] = getRedHDR();
        vals[offset + 1] = getGreenHDR();
        vals[offset + 2] = getBlueHDR();
        vals[offset + 3] = getAlpha();
    }
    
    public boolean equals(ReadOnlyColor4f color, boolean asHDR) {
        if (color == null)
            return false;
        
        if (asHDR) {
            return Float.compare(getRedHDR(), color.getRedHDR()) == 0 &&
                   Float.compare(getGreenHDR(), color.getGreenHDR()) == 0 &&
                   Float.compare(getBlueHDR(), color.getBlueHDR()) == 0 &&
                   Float.compare(getAlpha(), color.getAlpha()) == 0;
        } else {
            return Float.compare(getRed(), color.getRed()) == 0 &&
                   Float.compare(getGreen(), color.getGreen()) == 0 &&
                   Float.compare(getBlue(), color.getBlue()) == 0 &&
                   Float.compare(getAlpha(), color.getAlpha()) == 0;
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ReadOnlyColor4f))
            return false;
        return equals((ReadOnlyColor4f) o, true);
    }
    
    @Override
    public int hashCode() {
        int result = 17;

        result += 31 * result + Float.floatToIntBits(getRedHDR());
        result += 31 * result + Float.floatToIntBits(getGreenHDR());
        result += 31 * result + Float.floatToIntBits(getBlueHDR());
        result += 31 * result + Float.floatToIntBits(getAlpha());

        return result;
    }
}
