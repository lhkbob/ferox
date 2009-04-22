package com.ferox.renderer.impl.jogl.drivers;

import java.util.Arrays;
import java.util.List;

import javax.media.opengl.GL;

import com.ferox.math.Color;
import com.ferox.math.Plane;
import com.ferox.math.Transform;
import com.ferox.renderer.RenderException;
import com.ferox.renderer.impl.AbstractRenderer;
import com.ferox.renderer.impl.StateDriver;
import com.ferox.renderer.impl.jogl.EnumUtil;
import com.ferox.renderer.impl.jogl.JoglSurfaceFactory;
import com.ferox.renderer.impl.jogl.record.JoglStateRecord;
import com.ferox.renderer.impl.jogl.record.TextureRecord;
import com.ferox.renderer.impl.jogl.record.TextureRecord.TextureGenRecord;
import com.ferox.renderer.impl.jogl.record.TextureRecord.TextureUnit;
import com.ferox.resource.UnitList.Unit;
import com.ferox.state.MultiTexture;
import com.ferox.state.State;
import com.ferox.state.Texture;
import com.ferox.state.Texture.CombineAlpha;
import com.ferox.state.Texture.CombineOperand;
import com.ferox.state.Texture.CombineRgb;
import com.ferox.state.Texture.CombineSource;
import com.ferox.state.Texture.EnvMode;
import com.ferox.state.Texture.TexCoordGen;

/** JoglTextureStateDriver provides support for the TEXTURE role
 * and can use instances of Texture and MultiTexture.  When using
 * a Texture, it is emulated as a MultiTexture with its 0th unit set to
 * the desired texture.
 * 
 * This driver assumes the following convention is used for texturing:
 *  - when rendering, only one texture is bound to a unit, and only a single
 *    target of the unit is used at one time.  If the unit is disabled, then
 *    all its bindings are set to 0.
 *  - if something else binds textures to perform an operation (e.g. PbufferDelegate,
 *    or the TextureImage resource drivers), they must make sure that that convention
 *    is still followed after they are finished.
 * 
 * @author Michael Ludwig
 *
 */
public class JoglTextureStateDriver implements StateDriver {
	private static final float[] DEF_PLANE_S = {1f, 0f, 0f, 0f};
	private static final float[] DEF_PLANE_T = {0f, 1f, 0f, 0f};
	private static final float[] DEF_PLANE_RQ = {0f, 0f, 0f, 0f};
	
	private Texture defaultEnv;
	
	private MultiTexture lastApplied;
	private MultiTexture queuedTexture;
	private float queuedInfluence;
	
	private MultiTexture singleUnit; // used to store a Texture that's in lastApplied or queuedTexture
	private boolean lastAppliedDirty; // will be true if lastApplied == singleUnit && queuedTexture == singleUnit && singleUnit.0th is changed
		
	private JoglSurfaceFactory factory;
	
	public JoglTextureStateDriver(JoglSurfaceFactory factory) {
		this.factory = factory;
		this.singleUnit = new MultiTexture();
		
		this.lastApplied = null;
		this.queuedTexture = null;
		this.lastAppliedDirty = false;
		this.queuedInfluence = -1f;		
		
		this.defaultEnv = new Texture();
		
		this.defaultEnv.setTextureEnvMode(EnvMode.MODULATE);
		this.defaultEnv.setTextureEnvColor(new Color(0f, 0f, 0f, 0f));
		
		this.defaultEnv.setTextureTransform(null);
		this.defaultEnv.setTexCoordGenSTR(TexCoordGen.NONE);
		
		this.defaultEnv.setCombineAlphaEquation(CombineAlpha.MODULATE);
		this.defaultEnv.setCombineRgbEquation(CombineRgb.MODULATE);
		this.defaultEnv.setSourceRgb(CombineSource.CURR_TEX, CombineSource.PREV_TEX, CombineSource.BLEND_COLOR);
		this.defaultEnv.setSourceAlpha(CombineSource.CURR_TEX, CombineSource.PREV_TEX, CombineSource.BLEND_COLOR);
		this.defaultEnv.setOperandRgb(CombineOperand.COLOR, CombineOperand.COLOR, CombineOperand.ALPHA);
		this.defaultEnv.setOperandAlpha(CombineOperand.ALPHA, CombineOperand.ALPHA, CombineOperand.ALPHA);
	}
	
	@Override
	public void doApply() {
		// apply the MultiTexture if there were changes
		if (this.queuedTexture != null) {
			if (this.lastAppliedDirty || this.queuedTexture != this.lastApplied)
				this.apply(this.factory.getRenderer(), this.factory.getRecord(), this.queuedTexture);
		} else if (this.lastApplied != null)
			this.apply(this.factory.getRenderer(), this.factory.getRecord(), null);
		
		this.lastApplied = this.queuedTexture;
		this.reset();
	}

	@Override
	public void queueAppearanceState(State state) throws RenderException {
		// like in SingleStateDriver, this is okay
		this.queueInfluenceState(state, 0f);
	}

	@Override
	public void queueInfluenceState(State state, float influence) throws RenderException {
		if (state instanceof Texture) {
			if (influence >= this.queuedInfluence) {
				// we're dirty if we're reusing singleState, and the 0th unit has changed
				this.lastAppliedDirty = this.lastApplied == this.singleUnit && this.singleUnit.getTexture(0) != state;
				
				this.queuedTexture = this.singleUnit;
				this.singleUnit.setTexture(0, (Texture) state);
				this.queuedInfluence = influence;
			}
		} else if (state instanceof MultiTexture) {
			if (influence >= this.queuedInfluence) {
				this.queuedTexture = (MultiTexture) state;
				this.lastAppliedDirty = false;
				this.queuedInfluence = influence;
			}
		} else // not a supported type
			throw new RenderException("This state driver only supports Texture and MultiTexture, not: " + state);
	}

	@Override
	public void reset() {
		this.queuedTexture = null;
		this.queuedInfluence = -1f;
		this.lastAppliedDirty = false;
	}
	
	@Override
	public void restoreDefaults() {
		this.reset();
		this.lastApplied = null;
		
		GL gl = this.factory.getGL();
		TextureRecord tr = this.factory.getRecord().textureRecord;
		
		TextureUnit tu;
		for (int i = 0; i < tr.textureUnits.length; i++) {
			gl.glActiveTexture(GL.GL_TEXTURE0 + i);
			
			tu = tr.textureUnits[i];
			// sets all env except for the combine mode, and texgen
			this.setTexEnv(gl, tu, this.defaultEnv);
			// force combine mode to be set
			setCombineEnv(gl, tu, this.defaultEnv);
			// at this point, texgen is disabled, but must reset other values
			setDefaultTexGen(gl, GL.GL_S, tu.texGenS, DEF_PLANE_S);
			setDefaultTexGen(gl, GL.GL_T, tu.texGenT, DEF_PLANE_T);
			setDefaultTexGen(gl, GL.GL_R, tu.texGenR, DEF_PLANE_RQ);
			
			// unbind the texture
			bindTexture(gl, tu, -1, 0);
		}
		gl.glActiveTexture(GL.GL_TEXTURE0);
		tr.activeTexture = 0;
	}
	
	/* Modify the given context so that its TextureRecord matches the given MultiTexture.
	 * If next is null, all texturing will be disabled instead. */
	private void apply(AbstractRenderer renderer, JoglStateRecord record, MultiTexture next) {
		GL gl = this.factory.getGL();
		TextureRecord tr = record.textureRecord;
		int activeTex = tr.activeTexture;
		
		List<Unit<Texture>> toApply = (next == null ? null : next.getTextures());
		List<Unit<Texture>> toRestore = (this.lastApplied == null ? null : this.lastApplied.getTextures());
		
		int numT;
		int unit;
		Unit<Texture> tex;
		
		if (toApply != null) {
			// bind all textures in the next texture
			numT = toApply.size();
			for (int i = 0; i < numT; i++) {
				tex = toApply.get(i);
				unit = tex.getUnit();
				// must make sure it's a valid unit, and that it's a different Texture instance
				if (unit < tr.textureUnits.length &&
					(this.lastApplied == null || this.lastAppliedDirty || this.lastApplied.getTexture(unit) != tex.getData())) {
					// bind the texture
					activeTex = this.applyTexture(gl, renderer, activeTex, unit, tr.textureUnits[unit], tex.getData());
				}
			}
		}
		
		if (toRestore != null) {
			// unbind all textures in the previous texture that weren't overridden by toApply
			numT = toRestore.size();
			for (int i = 0; i < numT; i++) {
				tex = toRestore.get(i);
				unit = tex.getUnit();
				// must make sure it's a valid unit, and that it wasn't just bound above
				if (unit < tr.textureUnits.length
				    && (toApply == null || next.getTexture(unit) == null)) {
					// unbind the texture by passing in null
					activeTex = this.applyTexture(gl, renderer, activeTex, unit, tr.textureUnits[unit], null);
				}
			}
		}
		// save the active texture unit back into the record
		tr.activeTexture = activeTex;
	}
	
	/* Binds the given texture to the desiredUnit, and configures the texture environment.  Returns the new active unit.
	 * If texture is null, the texture bindings on the desiredUnit are broken. */
	private int applyTexture(GL gl, AbstractRenderer renderer, int activeUnit, int desiredUnit, TextureUnit tu, Texture next) {
		TextureHandle nextH = (next == null ? null : (TextureHandle) renderer.getHandle(next.getTexture()));
		// set the texture unit
		if (activeUnit != desiredUnit) {
			gl.glActiveTexture(GL.GL_TEXTURE0 + desiredUnit);
			activeUnit = desiredUnit;
		}

		if (nextH == null) {
			// disable the texture
			bindTexture(gl, tu, -1, 0); // unbind the bound object
			// disable tex-gen, too
			setTexGen(gl, GL.GL_S, GL.GL_TEXTURE_GEN_S, tu.texGenS, TexCoordGen.NONE, null);
			setTexGen(gl, GL.GL_T, GL.GL_TEXTURE_GEN_T, tu.texGenT, TexCoordGen.NONE, null);
			setTexGen(gl, GL.GL_R, GL.GL_TEXTURE_GEN_R, tu.texGenR, TexCoordGen.NONE, null);
		} else {
			// enable the texture
			bindTexture(gl, tu, nextH.glTarget, nextH.id);
			// set the environment
			this.setTexEnv(gl, tu, next);
		}
		
		return activeUnit;
	}
	
	/* Make the necessary changes to the active unit based off of the non-null Texture instance. 
	 * unitValue should be the active unit - GL_TEXTURE0. */
	private void setTexEnv(GL gl, TextureUnit unit, Texture tex) {
		// texture transform
		Transform t = tex.getTextureTransform();
		if (t == null) {
			if (!unit.isTextureMatrixIdentity) {
				gl.glMatrixMode(GL.GL_TEXTURE_MATRIX);
				gl.glLoadIdentity();
				gl.glMatrixMode(GL.GL_MODELVIEW);
				
				unit.isTextureMatrixIdentity = true;
			}
		} else {
			gl.glMatrixMode(GL.GL_TEXTURE_MATRIX);
			this.factory.getTransformDriver().loadMatrix(gl, t);
			gl.glMatrixMode(GL.GL_MODELVIEW);
			unit.isTextureMatrixIdentity = false;
		}
		// tc_s
		setTexGen(gl, GL.GL_S, GL.GL_TEXTURE_GEN_S, unit.texGenS, tex.getTexCoordGenS(), tex.getTexCoordGenPlaneS());
		// tc_t
		setTexGen(gl, GL.GL_T, GL.GL_TEXTURE_GEN_T, unit.texGenT, tex.getTexCoordGenT(), tex.getTexCoordGenPlaneT());
		// tc_r
		setTexGen(gl, GL.GL_R, GL.GL_TEXTURE_GEN_R, unit.texGenR, tex.getTexCoordGenR(), tex.getTexCoordGenPlaneR());
		
		// env color
		Color blend = tex.getTextureEnvColor();
		if (!blend.equals(unit.textureEnvColor)) {
			blend.get(unit.textureEnvColor);
			gl.glTexEnvfv(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_COLOR, unit.textureEnvColor, 0);
		}
		// env mode
		int envMode = EnumUtil.getGLTexEnvMode(tex.getTextureEnvMode());
		if (envMode != unit.textureEnvMode) {
			unit.textureEnvMode = envMode;
			gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, envMode);
		}
		// combine state if needed
		if (envMode == GL.GL_COMBINE)
			setCombineEnv(gl, unit, tex);
	}
	
	/* Bind the given id to the active unit.  Pass in a -1 as glTarget to unbind the
	 * active texture (in which case id is ignored).  If glTarget is valid, it assumes
	 * id is a valid non-zero texture object. */
	private static void bindTexture(GL gl, TextureUnit unit, int glTarget, int id) {
		// unbind and disable old target
		if (unit.enableTarget && glTarget != unit.enabledTarget) {
			gl.glBindTexture(unit.enabledTarget, 0);
			gl.glDisable(unit.enabledTarget);
			
			unit.enabledTarget = -1;
			unit.texBinding = 0;
			unit.enableTarget = false;
		}
		
		if (glTarget > 0 && id > 0) {
			if (!unit.enableTarget) {
				gl.glEnable(glTarget);
				unit.enabledTarget = glTarget;
				unit.enableTarget = true;
			}
			if (unit.texBinding != id) {
				gl.glBindTexture(glTarget, id);
				unit.texBinding = id;
			}
		}
	}
	
	/* Set the defaults for the given record, it changes the mode to eye linear,
	 * and sets the object and eye planes to the given array. */
	private static void setDefaultTexGen(GL gl, int coord, TextureGenRecord tgr, float[] plane) {
		if (tgr.textureGenMode != GL.GL_EYE_LINEAR) {
			tgr.textureGenMode = GL.GL_EYE_LINEAR;
			gl.glTexGeni(coord, GL.GL_TEXTURE_GEN_MODE, GL.GL_EYE_LINEAR);
		}
		
		if (!Arrays.equals(tgr.eyePlane, plane)) {
			System.arraycopy(plane, 0, tgr.eyePlane, 0, plane.length);
			gl.glTexGenfv(coord, GL.GL_EYE_PLANE, plane, 0);
		}
		
		if (!Arrays.equals(tgr.objectPlane, plane)) {
			System.arraycopy(plane, 0, tgr.objectPlane, 0, plane.length);
			gl.glTexGenfv(coord, GL.GL_OBJECT_PLANE, plane, 0);
		}
	}
	
	/* Set the texture gen for the given coordinate.  coord should be one of
	 * GL_S/T/R, and boolMode would then be GL_TEXTURE_GEN_x, and tgr is the
	 * matching record.  If genMode is NONE, generation is disabled, otherwise
	 * its enabled and set, possibly resetting the texture plane. */
	private static void setTexGen(GL gl, int coord, int boolMode, TextureGenRecord tgr, TexCoordGen genMode, Plane eyeOrObject) {
		if (genMode == TexCoordGen.NONE) {
			// disable coordinate generation for this coord
			if (tgr.enableTexGen) {
				tgr.enableTexGen = false;
				gl.glDisable(boolMode);
			}
		} else {
			// possibly set the planes
			if (genMode == TexCoordGen.EYE) {
				// always push the eye-plane through
				eyeOrObject.get(tgr.eyePlane);
				gl.glTexGenfv(coord, GL.GL_EYE_PLANE, tgr.eyePlane, 0);
			} else if (genMode == TexCoordGen.OBJECT) {
				if (!eyeOrObject.equals(tgr.objectPlane)) {
					eyeOrObject.get(tgr.objectPlane);
					gl.glTexGenfv(coord, GL.GL_OBJECT_PLANE, tgr.objectPlane, 0);
				}
			}
			// set the mode
			int mode = EnumUtil.getGLTexGen(genMode);
			if (mode != tgr.textureGenMode) {
				gl.glTexGeni(coord, GL.GL_TEXTURE_GEN_MODE, mode);
				tgr.textureGenMode = mode;
			}
			// enable it
			if (!tgr.enableTexGen) {
				gl.glEnable(boolMode);
				tgr.enableTexGen = true;
			}
		}
	}
	
	/* Set all state necessary for the GL_COMBINE environment mode. */
	private static void setCombineEnv(GL gl, TextureUnit tu, Texture tex) {
		// combine alpha
		int c = EnumUtil.getGLCombineAlphaFunc(tex.getCombineAlphaEquation());
		if (c != tu.combineAlpha) {
			tu.combineAlpha = c;
			gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_COMBINE_ALPHA, c);
		}
		// combine rgb
		c = EnumUtil.getGLCombineRGBFunc(tex.getCombineRgbEquation());
		if (c != tu.combineRgb) {
			tu.combineRgb = c;
			gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_COMBINE_RGB, c);
		}
		
		// combine source alpha 0
		c = EnumUtil.getGLCombineSrc(tex.getSourceAlpha0());
		if (c != tu.src0Alpha) {
			tu.src0Alpha = c;
			gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_SOURCE0_ALPHA, c);
		}
		// combine source alpha 1
		c = EnumUtil.getGLCombineSrc(tex.getSourceAlpha1());
		if (c != tu.src1Alpha) {
			tu.src1Alpha = c;
			gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_SOURCE1_ALPHA, c);
		}
		// combine source alpha 2
		c = EnumUtil.getGLCombineSrc(tex.getSourceAlpha2());
		if (c != tu.src2Alpha) {
			tu.src2Alpha = c;
			gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_SOURCE2_ALPHA, c);
		}
		
		// combine source rgb 0
		c = EnumUtil.getGLCombineSrc(tex.getSourceRgb0());
		if (c != tu.src0Rgb) {
			tu.src0Rgb = c;
			gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_SOURCE0_RGB, c);
		}
		// combine source rgb 1
		c = EnumUtil.getGLCombineSrc(tex.getSourceRgb1());
		if (c != tu.src1Rgb) {
			tu.src1Rgb = c;
			gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_SOURCE1_RGB, c);
		}
		// combine source rgb 2
		c = EnumUtil.getGLCombineSrc(tex.getSourceRgb2());
		if (c != tu.src2Rgb) {
			tu.src2Rgb = c;
			gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_SOURCE2_RGB, c);
		}
		
		// combine operand alpha 0
		c = EnumUtil.getGLCombineOp(tex.getOperandAlpha0());
		if (c != tu.operand0Alpha) {
			tu.operand0Alpha = c;
			gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_OPERAND0_ALPHA, c);
		}
		// combine operand alpha 1
		c = EnumUtil.getGLCombineOp(tex.getOperandAlpha1());
		if (c != tu.operand1Alpha) {
			tu.operand1Alpha = c;
			gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_OPERAND1_ALPHA, c);
		}
		// combine operand alpha 2
		c = EnumUtil.getGLCombineOp(tex.getOperandAlpha2());
		if (c != tu.operand2Alpha) {
			tu.operand2Alpha = c;
			gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_OPERAND2_ALPHA, c);
		}
		
		// combine operand rgb 0
		c = EnumUtil.getGLCombineOp(tex.getOperandRgb0());
		if (c != tu.operand0Rgb) {
			tu.operand0Rgb = c;
			gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_OPERAND0_RGB, c);
		}
		// combine operand rgb 1
		c = EnumUtil.getGLCombineOp(tex.getOperandRgb1());
		if (c != tu.operand1Rgb) {
			tu.operand1Rgb = c;
			gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_OPERAND1_RGB, c);
		}
		// combine operand rgb 2
		c = EnumUtil.getGLCombineOp(tex.getOperandRgb2());
		if (c != tu.operand2Rgb) {
			tu.operand2Rgb = c;
			gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_OPERAND2_RGB, c);
		}
	}
}
