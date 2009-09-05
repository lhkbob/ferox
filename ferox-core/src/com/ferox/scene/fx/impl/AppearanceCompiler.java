package com.ferox.scene.fx.impl;

import com.ferox.scene.fx.Appearance;

public interface AppearanceCompiler<T> {
	public T compile(Appearance a);
	
	public void clean(Appearance a);
}
