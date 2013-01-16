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
package com.ferox.resource.geom.text;

/**
 * <p>
 * A Glyph represents the bounding-box and positioning information for a single
 * character within a font. It does not distinguish between AWT's various types
 * of GlyphMetrics for multiple character sequences.
 * </p>
 * <p>
 * It assumes a fairly standard English character set that proceeds from
 * left-to-right. Each glyph has a local origin that represents the "lower-left"
 * corner of the character. This is not the absolute location of the character.
 * Characters may be positioned above or below this baseline, or to the left or
 * right of it. Above and to the right represent positive coordinate values.
 * </p>
 * 
 * @author Michael Ludwig
 */
public class Glyph {
    private final float advance;
    private final float tcL, tcR, tcT, tcB;
    private final float x, y;
    private final float width, height;

    /**
     * <p>
     * Create a new Glyph with the given values. The texture coordinates
     * specified in tcL, tcR, tcT and tcB must be the values required to access
     * the glyph from its associated CharacterSet.
     * </p>
     * <p>
     * The x, y positions and width and height, however, represent the
     * positioning and dimensions of the glyph along a line of text, not within
     * a CharacterSet.
     * </p>
     * 
     * @param advance The advance needed for the character
     * @param tcL The texture coordinate for the left edge
     * @param tcR The texture coordinate for the right edge
     * @param tcB The texture coordinate for the bottom edge
     * @param tcT The texture coordinate for the top edge
     * @param x The x offset of this character from its origin
     * @param y The y offset of this character from its origin
     * @param width The width of the character
     * @param height The height of the character
     */
    public Glyph(float advance, float tcL, float tcR, float tcB, float tcT, float x,
                 float y, float width, float height) {
        this.advance = advance;
        this.tcL = tcL;
        this.tcR = tcR;
        this.tcT = tcT;
        this.tcB = tcB;

        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /**
     * Return the horizontal distance to advance for the next character. The
     * advance is measured from this character's origin (not getX()).
     * 
     * @return This glyph's advance
     */
    public float getAdvance() {
        return advance;
    }

    /**
     * Return the texture coord that represents the left edge within the
     * associated char-set's texture.
     * 
     * @return This glyph's left texture coordinate
     */
    public float getTexCoordLeft() {
        return tcL;
    }

    /**
     * Return the texture coord that represents the right edge within the
     * associated char-set's texture.
     * 
     * @return This glyph's right texture coordinate
     */
    public float getTexCoordRight() {
        return tcR;
    }

    /**
     * Return the texture coord that represents the top edge within the
     * associated char-set's texture.
     * 
     * @return This glyph's top texture coordinate
     */
    public float getTexCoordTop() {
        return tcT;
    }

    /**
     * Return the texture coord that represents the bottom edge within the
     * associated char-set's texture.
     * 
     * @return This glyph's bottom texture coordinate
     */
    public float getTexCoordBottom() {
        return tcB;
    }

    /**
     * <p>
     * Return the appropriate x distance relative to this metric's local origin
     * of the character's quad's left edge.
     * </p>
     * <p>
     * A negative implies to the left of the origin.
     * </p>
     * 
     * @return The x coordinate of the glyph in a line of text
     */
    public float getX() {
        return x;
    }

    /**
     * <p>
     * Return the appropriate y distance relative to this metric's local origin
     * of the character's bottom edge.
     * </p>
     * <p>
     * A negative value implies the character is position below the baseline of
     * the font.
     * </p>
     * 
     * @return The y coordinate of the glyph in a line of text
     */
    public float getY() {
        return y;
    }

    /**
     * @return The width of the character's quad, which will be positive.
     */
    public float getWidth() {
        return width;
    }

    /**
     * @return The height of the character's quad, which will be positive.
     */
    public float getHeight() {
        return height;
    }
}
