package com.ferox.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <p>
 * A UnitList is meant to store the list of <unit, item> intersection, often
 * when using textures.
 * </p>
 * <p>
 * The UnitList supports units >= 0, with no maximum limit. It will dynamically
 * change between array access and list access to maximize storage size and
 * search time.
 * </p>
 * <p>
 * In most cases, access should just be an array access.
 * </p>
 * 
 * @author Michael Ludwig
 */
public class UnitList<T> {
	/**
	 * Represents a single intersection between the unit value and the item at
	 * that unit.
	 */
	public static class Unit<T> {
		private T data;
		private final int unit;

		private Unit(T data, int unit) {
			this.data = data;
			this.unit = unit;
		}

		/**
		 * Get the item bound to the given unit.
		 * 
		 * @return The T bound to getUnit()
		 */
		public T getData() {
			return this.data;
		}

		/**
		 * Get the unit index for this Unit.
		 * 
		 * @return The unit that getData() is bound to
		 */
		public int getUnit() {
			return this.unit;
		}
	}

	private static final int MAX_UNIT_DISPARITY = 16;
	private static final float MIN_UNIT_USAGE = .5f;

	private final List<Unit<T>> units;
	private final List<Unit<T>> readOnlyUnits;

	private boolean useRandomAccess;
	private T[] randomAccessData;
	private int raUnitOffset;

	/** Construct an empty UnitList. */
	public UnitList() {
		this.units = new ArrayList<Unit<T>>();
		this.readOnlyUnits = Collections.unmodifiableList(this.units);
		this.resetRandomAccess();
	}

	/**
	 * Set the item on the given unit. If item is null, makes it so that there
	 * is no item bound on the given unit.
	 * 
	 * @param unit The unit to bind item to
	 * @param item The new item bound to unit
	 * @throws IndexOutOfBoundsException if unit < 0
	 */
	@SuppressWarnings("unchecked")
	public void setItem(int unit, T item) {
		if (unit < 0)
			throw new IndexOutOfBoundsException("Invalid unit, units cannot be less than 0: " + unit);

		int nuIndex = -1;

		int count = this.size();
		Unit<T> tu;
		for (int i = 0; i < count; i++) {
			tu = this.units.get(i);
			if (tu.unit == unit) {
				nuIndex = unit;
				break;
			}
		}

		if (nuIndex < 0 && item == null)
			return; // no change necessary

		if (nuIndex < 0) {
			this.units.add(new Unit<T>(item, unit));
			count++;
		} else if (item == null) {
			this.units.remove(nuIndex);
			count--;
		} else
			this.units.get(nuIndex).data = item;

		if (this.units.isEmpty()) { // no more textures left, so reset the ra
			// record
			this.resetRandomAccess();
			return;
		}

		// calculate the new unit range
		int minUnit = Integer.MAX_VALUE;
		int maxUnit = Integer.MIN_VALUE;
		for (int i = 0; i < count; i++) {
			tu = this.units.get(i);
			if (tu.unit < minUnit)
				minUnit = tu.unit;
			if (tu.unit > maxUnit)
				maxUnit = tu.unit;
		}

		boolean useRa = (maxUnit - minUnit) < MAX_UNIT_DISPARITY || ((float) count) / (maxUnit - minUnit) > MIN_UNIT_USAGE;

		if (!useRa)
			// no longer need random access, so clear it
			this.resetRandomAccess();
		else if (this.useRandomAccess) {
			if (minUnit != this.raUnitOffset || maxUnit != (this.raUnitOffset + this.randomAccessData.length - 1)) {
				// we need an update
				T[] newTex = (T[]) new Object[maxUnit - minUnit + 1];
				System.arraycopy(this.randomAccessData, Math.max(0, minUnit - this.raUnitOffset), newTex,
								 Math.max(0, this.raUnitOffset - minUnit),
								 Math.min(this.randomAccessData.length + this.raUnitOffset, maxUnit + 1) - this.raUnitOffset);
				this.randomAccessData = newTex;
				this.raUnitOffset = minUnit;
			}

			// update the ra record
			this.randomAccessData[unit - this.raUnitOffset] = item;
		} else {
			this.useRandomAccess = true;
			this.randomAccessData = (T[]) new Object[maxUnit - minUnit + 1];
			this.raUnitOffset = minUnit;

			// fill the ra record
			for (int i = 0; i < count; i++) {
				tu = this.units.get(i);
				this.randomAccessData[tu.unit - this.raUnitOffset] = tu.data;
			}
		}
	}

	/**
	 * Get the item currently set for the given unit. A return value of null
	 * signifies that there is no item bound to that unit.
	 * 
	 * @param unit The unit that the returned item is bound to
	 * @return The item bound to unit, or null if there was no binding
	 * @throws IndexOutOfBoundsException if unit < 0
	 */
	public T getItem(int unit) {
		if (unit < 0)
			throw new IndexOutOfBoundsException("Invalid unit, units cannot be less than 0: " + unit);

		if (this.useRandomAccess) {
			if (unit < this.raUnitOffset || unit >= (this.raUnitOffset + this.randomAccessData.length))
				return null;
			return this.randomAccessData[unit - this.raUnitOffset];
		} else {
			int numTex = this.units.size();
			Unit<T> tu;
			for (int i = 0; i < numTex; i++) {
				tu = this.units.get(i);
				if (unit == tu.unit)
					return tu.data;
			}

			return null;
		}
	}

	/**
	 * Return a list of all bounds units. Unit values not present have no item
	 * bound to them.
	 * 
	 * @return An unmodifiable list of all non-null bindings to units
	 */
	public List<Unit<T>> getItems() {
		return this.readOnlyUnits;
	}

	/**
	 * Return the number of items in this UnitList.
	 * 
	 * @return The number of bound items
	 */
	public int size() {
		return this.units.size();
	}

	/** Remove all items currently set in this UnitList. */
	public void clear() {
		this.units.clear();
		this.resetRandomAccess();
	}

	// reset the random access portion of storage
	private void resetRandomAccess() {
		this.useRandomAccess = false;
		this.randomAccessData = null;
		this.raUnitOffset = 0;
	}
}
