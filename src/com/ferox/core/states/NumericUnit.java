package com.ferox.core.states;


public class NumericUnit implements StateUnit {
	private static NumericUnit[] cache;
	
	private int unit;
	
	public NumericUnit(int unit) {
		this.unit = Math.max(0, unit);
	}
	
	public int ordinal() {
		return this.unit;
	}

	public static NumericUnit get(int unit) {
		unit = Math.max(0, unit);
		
		if (cache == null || cache.length <= unit) {
			NumericUnit[] temp = new NumericUnit[unit + 1];
			if (cache != null) 
				System.arraycopy(cache, 0, temp, 0, cache.length);
			cache = temp;
		}
		if (cache[unit] == null)
			cache[unit] = new NumericUnit(unit);
		return cache[unit];
	}
}
