package com.ferox.scene.ffp;

import java.util.Iterator;

import com.ferox.math.bounds.Frustum;
import com.ferox.renderer.RenderThreadingOrganizer;
import com.ferox.renderer.TextureSurface;
import com.ferox.resource.Geometry;
import com.ferox.scene.AmbientLight;
import com.ferox.scene.DirectionLight;
import com.ferox.scene.Renderable;
import com.ferox.scene.SpotLight;
import com.ferox.scene.ViewNode;
import com.ferox.scene.ffp.RenderConnection.Stream;
import com.ferox.scene.ffp.ShadowMapFrustumController.ShadowMapFrustum;
import com.ferox.entity.Component;
import com.ferox.entity.ComponentId;
import com.ferox.entity.Controller;
import com.ferox.entity.Entity;
import com.ferox.entity.EntitySystem;

public class FixedFunctionRenderController extends Controller {
	private static final ComponentId<ViewNode> VN_ID = Component.getComponentId(ViewNode.class);
	private static final ComponentId<Renderable> R_ID = Component.getComponentId(Renderable.class);
	private static final ComponentId<SpotLight> SL_ID = Component.getComponentId(SpotLight.class);
	private static final ComponentId<DirectionLight> DL_ID = Component.getComponentId(DirectionLight.class);
	private static final ComponentId<AmbientLight> AL_ID = Component.getComponentId(AmbientLight.class);
	
	private static final ComponentId<ShadowMapFrustum> SMF_ID = Component.getComponentId(ShadowMapFrustum.class);
	
	private final EntityAtomBuilder entityBuilder;
	private final FixedFunctionAtomRenderer renderer;
	
	public FixedFunctionRenderController(EntitySystem system, RenderThreadingOrganizer organizer) {
		this(system, organizer, 512);
	}
	
	public FixedFunctionRenderController(EntitySystem system, RenderThreadingOrganizer organizer, int shadowMapSize) {
		this(system, organizer, shadowMapSize, Geometry.DEFAULT_VERTICES_NAME, 
			 Geometry.DEFAULT_NORMALS_NAME, Geometry.DEFAULT_TEXCOORD_NAME);
	}
	
	public FixedFunctionRenderController(EntitySystem system, RenderThreadingOrganizer organizer, int shadowMapSize,
										 String vertexBinding, String normalBinding, String texCoordBinding) {
		this(system, new FixedFunctionAtomRenderer(organizer, shadowMapSize, vertexBinding, normalBinding, texCoordBinding));
	}
	
	public FixedFunctionRenderController(EntitySystem system, FixedFunctionAtomRenderer renderer) {
	    super(system);
		if (renderer == null)
			throw new NullPointerException("FixedFunctionAtomRenderer cannot be null");
		this.renderer = renderer;
		entityBuilder = new EntityAtomBuilder();
	}
	
	public FixedFunctionAtomRenderer getAtomRenderer() {
		return renderer;
	}
	
	public RenderThreadingOrganizer getRenderThreadingOrganizer() {
		return renderer.getRenderThreadingOrganizer();
	}
	
	public TextureSurface getShadowMap() {
		return renderer.getShadowMap();
	}
	
	@Override
	public void process() {
		// process every view, we use a threading organizer so that
		// actual rendering can be managed externally without worrying about
		// which thread executed this controller
		Iterator<Entity> views = system.iterator(VN_ID);
		while(views.hasNext()) {
			processView(system, views.next());
		}
	}
	
	private void processView(EntitySystem system, Entity view) {
		ViewNode vn = view.get(VN_ID);
		if (vn == null)
			return; // don't have anything to render
		
		Frustum f = vn.getFrustum();
		RenderConnection con = renderer.getConnection(vn.getRenderSurface());
	
		// process all entities
		Stream<RenderAtom> raStream = con.getRenderAtomStream();
		Stream<LightAtom> laStream = con.getLightAtomStream();
		Stream<ShadowAtom> saStream = con.getShadowAtomStream();
		
		// determine shadowing info for the view
		ShadowMapFrustum smf = view.getMeta(vn, SMF_ID);
		LightAtom shadowLightAtom = null;
		Frustum shadowFrustum = null;
		Entity shadowEntity = null;
		if (smf != null) {
			shadowFrustum = smf.getFrustum();
			shadowEntity = smf.getLight();
			
			if (shadowFrustum != null) {
				// generate the light atoms for this entity now so it can be skipped later
				// note that null is used for a frustum to force the chosen shadow caster to be built
				if (smf.isSpotLight()) {
					shadowLightAtom = entityBuilder.buildSpotLightAtom(shadowEntity, null, laStream);
					entityBuilder.buildDirectionLightAtom(shadowEntity, f, laStream);
				} else {
					shadowLightAtom = entityBuilder.buildDirectionLightAtom(shadowEntity, null, laStream);
					entityBuilder.buildSpotLightAtom(shadowEntity, f, laStream);
				}
			}
		}
		
		// process all renderables (and shadow casters)
		Entity e;
		Iterator<Entity> it = system.iterator(R_ID);
		while(it.hasNext()) {
			e = it.next();
			if (shadowFrustum != null)
				entityBuilder.buildShadowAtom(e, shadowFrustum, saStream);
			entityBuilder.buildRenderAtom(e, f, raStream);
		}
		
		// process all ambient lights
		it = system.iterator(AL_ID);
		while(it.hasNext())
			entityBuilder.buildAmbientLightAtom(it.next(), f, laStream);
		
		// process all spot lights except for the shadow caster
		it = system.iterator(SL_ID);
		while(it.hasNext()) {
			e = it.next();
			if (e != shadowEntity)
				entityBuilder.buildSpotLightAtom(e, f, laStream);
		}
		
		// process all direction lights except for the shadow caster
		it = system.iterator(DL_ID);
		while(it.hasNext()) {
			e = it.next();
			if (e != shadowEntity)
				entityBuilder.buildDirectionLightAtom(e, f, laStream);
		}
		
		// configure the connection to use the correct views
		con.setShadowLight(shadowFrustum, shadowLightAtom);
		con.setView(vn.getFrustum(), vn.getLeft(), vn.getRight(), vn.getBottom(), vn.getTop());
		
		// finish the queueing up
		con.close();
	}
}
