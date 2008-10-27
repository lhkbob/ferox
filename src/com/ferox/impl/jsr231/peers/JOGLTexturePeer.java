package com.ferox.impl.jsr231.peers;

import java.nio.FloatBuffer;
import java.util.Arrays;

import javax.media.opengl.GL;

import com.ferox.core.states.*;
import com.ferox.core.states.atoms.Texture;
import com.ferox.core.states.atoms.Texture2D;
import com.ferox.core.states.atoms.TextureData;
import com.ferox.core.states.atoms.Texture.*;
import com.ferox.core.states.manager.TextureManager;
import com.ferox.core.util.BufferUtil;
import com.ferox.core.util.FeroxException;
import com.ferox.impl.jsr231.JOGLRenderContext;

public class JOGLTexturePeer extends SimplePeer<Texture, NoRecord> {
	int texUnit;
	private FloatBuffer matrix = BufferUtil.newFloatBuffer(16);
	
	public JOGLTexturePeer(JOGLRenderContext context) {
		super(context);
		this.texUnit = 0;
		this.matrix = BufferUtil.newFloatBuffer(16);
	}
	
	static int getGLTarget(TextureData data) {
		if (data == null)
			return -1;
		switch(data.getTarget()) {
		case TEX2D:
			if (((Texture2D)data).getWidth() == ((Texture2D)data).getHeight())
				return GL.GL_TEXTURE_2D;
			else
				return GL.GL_TEXTURE_RECTANGLE_ARB;
		case TEX3D:
			return GL.GL_TEXTURE_3D;
		case CUBEMAP:
			return GL.GL_TEXTURE_CUBE_MAP;
		default:
			throw new FeroxException("Unsupported TextureData target");
		}
	}
	
	private static int getGLTexEnvMode(EnvMode mode) {
		switch(mode) {
		case REPLACE:
			return GL.GL_REPLACE;
		case DECAL:
			return GL.GL_DECAL;
		case MODULATE:
			return GL.GL_MODULATE;
		case BLEND:
			return GL.GL_BLEND;
		case COMBINE:
			return GL.GL_COMBINE;
		default:
			throw new FeroxException("Unsupported Texture env mode");
		}
	}
	
	private static int getGLTexGen(AutoTCGen gen) {
		switch(gen) {
		case EYE:
			return GL.GL_EYE_LINEAR;
		case OBJECT:
			return GL.GL_OBJECT_LINEAR;
		case SPHERE:
			return GL.GL_SPHERE_MAP;
		case REFLECTION:
			return GL.GL_REFLECTION_MAP;
		case NORMAL:
			return GL.GL_NORMAL_MAP;
		case NONE:
			return 0;
		default:
			throw new FeroxException("Invalid tex coord gen requested");
		}
	}
	
	private static int getCombineRGBFunc(CombineRGBFunc func) {
		switch(func) {
		case ADD:
			return GL.GL_ADD;
		case ADD_SIGNED:
			return GL.GL_ADD_SIGNED;
		case DOT3_RGB:
			return GL.GL_DOT3_RGB;
		case DOT3_RGBA:
			return GL.GL_DOT3_RGBA;
		case INTERPOLATE:
			return GL.GL_INTERPOLATE;
		case MODULATE:
			return GL.GL_MODULATE;
		case REPLACE:
			return GL.GL_REPLACE;
		case SUBTRACT:
			return GL.GL_SUBTRACT;
		default:
			throw new FeroxException("Invalid combine rgb function");
		}
	}
	
	private static int getCombineAlphaFunc(CombineAlphaFunc func) {
		switch(func) {
		case ADD:
			return GL.GL_ADD;
		case ADD_SIGNED:
			return GL.GL_ADD_SIGNED;
		case INTERPOLATE:
			return GL.GL_INTERPOLATE;
		case MODULATE:
			return GL.GL_MODULATE;
		case REPLACE:
			return GL.GL_REPLACE;
		case SUBTRACT:
			return GL.GL_SUBTRACT;
		default:
			throw new FeroxException("Invalid combine rgb function");
		}
	}
	
	private static int getCombineOp(CombineOp op) {
		switch(op) {
		case ALPHA:
			return GL.GL_SRC_ALPHA;
		case COLOR:
			return GL.GL_SRC_COLOR;
		case ONE_MINUS_ALPHA:
			return GL.GL_ONE_MINUS_SRC_ALPHA;
		case ONE_MINUS_COLOR:
			return GL.GL_ONE_MINUS_SRC_COLOR;
		default:
			throw new FeroxException("Invalid combine operation");
		}
	}
	
	private static int getCombineSrc(CombineSource src) {
		switch(src) {
		case BLEND_COLOR:
			return GL.GL_CONSTANT;
		case CURR_TEX:
			return GL.GL_TEXTURE;
		case PREV_TEX:
			return GL.GL_PREVIOUS;
		case VERTEX_COLOR:
			return GL.GL_PRIMARY_COLOR;
		case TEX0:
			return GL.GL_TEXTURE0;
		case TEX1:
			return GL.GL_TEXTURE1;
		case TEX2:
			return GL.GL_TEXTURE2;
		case TEX3:
			return GL.GL_TEXTURE3;
		case TEX4:
			return GL.GL_TEXTURE4;
		case TEX5:
			return GL.GL_TEXTURE5;
		case TEX6:
			return GL.GL_TEXTURE6;
		case TEX7:
			return GL.GL_TEXTURE7;
		case TEX8:
			return GL.GL_TEXTURE8;
		case TEX9:
			return GL.GL_TEXTURE9;
		case TEX10:
			return GL.GL_TEXTURE10;
		case TEX11:
			return GL.GL_TEXTURE11;
		case TEX12:
			return GL.GL_TEXTURE12;
		case TEX13:
			return GL.GL_TEXTURE13;
		case TEX14:
			return GL.GL_TEXTURE14;
		case TEX15:
			return GL.GL_TEXTURE15;
		case TEX16:
			return GL.GL_TEXTURE16;
		case TEX17:
			return GL.GL_TEXTURE17;
		case TEX18:
			return GL.GL_TEXTURE18;
		case TEX19:
			return GL.GL_TEXTURE19;
		case TEX20:
			return GL.GL_TEXTURE20;
		case TEX21:
			return GL.GL_TEXTURE21;
		case TEX22:
			return GL.GL_TEXTURE22;
		case TEX23:
			return GL.GL_TEXTURE23;
		case TEX24:
			return GL.GL_TEXTURE24;
		case TEX25:
			return GL.GL_TEXTURE25;
		case TEX26:
			return GL.GL_TEXTURE26;
		case TEX27:
			return GL.GL_TEXTURE27;
		case TEX28:
			return GL.GL_TEXTURE28;
		case TEX29:
			return GL.GL_TEXTURE29;
		case TEX30:
			return GL.GL_TEXTURE30;
		case TEX31:
			return GL.GL_TEXTURE31;
		default:
			throw new FeroxException("Invalid combine source");
		}
	}
	
	protected void applyState(Texture prevA, NoRecord prevR, Texture nextA, NoRecord nextR, GL gl) {
		int prevTarget = (prevA == null ? -1 : getGLTarget(prevA.getData()));
		int target = getGLTarget(nextA.getData());
		
		if (prevTarget != target) {
			if (prevTarget != -1) {
				gl.glDisable(prevTarget);
			}
			if (target != -1) {
				gl.glEnable(target);
			} else {
				this.restoreState(prevA, prevR);
				return;
			}
		}
		
		if (prevA == null || prevA.getData() != nextA.getData()) {
			//System.out.println("Bind texture: " + nextA.getData() + " " + this.texUnit);
			nextA.getData().applyState(this.context.getRenderManager(), NumericUnit.get(this.texUnit));
		}
		
		if (prevTarget == -1) {
			// no previous texture on this unit
			if (this.texUnit < TextureManager.getMaxFixedFunctionTextureUnits()) {
				gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, getGLTexEnvMode(nextA.getTexEnvMode()));
				if (nextA.getTexEnvMode() == EnvMode.BLEND || nextA.getTexEnvMode() == EnvMode.COMBINE)
					gl.glTexEnvfv(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_COLOR, nextA.getTexEnvColor(), 0);
				if (nextA.getTexEnvMode() == EnvMode.COMBINE) 
					this.modifyCombineStateFull(gl, nextA);
			}
			
			if (nextA.getRCoordGen() != AutoTCGen.NONE) {
				gl.glEnable(GL.GL_TEXTURE_GEN_R);
				gl.glTexGeni(GL.GL_R, GL.GL_TEXTURE_GEN_MODE, getGLTexGen(nextA.getRCoordGen()));
				if (nextA.getRCoordGen() == AutoTCGen.EYE)
					gl.glTexGenfv(GL.GL_R, GL.GL_EYE_PLANE, nextA.getTexCoordGenPlaneR(), 0);
				else if (nextA.getRCoordGen() == AutoTCGen.OBJECT)
					gl.glTexGenfv(GL.GL_R, GL.GL_OBJECT_PLANE, nextA.getTexCoordGenPlaneR(), 0);
			}
			if (nextA.getSCoordGen() != AutoTCGen.NONE) {
				gl.glEnable(GL.GL_TEXTURE_GEN_S);
				gl.glTexGeni(GL.GL_S, GL.GL_TEXTURE_GEN_MODE, getGLTexGen(nextA.getSCoordGen()));
				if (nextA.getSCoordGen() == AutoTCGen.EYE)
					gl.glTexGenfv(GL.GL_S, GL.GL_EYE_PLANE, nextA.getTexCoordGenPlaneS(), 0);
				else if (nextA.getSCoordGen() == AutoTCGen.OBJECT)
					gl.glTexGenfv(GL.GL_S, GL.GL_OBJECT_PLANE, nextA.getTexCoordGenPlaneS(), 0);
			}
			if (nextA.getTCoordGen() != AutoTCGen.NONE) {
				gl.glEnable(GL.GL_TEXTURE_GEN_T);
				gl.glTexGeni(GL.GL_T, GL.GL_TEXTURE_GEN_MODE, getGLTexGen(nextA.getTCoordGen()));
				if (nextA.getTCoordGen() == AutoTCGen.EYE)
					gl.glTexGenfv(GL.GL_T, GL.GL_EYE_PLANE, nextA.getTexCoordGenPlaneT(), 0);
				else if (nextA.getTCoordGen() == AutoTCGen.OBJECT)
					gl.glTexGenfv(GL.GL_T, GL.GL_OBJECT_PLANE, nextA.getTexCoordGenPlaneT(), 0);
			}
			
			if (nextA.getTexCoordTransform() != null) {
				nextA.getTexCoordTransform().getOpenGLMatrix((FloatBuffer) this.matrix.rewind());
				gl.glMatrixMode(GL.GL_TEXTURE_MATRIX);
				gl.glLoadMatrixf((FloatBuffer) this.matrix.rewind());
				gl.glMatrixMode(GL.GL_MODELVIEW);
			}
		} else {
			if (this.texUnit < TextureManager.getMaxFixedFunctionTextureUnits()) {
				if (nextA.getTexEnvMode() != prevA.getTexEnvMode())
					gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, getGLTexEnvMode(nextA.getTexEnvMode()));
			
				if (nextA.getTexEnvMode() == EnvMode.BLEND || nextA.getTexEnvMode() == EnvMode.COMBINE) {
					float[] nC = nextA.getTexEnvColor();
					float[] pC = prevA.getTexEnvColor();
					if ((prevA.getTexEnvMode() != EnvMode.BLEND && prevA.getTexEnvMode() != EnvMode.COMBINE) 
						|| !Arrays.equals(nC, pC))
						gl.glTexEnvfv(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_COLOR, nC, 0);
				} 
				if (nextA.getTexEnvMode() == EnvMode.COMBINE) {
					if (prevA.getTexEnvMode() == EnvMode.COMBINE) 
						this.modifyCombineStatePartial(gl, prevA, nextA);
					else 
						this.modifyCombineStateFull(gl, nextA);
				}
			}
			
			if (nextA.getRCoordGen() != prevA.getRCoordGen()) {
				if (nextA.getRCoordGen() == AutoTCGen.NONE)
					gl.glDisable(GL.GL_TEXTURE_GEN_R);
				else {
					gl.glEnable(GL.GL_TEXTURE_GEN_R);
					gl.glTexGeni(GL.GL_R, GL.GL_TEXTURE_GEN_MODE, getGLTexGen(nextA.getRCoordGen()));
				}
			}
			if (nextA.getRCoordGen() == AutoTCGen.EYE || nextA.getRCoordGen() == AutoTCGen.OBJECT) {
				float[] p1 = nextA.getTexCoordGenPlaneR();
				float[] p2 = prevA.getTexCoordGenPlaneR();
				if (prevA.getRCoordGen() != nextA.getRCoordGen() || !Arrays.equals(p1, p2))
					gl.glTexGenfv(GL.GL_R, (nextA.getRCoordGen() == AutoTCGen.EYE ? GL.GL_EYE_PLANE : GL.GL_OBJECT_PLANE), p1, 0);
			}
			
			if (nextA.getSCoordGen() != prevA.getSCoordGen()) {
				if (nextA.getSCoordGen() == AutoTCGen.NONE)
					gl.glDisable(GL.GL_TEXTURE_GEN_S);
				else {
					gl.glEnable(GL.GL_TEXTURE_GEN_S);
					gl.glTexGeni(GL.GL_S, GL.GL_TEXTURE_GEN_MODE, getGLTexGen(nextA.getSCoordGen()));
				}
			}
			if (nextA.getSCoordGen() == AutoTCGen.EYE || nextA.getSCoordGen() == AutoTCGen.OBJECT) {
				float[] p1 = nextA.getTexCoordGenPlaneS();
				float[] p2 = prevA.getTexCoordGenPlaneS();
				if (prevA.getSCoordGen() != nextA.getSCoordGen() || !Arrays.equals(p1, p2))
					gl.glTexGenfv(GL.GL_S, (nextA.getRCoordGen() == AutoTCGen.EYE ? GL.GL_EYE_PLANE : GL.GL_OBJECT_PLANE), p1, 0);
			}
			
			if (nextA.getTCoordGen() != prevA.getTCoordGen()) {
				if (nextA.getTCoordGen() == AutoTCGen.NONE)
					gl.glDisable(GL.GL_TEXTURE_GEN_T);
				else {
					gl.glEnable(GL.GL_TEXTURE_GEN_T);
					gl.glTexGeni(GL.GL_T, GL.GL_TEXTURE_GEN_MODE, getGLTexGen(nextA.getTCoordGen()));
				}
			}
			if (nextA.getTCoordGen() == AutoTCGen.EYE || nextA.getTCoordGen() == AutoTCGen.OBJECT) {
				float[] p1 = nextA.getTexCoordGenPlaneT();
				float[] p2 = prevA.getTexCoordGenPlaneT();
				if (prevA.getTCoordGen() != nextA.getTCoordGen() || (p1[0] != p2[0] && p1[1] != p2[1] && p1[2] != p2[2] && p1[3] != p2[3]))
					gl.glTexGenfv(GL.GL_T, (nextA.getRCoordGen() == AutoTCGen.EYE ? GL.GL_EYE_PLANE : GL.GL_OBJECT_PLANE), p1, 0);
			}
			
			if (nextA.getTexCoordTransform() != prevA.getTexCoordTransform()) {
				if (nextA.getTexCoordTransform() != null) {
					nextA.getTexCoordTransform().getOpenGLMatrix((FloatBuffer) this.matrix.rewind());
					gl.glMatrixMode(GL.GL_TEXTURE_MATRIX);
					gl.glLoadMatrixf((FloatBuffer) this.matrix.rewind());
					gl.glMatrixMode(GL.GL_MODELVIEW);
				} else {
					gl.glMatrixMode(GL.GL_TEXTURE_MATRIX);
					gl.glLoadIdentity();
					gl.glMatrixMode(GL.GL_MODELVIEW);
				}
			} 
		}
	}
	
	private void modifyCombineStateFull(GL gl, Texture tex) {
		gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_COMBINE_ALPHA, getCombineAlphaFunc(tex.getCombineAlphaFunc()));
		gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_COMBINE_RGB, getCombineRGBFunc(tex.getCombineRGBFunc()));
		
		gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_SOURCE0_RGB, getCombineSrc(tex.getSourceRGB0()));
		gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_SOURCE0_ALPHA, getCombineSrc(tex.getSourceAlpha0()));
		gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_SOURCE1_RGB, getCombineSrc(tex.getSourceRGB1()));
		gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_SOURCE1_ALPHA, getCombineSrc(tex.getSourceAlpha1()));
		gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_SOURCE2_RGB, getCombineSrc(tex.getSourceRGB2()));
		gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_SOURCE2_ALPHA, getCombineSrc(tex.getSourceAlpha2()));
		
		gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_OPERAND0_RGB, getCombineOp(tex.getOperandRGB0()));
		gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_OPERAND0_ALPHA, getCombineOp(tex.getOperandAlpha0()));
		gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_OPERAND1_RGB, getCombineOp(tex.getOperandRGB1()));
		gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_OPERAND1_ALPHA,  getCombineOp(tex.getOperandAlpha1()));
		gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_OPERAND2_RGB, getCombineOp(tex.getOperandRGB2()));
		gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_OPERAND2_ALPHA,  getCombineOp(tex.getOperandAlpha2()));
	}
	
	private void modifyCombineStatePartial(GL gl, Texture prev, Texture next) {
		if (next.getCombineAlphaFunc() != prev.getCombineAlphaFunc())
			gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_COMBINE_ALPHA, getCombineAlphaFunc(next.getCombineAlphaFunc()));
		if (next.getCombineRGBFunc() != prev.getCombineRGBFunc())
			gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_COMBINE_RGB, getCombineRGBFunc(next.getCombineRGBFunc()));
		
		if (next.getSourceAlpha0() != prev.getSourceAlpha0())
			gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_SOURCE0_ALPHA, getCombineSrc(next.getSourceAlpha0()));
		if (next.getSourceAlpha1() != prev.getSourceAlpha1())
			gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_SOURCE1_ALPHA, getCombineSrc(next.getSourceAlpha1()));
		if (next.getSourceAlpha2() != prev.getSourceAlpha2())
			gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_SOURCE2_ALPHA, getCombineSrc(next.getSourceAlpha2()));
		
		if (next.getSourceRGB0() != prev.getSourceRGB0())
			gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_SOURCE0_RGB, getCombineSrc(next.getSourceRGB0()));
		if (next.getSourceRGB1() != prev.getSourceRGB1())
			gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_SOURCE1_RGB, getCombineSrc(next.getSourceRGB1()));
		if (next.getSourceRGB2() != prev.getSourceRGB2())
			gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_SOURCE2_RGB, getCombineSrc(next.getSourceRGB2()));
		
		if (next.getOperandAlpha0() != prev.getOperandAlpha0())
			gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_OPERAND0_ALPHA, getCombineOp(next.getOperandAlpha0()));
		if (next.getOperandAlpha1() != prev.getOperandAlpha1())
			gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_OPERAND1_ALPHA, getCombineOp(next.getOperandAlpha1()));
		if (next.getOperandAlpha2() != prev.getOperandAlpha2())
			gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_OPERAND2_ALPHA, getCombineOp(next.getOperandAlpha2()));
		
		if (next.getOperandRGB0() != prev.getOperandRGB0())
			gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_OPERAND0_RGB, getCombineOp(next.getOperandRGB0()));
		if (next.getOperandRGB1() != prev.getOperandRGB1())
			gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_OPERAND1_RGB, getCombineOp(next.getOperandRGB1()));
		if (next.getOperandRGB2() != prev.getOperandRGB2())
			gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_OPERAND2_RGB, getCombineOp(next.getOperandRGB2()));
	}

	protected void restoreState(Texture cleanA, NoRecord cleanR, GL gl) {
		if (cleanA == null || cleanA.getData() == null)
			return;
		int t = getGLTarget(cleanA.getData());
		
		gl.glDisable(t);
		cleanA.getData().restoreState(this.context.getRenderManager(), NumericUnit.get(this.texUnit));
		
		if (cleanA.getRCoordGen() != AutoTCGen.NONE)
			gl.glDisable(GL.GL_TEXTURE_GEN_R);
		if (cleanA.getSCoordGen() != AutoTCGen.NONE)
			gl.glDisable(GL.GL_TEXTURE_GEN_S);
		if (cleanA.getTCoordGen() != AutoTCGen.NONE)
			gl.glDisable(GL.GL_TEXTURE_GEN_T);
		
		if (cleanA.getTexCoordTransform() != null) {
			gl.glMatrixMode(GL.GL_TEXTURE_MATRIX);
			gl.glLoadIdentity();
			gl.glMatrixMode(GL.GL_MODELVIEW);
		}
	}

	public void prepareManager(StateManager manager, StateManager previous) {
		this.prepareManager((TextureManager)manager, (TextureManager)previous);
	}
	
	private void prepareManager(TextureManager manager, TextureManager previous) {
		if (previous == null || manager.getTextureFilterHint() != previous.getTextureFilterHint())
			this.context.getGL().glHint(GL.GL_PERSPECTIVE_CORRECTION_HINT, JOGLRenderContext.getGLQuality(manager.getTextureFilterHint()));
	}

	public void setUnit(StateUnit unit) {
		this.texUnit = ((NumericUnit)unit).ordinal();
		this.context.getGL().glActiveTexture(GL.GL_TEXTURE0 + this.texUnit);
	}
}
