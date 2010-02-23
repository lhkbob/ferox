package com.ferox.scene.ffp;

import java.util.Iterator;

import javax.media.j3d.Light;

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
import com.ferox.resource.TextureImage.Filter;
import com.ferox.resource.TextureImage.TextureTarget;
import com.ferox.resource.TextureImage.TextureWrap;
import com.ferox.scene.SceneElement;
import com.ferox.scene.ViewNode;
import com.ferox.scene.controller.ViewNodeController;
import com.ferox.util.Bag;
import com.ferox.util.entity.Component;
import com.ferox.util.entity.ComponentId;
import com.ferox.util.entity.Controller;
import com.ferox.util.entity.Entity;
import com.ferox.util.entity.EntitySystem;

public class FixedFunctionRenderController implements Controller {
	private static final ComponentId<ViewNode> VN_ID = Component.getComponentId(ViewNode.class);
	
	private final EntityAtomBuilder entityBuilder;
	private final FixedFunctionAtomRenderer renderer;
	
	public FixedFunctionRenderController(Framework framework) {
		this(framework, 512);
	}
	
	public FixedFunctionRenderController(Framework framework, int shadowMapSize) {
		this(framework, shadowMapSize, Geometry.DEFAULT_VERTICES_NAME, 
			 Geometry.DEFAULT_NORMALS_NAME, Geometry.DEFAULT_TEXCOORD_NAME);
	}
	
	public FixedFunctionRenderController(Framework framework, int shadowMapSize,
										 String vertexBinding, String normalBinding, String texCoordBinding) {
		renderer = new FixedFunctionAtomRenderer(framework, shadowMapSize, vertexBinding, normalBinding, texCoordBinding);
		entityBuilder = new EntityAtomBuilder();
	}
	
	public Framework getFramework() {
		return framework;
	}
	
	public TextureSurface getShadowMap() {
		return shadowMap;
	}
	
	@Override
	public void process(EntitySystem system) {
		LightAndFrustum shadowLight = system.getResults().getResult(ShadowMapFrustumController.class);
		
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
}
