package com.ferox.scene;

import com.ferox.entity.TypedComponent;
import com.ferox.resource.VertexAttribute;
import com.ferox.resource.BufferData.DataType;

/**
 * <p>
 * Material subclasses represent lighting models that compute shading for a
 * rendered entity, based on the presence of lights in the scene. The materials
 * or lighting models can be combined with the various color components and
 * texture maps to add variety to the rendered entity.
 * </p>
 * <p>
 * The absence of a Material component implies that the entity should be
 * rendered without any light shading. It is undefined what it means to have an
 * entity with multiple lighting models.
 * </p>
 * 
 * @author Michael Ludwig
 * @param <T> The concrete type of Material
 */
public abstract class Material<T extends Material<T>> extends TypedComponent<T> {
    private VertexAttribute normals;

    /**
     * Create a new Material that will initially use the given normals.
     * 
     * @param normals The starting normal vector data
     * @throws NullPointerException if normals is null
     */
    protected Material(VertexAttribute normals) {
        super(null, false);
        setNormals(normals);
    }

    /**
     * Override the cloning constructor to only operate on an actual clone. Use
     * the {@link #Material(VertexAttribute)} in subclasses when a clone is not
     * needed
     * 
     * @param clone The Material of type T to clone
     * @throws NullPointerException if clone is null
     */
    protected Material(T clone) {
        super(clone, true);
    }

    /**
     * Set the normal vectors store per-vertex normal data used when computing
     * lighting. Normals are used when rendering an entity that is a
     * {@link Renderable}. There must be one normal for every vertex used in the
     * geometry of the Renderable.
     * 
     * @param normals The new vertex attribute holding normal vector data
     * @return The new version of the component
     * @throws NullPointerException if normals is null
     * @throws IllegalArgumentException if normals has an element size other
     *             than 3, or is not float data
     */
    public final int setNormals(VertexAttribute normals) {
        if (normals == null)
            throw new NullPointerException("Normals cannot be null");
        if (normals.getData().getData().getDataType() != DataType.FLOAT)
            throw new IllegalArgumentException("Normals must have FLOAT data");
        if (normals.getElementSize() != 3)
            throw new IllegalArgumentException("Normals must have an element size of 3, not: " + normals.getElementSize());
        
        this.normals = normals;
        return notifyChange();
    }
    
    /**
     * @return The normal vector data to use for lighting calculations
     */
    public final VertexAttribute getNormals() {
        return normals;
    }
}
