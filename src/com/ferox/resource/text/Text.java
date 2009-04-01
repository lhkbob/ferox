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
	
	public Text(CharacterSet charSet) throws NullPointerException {
		this(charSet, "");
	}
	
	public Text(CharacterSet charSet, String text) throws NullPointerException {
		if (charSet == null)
			throw new NullPointerException("Must provide a non-null CharacterSet");
		
		this.charSet = charSet;
		this.setText(text);
		this.setWrapWidth(-1f);
		
		this.layoutText();
	}
	
	public Appearance createAppearance(Color textColor) {
		Material m = new Material(textColor);
		Texture chars = new Texture(this.charSet.getCharacterSet());
		
		AlphaTest at = new AlphaTest();
		Appearance a = new Appearance(m, chars, at);
		
		if (this.charSet.isAntiAliased()) {
			BlendMode bm = new BlendMode();
			
			// must set it this way, so inter-poly 
			// depth testing doesn't get goofed up
			at.setTest(PixelTest.GREATER);
			at.setReferenceValue(0f);
			
			a.addState(bm);
		}
		
		return a;
	}
	
	public float getWrapWidth() {
		return this.maxTextWidth;
	}
	
	public float getTextWidth() {
		return this.width;
	}
	
	public float getTextHeight() {
		return this.height;
	}
	
	public float[] getInterleavedCoordinates() {
		return this.coords;
	}
	
	public void setWrapWidth(float maxWidth) {
		this.maxTextWidth = maxWidth;
		this.layoutDirty = true;
	}
	
	public void setText(String text) {
		this.text = text;
		this.layoutDirty = true;
	}
	
	public String getText() {
		return this.text;
	}
	
	public CharacterSet getCharacterSet() {
		return this.charSet;
	}
	
	public boolean isLayoutDirty() {
		return this.layoutDirty;
	}
	
	public void layoutText() {
		if (this.layoutDirty) {
			LineMetrics lm = this.charSet.getFont().getLineMetrics(this.text, this.charSet.getFontRenderContext());
			TextLayout tl = new TextLayout(this.charSet, lm, this.maxTextWidth);
			this.coords = tl.doLayout(this.text, this.coords);
						
			this.width = tl.getMaxWidth();
			this.height = tl.getMaxHeight();
			
			this.numCoords = this.text.length() * 4;
			this.layoutDirty = false;
		}
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
