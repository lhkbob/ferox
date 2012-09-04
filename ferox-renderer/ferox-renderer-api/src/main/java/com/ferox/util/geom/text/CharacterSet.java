package com.ferox.util.geom.text;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphMetrics;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.ferox.resource.BufferData;
import com.ferox.resource.Mipmap;
import com.ferox.resource.Texture;
import com.ferox.resource.Texture.Target;
import com.ferox.resource.TextureFormat;
import com.ferox.util.geom.text.RectanglePacker.Rectangle;

/**
 * <p>
 * CharacterSet represents a packed character sheet for a set of characters and
 * a Font that they are rendered with. It provides mappings to access the
 * locations of specific characters within its 2D Texture.
 * </p>
 * <p>
 * The generated 2D Texture can be configured to be a power-of-two texture or
 * not. This should be chosen based on the hardware constraints. If supported, a
 * npot texture may provide a more efficient use of space.
 * </p>
 * <p>
 * Each character in a CharacterSet has a Glyph representing its tex-coord
 * location within it, as well as its local coordinates to aid in generating
 * vertex coordinates for a string of coordinates.
 * </p>
 * <p>
 * Like Glyph, this was designed with Roman, left-to-right characters in mind.
 * Support for complex characters is untested.
 * </p>
 * 
 * @author Michael Ludwig
 */
public class CharacterSet {
    /**
     * The default character string used if not specified in a constructor. This
     * consists of the characters from 32 to 127 and the missing glyph
     * character.
     */
    public static final String DEFAULT_CHAR_SET;
    private static final int CHAR_PADDING = 4;

    static {
        StringBuilder b = new StringBuilder();
        for (int i = 32; i < 128; i++)
            b.append((char) i);
        DEFAULT_CHAR_SET = b.toString();
    }

    private Texture characters;
    private final Font font;
    private FontRenderContext context;
    private final boolean antiAlias;
    private final boolean useNpotTexture;

    private final Map<Character, Glyph> metrics;

    /**
     * Create a CharacterSet using the Font "Arial-PLAIN-12" and
     * DEFAULT_CHAR_SET.
     * 
     * @param antiAlias Whether or not the edges of the text should be
     *            antialiased
     * @param useNpotTexture Whether or not an NPOT Texture2D can be used
     */
    public CharacterSet(boolean antiAlias, boolean useNpotTexture) {
        this(null, antiAlias, useNpotTexture);
    }

    /**
     * Create a CharacterSet usint the given font and DEFAULT_CHAR_SET. If the
     * font is null, then Arial-PLAIN-12 is used instead.
     * 
     * @param font The font to use, "Arial-PLAIN-12" is used if null
     * @param antiAlias Whether or not the edges of the text should be
     *            antialiased
     * @param useNpotTexture Whether or not an NPOT Texture2D can be used
     */
    public CharacterSet(Font font, boolean antiAlias, boolean useNpotTexture) {
        this(font, null, antiAlias, useNpotTexture);
    }

    /**
     * Create a CharacterSet using the given font and character set. If the font
     * is null, "Arial-PLAIN-12" is used. If the characterSet is null,
     * DEFAULT_CHAR_SET is used. Characters that are not present in the font,
     * that are whitespace, or are duplicates are not included in the rendered
     * character set texture.
     * 
     * @param font The font to use, "Arial-PLAIN-12" is used if null
     * @param characterSet Specify the characters to have present in the
     *            rendered texture for this CharacterSet
     * @param antiAlias Whether or not the edges of the text should be
     *            antialiased
     * @param useNpotTexture Whether or not an NPOT Texture2D can be used
     */
    public CharacterSet(Font font, String characterSet, boolean antiAlias, boolean useNpotTexture) {
        if (font == null)
            font = Font.decode("Arial-BOLD-14");
        if (characterSet == null)
            characterSet = DEFAULT_CHAR_SET;

        this.font = font;
        this.antiAlias = antiAlias;
        this.useNpotTexture = useNpotTexture;
        metrics = new HashMap<Character, Glyph>();
        buildCharacterSet(characterSet);
    }

    /**
     * <p>
     * Return the Glyph for the given character, associated with this
     * CharacterSet. Returns the glyph for the "unknown" character if c isn't
     * present
     * </p>
     * <p>
     * All CharacterSet's have the missing glyph character, and the space. Any
     * other whitespace besides ' ' are never present in the CharacterSet.
     * </p>
     * 
     * @param c The char in question
     * @return The Glyph that should be used to render c
     */
    public Glyph getGlyph(char c) {
        Glyph g = metrics.get(Character.valueOf(c));
        if (g == null) {
            c = (char) font.getMissingGlyphCode();
            g = metrics.get(Character.valueOf(c));
        }

        return g;
    }

    /**
     * <p>
     * Return the 2D Texture that contains the character sheet for all characters
     * of this CharacterSet. Use the character metrics returned by getGlyph() to
     * access the image data.
     * </p>
     * <p>
     * The texture will have a transparent background, with the characters
     * rendered in white.
     * </p>
     * 
     * @return The Texture holding all rendered characters
     */
    public Texture getTexture() {
        return characters;
    }

    /**
     * @return The Font that the characters of this CharacterSet are rendered
     *         with.
     */
    public Font getFont() {
        return font;
    }

    /**
     * Return true if the character sheet was rendered with anti-aliased text.
     * If true, a BlendMode should be used to get the correct display, otherwise
     * an AlphaTest should be acceptable.
     * 
     * @return Anti-aliasing of this CharacterSet
     */
    public boolean isAntiAliased() {
        return antiAlias;
    }

    /**
     * @return The FontRenderContext used to layout this CharacterSet
     */
    public FontRenderContext getFontRenderContext() {
        return context;
    }

    /*
     * Compute metrics[] and metricOffset, must be called after font is assigned
     * and generate the Texture2D that stores the packed glyphs.
     */
    private void buildCharacterSet(String characterSet) {
        char[] characters = getCharArray(characterSet);

        BufferedImage charSet = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = charSet.createGraphics();
        context = g2d.getFontRenderContext();

        // pack all the glyphs
        RectanglePacker<GlyphMetrics> rp = new RectanglePacker<GlyphMetrics>(64, 64);
        GlyphVector v = font.layoutGlyphVector(context, characters, 0, characters.length,
                                               Font.LAYOUT_LEFT_TO_RIGHT);

        GlyphMetrics g;
        Rectangle2D bounds;
        GlyphMetrics[] glyphs = new GlyphMetrics[characters.length];
        Rectangle[] glyphRectangles = new Rectangle[characters.length];
        for (int i = 0; i < characters.length; i++) {
            g = v.getGlyphMetrics(i);
            bounds = g.getBounds2D();

            glyphs[i] = g;
            glyphRectangles[i] = rp.insert(g, (int) bounds.getWidth() + CHAR_PADDING * 2, 
                                              (int) bounds.getHeight() + CHAR_PADDING * 2);
        }
        g2d.dispose(); // dispose of dummy image

        int width = (!useNpotTexture ? ceilPot(rp.getWidth()) : rp.getWidth());
        int height = (!useNpotTexture ? ceilPot(rp.getHeight()) : rp.getHeight());

        // compute a Glyph for each character and render it into the image
        charSet = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        g2d = charSet.createGraphics();
        // clear the image
        g2d.setColor(new Color(0f, 0f, 0f, 0f));
        g2d.fillRect(0, 0, width, height);

        // prepare the text to be rendered as white,
        g2d.setColor(Color.WHITE);
        g2d.setFont(font);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, (antiAlias ? RenderingHints.VALUE_TEXT_ANTIALIAS_ON 
                                                                              : RenderingHints.VALUE_TEXT_ANTIALIAS_OFF));

        // and flipped, so glyph dimensions make sense in openGL coord system
        g2d.scale(1, -1);
        g2d.translate(0, -height);

        Rectangle r;
        Rectangle2D glyphBounds;
        Glyph glyph;
        // create an actual Glyph and render the char into the buffered image
        for (int i = 0; i < characters.length; i++) {
            g = glyphs[i];
            r = glyphRectangles[i];
            glyphBounds = g.getBounds2D();

            glyph = new Glyph(g.getAdvance(), // advance
                              (float) r.getX() / width, (float) (r.getX() + r.getWidth()) / width, // left and right tex coords
                              (float) (height - r.getY() - r.getHeight()) / height, (float) (height - r.getY()) / height, // bottom and top tex coords
                              (float) glyphBounds.getX(), (float) -(glyphBounds.getHeight() + glyphBounds.getY()), // local x and y positions
                              (float) glyphBounds.getWidth() + CHAR_PADDING * 2, (float) glyphBounds.getHeight() + CHAR_PADDING * 2); // width-height
            metrics.put(Character.valueOf(characters[i]), glyph);

            g2d.drawChars(characters, i, 1, 
                          r.getX() - (int) glyphBounds.getX() + CHAR_PADDING, 
                          r.getY() - (int) glyphBounds.getY() + CHAR_PADDING);
        }
        g2d.dispose();

        // create the texture
        int[] data = ((DataBufferInt) charSet.getRaster().getDataBuffer()).getData();
        this.characters = new Texture(Target.T_2D, new Mipmap(new BufferData(data), width, height, 1, TextureFormat.ARGB_8888));
    }

    /*
     * Turn the string into an array of characters, and add the missing glyph
     * code. Only includes characters the font can render.
     */
    private char[] getCharArray(String characterSet) {
        Set<Character> set = new HashSet<Character>();
        for (char c : characterSet.toCharArray())
            if (font.canDisplay(c) && !Character.isWhitespace(c))
                set.add(Character.valueOf(c));

        // always add these
        set.add(Character.valueOf((char) font.getMissingGlyphCode()));
        set.add(Character.valueOf(' '));

        char[] characters = new char[set.size()];
        int i = 0;
        for (Character c : set)
            characters[i++] = c.charValue();

        return characters;
    }

    // Return smallest POT >= num
    private static int ceilPot(int num) {
        int pot = 1;
        while (pot < num)
            pot = pot << 1;
        return pot;
    }
}
