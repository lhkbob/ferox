package com.ferox.scene.controller.ffp;

import com.ferox.entity.Component;
import com.ferox.entity.Entity;
import com.ferox.entity.EntitySystem;
import com.ferox.math.Vector3f;
import com.ferox.math.bounds.AxisAlignedBox;
import com.ferox.math.bounds.Frustum;
import com.ferox.renderer.Surface;
import com.ferox.scene.BlinnPhongLightingModel;
import com.ferox.scene.Fog;
import com.ferox.scene.TexturedMaterial;
import com.ferox.scene.ViewNode;
import com.ferox.util.Bag;

/**
 * RenderConnection is a light-weight container around the elements of a scene
 * that have been extracted from an {@link EntitySystem} by a
 * {@link FixedFunctionRenderController}. Additionally, implementations will
 * contain the necessary logic to queue RenderPass implementations that
 * correctly render the description contained by this RenderConnection.
 * 
 * @author Michael Ludwig
 */
public abstract class RenderConnection {
    private static final AxisAlignedBox INFINITE = new AxisAlignedBox(new Vector3f(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY),
                                                                      new Vector3f(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY));
    private final Bag<Entity> renderEntities;
    private final Bag<Entity> shadowEntities;
    
    private final Bag<Component> lights;
    private final Bag<AxisAlignedBox> lightBounds;
    
    private Component shadowLight;
    private AxisAlignedBox shadowBounds;
    private Frustum shadowFrustum;
    
    private Frustum viewFrustum;
    
    private int viewLeft, viewRight, viewBottom, viewTop;
    
    /**
     * Create an initially empty RenderConnection.
     */
    public RenderConnection() {
        renderEntities = new Bag<Entity>();
        shadowEntities = new Bag<Entity>();
        lights = new Bag<Component>();
        lightBounds = new Bag<AxisAlignedBox>();
    }

    /**
     * Add the given Entity to the list of entities which cast shadows from the
     * configured {@link #getShadowCastingLight() shadow-casting light}.
     * 
     * @param e The Entity that casts shadows
     * @throws NullPointerException if e is null
     */
    public void addShadowCastingEntity(Entity e) {
        shadowEntities.add(e);
    }

    /**
     * Add the given Entity to the list of entities which should be rendered
     * based on their attached Components, such as
     * {@link BlinnPhongLightingModel} or {@link TexturedMaterial}.
     * 
     * @param e The Entity that is to be rendered
     * @throws NullPointerException if e is null
     */
    public void addRenderedEntity(Entity e) {
        renderEntities.add(e);
    }

    /**
     * <p>
     * Add a light that will influence the scene based on the provided light
     * bounds. If <tt>lightBounds</tt> is null, an AxisAlignedBox that has
     * infinite dimensions is used.
     * </p>
     * <p>
     * If <tt>shadowFrustum</tt> is non-null, the provided light represents a
     * shadow-casting light. Each time a shadow-casting light is added, its
     * state overwrites the state of any previous shadow-casting light because
     * only one shadow casting light is supported at a given time. When
     * non-null, the provided Frustum should represent a proper projection of
     * the scene as seen by the light.
     * </p>
     * 
     * @param light The light data, either an AmbientLight, DirectionLight or
     *            SpotLight
     * @param lightBounds The AxisAlignedBox representing the light's influence
     *            or null
     * @param shadowFrustum A non-null Frustum if this light casts shadows, or
     *            null
     * @throws NullPointerException if light is null
     */
    public void addLight(Component light, AxisAlignedBox lightBounds, Frustum shadowFrustum) {
        if (lightBounds == null)
            lightBounds = INFINITE;
        
        this.lightBounds.add(lightBounds);
        lights.add(light);
        
        if (shadowFrustum != null) {
            this.shadowFrustum = shadowFrustum;
            shadowLight = light;
            shadowBounds = lightBounds;
        }
    }

    /**
     * Add the given Fog to the scene. The densest fog that influences the
     * viewing location will be used to apply OpenGL eye-space fog to the
     * rendered scene.
     * 
     * @param fog The Fog to add
     * @throws NullPointerException if fog is null
     */
    public void addFog(Fog fog) {
        throw new UnsupportedOperationException();
    }

    /**
     * Set the view Frustum and viewport dimensions that will be used when
     * rendering the primary scene.
     * 
     * @param vn The ViewNode that represents the primary viewing location
     * @throws NullPointerException if vn is null
     */
    public void setView(ViewNode vn) {
        viewFrustum = vn.getFrustum();
        viewLeft = vn.getLeft();
        viewRight = vn.getRight();
        viewBottom = vn.getBottom();
        viewTop = vn.getTop();
    }

    /**
     * @return The list of Entities that cast shadows for the shadow map
     */
    public Bag<Entity> getShadowCastingEntities() {
        return shadowEntities;
    }

    /**
     * @return The list of Entities that embody the scene to render
     */
    public Bag<Entity> getRenderedEntities() {
        return renderEntities;
    }

    /**
     * @return The list of lights which influence the scene
     */
    public Bag<Component> getLights() {
        return lights;
    }

    /**
     * @return The list of light bounds. Each element in the returned Bag is
     *         paired with a light Component at the same index from the lights
     *         returned by {@link #getLights()}
     */
    public Bag<AxisAlignedBox> getLightBounds() {
        return lightBounds;
    }
    
    /**
     * @return The primary viewing Frustum
     */
    public Frustum getViewFrustum() {
        return viewFrustum;
    }

    /**
     * @return The Frustum to use for shadow-map generation, may be null, in
     *         which case the shadow map passes should not be rendered
     */
    public Frustum getShadowFrustum() {
        return shadowFrustum;
    }

    /**
     * @return The light Component that casts shadows, which can be null. This
     *         returned component will be in the Bag returned by
     *         {@link #getLights()}
     */
    public Component getShadowCastingLight() {
        return shadowLight;
    }

    /**
     * @return The bounds of the shadow-casting light. This will be null only
     *         when {@link #getShadowCastingLight()} is null
     */
    public AxisAlignedBox getShadowCastingLightBounds() {
        return shadowBounds;
    }
    
    /**
     * @return The left edge of the primary viewport
     */
    public int getViewportLeft() {
        return viewLeft;
    }
    
    /**
     * @return The right edge of the primary viewport
     */
    public int getViewportRight() {
        return viewRight;
    }
    
    /**
     * @return The bottom edge of the primary viewport
     */
    public int getViewportBottom() {
        return viewBottom;
    }
    
    /**
     * @return The top edge of the primary viewport
     */
    public int getViewportTop() {
        return viewTop;
    }

    /**
     * Reset all internal datastructures so that this RenderConnection
     * represents an empty scene again.
     */
    public void reset() {
        viewFrustum = null;
        shadowFrustum = null;
        shadowLight = null;
        shadowBounds = null;
        
        renderEntities.clear(true);
        shadowEntities.clear(true);
        lights.clear(true);
        lightBounds.clear(true);
    }

    /**
     * Queue all necessary RenderPasses to render the contents of this
     * RenderConnection.
     * 
     * @param surface The Surface that will be rendered into for the primary
     *            view
     */
    public abstract void flush(Surface surface);

    /**
     * To be invoked by a RenderPass that's beginning the generation of the
     * shadow map.
     */
    public abstract void notifyShadowMapBegin();

    /**
     * To be invoked by a RenderPass that's ending the generation of the shadow
     * map.
     */
    public abstract void notifyShadowMapEnd();
    
    /**
     * To be invoked by a RenderPass that's beginning the base/default lighting
     * pass.
     */
    public abstract void notifyBaseLightingPassBegin();
    
    /**
     * To be invoked by a RenderPass that's ending the base/default lighting
     * pass.
     */
    public abstract void notifyBaseLightingPassEnd();

    /**
     * To be invoked by a RenderPass that's beginning the shadowed lighting
     * pass.
     */
    public abstract void notifyShadowedLightingPassBegin();
    
    /**
     * To be invoked by a RenderPass that's ending the shadowed lighting pass.
     */
    public abstract void notifyShadowedLightingPassEnd();
}
