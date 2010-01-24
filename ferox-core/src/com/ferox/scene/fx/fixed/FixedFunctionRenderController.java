package com.ferox.scene.fx.fixed;

import com.ferox.renderer.DisplayOptions;
import com.ferox.renderer.Framework;
import com.ferox.renderer.RenderCapabilities;
import com.ferox.renderer.TextureSurface;
import com.ferox.renderer.DisplayOptions.DepthFormat;
import com.ferox.renderer.DisplayOptions.PixelFormat;
import com.ferox.resource.TextureImage.TextureTarget;
import com.ferox.util.entity.Controller;
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
								numTex > 1 && caps.getVersion() > 1.3f; // FIXME: is this the correct version
								
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
	
	@Override
	public void process() {
		// TODO Auto-generated method stub
		
	}
}
