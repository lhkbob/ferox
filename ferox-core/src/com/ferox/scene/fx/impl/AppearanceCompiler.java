package com.ferox.scene.fx.impl;

import com.ferox.scene.fx.Appearance;

public interface AppearanceCompiler<T> {
	public T compile(Appearance a, T previous);
	
	public void clean(T compiled);
}
