package com.ferox.util.input;

import com.ferox.entity.Component;
import com.ferox.entity.ComponentId;
import com.ferox.entity.Entity;
import com.ferox.input.MouseKeyEventSource;
import com.ferox.input.KeyEvent.KeyCode;
import com.ferox.input.MouseEvent.MouseButton;
import com.ferox.input.logic.AndCondition;
import com.ferox.input.logic.InputManager;
import com.ferox.input.logic.InputState;
import com.ferox.input.logic.KeyHeldCondition;
import com.ferox.input.logic.MouseButtonHeldCondition;
import com.ferox.input.logic.MouseMovedCondition;
import com.ferox.input.logic.Trigger;
import com.ferox.math.ReadOnlyVector3f;
import com.ferox.math.Vector3f;
import com.ferox.math.bounds.Frustum;
import com.ferox.scene.SceneElement;
import com.ferox.scene.ViewNode;

public class FreeLookCameraInputManager extends InputManager {
    private static final ComponentId<SceneElement> SE_ID = Component.getComponentId(SceneElement.class);
    private static final ComponentId<ViewNode> VN_ID = Component.getComponentId(ViewNode.class);
    
    private float translationSpeed;
    private Entity camera;
    
    public FreeLookCameraInputManager(MouseKeyEventSource source, Entity camera) {
        super(source);
        installTriggers();

        setTranslationalSpeed(30f);
        setCamera(camera);
    }
    
    public float getTranslationalSpeed() {
        return translationSpeed;
    }
    
    public void setTranslationalSpeed(float speed) {
        if (speed <= 0f)
            throw new IllegalArgumentException("Speed must be greater than 0, not: " + speed);
        translationSpeed = speed;
    }
    
    public Entity getCamera() {
        return camera;
    }
    
    public void setCamera(Entity camera) {
        this.camera = camera;
    }
    
    private void installTriggers() {
        // forward
        addTrigger(new TranslateTrigger(false, false), new KeyHeldCondition(KeyCode.W));
        // back
        addTrigger(new TranslateTrigger(false, true), new KeyHeldCondition(KeyCode.S));
        // left
        addTrigger(new TranslateTrigger(true, false), new KeyHeldCondition(KeyCode.A));
        // right
        addTrigger(new TranslateTrigger(true, true), new KeyHeldCondition(KeyCode.D));
        
        // mouse look
        addTrigger(new LookTrigger(), new AndCondition(new MouseButtonHeldCondition(MouseButton.LEFT), 
                                                       new MouseMovedCondition()));
    }
    
    // FIXME: this should really be set up as an or trigger for each cardinal direction
    // and within boolean it tracks which directions to go, so that diagonals move the same
    // overall speed as forwards/backs
    private class TranslateTrigger implements Trigger {
        private final boolean ortho;
        private final boolean flip;
        
        public TranslateTrigger(boolean ortho, boolean flip) {
            this.ortho = ortho;
            this.flip = flip;
        }

        @Override
        public void onTrigger(InputState prev, InputState next) {
            float dt = (next.getTimestamp() - prev.getTimestamp()) / 1e3f;
            
            // first check if there's a scene element present, because it
            // takes control of the transform of any otherwise present viewnode
            SceneElement se = camera.get(SE_ID);
            if (se != null) {
                translate(se.getTransform().getRotation().getCol(2),
                          se.getTransform().getRotation().getCol(1),
                          se.getTransform().getTranslation(), dt);
                return;
            }
            
            // if no-one attached a scene element, also work correctly with 
            // just a view node
            ViewNode vn = camera.get(VN_ID);
            if (vn != null) {
                Frustum f = vn.getFrustum();
                Vector3f trans = new Vector3f(f.getLocation());
                translate(f.getDirection(), f.getUp(), trans, dt);
                
                // reassign the new location of the camera
                vn.getFrustum().setOrientation(trans, f.getDirection(), f.getUp());
                return;
            }
            
            // the entity isn't currently a camera so we'll just do nothing
        }
        
        private void translate(ReadOnlyVector3f forward, ReadOnlyVector3f up, Vector3f trans, float dt) {
            Vector3f dir = new Vector3f(forward);
            if (ortho)
                up.cross(forward, dir);
            if (flip)
                dir.scale(-1f);
            
            dir.normalize().scaleAdd(translationSpeed * dt, trans, trans);
        }
    }

    private class LookTrigger implements Trigger {

        @Override
        public void onTrigger(InputState prev, InputState next) {
            System.out.println(next.getMouseState().getDeltaX() + ", " + next.getMouseState().getDeltaY());
        }
    }
}
