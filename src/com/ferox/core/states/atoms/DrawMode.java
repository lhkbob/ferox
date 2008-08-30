package com.ferox.core.states.atoms;

import com.ferox.core.states.NullUnit;
import com.ferox.core.states.StateAtom;
import com.ferox.core.states.StateUnit;
import com.ferox.core.util.io.InputChunk;
import com.ferox.core.util.io.OutputChunk;

public class DrawMode extends StateAtom {
	public static enum DrawStyle {
		FILLED, WIREFRAME
	}
	
	public static enum DrawFace {
		BACK, FRONT, FRONT_AND_BACK
	}
	
	public static enum Winding {
		CLOCKWISE, COUNTER_CLOCKWISE
	}
	
	private DrawStyle frontMode;
	private DrawStyle backMode;
	private DrawFace drawFace;
	private Winding winding;
	
	public DrawMode() {
		super();
		this.setDrawFace(DrawFace.FRONT_AND_BACK);
		this.setBackMode(DrawStyle.WIREFRAME);
		this.setFrontMode(DrawStyle.FILLED);
		this.setWinding(Winding.COUNTER_CLOCKWISE);
	}

	public DrawStyle getFrontMode() {
		return this.frontMode;
	}

	public void setFrontMode(DrawStyle frontMode) {
		this.frontMode = frontMode;
	}

	public DrawStyle getBackMode() {
		return this.backMode;
	}

	public void setBackMode(DrawStyle backMode) {
		this.backMode = backMode;
	}

	public DrawFace getDrawFace() {
		return this.drawFace;
	}

	public void setDrawFace(DrawFace drawFace) {
		this.drawFace = drawFace;
	}

	public Winding getWinding() {
		return this.winding;
	}

	public void setWinding(Winding winding) {
		this.winding = winding;
	}
	
	@Override
	public void readChunk(InputChunk in) {
		super.readChunk(in);
		
		
		this.backMode = in.getEnum("backMode", DrawStyle.class);
		this.drawFace = in.getEnum("drawFace", DrawFace.class);
		this.frontMode = in.getEnum("frontMode", DrawStyle.class);
		this.winding = in.getEnum("winding", Winding.class);
	}
	
	@Override
	public void writeChunk(OutputChunk out) {
		super.writeChunk(out);
		
		out.setEnum("backMode", this.backMode);
		out.setEnum("frontMode", this.frontMode);
		out.setEnum("drawFace", this.drawFace);
		out.setEnum("winding", this.winding);
	}

	@Override
	public Class<DrawMode> getAtomType() {
		return DrawMode.class;
	}

	@Override
	public boolean isValidUnit(StateUnit unit) {
		return unit instanceof NullUnit;
	}
}
