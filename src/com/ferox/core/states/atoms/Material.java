package com.ferox.core.states.atoms;

import com.ferox.core.states.NullUnit;
import com.ferox.core.states.StateAtom;
import com.ferox.core.states.StateUnit;
import com.ferox.core.util.io.InputChunk;
import com.ferox.core.util.io.OutputChunk;

public class Material extends StateAtom {	
	private float[] amb;
	private float[] diff;
	private float[] spec;
	private float shininess;
	
	/**
	 * Creates a Material with white diffuse color, white specular color, and a shininess of 5.
	 */
	public Material() {
		this(new float[] {1f, 1f, 1f, 1f});
	}
	
	/**
	 * Creates a material with the given diffuse color, white specular color and shininess of 5.
	 */
	public Material(float[] diff) {
		this(diff, new float[] {1f, 1f, 1f, 1f}, 5);
	}

	/**
	 * Creates a Material with the given diffuse and specular colors and the given shininess.
	 */
	public Material(float[] diff, float[] spec, float shininess) {
		super();
		this.amb = new float[] {.2f, .2f, .2f, 1f};

		this.setDiffuseColor(diff);
		this.setSpecularColor(spec);
		this.setShininess(shininess);
	}
	
	/**
	 * Get the diffuse color, changes to the returned array will affect the material's properties,
	 * however to guarantee efficient material state modification, setDiffuseColor() should still
	 * be called.
	 */
	public float[] getDiffuseColor() {
		return this.diff;
	}

	/**
	 * Get the shininess of the material, higher value means shinier.
	 */
	public float getShininess() {
		return this.shininess;
	}

	/**
	 * Get the specular color, changes to the returned array will affect the material's properties,
	 * however to guarantee efficient material state modification, setSpecularColor() should still
	 * be called.
	 */
	public float[] getSpecularColor() {
		return this.spec;
	}
	
	public float[] getAmbientColor() {
		return this.amb;
	}
	
	/**
	 * Sets the diffuse color to the given array, also updates an internal hash to speed up
	 * material equality checks.  The array must be of length 4.
	 */
	public void setDiffuseColor(float[] diff) throws IllegalArgumentException {
		if (diff == null || diff.length != 4)
			throw new IllegalArgumentException("Diffuse color must have 4 elements to it");
		this.diff = diff;
	}

	public void setAmbientColor(float[] amb) throws IllegalArgumentException {
		if (amb == null || amb.length != 4)
			throw new IllegalArgumentException("Ambient color must have 4 elements to it");
		this.amb = amb;
	}
	
	/**
	 * Sets the shininess value, negative numbers give undefined results or opengl errors.
	 */
	public void setShininess(float shininess) {
		this.shininess = Math.max(0, shininess);
	}

	/**
	 * Sets the specular color to the given array, also updates an internal hash to speed up
	 * material equality checks.  The array must be of length 4.
	 */
	public void setSpecularColor(float[] spec) throws IllegalArgumentException {
		if (spec == null || spec.length != 4) 
			throw new IllegalArgumentException("Specular color must have 4 elements to it");
		this.spec = spec;
	}
	
	@Override
	public Class<Material> getAtomType() {
		return Material.class;
	}
	
	@Override
	public void readChunk(InputChunk in) {
		super.readChunk(in);
		
		this.diff = in.getFloatArray("diff");
		this.spec = in.getFloatArray("spec");
		this.amb = in.getFloatArray("amb");
		this.shininess = in.getFloat("shininess");
	}
	
	@Override
	public void writeChunk(OutputChunk out) {
		super.writeChunk(out);
		
		out.set("diff", this.diff);
		out.set("spec", this.spec);
		out.set("amb", this.amb);
		out.set("shininess", this.shininess);
	}

	@Override
	public boolean isValidUnit(StateUnit unit) {
		return unit instanceof NullUnit;
	}
}
