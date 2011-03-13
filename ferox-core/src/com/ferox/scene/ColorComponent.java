package com.ferox.scene;

import com.ferox.entity.TypedComponent;
import com.ferox.math.Color3f;
import com.ferox.math.ReadOnlyColor3f;

/**
 * ColorComponent is an abstract Component type for components that provide a
 * color used in describing a material to render an Entity with. This makes it
 * semantically different from the abstract {@link Light}, which also only
 * stores a color. In addition to using ColorComponent subclasses to describe an
 * Entity's material, there will often be similar Components extending from
 * {@link TextureMap} that function the same, but can act on a per-pixel basis.
 * 
 * @author Michael Ludwig
 * @param <T> The concrete type of ColorComponent
 */
public abstract class ColorComponent<T extends ColorComponent<T>> extends TypedComponent<T> {
    private final Color3f color;

    /**
     * Create a ColorComponent that copies the given color initially.
     * 
     * @param color The starting color
     * @throws NullPointerException if color is null
     */
    protected ColorComponent(ReadOnlyColor3f color) {
        super(null, false);
        this.color = new Color3f();
        setColor(color);
    }

    /**
     * Override the cloning constructor to only operate on an actual clone. Use
     * {@link #ColorComponent(ReadOnlyColor3f)} in subclasses when a clone is
     * not needed.
     * 
     * @param clone The Light of type T to clone
     * @throws NullPointerException if clone is null
     */
    protected ColorComponent(T clone) {
        super(clone, true);
        color = new Color3f(clone.color);
    }

    /**
     * Return the color stored by this component. This color will be used for
     * different purposes depending on the concrete type of ColorComponent. The
     * same instance is always returned, and will reflect any changes to the
     * color made via {@link #setColor(ReadOnlyColor3f)}.
     * 
     * @return This component's color
     */
    public ReadOnlyColor3f getColor() {
        return color;
    }
    
    /**
     * 
     * @param color
     * @return
     */
    public int setColor(ReadOnlyColor3f color) {
        if (color == null)
            throw new NullPointerException("Color cannot be null");
        this.color.set(color);
        return notifyChange();
    }
}
