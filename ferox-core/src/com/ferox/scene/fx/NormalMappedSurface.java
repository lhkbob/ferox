package com.ferox.scene.fx;

public class NormalMappedSurface implements Component {
	private TextureUnit normalMap;
	
	private int tangentSlot;
	private int bitangentSlot;
	
	public NormalMappedSurface(TextureUnit normalMap,
							   int tangentSlot, int bitangentSlot) {
		setNormalMap(normalMap);
		setTangentAttributeSlot(tangentSlot);
		setBitangentAttributeSlot(bitangentSlot);
	}
	
	public void setNormalMap(TextureUnit normalMap) {
		if (normalMap == null)
			throw new NullPointerException("Normal map must be non-null");
		if (normalMap.getTexture().getFormat().getNumComponents() != 3)
			throw new IllegalArgumentException("Normal map must use a texture format with 3 components, not: " 
											   + normalMap.getTexture().getFormat());
		this.normalMap = normalMap;
	}
	
	public TextureUnit getNormalMap() {
		return normalMap;
	}
	
	public void setTangentAttributeSlot(int slot) {
		if (slot < 0)
			throw new IllegalArgumentException("Tangent slot must be positive");
		this.tangentSlot = slot;
	}
	
	public int getTangentAttributeSlot() {
		return tangentSlot;
	}
	
	public void setBitangentAttributeSlot(int slot) {
		if (slot < 0)
			throw new IllegalArgumentException("Bitangent slot must be positive");
		this.bitangentSlot = slot;
	}
	
	public int getBitangentAttributeSlot() {
		return bitangentSlot;
	}

	@Override
	public final Class<NormalMappedSurface> getType() {
		return NormalMappedSurface.class;
	}
}
