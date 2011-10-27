package com.ferox.physics.entity;

import java.util.Iterator;

import com.ferox.entity2.Component;
import com.ferox.entity2.ComponentId;
import com.ferox.entity2.Controller;
import com.ferox.entity2.Entity;
import com.ferox.entity2.EntitySystem;
import com.ferox.math.AffineTransform;
import com.ferox.math.Vector3f;
import com.ferox.physics.collision.Collidable;
import com.ferox.physics.collision.shape.Box;
import com.ferox.physics.dynamics.DiscretePhysicsWorld;
import com.ferox.physics.dynamics.PhysicsWorld;
import com.ferox.physics.dynamics.PhysicsWorldConfiguration;
import com.ferox.physics.dynamics.RigidBody;
import com.ferox.scene.Transform;

public class PhysicsController extends Controller {
    private static final ComponentId<PhysicsBody> P_ID = Component.getComponentId(PhysicsBody.class);
    private static final ComponentId<Transform> SE_ID = Component.getComponentId(Transform.class);
    private static final ComponentId<RigidBodyMetadata> RB_ID = Component.getComponentId(RigidBodyMetadata.class);
    
    private final PhysicsWorld world;
    private long lastFrame;
    
    public PhysicsController(EntitySystem system) {
        this(system, new DiscretePhysicsWorld(new PhysicsWorldConfiguration()));
    }
    
    public PhysicsController(EntitySystem system, PhysicsWorld physicsWorld) {
        super(system);
        world = physicsWorld;
        
        Box box = new Box(100, 100, 100);
        Collidable co = new Collidable(new AffineTransform(new Vector3f(0f, -50f, 0f)), box);
        world.add(co);
        lastFrame = -1;
    }

    @Override
    protected void executeImpl() {
        Iterator<Entity> it = system.iterator(P_ID);
        while(it.hasNext()) {
            preProcess(it.next());
        }
        
        if (lastFrame < 0)
            world.step(1f / 60f);
        else
            world.step((System.nanoTime() - lastFrame) / 1e9f);
        
        it = system.iterator(P_ID);
        while(it.hasNext()) {
            postProcess(it.next());
        }
//        lastFrame = System.nanoTime();
    }
    
    private void preProcess(Entity e) {
        PhysicsBody pb = e.get(P_ID);
        Transform se = e.get(SE_ID);
        
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
                rb.body.setTransform(se.getTransform());
            }
        }
    }
    
    private void postProcess(Entity e) {
        Transform se = e.get(SE_ID);
        RigidBodyMetadata rb = e.getMeta(e.get(P_ID), RB_ID);
        
        if (se != null && rb != null) {
            // copy physics transform back into scene element
            se.getTransform().set(rb.body.getTransform());
        }
    }
    
    private static class RigidBodyMetadata extends Component {
        RigidBody body;
    }
}
