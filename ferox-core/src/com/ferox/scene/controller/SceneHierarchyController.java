package com.ferox.scene.controller;

import java.util.Iterator;

import com.ferox.entity2.Component;
import com.ferox.entity2.ComponentContainer;
import com.ferox.entity2.Controller;
import com.ferox.entity2.Entity;
import com.ferox.entity2.EntitySystem;
import com.ferox.entity2.MetaComponent;
import com.ferox.entity2.Parallel;
import com.ferox.entity2.TypedComponent;
import com.ferox.entity2.TypedId;
import com.ferox.math.bounds.AxisAlignedBox;
import com.ferox.math.bounds.Octree;
import com.ferox.math.bounds.Octree.Strategy;
import com.ferox.math.bounds.SpatialHierarchy;
import com.ferox.scene.Renderable;
import com.ferox.scene.Transform;

@Parallel(reads={Transform.class, Renderable.class}, writes={SpatialHierarchy.class})
public class SceneHierarchyController extends Controller {
    private final SceneHierarchyListener listener;
    private final SpatialHierarchy<Entity> hierarchy;
    private final AxisAlignedBox tempWorldBounds;
    
    public SceneHierarchyController(EntitySystem system) {
        this(system, new Octree<Entity>(Strategy.STATIC));
    }
    
    public SceneHierarchyController(EntitySystem system, SpatialHierarchy<Entity> hierarchy) {
        super(system);
        if (hierarchy == null)
            throw new NullPointerException("SpatialHierarchy cannot be null");
        this.hierarchy = hierarchy;
        listener = new SceneHierarchyListener();
        tempWorldBounds = new AxisAlignedBox();
        
        system.addEntityListener(listener);
    }
    
    public SpatialHierarchy<Entity> getSpatialHierarchy() {
        return hierarchy;
    }

    @Override
    protected void executeImpl() {
        EntitySystem system = getEntitySystem();
        
        // This could be a possible performance issue by blocking over the entire
        // iteration.  If an entity is removed from the system on another thread
        // and needs to have its metadata cleaned up, this will cause a large
        // block while this loop completes.
        synchronized(hierarchy) {
            Iterator<Renderable> renderables = system.iterator(Renderable.ID);
            while(renderables.hasNext()) {
                Renderable renderable = renderables.next();
                ComponentContainer e = renderable.getOwner();
                if (e != null) {
                    SceneMetadata meta = e.get(SceneMetadata.ID);

                    if (meta == null) {
                        // make sure we have a metadata
                        meta = new SceneMetadata();
                        e.add(meta);
                    }

                    int newTransformVersion = e.getVersion(Transform.ID);
                    int newRenderableVersion = e.getVersion(Renderable.ID);
                    if (meta.transformVersion != newTransformVersion || meta.renderableVersion != newRenderableVersion 
                        || meta.hierarchyKey == null) {
                        Transform t = e.get(Transform.ID);
                        if (t != null)
                            renderable.getLocalBounds().transform(t.getMatrix(), tempWorldBounds);
                        else
                            tempWorldBounds.set(renderable.getLocalBounds());

                        if (meta.hierarchyKey == null)
                            meta.hierarchyKey = hierarchy.add((Entity) e, tempWorldBounds);
                        else
                            hierarchy.update((Entity) e, tempWorldBounds, meta.hierarchyKey);
                        meta.transformVersion = newTransformVersion;
                        meta.renderableVersion = newRenderableVersion;
                    }
                }
            }
        }
    }

    @Override
    protected void destroyImpl() {
        Iterator<SceneMetadata> it = getEntitySystem().iterator(SceneMetadata.ID);
        while(it.hasNext()) {
            // We only remove the SceneMetadata from the entity, we don't care
            // about the spatial hierarchy because this controller is the 'owner',
            // so the hierarchy should be discarded now
            it.next();
            it.remove();
        }
        getEntitySystem().removeEntityListener(listener);
    }
    
    private final class SceneHierarchyListener extends MetaComponentListener<Renderable, SceneMetadata> {
        public SceneHierarchyListener() {
            super(Renderable.class, SceneMetadata.class);
        }
        
        @Override
        protected void add(Entity e, Renderable component) {
            // do nothing, we add these during the process phase
        }

        @Override
        protected void remove(Entity e, SceneMetadata meta) {
            if (meta.hierarchyKey != null) {
                synchronized(hierarchy) {
                    hierarchy.remove(e, meta.hierarchyKey);
                }
            }
        }
    }
    
    @MetaComponent
    private static final class SceneMetadata extends TypedComponent<SceneMetadata> {
        static final TypedId<SceneMetadata> ID = Component.getTypedId(SceneMetadata.class);
        
        int transformVersion;
        int renderableVersion; // assume renderable change means a good chance of local bounds change
        Object hierarchyKey;
        
        public SceneMetadata() {
            super(null, false);
        }
    }
}
