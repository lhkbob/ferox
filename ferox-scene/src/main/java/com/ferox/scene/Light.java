package com.ferox.scene;

import com.ferox.math.Color3f;
import com.ferox.math.ReadOnlyColor3f;
import com.ferox.math.entreri.ColorRGBProperty;
import com.googlecode.entreri.Component;
import com.googlecode.entreri.EntitySystem;

/**
 * <p>
 * Light is an abstract class that Components representing lights within a 3D
 * scene should extend from. Light does not enforce any particular rules on how
 * a light is described, except that it has a {@link #getColor() color}. The
 * Light type exists so that the myriad types of lights do not need to repeat
 * this definition.
 * </p>
 * <p>
 * Additionally, the colors held by Light components should use the HDR values
 * stored in the returned {@link ReadOnlyColor3f}'s. This is because the color
 * of a light can be ultra-bright, going past the conventional limit of 1 for a
 * color component. Support for the HDR values when rendering is dependent on
 * the rendering framework, however.
 * </p>
 * 
 * @author Michael Ludwig
 * @param <T> The concrete type of light
 */
public abstract class Light<T extends Light<T>> extends Component {
    public static final ReadOnlyColor3f DEFAULT_COLOR = new Color3f(.2f, .2f, .2f);

    private ColorRGBProperty color;

    protected Light(EntitySystem system, int index) {
        super(system, index);
    }
    
    
    @Override
    protected void init(Object... initParams) {
        setColor(DEFAULT_COLOR);
    }

    /**
     * Return the color of this Light. The returned color is a cached instance
     * shared within the component's EntitySystem, so it should be cloned before
     * accessing another component of this type.
     * 
     * @return The color of this Light
     */
    public final ReadOnlyColor3f getColor() {
        return color.get(getIndex());
    }
    
    /**
     * Return the color of this light in <tt>store</tt>. If store is
     * null, a new ColorRGB is created to hold the color and returned.
     * 
     * @param store The result ColorRGB to hold the color
     * @return The color in store, or a new vector if store was null
     */
    public final Color3f getPosition(Color3f store) {
        return color.get(getIndex(), store);
    }

    /**
     * Set the color of this Light. The color values in <tt>color</tt> are
     * copied into an internal instance, so any future changes to <tt>color</tt>
     * will not affect this Component.
     * 
     * @param color The new color
     * @return This light for chaining purposes
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
