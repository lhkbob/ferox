package com.ferox.physics.entity;

import java.util.Iterator;

import com.ferox.entity.Component;
import com.ferox.entity.ComponentId;
import com.ferox.entity.Controller;
import com.ferox.entity.Entity;
import com.ferox.entity.EntitySystem;
import com.ferox.physics.dynamics.PhysicsWorld;
import com.ferox.physics.dynamics.RigidBody;
import com.ferox.scene.SceneElement;

public class PhysicsController extends Controller {
    private static final ComponentId<PhysicsBody> P_ID = Component.getComponentId(PhysicsBody.class);
    private static final ComponentId<SceneElement> SE_ID = Component.getComponentId(SceneElement.class);
    private static final ComponentId<RigidBodyMetadata> RB_ID = Component.getComponentId(RigidBodyMetadata.class);
    
    private final PhysicsWorld world;
    
    public PhysicsController(EntitySystem system) {
        this(system, new PhysicsWorld());
    }
    
    public PhysicsController(EntitySystem system, PhysicsWorld physicsWorld) {
        super(system);
        world = physicsWorld;
    }

    @Override
    protected void processImpl() {
        Iterator<Entity> it = system.iterator(P_ID);
        while(it.hasNext()) {
            preProcess(it.next());
        }
        
        world.step(1f / 60f);
        
        it = system.iterator(P_ID);
        while(it.hasNext()) {
            postProcess(it.next());
        }
    }
    
    private void preProcess(Entity e) {
        PhysicsBody pb = e.get(P_ID);
        SceneElement se = e.get(SE_ID);
        
        if (se != null && pb != null) {
            RigidBodyMetadata rb = e.getMeta(pb, RB_ID);
            if (rb == null) {
                // haven't seen the physics body before
                rb = new RigidBodyMetadata();
                rb.body = new RigidBody(se.getTransform(), pb.getShape(), pb.getMass());
                
                world.add(rb.body);
                e.addMeta(pb, rb);
            } else {
                // update transform, shape and mass
                rb.body.setMass(pb.getMass());
                rb.body.setShape(pb.getShape());
                rb.body.setWorldTransform(se.getTransform());
            }
        }
    }
    
    private void postProcess(Entity e) {
        SceneElement se = e.get(SE_ID);
        RigidBodyMetadata rb = e.getMeta(e.get(P_ID), RB_ID);
        
        if (se != null && rb != null) {
            // copy physics transform back into scene element
            se.getTransform().set(rb.body.getWorldTransform());
        }
    }
    
    private static class RigidBodyMetadata extends Component {
        RigidBody body;
    }
}
