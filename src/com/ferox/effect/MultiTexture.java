package com.ferox.effect;

import java.util.List;

import com.ferox.util.UnitList;
import com.ferox.util.UnitList.Unit;

/**
 * MultiTexture allows for combining multiple textures onto an Appearance,
 * allowing for multi-texturing and more advanced texture effects (if used in
 * conjunction with ShaderPrograms).
 * 
 * MultiTexture imposes no limit to the number of textures allowed, but most
 * renderers have some limit (depending on mode of operation and hardware). If
 * textures are in units beyond a renderer's limits, then those textures will be
 * ignored when rendering anything.
 * 
 * Realistic limits tend to be between 4 and 16 for modern hardware.
 * 
 * When referring to texture units, units start at unit 0 and increase by 1 for
 * each subsequent unit.
 * 
 * @author Michael Ludwig
 * 
 */
public class MultiTexture extends AbstractEffect {
	private final UnitList<Texture> units;

	/** Create a MultiTexture with no textures currently bound. */
	public MultiTexture() {
		this((Texture) null);
	}

	/**
	 * Create a MultiTexture with the given textures, starting at unit 0, and
	 * increasing by 1. If null values are passed in, they still advance the
	 * unit count for the next texture.
	 * 
	 * @param textures List of textures to use, starting at unit 0
	 */
	public MultiTexture(Texture... textures) {
		units = new UnitList<Texture>();

		for (int i = 0; i < textures.length; i++) {
			setTexture(i, textures[i]);
		}
	}

	/**
	 * Set the texture on the given unit. If texture is null, makes it so that
	 * there is no texture on the given unit.
	 * 
	 * There is a hardware maximum unit, and if a Texture is bound to a unit
	 * above this, it will be ignored.
	 * 
	 * @param unit Texture unit texture is assigned to
	 * @param texture Texture object to use, null breaks old binding
	 * 
	 * @throws IllegalArgumentException if unit < 0
	 */
	public void setTexture(int unit, Texture texture)
					throws IllegalArgumentException {
		units.setItem(unit, texture);
	}

	/**
	 * Get the texture currently set for the given unit. A return value of null
	 * signifies that there is no texture bound to that unit.
	 * 
	 * @param unit Texture unit to query bound texture
	 * @return Texture bound to unit, null means no binding
	 * 
	 * @throws IllegalArgumentException if unit < 0
	 */
	public Texture getTexture(int unit) throws IllegalArgumentException {
		return units.getItem(unit);
	}

	/**
	 * Get an unmodifiable list of all the textures on this MultiTexture object.
	 * 
	 * For each TextureUnit in the list, the following will hold:
	 * getTexture(tu.getUnit()) == tu.getTexture(). tu.getTexture() will not be
	 * null.
	 * 
	 * Note: the texture units may not be in order of unit and it is possible to
	 * have multiple texture's with disparate unit values (e.g. a unit for 0 and
	 * 8, but no others).
	 * 
	 * @return Unmodifiable list of all bound textures and their units
	 */
	public List<Unit<Texture>> getTextures() {
		return units.getItems();
	}

	/**
	 * Get the number of units that have non-null textures bound to them.
	 * 
	 * @return Number of bound textures
	 */
	public int getNumTextures() {
		return units.size();
	}

	/** Remove all textures currently bound to this multi texture unit. */
	public void clearTextures() {
		units.clear();
	}

	@Override
	public String toString() {
		return "(" + super.toString() + " numTextures: " + getNumTextures()
						+ ")";
	}
}
