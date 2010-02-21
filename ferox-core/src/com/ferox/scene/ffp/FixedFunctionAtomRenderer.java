package com.ferox.scene.ffp;

import java.util.HashMap;
import java.util.Map;

import com.ferox.renderer.DisplayOptions;
import com.ferox.renderer.Framework;
import com.ferox.renderer.RenderCapabilities;
import com.ferox.renderer.RenderSurface;
import com.ferox.renderer.TextureSurface;
import com.ferox.renderer.DisplayOptions.DepthFormat;
import com.ferox.renderer.DisplayOptions.PixelFormat;
import com.ferox.renderer.Renderer.Comparison;
import com.ferox.resource.TextureImage;
import com.ferox.resource.TextureImage.DepthMode;
import com.ferox.resource.TextureImage.Filter;
import com.ferox.resource.TextureImage.TextureTarget;
import com.ferox.resource.TextureImage.TextureWrap;

public class FixedFunctionAtomRenderer {
	private final Map<RenderSurface, RenderConnection> connections;
	private final Framework framework;
	
	private final TextureSurface shadowMap;
	private final int maxMaterialTexUnits; // 0, 1, or 2
	
	private final String vertexBinding;
	private final String normalBinding;
	private final String texCoordBinding;
	
	public FixedFunctionAtomRenderer(Framework framework, int shadowMapSize, 
									 String vertexBinding, String normalBinding, String texCoordBinding) {
		if (framework == null)
			throw new NullPointerException("Framework cannot be null");
		if (vertexBinding == null || normalBinding == null || texCoordBinding == null)
			throw new NullPointerException("Attribute bindings cannot be null");
		
		RenderCapabilities caps = framework.getCapabilities();
		if (!caps.hasFixedFunctionRenderer())
			throw new IllegalArgumentException("Framework must support a FixedFunctionRenderer");
		
		this.framework = framework;
		this.vertexBinding = vertexBinding;
		this.normalBinding = normalBinding;
		this.texCoordBinding = texCoordBinding;
		
		int numTex = caps.getMaxFixedPipelineTextures();
		boolean shadowsRequested = shadowMapSize > 0; // size is positive
		boolean shadowSupport = (caps.getFboSupport() || caps.getPbufferSupport()) && 
								numTex > 1 && caps.getVersion() > 1.4f;
						
		if (shadowsRequested && shadowSupport) {
			maxMaterialTexUnits = (numTex > 2 ? 2 : 1);
			// convert size to a power of two
			int sz = 1;
			while(sz < shadowMapSize)
				sz = sz << 1;
			// create the shadow map
			shadowMap = framework.createTextureSurface(new DisplayOptions(PixelFormat.NONE, DepthFormat.DEPTH_24BIT), TextureTarget.T_2D, 
																		  sz, sz, 1, 0, 0, false);
			
			// set up the depth comparison
			TextureImage sm = shadowMap.getDepthBuffer();
			sm.setFilter(Filter.LINEAR);
			sm.setWrapSTR(TextureWrap.CLAMP);
			sm.setDepthCompareEnabled(true);
			sm.setDepthCompareTest(Comparison.LEQUAL);
			sm.setDepthMode(DepthMode.ALPHA);
		} else {
			maxMaterialTexUnits = Math.min(2, numTex);
			shadowMap = null;
		}
		
		connections = new HashMap<RenderSurface, RenderConnection>();
	}
	
	public RenderConnection getConnection(RenderSurface surface) {
		return null;
	}
	
	public void queueAll(RenderSurface surface) {
		
	}
	
	public Framework getFramework() {
		return null;
	}
}
