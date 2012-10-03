package com.ferox.scene;

import com.lhkbob.entreri.TypeId;

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
 * 
 * @see DiffuseColorMap
 * @author Michael Ludwig
 */
public final class DiffuseColor extends ColorComponent<DiffuseColor> {
    /**
     * The shared TypedId representing DiffuseColor.
     */
    public static final TypeId<DiffuseColor> ID = TypeId.get(DiffuseColor.class);

    private DiffuseColor() {}
}
