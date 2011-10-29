package com.ferox.util.geom.text;

import java.awt.font.LineMetrics;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import com.ferox.renderer.Renderer.PolygonType;
import com.ferox.resource.VertexAttribute;
import com.ferox.resource.VertexBufferObject.StorageMode;
import com.ferox.util.geom.Geometry;

/**
 * <p>
 * Text represents a Geometry that can generate laid-out text based on a
 * {@link CharacterSet}. It assumes that the text is laid-out left to right and
 * Unicode-16 is untested.
 * </p>
 * <p>
 * Text treats \t as TAB_SPACE_COUNT spaces in a row. \n and \r are interpreted
 * as well, causing a newline to appear if the layout encounters: \n, \r, or
 * \n\r. Spaces are not placed at the beginning of a new line, so that text is
 * justified to the left-edge (this is ignored for the 1st line). The layout
 * policy attempts to make a reasonably attractive block of text, suitable for a
 * text area, etc. The texture coordinates generated are intended to access the
 * Text's CharacterSet. The normals are generated such that they are forward
 * facing when text is aligned left to right.
 * </p>
 * <p>
 * Text requires that a specific set of Renderer states be used to render the
 * text appropriately. The "characters" within a Text instance are appropriately
 * sized quads intended to access its CharacterSet's Texture. Thus, a Renderer
 * must be configured to use the CharacterSet's texture and be accessed by the
 * texture coordinates defined by the text geometry. Additionally, blending or
 * alpha testing should be used so that the CharacterSet's transparent
 * background is properly ignored.
 * </p>
 * <p>
 * After modifying a Text's textual content, via {@link #setText(String)}, or by
 * modifying its layout parameters, via {@link #setWrapWidth(float)} or
 * {@link #setCharacterSet(CharacterSet)}, the Text will become dirty and will
 * require an update with the Framework. As always, this update can occur
 * automatically.
 * </p>
 * <p>
 * It is HIGHLY recommended that CharacterSets are shared by multiple instances
 * of Text that need the same font.
 * </p>
 * 
 * @author Michael Ludwig
 */
public class Text implements Geometry {
    /** Number of spaces a tab character represents. Initially this is set to 4. */
    public static int TAB_SPACE_COUNT = 4;

    private CharacterSet charSet;
    private String text;

    private float width;
    private float height;

    private float maxTextWidth; // if <= 0, then no wrapping is done
    private float scale;
    
    /**
     * Create a Text that uses the given CharacterSet and has its text set to
     * the empty string.
     * 
     * @param charSet The CharacterSet storing font information
     * @param type The compile type to use for Text
     * @throws NullPointerException if charSet is null
     */
    public Text(CharacterSet charSet, StorageMode mode) {
        this(charSet, "", mode);
    }

    /**
     * Create a Text with the given CharacterSet and initial text value.
     * 
     * @param charSet The CharacterSet storing font information
     * @param text The initial text content
     * @param type The compile type to use for Text
     * @throws NullPointerException if charSet is null
     */
    public Text(CharacterSet charSet, String text, StorageMode mode) {
        // avoid the setters so we can do one layout at the end
        if (charSet == null)
            throw new NullPointerException("CharacterSet cannot be null");
        this.charSet = charSet;
        this.text = (text == null ? "" : text);
        maxTextWidth = -1f;
        scale = 1f;

        layoutText();
    }
    
    /**
     * @return The current scale factor used to scale the vertices within this
     *         Text instance
     */
    public float getScale() {
        return scale;
    }

    /**
     * Set the scale factor that scales the vertices of each quad within the
     * Text geometry. If scale is one, the quads are sized so the text appears
     * the appropriate size when using an orthographic projection with a 1x1
     * pixel mapping. If using other projections, it may be desired to use a
     * higher-point Font but still use small quads.
     * 
     * @param scale The new scale factor
     * @throws IllegalArgumentException if scale is less than or equal to 0
     */
    public void setScale(float scale) {
        if (scale <= 0f)
            throw new IllegalArgumentException("Text scale cannot be negative: " + scale);
        this.scale = scale;
        layoutText();
    }

    /**
     * @see #setWrapWidth(float)
     * @return The wrap width used by this Text
     */
    public float getWrapWidth() {
        return maxTextWidth;
    }

    /**
     * <p>
     * Get the current width of this Text. The returned value is suitable for
     * drawing a tightly packed box around the text.
     * </p>
     * <p>
     * The center of the block of text is considered to be the origin, and the
     * left edge extends to an x-value with
     * <code>-{@link #getTextWidth()} / 2</code> and the right edge extends to
     * an x-value with <code>{@link #getTextWidth()} / 2</code>.
     * </p>
     * 
     * @return The width of the text
     */
    public float getTextWidth() {
        return width;
    }

    /**
     * <p>
     * Get the current height of this Text. The returned value can be used to
     * draw a tightly packed box around the text.
     * </p>
     * <p>
     * The center of the block of text is considered to be the origin, and the
     * bottom edge extends to a y-value with
     * <code>-{@link #getTextWidth()} / 2</code> and the top edge extends to an
     * y-value with <code>{@link #getTextWidth()} / 2</code>. This includes the ascent and
     * descent of the font.
     * </p>
     * 
     * @return The height of the text
     */
    public float getTextHeight() {
        return height;
    }

    /**
     * <p>
     * Set the wrap width that determines how text is laid out. A value <= 0
     * implies that no wrapping is formed. In this case text will only be on
     * multiple lines if '\n', '\r' or '\n\r' are encountered.
     * </p>
     * <p>
     * If it's positive, then this value represents the maximum allowed width of
     * a line of text. Words that would extend beyond this will be placed on a
     * newline. If a word can't fit on a line, its characters will be wrapped.
     * Punctuation proceeding words are treated as part of the word.
     * </p>
     * <p>
     * As far as layout works, the text is centered about its local origin. See
     * {@link #getTextWidth()} and {@link #getTextHeight()} for details. In
     * multiline text, subsequent lines start at progressively negative
     * y-values. A rectangle with corners (-getTextWidth()/2,-getTextHeight()/2)
     * and (getTextWidth()/2, getTextHeight()/2) would tightly enclose the body
     * of text.
     * </p>
     * <p>
     * Characters that are not present in the CharacterSet are rendered with the
     * missing glyph for that set's font.
     * </p>
     * <p>
     * This causes the Text's layout to be recomputed.
     * </p>
     * 
     * @param maxWidth The maximum width of the laid-out text
     */
    public void setWrapWidth(float maxWidth) {
        maxTextWidth = maxWidth;
        layoutText();
    }

    /**
     * <p>
     * Set the text that will be rendered. If null is given, the empty string is
     * used instead.This causes the Text's layout to be recomputed.
     * </p>
     * 
     * @see #setWrapWidth(float)
     * @param text The new String text to use
     */
    public void setText(String text) {
        if (text == null)
            text = "";

        this.text = text;
        layoutText();
    }

    /**
     * Return the text that should be rendered by this geometry, assuming that
     * it's layout is not dirty, and that it's up-to-date with the Framework.
     * This will not be null.
     * 
     * @return The text that will be rendered
     */
    public String getText() {
        return text;
    }

    /**
     * @return The CharacterSet used to display the text. This will not be null.
     */
    public CharacterSet getCharacterSet() {
        return charSet;
    }

    /**
     * <p>
     * Set the CharacterSet that determines the size and font of the rendered
     * characters within this Text instance. This should be shared across Text
     * instances that use the same font. This causes the layout to be
     * recomputed.
     * </p>
     * 
     * @param set The new CharacterSet for rendering characters
     * @throws NullPointerException if set is null
     */
    public void setCharacterSet(CharacterSet set) {
        if (set == null)
            throw new NullPointerException("Cannot use a null CharacterSet");

        charSet = set;
        layoutText();
    }
    
    private FloatBuffer reuseBuffer(VertexAttribute old, int newLen) {
        if (old == null || old.getData().capacity() < newLen || old.getData().capacity() * .75f > newLen) {
            // no old data, or old data is too small, or old data is too big
            return ByteBuffer.allocateDirect(newLen * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        } else
            return old.getData();
    }
    
    private IntBuffer reuseBuffer(IntBuffer old, int newLen) {
        // unlike reusing float buffers, which can contain garbage, we need
        // an exact match for indices
        if (old == null || old.capacity() != newLen)
            return ByteBuffer.allocateDirect(newLen * 4).order(ByteOrder.nativeOrder()).asIntBuffer();
        else
            return old;
    }

    private void layoutText() {
        LineMetrics lm = charSet.getFont().getLineMetrics(text, charSet.getFontRenderContext());
        TextLayout tl = new TextLayout(charSet, lm, maxTextWidth);
        float[] it2v2 = tl.doLayout(text);
        
        width = scale * tl.getMaxWidth();
        height = scale * tl.getMaxHeight();

        int vertexCount = it2v2.length / 4;
        if (vertexCount > 0) {
            FloatBuffer v = reuseBuffer(getVertices(), vertexCount * 3);
            FloatBuffer n = reuseBuffer(getNormals(), vertexCount * 3);
            FloatBuffer t = reuseBuffer(getTextureCoordinates(), vertexCount * 2);

            IntBuffer i = reuseBuffer(getIndices(), vertexCount);
            
            // compute centering information
            float xOffset = -width / 2f;//0f;
            float yOffset = height / 2f;//0f;

            // extract individual arrays from interleaved array into nio buffers
            for (int j = 0; j < vertexCount; j++) {
                // tex
                t.put(j * 2 + 0, it2v2[j * 4 + 0]);
                t.put(j * 2 + 1, it2v2[j * 4 + 1]);

                // vertex
                v.put(j * 3 + 0, scale * it2v2[j * 4 + 2] + xOffset);
                v.put(j * 3 + 1, scale * it2v2[j * 4 + 3] + yOffset);
                v.put(j * 3 + 2, 0f);

                // normal
                n.put(j * 3 + 0, 0f);
                n.put(j * 3 + 1, 0f);
                n.put(j * 3 + 2, 1f);

                // index
                i.put(j, j);
            }

            setAttribute(getVertexName(), new VertexAttribute(v, 3));
            setAttribute(getNormalName(), new VertexAttribute(n, 3));
            setAttribute(getTextureCoordinateName(), new VertexAttribute(t, 2));
            setIndices(i, PolygonType.QUADS);
        } else {
            // empty the geometry
            removeAttribute(getVertexName());
            removeAttribute(getNormalName());
            removeAttribute(getTextureCoordinateName());
            setIndices(null, PolygonType.QUADS);
        }
    }

    /** Helper class to place the characters into a multi-line block of text. */
    private static class TextLayout {
        // progress of cursor within text
        private float cursorX;
        private float cursorY;

        private final float leftEdge;
        // amount to subtract cursorY to get the next line
        private final float height;
        private final float ascent, descent;

        private float maxWidth;
        private final float wrapWidth;

        private final CharacterSet charSet;

        /*
         * If wrapWidth <= 0, then no forced wrapping is performed. charSet and
         * lm must not be null.
         */
        public TextLayout(CharacterSet charSet, LineMetrics lm, float wrapWidth) {
            leftEdge = 0f;

            height = lm.getHeight();
            ascent = lm.getAscent();
            descent = lm.getDescent();

            this.wrapWidth = wrapWidth;
            this.charSet = charSet;
        }

        /*
         * Return the height of the multi-line text block, after the last call
         * to doLayout().
         */
        public float getMaxHeight() {
            return -cursorY + descent;
        }

        /*
         * Return the max width of the multi-line text block, after the last
         * call to doLayout().
         */
        public float getMaxWidth() {
            return maxWidth;
        }

        /*
         * Layout out the given text, returning an array of coords with a usage
         * pattern of T2F_V2F
         */
        public float[] doLayout(String text) {
            // reset values for the layout
            maxWidth = leftEdge;

            cursorX = leftEdge;
            cursorY = -ascent;

            // we're being conservative here, but memory is cheap
            int len = text.length();
            int numPrims = len * 16;
            float[] coords = new float[numPrims];

            int coordIndex = 0;
            StringBuilder currentWord = new StringBuilder();
            boolean wordIsBlank = false; // blank implies whitespace or '-'
            char c;
            for (int i = 0; i < len; i++) {
                c = text.charAt(i);

                if (Character.isWhitespace(c) || c == '-') {
                    // always start a new word
                    coordIndex = placeWord(currentWord, coords, coordIndex);
                    wordIsBlank = true;
                } else if (wordIsBlank) {
                    // we're starting a real word, so add in the prior
                    // white-space
                    coordIndex = placeWord(currentWord, coords, coordIndex);
                    wordIsBlank = false;
                }

                currentWord.append(c);
            }
            // add in last word, if needed
            coordIndex = placeWord(currentWord, coords, coordIndex);

            maxWidth = Math.max(maxWidth, cursorX);
            return coords;
        }

        /*
         * Possibly move to a newline, and then place each char within the word.
         * Return the index for the next character.
         * 
         * Does nothing if word is empty, resets the word afterwards.
         */
        private int placeWord(StringBuilder word, float[] coords, int index) {
            if (word.length() == 0)
                return index;

            if (wrapWidth > 0) {
                // check if we need to move the word down
                // char-by-char wrapping happens in placeChars
                float wordWidth = getWordWidth(word);
                if (wordWidth < wrapWidth && (wordWidth + cursorX) > wrapWidth)
                    newline();
            }

            index = placeChars(word, coords, index);
            word.setLength(0);

            return index;
        }

        /*
         * Place all chars within c, moving to a newline if they can't fit.
         * Returns the index for the next character after all of c have been
         * placed.
         */
        private int placeChars(StringBuilder c, float[] coords, int index) {
            Glyph g;
            int len = c.length();
            char chr;

            for (int i = 0; i < len; i++) {
                chr = c.charAt(i);

                switch (chr) {
                case '\n':
                    newline();
                    break;
                case '\r':
                    if (i == 0 || c.charAt(i - 1) != '\n')
                        newline();
                    break;
                case '\t':
                    // advance TAB_SPACE_COUNT spaces, but don't place anything
                    g = charSet.getGlyph(' ');
                    cursorX += TAB_SPACE_COUNT * g.getAdvance();
                    break;
                case ' ':
                    // just advance the space width, but don't place glyphs
                    // only place space if we've moved off of left edge, or on first line
                    g = charSet.getGlyph(' ');
                    if (cursorX > leftEdge || cursorY == -ascent)
                        cursorX += g.getAdvance();
                    break;
                default:
                    // place a glyph for the char
                    g = charSet.getGlyph(chr);

                    if (wrapWidth > 0f) {
                        // place a newline if the char can't fit on this line
                        // and it wasn't the first char for the line (we always
                        // put 1 char)
                        if (cursorX > leftEdge && cursorX + g.getAdvance() > wrapWidth)
                            newline();
                    }
                    index = placeGlyph(g, coords, index);
                    break;
                }
            }

            return index;
        }

        /*
         * Update coords, at index, to represent the glyph. It updates the
         * cursorX position for the next char, and returns the index for the
         * next character.
         */
        private int placeGlyph(Glyph g, float[] coords, int index) {
            // tex coords for the glyph
            float tcL = g.getTexCoordLeft();
            float tcR = g.getTexCoordRight();
            float tcB = g.getTexCoordBottom();
            float tcT = g.getTexCoordTop();

            // adjusted vertices for the glyph's quad
            float vtL = cursorX + g.getX();
            float vtR = cursorX + g.getX() + g.getWidth();
            float vtB = cursorY + g.getY();
            float vtT = cursorY + g.getY() + g.getHeight();

            // lower left
            coords[index++] = tcL;
            coords[index++] = tcB;
            coords[index++] = vtL;
            coords[index++] = vtB;

            // lower right
            coords[index++] = tcR;
            coords[index++] = tcB;
            coords[index++] = vtR;
            coords[index++] = vtB;

            // upper right
            coords[index++] = tcR;
            coords[index++] = tcT;
            coords[index++] = vtR;
            coords[index++] = vtT;

            // upper left
            coords[index++] = tcL;
            coords[index++] = tcT;
            coords[index++] = vtL;
            coords[index++] = vtT;

            // advance the x position
            cursorX += g.getAdvance();

            return index;
        }

        /*
         * Update cursorX and cursorY so that the next placed characters are on
         * the newline.
         */
        private void newline() {
            maxWidth = Math.max(maxWidth, cursorX);

            cursorX = leftEdge;
            cursorY -= height;
        }

        /*
         * Calculate the width of an un-split word, based off the advances of
         * all Glyphs present in the word.
         */
        private float getWordWidth(StringBuilder word) {
            float width = 0f;
            int l = word.length();
            char c;
            for (int i = 0; i < l; i++) {
                c = word.charAt(i);
                switch (c) {
                case '\n':
                case '\r':
                    // do nothing, since they only change the line position
                    break;
                case '\t':
                    width += TAB_SPACE_COUNT * charSet.getGlyph(' ').getAdvance();
                    break;
                default:
                    // this works for spaces, too
                    width += charSet.getGlyph(word.charAt(i)).getAdvance();
                    break;
                }
            }

            return width;
        }
    }
}
