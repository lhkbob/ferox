package com.ferox.scene;

import org.openmali.vecmath.Vector3f;

import com.ferox.math.BoundVolume;
import com.ferox.math.Color;
import com.ferox.renderer.InfluenceAtom;
import com.ferox.renderer.RenderAtom;
import com.ferox.renderer.RenderQueue;
import com.ferox.renderer.View;
import com.ferox.renderer.util.RenderQueueDataCache;
import com.ferox.state.State;

/** Represents a light within a scene.  Any shape that intersects this light's bounds will be affected by 
 * the light, assuming that the shape has a state attached to it that enables it to receive light.  
 * It is not recommended to directly attach light objects to Appearances, although it will still work.  The 
 * light requires a world transform in order to influence objects correctly, so unless you update the world
 * transform by hand, it is much easier to attach it to a scene.  Also, appearances only support one state
 * per type, but by using influence atoms, it is possible to have multiple lights affect the same scene element.
 * 
 * Default diffuse color is <.8, .8, .8, 1>, ambient is <.2, .2, .2, 1>, specular is <1, 1, 1, 1> and
 * the default direction is <0, 0, 1>.
 * 
 * All colors are stored as references, any changes to the color objects will be reflected in the rendering
 * and other lights/materials using the color objects.
 * 
 * @author Michael Ludwig
 *
 */
public abstract class Light extends Leaf implements State, InfluenceAtom {
	private static final Color DEFAULT_AMBIENT = new Color(.2f, .2f, .2f);
	private static final Color DEFAULT_DIFFUSE = new Color(.8f, .8f, .8f);
	private static final Color DEFAULT_SPEC = new Color(1f, 1f, 1f);
	
	
	private Color ambient;
	private Color diffuse;
	private Color specular;
	
	private Vector3f direction;
	
	private RenderQueueDataCache cache;
	private Object renderData;
	
	/** Create a light with default ambient, diffuse, specular colors and default direction. */
	public Light() {
		this(null);
	}
	
	/** Create a light with default ambient, specular colors and direction, and the given diffuse color. */
	public Light(Color diffuse) {
		this(diffuse, null, null);
	}
	
	/** Create a light with the given colors and the default direction. */
	public Light(Color diffuse, Color specular, Color ambient) {
		this(diffuse, specular, ambient, null);
	}
	
	/** Create a light with the given colors, shining in the given direction.  If any input are null, the default
	 * value is assumed. */
	public Light(Color diffuse, Color specular, Color ambient, Vector3f direction) {
		this.cache = new RenderQueueDataCache();
		this.setAmbient(ambient);
		this.setDiffuse(diffuse);
		this.setSpecular(specular);
		this.setDirection(direction);
	}
	
	/** Get the ambient color of this light. */
	public Color getAmbient() {
		return this.ambient;
	}

	/** Set the ambient color of this light.  If ambient is null, sets the color to be the default. */
	public void setAmbient(Color ambient) {
		if (ambient == null)
			ambient = new Color(DEFAULT_AMBIENT);
		this.ambient = ambient;
	}

	/** Get the diffuse color of this light. */
	public Color getDiffuse() {
		return this.diffuse;
	}

	/** Set the diffuse color of this light.  If diffuse is null, sets the color to be the default. */
	public void setDiffuse(Color diffuse) {
		if (diffuse == null)
			diffuse = new Color(DEFAULT_DIFFUSE);
		this.diffuse = diffuse;
	}

	/** Get the specular color of this light. */
	public Color getSpecular() {
		return this.specular;
	}

	/** Set the specular color of this light.  If specular is null, sets the color to be the default. */
	public void setSpecular(Color specular) {
		if (specular == null)
			specular = new Color(DEFAULT_SPEC);
		this.specular = specular;
	}

	/** Get the direction that this light is shining, in local space.  In some cases, e.g. a spot light with 180 degree
	 * spotlight arc (a point light), the direction of the light is meaningless. */
	public Vector3f getDirection() {
		return this.direction;
	}

	/** Set the direction that this light is shining, in local space.  Has no visible effect when called on a point light, since
	 * the light shines everywhere, however the vector is still stored.  If direction is null, it is set to the default. */
	public void setDirection(Vector3f direction) {
		if (direction == null)
			direction = new Vector3f(0f, 0f, 1f);
		this.direction = direction;
	}

	@Override
	protected BoundVolume adjustLocalBounds(BoundVolume local) {
		return local;
	}

	/** Subclasses will all be of type Light. */
	@Override
	public final Role getRole() {
		return Role.LIGHTING;
	}
	
	@Override
	public VisitResult visit(RenderQueue RenderQueue, View view, VisitResult parentResult) {
		VisitResult sp = super.visit(RenderQueue, view, parentResult);
		if (sp != VisitResult.FAIL)
			RenderQueue.add(this);
		return sp;
	}

	@Override
	public Object getRenderQueueData(RenderQueue pipe) {
		return this.cache.getRenderQueueData(pipe);
	}

	@Override
	public State getState() {
		return this;
	}

	@Override
	public float influences(RenderAtom atom) {
		if (atom == null)
			return 0f;
		BoundVolume volume = atom.getBounds();
		if (volume == null)
			return .5f;
		else if (!volume.intersects(this.worldBounds))
			return 0f;
		
		float colorValue = Math.max(.8f * getColorValue(this.ambient), Math.max(.6f * getColorValue(this.diffuse), .4f * getColorValue(this.specular)));
		return Math.max(0f, Math.min(colorValue, 1f));
	}
	
	private static float getColorValue(Color color) {
		return color.getRed() + color.getGreen() + color.getBlue() + color.getAlpha();
	}

	@Override
	public void setRenderQueueData(RenderQueue pipe, Object data) {
		this.cache.setRenderQueueData(pipe, data);
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
