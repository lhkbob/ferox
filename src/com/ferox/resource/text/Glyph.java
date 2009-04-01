package com.ferox.resource.text;

/** A Glyph represents the bounding-box and positioning information
 * for a single character within a font.  It does not distinguish between
 * AWT's various types of GlyphMetrics for multiple character sequences.
 * 
 * It assumes a fairly standard English character set that proceeds from
 * left-to-right.  Each glyph has a local origin that represents the
 * "lower-left" corner of the character.  This is not the absolute location
 * of the character.  Characters may be positioned above or below this baseline,
 * or to the left or right of it.  Above and to the right represent positive
 * coordinate values.
 * 
 * @author Michael Ludwig
 *
 */
public class Glyph {
	private float advance;
	private float tcL, tcR, tcT, tcB;
	private float x, y;
	private float width, height;
	
	/** Create a new Glyph with the given values. */
	public Glyph(float advance, float tcL, float tcR, float tcT, float tcB, float x, float y, float width, float height) {
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
	
	/** Return the horizontal distance to advance
	 * for the next character. The advance is measured
	 * from this character's origin (not getX()). */
	public float getAdvance() { 
		return this.advance; 
	}
	
	/** Return the texture coord that represents the
	 * left edge within the associated char-set's texture. */
	public float getTexCoordLeft() { 
		return this.tcL; 
	}
	
	/** Return the texture coord that represents the
	 * right edge within the associated char-set's texture. */
	public float getTexCoordRight() { 
		return this.tcR;
	}
	
	/** Return the texture coord that represents the
	 * top edge within the associated char-set's texture. */
	public float getTexCoordTop() { 
		return this.tcT; 
	}
	
	/** Return the texture coord that represents the
	 * bottom edge within the associated char-set's texture. */
	public float getTexCoordBottom() { 
		return this.tcB; 
	}
	
	/** Return the appropriate x distance relative to
	 * this metric's local origin of the character's 
	 * quad's left edge.
	 * 
	 * Negative implies to the left of the origin. */
	public float getX() { 
		return this.x; 
	}
	
	/** Return the appropriate y distance relative to
	 * this metric's local origin of the character's
	 * bottom edge.
	 * 
	 * A negative value implies the character is position
	 * below the baseline of the font. */
	public float getY() { 
		return this.y; 
	}
	
	/** Return the width of the character's quad,
	 * this will be positive. */
	public float getWidth() { 
		return this.width; 
	}
	
	/** Return the height of the character's quad,
	 * this will be positive. */
	public float getHeight() {
		return this.height;
	}
}
