package com.ferox.core.scene.states;

import org.openmali.vecmath.Vector3f;

import com.ferox.core.scene.InfluenceLeaf;
import com.ferox.core.scene.SpatialLeaf;
import com.ferox.core.scene.SpatialState;
import com.ferox.core.scene.Transform;
import com.ferox.core.states.NumericUnit;
import com.ferox.core.states.StateUnit;
import com.ferox.core.states.manager.LightManager;
import com.ferox.core.util.io.InputChunk;
import com.ferox.core.util.io.OutputChunk;

/**
 * Represents the diffuse and specular colorings of a light, use the subclasses SpotLight and DirectionLight
 * to actually apply lights to a scene.
 * @author Michael Ludwig
 *
 */
public abstract class Light extends SpatialState {
	private static StateUnit[] lightUnits = null;
	
	private float[] diff;
	private float[] spec;
	private float[] amb;
	private Vector3f lightDirection;
	private final Transform worldTransform;
	
	/**
	 * Creates a white light.
	 */
	public Light() {
		this(new float[] {1f, 1f, 1f, 1f});
	}
	
	/**
	 * Creates a light with the given diffuse color and a white specular color.
	 */
	public Light(float[] diff) {
		this(diff, new float[] {1f, 1f, 1f, 1f});
	}
	
	/**
	 * Creates a light with the given diffuse color and specular color.
	 */
	public Light(float[] diff, float[] spec) {
		super();
			
		this.amb = new float[] {.2f, .2f, .2f, 1f};
		this.worldTransform = new Transform();
		
		this.setDiffuseColor(diff);
		this.setSpecularColor(spec);
		this.setLightDirection(null);
	}
	
	public Vector3f getLightDirection() {
		return this.lightDirection;
	}

	public void setLightDirection(Vector3f lightDirection) {
		if (lightDirection == null || lightDirection.lengthSquared() < .0001f)
			lightDirection = new Vector3f(0f, 0f, 1f);
		this.lightDirection = lightDirection;
		this.lightDirection.normalize();
	}

	public Transform getWorldTransform() {
		return this.worldTransform;
	}

	/**
	 * Get the diffuse color, modifications to this array are reflected in the light.
	 */
	public float[] getDiffuseColor() {
		return this.diff;
	}
	
	/**
	 * Get the specular color, modifications to this array are reflected in the light.
	 */
	public float[] getSpecularColor() {
		return this.spec;
	}
	
	public float[] getAmbientColor() {
		return this.amb;
	}
	
	/**
	 * Sets the diffuse color, can't be null and must be of length 4.
	 */
	public void setDiffuseColor(float[] diff) throws IllegalArgumentException {
		if (diff == null || diff.length != 4) {
			throw new IllegalArgumentException("Diffuse color must have 4 elements to it");
		}
		this.diff = diff;
	}
	
	public void setAmbientColor(float[] amb) throws IllegalArgumentException {
		if (amb == null || amb.length != 4) {
			throw new IllegalArgumentException("Ambient color must have 4 elements to it");
		}
		this.amb = amb;
	}
	
	/**
	 * Sets the specular color, can't be null and must be of length 4.
	 */
	public void setSpecularColor(float[] spec) throws IllegalArgumentException {
		if (spec == null || spec.length != 4) {
			throw new IllegalArgumentException("Specular color must have 4 elements to it");
		}
		this.spec = spec;
	}
	
	@Override
	public StateUnit[] availableUnits() {
		if (LightManager.getMaxNumLights() < 0)
			return null;
		if (lightUnits == null) {
			lightUnits = new StateUnit[LightManager.getMaxNumLights()];
			for (int i = 0; i < lightUnits.length; i++)
				lightUnits[i] = NumericUnit.get(i);
		}
		return lightUnits;
	}

	@Override
	public void updateSpatial() {
		InfluenceLeaf leaf = this.getInfluenceLeaf();
		this.worldTransform.set(leaf.getWorldTransform());
	}
	
	public float getInfluence(SpatialLeaf leaf) {
		//TODO: implement
		return 0;
	}
	
	@Override
	public Class<Light> getAtomType() {
		return Light.class;
	}
	
	@Override
	public boolean isValidUnit(StateUnit unit) {
		if (unit == null || !(unit instanceof NumericUnit))
			return false;
		if (LightManager.getMaxNumLights() >= 0 && 
			LightManager.getMaxNumLights() <= unit.ordinal())
			return false;
		return true;
	}
	
	@Override
	public void readChunk(InputChunk in) {
		this.diff = in.getFloatArray("diffuse");
		this.spec = in.getFloatArray("specular");
		this.amb = in.getFloatArray("ambient");
		this.lightDirection.set(in.getFloatArray("direction"));
	}
	
	@Override
	public void writeChunk(OutputChunk out) {
		out.setFloatArray("diffuse", this.diff);
		out.setFloatArray("specular", spec);
		out.setFloatArray("ambient", this.amb);
		out.setFloatArray("direction", new float[] {this.lightDirection.x, this.lightDirection.y, this.lightDirection.z});
	}
}
