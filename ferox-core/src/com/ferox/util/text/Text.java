package com.ferox.util.text;

import java.awt.font.LineMetrics;

import com.ferox.resource.IndexedArrayGeometry;

/**
 * <p>
 * Text represents a Geometry that can generate laid-out text based on a
 * CharacterSet. It assumes that the text is laid-out left to right and
 * Unicode-16 is untested.
 * </p>
 * <p>
 * Text treats \t as TAB_SPACE_COUNT spaces in a row. \n and \r are interpreted
 * as well, causing a newline to appear if the layout encounters: \n, \r, or
 * \n\r. Spaces are not placed at the beginning of a new line, so that text is
 * justified to the left-edge (this is ignored for the 1st line). The layout
 * policy attempts to make a reasonably attractive block of text, suitable for a
 * text area, etc.
 * </p>
 * <p>
 * Text requires that a specific type of Appearance be used:
 * <ol>
 * <li>Has a Texture or MultiTexture with the CharacterSet's Texture2D on the
 * 0th texture unit.</li>
 * <li>It must use a BlendMode or AlphaTest to properly discard the transparent
 * pixels.</li>
 * <li>DepthTest may be useful, but is not required.</li>
 * <li>Text does not generate any normal information, but renderers should set a
 * normal to <0, 0, 1> so that lighting can still work.</li>
 * </ol>
 * </p>
 * <p>
 * There are two phases of updates for Text. A Text's layout must be updated if
 * its text string changes, or its wrapping width changes. These events mark its
 * layout as dirty. A call to layoutText() will generate the coordinates for the
 * Text that look up the correctly sized positions in its CharacterSet.
 * </p>
 * <p>
 * If the Text's compile type is not NONE, it must also be updated with a
 * Framework, since Text is a geometry. Renderers must layout the text if
 * isLayoutDirty() returns true when updating Text. After making changes that
 * cause isLayoutDirty() to return true, the Text should be updated with the
 * Framework.
 * </p>
 * <p>
 * It is HIGHLY recommended that CharacterSets are shared by multiple instances
 * of Text that need the same font.
 * </p>
 * 
 * @author Michael Ludwig
 */
public class Text extends IndexedArrayGeometry {
	/** Number of spaces a tab character represents. */
	public static final int TAB_SPACE_COUNT = 4;

	private CharacterSet charSet;
	private String text;

	private float width;
	private float height;

	private float maxTextWidth; // if <= 0, then no wrapping is done

	private boolean layoutDirty;

	/**
	 * Create a Text that uses the given CharacterSet and has its text set to
	 * the empty string.
	 * 
	 * @param charSet The CharacterSet storing font information
	 * @param type The compile type to use for Text
	 * @throws NullPointerException if charSet is null
	 */
	public Text(CharacterSet charSet, CompileType type) {
		this(charSet, "", type);
	}

	/**
	 * Create a Text with the given CharacterSet and initial text value.
	 * 
	 * @param charSet The CharacterSet storing font information
	 * @param text The initial text content
	 * @param type The compile type to use for Text
	 * @throws NullPointerException if charSet is null
	 */
	public Text(CharacterSet charSet, String text, CompileType type) {
		super(type);

		setCharacterSet(charSet);
		setText(text);
		setWrapWidth(-1f);

		layoutText(); // give us some valid values for the other properties
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
	 * Get the current width of this Text. This is only valid if isLayoutDirty()
	 * returns false. The returned value is suitable for drawing a tightly
	 * packed box around the text.
	 * </p>
	 * <p>
	 * It is measured from the left edge of the lines of text, to the right edge
	 * of the longest line of text.
	 * </p>
	 * 
	 * @return The width of the text
	 */
	public float getTextWidth() {
		return width;
	}

	/**
	 * <p>
	 * Get the current height of this Text. This is only valid if
	 * isLayoutDirty() returns false. The returned value can be used to draw a
	 * tightly packed box around the text.
	 * </p>
	 * <p>
	 * It is measured from the top edge of the text, to the bottom edge of the
	 * last line of text. This includes the ascent and descent of the font.
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
	 * multiple lines if \n, \r or \n\r are encountered.
	 * </p>
	 * <p>
	 * If it's positive, then this value represents the maximum allowed width of
	 * a line of text. Words that would extend beyond this will be placed on a
	 * newline. If a word can't fit on a line, its characters will be wrapped.
	 * Punctuation proceeding words are treated as part of the word.
	 * </p>
	 * <p>
	 * As far as layout works, the upper left corner of the first character of
	 * text represents the origin. Subsequent lines start at progressively
	 * negative y-values. A rectangle with corners (0,0) and (getTextWidth(),
	 * getTextHeight()) would tightly enclose the body of text.
	 * </p>
	 * <p>
	 * Characters that are not present in the CharacterSet are rendered with the
	 * missing glyph for that set's font.
	 * </p>
	 * <p>
	 * This causes the Text's layout to be flagged as dirty.
	 * </p>
	 * 
	 * @param maxWidth The maximum width of the laid-out text
	 */
	public void setWrapWidth(float maxWidth) {
		maxTextWidth = maxWidth;
		layoutDirty = true;
	}

	/**
	 * <p>
	 * Set the text that will be rendered. If null is given, the empty string is
	 * used instead.This causes the Text's layout to be flagged as dirty.
	 * </p>
	 * 
	 * @see #setWrapWidth(float)
	 * @param text The new String text to use
	 */
	public void setText(String text) {
		if (text == null)
			text = "";

		this.text = text;
		layoutDirty = true;
	}

	/**
	 * Return the text that should be rendered by this geometry, assuming that
	 * it's layout is not dirty, and that it's up-to-date with the Framework
	 * (depending on the compile type, of course).
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
	 * Set the CharacterSet used to render individual characters in the set
	 * String for this Text.This marks the Text's layout as dirty.
	 * </p>
	 * 
	 * @param set The new CharacterSet for rendering characters
	 * @throws NullPointerException if set is null
	 */
	public void setCharacterSet(CharacterSet set) {
		if (set == null)
			throw new NullPointerException("Cannot use a null CharacterSet");

		charSet = set;
		layoutDirty = true;
	}

	/**
	 * <p>
	 * Return whether or not this Text needs to be re-laid out. It can be more
	 * efficient to make many changes that require re-laying out, and then
	 * perform the layout at the end.
	 * </p>
	 * <p>
	 * This will be true if setCharacterSet(), setWrapWidth() or setText() have
	 * been called after the last call to layoutText().
	 * </p>
	 * 
	 * @return True if the text layout is dirty
	 */
	public boolean isLayoutDirty() {
		return layoutDirty;
	}

	/**
	 * <p>
	 * Perform the layout computations for this Text. This performs the same
	 * operations even if isLayoutDirty() returns false, so use only when
	 * necessary.
	 * </p>
	 * <p>
	 * If the compile type of this Text is not NONE, then it must be updated by
	 * a Framework, too, before the newly laid out text is visible in rendering.
	 * </p>
	 */
	public void layoutText() {
		LineMetrics lm = charSet.getFont().getLineMetrics(text, charSet.getFontRenderContext());
		TextLayout tl = new TextLayout(charSet, lm, maxTextWidth);
		float[] it2v3 = tl.doLayout(text);

		// extract individual arrays from interleaved array
		float[] v = new float[it2v3.length / 5 * 3];
		float[] t = new float[it2v3.length / 5 * 2];
		int[] i = new int[it2v3.length / 5];

		for (int j = 0; j < i.length; j++) {
			// tex
			t[j * 2 + 0] = it2v3[j * 5 + 0];
			t[j * 2 + 1] = it2v3[j * 5 + 1];

			// coord
			v[j * 3 + 0] = it2v3[j * 5 + 2];
			v[j * 3 + 1] = it2v3[j * 5 + 3];
			v[j * 3 + 2] = it2v3[j * 5 + 4];

			// index
			i[j] = j;
		}

		setVertices(v);
		setTextureCoordinates(0, new VectorBuffer(t, 2));
		setIndices(i, PolygonType.QUADS);

		width = tl.getMaxWidth();
		height = tl.getMaxHeight();

		layoutDirty = false;
	}

	/** Helper class to place the characters into a multi-line block of text. */
	private static class TextLayout {
		// progress of cursor within text
		private float cursorX;
		private float cursorY;

		private final float leftEdge;
		private final float height; // amount to subtract cursorY to get the
		// next line
		private final float ascent, descent;

		private float maxWidth;
		private final float wrapWidth;

		private int numGlyphs;

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

		/* Return the number of placed glyphs after the last layout. */
		public int getNumGlyphs() {
			return numGlyphs;
		}

		/*
		 * Layout out the given text, returning an array of coords with a usage
		 * pattern of T2F_V3F
		 */
		public float[] doLayout(String text) {
			// reset values for the layout
			maxWidth = leftEdge;

			cursorX = leftEdge;
			cursorY = -ascent;

			// we're being conservative here, but memory is cheap
			int len = text.length();
			int numPrims = len * 20;
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
			numGlyphs = coordIndex / 20;

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
					// only place space if we've moved off of left edge
					g = charSet.getGlyph(' ');
					if (cursorX > leftEdge || cursorY == -ascent)
						cursorX += g.getAdvance();
					break;
				default:
					// place a glyph for the char
					g = charSet.getGlyph(chr);

					if (wrapWidth > 0f)
						// place a newline if the char can't fit on this line
						// and it wasn't the first char for the line (we always
						// put 1 char)
						if (cursorX > leftEdge && cursorX + g.getAdvance() > wrapWidth)
							newline();
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
			coords[index++] = 0f;

			// lower right
			coords[index++] = tcR;
			coords[index++] = tcB;
			coords[index++] = vtR;
			coords[index++] = vtB;
			coords[index++] = 0f;

			// upper right
			coords[index++] = tcR;
			coords[index++] = tcT;
			coords[index++] = vtR;
			coords[index++] = vtT;
			coords[index++] = 0f;

			// upper left
			coords[index++] = tcL;
			coords[index++] = tcT;
			coords[index++] = vtL;
			coords[index++] = vtT;
			coords[index++] = 0f;

			// advance the x position
			cursorX += g.getAdvance();

			return index;
		}

		/*
		 * Update cursorX and cursorY so that the next placed characters are one
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
