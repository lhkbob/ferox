package com.ferox.math;

public final class Color4f extends ReadOnlyColor4f {
    private static final int RED = 0;
    private static final int GREEN = 1;
    private static final int BLUE = 2;
    
    private final float[] rgba;
    private final float[] rgbaHDR;
    private float alpha;
    
    public Color4f() {
        rgba = new float[] { 0f, 0f, 0f };
        rgbaHDR = new float[] { 0f, 0f, 0f };
    }
    
    public Color4f(Color4f color) {
        this();
        set(color);
    }
    
    public Color4f(float red, float green, float blue) {
        this();
        set(red, green, blue);
    }
    
    public Color4f(float red, float green, float blue, float alpha) {
        this();
        set(red, green, blue, alpha);
    }
    
    public Color4f(int rgba) {
        this();
        set(rgba);
    }
    
    public Color4f set(ReadOnlyColor4f color) {
        return set(color.getRedHDR(), color.getGreenHDR(), color.getBlueHDR(), color.getAlpha());
    }
    
    public Color4f set(int rgba) {
        int r = (rgba >> 24) & 0xff;
        int g = (rgba >> 16) & 0xff;
        int b = (rgba >> 8) & 0xff;
        int a = (rgba) & 0xff;
        return set(r / 255f, g / 255f, b / 255f, a / 255f);
    }
    
    public Color4f set(float red, float green, float blue) {
        return set(red, green, blue, 1f);
    }
    
    public Color4f set(float red, float green, float blue, float alpha) {
        return setRed(red).
               setGreen(green).
               setBlue(blue).
               setAlpha(alpha);
    }
    
    public Color4f setRed(float red) {
        return setValue(red, RED);
    }
    
    public Color4f setGreen(float green) {
        return setValue(green, GREEN);
    }
    
    public Color4f setBlue(float blue) {
        return setValue(blue, BLUE);
    }
    
    public Color4f setAlpha(float alpha) {
        this.alpha = Math.max(0f, Math.min(alpha, 1f));
        return this;
    }
    
    private Color4f setValue(float v, int i) {
        rgbaHDR[i] = Math.max(0f, v);
        rgba[i] = Math.min(rgbaHDR[i], 1f);
        return this;
    }
    
    public Color4f set(int index, float value) {
        if (index == 3)
            return setAlpha(value);
        else if (index >= 0 && index < 3)
            return setValue(value, index);
        else
            throw new IndexOutOfBoundsException("Illegal index, must be in [0, 3], not: " + index);
    }
    
    public Color4f set(float[] values, int offset) {
        return set(values[offset], values[offset + 1], values[offset + 2], values[offset + 3]);
    }
    
    @Override
    public float getRed() {
        return rgba[RED];
    }

    @Override
    public float getGreen() {
        return rgba[GREEN];
    }

    @Override
    public float getBlue() {
        return rgba[BLUE];
    }

    @Override
    public float getAlpha() {
        return alpha;
    }

    @Override
    public float getRedHDR() {
        return rgbaHDR[RED];
    }

    @Override
    public float getGreenHDR() {
        return rgbaHDR[GREEN];
    }

    @Override
    public float getBlueHDR() {
        return rgbaHDR[BLUE];
    }
}
