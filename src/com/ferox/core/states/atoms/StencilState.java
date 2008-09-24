package com.ferox.core.states.atoms;

import com.ferox.core.states.FragmentTest;
import com.ferox.core.states.NullUnit;
import com.ferox.core.states.StateAtom;
import com.ferox.core.states.StateUnit;
import com.ferox.core.util.io.InputChunk;
import com.ferox.core.util.io.OutputChunk;

public class StencilState extends StateAtom {
	public static enum StencilOp {
		KEEP, ZERO, REPLACE, INCREMENT, DECREMENT, INVERT, INCREMENT_WRAP, DECREMENT_WRAP
	}
	
	private FragmentTest stencilFunc;
	
	private StencilOp stencilFail;
	private StencilOp depthFail;
	private StencilOp depthPass;
	
	private int reference;
	private int funcMask;
	
	private int writeMask;
	private boolean stencilEnabled;
	
	public StencilState() {
		super();
		this.stencilFunc = FragmentTest.ALWAYS;
		
		this.stencilFail = StencilOp.KEEP;
		this.depthFail = StencilOp.KEEP;
		this.depthPass = StencilOp.KEEP;
		
		this.reference = 0;
		this.funcMask = Integer.MAX_VALUE - Integer.MIN_VALUE;
		
		this.writeMask = Integer.MAX_VALUE - Integer.MIN_VALUE;
		this.stencilEnabled = true;
	}
	
	public boolean isStencilEnabled() {
		return this.stencilEnabled;
	}
	
	public void setStencilEnabled(boolean e) {
		this.stencilEnabled = e;
	}
	
	public FragmentTest getStencilFunction() {
		return this.stencilFunc;
	}
	
	public void setStencilFunction(FragmentTest stencilFunc) throws NullPointerException {
		if (stencilFunc == null)
			throw new NullPointerException("Stencil func can't be null");
		this.stencilFunc = stencilFunc;
	}

	public StencilOp getStencilFailOp() {
		return this.stencilFail;
	}

	public void setStencilFailOp(StencilOp stencilFail) throws NullPointerException {
		if (stencilFail == null)
			throw new NullPointerException("StencilOp can't be null");
		this.stencilFail = stencilFail;
	}

	public StencilOp getDepthFailOp() {
		return this.depthFail;
	}

	public void setDepthFailOp(StencilOp depthFail) throws NullPointerException {
		if (depthFail == null)
			throw new NullPointerException("StencilOp can't be null");
		this.depthFail = depthFail;
	}

	public StencilOp getDepthPassOp() {
		return this.depthPass;
	}

	public void setDepthPassOp(StencilOp depthPass) throws NullPointerException {
		if (depthPass == null)
			throw new NullPointerException("StencilOp can't be null");
		this.depthPass = depthPass;
	}

	public int getReferenceValue() {
		return this.reference;
	}

	public void setReferenceValue(int reference) {
		this.reference = reference;
	}

	public int getStencilFuncMask() {
		return this.funcMask;
	}

	public void setStencilFuncMask(int funcMask) {
		this.funcMask = funcMask;
	}

	public int getStencilWriteMask() {
		return this.writeMask;
	}

	public void setStencilWriteMask(int writeMask) {
		this.writeMask = writeMask;
	}

	@Override
	public Class<StencilState> getAtomType() {
		return StencilState.class;
	}
	
	@Override
	public void writeChunk(OutputChunk out) {
		super.writeChunk(out);
		
		int[] params = new int[] {this.reference, this.funcMask, this.writeMask};
		out.set("stencil", params);
		out.set("sFunc", this.stencilFunc);
		out.set("sFail", this.stencilFail);
		out.set("dFail", this.depthFail);
		out.set("dPass", this.depthPass);
		out.set("enabled", this.stencilEnabled);
	}
	
	@Override
	public void readChunk(InputChunk in) {
		super.readChunk(in);
		
		int[] params = in.getIntArray("stencil");
		
		this.stencilEnabled = in.getBoolean("enabled");
		
		this.stencilFunc = in.getEnum("sFunc", FragmentTest.class);
		this.stencilFail = in.getEnum("sFail", StencilOp.class);
		this.depthFail = in.getEnum("dFail", StencilOp.class);
		this.depthPass = in.getEnum("dPass", StencilOp.class);
		this.reference = params[0];
		this.funcMask = params[1];
		this.writeMask = params[2];
	}

	@Override
	public boolean isValidUnit(StateUnit unit) {
		return unit instanceof NullUnit;
	}
}
