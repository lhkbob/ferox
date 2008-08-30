package com.ferox.core.states;


public class NullUnit implements StateUnit {
	private static NullUnit nu = new NullUnit();
	
	public NullUnit() {
		// do nothing
	}
	
	public int ordinal() {
		return 0;
	}

	public static NullUnit get() {
		return nu;
	}
}
