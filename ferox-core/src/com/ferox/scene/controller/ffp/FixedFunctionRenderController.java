package com.ferox.scene.controller.ffp;

import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

import com.ferox.entity.Component;
import com.ferox.entity.ComponentId;
import com.ferox.entity.Controller;
import com.ferox.entity.Entity;
import com.ferox.entity.EntitySystem;
import com.ferox.math.bounds.Frustum;
import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.renderer.RenderCapabilities;
import com.ferox.renderer.Surface;
import com.ferox.renderer.TextureSurface;
import com.ferox.renderer.TextureSurfaceOptions;
import com.ferox.renderer.ThreadQueueManager;
import com.ferox.renderer.Renderer.Comparison;
import com.ferox.resource.Geometry;
import com.ferox.resource.Texture;
import com.ferox.resource.Texture.Filter;
import com.ferox.resource.Texture.Target;
import com.ferox.resource.Texture.WrapMode;
import com.ferox.scene.AmbientLight;
import com.ferox.scene.DirectionLight;
import com.ferox.scene.Renderable;
import com.ferox.scene.SceneElement;
import com.ferox.scene.ShadowCaster;
import com.ferox.scene.Shape;
import com.ferox.scene.SpotLight;
import com.ferox.scene.TexturedMaterial;
import com.ferox.scene.ViewNode;
import com.ferox.util.HashFunction;

/**
 * FixedFunctionRenderController is a controller implementation that processes
 * scenes described by the com.ferox.scene Components and renders them using the
 * fixed-function pipeline exposed by {@link FixedFunctionRenderer}. Currently,
 * it supports:
 * <ul>
 * <li>Renderable - only these entities will be rendered</li>
 * <li>SceneElement - specifies the transform of the renderable</li>
 * <li>ShadowCaster - whether a light or renderable casts shadows</li>
 * <li>ShadowReceiver - whether a renderable should receive shadows</li>
 * <li>Shape - provides geometry to render</li>
 * <li>BlinnPhongLightingModel - the only available lighting model in OpenGL</li>
 * <li>SolidLightingModel - supported, effectively disables lighting</li>
 * <li>TexturedMaterial - may ignore one or both Textures depending on hardware
 * capabilities</li>
 * </ul>
 * 
 * @author Michael Ludwig
 */
public class FixedFunctionRenderController extends Controller {
	private static final ComponentId<ViewNode> VN_ID = Component.getComponentId(ViewNode.class);
	private static final ComponentId<ShadowMapFrustum> SMF_ID = Component.getComponentId(ShadowMapFrustum.class);

	private static final ComponentId<Renderable> R_ID = Component.getComponentId(Renderable.class);
	private static final ComponentId<SceneElement> SE_ID = Component.getComponentId(SceneElement.class);
	private static final ComponentId<ShadowCaster> SC_ID = Component.getComponentId(ShadowCaster.class);
	
	private static final ComponentId<Shape> S_ID = Component.getComponentId(Shape.class);
	private static final ComponentId<TexturedMaterial> T_ID = Component.getComponentId(TexturedMaterial.class);
	
	private static final ComponentId<SpotLight> SL_ID = Component.getComponentId(SpotLight.class);
	private static final ComponentId<DirectionLight> DL_ID = Component.getComponentId(DirectionLight.class);
	private static final ComponentId<AmbientLight> AL_ID = Component.getComponentId(AmbientLight.class);
	
	
	private final ThreadQueueManager manager;
	private final TextureSurface shadowMap;
	
	private final Queue<RenderConnectionImpl> connectionPool;
    private final int maxMaterialTextureUnits;

	private final String vertexBinding;
	private final String normalBinding;
	private final String texCoordBinding;

    /**
     * Create a FixedFunctionRenderController that is attached to the given
     * EntitySystem. The provided ThreadQueueManager is used to queue the
     * necessary RenderPasses to render the scene description contained within
     * <tt>system</tt>. The controller will use a shadow-map that has a width
     * and height of 512.
     * 
     * @param system The EntitySystem owning this controller
     * @param manager The ThreadQueueManager used for queuing RenderPasses
     * @throws NullPointerException if system or manager are null
     */
	public FixedFunctionRenderController(EntitySystem system, ThreadQueueManager manager) {
		this(system, manager, 512);
	}

    /**
     * Create a FixedFunctionRenderController that is attached to the given
     * EntitySystem. The provided ThreadQueueManager is used to queue the
     * necessary RenderPasses to render the scene description contained within
     * <tt>system</tt>. The controller will create a shadow map with a width and
     * height equal to <tt>shadowMapSize</tt>. If this parameter is negative,
     * shadow mapping will be disabled.
     * 
     * @param system The EntitySystem owning this controller
     * @param manager The ThreadQueueManager used for queuing RenderPasses
     * @param shadowMapSize The width and height of the shadow map to use, or
     *            negative if now shadows are to be rendered
     * @throws NullPointerException if system or manager are null
     */
	public FixedFunctionRenderController(EntitySystem system, ThreadQueueManager manager, int shadowMapSize) {
		this(system, manager, shadowMapSize, Geometry.DEFAULT_VERTICES_NAME, 
			 Geometry.DEFAULT_NORMALS_NAME, Geometry.DEFAULT_TEXCOORD_NAME);
	}

    /**
     * Create a FixedFunctionRenderController that is attached to the given
     * EntitySystem. The provided ThreadQueueManager is used to queue the
     * necessary RenderPasses to render the scene description contained within
     * <tt>system</tt>. The controller will create a shadow map with a width and
     * height equal to <tt>shadowMapSize</tt>. If this parameter is negative,
     * shadow mapping will be disabled. In addition, <tt>vertexBinding</tt>,
     * <tt>normalBinding</tt> and <tt>texCoordBinding</tt> specify the attribute
     * names within rendered geometries if they are to differ from the defaults
     * defined in {@link Geometry}.
     * 
     * @param system The EntitySystem owning this controller
     * @param manager The ThreadQueueManager used for queuing RenderPasses
     * @param shadowMapSize The width and height of the shadow map to use, or
     *            negative if now shadows are to be rendered
     * @param vertexBinding The attribute name for vertices
     * @param normalBinding The attribute name for normals
     * @param texCoordBinding The attribute name for texture coordinates
     * @throws NullPointerException if system, manager, vertexBinding,
     *             normalBinding, or texCoordBinding are null
     */
	public FixedFunctionRenderController(EntitySystem system, ThreadQueueManager manager, int shadowMapSize,
										 String vertexBinding, String normalBinding, String texCoordBinding) {
	    super(system);
	    if (manager == null)
	        throw new NullPointerException("ThreadQueueManager cannot be null");
	    if (vertexBinding == null || normalBinding == null || texCoordBinding == null)
            throw new NullPointerException("Attribute bindings cannot be null");
	    
	    RenderCapabilities caps = manager.getFramework().getCapabilities();
        if (!caps.hasFixedFunctionRenderer())
            throw new IllegalArgumentException("Framework must support a FixedFunctionRenderer");
        
        this.manager = manager;
        this.vertexBinding = vertexBinding;
        this.normalBinding = normalBinding;
        this.texCoordBinding = texCoordBinding;
        
        connectionPool = new ConcurrentLinkedQueue<RenderConnectionImpl>();
	    
        int numTex = caps.getMaxFixedPipelineTextures();
        boolean shadowsRequested = shadowMapSize > 0; // size is positive
        boolean shadowSupport = (caps.getFboSupport() || caps.getPbufferSupport()) && 
                                numTex > 1 && caps.getDepthTextureSupport();
                        
        if (shadowsRequested && shadowSupport) {
            maxMaterialTextureUnits = (numTex > 2 ? 2 : 1);
            // convert size to a power of two
            int sz = 1;
            while(sz < shadowMapSize)
                sz = sz << 1;
            // create the shadow map
            TextureSurfaceOptions options = new TextureSurfaceOptions().setTarget(Target.T_2D)
                                                                       .setWidth(sz)
                                                                       .setHeight(sz)
                                                                       .setUseDepthTexture(true)
                                                                       .setColorBufferFormats();
            shadowMap = manager.getFramework().createSurface(options);
            
            // set up the depth comparison
            Texture sm = shadowMap.getDepthBuffer();
            sm.setFilter(Filter.LINEAR);
            sm.setWrapMode(WrapMode.CLAMP);
            sm.setDepthCompareEnabled(true);
            sm.setDepthComparison(Comparison.LEQUAL);
        } else {
            maxMaterialTextureUnits = 2;
            shadowMap = null;
        }
	}
	
	/**
     * @return The ThreadQueueManager used by this FixedFunctionRenderController
     */
	public ThreadQueueManager getThreadQueueManager() {
		return manager;
	}

    /**
     * @return The TextureSurface created by the FixedFunctionRenderController
     *         for shadow map generation. This may be null if shadow mapping is
     *         not supported on the current hardware, or if shadow mapping was
     *         disabled. If this controller's lifetime does not match that of
     *         the Framework it's tied to, this surface must be manually
     *         destroyed.
     */
	public TextureSurface getShadowMap() {
		return shadowMap;
	}
	
	@Override
    protected void processImpl() {
		// process every view, we use a ThreadQueueManager so that
		// actual rendering can be managed externally without worrying about
		// which thread executed this controller
		Iterator<Entity> views = system.iterator(VN_ID);
		while(views.hasNext()) {
			processView(system, views.next());
		}
	}
	
	private void processLights(ComponentId<?> lightType, Frustum viewFrustum, 
	                           Frustum shadowFrustum, Component shadowLight, RenderConnection con) {
	    Entity e;
	    SceneElement se;
	    Component light;
	    
	    Iterator<Entity> it = system.iterator(lightType);
	    while(it.hasNext()) {
	        e = it.next();
	        se = e.get(SE_ID);
	        light = e.get(lightType);
	        
	        if (se == null || viewFrustum == null || se.isVisible(viewFrustum)) {
	            con.addLight(light, (se == null ? null : se.getWorldBounds()), 
	                         (light == shadowLight ? shadowFrustum : null));
	        }
	    }
	}
	
	private void processView(EntitySystem system, Entity view) {
		ViewNode vn = view.get(VN_ID);
		if (vn == null)
			return; // don't have anything to render
		
		RenderConnection con = connectionPool.poll();
		if (con == null)
		    con = new RenderConnectionImpl();
		
		// prepare the rendering
		Frustum viewFrustum = vn.getFrustum();
		Frustum shadowFrustum = null;
		Component shadowCaster = null;
		
		// first take care of shadowing information
		ShadowMapFrustum smf = view.getMeta(vn, SMF_ID);
		if (smf != null && shadowMap != null) {
		    shadowFrustum = smf.getFrustum();
		    shadowCaster = smf.getLight();
		}
		
		// process all renderables (and shadow casters)
		Entity e;
		SceneElement se;
		Iterator<Entity> it = system.iterator(R_ID);
		while(it.hasNext()) {
		    e = it.next();
		    se = e.get(SE_ID);
		    if ((se == null || shadowFrustum == null || se.isVisible(shadowFrustum)) &&
		        (e.get(SC_ID) != null || e.getMeta(e.get(R_ID), SC_ID) != null))
		        con.addShadowCastingEntity(e);
		    if (se == null || viewFrustum == null || se.isVisible(viewFrustum))
		        con.addRenderedEntity(e);
		}
		
		processLights(AL_ID, viewFrustum, shadowFrustum, shadowCaster, con);
		processLights(SL_ID, viewFrustum, shadowFrustum, shadowCaster, con);
		processLights(DL_ID, viewFrustum, shadowFrustum, shadowCaster, con);
		
		// configure the view
		con.setView(vn);
		con.flush(vn.getRenderSurface());
	}
	
	private class RenderConnectionImpl extends RenderConnection {
	    private final ShadowMapGeneratorPass shadowMapPass;
	    private final DefaultLightingPass defaultPass;
	    private final ShadowedLightingPass shadowLightPass;
	    
	    private final Semaphore shadowMapAccessor; // FIXME: this needs to be up a level so all connections share this
	    
	    public RenderConnectionImpl() {
	        shadowMapAccessor = new Semaphore(1, false);
	        
	        defaultPass = new DefaultLightingPass(this, manager.getFramework().getCapabilities().getMaxActiveLights(), 
                                                  maxMaterialTextureUnits, vertexBinding, normalBinding, texCoordBinding);

	        if (shadowMap == null) {
	            shadowMapPass = null;
	            shadowLightPass = null;
	        } else {
	            shadowMapPass = new ShadowMapGeneratorPass(this, maxMaterialTextureUnits, vertexBinding);
	            shadowLightPass = new ShadowedLightingPass(this, shadowMap.getDepthBuffer(), maxMaterialTextureUnits,
	                                                       vertexBinding, normalBinding, texCoordBinding);
	        }
	    }

        @Override
        public void flush(Surface surface) {
            getRenderedEntities().sort(ENTITY_HASHER);
            getShadowCastingEntities().sort(ENTITY_HASHER);
            
            if (shadowMap == null || getShadowFrustum() == null) {
                // just do the default pass
                manager.queue(surface, defaultPass);
            } else {
                // do all three
                String group = manager.getSurfaceGroup(surface);
                manager.queue(group, shadowMap, shadowMapPass);
                manager.queue(group, surface, defaultPass);
                manager.queue(group, surface, shadowLightPass);
            }
        }

        @Override
        public void notifyBaseLightingPassBegin() {
            // begin rendering right away, this phase doesn't
            // depend on the shadow map
        }

        @Override
        public void notifyBaseLightingPassEnd() {
            if (shadowMap == null || getShadowFrustum() == null) {
                // if there's no shadow map, this is the final stage
                // so we must close up shop
                close();
            }
        }

        @Override
        public void notifyShadowMapBegin() {
            try {
                shadowMapAccessor.acquire();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // preserve status
                throw new RuntimeException(e);
            }
        }

        @Override
        public void notifyShadowMapEnd() {
            shadowMapAccessor.release();
        }

        @Override
        public void notifyShadowedLightingPassBegin() {
            try {
                shadowMapAccessor.acquire();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // preserve status
                throw new RuntimeException(e);
            }
        }

        @Override
        public void notifyShadowedLightingPassEnd() {
            // if invoked, this is always the last stage, so close up
            shadowMapAccessor.release();
            close();
        }
        
        private void close() {
            reset();
            connectionPool.add(this);
        }
	}
	
	private static final HashFunction<Entity> ENTITY_HASHER = new HashFunction<Entity>() {
        @Override
        public int hashCode(Entity value) {
            Shape s = value.get(S_ID);
            int geomId = (s == null ? 0 : s.getGeometry().getId());
            
            TexturedMaterial tm = value.get(T_ID);
            int tpId = (tm == null ? 0 : (tm.getPrimaryTexture() == null ? 0 : tm.getPrimaryTexture().getId()));
            int tdId = (tm == null ? 0 : (tm.getDecalTexture() == null ? 0 : tm.getDecalTexture().getId()));
            
            return ((geomId << 20) | (tpId << 10) | (tdId));
        }
	};
}
