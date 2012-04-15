package com.ferox.scene;

import com.ferox.math.ColorRGB;
import com.ferox.math.Const;
import com.ferox.math.entreri.ColorRGBProperty;
import com.lhkbob.entreri.ComponentData;
import com.lhkbob.entreri.annot.Unmanaged;

/**
 * <p>
 * ColorComponent is an abstract Component type for components that provide a
 * color used in describing a material to render an Entity with. This makes it
 * semantically different from the abstract {@link Light}, which also only
 * stores a color. In addition to using ColorComponent subclasses to describe an
 * Entity's material, there will often be similar Components extending from
 * {@link TextureMap} that function the same, but can act on a per-pixel basis.
 * </p>
 * 
 * @author Michael Ludwig
 * @param <T> The concrete type of ColorComponent
 */
public abstract class ColorComponent<T extends ColorComponent<T>> extends ComponentData<T> {
    private ColorRGBProperty color;
    
    @Unmanaged
    private final ColorRGB cache = new ColorRGB();

    protected ColorComponent() { }

    /**
     * Return the color stored by this component. This color will be used for
     * different purposes depending on the concrete type of ColorComponent. The
     * returned color is a cached instance shared within the component's
     * EntitySystem, so it should be cloned before accessing another component
     * of this type.
     * 
     * @return This component's color
     */
    public final @Const ColorRGB getColor() {
        return cache;
    }

    /**
     * Set the color of this component by copying <tt>color</tt> into this
     * components color object.
     * 
     * @param color The new color values
     * @return This component for chaining purposes
     * @throws NullPointerException if color is null
     */
    @SuppressWarnings("unchecked")
    public final T setColor(@Const ColorRGB color) {
        if (color == null)
            throw new NullPointerException("Color cannot be null");
        cache.set(color);
        this.color.set(color, getIndex());
        return (T) this;
    }
    
    @Override
    protected void onSet(int index) {
        color.get(index, cache);
    }
}
