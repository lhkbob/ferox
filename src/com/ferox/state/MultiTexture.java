package com.ferox.state;

import java.util.List;

import com.ferox.resource.UnitList;
import com.ferox.resource.UnitList.Unit;

/** MultiTexture allows for combining multiple textures 
 * onto an Appearance, allowing for multi-texturing and
 * more advanced texture effects (if used in conjunction with
 * ShaderPrograms).
 * 
 * MultiTexture imposes no limit to the number of textures allowed,
 * but most renderers have some limit (depending on mode of operation
 * and hardware).  If textures are in units beyond a renderer's limits,
 * then those textures will be ignored when rendering anything.
 * 
 * Realistic limits tend to be between 4 and 16 for modern hardware.
 * 
 * When referring to texture units, units start at unit 0 and increase
 * by 1 for each subsequent unit.
 * 
 * @author Michael Ludwig
 *
 */
public class MultiTexture implements State {
	private UnitList<Texture> units;
	private Object renderData;
	
	/** Create a MultiTexture with no textures currently bound. */
	public MultiTexture() {
		this((Texture)null);
	}
	
	/** Create a MultiTexture with the given textures, starting at
	 * unit 0, and increasing by 1.  If null values are passed in,
	 * they still advance the unit count for the next texture. */
	public MultiTexture(Texture... textures) {
		this.units = new UnitList<Texture>();
		
		for (int i = 0; i < textures.length; i++)
			this.setTexture(i, textures[i]);
	}
	
	/** Set the texture on the given unit.  If texture is null,
	 * makes it so that there is no texture on the given unit.
	 * 
	 * There is a hardware maximum unit, and if a Texture is
	 * bound to a unit above this, it will likely be ignored.
	 * 
	 * If unit is less than 0, then this method fails. */
	public void setTexture(int unit, Texture texture) throws StateException {
		this.units.setItem(unit, texture);
	}
	
	/** Get the texture currently set for the given unit. 
	 * A return value of null signifies that there is no texture
	 * bound to that unit.
	 * 
	 * Fails if unit is less than 0. */
	public Texture getTexture(int unit) throws StateException {
		return this.units.getItem(unit);
	}
	
	/** Get an unmodifiable list of all the textures on this MultiTexture
	 * object. 
	 * 
	 * For each TextureUnit in the list, the following will hold:
	 *   getTexture(tu.getUnit()) == tu.getTexture().
	 *   tu.getTexture() will not be null.
	 *   
	 * Note: the texture units may not be in order of unit and it is
	 * possible to have multiple texture's with disparate unit values
	 * (e.g. a unit for 0 and 8, but no others). */
	public List<Unit<Texture>> getTextures() {
		return this.units.getItems();
	}
	
	/** Get the number of units that have non-null textures bound to them. */
	public int getNumTextures() {
		return this.units.size();
	}
	
	/** Remove all textures currently bound to this multi texture unit. */
	public void clearTextures() {
		this.units.clear();
	}

	@Override
	public Role getRole() {
		return Role.TEXTURE;
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
