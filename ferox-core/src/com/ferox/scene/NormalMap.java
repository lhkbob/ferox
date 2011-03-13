package com.ferox.scene;

import com.ferox.entity.Template;
import com.ferox.resource.Texture;
import com.ferox.resource.TextureFormat;

/**
 * <p>
 * The NormalMap component adds normal-mapping to the effects applied when
 * rendering an Entity. It must be combined with Components that enable lighting
 * for it to have any effect. The normal vectors are stored in a 3-component
 * texture and the vectors can be in object or tangent space.
 * </p>
 * <p>
 * The normals within the Texture are assumed to be of unit length. If the
 * format of the texture is {@link TextureFormat#RGB_FLOAT} the x, y, and z
 * coordinates are taken as is from the red, green, and blue components of each
 * texel. If the format is any other type, the x, y, and z coordinates are
 * packed into the range, [0, 1] and are converted to [-1, 1] with
 * <code>2 * [x,y,z] - 1</code>.
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
public final class NormalMap extends TextureMap<NormalMap> {
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
        super(normalMap);
    }

    /**
     * Create a NormalMap that uses the given Texture, and
     * <tt>isObjectSpace</tt> determines whether or not the stored vectors are
     * in object space or tangent space.
     * 
     * @param normalMap The normal map to use
     * @param isObjectSpace True if normals are in object space
     * @throws NullPointerException if normalMap is null
     * @throws IllegalArgumentException if the normal map isn't a 3-component
     *             texture
     */
    public NormalMap(Texture normalMap, boolean isObjectSpace) {
        super(normalMap);
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
        super(clone);
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

    @Override
    protected void validate(Texture tex) {
        if (tex.getFormat().getNumComponents() != 3)
            throw new IllegalArgumentException("Normal map must use a texture format with 3 components, not: " 
                                               + tex.getFormat());
    }
}
