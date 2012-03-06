package com.ferox.scene;

import com.ferox.resource.BufferData.DataType;
import com.ferox.resource.VertexAttribute;
import com.googlecode.entreri.Controller;
import com.googlecode.entreri.Entity;
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
// FIXME: this init param might not be needed, since a null normal vector set
// can be replaced by an object-space normal map
@InitParams(VertexAttribute.class)
public abstract class Material<T extends Material<T>> extends EntitySetComponent {
    private ObjectProperty<VertexAttribute> normals;

    protected Material(EntitySystem system, int index) {
        super(system, index);
    }
    
    @Override
    protected void init(Object... initParams) {
        super.init();
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
    
    /**
     * Return true if this Entity has been flagged as lit by the given light
     * Entity. Generally, it is assumed that <tt>e</tt> is a {@link Light}
     * or other "light" component. Implementations of {@link Controller} are
     * responsible for using this as appropriate
     * 
     * @param e The Entity to check light influence
     * @return Whether or not this component's entity is lit by e
     * @throws NullPointerException if e is null
     */
    public boolean isLit(Entity e) {
        return contains(e.getId());
    }

    /**
     * As {@link #isLit(Entity)} but only requires the id of an Entity.
     * 
     * @param entityId The entity id
     * @return True if the entity represented by <tt>entityId</tt> influences
     *         this entity
     */
    public boolean isLit(int entityId) {
        return contains(entityId);
    }

    /**
     * Set whether or not this Entity is considered lit by the light Entity,
     * <tt>e</tt>. The method is provided so that Controllers can implement
     * their own light influence algorithms.
     * 
     * @param e The Entity whose light influence is assigned
     * @param lit Whether or not the Entity is lit or influence by the light, e
     * @return This component, for chaining purposes
     * @throws NullPointerException if e is null
     */
    public T setLit(Entity e, boolean lit) {
        return setLit(e.getId(), lit);
    }

    /**
     * As {@link #setLit(Entity, boolean)} but only requires the id of an
     * Entity.
     * 
     * @param entityId The entity id that is lighting or not lighting this
     *            entity
     * @param lit True if the entity is lit by entityId
     * @return This component, for chaining purposes
     */
    @SuppressWarnings("unchecked")
    public T setLit(int entityId, boolean lit) {
        if (lit)
            put(entityId);
        else
            remove(entityId);
        return (T) this;
    }

    /**
     * Reset the lit flags so that the Entity is no longer lit by any lights.
     * Subsequent calls to {@link #isLit(Entity)} will return false until an
     * Entity has been flagged as lit via {@link #setLit(Entity, boolean)}.
     * 
     * @return This component, for chaining purposes
     */
    @SuppressWarnings("unchecked")
    public T resetLightInfluences() {
        clear();
        return (T) this;
    }
}
