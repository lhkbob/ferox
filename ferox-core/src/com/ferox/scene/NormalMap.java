package com.ferox.scene;

import com.ferox.entity.Component;
import com.ferox.entity.Template;
import com.ferox.entity.TypedId;
import com.ferox.resource.Texture;
import com.ferox.resource.TextureFormat;
import com.ferox.resource.VertexAttribute;
import com.ferox.resource.BufferData.DataType;

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
    /**
     * The shared TypedId representing NormalMap.
     */
    public static final TypedId<NormalMap> ID = Component.getTypedId(NormalMap.class);
    
    private VertexAttribute tangentVectors;

    /**
     * Create a NormalMap that initially uses the given Texture as the source
     * for vector normals. If the tangents vertex attribute is non-null, the
     * normals are assumed to be in tangent space. When null, the normals
     * are in object space.
     * 
     * @param normalMap The normal map to use
     * @param texCoords The texture coordinates to access normalMap
     * @param tangents The tangent vectors creating the tangent space
     * @throws NullPointerException if normalMap or texCoords is null
     * @throws IllegalArgumentException if the normal map isn't a 3-component
     *             texture
     */
    public NormalMap(Texture normalMap, VertexAttribute texCoords, VertexAttribute tangents) {
        super(normalMap, texCoords);
        setTangents(tangents);
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
        tangentVectors = clone.tangentVectors;
    }

    /**
     * Return whether or not the normal vectors are in object space or tangent
     * space. See {@link #setTangents(boolean)} for details.
     * 
     * @return True if in object space
     */
    public boolean isObjectSpace() {
        return tangentVectors == null;
    }

    /**
     * <p>
     * Return the tangent vectors that form a the basis of the tangent space for
     * a vertex when the tangent is combined with the vertex's normal and the
     * cross product of the normal and tangent vector (or bitangent).
     * </p>
     * <p>
     * This will return null if {@link #isObjectSpace()} returns true.
     * </p>
     * 
     * @return The tangent vectors if this NormalMap is in tangent space
     */
    public VertexAttribute getTangents() {
        return tangentVectors;
    }

    /**
     * <p>
     * Set the vertex attribute storing tangent vectors for the entity's
     * geometry. While normal vectors are orthogonal to the surface of the
     * geometry, tangent vectors are tangent to the surface. When combined with
     * the normal data, the tangent, normal, and (tangent X normal) create an
     * orthonormal basis representing the 'tangent space' of the geometry at a
     * particular vertex.
     * </p>
     * <p>
     * When <tt>tangents</tt> is non-null, it is assumed that the normal vectors
     * encoded in the texture are in this tangent space. When <tt>tangents</tt>
     * is null, it is assumed that the vectors in the texture are in object or
     * model space.
     * </p>
     * 
     * @param tangents The new tangent vertex attribute to use
     * @return The new version of the component
     * @throws IllegalArgumentException if tangents is not null and has
     *             non-float data, or an element size other than 3
     */
    public int setTangents(VertexAttribute tangents) {
        if (tangents != null) {
            if (tangents.getData().getData().getDataType() != DataType.FLOAT)
                throw new IllegalArgumentException("Tangents must have FLOAT data");
            if (tangents.getElementSize() != 3)
                throw new IllegalArgumentException("Tangents must have an element size of 3, not: " + tangents.getElementSize());
        }
        tangentVectors = tangents;
        return notifyChange();
    }

    @Override
    protected void validate(Texture tex) {
        if (tex.getFormat().getNumComponents() != 3)
            throw new IllegalArgumentException("Normal map must use a texture format with 3 components, not: " 
                                               + tex.getFormat());
    }
}
