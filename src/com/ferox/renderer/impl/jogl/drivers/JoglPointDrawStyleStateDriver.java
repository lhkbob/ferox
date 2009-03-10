package com.ferox.renderer.impl.jogl.drivers;

import javax.media.opengl.GL;

import org.openmali.vecmath.Vector3f;

import com.ferox.renderer.impl.jogl.JoglContext;
import com.ferox.renderer.impl.jogl.JoglSurfaceFactory;
import com.ferox.renderer.impl.jogl.record.RasterizationRecord;
import com.ferox.renderer.impl.jogl.record.TextureRecord;
import com.ferox.renderer.impl.jogl.record.TextureRecord.TextureUnit;
import com.ferox.state.PointStyle;
import com.ferox.state.PointStyle.PointSpriteOrigin;

/** This state driver provides an implementation for the POINT_DRAW_STYLE
 * role by relying on PointStyle instances.
 * 
 * @author Michael Ludwig
 *
 */
public class JoglPointDrawStyleStateDriver extends SingleStateDriver<PointStyle> {
	private boolean glslSupport;
	private boolean pointSpriteSupport;
	
	public JoglPointDrawStyleStateDriver(JoglSurfaceFactory factory) {
		super(new PointStyle(), PointStyle.class, factory);
		this.glslSupport = factory.getRenderer().getCapabilities().getGlslSupport();
		this.pointSpriteSupport = factory.getRenderer().getCapabilities().getPointSpriteSupport();
	}

	@Override
	protected void apply(GL gl, JoglContext context, PointStyle nextState) {
		RasterizationRecord rr = context.getStateRecord().rasterRecord;
		
		// size
		if (rr.pointSize != nextState.getPointSize()) {
			rr.pointSize = nextState.getPointSize();
			gl.glPointSize(rr.pointSize);
		}
		// distance atten.
		Vector3f at = nextState.getDistanceAttenuation();
		if (at.x != rr.pointDistanceAttenuation[0] || at.y != rr.pointDistanceAttenuation[1] ||
		    at.z != rr.pointDistanceAttenuation[2]) {
			at.get(rr.pointDistanceAttenuation);
			gl.glPointParameterfv(GL.GL_POINT_DISTANCE_ATTENUATION, rr.pointDistanceAttenuation, 0);
		}
		// maximum
		if (nextState.getPointSizeMax() != rr.pointSizeMax) {
			rr.pointSizeMax = nextState.getPointSizeMax();
			gl.glPointParameterf(GL.GL_POINT_SIZE_MAX, rr.pointSizeMax);
		}
		// minimum and fade threshold
		float min = nextState.getPointSizeMin();
		if (min != rr.pointSizeMin) {
			rr.pointSizeMin = min;
			gl.glPointParameterf(GL.GL_POINT_SIZE_MIN, min);
		}
		if (min != rr.pointFadeThresholdSize) {
			rr.pointFadeThresholdSize = min;
			gl.glPointParameterf(GL.GL_POINT_FADE_THRESHOLD_SIZE, min);
		}
		// smoothing
		if (nextState.isSmoothingEnabled() != rr.enablePointSmooth) {
			rr.enablePointSmooth = nextState.isSmoothingEnabled();
			if (rr.enablePointSmooth)
				gl.glEnable(GL.GL_POINT_SMOOTH);
			else
				gl.glDisable(GL.GL_POINT_SMOOTH);
		}
		// vertex shader
		if (this.glslSupport && nextState.isVertexShaderSizingEnabled() != rr.enableVertexShaderSize) {
			// only do this if glsl is supported
			rr.enableVertexShaderSize = nextState.isVertexShaderSizingEnabled();
			if (rr.enableVertexShaderSize)
				gl.glEnable(GL.GL_VERTEX_PROGRAM_POINT_SIZE);
			else
				gl.glDisable(GL.GL_VERTEX_PROGRAM_POINT_SIZE);
		}
		// point sprites
		if (this.pointSpriteSupport) {
			// only do this if point sprites are supported
			TextureRecord tr = context.getStateRecord().textureRecord;
			if (nextState.isPointSpriteEnabled()) {
				// must enable it
				if (!rr.enablePointSprite) {
					rr.enablePointSprite = true;
					gl.glEnable(GL.GL_POINT_SPRITE_ARB);
				}
				setPointSpriteOrigin(gl, rr, nextState.getPointSpriteOrigin());
				int unit = nextState.getPointSpriteTextureUnit();
				if (unit > tr.textureUnits.length)
					unit = tr.textureUnits.length;
				enableCoordReplace(gl, tr, unit);
			} else {
				if (rr.enablePointSprite) {
					rr.enablePointSprite = false;
					gl.glDisable(GL.GL_POINT_SPRITE_ARB);
				}
				enableCoordReplace(gl, tr, -1);
			}
		}
	}
	
	/* Enables coord replace on the given unit and disables it on every other one. 
	 * If enableUnit doesn't equal a valid texture unit (TEXTURE0 ...), this will disable
	 * all units. */
	private static void enableCoordReplace(GL gl, TextureRecord tr, int enableUnit) {
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
	
	private static int setCoordReplaceEnabled(GL gl, int activeUnit, int desiredUnit, TextureUnit tu, boolean enable) {
		if (tu.enableCoordReplace != enable) {
			if (activeUnit != desiredUnit) {
				gl.glActiveTexture(GL.GL_TEXTURE0 + desiredUnit);
				// update the returned unit
				activeUnit = desiredUnit;
			}
			
			tu.enableCoordReplace = enable;
			gl.glTexEnvi(GL.GL_POINT_SPRITE_ARB, GL.GL_COORD_REPLACE_ARB, (enable ? GL.GL_TRUE : GL.GL_FALSE));
		}
		
		return activeUnit;
	}
	
	private static void setPointSpriteOrigin(GL gl, RasterizationRecord rr, PointSpriteOrigin origin) {
		if (origin == PointSpriteOrigin.UPPER_LEFT) {
			if (rr.pointSpriteOrigin != GL.GL_UPPER_LEFT) {
				rr.pointSpriteOrigin = GL.GL_UPPER_LEFT;
				gl.glPointParameteri(GL.GL_POINT_SPRITE_COORD_ORIGIN, GL.GL_UPPER_LEFT);
			}
		} else {
			if (rr.pointSpriteOrigin != GL.GL_LOWER_LEFT) {
				rr.pointSpriteOrigin = GL.GL_LOWER_LEFT;
				gl.glPointParameteri(GL.GL_POINT_SPRITE_COORD_ORIGIN, GL.GL_LOWER_LEFT);
			}
		}
	}
}
