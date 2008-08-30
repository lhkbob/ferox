package com.ferox.core.states.atoms;

import com.ferox.core.states.FragmentTest;
import com.ferox.core.states.NullUnit;
import com.ferox.core.states.StateAtom;
import com.ferox.core.states.StateUnit;
import com.ferox.core.util.io.InputChunk;
import com.ferox.core.util.io.OutputChunk;

public class AlphaState extends StateAtom {	
	private FragmentTest alphaTest;
	private float refValue;
	private boolean alphaEnabled;
	
	public AlphaState() {
		super();
		this.alphaTest = FragmentTest.GEQUAL;
		this.refValue = 1f;
		this.alphaEnabled = true;
	}

	public FragmentTest getAlphaTest() {
		return this.alphaTest;
	}

	public void setAlphaTest(FragmentTest alphaTest) {
		this.alphaTest = alphaTest;
	}

	public float getAlphaReferenceValue() {
		return this.refValue;
	}

	public void setAlphaRefValue(float refValue) {
		if (refValue < 0f || refValue > 1f)
			throw new IllegalArgumentException("alpha reference value must be between 0 and 1");
		this.refValue = refValue;
	}

	public boolean isAlphaEnabled() {
		return this.alphaEnabled;
	}

	public void setAlphaEnabled(boolean alphaEnabled) {
		this.alphaEnabled = alphaEnabled;
	}
	
	@Override
	public void writeChunk(OutputChunk out) {
		super.writeChunk(out);
		
		out.setEnum("test", this.alphaTest);
		out.setFloat("refValue", this.refValue);
		out.setBoolean("aEnabled", this.alphaEnabled);
	}
	
	@Override
	public void readChunk(InputChunk in) {
		super.readChunk(in);
		
		this.alphaTest = in.getEnum("test", FragmentTest.class);
		
		this.refValue = in.getFloat("refValue");
		this.alphaEnabled = in.getBoolean("aEnabled");
	}

	@Override
	public Class<AlphaState> getAtomType() {
		return AlphaState.class;
	}

	@Override
	public boolean isValidUnit(StateUnit unit) {
		return unit instanceof NullUnit;
	}
}
