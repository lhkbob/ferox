package com.ferox.scene.controller.ffp;

import java.util.Iterator;

import com.ferox.entity.Component;
import com.ferox.entity.ComponentId;
import com.ferox.entity.Controller;
import com.ferox.entity.Entity;
import com.ferox.entity.EntitySystem;
import com.ferox.math.Color4f;
import com.ferox.math.Vector3f;
import com.ferox.math.bounds.Frustum;
import com.ferox.math.bounds.SpatialHierarchy;
import com.ferox.scene.DirectionLight;
import com.ferox.scene.ShadowCaster;
import com.ferox.scene.SpotLight;
import com.ferox.scene.ViewNode;
import com.ferox.scene.controller.SceneController;
import com.ferox.scene.controller.VisibilityCallback;

/**
 * <p>
 * ShadowMapFrustumController is a Controller that determines the information
 * necessary to add shadows to a rendered scene using the shadow-mapping
 * algorithm. Using different weighting parameters, it chooses an Entity that is
 * a {@link SpotLight} or {@link DirectionLight} as the shadow caster for each
 * {@link ViewNode} within a scene.
 * </p>
 * <p>
 * Once a shadow-casting light is chosen, it computes a Frustum representing the
 * necessary projection information to use with shadow mapping and the given
 * light. This information is stored in a {@link ShadowMapFrustum} and is
 * attached to each {@link ViewNode} as a meta-Component.
 * </p>
 * <p>
 * This controller should process the scene after the scene has been updated but
 * before controllers which depend on visibility information are executed.
 * </p>
 * 
 * @author Michael Ludwig
 */
public class ShadowMapFrustumController extends Controller {
    private static final ComponentId<DirectionLight> DL_ID = Component.getComponentId(DirectionLight.class);
    private static final ComponentId<SpotLight> SL_ID = Component.getComponentId(SpotLight.class);
    private static final ComponentId<ViewNode> VN_ID = Component.getComponentId(ViewNode.class);
    private static final ComponentId<ShadowCaster> SC_ID = Component.getComponentId(ShadowCaster.class);
    
    private static final ComponentId<ShadowMapFrustum> SMF_ID = Component.getComponentId(ShadowMapFrustum.class);
    
    private final SpatialHierarchy<Entity> hierarchy;
    
    private float shadowMapScale;
    private final int shadowMapSize;

    /**
     * Create a ShadowMapFrustumController for the given EntitySystem. The
     * created controller will use the given {@link SpatialHierarchy} to
     * determine visibility information when detecting the Entities that cast
     * shadows for shadow-mapping purposes. <tt>shadowMapSize</tt> is the
     * dimension size of the shadow-map. <tt>shadowMapScale</tt> is an
     * implementation parameter that controls the size of generated frustums.
     * 
     * @param system The EntitySystem which owns this controller
     * @param hierarchy The SpatialHierarchy used to access the system's scene
     *            data, should be the same as provided to the
     *            {@link SceneController}
     * @param shadowMapScale The scale parameter for frustum sizing
     * @param shadowMapSize The dimension of the shadow map
     * @throws NullPointerException if hierarchy or system are null
     * @throws IllegalArgumentException if shadowMapSize < 1
     */
    public ShadowMapFrustumController(EntitySystem system, SpatialHierarchy<Entity> hierarchy, 
                                      float shadowMapScale, int shadowMapSize) {
        super(system);
        
        if (hierarchy == null)
            throw new NullPointerException("SpatialHierarchy cannot be null");
        if (shadowMapSize < 1)
            throw new IllegalArgumentException("Shadow map size must be at least 1: " + shadowMapSize);
        this.shadowMapSize = shadowMapSize;
        this.hierarchy = hierarchy;
        
        setShadowMapScale(shadowMapScale);
    }
    
    public float getShadowMapScale() {
        return shadowMapScale;
    }
    
    public void setShadowMapScale(float scale) {
        if (scale <= 0f)
            throw new IllegalArgumentException("Scale must be greater than 0");
        shadowMapScale = scale;
    }
    
    @Override
    protected void processImpl() {
        Iterator<Entity> vi = system.iterator(VN_ID);
        while(vi.hasNext()) {
            Entity e = vi.next();
            ViewNode viewNode = e.get(VN_ID);
            ShadowMapFrustum lf = e.getMeta(viewNode, SMF_ID);
            
            float bestWeight = 0f;
            Component bestLight = null;
            
            Entity light;
            float weight;
            
            // first check direction lights
            DirectionLight dl;
            DirectionLight oldDirLight = (lf == null || !(lf.getLight() instanceof DirectionLight) ? null : (DirectionLight) lf.getLight());
            Iterator<Entity> it = system.iterator(DL_ID);
            while(it.hasNext()) {
                light = it.next();
                dl = light.get(DL_ID);
                if (dl != null && (light.get(SC_ID) != null || light.getMeta(dl, SC_ID) != null)) {
                    weight = calculateWeight(dl, viewNode.getFrustum(), dl == oldDirLight);
                    if (weight > bestWeight) {
                        bestLight = dl;
                        bestWeight = weight;
                    }
                }
            }
            
            // now check spot lights
            SpotLight sl;
            SpotLight oldSpotLight = (lf == null || !(lf.getLight() instanceof SpotLight) ? null : (SpotLight) lf.getLight());
            it = system.iterator(SL_ID);
            while(it.hasNext()) {
                light = it.next();
                sl = light.get(SL_ID);
                if (sl != null && (light.get(SC_ID) != null || light.getMeta(sl, SC_ID) != null)) {
                    weight = calculateWeight(sl, viewNode.getFrustum(), sl == oldSpotLight);
                    if (weight > bestWeight) {
                        bestLight = sl;
                        bestWeight = weight;
                    }
                }
            }
            
            if (bestLight != null) {
                // form the shadow map frustum
                Frustum f;
                if (bestLight instanceof SpotLight)
                    f = computeLightFrustum((SpotLight) bestLight, viewNode.getFrustum(), (lf == null ? null : lf.getFrustum()));
                else
                    f = computeLightFrustum((DirectionLight) bestLight, viewNode.getFrustum(), (lf == null ? null : lf.getFrustum()));
                
                if (lf == null) {
                    lf = new ShadowMapFrustum(f, bestLight);
                    e.addMeta(viewNode, lf);
                } else {
                    lf.setFrustum(f);
                    lf.setLight(bestLight);
                }
                
                // modify all scene elements to be potentially visible
                hierarchy.query(f, new VisibilityCallback(f));
            } else {
                // no more shadow mapping
                e.removeMeta(viewNode, SMF_ID);
            }
        }
    }
    
    private Frustum computeLightFrustum(SpotLight light, Frustum view, Frustum result) {
        if (result != null) {
            result.setPerspective(light.getCutoffAngle() * 2, 1f, .01f, view.getFrustumFar() - view.getFrustumNear());
        } else {
            // create a new frustum
            result = new Frustum(light.getCutoffAngle() * 2, 1f, .01f, view.getFrustumFar() - view.getFrustumNear());
        }
        
        // orient frustum to be at spot's position and direction, while being
        // consistent with view's up vector
        result.setOrientation(light.getPosition(), light.getDirection(), view.getUp());
        return result;
    }
    
    private Frustum computeLightFrustum(DirectionLight light, Frustum view, Frustum result) {
        float scale = shadowMapScale * shadowMapSize;
        float distance = view.getFrustumFar() - view.getFrustumNear();
        
        if (result != null) {
            result.setFrustum(true, -scale, scale, -scale, scale, 0, distance);
        } else {
            // create a new frustum
            result = new Frustum(true, -scale, scale, -scale, scale, 0, distance);
        }
        
        // orient frustum to face in light's direction, offset a ways from the scene
        distance *= .25f; // focus 1/4 down the frustum
        Vector3f loc = new Vector3f(view.getLocation());
        view.getDirection().scaleAdd(distance, loc, loc); // point 1/4 down frustum
        
        float height = (view.isOrthogonalProjection() ? (view.getFrustumTop() - view.getFrustumBottom()) : 
                                                        (float) (distance * Math.tan(Math.toRadians(view.getFieldOfView()))));
        light.getDirection().scaleAdd(-height, loc, loc);
        
        result.setOrientation(loc, light.getDirection(), view.getUp());
        return result;
    }
    
    private float calculateWeight(SpotLight light, Frustum view, boolean old) {
        // exclude point lights: some physical accuracy
        if (light.getCutoffAngle() > 90f)
            return -1f;
        
        // [0, 1] - bonuses lights that are brighter
        Color4f c = light.getColor();
        float brightness = (c.getRed() + c.getGreen() + c.getBlue()) / 3f;
        
        // [0, 1] - bonuses lights that are in line with the camera direction:
        // this is because spotlights will likely be intended as flash lights
        float direction = (1 + view.getDirection().dot(light.getDirection())) / 2f;
        
        // [0, 1] - bonuses lights that are near the camera location:
        // proximity increases importance of shadows generated from light
        float scale = view.getFrustumFar() - view.getFrustumNear();
        float distance = view.getLocation().distance(light.getPosition());
        float position = Math.max(scale - distance, 0f) / scale;
        
        // [0, 1] - bonuses the previous shadow light: to avoid flickering
        float bonus = (old ? 1f : 0f);
        
        
        return (brightness + direction + position + bonus) / 4f;
    }
    
    private float calculateWeight(DirectionLight light, Frustum view, boolean old) {
        // [0, 1] - bonuses lights that are brighter: some physical accuracy
        Color4f c = light.getColor();
        float brightness = (c.getRed() + c.getGreen() + c.getBlue()) / 3f;
        
        // [0, 1] - bonuses lights that are opposite of up direction (shining down):
        // this is because they are more likely to create important shadows that will
        // render well
        float direction = (1 - view.getUp().dot(light.getDirection())) / 2f;
        
        // [0, 1] - bonuses the previous shadow light: to avoid flickering
        float bonus = (old ? 1f : 0f);
        return (brightness + direction + bonus) / 3f;
    }
}
