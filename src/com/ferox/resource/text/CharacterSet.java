package com.ferox.resource.text;

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
import com.ferox.resource.Texture2D;
import com.ferox.resource.TextureFormat;
import com.ferox.resource.BufferData.DataType;
import com.ferox.resource.TextureImage.Filter;
import com.ferox.resource.text.RectanglePacker.Rectangle;

/** CharacterSet represents a packed character sheet
 * for a set of characters and a Font that they are
 * rendered with.  It provides mappings to access the
 * locations of specific characters within its Texture2D.
 * 
 * The generated Texture2D can be configured to be a power-of-two
 * texture or not.  This should be chosen based on the hardware
 * constraints.  If supported, a npot texture may provide a more
 * efficient use of space.
 * 
 * Each character in a CharacterSet has a Glyph representing
 * its tex-coord location within it, as well as its local
 * coordinates to aid in generating vertex coordinates for
 * a string of coordinates.
 * 
 * Like Glyph, this was designed with English, left-to-right
 * characters in mind.  Support for complex characters is untested.
 * 
 * @author Michael Ludwig
 *
 */
public class CharacterSet {
	public static final String DEFAULT_CHAR_SET;
	public static final int CHAR_PADDING = 4;
	
	static {
		StringBuilder b = new StringBuilder();
		for (int i = 32; i < 128; i++)
			b.append((char) i);
		DEFAULT_CHAR_SET = b.toString();
	}
	
	private Texture2D characters;
	private Font font;
	private FontRenderContext context;
	private boolean antiAlias;
	private boolean useNpotTexture;
	
	private Map<Character, Glyph> metrics;
	
	/** Create a CharacterSet using the Font "Arial-PLAIN-12"
	 * and DEFAULT_CHAR_SET. */
	public CharacterSet(boolean antiAlias, boolean useNpotTexture) {
		this(null, antiAlias, useNpotTexture);
	}
	
	/** Create a CharacterSet usint the given font and 
	 * DEFAULT_CHAR_SET.  If the font is null, then
	 * Arial-PLAIN-12 is used instead. */
	public CharacterSet(Font font, boolean antiAlias, boolean useNpotTexture) {
		this(font, null, antiAlias, useNpotTexture);
	}
	
	/** Create a CharacterSet using the given font and
	 * character set.  If the font is null, "Arial-PLAIN-12"
	 * is used.  If the characterSet is null, DEFAULT_CHAR_SET
	 * is used.  All duplicate characters are removed from the
	 * character set.  */
	public CharacterSet(Font font, String characterSet, boolean antiAlias, boolean useNpotTexture) {
		if (font == null)
			font = Font.decode("Arial-PLAIN-12");
		if (characterSet == null)
			characterSet = DEFAULT_CHAR_SET;
		
		this.font = font;
		this.antiAlias = antiAlias;
		this.useNpotTexture = useNpotTexture;
		this.metrics = new HashMap<Character, Glyph>();
		this.buildCharacterSet(characterSet);
	}
	
	/** Return the Glyph for the given character,
	 * associated with this CharacterSet.  Returns the 
	 * glyph for the "unknown" character if c isn't present 
	 * 
	 * All CharacterSet's have the missing glyph character,
	 * and the space.  Any other whitespace besides ' ' are
	 * never present in the CharacterSet. */
	public Glyph getGlyph(char c) {
		Glyph g = this.metrics.get(Character.valueOf(c));
		if (g == null) {
			c = (char) this.font.getMissingGlyphCode();
			g = this.metrics.get(Character.valueOf(c));
		}
		
		return g;
	}
	
	/** Return the Texture2D that contains the character sheet
	 * for all characters of this CharacterSet.  Use the
	 * character metrics returned by getMetric() to access the
	 * image data.
	 * 
	 * The texture will have a transparent background, with
	 * the characters rendered in white. */
	public Texture2D getCharacterSet() {
		return this.characters;
	}
	
	/** Return the Font that the characters of this
	 * CharacterSet are rendered with. */
	public Font getFont() {
		return this.font;
	}
	
	/** Return true if the character sheet was rendered with
	 * anti-aliased text.  If true, a BlendMode should be used
	 * to get the correct display, otherwise an AlphaTest should
	 * be acceptable. */
	public boolean isAntiAliased() {
		return this.antiAlias;
	}
	
	/** Return the FontRenderContext used to layout this CharacterSheet. */
	public FontRenderContext getFontRenderContext() {
		return this.context;
	}
	
	/* Compute metrics[] and metricOffset, must be called after font is assigned
	 * and generate the Texture2D that stores the packed glyphs. */
	private void buildCharacterSet(String characterSet) {
		char[] characters = this.getCharArray(characterSet);
		
		BufferedImage charSet = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = charSet.createGraphics();
		this.context = g2d.getFontRenderContext();
		
		// pack all the glyphs
		RectanglePacker<GlyphMetrics> rp = new RectanglePacker<GlyphMetrics>(64, 64);
		GlyphVector v = this.font.layoutGlyphVector(this.context, characters, 0, characters.length, Font.LAYOUT_LEFT_TO_RIGHT);
		GlyphMetrics g;
		
		GlyphMetrics[] glyphs = new GlyphMetrics[characters.length];
		Rectangle[] glyphRectangles = new Rectangle[characters.length];
		for (int i = 0; i < characters.length; i++) {
			g = v.getGlyphMetrics(i);
			glyphs[i] = g;
			glyphRectangles[i] = rp.insert(g, (int) g.getBounds2D().getWidth() + CHAR_PADDING * 2, (int) g.getBounds2D().getHeight() + CHAR_PADDING * 2);
		}
		g2d.dispose(); // dispose of dummy image
		
		int width = (!this.useNpotTexture ? ceilPot(rp.getWidth()) : rp.getWidth());
		int height = (!this.useNpotTexture ? ceilPot(rp.getHeight()) : rp.getHeight());
		
		// compute a Glyph for each character and render it into the image
		charSet = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		g2d = charSet.createGraphics();
		// clear the image
		g2d.setColor(new Color(0f, 0f, 0f, 0f));
		g2d.fillRect(0, 0, width, height);
		
		// prepare the text to be rendered as white,
		g2d.setColor(Color.WHITE);
		g2d.setFont(this.font);
		g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, (this.antiAlias ? RenderingHints.VALUE_TEXT_ANTIALIAS_ON : 
																					 RenderingHints.VALUE_TEXT_ANTIALIAS_OFF));

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
							  (float) r.getX() / width, (float) (r.getX() + r.getWidth()) / width, // left-right
							  (float) (height - r.getY()) / height, (float) (height - r.getY() - r.getHeight()) / height, // top-bottom
							  (float) glyphBounds.getX(), (float) -(glyphBounds.getHeight() + glyphBounds.getY()), // x-y
							  (float) glyphBounds.getWidth() + CHAR_PADDING * 2, (float) glyphBounds.getHeight() + CHAR_PADDING * 2); // width-height
			this.metrics.put(Character.valueOf(characters[i]), glyph);
			
			g2d.drawChars(characters, i, 1, r.getX() - (int) glyphBounds.getX() + CHAR_PADDING, r.getY() - (int) glyphBounds.getY() + CHAR_PADDING);
		}
		g2d.dispose();
		
		// create the texture
		int[] data = ((DataBufferInt) charSet.getRaster().getDataBuffer()).getData();
		BufferData[] imageData = {new BufferData(data, true)};
		this.characters = new Texture2D(imageData, width, height, TextureFormat.ARGB_8888, DataType.UNSIGNED_INT, Filter.LINEAR);
	}
	
	/* Turn the string into an array of characters, and
	 * add the missing glyph code. Only includes characters the font can render. */
	private char[] getCharArray(String characterSet) {
		Set<Character> set = new HashSet<Character>();
		for (char c: characterSet.toCharArray()) {
			if (this.font.canDisplay(c) && !Character.isWhitespace(c))
				set.add(Character.valueOf(c));
		}
		
		 // always add these
		set.add(Character.valueOf((char) this.font.getMissingGlyphCode()));
		set.add(Character.valueOf(' '));

		char[] characters = new char[set.size()];
		int i = 0;
		for (Character c: set) {
			characters[i++] = c.charValue();
		}
		
		return characters;
	}
	
	// Return smallest POT >= num
	private static int ceilPot(int num) {
		int pot = 1;
		while(pot < num)
			pot = pot << 1;
		return pot;
	}
}
