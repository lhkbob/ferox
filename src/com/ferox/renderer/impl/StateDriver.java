package com.ferox.renderer.impl;

import com.ferox.renderer.RenderException;
import com.ferox.state.State;

/** State driver's provide low-level implementations for a specific
 * type of state (as returned by a state objects getType() method).
 * DriverFactories for state drivers should specify drivers at the
 * state type level, not implementation class level.
 * 
 * For a given state type, there will only ever be one driver object
 * for an AbstractRenderer.  When an AbstractRenderer uses a state
 * driver returned by a valid state driver factory, it is guaranteed
 * that the state objects specified will return the expected type
 * in their getType() method.
 * 
 * When rendering a render atom, there are two different ways in
 * which a State object can be applied: either by being in the 
 * atom's appearance, or being in an influence atom that was determined
 * to have influenced the render atom.
 * 
 * Driver implementations may have some internal limit to the number
 * of simultaneous state instances active for a given type, and may
 * need to ignore some that are queued before a call to doApply().
 * Drivers should make all efforts to use states that will have the 
 * most influence.  Appearance queued states should be weighted less than
 * states queued from influence atoms.
 * 
 * @author Michael Ludwig
 *
 */
public interface StateDriver {
	/** Invoked by an AbstractRenderer to queue a state of this 
	 * driver's expected type.  The given state instance was from
	 * the Appearance of a render atom about to be rendered.
	 * 
	 * If multiple calls to queueAppearanceState() are issued, use the
	 * state specified in the latest queue request.  Only one state
	 * object should be queued by appearance at any point in time.
	 * 
	 * It should be assumed that state has the expected type, that
	 * it's not null.  The driver should not actually modify low-level
	 * graphics until doApply() is called.
	 * 
	 * However, no graphics calls should be made until doApply() is called.
	 * 
	 * Throw an exception if the state has the correct type but is 
	 * somehow unsupported. */
	public void queueAppearanceState(State state) throws RenderException;
	
	/** Similar to queueAppearanceState(), this is invoked when a state
	 * should be applied and the state's source was from an influence atom.
	 * 
	 * If multiple calls to queueInfluenceState are issued, beyond the amount
	 * of simultaneous states, remember the states that have the highest influence
	 * value.
	 * 
	 * The same assumptions and rules as with appearance states.  Additionally,
	 * it can be assumed that influence is in (0, 1] (unless the implementation
	 * uses this method in part of queueAppearanceState()). */
	public void queueInfluenceState(State state, float influence) throws RenderException;
	
	/** Perform the low-level graphics calls necessary to properly apply
	 * the states that have been queued since the last call to doApply().
	 * If some states must be discarded, influence-queued states should be
	 * weighted above appearance-queued states.
	 * 
	 * If no states have been queued since the last call of doApply(), this
	 * call should restore the state record to the default.  This depends
	 * on the type of state, but the rules are in renderAtom() in Renderer.
	 * 
	 * Drivers can assumed that a low-level context is made current on the calling
	 * thread and that low-level graphics calls are allowed.  Implementations must
	 * provide a means by which to get at this information. */
	public void doApply();
	
	/** Clear any queued states that weren't used by a call to doApply().
	 * This could be called because of an end of the frame, or because
	 * a render atom is ignored, or to clean-up after an exception was thrown.
	 * 
	 * Graphics state should not be modified, all necessary state changes
	 * must be made during doApply(). */
	public void reset();
}
