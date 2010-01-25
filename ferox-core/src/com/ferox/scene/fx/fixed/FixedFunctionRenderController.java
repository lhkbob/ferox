package com.ferox.scene.fx.fixed;

import java.util.Iterator;

import com.ferox.renderer.DisplayOptions;
import com.ferox.renderer.Framework;
import com.ferox.renderer.RenderCapabilities;
import com.ferox.renderer.TextureSurface;
import com.ferox.renderer.DisplayOptions.DepthFormat;
import com.ferox.renderer.DisplayOptions.PixelFormat;
import com.ferox.renderer.Renderer.Comparison;
import com.ferox.resource.TextureImage;
import com.ferox.resource.TextureImage.DepthMode;
import com.ferox.resource.TextureImage.TextureTarget;
import com.ferox.scene.SceneElement;
import com.ferox.scene.fx.ViewNode;
import com.ferox.scene.fx.ViewNodeController;
import com.ferox.util.Bag;
import com.ferox.util.entity.Component;
import com.ferox.util.entity.Controller;
import com.ferox.util.entity.Entity;
import com.ferox.util.entity.EntitySystem;

public class FixedFunctionRenderController extends Controller {
	private static enum RenderMode {
		NO_TEXTURE_NO_SHADOWMAP(0, false),
		SINGLE_TEXTURE_NO_SHADOWMAP(1, false),
		DUAL_TEXTURE_NO_SHADOWMAP(2, false),
		SINGLE_TEXTURE_SHADOWMAP(1, true),
		DUAL_TEXTURE_SHADOWMAP(2, true);
		
		private int numTex; private boolean shadows;
		private RenderMode(int numTex, boolean shadows) {
			this.numTex = numTex; this.shadows = shadows;
		}
		
		public int getMinimumTextures() { return numTex; }
		
		public boolean getShadowsEnabled() { return shadows; }
	}
	
	private static final int VN_ID = Component.getTypeId(ViewNode.class);
	private static final int SE_ID = Component.getTypeId(SceneElement.class);
	private static final int RA_ID = Component.getTypeId(RenderAtomComponent.class);
	
	private final Framework framework;
	private final RenderMode renderMode;
	
	private final int shadowMapSize;
	private final TextureSurface shadowMap;
	
	private String verticesAttribute;
	private String normalsAttribute;
	private String texCoordsAttribute;
	
	public FixedFunctionRenderController(EntitySystem system, Framework framework, int shadowMapSize) {
		super(system);
		if (framework == null)
			throw new NullPointerException("Framework cannot be null");
		this.framework = framework;

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
			this.shadowMapSize = sz;
			// create the shadow map
			shadowMap = framework.createTextureSurface(new DisplayOptions(PixelFormat.NONE, DepthFormat.DEPTH_24BIT), TextureTarget.T_2D, 
																		  sz, sz, 1, 0, 0, false);
			TextureImage sm = shadowMap.getDepthBuffer();
			sm.setDepthCompareEnabled(true);
			sm.setDepthCompareTest(Comparison.LESS);
			sm.setDepthMode(DepthMode.ALPHA);
		} else {
			// no shadowing is needed
			this.shadowMapSize = -1;
			shadowMap = null;
		}
	}
	
	public String getTextureCoordinateBinding() {
		return texCoordsAttribute;
	}
	
	public String getNormalBinding() {
		return normalsAttribute;
	}
	
	public String getVertexBinding() {
		return verticesAttribute;
	}
	
	public int getShadowMapSize() {
		return shadowMapSize;
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
		
		ShadowMapFrustumController smController = system.getController(ShadowMapFrustumController.class);
		ViewNodeController vnController = system.getController(ViewNodeController.class);
		
		Iterator<Entity> views = system.iterator(VN_ID);
		while(views.hasNext()) {
			ViewNode vn = (ViewNode) views.next().get(VN_ID);
			Bag<Entity> visible = vnController.getVisibleEntities(vn.getFrustum());
			// TODO: how should these render atoms be stored? we could maintain parallel render atom bags
			// but these would need to be pooled to be efficient
			// I was thinking making RA a linked structure, but that actually WONT work because we have
			// to have the same RA in multiple lists at a time
			
			// FIXME: generate render atom bag for main frustum
			// FIXME: generate render atom bag for shadow map light, if it's not null
			// FIXME: setup render pass for shadow map pass, if light is picked
			// FIXME: setup render pass for base light pass
			// FIXME: setup render pass for shadowed lights, if light is picked
			
			// FIXME: queue those passes in order
			// TODO: determine whether or not passes are light weight, discardable objects
			// or if I should cache them between frames
		}
	}
	
	private RenderAtom getAtom(Entity e) {
		RenderAtomComponent rac = (RenderAtomComponent) e.get(RA_ID);
		if (rac == null) {
			// FIXME: make sure it's a renderable here to avoid problems, same with SceneElement
			// new renderable, so make a new render atom for it
			rac = new RenderAtomComponent();
			e.add(rac);
		}
		
		if (rac.entityHash != e.getComponentHash()) {
			// entity has changed, so make sure everything is up to date
			// FIXME: check for blinnPhong model or solid lighting model, and set colors
			// FIXME: if has BP model, then set lighting to true
			// FIXME: set casts/receivesShadows based on presence of ShadowCaster and ShadowReceiver
			// FIXME: set geometry based on Shape, or (discard/use default shape?? if no Shape)
			// FIXME: discard if no longer a Renderable
			// FIXME: set world transform and bounds based on SceneElement (discard if no longer SceneElement)
			// FIXME: set textures to null, or correct images if has a TexturedMaterial
			
			// FIXME: discard involves removing rac from entity, and then returning null early
			
			
			// store this for later so we don't redo work
			rac.entityHash = e.getComponentHash();
		}
		
		return rac.atom;
	}
	
	private static class RenderAtomComponent extends Component {
		private static final String DESCR = "Internal data used to associate renderable entities with a RenderAtom";
		
		int entityHash;
		final RenderAtom atom;
		
		public RenderAtomComponent() {
			super(DESCR, false);
			atom = new RenderAtom();
		}
	}
}
