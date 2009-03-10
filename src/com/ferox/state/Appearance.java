package com.ferox.state;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.ferox.state.State.Role;

/**
 * Represents the overall appearance of something on the screen, including its
 * geometry and shape.  Used by Shape to specify objects in a scene.  Wherever
 * possible, it is recommended to re-use Appearance instances for performance
 * interests.
 * 
 * @author Michael Ludwig
 *
 */
public class Appearance {
	private List<State> states;
	private List<State> lockedStates;
	private int cachedHash;
	
	/** Construct an appearance with the given list of states.  The last state of a given
	 * type will be used if there are more than one state with the same type. */
	public Appearance(State... states) {
		this.states = new ArrayList<State>(states.length);
		this.lockedStates = Collections.unmodifiableList(this.states);
		
		for (State s: states)
			this.addState(s);
	}
	
	/** Create an appearance with no attached states. */
	public Appearance() {
		this((State)null);
	}
	
	/** Add the given state to this appearance.  Does nothing if state is null.
	 * Returns the previous state of state's type.  A return value of null implies
	 * that there was no previous state of the given type set on this Appearance. 
	 * 
	 * Marks this Appearance as dirty. */
	public State addState(State state) {
		if (state == null)
			return null;
		int size = this.states.size();
		State prev = null;

		for (int i = 0; i < size; i++) {
			prev = this.states.get(i);
			if (prev.getRole().equals(state.getRole())) {
				this.states.set(i, state);
				this.computeHash();
				return prev;
			}
		}
		
		this.states.add(state);
		this.computeHash();
		return null;
	}
	
	/** Removes the given state from this appearance.  Does nothing if state is null.
	 * Returns true if the state was present to be removed. If true is returned, 
	 * this Appearance is marked as dirty. */
	public boolean removeState(State state) {
		boolean rem = this.states.remove(state);
		this.computeHash();
		return rem;
	}
	
	/** Get the state for a given type on this appearance. */
	public State getState(Role role) {
		if (role == null)
			return null;
		for (State s: this.states) {
			if (s.getRole().equals(role))
				return s;
		}
		return null;
	}
	
	/** Get all states on this appearance. Returns an unmodifiable list backed by this appearance.
	 * Therefore any changes to this appearance will be present in the list, too. */
	public List<State> getStates() {
		return this.lockedStates;
	}
	
	/** Return the xor of all of this Appearance's state's 
	 * identity hashes.  If this number changes between subsequent
	 * calls to sortKey(), it implies the list returned by
	 * getStates() has had a state added or removed. */
	public int sortKey() {
		return this.cachedHash;
	}
	
	// compute the new cachedHash to be returned by sortKey()
	private void computeHash() {
		int size = this.states.size();
		this.cachedHash = 0;
		
		for (int i = 0; i < size; i++)
			this.cachedHash ^= System.identityHashCode(this.states.get(i));
	}
}
