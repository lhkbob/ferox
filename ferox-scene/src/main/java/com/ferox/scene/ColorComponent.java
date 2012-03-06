package com.ferox.scene;

import com.ferox.math.Color3f;
import com.ferox.math.ReadOnlyColor3f;
import com.ferox.math.entreri.Color3fProperty;
import com.googlecode.entreri.Component;
import com.googlecode.entreri.EntitySystem;

/**
 * <p>
 * ColorComponent is an abstract Component type for components that provide a
 * color used in describing a material to render an Entity with. This makes it
 * semantically different from the abstract {@link Light}, which also only
 * stores a color. In addition to using ColorComponent subclasses to describe an
 * Entity's material, there will often be similar Components extending from
 * {@link TextureMap} that function the same, but can act on a per-pixel basis.
 * </p>
 * <p>
 * ColorComponent does not define any initialization parameters.
 * </p>
 * 
 * @author Michael Ludwig
 * @param <T> The concrete type of ColorComponent
 */
public abstract class ColorComponent<T extends ColorComponent<T>> extends Component {
    public static final ReadOnlyColor3f DEFAULT_COLOR = new Color3f(.5f, .5f, .5f);
    
    private Color3fProperty color;

    protected ColorComponent(EntitySystem system, int index) {
        super(system, index);
    }
    
    @Override
    protected void init(Object... initParams) {
        setColor(DEFAULT_COLOR);
    }

    /**
     * Return the color stored by this component. This color will be used for
     * different purposes depending on the concrete type of ColorComponent. The
     * returned color is a cached instance shared within the component's
     * EntitySystem, so it should be cloned before accessing another component
     * of this type.
     * 
     * @return This component's color
     */
    public final ReadOnlyColor3f getColor() {
        return color.get(getIndex());
    }

    /**
     * Return the color of this component in <tt>store</tt>. If store is null, a
     * new color is created to hold the position and returned.
     * 
     * @param store The result color to hold the color
     * @return The color in store, or a new vector if store was null
     */
    public final Color3f getPosition(Color3f store) {
        return color.get(getIndex(), store);
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
    public final T setColor(ReadOnlyColor3f color) {
        if (color == null)
            throw new NullPointerException("Color cannot be null");
        this.color.set(color, getIndex());
        return (T) this;
    }
}
