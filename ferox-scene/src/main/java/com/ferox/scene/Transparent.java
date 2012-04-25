package com.ferox.scene;

import com.lhkbob.entreri.ComponentData;
import com.lhkbob.entreri.TypeId;
import com.lhkbob.entreri.property.DoubleProperty;
import com.lhkbob.entreri.property.DoubleProperty.DefaultDouble;

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
public final class Transparent extends ComponentData<Transparent> {
    /**
     * The shared TypedId representing Transparent.
     */
    public static final TypeId<Transparent> ID = TypeId.get(Transparent.class);
    
    @DefaultDouble(0.5)
    private DoubleProperty opacity;

    private Transparent() { }
    
    /**
     * Return the opacity of the Entity. This is a value between 0 and 1,
     * representing the fraction of light that is blocked by the Entity. A value
     * of 1 means the Entity is fully opaque and a value of 0 means the Entity
     * is completely transparent.
     * 
     * @return The opacity
     */
    public double getOpacity() {
        return opacity.get(getIndex(), 0);
    }

    /**
     * Set the opacity of this Transparent component. A value of 1 means the
     * Entity is fully opaque and a value of 0 means the Entity is completely
     * transparent. Intermediate values blend the Entity with its background in
     * different contributions depending on the exact opacity.
     * 
     * @param opacity The opacity, must be in [0, 1]
     * @return This component for chaining purposes
     * @throws IllegalArgumentException if opacity is not between 0 and 1
     */
    public Transparent setOpacity(double opacity) {
        if (opacity < 0f || opacity > 1f)
            throw new IllegalArgumentException("Opacity must be in [0, 1], not: " + opacity);
        this.opacity.set(opacity, getIndex(), 0);
        return this;
    }
}