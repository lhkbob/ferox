package com.ferox.renderer.impl.jogl.drivers.effect;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2ES1;
import javax.media.opengl.GLBase;

import com.ferox.effect.PointStyle;
import com.ferox.effect.PointStyle.PointSpriteOrigin;
import com.ferox.math.Vector3f;
import com.ferox.renderer.impl.jogl.JoglContextManager;
import com.ferox.renderer.impl.jogl.record.JoglStateRecord;
import com.ferox.renderer.impl.jogl.record.RasterizationRecord;
import com.ferox.renderer.impl.jogl.record.TextureRecord;
import com.ferox.renderer.impl.jogl.record.TextureRecord.TextureUnit;

/**
 * This state driver provides an implementation for the POINT_DRAW_STYLE role by
 * relying on PointStyle instances.
 * 
 * @author Michael Ludwig
 */
public class JoglPointStyleEffectDriver extends SingleEffectDriver<PointStyle, GL2> {

	private final boolean glslSupport;
	private final boolean pointSpriteSupport;

	public JoglPointStyleEffectDriver(JoglContextManager factory) {
		super(new PointStyle(), PointStyle.class, factory);
		glslSupport = factory.getFramework().getCapabilities().getGlslSupport();
		pointSpriteSupport = factory.getFramework().getCapabilities().getPointSpriteSupport();
	}
	
	@Override
	protected GL2 convert(GLBase gl) {
		return gl.getGL2();
	}

	@Override
	protected void apply(GL2 gl, JoglStateRecord record, PointStyle nextState) {
		RasterizationRecord rr = record.rasterRecord;

		setPoint(gl, rr, nextState.getPointSize(), nextState.getDistanceAttenuation(), 
				 nextState.getPointSizeMin(), nextState.getPointSizeMax(), 
				 nextState.isSmoothingEnabled());

		setVertexShaderEnabled(gl, rr, nextState.isVertexShaderSizingEnabled());

		// point sprites
		if (pointSpriteSupport) {
			// only do this if point sprites are supported
			TextureRecord tr = record.textureRecord;
			if (nextState.isPointSpriteEnabled()) {
				// must enable it
				if (!rr.enablePointSprite) {
					rr.enablePointSprite = true;
					gl.glEnable(GL2.GL_POINT_SPRITE);
				}
				setPointSpriteOrigin(gl, rr, nextState.getPointSpriteOrigin());
				int unit = nextState.getPointSpriteTextureUnit();
				if (unit > tr.textureUnits.length)
					unit = tr.textureUnits.length;
				enableCoordReplace(gl, tr, unit);
			} else {
				if (rr.enablePointSprite) {
					rr.enablePointSprite = false;
					gl.glDisable(GL2.GL_POINT_SPRITE);
				}
				enableCoordReplace(gl, tr, -1);
			}
		}
	}

	private void setVertexShaderEnabled(GL gl, RasterizationRecord rr, boolean enable) {
		// vertex shader
		if (glslSupport && enable != rr.enableVertexShaderSize) {
			// only do this if glsl is supported
			rr.enableVertexShaderSize = enable;
			if (rr.enableVertexShaderSize)
				gl.glEnable(GL2.GL_VERTEX_PROGRAM_POINT_SIZE);
			else
				gl.glDisable(GL2.GL_VERTEX_PROGRAM_POINT_SIZE);
		}
	}

	private void setPoint(GL2ES1 gl, RasterizationRecord rr, float pointSize, 
						  Vector3f distance, float min, float max, boolean smoothing) {
		// size
		if (rr.pointSize != pointSize) {
			rr.pointSize = pointSize;
			gl.glPointSize(rr.pointSize);
		}
		// distance atten.
		Vector3f at = distance;
		if (at.x != rr.pointDistanceAttenuation[0] || at.y != rr.pointDistanceAttenuation[1] || 
			at.z != rr.pointDistanceAttenuation[2]) {
			at.get(rr.pointDistanceAttenuation, 0);
			gl.glPointParameterfv(GL2.GL_POINT_DISTANCE_ATTENUATION, rr.pointDistanceAttenuation, 0);
		}
		// maximum
		if (max != rr.pointSizeMax) {
			rr.pointSizeMax = max;
			gl.glPointParameterf(GL2.GL_POINT_SIZE_MAX, rr.pointSizeMax);
		}
		// minimum and fade threshold
		if (min != rr.pointSizeMin) {
			rr.pointSizeMin = min;
			gl.glPointParameterf(GL2.GL_POINT_SIZE_MIN, min);
		}
		if (min != rr.pointFadeThresholdSize) {
			rr.pointFadeThresholdSize = min;
			gl.glPointParameterf(GL2.GL_POINT_FADE_THRESHOLD_SIZE, min);
		}
		// smoothing
		if (smoothing != rr.enablePointSmooth) {
			rr.enablePointSmooth = smoothing;
			if (rr.enablePointSmooth)
				gl.glEnable(GL2.GL_POINT_SMOOTH);
			else
				gl.glDisable(GL2.GL_POINT_SMOOTH);
		}
	}

	/*
	 * Enables coord replace on the given unit and disables it on every other
	 * one. If enableUnit doesn't equal a valid texture unit (TEXTURE0 ...),
	 * this will disable all units.
	 */
	private void enableCoordReplace(GL2ES1 gl, TextureRecord tr, int enableUnit) {
		int activeTex = tr.activeTexture;
		// set the active texture unit first
		TextureUnit tu = tr.textureUnits[activeTex];
		setCoordReplaceEnabled(gl, activeTex, activeTex, tu, activeTex == enableUnit);

		// now make sure all the others are set
		for (int i = 0; i < tr.textureUnits.length; i++) {
			if (i == tr.activeTexture)
				continue; // we set this one earlier
			tu = tr.textureUnits[i];
			activeTex = setCoordReplaceEnabled(gl, activeTex, i, tu, i == enableUnit);
		}
		tr.activeTexture = activeTex;
	}

	private int setCoordReplaceEnabled(GL2ES1 gl, int activeUnit, int desiredUnit, 
						               TextureUnit tu, boolean enable) {
		if (tu.enableCoordReplace != enable) {
			if (activeUnit != desiredUnit) {
				gl.glActiveTexture(GL.GL_TEXTURE0 + desiredUnit);
				// update the returned unit
				activeUnit = desiredUnit;
			}

			tu.enableCoordReplace = enable;
			gl.glTexEnvi(GL2.GL_POINT_SPRITE, GL2.GL_COORD_REPLACE, (enable ? GL.GL_TRUE 
																		    : GL.GL_FALSE));
		}

		return activeUnit;
	}

	private void setPointSpriteOrigin(GL2 gl, RasterizationRecord rr, PointSpriteOrigin origin) {
		if (origin == PointSpriteOrigin.UPPER_LEFT) {
			if (rr.pointSpriteOrigin != GL2.GL_UPPER_LEFT) {
				rr.pointSpriteOrigin = GL2.GL_UPPER_LEFT;
				gl.glPointParameteri(GL2.GL_POINT_SPRITE_COORD_ORIGIN, GL2.GL_UPPER_LEFT);
			}
		} else if (rr.pointSpriteOrigin != GL2.GL_LOWER_LEFT) {
			rr.pointSpriteOrigin = GL2.GL_LOWER_LEFT;
			gl.glPointParameteri(GL2.GL_POINT_SPRITE_COORD_ORIGIN, GL2.GL_LOWER_LEFT);
		}
	}
}
