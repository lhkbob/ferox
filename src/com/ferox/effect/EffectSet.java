package com.ferox.effect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.ferox.effect.EffectType.Type;

/**
 * EffectSet is a simple data structure to hold onto a collection of Effects,
 * and it manages the logic behind an Effect's Types and EffectType annotations.
 * 
 * @author Michael Ludwig
 */
public class EffectSet {
	private List<Effect> listEffects;
	private List<Effect> lockedEffects;

	/** Default constructor, sets up an empty EffectSet. */
	public EffectSet() {
		listEffects = new ArrayList<Effect>();
		lockedEffects = Collections.unmodifiableList(listEffects);
	}

	/**
	 * Add the given Effect to the system. This has no effect if e is null or is
	 * already in the EffectSet.
	 * 
	 * @param e The Effect to add to this EffectSet
	 * @throws IllegalStateException if e conflicts with already added Effect
	 */
	public void add(Effect e) {
		if (e != null && !listEffects.contains(e)) {
			// only add if if it's not already present,
			// and now check for conflicts with old Effects
			int numEffects = listEffects.size();
			for (int j = 0; j < numEffects; j++) {
				if (inConflict(e, listEffects.get(j)))
					throw new IllegalStateException("Cannot add Effect " + e
							+ ", it's in conflict with " + listEffects.get(j));
			}

			// if we've gotten here, it's okay to add the effect
			listEffects.add(e);
		}
	}

	/**
	 * Remove an Effect from this EffectSet. Has no effect if e is null or
	 * wasn't part of this set.
	 * 
	 * @param e The Effect to remove
	 * @return True if the EffectSet was modified
	 */
	public boolean remove(Effect e) {
		return (e == null ? false : listEffects.remove(e));
	}

	/** @return The number of Effects in this EffectSet */
	public int size() {
		return listEffects.size();
	}

	/**
	 * Get all Effects that have been added to this EffectSet. They are
	 * guaranteed to not be in conflict, based on each's associated Types.
	 * 
	 * @return An unmodifiable list holding all Effects
	 */
	public List<Effect> getAll() {
		return lockedEffects;
	}

	/** Remove all Effects from this EffectSet. */
	public void clear() {
		listEffects.clear();
	}

	/*
	 * Utility to determine if e1 and e2 are in conflict, based off of their
	 * types.
	 */
	private static boolean inConflict(Effect e1, Effect e2) {
		Type[] type1 = e1.getTypes();
		Type[] type2 = e2.getTypes();

		for (int i = 0; i < type1.length; i++) {
			if (!type1[i].getMultipleEffects()) {
				// only check further if we can't have multiples
				for (int j = 0; j < type2.length; j++) {
					if (!type2[j].getMultipleEffects()
							&& type1[i].equals(type2[j]))
						return true;
				}
			}
		}

		// we've gotten here, so no common Types, or the Types all allow
		// multiple effects
		return false;
	}
}
