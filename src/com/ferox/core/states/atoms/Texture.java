package com.ferox.core.states.atoms;

import com.ferox.core.scene.Transform;
import com.ferox.core.states.NumericUnit;
import com.ferox.core.states.StateAtom;
import com.ferox.core.states.StateUnit;
import com.ferox.core.states.manager.TextureManager;
import com.ferox.core.util.io.InputChunk;
import com.ferox.core.util.io.OutputChunk;

public class Texture extends StateAtom {
	public static enum EnvMode {
		REPLACE, DECAL, MODULATE, BLEND, COMBINE
	}
	
	public static enum AutoTCGen {
		NONE, OBJECT, EYE, SPHERE, REFLECTION, NORMAL
	}
	
	public static enum CombineAlphaFunc {
		REPLACE, ADD, MODULATE, INTERPOLATE, ADD_SIGNED, SUBTRACT
	}
	
	public static enum CombineRGBFunc {
		REPLACE, ADD, MODULATE, INTERPOLATE, ADD_SIGNED, SUBTRACT, DOT3_RGB, DOT3_RGBA
	}
	
	public static enum CombineSource {
		CURR_TEX, PREV_TEX, BLEND_COLOR, VERTEX_COLOR,
		TEX0, TEX1, TEX2, TEX3, TEX4, TEX5, TEX6, TEX7, TEX8, TEX9, TEX10, TEX11, TEX12,
		TEX13, TEX14, TEX15, TEX16, TEX17, TEX18, TEX19, TEX20, TEX21, TEX22, TEX23, TEX24, TEX25,
		TEX26, TEX27, TEX28, TEX29, TEX30, TEX31
	}
	
	public static enum CombineOp {
		COLOR, ALPHA, ONE_MINUS_COLOR, ONE_MINUS_ALPHA
	}
	
	private TextureData data;
	
	private CombineRGBFunc combineRGBFunc;
	private CombineAlphaFunc combineAlphaFunc;
	
	private CombineSource sourceRGB0;
	private CombineSource sourceRGB1;
	private CombineSource sourceRGB2;
	
	private CombineSource sourceAlpha0;
	private CombineSource sourceAlpha1;
	private CombineSource sourceAlpha2;
	
	private CombineOp operandRGB0;
	private CombineOp operandRGB1;
	private CombineOp operandRGB2;
	
	private CombineOp operandAlpha0;
	private CombineOp operandAlpha1;
	private CombineOp operandAlpha2;
	
	private EnvMode texEnvMode;
	private float[] texEnvColor;
	
	private float[] planeR;
	private float[] planeS;
	private float[] planeT;
	
	private AutoTCGen rCoordGen;
	private AutoTCGen sCoordGen;
	private AutoTCGen tCoordGen;
	
	private Transform texTrans;
	
	public Texture(TextureData data) {
		this();
		this.setData(data);
	}
	
	public Texture(TextureData data, EnvMode mode) {
		this();
		this.setData(data);
		this.setTexEnvMode(mode);
	}
	
	public Texture(TextureData data, EnvMode mode, float[] envColor, AutoTCGen texCoord) {
		this();
		this.setData(data);
		this.setTexEnvMode(mode);
		this.setTexEnvColor(envColor);
		this.setSTRCoordGen(texCoord);
	}
	
	public Texture() {
		super();
		
		this.rCoordGen = this.sCoordGen = this.tCoordGen = AutoTCGen.NONE;
		this.texEnvColor = new float[] {0f, 0f, 0f, 0f};
		this.texEnvMode = EnvMode.MODULATE;
		
		this.combineAlphaFunc = CombineAlphaFunc.MODULATE;
		this.combineRGBFunc = CombineRGBFunc.MODULATE;
		
		this.sourceRGB0 = CombineSource.CURR_TEX;
		this.sourceRGB1 = CombineSource.PREV_TEX;
		this.sourceRGB2 = CombineSource.VERTEX_COLOR;
		this.sourceAlpha0 = CombineSource.CURR_TEX;
		this.sourceAlpha1 = CombineSource.PREV_TEX;
		this.sourceAlpha2 = CombineSource.VERTEX_COLOR;
		
		this.operandRGB0 = CombineOp.COLOR;
		this.operandRGB1 = CombineOp.COLOR;
		this.operandRGB2 = CombineOp.ALPHA;
		this.operandAlpha0 = CombineOp.ALPHA;
		this.operandAlpha1 = CombineOp.ALPHA;
		this.operandAlpha2 = CombineOp.ALPHA;
		
		this.planeR = new float[] {0f, 0f, 1f, 0f};
		this.planeS = new float[] {1f, 0f, 0f, 0f};
		this.planeT = new float[] {0f, 1f, 0f, 0f};
		
		this.data = null;
		this.texTrans = null;
	}

	@Override
	public Class<Texture> getAtomType() {
		return Texture.class;
	}
	
	public float[] getTexCoordGenPlaneR() {
		return this.planeR;
	}
	
	public float[] getTexCoordGenPlaneS() {
		return this.planeS;
	}
	
	public float[] getTexCoordGenPlaneT() {
		return this.planeT;
	}
	
	public void setTexCoordGenPlaneR(float[] plane) throws IllegalArgumentException {
		if (plane.length != 4)
			throw new IllegalArgumentException("Plane needs 4 elements");
		this.planeR = plane;
	}
	
	public void setTexCoordGenPlaneS(float[] plane) throws IllegalArgumentException {
		if (plane.length != 4)
			throw new IllegalArgumentException("Plane needs 4 elements");
		this.planeS = plane;
	}
	
	public void setTexCoordGenPlaneT(float[] plane) throws IllegalArgumentException {
		if (plane.length != 4)
			throw new IllegalArgumentException("Plane needs 4 elements");
		this.planeT = plane;
	}
	
	public void setTexCoordTransform(Transform trans) {
		this.texTrans = trans;
	}
	
	public Transform getTexCoordTransform() {
		return this.texTrans;
	}
	
	public TextureData getData() {
		return this.data;
	}

	public void setData(TextureData data) {
		this.data = data;
	}

	public CombineRGBFunc getCombineRGBFunc() {
		return this.combineRGBFunc;
	}

	public void setCombineRGBFunc(CombineRGBFunc combineRGBFunc) {
		this.combineRGBFunc = combineRGBFunc;
	}

	public CombineAlphaFunc getCombineAlphaFunc() {
		return this.combineAlphaFunc;
	}

	public void setCombineAlphaFunc(CombineAlphaFunc combineAlphaFunc) {
		this.combineAlphaFunc = combineAlphaFunc;
	}
	
	public CombineSource getSourceRGB0() {
		return this.sourceRGB0;
	}

	public void setSourceRGB0(CombineSource sourceRGB0) {
		this.sourceRGB0 = sourceRGB0;
	}

	public CombineSource getSourceRGB1() {
		return this.sourceRGB1;
	}

	public void setSourceRGB1(CombineSource sourceRGB1) {
		this.sourceRGB1 = sourceRGB1;
	}

	public CombineSource getSourceRGB2() {
		return this.sourceRGB2;
	}

	public void setSourceRGB2(CombineSource sourceRGB2) {
		this.sourceRGB2 = sourceRGB2;
	}

	public CombineSource getSourceAlpha0() {
		return this.sourceAlpha0;
	}

	public void setSourceAlpha0(CombineSource sourceAlpha0) {
		this.sourceAlpha0 = sourceAlpha0;
	}

	public CombineSource getSourceAlpha1() {
		return this.sourceAlpha1;
	}

	public void setSourceAlpha1(CombineSource sourceAlpha1) {
		this.sourceAlpha1 = sourceAlpha1;
	}

	public CombineSource getSourceAlpha2() {
		return this.sourceAlpha2;
	}

	public void setSourceAlpha2(CombineSource sourceAlpha2) {
		this.sourceAlpha2 = sourceAlpha2;
	}
	
	public CombineOp getOperandRGB0() {
		return this.operandRGB0;
	}

	public void setOperandRGB0(CombineOp operandRGB0) {
		this.operandRGB0 = operandRGB0;
	}

	public CombineOp getOperandRGB1() {
		return this.operandRGB0;
	}

	public void setOperandRGB1(CombineOp operandRGB1) {
		this.operandRGB1 = operandRGB1;
	}

	public CombineOp getOperandRGB2() {
		return this.operandRGB2;
	}

	public void setOperandRGB2(CombineOp operandRGB2) {
		this.operandRGB2 = operandRGB2;
	}

	public CombineOp getOperandAlpha0() {
		return this.operandAlpha0;
	}

	public void setOperandAlpha0(CombineOp operandAlpha0) {
		this.operandAlpha0 = operandAlpha0;
	}

	public CombineOp getOperandAlpha1() {
		return this.operandAlpha1;
	}

	public void setOperandAlpha1(CombineOp operandAlpha1) {
		this.operandAlpha1 = operandAlpha1;
	}

	public CombineOp getOperandAlpha2() {
		return this.operandAlpha2;
	}

	public void setOperandAlpha2(CombineOp operandAlpha2) {
		this.operandAlpha2 = operandAlpha2;
	}
	
	public void setOperandAlpha(CombineOp a0, CombineOp a1, CombineOp a2) {
		this.setOperandAlpha0(a0);
		this.setOperandAlpha1(a1);
		this.setOperandAlpha2(a2);
	}
	
	public void setOperandRGB(CombineOp r0, CombineOp r1, CombineOp r2) {
		this.setOperandRGB0(r0);
		this.setOperandRGB1(r1);
		this.setOperandRGB2(r2);
	}
	
	public void setSourceAlpha(CombineSource a0, CombineSource a1, CombineSource a2) {
		this.setSourceAlpha0(a0);
		this.setSourceAlpha1(a1);
		this.setSourceAlpha2(a2);
	}
	
	public void setSourceRGB(CombineSource r0, CombineSource r1, CombineSource r2) {
		this.setSourceRGB0(r0);
		this.setSourceRGB1(r1);
		this.setSourceRGB2(r2);
	}

	public EnvMode getTexEnvMode() {
		return this.texEnvMode;
	}

	public void setTexEnvMode(EnvMode texEnvMode) {
		this.texEnvMode = texEnvMode;
	}

	public float[] getTexEnvColor() {
		return this.texEnvColor;
	}

	public void setTexEnvColor(float[] texEnvColor) {
		if (texEnvColor.length != 4)
			throw new IllegalArgumentException("Texture env color must have 4 elements"); 
		this.texEnvColor = texEnvColor;
	}
	
	public AutoTCGen getRCoordGen() {
		return this.rCoordGen;
	}

	public void setRCoordGen(AutoTCGen coordGen) {
		this.rCoordGen = coordGen;
	}

	public AutoTCGen getSCoordGen() {
		return this.sCoordGen;
	}

	public void setSCoordGen(AutoTCGen coordGen) {
		this.sCoordGen = coordGen;
	}

	public AutoTCGen getTCoordGen() {
		return this.tCoordGen;
	}

	public void setTCoordGen(AutoTCGen coordGen) {
		this.tCoordGen = coordGen;
	}
	
	public void setSTRCoordGen(AutoTCGen coordGen) {
		this.setSTRCoordGen(coordGen, coordGen, coordGen);
	}
	
	public void setSTRCoordGen(AutoTCGen s, AutoTCGen t, AutoTCGen r) {
		this.setSCoordGen(s);
		this.setTCoordGen(t);
		this.setRCoordGen(r);
	}
	
	@Override
	public void readChunk(InputChunk in) {
		super.readChunk(in);
		
		this.data = (TextureData)in.getObject("data");
		
		this.combineAlphaFunc = in.getEnum("comAF", CombineAlphaFunc.class);
		this.combineRGBFunc = in.getEnum("comRGBF", CombineRGBFunc.class);
		this.operandAlpha0 = in.getEnum("opA0", CombineOp.class);
		this.operandAlpha1 = in.getEnum("opA1", CombineOp.class);
		this.operandAlpha2 = in.getEnum("opA2", CombineOp.class);
		this.operandRGB0 = in.getEnum("opRGB0", CombineOp.class);
		this.operandRGB1 = in.getEnum("opRGB1", CombineOp.class);
		this.operandRGB2 = in.getEnum("opRGB2", CombineOp.class);
		this.sourceAlpha0 = in.getEnum("srcA0", CombineSource.class);
		this.sourceAlpha1 = in.getEnum("srcA1", CombineSource.class);
		this.sourceAlpha2 = in.getEnum("srcA2", CombineSource.class);
		this.sourceRGB0 = in.getEnum("srcRGB0", CombineSource.class);
		this.sourceRGB1 = in.getEnum("srcRGB1", CombineSource.class);
		this.sourceRGB2 = in.getEnum("srcRGB2", CombineSource.class);
		
		this.planeR = in.getFloatArray("planeR");
		this.planeS = in.getFloatArray("planeS");
		this.planeT = in.getFloatArray("planeT");
		
		this.rCoordGen = in.getEnum("rGen", AutoTCGen.class);
		this.sCoordGen = in.getEnum("sGen", AutoTCGen.class);
		this.tCoordGen = in.getEnum("tGen", AutoTCGen.class);
		
		this.texEnvColor = in.getFloatArray("color");
		this.texEnvMode = in.getEnum("envmode", EnvMode.class);
		this.texTrans = (Transform)in.getObject("trans");
	}
	
	@Override
	public void writeChunk(OutputChunk out) {
		super.writeChunk(out);
		
		out.setObject("data", this.data);
		
		out.setEnum("comAF", this.combineAlphaFunc);
		out.setEnum("comRGBF", this.combineRGBFunc);
		out.setEnum("opA0", this.operandAlpha0);
		out.setEnum("opA1", this.operandAlpha1);
		out.setEnum("opA2", this.operandAlpha2);
		out.setEnum("opRGB0", this.operandRGB0);
		out.setEnum("opRGB1", this.operandRGB1);
		out.setEnum("opRGB2", this.operandRGB2);
		out.setEnum("srcA0", this.sourceAlpha0);
		out.setEnum("srcA1", this.sourceAlpha1);
		out.setEnum("srcA2", this.sourceAlpha2);
		out.setEnum("srcRGB0", this.sourceRGB0);
		out.setEnum("srcRGB1", this.sourceRGB1);
		out.setEnum("srcRGB2", this.sourceRGB2);
		
		out.setFloatArray("planeR", this.planeR);
		out.setFloatArray("planeS", this.planeS);
		out.setFloatArray("planeT", this.planeT);
		
		out.setEnum("rGen", this.rCoordGen);
		out.setEnum("sGen", this.sCoordGen);
		out.setEnum("tGen", this.tCoordGen);
		
		out.setFloatArray("color", this.texEnvColor);
		out.setEnum("envmode", this.texEnvMode);
		out.setObject("trans", this.texTrans);
	}

	@Override
	public boolean isValidUnit(StateUnit unit) {
		if (!(unit instanceof NumericUnit))
			return false;
		if (TextureManager.getMaxTextureUnits() >= 0 && 
			TextureManager.getMaxTextureUnits() <= unit.ordinal())
			return false;
		return true;
	}
}
