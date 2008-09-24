package com.ferox.core.states.atoms;

import com.ferox.core.states.NullUnit;
import com.ferox.core.states.StateAtom;
import com.ferox.core.states.StateUnit;
import com.ferox.core.util.io.InputChunk;
import com.ferox.core.util.io.OutputChunk;

public class BlendState extends StateAtom {
	public static enum BlendFunction {
		ADD, SUBTRACT, REVERSE_SUBTRACT, MIN, MAX
	}
	
	public static enum BlendFactor {
		ZERO, ONE, SRC_COLOR, ONE_MINUS_SRC_COLOR, SRC_ALPHA, ONE_MINUS_SRC_ALPHA, SRC_ALPHA_SATURATE
	}
	
	private BlendFunction blendFunc;
	private BlendFactor srcBlendFactor;
	private BlendFactor dstBlendFactor;
	private boolean blendEnabled;
	
	public BlendState() {
		super();
		this.blendFunc = BlendFunction.ADD;
		this.srcBlendFactor = BlendFactor.SRC_ALPHA;
		this.dstBlendFactor = BlendFactor.ONE_MINUS_SRC_ALPHA;
		this.blendEnabled = true;
	}
	
	public BlendFunction getBlendFunction() {
		return this.blendFunc;
	}

	public void setBlendFunction(BlendFunction blendFunc) throws NullPointerException {
		if (blendFunc == null)
			throw new NullPointerException("Blend function can't be null");
		this.blendFunc = blendFunc;
	}

	public BlendFactor getSourceBlendFactor() {
		return this.srcBlendFactor;
	}

	public void setSourceBlendFactor(BlendFactor srcBlendFactor) throws NullPointerException {
		if (srcBlendFactor == null)
			throw new NullPointerException("Source blend factor can't be null");
		this.srcBlendFactor = srcBlendFactor;
	}

	public BlendFactor getDestBlendFactor() {
		return this.dstBlendFactor;
	}

	public void setDestBlendFactor(BlendFactor dstBlendFactor) throws NullPointerException {
		if (dstBlendFactor == null)
			throw new NullPointerException("Dest blend factor can't be null");
		this.dstBlendFactor = dstBlendFactor;
	}

	public boolean isBlendEnabled() {
		return this.blendEnabled;
	}

	public void setBlendEnabled(boolean blendEnabled) {
		this.blendEnabled = blendEnabled;
	}

	@Override
	public void writeChunk(OutputChunk out) {
		super.writeChunk(out);
		
		out.set("bFunc", this.blendFunc);
		out.set("srcFactor", this.srcBlendFactor);
		out.set("dstFactor", this.dstBlendFactor);
		out.set("bEnabled", this.blendEnabled);
	}
	
	@Override
	public void readChunk(InputChunk in) {
		super.readChunk(in);
		
		this.blendFunc = in.getEnum("bFunc", BlendFunction.class);
		this.srcBlendFactor = in.getEnum("srcFactor", BlendFactor.class);
		this.dstBlendFactor = in.getEnum("dstFactor", BlendFactor.class);
		
		this.blendEnabled = in.getBoolean("bEnabled");
	}

	@Override
	public Class<BlendState> getAtomType() {
		return BlendState.class;
	}
	
	@Override
	public boolean isValidUnit(StateUnit unit) {
		return unit instanceof NullUnit;
	}
}
