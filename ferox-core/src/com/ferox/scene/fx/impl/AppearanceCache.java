package com.ferox.scene.fx.impl;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;

import com.ferox.scene.fx.Appearance;
import com.ferox.scene.fx.SceneCompositor;

@SuppressWarnings("unchecked")
public class AppearanceCache<T> {
	private class AppearanceReference extends WeakReference<Appearance> {
		private final T cache;
		
		public AppearanceReference(Appearance referent, T compiledAppearance) {
			super(referent);
			cache = compiledAppearance;
		}
	}
	
	private final SceneCompositor compositor;
	private final AppearanceCompiler<T> compiler;

	private final Set<AppearanceReference> compiledAppearances; // used for clean-up
	private final IdentityHashMap<Appearance, T> tempCompiledAppearances;
	
	public AppearanceCache(SceneCompositor compositor, AppearanceCompiler<T> compiler) {
		this.compiler = compiler;
		this.compositor = compositor;
		
		tempCompiledAppearances = new IdentityHashMap<Appearance, T>();
		compiledAppearances = new HashSet<AppearanceReference>();
	}
	
	public T get(Appearance a) {
		if (a == null)
			throw new NullPointerException("Cannot retreive a compilation for a null Appearance");
		
		// check for an explicitly compiled version
		T cache = (T) a.getData(compositor);
		if (cache != null)
			return cache;
		
		// check if it's already been temporarily compiled
		cache = tempCompiledAppearances.get(a);
		if (cache != null)
			return cache;
		
		// no hit, so compile it now
		cache = compiler.compile(a, null);
		tempCompiledAppearances.put(a, cache);
		return cache;
	}
	
	public T compile(Appearance a) {
		if (a == null)
			throw new NullPointerException("Cannot compile a null Appearance");
		
		// clean up any old compilations
		T cache = tempCompiledAppearances.remove(a);
		
		if (cache == null) {
			cache = (T) a.getData(compositor);
			if (cache != null)
				remove(a);
		}
		
		// if cache is non-null during compile(), it will be reused by compiler
		cache = compiler.compile(a, cache);
		
		// persist compiled result
		compiledAppearances.add(new AppearanceReference(a, cache));
		a.setData(compositor, cache);
		
		return cache;
	}
	
	public void clean(Appearance a) {
		if (a == null)
			throw new NullPointerException("Cannot clean-up a null Appearance");
		
		T cache = tempCompiledAppearances.remove(a);
		if (cache != null)
			compiler.clean(cache);
		
		cache = (T) a.getData(compositor);
		if (cache != null) {
			compiler.clean(cache);
			a.setData(compositor, null);
			remove(a);
		}
	}
	
	public void cleanAll() {
		reset();
		
		AppearanceReference ar;
		Iterator<AppearanceReference> it = compiledAppearances.iterator();
		while(it.hasNext()) {
			ar = it.next();
			compiler.clean(ar.cache);
			if (ar.get() != null)
				ar.get().setData(compositor, null);
			
			it.remove();
		}
	}
	
	public void reset() {
		Iterator<Entry<Appearance, T>> it = tempCompiledAppearances.entrySet().iterator();
		while(it.hasNext()) {
			compiler.clean(it.next().getValue());
			it.remove();
		}
		
		AppearanceReference ar;
		Iterator<AppearanceReference> ca = compiledAppearances.iterator();
		while(ca.hasNext()) {
			ar = ca.next();
			if (ar.get() == null) {
				// appearance has been gc'ed
				compiler.clean(ar.cache);
				ca.remove();
			}
		}
	}
	
	private void remove(Appearance a) {
		Iterator<AppearanceReference> it = compiledAppearances.iterator();
		while(it.hasNext()) {
			if (it.next().get() == a) {
				it.remove();
				break;
			}
		}
	}
}
