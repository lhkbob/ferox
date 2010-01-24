package com.ferox.scene.fx;

import com.ferox.renderer.Renderer.DrawStyle;
import com.ferox.util.entity.Component;

public class Renderable extends Component {
	private static final String DESCR = "Declares that an Entity should be rendered";
	
	private DrawStyle frontStyle;
	private DrawStyle backStyle;
	
	public Renderable() {
		this(DrawStyle.SOLID, DrawStyle.NONE);
	}
	
	public Renderable(DrawStyle front, DrawStyle back) {
		super(DESCR);
		
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
