package com.ferox.core.states.atoms;

import com.ferox.core.states.FragmentTest;
import com.ferox.core.states.NullUnit;
import com.ferox.core.states.StateAtom;
import com.ferox.core.states.StateUnit;
import com.ferox.core.util.io.InputChunk;
import com.ferox.core.util.io.OutputChunk;

public class ZBuffer extends StateAtom {
	private FragmentTest zTestFunction;
	private boolean zMask;
	
	public ZBuffer() {
		super();
		this.setZBufferWriteEnabled(true);
		this.setDepthTest(FragmentTest.LEQUAL);
	}

	public FragmentTest getDepthTest() {
		return this.zTestFunction;
	}

	public void setDepthTest(FragmentTest testFunction) {
		this.zTestFunction = testFunction;
	}

	public boolean isZBufferWriteEnabled() {
		return this.zMask;
	}
 
	public void setZBufferWriteEnabled(boolean mask) {
		this.zMask = mask;
	}
	
	@Override
	public void readChunk(InputChunk in) {
		super.readChunk(in);
		
		this.zTestFunction = in.getEnum("zTest", FragmentTest.class);
		this.zMask = in.getBoolean("zMask");
	}
	
	@Override
	public void writeChunk(OutputChunk out) {
		super.writeChunk(out);
		
		out.setEnum("zTest", this.zTestFunction);
		out.setBoolean("zMask", this.zMask);
	}

	@Override
	public Class<? extends StateAtom> getAtomType() {
		return ZBuffer.class;
	}

	@Override
	public boolean isValidUnit(StateUnit unit) {
		return unit instanceof NullUnit;
	}
}
