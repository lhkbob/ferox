package com.ferox.renderer;

import com.ferox.state.State;

/** An influence atom represents a dynamically attached state based on some
 * rule of "influence".  At the moment the only implemented influences are
 * lights and fogs, allowing programmers to add a light or fog node to a scene
 * directly without worrying about setting it on the Appearance (they do have
 * to add a tag state that enables the dynamic state, but that's light/fog
 * implementation dependent).
 * 
 * @author Michael Ludwig
 *
 */
public interface InfluenceAtom {
	/** Get the state that should be dynamically added to each
	 * render atom that it influences in the RenderQueue. 
	 * 
	 * Must not return null - arbiters of atoms should not submit an
	 * influence atom to a RenderQueue if it doesn't have a state.
	 * 
	 * Most states returned by influence atom also require render atom's
	 * to have an enabler state set in their appearance.  For example,
	 * a Light will have no effect unless the render atoms also have a 
	 * LightReceiver state attached to its appearance. */
	public State getState();
	
	/** Compute the influence for the given atom.
	 * Return a value > 0 (and <= 1) to designate that this influence atom
	 * influences the given render atom.  A value <= 0 implies that
	 * this influence has no affect on the render atom.
	 * 
	 * When deciding which influence atoms to use for a given type, assuming
	 * there is an implementation limitation to the amount, the renderer
	 * can use the influence value to determine which influence atoms to use. */
	public float influences(RenderAtom atom);
	
	/** Get the RenderQueue specific data for this InfluenceAtom.
	 * Returns null if the RenderQueue never set any data, or if it set null.
	 * 
	 * Returns null if the RenderQueue is null. */
	public Object getRenderQueueData(RenderQueue pipe);
	
	/** Set the RenderQueue specific data for this InfluenceAtom, overwriting
	 * any previous value.
	 * 
	 * Does nothing if the RenderQueue is null. */
	public void setRenderQueueData(RenderQueue pipe, Object data);
}
