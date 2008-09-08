package com.ferox.core.scene.states;

import com.ferox.core.scene.SpatialLeaf;
import com.ferox.core.scene.SpatialState;
import com.ferox.core.states.NullUnit;
import com.ferox.core.states.Quality;
import com.ferox.core.states.StateUnit;
import com.ferox.core.util.io.InputChunk;
import com.ferox.core.util.io.OutputChunk;

/**
 * Stores all of the information to use fog in a rendering system.  Attach to a FogManager (which can then
 * be added to the state tree ) to have render atoms affected by this Fog object.
 * @author Michael Ludwig
 *
 */
public class Fog extends SpatialState {
	public static enum FogFunction {
		EXP, EXP_SQUARED, LINEAR
	}

	private static final StateUnit[] units = new StateUnit[] {NullUnit.get()};
	
	private float[] color;
	
	private float density;
	private float start;
	private float end;
	private FogFunction eq;
	private Quality qual;
	
	/**
	 * Same as Fog(new float[] {.4, .4, .4, 1}, 1, .5, 6, FOG_LINEAR).
	 */
	public Fog() {
		this(new float[] {.4f, .4f, .4f, 1f}, 1f, 0f, 1f, FogFunction.EXP);
	}
	
	/**
	 * Same as Fog(color, density, start, end, equation, QUALITY_NICEST).
	 */
	public Fog(float[] color, float density, float start, float end, FogFunction equation) {
		this(color, density, start, end, equation, Quality.NICEST);
	}
	
	/**
	 * Creates a fog atom with the given color, density, start distance, end distance, equation, and
	 * quality.  1 implies full fog density.  if a vertex is closer than start distance, it is 
	 * un-fogged, if it is further than end distance, it is fully fogged.
	 */
	public Fog(float[] color, float density, float start, float end, FogFunction equation, Quality quality) {
		super();
		
		this.setFogColor(color);
		this.setDensity(density);
		this.setFogEquation(equation);
		this.setFogStart(start);
		this.setFogEnd(end);
		this.setQuality(quality);
	}

	/**
	 * Get the density for this fog.
	 */
	public float getDensity() {
		return this.density;
	}

	/**
	 * Get the 4-element array for the fog color.
	 */
	public float[] getFogColor() {
		return this.color;
	}
	
	/**
	 * Get the far end fog distance.
	 */
	public float getFogEnd() {
		return this.end;
	}
	
	/**
	 * Get the fog equation that calculates intermediate fog values between start and end depths.
	 */
	public FogFunction getFogEquation() {
		return this.eq;
	}
	
	/**
	 * Get the near fog start distance.
	 */
	public float getFogStart() {
		return this.start;
	}

	/**
	 * Get the quality of the fog algorithm for the fog.
	 */
	public Quality getQuality() {
		return this.qual;
	}

	/**
	 * Set the density for this fog.
	 */
	public void setDensity(float density) {
		this.density = Math.max(0, density);
	}

	/**
	 * Set the 4-element array for the fog color, throws an exception if the color.length != 4.
	 */
	public void setFogColor(float[] color) throws IllegalArgumentException {
		if (color == null || color.length != 4)
			throw new IllegalArgumentException("Fog color must have 4 elements to it");
		this.color = color;
	}

	/**
	 * Set the far end fog distance.
	 */
	public void setFogEnd(float end) {
		this.end = Math.max(0, end);
	}

	/**
	 * Set the fog equation that calculates intermediate fog values between start and end depths.
	 */
	public void setFogEquation(FogFunction eq) {
		this.eq = eq;
	}

	/**
	 * Set the near fog start distance.
	 */
	public void setFogStart(float start) {
		this.start = Math.max(start, 0);
	}

	/**
	 * Set the quality of the fog algorithm for the fog.
	 */
	public void setQuality(Quality qual) {
		this.qual = qual;
	}
	
	@Override
	public void readChunk(InputChunk in) {
		super.readChunk(in);
		
		this.color = in.getFloatArray("color");
		
		float[] temp = in.getFloatArray("params");
		this.density = temp[0];
		this.end = temp[1];
		this.start = temp[2];
		
		this.eq = in.getEnum("eq", FogFunction.class);
		this.qual = in.getEnum("qual", Quality.class);
	}
	
	@Override
	public void writeChunk(OutputChunk out) {
		super.writeChunk(out);
		
		out.setFloatArray("color", this.color);
		float[] temp = new float[] {this.density, this.end, this.start};
		out.setFloatArray("params", temp);
		out.setEnum("eq", this.eq);
		out.setEnum("qual", this.qual);
	}

	@Override
	public Class<Fog> getAtomType() {
		return Fog.class;
	}

	@Override
	public boolean isValidUnit(StateUnit unit) {
		return unit instanceof NullUnit;
	}

	@Override
	public StateUnit[] availableUnits() {
		return units;
	}

	@Override
	public void updateSpatial() {
		// do nothing
	}

	@Override
	public float getInfluence(SpatialLeaf leaf) {
		return this.density;
	}
}
