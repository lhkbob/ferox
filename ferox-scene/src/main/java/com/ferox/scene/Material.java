package com.ferox.scene;

import com.ferox.resource.BufferData.DataType;
import com.ferox.resource.VertexAttribute;
import com.googlecode.entreri.Component;
import com.googlecode.entreri.EntitySystem;
import com.googlecode.entreri.InitParams;
import com.googlecode.entreri.property.ObjectProperty;

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
 * <p>
 * Material defines one initialization parameter, a VertexAttribute, that will
 * be passed to {@link #setNormals(VertexAttribute)}.
 * </p>
 * 
 * @author Michael Ludwig
 * @param <T> The concrete type of Material
 */
@InitParams(VertexAttribute.class)
public abstract class Material<T extends Material<T>> extends Component {
    private ObjectProperty<VertexAttribute> normals;

    protected Material(EntitySystem system, int index) {
        super(system, index);
    }
    
    @Override
    protected void init(Object... initParams) {
        setNormals((VertexAttribute) initParams[0]);
    }

    /**
     * Set the normal vectors store per-vertex normal data used when computing
     * lighting. Normals are used when rendering an entity that is a
     * {@link Renderable}. There must be one normal for every vertex used in the
     * geometry of the Renderable.
     * 
     * @param normals The new vertex attribute holding normal vector data
     * @return This material for chaining purposes
     * @throws NullPointerException if normals is null
     * @throws IllegalArgumentException if normals has an element size other
     *             than 3, or is not float data
     */
    @SuppressWarnings("unchecked")
    public final T setNormals(VertexAttribute normals) {
        if (normals == null)
            throw new NullPointerException("Normals cannot be null");
        if (normals.getData().getData().getDataType() != DataType.FLOAT)
            throw new IllegalArgumentException("Normals must have FLOAT data");
        if (normals.getElementSize() != 3)
            throw new IllegalArgumentException("Normals must have an element size of 3, not: " + normals.getElementSize());

        this.normals.set(normals, getIndex(), 0);
        return (T) this;
    }
    
    /**
     * @return The normal vector data to use for lighting calculations
     */
    public final VertexAttribute getNormals() {
        return normals.get(getIndex(), 0);
    }
}
