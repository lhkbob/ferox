package com.ferox.scene.controller;

import java.util.Iterator;

import com.ferox.entity.Component;
import com.ferox.entity.ComponentId;
import com.ferox.entity.Controller;
import com.ferox.entity.Entity;
import com.ferox.entity.EntitySystem;
import com.ferox.math.Matrix3f;
import com.ferox.math.MutableVector3f;
import com.ferox.math.ReadOnlyVector3f;
import com.ferox.math.Transform;
import com.ferox.math.Vector3f;
import com.ferox.scene.Billboarded;
import com.ferox.scene.Billboarded.Axis;
import com.ferox.scene.SceneElement;

/**
 * <p>
 * The BillboardController processes an {@link EntitySystem} to implement the
 * behavior necessary that allows {@link SceneElement} to be {@link Billboarded}
 * . This controller will process all Billboarded entities that are also
 * SceneElements, and update the SceneElement's transform as required to meet
 * the constraints of the billboard.
 * </p>
 * <p>
 * Any Billboarded entities that are not SceneElements are ignored. The
 * SceneElement is required as a source for the location, and the result of the
 * orientation computation.
 * </p>
 * 
 * @author Michael Ludwig
 */
public class BillboardController extends Controller {
    private static final ComponentId<Billboarded> B_ID = Component.getComponentId(Billboarded.class);
    private static final ComponentId<SceneElement> SE_ID = Component.getComponentId(SceneElement.class);
    
    private static final Vector3f ZERO = new Vector3f();
    
    public BillboardController(EntitySystem system) {
        super(system);
    }
    
    @Override
    protected void processImpl() {
        Iterator<Entity> it = system.iterator(B_ID);
        while(it.hasNext()) {
            process(it.next());
        }
    }

    private void process(Entity e) {
        Billboarded b = e.get(B_ID);
        SceneElement se = e.get(SE_ID);
        if (b != null && se != null) {
            satisfyConstraint(b, se.getTransform(), Axis.X);
            satisfyConstraint(b, se.getTransform(), Axis.Y);
            satisfyConstraint(b, se.getTransform(), Axis.Z);
        }
    }
    
    private void satisfyConstraint(Billboarded b, Transform t, Axis axis) {
        // X = 0, Y = 1, Z = 2
        int o = axis.ordinal();
        ReadOnlyVector3f constraint = b.getConstraint(axis);
        Matrix3f rotation = t.getRotation();
        
        if (constraint != null) {
            Vector3f d = new Vector3f();
            if (b.isPositionConstraint(axis))
                constraint.sub(t.getTranslation(), d).normalize();
            else
                constraint.normalize(d);
            
            if (b.isConstraintAxisNegated(axis))
                d.scale(-1f);
            
            MutableVector3f a = rotation.getCol((o + 2) % 3, null).ortho(d);
            if (!a.epsilonEquals(ZERO, .0001f)) {
                // properly perpendicular so we can update matrix
                a.normalize();
                
                rotation.setCol(o, d);
                rotation.setCol((o + 2) % 3, a);
                rotation.setCol((o + 1) % 3, a.cross(d));
            } else {
                // singularity for ortho axis (it equals desired direction)
                rotation.getCol((o + 1) % 3, a).ortho(d).normalize();
                
                rotation.setCol(o, d);
                rotation.setCol((o + 1) % 3, a);
                rotation.setCol((o + 2) % 3, d.cross(a));
            }
        }
    }
}
