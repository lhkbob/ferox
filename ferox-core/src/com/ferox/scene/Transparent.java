package com.ferox.scene;

import com.ferox.entity.Component;
import com.ferox.entity.Template;
import com.ferox.entity.TypedComponent;
import com.ferox.entity.TypedId;

/**
 * <p>
 * The Transparent component can be added to an Entity to change the Entity's
 * transparency or opacity. When an Entity is not fully opaque (thus partially
 * transparent), some amount of light reflected or emitted from behind the
 * Entity can travel through the Entity. The details and fidelity of being able
 * to render this depends entirely on the controller implementation handling the
 * rendering of the scene.
 * </p>
 * <p>
 * The simplest way will likely be using linearly interpolated blending based on
 * the opacity of the Entity. It could get as complex as performing subsurface
 * scattering or applying Fresnel effects, although these might require
 * additional Components to properly configure.
 * </p>
 * <p>
 * If no Transparent component is in an Entity, the Entity should be considered
 * fully opaque unless another type of component can configure transparency
 * (such as a particle system). This behavior also depends on the controllers
 * used to process the scene.
 * </p>
 * 
 * @author Michael Ludwig
 */
public final class Transparent extends TypedComponent<Transparent> {
    /**
     * The shared TypedId representing Transparent.
     */
    public static final TypedId<Transparent> ID = Component.getTypedId(Transparent.class);
    
    private float opacity;

    /**
     * Create a new Transparent component that uses the given opacity.
     * 
     * @param opacity The opacity, must be in [0, 1]
     * @throws IllegalArgumentException if opacity is less than 0 or greater
     *             than 1
     */
    public Transparent(float opacity) {
        super(null, false);
        setOpacity(opacity);
    }

    /**
     * Create a Transparent component that is a clone of <tt>clone</tt>, for use
     * with a {@link Template}.
     * 
     * @param clone The component to clone
     * @throws NullPointerException if clone is null
     */
    public Transparent(Transparent clone) {
        super(clone, true);
        opacity = clone.opacity;
    }

    /**
     * Return the opacity of the Entity. This is a value between 0 and 1,
     * representing the fraction of light that is blocked by the Entity. A value
     * of 1 means the Entity is fully opaque and a value of 0 means the Entity
     * is completely transparent.
     * 
     * @return The opacity
     */
    public float getOpacity() {
        return opacity;
    }

    /**
     * Set the opacity of this Transparent component. A value of 1 means the
     * Entity is fully opaque and a value of 0 means the Entity is completely
     * transparent. Intermediate values blend the Entity with its background in
     * different contributions depending on the exact opacity.
     * 
     * @param opacity The opacity, must be in [0, 1]
     * @return The new version of the component, via {@link #notifyChange()}
     */
    public int setOpacity(float opacity) {
        if (opacity < 0f || opacity > 1f)
            throw new IllegalArgumentException("Opacity must be in [0, 1], not: " + opacity);
        this.opacity = opacity;
        return notifyChange();
    }
}
