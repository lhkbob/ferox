package com.ferox.state;

import com.ferox.math.Color;

/** 
 * In order for lighting to be applied to a scene element,
 * it must have a light receiver state added to its appearance.
 * 
 * If not, it will be rendered as solid colors even if it intersects
 * a light node in the scene.  Light receiver allows for the control
 * of the specular highlight computation (separate and/or from a local
 * viewpoint) for more accuracy at the expense of speed.  Also controls
 * the global ambient light applied to a scene element.
 * 
 * @author Michael Ludwig
 *
 */
public class LightReceiver implements State {
	private static final Color DEFAULT_AMBIENT = new Color(0f, 0f, 0f, 1f);
	
	private boolean separateSpec;
	private boolean localViewer;
	private boolean useTwoSidedLighting;
	private Color globalAmb;
	
	private Object renderData;
	
	/** Creates a LightReceiver that doesn't use separate specular or local viewing,
	 * and uses the default global ambient color, and no two-sided lighting. */
	public LightReceiver() {
		this(null);
	}
	
	/** Creates a LightReceiver that uses the given color for ambient lighting,
	 * with no local viewing, separate specular or two-sided lighting. */
	public LightReceiver(Color globalAmbient) {
		this(null, false, false, false);
	}
	
	/** Creates a LightReceiver with the given parameters. 
	 * If globalAmbient is null, uses the default. */
	public LightReceiver(Color globalAmbient, boolean separateSpecular, boolean useLocalViewer, boolean twoSided) {
		this.setSeparateSpecular(separateSpecular);
		this.setLocalViewer(useLocalViewer);
		this.setTwoSidedLighting(twoSided);
		this.setGlobalAmbient(globalAmbient);
	}
	
	/** Set whether or not the back face of a rendered primitive should
	 * have its lighting parameters computed separately.  This can be 
	 * slower, but look nicer.  It should be set to false if the back face
	 * isn't ever showing since it'll be wasted effort then. */
	public void setTwoSidedLighting(boolean use) {
		this.useTwoSidedLighting = use;
	}
	
	/** Return true if two sided lighting should be enabled when
	 * rendering.  This might be slower, but will produce more realistic
	 * results for geometries that have their back face showing. */
	public boolean getTwoSidedLighting() {
		return this.useTwoSidedLighting;
	}
	
	/** Whether or not specular highlights are computed in a second computation.
	 * True is slower, but provides better realism. Default is false. */
	public boolean getSeparateSpecular() {
		return this.separateSpec;
	}

	/** Set whether or not to compute specular separate from other lighting 
	 * computations.  True is slower and more accurate.  Default is false. */
	public void setSeparateSpecular(boolean separateSpec) {
		this.separateSpec = separateSpec;
	}

	/** Whether or not specular lighting is computed from a local viewer.
	 * True is slower, but more realistic. */
	public boolean isLocalViewer() {
		return localViewer;
	}

	/** Set whether or not specular lighting is computed using a local
	 * viewer, or one that is infinitely far away (changes in view won't 
	 * change specular highlight).  True is slower, but nicer looking.
	 * Default is false. */
	public void setLocalViewer(boolean localViewer) {
		this.localViewer = localViewer;
	}

	/** Get the global ambient light applied to anything rendered.
	 * This ambient light is applied in addition to any intersecting lights,
	 * and will be present even if nothing lights the scene element. */
	public Color getGlobalAmbient() {
		return globalAmb;
	}

	/** Set the global ambient light color.  If null, uses th default,
	 * <0, 0, 0, 1> */
	public void setGlobalAmbient(Color globalAmb) {
		if (globalAmb == null)
			globalAmb = new Color(DEFAULT_AMBIENT);
		this.globalAmb = globalAmb;
	}

	@Override
	public Role getRole() {
		return Role.GLOBAL_LIGHTING;
	}
	
	@Override
	public Object getStateData() {
		return this.renderData;
	}
	
	@Override
	public void setStateData(Object data) {
		this.renderData = data;
	}
}
