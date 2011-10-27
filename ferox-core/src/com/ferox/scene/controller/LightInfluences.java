package com.ferox.scene.controller;

import java.util.HashMap;

import com.ferox.entity2.Component;
import com.ferox.entity2.Controller;
import com.ferox.entity2.MetaComponent;
import com.ferox.entity2.TypedComponent;
import com.ferox.entity2.TypedId;
import com.ferox.scene.Light;

/**
 * <p>
 * LightInfluences is a meta component that stores per-entity information about
 * which lights influence its rendering. This is necessary in forward rendering
 * engines (such as the fixed function pipeline or forward shading), where light
 * influences must be known a priori to the rendering.
 * </p>
 * <p>
 * LightInfluences metadata does not need to be used for deferred engines,
 * generally, because light influences are determined by rendering the light
 * shape onto the screen.
 * </p>
 * 
 * @author Michael Ludwig
 */
@MetaComponent
public class LightInfluences extends TypedComponent<LightInfluences> {
    /**
     * The shared TypedId representing LightInfluences.
     */
    public static final TypedId<LightInfluences> ID = Component.getTypedId(LightInfluences.class);
    
    private static final Object VALUE = new Object();
    
    private final HashMap<Light<?>, Object> lights;
    
    public LightInfluences() {
        super(null, false);
        lights = new HashMap<Light<?>, Object>();
    }

    /**
     * Return true if this Entity has been flagged as influenced by the
     * Light <tt>l</tt>. Implementations of {@link Controller} are responsible
     * for assigning this as appropriate.
     * 
     * @param l The Light to check for influence
     * @return Whether or not the Entity is influenced by l
     * @throws NullPointerException if l is null
     */
    public boolean isVisible(Light<?> l) {
        if (l == null)
            throw new NullPointerException("Light cannot be null");
        return lights.containsKey(l);
    }

    /**
     * Set whether or not this Entity is considered influenced by the Light
     * <tt>l</tt>. The method is provided so that rendering controllers that
     * are forward shading can easily track which entities light which
     * rendered objects.
     * .
     * 
     * @param l The Light whose influence is assigned
     * @param influence Whether or not the Entity is influenced by l
     * @return The new version of the component, via {@link #notifyChange()}
     * @throws NullPointerException if l is null
     */
    public int setInfluenced(Light<?> l, boolean influence) {
        if (l == null)
            throw new NullPointerException("Light cannot be null");
        if (influence)
            lights.put(l, VALUE);
        else
            lights.remove(l);
        return notifyChange();
    }

    /**
     * Reset the light influences flags so that the Entity is no longer influenced by any
     * Lights. Subsequent calls to {@link #isInfluenced(Light)} will return
     * false until a Light has been flagged as influencing it via
     * {@link #setInfluenced(Light, boolean)}.
     * 
     * @return The new version of the component, via {@link #notifyChange()}
     */
    public int resetInfluences() {
        lights.clear();
        return notifyChange();
    }
}
