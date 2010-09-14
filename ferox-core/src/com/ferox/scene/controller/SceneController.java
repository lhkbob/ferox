package com.ferox.scene.controller;

import java.util.Iterator;

import com.ferox.entity.Component;
import com.ferox.entity.ComponentId;
import com.ferox.entity.Controller;
import com.ferox.entity.Entity;
import com.ferox.entity.EntityListener;
import com.ferox.entity.EntitySystem;
import com.ferox.math.bounds.AxisAlignedBox;
import com.ferox.math.bounds.SpatialHierarchy;
import com.ferox.scene.SceneElement;

/**
 * <p>
 * SceneController is a Controller implementation that handles the processing
 * and updating of {@link SceneElement SceneElements} within a scene. It is
 * responsible for managing a {@link SpatialHierarchy} and for reseting an
 * element's visibility. For each Entity that is a SceneElement, a
 * SceneController performs the following operations:
 * <ol>
 * <li>Reset the visibility of the SceneElement, see
 * {@link SceneElement#resetVisibility()}.</li>
 * <li>Determine if a SceneElement has had its transform changed.</li>
 * <li>For modified elements, transform the element's local bound to world space
 * and store the computed world bounds on the element.</li>
 * <li>Add the element to the SceneController's SpatialHierarchy, or update the
 * element if needed (
 * {@link SpatialHierarchy#update(Object, AxisAlignedBox, Object)}).</li>
 * </ol>
 * </p>
 * <p>
 * The SceneController is intended to be the Controller that manages a
 * SpatialHierarchy. In general, Controllers which update the local bounds or
 * transform of a SceneElement should process a scene before the
 * SceneController. Any Controller that relies on the SpatialHierarchy for the
 * scene should process the scene after the SceneController runs.
 * </p>
 * 
 * @see SceneElement
 * @see SpatialHierarchy
 * @author Michael Ludwig
 */
public class SceneController extends Controller {
    private static final ComponentId<ElementData> ED_ID = Component.getComponentId(ElementData.class);
    private static final ComponentId<SceneElement> SE_ID = Component.getComponentId(SceneElement.class);

    private final SpatialHierarchy<Entity> hierarchy;
    private final SceneElementListener listener;

    /**
     * Create a new SceneController that will use <tt>hierarchy</tt> to organize
     * the Entities within the EntitySystem that this controller will process.
     * It is assumed that the SceneController is the manager of which Entities
     * are added and removed from the hierarchy. If this is not true, duplicate
     * or undefined results may occur. Additionally, the SceneController should
     * ever process a single EntitySystem, or Entities from various systems can
     * corrupt the hierarchy.
     * 
     * @param system The EntitySystem this controller processes
     * @param hierarchy The SpatialHierarchy instance to use, which is expected
     *            to be empty at this point
     * @throws NullPointerException if hierarchy is null
     */
    public SceneController(EntitySystem system, SpatialHierarchy<Entity> hierarchy) {
        super(system);
        
        if (hierarchy == null)
            throw new NullPointerException("SpatialHierarchy cannot be null");
        this.hierarchy = hierarchy;
        listener = new SceneElementListener();
    }
    
    @Override
    protected void processImpl() {
        // process all SceneElements
        Entity e;
        SceneElement element;
        Iterator<Entity> it = system.iterator(SE_ID);
        while(it.hasNext()) {
            e = it.next();
            element = e.get(SE_ID);
            if (element != null) {
                ElementData data = e.getMeta(element, ED_ID);

                if (data == null) {
                    // new scene element
                    data = new ElementData();
                    e.addMeta(element, data);
                    e.addListener(listener);
                }
                processEntity(e, element, data);
            }
        }
    }
    
    private void processEntity(Entity e, SceneElement se, ElementData ed) {
        AxisAlignedBox local = se.getLocalBounds();
        AxisAlignedBox world = null;
        
        if (local != null)
            world = local.transform(se.getTransform(), se.getWorldBounds());
        se.setWorldBounds(world);
        
        if (ed.hierarchyKey == null)
            ed.hierarchyKey = hierarchy.add(e, se.getWorldBounds());
        else
            hierarchy.update(e, se.getWorldBounds(), ed.hierarchyKey);
        
        // reset visibility
        se.resetVisibility();
    }
    
    private class SceneElementListener implements EntityListener {
        @Override
        public void onComponentAdd(Entity e, Component c) {
            // cleanup entity data if c is a scene element and
            // the entity previously had another scene element
            if (c instanceof SceneElement) {
                SceneElement old = e.get(SE_ID);
                if (old != null)
                    cleanupEntityData(e, old);
            }
        }

        @Override
        public void onComponentRemove(Entity e, Component c) {
            if (c instanceof SceneElement) {
                // this is called before the meta-components are actually removed
                cleanupEntityData(e, (SceneElement) c);
                // remove this listener from the entity
                e.removeListener(this);
            }
        }

        @Override
        public void onSystemAdd(Entity e, EntitySystem system) {
            // do nothing
        }

        @Override
        public void onSystemRemove(Entity e, EntitySystem system) {
            // note that we don't need to check the system because the listener
            // is only added to scene elements already processed by this controller.
            // any system change by an entity must always first have a removal.
            
            SceneElement se = e.get(SE_ID);
            if (se != null) {
                cleanupEntityData(e, se);
                // remove this listener from the entity
                e.removeListener(this);
            }
        }
        
        private void cleanupEntityData(Entity e, SceneElement se) {
            ElementData data = e.getMeta(se, ED_ID);
            if (data != null && data.hierarchyKey != null) {
                // remove the entity from the hierarchy so it can possibly be garbage collected
                hierarchy.remove(e, data.hierarchyKey);
            }
        }
    }

    /*
     * Meta-Component that tracks the SceneElement changes to reduce
     * inserts and changes to the spatial-hierarchies
     */
    private static class ElementData extends Component {
        Object hierarchyKey;
    }
}
