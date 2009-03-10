package com.ferox.state;


/** Designates that a class represents some abstract notion of "state".
 * This would be anything that alters the way in which a shape is rendered to 
 * a buffer.  Examples include material color, draw mode, and textures.
 * 
 * To prevent low-level state corruption when rendering, each State declares
 * a Role that it embodies.  An Appearance can only have one state of each role,
 * unless more are added dynamically by InfluenceAtoms (but then the Renderer
 * can discard states that are unsupported).
 * 
 * @author Michael Ludwig
 *
 */
public interface State{
	/** The allowed roles that a State implementation is able to
	 * declare itself as.  A Renderer does not necessarily support every
	 * state that has the same Role.  If you create another material-like class
	 * with the MATERIAL role, you'd also have to add support to the Renderer. */
	public static enum Role {
		ALPHA_TEST, 		/** State controls discarding of pixels based on alpha color. */
		DEPTH_TEST, 		/** State controls discarding of pixels based on depth value and update of depth buffer. */
		STENCIL_TEST, 		/** State controls updates and tests against the stencil buffer. */
		COLOR_MASK, 		/** State controls updates to the color buffer based on RGB colors of a pixel. */
		BLEND_MODE, 		/** State controls how pixels are blended into the frame buffer. */
		FOG_COLOR, 			/** State controls the color and other properties of fog. */
		LIGHTING, 			/** State controls the color and properties of lights. */
		FOG_ENABLER, 		/** State allows a Geometry to be affected by fog. */
		GLOBAL_LIGHTING,	/** State controls how LIGHTING is used in the world. */
		MATERIAL,			/** State determines the material colors to use when lighting or coloring. */
		TEXTURE, 			/** State determines which TextureImages are bound when rendering the Geometry. */
		SHADER, 			/** State determines if a shader program is used when rendering the Geometry. */
		POLYGON_DRAW_STYLE, /** State controls the style of rendered polygons. */
		LINE_DRAW_STYLE, 	/** State controls the style of rendered lines. */
		POINT_DRAW_STYLE, 	/** State controls the style of rendered points. */
		CLIPPING 			/** State controls the extra clipping planes used when rendering the Geometry. */
	}
	
	/** Return the Role that represents this State's affect.  This must not
	 * return null.  Implementations are responsible for choosing the Role that
	 * best resembles them.
	 * 
	 * If an implementation would need multiple roles, it is likely doing work
	 * that's beyond the scope of a State.  Instead, consider subclassing Appearance
	 * and delegating to smaller State implementations. */
	public Role getRole();
	
	/** Get the renderer specific data that has been assigned to this State.
	 * This object should be modified unless it's by the Renderer that created it.
	 * 
	 * Undefined behavior occurs if it's changed. */
	public Object getStateData();
	
	/** Assign the renderer specific data for this object.  This should not be
	 * called directly, instead it is used by the currently active Renderer instance. 
	 * 
	 * Undefined behavior occurs if this is set by something other than the Renderer. */
	public void setStateData(Object data);
	
	/** A common enum to describe the quality state effects when rendering.  DONT_CARE allows implementation to choose. */
	public static enum Quality {
		FAST, BEST, DONT_CARE
	}
	
	/** A common enum used by some states to describe different pixel comparison tests, such as alpha or depth testing. */
	public static enum PixelTest {
		EQUAL, GREATER, LESS, GEQUAL, LEQUAL, NOT_EQUAL, NEVER, ALWAYS
	}
}
