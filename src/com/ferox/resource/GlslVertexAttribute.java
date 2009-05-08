package com.ferox.resource;

/**
 * <p>
 * GlslVertexAttribute represents a bindable vertex attribute in a glsl shader
 * program. Its binding slot is parallel to the vertex attribute units in a
 * VertexArrayGeometry or a VertexBufferGeometry.
 * </p>
 * <p>
 * Each attribute has an associated type that determines the VertexArray that
 * should be used to access it. FLOAT needs an element size of 1, VECxF requires
 * an element size of x, MATyF requires an element size of y and takes up y
 * consecutive slots, where each slot represents a column in that matrix.
 * </p>
 * 
 * @author Michael Ludwig
 */
public class GlslVertexAttribute {
	/**
	 * The type of the declared vertex attribute. The matrix types use 2, 3, or
	 * 4 consecutive attribute slots to fill all values of the attribute.
	 */
	public static enum AttributeType {
		FLOAT(1), VEC2F(1), VEC3F(1), VEC4F(1), MAT2F(2), MAT3F(3), MAT4F(4);

		private int slotCount;

		private AttributeType(int slotCount) {
			this.slotCount = slotCount;
		}

		/** Return the number of adjacent slots that an attribute uses up. */
		public int getSlotCount() {
			return slotCount;
		}
	}

	private final AttributeType type;
	private final int bindingSlot;
	private final String name;

	private final GlslProgram owner;

	/**
	 * Attributes should only be created through bindAttribute() in a
	 * GlslProgram instance.
	 * 
	 * @param name The name of the attribute
	 * @param type The type of the attribute
	 * @param bindingSlot The generic vertex attribute slot to bind to
	 * @param owner The GlslProgram that this uniform should be declared in
	 * @throws IllegalArgumentException if bindingSlot < 1 or if name starts
	 *             with 'gl'
	 * @throws NullPointerException if any arguments are null
	 */
	protected GlslVertexAttribute(String name, AttributeType type,
		int bindingSlot, GlslProgram owner) throws IllegalArgumentException,
		NullPointerException {
		if (name == null)
			throw new NullPointerException("Name cannot be null");
		if (type == null)
			throw new NullPointerException("Attribute type cannot be null");
		if (owner == null)
			throw new NullPointerException("GlslProgram cannot be null");

		if (bindingSlot < 1)
			throw new IllegalArgumentException("BindingSlot must be >= 1: "
				+ bindingSlot);
		if (name.startsWith("gl"))
			throw new IllegalArgumentException("Name cannot start with 'gl': "
				+ name + ", that is reserved");

		this.type = type;
		this.name = name;
		this.owner = owner;

		this.bindingSlot = bindingSlot;
	}

	/**
	 * Return the attribute type of this GlslVertexAttribute.
	 * 
	 * @return The attribute type
	 */
	public AttributeType getType() {
		return type;
	}

	/**
	 * Return the lowest generic attribute slot used by this attribute. If
	 * getType() returns a matrix type, consecutive slots after this one will
	 * also be used.
	 * 
	 * @return The generic vertex attribute slot for this attribute
	 */
	public int getBindingSlot() {
		return bindingSlot;
	}

	/**
	 * Return the name of the attribute, as declared in this instance's owning
	 * program glsl code.
	 * 
	 * @return The name of the attribute
	 */
	public String getName() {
		return name;
	}

	/**
	 * Return the GlslProgram that this attribute is defined within.
	 * 
	 * @return The GlslProgram that this is defined in
	 */
	public GlslProgram getOwner() {
		return owner;
	}
}
