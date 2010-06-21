package com.ferox.scene.controller;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.ferox.scene.Attached;
import com.ferox.scene.SceneElement;
import com.ferox.entity.Component;
import com.ferox.entity.ComponentId;
import com.ferox.entity.Controller;
import com.ferox.entity.Entity;
import com.ferox.entity.EntitySystem;

/**
 * <p>
 * The AttachmentController processes an {@link EntitySystem} to compute the
 * final transform of any {@link SceneElement} that also has an {@link Attached}
 * Component in the Entity. When computing the final transform, there are three
 * transforms necessary:
 * <ol>
 * <li>The Attached's transform that stores the final result, <tt>A</tt></li>
 * <li>The Attached's offset transform, {@link Attached#getOffset()}, <tt>O</tt>
 * </li>
 * <li>The Attached's parent's transform, {@link Attached#getAttachment()},
 * <tt>P</tt></li>
 * </ol>
 * The final transform is <code>A = P * O</code>
 * </p>
 * <p>
 * The AttachmentController requires the Attached entity to be a
 * {@link SceneElement} in order to have a transform for <tt>A</tt>.
 * Additionally, the {@link Attached#getAttachment parent entity} must also be a
 * SceneElement to provide a transform for <tt>P</tt>. Any Attached entity that
 * doesn't meet these requirements will be ignored by the AttachmentController
 * during processing.
 * </p>
 * 
 * @author Michael Ludwig
 */
public class AttachmentController extends Controller {
    private static final ComponentId<Attached> A_ID = Component.getComponentId(Attached.class);
    private static final ComponentId<SceneElement> SE_ID = Component.getComponentId(SceneElement.class);
    
    public AttachmentController(EntitySystem system) {
        super(system);
    }
    
    @Override
    protected void processImpl() {
        Set<Entity> alreadyProcessed = new HashSet<Entity>();
        Iterator<Entity> it = system.iterator(A_ID);
        while(it.hasNext()) {
            process(it.next(), alreadyProcessed);
        }
    }
    
    private void process(Entity e, Set<Entity> ignore) {
        if (ignore.contains(e))
            return; // don't process it anymore
        ignore.add(e);
        
        Attached a = e.get(A_ID);
        SceneElement se = e.get(SE_ID);
        if (a != null && se != null) {
            Entity attachedTo = a.getAttachment();
            Attached pa = attachedTo.get(A_ID);
            SceneElement pse = attachedTo.get(SE_ID);
            
            if (pse != null) {
                if (pa != null)
                    process(attachedTo, ignore);
                pse.getTransform().mul(a.getOffset(), se.getTransform());
            }
        }
    }
}
