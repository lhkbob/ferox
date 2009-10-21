package com.ferox.renderer.impl;

import com.ferox.renderer.RenderSurface;

public abstract class Action {
	private Action next;
	private RenderSurface surface;
	
	public Action(RenderSurface surface) {
		this.surface = surface;
	}
	
	public Action next() {
		return next;
	}
	
	public void setNext(Action action) {
		next = action;
	}
	
	public RenderSurface getRenderSurface() {
		return surface;
	}
	
	public Action prepare(Action previous) {
		if (previous != null && previous.next != this)
			throw new IllegalArgumentException("Previous Action does not point to this Action");
		if (surface != null && surface.isDestroyed()) {
			return splice(previous);
		}
		return next;
	}
	
	public abstract void perform();
	
	protected Action splice(Action prev) {
		Action n = next;
		next = null;
		if (prev != null)
			prev.next = n;
		
		return n;
	}
}
