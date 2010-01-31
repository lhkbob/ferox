package com.ferox.scene.fx.fixed;

import java.util.Iterator;

import com.ferox.math.Color4f;
import com.ferox.math.Frustum;
import com.ferox.renderer.DisplayOptions;
import com.ferox.renderer.Framework;
import com.ferox.renderer.RenderCapabilities;
import com.ferox.renderer.TextureSurface;
import com.ferox.renderer.DisplayOptions.DepthFormat;
import com.ferox.renderer.DisplayOptions.PixelFormat;
import com.ferox.renderer.Renderer.Comparison;
import com.ferox.resource.Geometry;
import com.ferox.resource.TextureImage;
import com.ferox.resource.TextureImage.DepthMode;
import com.ferox.resource.TextureImage.TextureTarget;
import com.ferox.scene.DirectedLight;
import com.ferox.scene.Light;
import com.ferox.scene.SceneElement;
import com.ferox.scene.fx.Renderable;
import com.ferox.scene.fx.ShadowCaster;
import com.ferox.scene.fx.ViewNode;
import com.ferox.scene.fx.ViewNodeController;
import com.ferox.util.Bag;
import com.ferox.util.entity.Component;
import com.ferox.util.entity.Controller;
import com.ferox.util.entity.Entity;
import com.ferox.util.entity.EntitySystem;
import com.ferox.util.entity.Indexable;

public class FixedFunctionRenderController extends Controller {
	private static final int LT_ID = Component.getTypeId(Light.class);
	private static final int DL_ID = Component.getTypeId(DirectedLight.class);
	private static final int SC_ID = Component.getTypeId(ShadowCaster.class);
	
	private static final int VN_ID = Component.getTypeId(ViewNode.class);
	private static final int SE_ID = Component.getTypeId(SceneElement.class);
	private static final int R_ID = Component.getTypeId(Renderable.class);
	
	private static final int TC_ID = Component.getTypeId(TargetComponent.class);
	private static final int RA_ID = Component.getTypeId(RenderAtomComponent.class);
	
	private final Framework framework;
	private final RenderMode renderMode;
	
	private final TextureSurface shadowMap;
	
	private final EntityAtomBuilder entityBuilder;
	private final Bag<LightAtom> lights;
	
	private final String vertexBinding;
	private final String normalBinding;
	private final String texCoordBinding;
	
	private int version;
	
	public FixedFunctionRenderController(EntitySystem system, Framework framework) {
		this(system, framework, 512);
	}
	
	public FixedFunctionRenderController(EntitySystem system, Framework framework, int shadowMapSize) {
		this(system, framework, shadowMapSize, Geometry.DEFAULT_VERTICES_NAME, 
			 Geometry.DEFAULT_NORMALS_NAME, Geometry.DEFAULT_TEXCOORD_NAME);
	}
	
	public FixedFunctionRenderController(EntitySystem system, Framework framework, int shadowMapSize,
										 String vertexBinding, String normalBinding, String texCoordBinding) {
		super(system);
		if (framework == null)
			throw new NullPointerException("Framework cannot be null");
		if (vertexBinding == null || normalBinding == null || texCoordBinding == null)
			throw new NullPointerException("Attribute bindings cannot be null");
		this.framework = framework;
		this.vertexBinding = vertexBinding;
		this.normalBinding = normalBinding;
		this.texCoordBinding = texCoordBinding;
		
		entityBuilder = new EntityAtomBuilder();
		lights = new Bag<LightAtom>();
		
		RenderCapabilities caps = framework.getCapabilities();
		if (!caps.hasFixedFunctionRenderer())
			throw new IllegalArgumentException("Framework must support FixedFunctionRenderer");
		
		int numTex = caps.getMaxFixedPipelineTextures();
		boolean shadowsRequested = shadowMapSize > 0; // size is positive
		boolean shadowSupport = (caps.getFboSupport() || caps.getPbufferSupport()) && 
								numTex > 1 && caps.getVersion() > 1.4f;
								
		RenderMode mode = null;
		if (shadowsRequested && shadowSupport) {
			// choose between DUAL_TEX_SM or SING_TEX_SM
			if (numTex > 2)
				mode = RenderMode.DUAL_TEXTURE_SHADOWMAP;
			else
				mode = RenderMode.SINGLE_TEXTURE_SHADOWMAP;
		}
		
		if (mode == null) {
			// choose between DUAL_TEX_NSM, SING_TEX_NSM, and NO_TEX_NSM
			if (numTex > 1)
				mode = RenderMode.DUAL_TEXTURE_NO_SHADOWMAP;
			else if (numTex == 1)
				mode = RenderMode.SINGLE_TEXTURE_NO_SHADOWMAP;
			else
				mode = RenderMode.NO_TEXTURE_NO_SHADOWMAP;
		}
		renderMode = mode;
		
		if (shadowsRequested && shadowSupport) {
			// convert size to a power of two
			int sz = 1;
			while(sz < shadowMapSize)
				sz = sz << 1;
			// create the shadow map
			shadowMap = framework.createTextureSurface(new DisplayOptions(PixelFormat.NONE, DepthFormat.DEPTH_24BIT), TextureTarget.T_2D, 
																		  sz, sz, 1, 0, 0, false);
			
			// set up the depth comparison
			TextureImage sm = shadowMap.getDepthBuffer();
			sm.setDepthCompareEnabled(true);
			sm.setDepthCompareTest(Comparison.LESS);
			sm.setDepthMode(DepthMode.ALPHA);
		} else {
			// no shadowing is needed
			shadowMap = null;
		}
	}
	
	public Framework getFramework() {
		return framework;
	}
	
	public TextureSurface getShadowMap() {
		return shadowMap;
	}
	
	@Override
	public void process() {
		validate();
		
		version++;
		ShadowMapFrustumController smController = system.getController(ShadowMapFrustumController.class);
		ViewNodeController vnController = system.getController(ViewNodeController.class);
	
		if (vnController != null) {
			processLights();

			Iterator<Entity> views = system.iterator(VN_ID);
			while(views.hasNext()) {
				Entity e = views.next();
				ViewNode vn = (ViewNode) e.get(VN_ID);

				if (smController != null) {
					Frustum smf = smController.getShadowMapFrustum(vn);
					processView(e, vnController.getVisibleEntities(vn.getFrustum()), smController.getShadowMapLight(vn),
								smf, smController.getShadowMapEntities(smf));
				} else {
					processView(e, vnController.getVisibleEntities(vn.getFrustum()), null, null, null);
				}
			}
		}
		
		Iterator<Entity> tcs = system.iterator(TC_ID);
		while(tcs.hasNext()) {
			if (tcs.next().get(VN_ID) == null)
				tcs.remove(); // clean up entities that aren't viewnodes anymore
		}
	}
	
	private void processLights() {
		lights.clear(true);
		Iterator<Entity> l = system.iterator(LT_ID);
		while(l.hasNext()) {
			LightAtom light = getLight(l.next());
			if (light != null)
				lights.add(light);
		}
	}
	
	private LightAtom getLight(Entity l) {
		Light light = (Light) l.get(LT_ID);
		DirectedLight dir = (DirectedLight) l.get(DL_ID);
		SceneElement se = (SceneElement) l.get(SE_ID);
		
		if (light == null)
			return null; // no atom
		LightAtom atom = new LightAtom();
		
		
		float intensity = Math.min(light.getIntensity(), 1f);
		atom.specularExponent = 128 * intensity;
		atom.quadAtt = 1 / (intensity + 1);
		atom.constAtt = 1f;
		atom.linAtt = 0f;
		
		Color4f color = light.getColor();
		atom.specular = new Color4f(intensity * color.getRed(), intensity * color.getGreen(), intensity * color.getBlue(), 1f);
		atom.diffuse = new Color4f(.8f * atom.specular.getRed(), .8f * atom.specular.getGreen(), .8f * atom.specular.getBlue(), 1f);
		
		atom.castsShadows = l.get(SC_ID) != null;
		
		if (se != null) {
			// point light or directed light
			atom.position = se.getTransform().getTranslation();
			atom.worldBounds = se.getWorldBounds();
			
			if (dir != null) {
				atom.direction = se.getTransform().transform(dir.getDirection(), null);
				atom.cutoffAngle = dir.getCutoffAngle();
			}
		} else if (dir != null) {
			// position-less direction light
			atom.direction = dir.getDirection();
			atom.cutoffAngle = -1f;
			
		} // else ambient light
		
		return atom;
	}
	
	private void processView(Entity view, Bag<Entity> visible, Entity shadowLight, Frustum shadowFrustum, Bag<Entity> shadows) {
		ViewNode vn = (ViewNode) view.get(VN_ID);
		TargetComponent tc = (TargetComponent) view.get(TC_ID);
		if (tc == null) {
			tc = new TargetComponent(this);
			view.add(tc);
		}
		
		// handle normal lighting
		LightAtom sl = (shadowLight == null ? null : getLight(shadowLight));
		tc.normalAtoms.clear(true);
		int sz = visible.size();
		for (int i = 0; i < sz; i++) {
			RenderAtom atom = getAtom(visible.get(i));
			if (atom != null)
				tc.normalAtoms.add(atom);
		}
		tc.normalAtoms.sort(RenderAtom.COMPARATOR);
		tc.basePass.setPass(vn.getFrustum(), tc.normalAtoms, lights, sl);
		
		boolean queueShadows = false;
		if (shadowLight != null && renderMode.isShadowsEnabled()) {
			// handle shadows
			tc.shadowAtoms.clear(true);
			sz = shadows.size();
			for (int i = 0; i < sz; i++) {
				RenderAtom atom = getAtom(shadows.get(i));
				if (atom != null && atom.castsShadow)
					tc.shadowAtoms.add(atom);
			}
			tc.shadowAtoms.sort(RenderAtom.COMPARATOR);
			
			tc.shadowPass.setPass(shadowFrustum, tc.shadowAtoms, lights, sl);
			tc.lightPass.setPass(vn.getFrustum(), tc.normalAtoms, lights, sl);
			tc.lightPass.setShadowFrustum(shadowFrustum);
			
			queueShadows = true;
		}
		
		// queue render passes on surface
		if (queueShadows) {
			framework.queue(shadowMap, tc.shadowPass, true, true, false); // must always clear this surface
			framework.queue(vn.getRenderSurface(), tc.basePass);
			framework.queue(vn.getRenderSurface(), tc.lightPass);
		} else {
			framework.queue(vn.getRenderSurface(), tc.basePass);
		}
	}
	
	private RenderAtom getAtom(Entity e) {
		RenderAtomComponent rac = (RenderAtomComponent) e.get(RA_ID);
		if (rac == null) {
			if (e.get(R_ID) == null || e.get(SE_ID) == null)
				return null; // entity is not a renderable or scene element, so ignore

			// new renderable, so make a new render atom for it
			rac = new RenderAtomComponent();
			e.add(rac);
		}
		
		if (rac.version != version) {
			// entity has changed, so make sure everything is up to date
			rac.atom = entityBuilder.build(e, rac.atom);
			if (rac.atom == null) {
				// could not make a valid atom, so don't render this anymore
				e.remove(RA_ID);
				return null;
			}
			
			// store this for later so we don't redo work
			rac.version = version;
		}
		
		return rac.atom;
	}
	
	@Indexable
	private static class TargetComponent extends Component {
		private static final String DESCR = "Internal data used for rendering to a ViewNode";
		
		final ShadowMapPass shadowPass;
		final BaseLightPass basePass;
		final ShadowLightPass lightPass;
		
		final Bag<RenderAtom> shadowAtoms;
		final Bag<RenderAtom> normalAtoms;
		
		public TargetComponent(FixedFunctionRenderController controller) {
			super(DESCR);
			
			normalAtoms = new Bag<RenderAtom>();
			basePass = new BaseLightPass(controller.renderMode, controller.framework.getCapabilities().getMaxActiveLights(), 
										 controller.vertexBinding, controller.normalBinding, controller.texCoordBinding);
			
			if (controller.renderMode.isShadowsEnabled()) {
				// shadows are supported
				shadowAtoms = new Bag<RenderAtom>();
				shadowPass = new ShadowMapPass(controller.renderMode, controller.vertexBinding);
				lightPass = new ShadowLightPass(controller.renderMode, controller.shadowMap.getDepthBuffer(), 
												controller.vertexBinding, controller.normalBinding, controller.texCoordBinding);
			} else {
				// no shadows are supported
				shadowAtoms = null;
				shadowPass = null;
				lightPass = null;
			}
		}
	}
	
	private static class RenderAtomComponent extends Component {
		private static final String DESCR = "Internal data used to associate renderable entities with a RenderAtom";
		
		int version;
		RenderAtom atom;
		
		public RenderAtomComponent() {
			super(DESCR, false);
		}
	}
}
