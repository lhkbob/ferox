package com.ferox.scene;

import com.lhkbob.entreri.TypeId;

/**
 * <p>
 * DirectionLight represents an direction light (or infinite point light), where
 * objects see light coming from the same direction, regardless of their
 * position. An example of a direction light is the sun.
 * </p>
 * <p>
 * DirectionLight should be combined with a {@link Transform} component to
 * specify its orientation. The direction is encoded in the 3rd column of the
 * 4x4 affine matrix. If there is no transform component present on an entity,
 * the direction defaults to the positive z axis.
 * </p>
 * 
 * @author Michael Ludwig
 */
public final class DirectionLight extends Light<DirectionLight> {
    /**
     * The shared TypedId representing DirectionLight.
     */
    public static final TypeId<DirectionLight> ID = TypeId.get(DirectionLight.class);

    private DirectionLight() {}
}
