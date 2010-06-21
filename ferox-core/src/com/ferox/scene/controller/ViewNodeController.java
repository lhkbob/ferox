package com.ferox.scene.controller;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.ferox.math.Matrix3f;
import com.ferox.math.bounds.Frustum;
import com.ferox.math.bounds.SpatialHierarchy;
import com.ferox.renderer.Surface;
import com.ferox.scene.SceneElement;
import com.ferox.scene.ViewNode;
import com.ferox.util.Bag;
import com.ferox.entity.Component;
import com.ferox.entity.ComponentId;
import com.ferox.entity.Controller;
import com.ferox.entity.Entity;
import com.ferox.entity.EntitySystem;

/**
 * ViewNodeController is a Controller implementation that handles the processing
 * necessary to update each ViewNode within an EntitySystem. For each Entity
 * that is a ViewNode, a ViewNodeController performs the following operations:
 * <ol>
 * <li>If the entity is also a SceneElement, modify the ViewNode's Frustum so
 * that it reflects the location and orientation of the SceneElement. The y-axis
 * is considered up and the z-axis is considered to be the direction.</li>
 * <li>If {@link ViewNode#getAutoUpdateViewport()} returns true, modify the
 * viewport and projection as described in ViewNode to match its Surface's
 * dimensions.</li>
 * <li>Invoke {@link Frustum#updateFrustumPlanes()} so the Frustum is up to
 * date.</li>
 * <li>Query the {@link SpatialHierarchy} of the system with the ViewNode's
 * Frustum and mark the returned SceneElements as visible using
 * {@link SceneElement#setVisible(Frustum, boolean)}</li>
 * </ol>
 * 
 * @author Michael Ludwig
 */
public class ViewNodeController extends Controller {
    private static final ComponentId<ViewNode> VN_ID = Component.getComponentId(ViewNode.class);
    private static final ComponentId<SceneElement> SE_ID = Component.getComponentId(SceneElement.class);

    private Map<ViewNode, Dimension> dimensions;
    private final Bag<Entity> queryCache;
    private final SpatialHierarchy<Entity> hierarchy;

    /**
     * Create a ViewNodeController. Because it requires a SpatialHierarchy to
     * perform queries based on a ViewNode's Frustum, a ViewNodeController
     * should not be used with multiple EntitySystems. The SpatialHierarchy
     * specified should only contain Entities from the appropriate system. It
     * should also be the hierarchy that is used by the other Controllers which
     * process the system (e.g. {@link SceneController}).
     * 
     * @param system The EntitySystem processed by this controller
     * @param hierarchy SpatialHierarchy that will contain Entities of the
     *            EntitySystem that this controller is to process.
     * @throws NullPointerException if hierarchy is null
     */
    public ViewNodeController(EntitySystem system, SpatialHierarchy<Entity> hierarchy) {
        super(system);
        if (hierarchy == null)
            throw new NullPointerException("SpatialHierarchy cannot be null");
        this.hierarchy = hierarchy;
        
        dimensions = new HashMap<ViewNode, Dimension>();
        queryCache = new Bag<Entity>();
    }

    @Override
    protected void processImpl() {
        Map<ViewNode, Dimension> nextDimensions = new HashMap<ViewNode, Dimension>();
        
        Iterator<Entity> it = system.iterator(VN_ID);
        while(it.hasNext()) {
            // process each viewnode independently
            process(it.next(), nextDimensions);
        }
        
        dimensions = nextDimensions;
    }
    
    private void process(Entity e, Map<ViewNode, Dimension> store) {
        ViewNode vn = e.get(VN_ID);
        SceneElement se = e.get(SE_ID);
        
        Frustum f = vn.getFrustum();
        boolean needsUpdate = true;
        if (se != null) {
            // modify the frustum so that it matches the scene element 
            // location and orientation
            Matrix3f m = se.getTransform().getRotation();
            m.getCol(1, f.getUp());
            m.getCol(2, f.getDirection());
            f.getLocation().set(se.getTransform().getTranslation());
        }
        
        if (vn.getAutoUpdateViewport()) {
            Surface surface = vn.getRenderSurface();
            
            int width = surface.getWidth();
            int height = surface.getHeight();
            
            Dimension dim = dimensions.get(vn);
            if (dim == null) {
                dim = new Dimension();
                dim.width = width;
                dim.height = height;
            }
            
            float scaleX = (float) width / dim.width;
            float scaleY = (float) height / dim.height;
            
            int vpLeft = (int) Math.max(0, Math.min(scaleX * vn.getLeft(), width));
            int vpRight = (int) Math.max(0, Math.min(scaleX * vn.getRight(), width));
            int vpBottom = (int) Math.max(0, Math.min(scaleY * vn.getBottom(), height));
            int vpTop = (int) Math.max(0, Math.min(scaleY * vn.getTop(), height));
            
            // assign the new viewport
            vn.setViewport(vpLeft, vpRight, vpBottom, vpTop);
            
            if (f.isOrthogonalProjection()) {
                // modify the frustum so that it spans the entire RS
                f.setFrustum(true, scaleX * f.getFrustumLeft(), scaleX * f.getFrustumRight(), 
                             scaleY * f.getFrustumBottom(), scaleY * f.getFrustumTop(), 
                             f.getFrustumNear(), f.getFrustumFar());
            } else {
                // modify the frustum to use the correct aspect ratio
                float fov = f.getFieldOfView();
                f.setPerspective(fov, (vpRight - vpLeft) / (float) (vpTop - vpBottom), 
                                 f.getFrustumNear(), f.getFrustumFar());
            }
            
            // remember surface's dimension for next process
            dim.width = width;
            dim.height = height;
            store.put(vn, dim);
            
            // setFrustum and setPerspective auto update the frustum, so 
            // set needsUpdate to false so we don't do it again
            needsUpdate = false;
        }
        
        if (needsUpdate)
            f.updateFrustumPlanes();
        
        // frustum is up-to-date, so now we perform a visibility query
        queryCache.clear(true);
        hierarchy.query(f, queryCache);

        // modify all scene elements to be potentially visible
        int ct = queryCache.size();
        for (int i = 0; i < ct; i++) {
            se = (SceneElement) queryCache.get(i).get(SE_ID);
            if (se != null)
                se.setVisible(f, true);
        }
    }
    
    private static class Dimension {
        int width;
        int height;
    }
}
