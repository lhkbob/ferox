package com.ferox.scene.controller.ffp;

import com.ferox.entity.Component;
import com.ferox.entity.Entity;
import com.ferox.math.bounds.AxisAlignedBox;
import com.ferox.math.bounds.Frustum;
import com.ferox.renderer.Surface;
import com.ferox.scene.Fog;
import com.ferox.util.Bag;

public abstract class RenderConnection {
    private final Bag<Entity> renderEntities;
    private final Bag<Entity> shadowEntities;
    
    private final Bag<Component> lights;
    private final Bag<AxisAlignedBox> lightBounds;
    
    private Component shadowLight;
    private AxisAlignedBox shadowBounds;
    private Frustum shadowFrustum;
    
    private Frustum viewFrustum;
    
    private int viewLeft, viewRight, viewBottom, viewTop;
    
    public RenderConnection() {
        renderEntities = new Bag<Entity>();
        shadowEntities = new Bag<Entity>();
        lights = new Bag<Component>();
        lightBounds = new Bag<AxisAlignedBox>();
    }

    public void addShadowCastingEntity(Entity e) {
        shadowEntities.add(e);
    }
    
    public void addRenderedEntity(Entity e) {
        renderEntities.add(e);
    }
    
    public void addLight(Component light, AxisAlignedBox lightBounds, Frustum shadowFrustum) {
        this.lightBounds.add(lightBounds);
        lights.add(light);
        
        if (shadowFrustum != null) {
            this.shadowFrustum = shadowFrustum;
            shadowLight = light;
            shadowBounds = lightBounds;
        }
    }
    
    public void addFog(Fog fog) {
        throw new UnsupportedOperationException();
    }
	
	public void setView(Frustum frustum, int left, int right, int bottom, int top) {
	    viewFrustum = frustum;
	    viewLeft = left;
	    viewRight = right;
	    viewBottom = bottom;
	    viewTop = top;
	}
	
	public Bag<Entity> getShadowCastingEntities() {
	    return shadowEntities;
	}
	
	public Bag<Entity> getRenderedEntities() {
	    return renderEntities;
	}
	
	public Bag<Component> getLights() {
	    return lights;
	}
	
	public Bag<AxisAlignedBox> getLightBounds() {
	    return lightBounds;
	}
	
	public Frustum getViewFrustum() {
	    return viewFrustum;
	}
	
	public Frustum getShadowFrustum() {
	    return shadowFrustum;
	}
	
	public Component getShadowCastingLight() {
	    return shadowLight;
	}
	
	public AxisAlignedBox getShadowCastingLightBounds() {
	    return shadowBounds;
	}
	
	public int getViewportLeft() {
	    return viewLeft;
	}
	
	public int getViewportRight() {
	    return viewRight;
	}
	
	public int getViewportBottom() {
	    return viewBottom;
	}
	
	public int getViewportTop() {
	    return viewTop;
	}
	
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
	
	public abstract void flush(Surface surface);
	
	public abstract void notifyShadowMapBegin();
	
	public abstract void notifyShadowMapEnd();
	
	public abstract void notifyBaseLightingPassBegin();
	
	public abstract void notifyBaseLightingPassEnd();
	
	public abstract void notifyShadowedLightingPassBegin();
	
	public abstract void notifyShadowedLightingPassEnd();
}
