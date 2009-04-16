package com.ferox.resource.text;

import java.awt.font.LineMetrics;

import com.ferox.math.BoundVolume;
import com.ferox.math.Color;
import com.ferox.resource.Geometry;
import com.ferox.resource.GeometryBoundsCache;
import com.ferox.state.AlphaTest;
import com.ferox.state.Appearance;
import com.ferox.state.BlendMode;
import com.ferox.state.Material;
import com.ferox.state.Texture;
import com.ferox.state.State.PixelTest;

/** Text represents a Geometry that can generate laid-out
 * text based on a CharacterSet.  It assumes that the text
 * is laid-out left to right and Unicode-16 is untested.
 * 
 * Text treats \t as TAB_SPACE_COUNT spaces in a row.  \n and
 * \r are interpreted as well, causing a newline to appear if
 * the layout encounters: \n, \r, or \n\r.  Spaces are not
 * placed at the beginning of a new line, so that text is justified
 * to the left-edge (this is ignored for the 1st line).
 * The layout policy attempts to make a reasonably
 * attractive block of text, suitable for a text area, etc.
 * 
 * Text requires that a specific type of Appearance be used:
 * 1. Has a Texture or MultiTexture with the CharacterSet's Texture2D
 * on the 0th texture unit.
 * 2. It must use a BlendMode or AlphaTest to properly discard
 * the transparent pixels.
 * 3. DepthTest may be useful, but is not required.
 * 4. Text does not generate any normal information, but renderers
 * should set a normal to <0, 0, 1> so that lighting can still work.
 * 
 * There are two phases of updates for Text.  A Text's layout must be
 * updated if its text string changes, or its wrapping width changes.
 * These events mark its layout as dirty.  A call to layoutText() will
 * generate the coordinates for the Text that look up the correctly
 * sized positions in its CharacterSet.
 * 
 * In addition to laying out the text, it must also be updated with
 * a Renderer, since Text is a geometry.  Renderers must layout the
 * text if isLayoutDirty() returns true when updating Text.
 * 
 * After making changes that cause isLayoutDirty() to return true,
 * the Text should be updated with the Renderer.
 * 
 * It is HIGHLY recommended that CharacterSets are shared by multiple
 * instances of Text that need the same font.
 * 
 * @author Michael Ludwig
 *
 */
public class Text implements Geometry {
	/** Number of spaces a tab represents. */
	public static final int TAB_SPACE_COUNT = 4;
	
	private CharacterSet charSet;
	private String text;
	
	private float width;
	private float height;
	
	private float maxTextWidth; // if <= 0, then no wrapping is done
	
	private boolean layoutDirty;
	private float[] coords; // it is interleaved T2F_V3F
	private int numCoords;
	
	private final GeometryBoundsCache boundsCache;
	
	private Object renderData;
	
	/** Create a Text that uses the given CharacterSet
	 * and has its text set to the empty string.
	 * 
	 * Throws a NullPointerException if charSet is null. */
	public Text(CharacterSet charSet) throws NullPointerException {
		this(charSet, "");
	}
	
	/** Create a Text with the given CharacterSet and
	 * initial text value.
	 * 
	 * Throws a NullPointerException if charSet is null. */
	public Text(CharacterSet charSet, String text) throws NullPointerException {
		if (charSet == null)
			throw new NullPointerException("Must provide a non-null CharacterSet");
		
		this.charSet = charSet;
		this.boundsCache = new GeometryBoundsCache(this);
		
		this.setText(text);
		this.setWrapWidth(-1f);
		
		this.layoutText(); // give us some valid values for the other properties
	}
	
	/** Utility method to generate a Appearance suitable for displaying
	 * the Text with the given color for the text.
	 * 
	 * It uses a BlendMode so that anti-aliased text and small text (even
	 * when not with AA) looks nice.  It also adds an AlphaTest to reduce
	 * z-fighting, that discards all pixels with an alpha of 0.
	 * 
	 * If possible, a better solution would be to add a DepthTest to the
	 * returned Appearance that disables depth testing.  This depend on
	 * the use of the Text, though. */
	public Appearance createAppearance(Color textColor) {
		Material m = new Material(textColor);
		Texture chars = new Texture(this.charSet.getCharacterSet());
		
		BlendMode bm = new BlendMode();
		
		// minor hack to prevent z-fighting
		AlphaTest at = new AlphaTest();
		at.setTest(PixelTest.GREATER);
		at.setReferenceValue(0f);
		
		Appearance a = new Appearance(m, chars, bm, at);
		
		return a;
	}
	
	/** Return the wrap width used by this Text.
	 * See setWrapWidth() for more information. */
	public float getWrapWidth() {
		return this.maxTextWidth;
	}
	
	/** Get the current width of this Text.  This
	 * is only valid if isLayoutDirty() returns false. 
	 * The returned value is suitable for drawing a tightly
	 * packed box around the text.
	 * 
	 * It is measured from the left edge of the lines of text,
	 * to the right edge of the longest line of text. */
	public float getTextWidth() {
		return this.width;
	}
	
	/** Get the current height of this Text.  This 
	 * is only valid if isLayoutDirty() returns false.
	 * The returned value can be used to draw a tightly 
	 * packed box around the text.
	 * 
	 * It is measured from the top edge of the text, to
	 * the bottom edge of the last line of text. This includes
	 * the ascent and descent of the font. */
	public float getTextHeight() {
		return this.height;
	}
	
	/** Return the computed coordinates of the Text that
	 * will correctly draw the laid out characters.  These
	 * might be on multiple lines.
	 * 
	 * The coordinates represent interleaved data, where
	 * it goes a 2-valued texture coordinate, then a 3-valued
	 * vertex.  Each batch of four complete 5 values represent
	 * a quad in counter-clockwise order. 
	 * 
	 * The returned array should be deemed read-only, or there
	 * will be undefined results.  The array may be larger
	 * than getVertexCount() * 5, so only the elements from
	 * 0 to getVertexCount() * 5 have valid coordinates. */
	public float[] getInterleavedCoordinates() {
		return this.coords;
	}

	/** Set the wrap width that determines how text is laid out.
	 * A value <= 0 implies that no wrapping is formed.  In this
	 * case text will only be on multiple lines if \n, \r or \n\r.
	 * 
	 * If it's positive, then this value represents the maximum
	 * allowed width of a line of text.  Words that would extend
	 * beyond this will be placed on a newline.  If a word can't fit
	 * on a line, its characters will be wrapped.  Punctuation proceeding
	 * words are treated as part of the word.
	 * 
	 * As far as layout works, the upper left corner of the first
	 * character of text represents the origin.  Subsequent lines
	 * start at progressively negative y-values.  A rectangle with
	 * corners (0,0) and (getTextWidth(), getTextHeight()) would tightly
	 * enclose the body of text.
	 * 
	 * Characters that are not present in the CharacterSet are rendered
	 * with the missing glyph for that set's font.
	 * 
	 * This causes the Text's layout to be flagged as dirty. */
	public void setWrapWidth(float maxWidth) {
		this.maxTextWidth = maxWidth;
		this.layoutDirty = true;
	}
	
	/** Set the text that will be rendered.  If null is given, the
	 * empty string is used instead.
	 * 
	 * See setWrapWidth() for how the text is laid out. 
	 * This causes the Text's layout to be flagged as dirty. */
	public void setText(String text) {
		if (text == null)
			text = "";
		
		this.text = text;
		this.layoutDirty = true;
	}
	
	/** Return the text that will be rendered if isLayoutDirty() 
	 * returns false. */
	public String getText() {
		return this.text;
	}
	
	/** Return the CharacterSet used to display the text. 
	 * This will not be null. */
	public CharacterSet getCharacterSet() {
		return this.charSet;
	}
	
	/** Set the CharacterSet used to render individual characters
	 * in the set String for this Text.
	 * 
	 * Throws a NullPointerException if set is null.
	 * This marks the Text's layout as dirty. */
	public void setCharacterSet(CharacterSet set) throws NullPointerException {
		if (set == null)
			throw new NullPointerException("Cannot use a null CharacterSet");
		
		this.charSet = set;
		this.layoutDirty = true;
	}
	
	/** Return whether or not this Text needs to be re-laid out.
	 * It can be more efficient to make many changes that require re-laying
	 * out, and then perform the layout at the end.
	 * 
	 * This will be true if setCharacterSet(), setWrapWidth() or setText()
	 * have been called after the last call to layoutText(). */
	public boolean isLayoutDirty() {
		return this.layoutDirty;
	}
	
	/** Perform the layout computations for this Text.  This
	 * performs the same operations even if isLayoutDirty() returns
	 * false, so use only when necessary. */
	public void layoutText() {
		LineMetrics lm = this.charSet.getFont().getLineMetrics(this.text, this.charSet.getFontRenderContext());
		TextLayout tl = new TextLayout(this.charSet, lm, this.maxTextWidth);
		this.coords = tl.doLayout(this.text, this.coords);

		this.width = tl.getMaxWidth();
		this.height = tl.getMaxHeight();

		this.numCoords = tl.getNumGlyphs() * 4;
		this.layoutDirty = false;
		
		this.boundsCache.setCacheDirty();
	}

	@Override
	public void getBounds(BoundVolume result) {
		this.boundsCache.getBounds(result);
	}
	
	@Override
	public boolean isAppearanceIgnored() {
		return false;
	}

	@Override
	public float getVertex(int index, int coord) throws IllegalArgumentException {
		if (index < 0 || index >= this.getVertexCount())
			throw new IllegalArgumentException("Illegal vertex index: " + index + " must be in [0, " + this.getVertexCount() + "]");
		if (coord < 0 || coord > 3)
			throw new IllegalArgumentException("Illegal vertex coordinate: " + coord + " must be in [0, 3]");
		
		if (coord == 3)
			return 1f; // we don't have a 4th coordinate
		
		int bIndex = 2 + index * 5;
		return this.coords[bIndex + coord];
	}

	@Override
	public int getVertexCount() {
		return this.numCoords;
	}

	@Override
	public void clearDirtyDescriptor() {
		// do nothing
	}

	@Override
	public Object getDirtyDescriptor() {
		return null;
	}

	@Override
	public Object getResourceData() {
		return this.renderData;
	}

	@Override
	public void setResourceData(Object data) {
		this.renderData = data;
	}
	
	/** Helper class to place the characters into a multi-line block of text. */
	private static class TextLayout {
		// progress of cursor within text
		private float cursorX;
		private float cursorY;
		
		private float leftEdge;
		private float height; // amount to subtract cursorY to get the next line
		private float ascent, descent;
		
		private float maxWidth;
		private float wrapWidth;
		
		private int numGlyphs;
		
		private CharacterSet charSet;
				
		/** If wrapWidth <= 0, then no forced wrapping is performed.
		 * charSet and lm must not be null. */
		public TextLayout(CharacterSet charSet, LineMetrics lm, float wrapWidth) {
			this.leftEdge = 0f;
			
			this.height = lm.getHeight();
			this.ascent = lm.getAscent();
			this.descent = lm.getDescent();
			
			this.wrapWidth = wrapWidth;
			this.charSet = charSet;
		}
		
		/** Return the height of the multi-line text block, after
		 * the last call to doLayout(). */
		public float getMaxHeight() {
			return -this.cursorY + this.descent;
		}
		
		/** Return the max width of the multi-line text block, after
		 * the last call to doLayout(). */
		public float getMaxWidth() {
			return this.maxWidth;
		}
		
		/** Return the number of placed glyphs after the last layout. */
		public int getNumGlyphs() {
			return this.numGlyphs;
		}
		
		/** Layout out the given text, using coords as the storage for the T2F_V3F
		 * coordinates.  Returns the float[] actually holding the coords (this creates
		 * a new one if coords is too small). */
		public float[] doLayout(String text, float[] coords) {
			// reset values for the layout
			this.maxWidth = this.leftEdge;
			
			this.cursorX = this.leftEdge;
			this.cursorY = -this.ascent;
			
			// we're being conservative here, but memory is cheap
			int len = text.length();
			int numPrims = len * 20;
			if (coords == null || numPrims > coords.length)
				coords = new float[numPrims];
			
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
					// we're starting a real word, so add in the prior white-space
					coordIndex = placeWord(currentWord, coords, coordIndex);
					wordIsBlank = false;
				}
				
				currentWord.append(c);
			}
			// add in last word, if needed
			coordIndex = placeWord(currentWord, coords, coordIndex);
			
			this.maxWidth = Math.max(this.maxWidth, this.cursorX);
			this.numGlyphs = coordIndex / 20;
			
			return coords;
		}
		
		/* Possibly move to a newline, and then place each char within the
		 * word.  Return the index for the next character. 
		 * 
		 * Does nothing if word is empty, resets the word afterwards. */
		private int placeWord(StringBuilder word, float[] coords, int index) {
			if (word.length() == 0)
				return index;
			
			if (this.wrapWidth > 0) {
				// check if we need to move the word down
				// char-by-char wrapping happens in placeChars
				float wordWidth = this.getWordWidth(word);
				if (wordWidth < this.wrapWidth 
					&& (wordWidth + this.cursorX) > this.wrapWidth) {
					this.newline();
				} 
			}
			
			index = this.placeChars(word, coords, index);
			word.setLength(0);
			
			return index;
		}
		
		/* Place all chars within c, moving to a newline if they can't fit.
		 * Returns the index for the next character after all of c have
		 * been placed. */
		private int placeChars(StringBuilder c, float[] coords, int index) {
			Glyph g;
			int len = c.length();
			char chr;
			
			for (int i = 0; i < len; i++) {
				chr = c.charAt(i);
				
				switch(chr) {
				case '\n': 
					this.newline(); 
					break;
				case '\r':
					if (i == 0 || c.charAt(i - 1) != '\n')
						this.newline();
					break;
				case '\t':
					// advance TAB_SPACE_COUNT spaces, but don't place anything
					g = this.charSet.getGlyph(' ');
					this.cursorX += TAB_SPACE_COUNT * g.getAdvance();
					break;
				case ' ':
					// just advance the space width, but don't place glyphs
					// only place space if we've moved off of left edge
					g = this.charSet.getGlyph(' ');
					if (this.cursorX > this.leftEdge || this.cursorY == -this.ascent)
						this.cursorX += g.getAdvance();
					break;
				default:
					// place a glyph for the char
					g = this.charSet.getGlyph(chr);
					
					if (this.wrapWidth > 0f) {
						// place a newline if the char can't fit on this line
						// and it wasn't the first char for the line (we always put 1 char)
						if (this.cursorX > this.leftEdge 
							&& this.cursorX + g.getAdvance() > this.wrapWidth) {
							this.newline();
						}
					}
					index = this.placeGlyph(g, coords, index);
					break;
				}
			}
			
			return index;
		}
		
		/* Update coords, at index, to represent the glyph.
		 * It updates the cursorX position for the next char, 
		 * and returns the index for the next character. */
		private int placeGlyph(Glyph g, float[] coords, int index) {
			// tex coords for the glyph
			float tcL = g.getTexCoordLeft();
			float tcR = g.getTexCoordRight();
			float tcB = g.getTexCoordBottom();
			float tcT = g.getTexCoordTop();
			
			// adjusted vertices for the glyph's quad
			float vtL = this.cursorX + g.getX();
			float vtR = this.cursorX + g.getX() + g.getWidth();
			float vtB = this.cursorY + g.getY();
			float vtT = this.cursorY + g.getY() + g.getHeight();
			
			// lower left
			coords[index++] = tcL; coords[index++] = tcB;
			coords[index++] = vtL; coords[index++] = vtB; coords[index++] = 0f;
			
			// lower right
			coords[index++] = tcR; coords[index++] = tcB;
			coords[index++] = vtR; coords[index++] = vtB; coords[index++] = 0f;
			
			// upper right
			coords[index++] = tcR; coords[index++] = tcT;
			coords[index++] = vtR; coords[index++] = vtT; coords[index++] = 0f;
			
			// upper left
			coords[index++] = tcL; coords[index++] = tcT;
			coords[index++] = vtL; coords[index++] = vtT; coords[index++] = 0f;
			
			// advance the x position
			this.cursorX += g.getAdvance();
			
			return index;
		}
		
		/* Update cursorX and cursorY so that the next placed 
		 * characters are one the newline. */
		private void newline() {
			this.maxWidth = Math.max(this.maxWidth, this.cursorX);
			
			this.cursorX = this.leftEdge;
			this.cursorY -= this.height;
		}
		
		/* Calculate the width of an un-split word, based off 
		 * the advances of all Glyphs present in the word. */
		private float getWordWidth(StringBuilder word) {
			float width = 0f;
			int l = word.length();
			char c;
			for (int i = 0; i < l; i++) {
				c = word.charAt(i);
				switch(c) {
				case '\n': case '\r':
					// do nothing, since they only change the line position
					break;
				case '\t':
					width += TAB_SPACE_COUNT * this.charSet.getGlyph(' ').getAdvance();
					break;
				default:
					// this works for spaces, too
					width += this.charSet.getGlyph(word.charAt(i)).getAdvance();
					break;
				}
			}
			
			return width;
		}
	}
}
