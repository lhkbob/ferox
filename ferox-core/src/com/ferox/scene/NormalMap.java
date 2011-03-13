package com.ferox.scene;

import com.ferox.resource.Texture;
import com.ferox.entity.Template;
import com.ferox.entity.TypedComponent;

/**
 * <p>
 * The NormalMap component adds normal-mapping to the effects applied when
 * rendering an Entity. It must be combined with Components that enable lighting
 * for it to have any effect. The normal vectors are stored in a 3-component
 * texture and the vectors can be in object or tangent space.
 * </p>
 * <p>
 * Tangent space normal vectors require the geometry of the Entity to provide
 * additional vertex attributes to describe the tangent space for each rendered
 * triangle. These attributes must be provided or configured else where because
 * it is likely to depend on the controllers actually rendering the Entities.
 * </p>
 * 
 * @author Michael Ludwig
 */
public final class NormalMap extends TypedComponent<NormalMap> {
    private Texture normalMap;
    private boolean isObjectSpace;

    /**
     * Create a NormalMap that initially uses the given Texture as the source
     * for vector normals. This assumes the normal map is in tangent space.
     * 
     * @param normalMap The normal map to use
     * @throws NullPointerException if normalMap is null
     * @throws IllegalArgumentException if the normal map isn't a 3-component
     *             texture
     */
    public NormalMap(Texture normalMap) {
        this(normalMap, false);
    }
    
    /**
     * 
     * @param normalMap
     * @param isObjectSpace
     */
    public NormalMap(Texture normalMap, boolean isObjectSpace) {
        super(null, false);
        setNormalMap(normalMap);
        setObjectSpace(isObjectSpace);
    }

    /**
     * Create a NormalMap that is a clone of <tt>clone</tt>, for use with
     * {@link Template}.
     * 
     * @param clone The NormalMap to clone
     * @throws NullPointerException if clone is null
     */
    public NormalMap(NormalMap clone) {
        super(clone, true);
        normalMap = clone.normalMap;
        isObjectSpace = clone.isObjectSpace;
    }

    /**
     * Return whether or not the normal vectors are in object space or tangent
     * space. See {@link #setObjectSpace(boolean)} for details.
     * 
     * @return True if in object space
     */
    public boolean isObjectSpace() {
        return isObjectSpace;
    }

    /**
     * <p>
     * Set whether or not the normal vectors stored in the normal map texture
     * are in object space, or in tangent space. This determines what matrix is
     * used to transform the normal vectors to camera space to compute lighting.
     * </p>
     * <p>
     * If <tt>object</tt> is true, then the normals are in the same coordinate
     * system as the geometry of the Entity, before its transformed. If it is
     * false, the normals are in the tangent space defined for each triangle or
     * polygon in the mesh. Tangent space normals work better with dynamic
     * animations, but require additional vertex attributes to specify the
     * tangent space.
     * </p>
     * 
     * @param object True if normals are in object space
     * @return The new version, via {@link #notifyChange()}
     */
    public int setObjectSpace(boolean object) {
        isObjectSpace = object;
        return notifyChange();
    }

    /**
     * <p>
     * Set the Texture that will be used as a normal map to create a bump-mapped
     * effect when rendering the Entity with lighting. If the Entity does not
     * have a material that uses lights, this will not have any effect.
     * </p>
     * <p>
     * The provided Texture must be a 3 component texture, where the x, y and z
     * vector coordinates are stored in the red, green and blue components of
     * each texel. If the format is not an unclamped floating point format, the
     * coordinates are stored in the range [0, 1], but must be scaled and
     * translated to [-1, 1] before use in a shader.
     * </p>
     * <p>
     * Note that this is only storing the reference to the texture, so the
     * NormalMap component will only report changes automatically if the
     * reference changes. If changes to the actual resource are required,
     * {@link #notifyChange()} must be called directly.
     * </p>
     * 
     * @param normalMap The new texture map reference
     * @throws NullPointerException if normalMap is null
     * @throws IllegalArgumentException if the normal map doesn't have 3
     *             components
     */
    public void setNormalMap(Texture normalMap) {
        if (normalMap == null)
            throw new NullPointerException("Normal map must be non-null");
        if (normalMap.getFormat().getNumComponents() != 3)
            throw new IllegalArgumentException("Normal map must use a texture format with 3 components, not: " 
                                               + normalMap.getFormat());
        this.normalMap = normalMap;
    }

    /**
     * Return the Texture to use as a normal map for the Entity.
     * 
     * @return The 3-component, non-null normal map texture
     */
    public Texture getNormalMap() {
        return normalMap;
    }
}
