package com.ferox.scene;

import com.ferox.entity.AbstractComponent;
import com.ferox.entity.Entity;
import com.ferox.math.Matrix4f;
import com.ferox.math.ReadOnlyMatrix4f;

public class Attached extends AbstractComponent<Attached> {
    private Entity attachedTo;
    private final Matrix4f offset;
    
    public Attached(Entity attachedTo) {
        this(attachedTo, new Matrix4f().setIdentity());
    }
    
    public Attached(Entity attachedTo, ReadOnlyMatrix4f offset) {
        super(Attached.class);
        this.offset = new Matrix4f(offset);
        setAttachment(attachedTo);
    }
    
    /**
     * Return the Entity that any updated Entities are visually attached or
     * linked to by constraining their transforms to be offset from this
     * Entity's SceneElement's transform. This is only valid when the Entity has
     * a SceneElement Component.
     * 
     * @return The attachment
     */
    public Entity getAttachment() {
        return attachedTo;
    }

    /**
     * Return the matrix transform offset that separates any updated Entity from
     * the the attachment Entity. Any changes to the returned instance will be
     * reflected in anything with this Attached component.
     * 
     * @return The offset used
     */
    public Matrix4f getOffset() {
        return offset;
    }
    
    /**
     * Set the Entity that all updated Entities will be visually attached to. If
     * this Entity is not also a SceneElement, the updates cannot be performed
     * and the updated Entities will not have their Transforms modified (it will
     * not throw an exception, however).
     * 
     * @param attachedTo The new attachment Entity
     * @throws NullPointerException if attachedTo is null
     */
    public void setAttachment(Entity attachedTo) {
        if (attachedTo == null)
            throw new NullPointerException("Entity attachedTo cannot be null");
        this.attachedTo = attachedTo;
    }
}
