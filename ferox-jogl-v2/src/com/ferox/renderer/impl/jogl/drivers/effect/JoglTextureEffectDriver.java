package com.ferox.renderer.impl.jogl.drivers.effect;

import java.util.List;

import javax.media.opengl.GL;

import com.ferox.effect.Effect;
import com.ferox.effect.MultiTexture;
import com.ferox.effect.Texture;
import com.ferox.effect.Texture.TexCoordGen;
import com.ferox.math.Color4f;
import com.ferox.math.Plane;
import com.ferox.math.Transform;
import com.ferox.renderer.UnsupportedEffectException;
import com.ferox.renderer.impl.AbstractFramework;
import com.ferox.renderer.impl.EffectDriver;
import com.ferox.renderer.impl.jogl.JoglContextManager;
import com.ferox.renderer.impl.jogl.JoglUtil;
import com.ferox.renderer.impl.jogl.TextureHandle;
import com.ferox.renderer.impl.jogl.record.JoglStateRecord;
import com.ferox.renderer.impl.jogl.record.TextureRecord;
import com.ferox.renderer.impl.jogl.record.TextureRecord.TextureGenRecord;
import com.ferox.renderer.impl.jogl.record.TextureRecord.TextureUnit;
import com.ferox.util.UnitList.Unit;

/**
 * <p>
 * JoglTextureEffectDriver provides support for the TEXTURE role and can use
 * instances of Texture and MultiTexture. When using a Texture, it is emulated
 * as a MultiTexture with its 0th unit set to the desired texture. This driver
 * assumes the following convention is used for texturing:
 * <ul>
 * <li>When rendering, only one texture is bound to a unit, and only a single
 * target of the unit is used at one time. If the unit is disabled, then all its
 * bindings are set to 0.</li>
 * <li>If something else binds textures to perform an operation (e.g.
 * PbufferDelegate, or the TextureImage resource drivers), they must make sure
 * that that convention is still followed after they are finished.</li>
 * </ul>
 * </p>
 * 
 * @author Michael Ludwig
 */
public class JoglTextureEffectDriver implements EffectDriver {
	private MultiTexture lastApplied;
	private MultiTexture queuedTexture;

	// used to store a Texture that's in lastApplied or queuedTexture
	private final MultiTexture singleUnit;
	// will be true if lastApplied == singleUnit && queuedTexture == singleUnit
	// but singleUnit's 0th texture is different than before
	private boolean lastAppliedDirty;

	private final JoglContextManager factory;

	public JoglTextureEffectDriver(JoglContextManager factory) {
		this.factory = factory;
		singleUnit = new MultiTexture();

		lastApplied = null;
		queuedTexture = null;
		lastAppliedDirty = false;
	}

	@Override
	public void doApply() {
		// apply the MultiTexture if there were changes
		if (queuedTexture != null) {
			if (lastAppliedDirty || queuedTexture != lastApplied)
				apply(factory.getFramework(), factory.getRecord(), queuedTexture);
		} else if (lastApplied != null)
			apply(factory.getFramework(), factory.getRecord(), null);

		lastApplied = queuedTexture;
		reset();
	}

	@Override
	public void queueEffect(Effect state) {
		if (state instanceof Texture) {
			// we're dirty if we're reusing singleState, and the 0th unit
			// has changed
			lastAppliedDirty = lastApplied == singleUnit && singleUnit.getTexture(0) != state;

			queuedTexture = singleUnit;
			singleUnit.setTexture(0, (Texture) state);
		} else if (state instanceof MultiTexture) {
			queuedTexture = (MultiTexture) state;
			lastAppliedDirty = false;
		} else
			throw new UnsupportedEffectException("This effect driver only supports Texture and MultiTexture, not: " + state);
	}

	@Override
	public void reset() {
		queuedTexture = null;
		lastAppliedDirty = false;
	}

	/*
	 * Modify the given context so that its TextureRecord matches the given
	 * MultiTexture. If next is null, all texturing will be disabled instead.
	 */
	private void apply(AbstractFramework renderer, JoglStateRecord record, MultiTexture next) {
		GL gl = factory.getGL();
		TextureRecord tr = record.textureRecord;
		int activeTex = tr.activeTexture;

		List<Unit<Texture>> toApply = (next == null ? null : next.getTextures());
		List<Unit<Texture>> toRestore = (lastApplied == null ? null : lastApplied.getTextures());

		int numT;
		int unit;
		Unit<Texture> tex;

		if (toApply != null) {
			// bind all textures in the next texture
			numT = toApply.size();
			for (int i = 0; i < numT; i++) {
				tex = toApply.get(i);
				unit = tex.getUnit();
				// must make sure it's a valid unit, and that it's a different
				// Texture instance
				if (unit < tr.textureUnits.length && (lastApplied == null || 
					lastAppliedDirty || lastApplied.getTexture(unit) != tex.getData()))
					// bind the texture
					activeTex = applyTexture(gl, renderer, activeTex, 
											 unit, tr.textureUnits[unit], tex.getData());
			}
		}

		if (toRestore != null) {
			// unbind all textures in the previous texture that weren't
			// overridden by toApply
			numT = toRestore.size();
			for (int i = 0; i < numT; i++) {
				tex = toRestore.get(i);
				unit = tex.getUnit();
				// must make sure it's a valid unit, and that it wasn't just
				// bound above
				if (unit < tr.textureUnits.length && (toApply == null || next.getTexture(unit) == null))
					// unbind the texture by passing in null
					activeTex = applyTexture(gl, renderer, activeTex, unit, 
											 tr.textureUnits[unit], null);
			}
		}
		// save the active texture unit back into the record
		tr.activeTexture = activeTex;
	}

	/*
	 * Binds the given texture to the desiredUnit, and configures the texture
	 * environment. Returns the new active unit. If texture is null, the texture
	 * bindings on the desiredUnit are broken.
	 */
	private int applyTexture(GL gl, AbstractFramework renderer, int activeUnit, 
							 int desiredUnit, TextureUnit tu, Texture next) {
		TextureHandle nextH = (next == null ? null : (TextureHandle) renderer.getHandle(next.getTexture(), factory));
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
			setTexEnv(gl, tu, next);
		}

		return activeUnit;
	}

	/*
	 * Make the necessary changes to the active unit based off of the non-null
	 * Texture instance. unitValue should be the active unit - GL_TEXTURE0.
	 */
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
			factory.getTransformDriver().loadMatrix(gl, t);
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
		Color4f blend = tex.getTextureEnvColor();
		if (!JoglUtil.equals(blend, unit.textureEnvColor)) {
			JoglUtil.get(blend, unit.textureEnvColor);
			gl.glTexEnvfv(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_COLOR, unit.textureEnvColor, 0);
		}
		// env mode
		int envMode = JoglUtil.getGLTexEnvMode(tex.getTextureEnvMode());
		if (envMode != unit.textureEnvMode) {
			unit.textureEnvMode = envMode;
			gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, envMode);
		}
		// combine state if needed
		if (envMode == GL.GL_COMBINE)
			setCombineEnv(gl, unit, tex);
	}

	/*
	 * Bind the given id to the active unit. Pass in a -1 as glTarget to unbind
	 * the active texture (in which case id is ignored). If glTarget is valid,
	 * it assumes id is a valid non-zero texture object.
	 */
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

	/*
	 * Set the texture gen for the given coordinate. coord should be one of
	 * GL_S/T/R, and boolMode would then be GL_TEXTURE_GEN_x, and tgr is the
	 * matching record. If genMode is NONE, generation is disabled, otherwise
	 * its enabled and set, possibly resetting the texture plane.
	 */
	private static void setTexGen(GL gl, int coord, int boolMode, 
								  TextureGenRecord tgr, TexCoordGen genMode, Plane eyeOrObject) {
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
				JoglUtil.get(eyeOrObject, tgr.eyePlane);
				gl.glTexGenfv(coord, GL.GL_EYE_PLANE, tgr.eyePlane, 0);
			} else if (genMode == TexCoordGen.OBJECT)
				if (!JoglUtil.equals(eyeOrObject, tgr.objectPlane)) {
					JoglUtil.get(eyeOrObject, tgr.objectPlane);
					gl.glTexGenfv(coord, GL.GL_OBJECT_PLANE, tgr.objectPlane, 0);
				}
			// set the mode
			int mode = JoglUtil.getGLTexGen(genMode);
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
		int c = JoglUtil.getGLCombineAlphaFunc(tex.getCombineAlphaEquation());
		if (c != tu.combineAlpha) {
			tu.combineAlpha = c;
			gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_COMBINE_ALPHA, c);
		}
		// combine rgb
		c = JoglUtil.getGLCombineRGBFunc(tex.getCombineRgbEquation());
		if (c != tu.combineRgb) {
			tu.combineRgb = c;
			gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_COMBINE_RGB, c);
		}

		// combine source alpha 0
		c = JoglUtil.getGLCombineSrc(tex.getSourceAlpha0());
		if (c != tu.src0Alpha) {
			tu.src0Alpha = c;
			gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_SOURCE0_ALPHA, c);
		}
		// combine source alpha 1
		c = JoglUtil.getGLCombineSrc(tex.getSourceAlpha1());
		if (c != tu.src1Alpha) {
			tu.src1Alpha = c;
			gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_SOURCE1_ALPHA, c);
		}
		// combine source alpha 2
		c = JoglUtil.getGLCombineSrc(tex.getSourceAlpha2());
		if (c != tu.src2Alpha) {
			tu.src2Alpha = c;
			gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_SOURCE2_ALPHA, c);
		}

		// combine source rgb 0
		c = JoglUtil.getGLCombineSrc(tex.getSourceRgb0());
		if (c != tu.src0Rgb) {
			tu.src0Rgb = c;
			gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_SOURCE0_RGB, c);
		}
		// combine source rgb 1
		c = JoglUtil.getGLCombineSrc(tex.getSourceRgb1());
		if (c != tu.src1Rgb) {
			tu.src1Rgb = c;
			gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_SOURCE1_RGB, c);
		}
		// combine source rgb 2
		c = JoglUtil.getGLCombineSrc(tex.getSourceRgb2());
		if (c != tu.src2Rgb) {
			tu.src2Rgb = c;
			gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_SOURCE2_RGB, c);
		}

		// combine operand alpha 0
		c = JoglUtil.getGLCombineOp(tex.getOperandAlpha0());
		if (c != tu.operand0Alpha) {
			tu.operand0Alpha = c;
			gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_OPERAND0_ALPHA, c);
		}
		// combine operand alpha 1
		c = JoglUtil.getGLCombineOp(tex.getOperandAlpha1());
		if (c != tu.operand1Alpha) {
			tu.operand1Alpha = c;
			gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_OPERAND1_ALPHA, c);
		}
		// combine operand alpha 2
		c = JoglUtil.getGLCombineOp(tex.getOperandAlpha2());
		if (c != tu.operand2Alpha) {
			tu.operand2Alpha = c;
			gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_OPERAND2_ALPHA, c);
		}

		// combine operand rgb 0
		c = JoglUtil.getGLCombineOp(tex.getOperandRgb0());
		if (c != tu.operand0Rgb) {
			tu.operand0Rgb = c;
			gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_OPERAND0_RGB, c);
		}
		// combine operand rgb 1
		c = JoglUtil.getGLCombineOp(tex.getOperandRgb1());
		if (c != tu.operand1Rgb) {
			tu.operand1Rgb = c;
			gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_OPERAND1_RGB, c);
		}
		// combine operand rgb 2
		c = JoglUtil.getGLCombineOp(tex.getOperandRgb2());
		if (c != tu.operand2Rgb) {
			tu.operand2Rgb = c;
			gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_OPERAND2_RGB, c);
		}
	}
}
