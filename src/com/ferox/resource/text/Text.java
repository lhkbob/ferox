package com.ferox.resource.text;

import java.awt.font.LineMetrics;

import com.ferox.math.AxisAlignedBox;
import com.ferox.math.BoundSphere;
import com.ferox.math.BoundVolume;
import com.ferox.math.Color;
import com.ferox.resource.Geometry;
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
	private CharacterSet charSet;
	private String text;
	
	private float width;
	private float height;
	
	private float maxTextWidth; // if <= 0, then no wrapping is done
	
	private boolean layoutDirty;
	private float[] coords; // it is interleaved T2F_V3F
	private int numCoords;
	
	private AxisAlignedBox cacheBox;
	private BoundSphere cacheSphere;
	
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
		this.setText(text);
		this.setWrapWidth(-1f);
		
		this.layoutText(); // give us some valid values for the other properties
	}
	
	/** Utility method to generate a Appearance suitable for displaying
	 * the Text with the given color for the text.  If the CharacterSet
	 * was anti-aliased, it uses a BlendMode, otherwise it uses an AlphaTest
	 * to present only the correct pixels.
	 * 
	 * When the BlendMode is used, it also sets an AlphaTest to discard all
	 * completely transparent pixels.  This helps to prevent awkward z-fighting
	 * between letters in the Text for certain fonts where the character
	 * boundaries overlap.  
	 * 
	 * If possible, a better solution would be to add a DepthTest to the
	 * returned Appearance that disables depth testing.  This depend on
	 * the use of the Text, though. */
	public Appearance createAppearance(Color textColor) {
		Material m = new Material(textColor);
		Texture chars = new Texture(this.charSet.getCharacterSet());
		
		AlphaTest at = new AlphaTest();
		Appearance a = new Appearance(m, chars, at);
		
		if (this.charSet.isAntiAliased()) {
			// must set it this way, so inter-poly 
			// depth testing doesn't get goofed up
			at.setTest(PixelTest.GREATER);
			at.setReferenceValue(0f);
			
			BlendMode bm = new BlendMode();
			a.addState(bm);
		}
		
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
	 * a quad in counter-clockwise order. */
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

		this.numCoords = this.text.length() * 4;
		this.layoutDirty = false;
		
		// reset the bound caches
		this.cacheBox = null;
		this.cacheSphere = null;
	}

	@Override
	public void getBounds(BoundVolume result) {
		if (result != null) {
			if (result instanceof AxisAlignedBox) {
				if (this.cacheBox == null) {
					this.cacheBox = new AxisAlignedBox();
					this.cacheBox.enclose(this);
				}
				this.cacheBox.clone(result);
			} else if (result instanceof BoundSphere) {
				if (this.cacheSphere == null) {
					this.cacheSphere = new BoundSphere();
					this.cacheSphere.enclose(this);
				}
				this.cacheSphere.clone(result);
			} else
				result.enclose(this);
		}
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
		
		/** Layout out the given text, using coords as the storage for the T2F_V3F
		 * coordinates.  Returns the float[] actually holding the coords (this creates
		 * a new one if coords is too small). */
		public float[] doLayout(String text, float[] coords) {			
			int numPrims = text.length() * 20;
			if (coords == null || numPrims > coords.length)
				coords = new float[numPrims];
			
			this.layout(text, coords);
			return coords;
		}
		
		/* Reset the position, and layout each word. */
		private void layout(String text, float[] coords) {
			this.maxWidth = this.leftEdge; // reset this, too
			
			this.cursorX = this.leftEdge;
			this.cursorY = -this.ascent;
			
			String[] words = text.split("\\b");
			int charIndex = 0;
			for (int i = 1; i < words.length; i++) {
				charIndex = this.placeWord(words[i], coords, charIndex);
			}
			// must update one more, for the last line that never had newline() called.
			this.maxWidth = Math.max(this.maxWidth, this.cursorX);
		}
		
		/* Possibly move to a newline, and then place each char within the
		 * word.  Return the index for the next character. */
		private int placeWord(String word, float[] coords, int index) {
			if (this.wrapWidth > 0) {
				float wordWidth = this.getWordWidth(word);
				if (wordWidth < this.wrapWidth 
					&& (wordWidth + this.cursorX) > this.wrapWidth) {
					this.newline();
				} 
			}
				
			return this.placeChars(word.toCharArray(), coords, index);
		}
		
		/* Place all chars within c, moving to a newline if they can't fit.
		 * Returns the index for the next character after all of c have
		 * been placed. */
		private int placeChars(char[] c, float[] coords, int index) {
			Glyph g;
			for (int i = 0; i < c.length; i++) {
				// check for newline and carriage return chars
				if (c[i] == '\n') {
					this.newline();
				} else if (c[i] == '\r') {
					// only move to a newline if the previous char wasn't '\n'
					if (i == 0 || c[i - 1] != '\n')
						this.newline();
				} else {
					// place a glyph for the char
					g = this.charSet.getGlyph(c[i]);
					
					if (this.wrapWidth > 0f) {
						// place a newline if the char can't fit on this line
						// and it wasn't the first char for the line (we always put 1 char)
						if (this.cursorX > this.leftEdge 
							&& this.cursorX + g.getAdvance() > this.wrapWidth) {
							this.newline();
						}
					}
					index = this.placeGlyph(g, coords, index);
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
		private float getWordWidth(String word) {
			float width = 0f;
			int l = word.length();
			for (int i = 0; i < l; i++) {
				width += this.charSet.getGlyph(word.charAt(i)).getAdvance();
			}
			
			return width;
		}
	}
}
