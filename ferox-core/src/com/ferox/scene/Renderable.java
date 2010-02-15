package com.ferox.scene;

import com.ferox.renderer.Renderer.DrawStyle;
import com.ferox.util.entity.AbstractComponent;
import com.ferox.util.entity.Description;

@Description("Declares that an Entity should be rendered")
public final class Renderable extends AbstractComponent<Renderable> {
	private DrawStyle frontStyle;
	private DrawStyle backStyle;
	
	public Renderable() {
		this(DrawStyle.SOLID, DrawStyle.NONE);
	}
	
	public Renderable(DrawStyle front, DrawStyle back) {
		super(Renderable.class);
		
		setDrawStyleFront(front);
		setDrawStyleBack(back);
	}
	
	public void setDrawStyleFront(DrawStyle front) {
		if (front == null)
			throw new NullPointerException("DrawStyle cannot be null");
		frontStyle = front;
	}
	
	public DrawStyle getDrawStyleFront() {
		return frontStyle;
	}
	
	public void setDrawStyleBack(DrawStyle back) {
		if (back == null)
			throw new NullPointerException("DrawStyle cannot be null");
		backStyle = back;
	}
	
	public DrawStyle getDrawStyleBack() {
		return backStyle;
	}
}
