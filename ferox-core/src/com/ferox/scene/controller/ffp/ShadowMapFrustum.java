package com.ferox.scene.controller.ffp;

import com.ferox.entity.AbstractComponent;
import com.ferox.entity.Component;
import com.ferox.math.bounds.Frustum;

/**
 * ShadowMapFrustum is a meta-Component that associates the shadow casting
 * information needed for shadow-mapping for a ViewNode. This information is
 * computed by a {@link ShadowMapFrustumController} and is used by the
 * {@link FixedFunctionRenderController} to render scenes with shadows.
 * 
 * @author Michael Ludwig
 */
public class ShadowMapFrustum extends AbstractComponent<ShadowMapFrustum> {
	private Frustum frustum;
	private Component shadowLight;

    /**
     * Create a ShadowMapFrustum that stores the given Frustum and light. See
     * {@link #setFrustum(Frustum)} and {@link #setLight(Component)}
     * 
     * @param f
     * @param light
     * @throws NullPointerException if f and light are null
     */
	public ShadowMapFrustum(Frustum f, Component light) {
		super(ShadowMapFrustum.class);
		setFrustum(f);
		setLight(light);
	}

    /**
     * Set the light Component that represents the shadow-casting light.
     * 
     * @param light The new shadow-casting light
     * @throws NullPointerException if light is null
     */
	public void setLight(Component light) {
	    if (light == null)
	        throw new NullPointerException("Light cannot be null");
	    
	    shadowLight = light;
	}
	
	/**
     * @return The shadow-casting light component, either a DirectionLight or a
     *         SpotLight.
     */
	public Component getLight() {
	    return shadowLight;
	}

    /**
     * @return A Frustum that represents the projection information to render a
     *         depth-image from the perspective of the light returned by
     *         {@link #getLight()}
     */
	public Frustum getFrustum() {
		return frustum;
	}

    /**
     * Set the Frustum that stores the shadow-mapping projection to use.
     * 
     * @param f The new shadow-mapping frustum
     * @throws NullPointerException if f is null
     */
	public void setFrustum(Frustum f) {
		if (f == null)
			throw new NullPointerException("Frustum cannot be null");
		frustum = f;
	}
}