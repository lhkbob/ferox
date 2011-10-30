package com.ferox.scene;

import com.googlecode.entreri.Component;
import com.googlecode.entreri.EntitySystem;
import com.googlecode.entreri.TypedId;

/**
 * <p>
 * DiffuseColor specifies the color of the diffuse reflection of a material. The
 * percent of light reflected is stored in each of the three color components.
 * This is combined with the lights in a scene and a lighting model that uses
 * diffuse reflection to determine a final color. Because the colors represent
 * amounts of reflection, HDR values are not used.
 * </p>
 * <p>
 * The diffuse color of a material is the primary source of "color" for an
 * object. If a renderable Entity does not provide a Component describing a
 * lighting model, the diffuse color should be used to render the Entity as a
 * solid without lighting.
 * </p>
 * <p>
 * DiffuseColor does not have any initialization parameters.
 * </p>
 * 
 * @see DiffuseColorMap
 * @author Michael Ludwig
 */
public final class DiffuseColor extends ColorComponent<DiffuseColor> {
    /**
     * The shared TypedId representing DiffuseColor.
     */
    public static final TypedId<DiffuseColor> ID = Component.getTypedId(DiffuseColor.class);
    
    private DiffuseColor(EntitySystem system, int index) {
        super(system, index);
    }
}
