package com.ferox.scene;

import java.util.concurrent.ExecutionException;

import com.ferox.renderer.FrameStatistics;
import com.ferox.renderer.Framework;
import com.ferox.resource.Geometry;
import com.ferox.scene.impl.fixed.FixedFunctionRenderController;
import com.ferox.scene.impl.fixed.ShadowMapFrustumController;
import com.ferox.util.entity.EntitySystem;

public class SceneCompositorFactory {
	private String vertexBinding;
	private String normalBinding;
	private String texCoordBinding;
	
	private Framework framework;
	
	private int shadowMapSize;
	
	public SceneCompositorFactory() {
		this(null);
	}
	
	public SceneCompositorFactory(Framework framework) {
		this(framework, 1024);
	}
	
	public SceneCompositorFactory(Framework framework, int shadowMapSize) {
		this(framework, shadowMapSize, Geometry.DEFAULT_VERTICES_NAME, 
			 Geometry.DEFAULT_NORMALS_NAME, Geometry.DEFAULT_TEXCOORD_NAME);
	}
	
	public SceneCompositorFactory(Framework framework, int shadowMapSize, String vertexBinding, 
						   String normalBinding, String texCoordBinding) {
		setFramework(framework);
		setVertexBinding(vertexBinding);
		setNormalBinding(normalBinding);
		setTextureCoordinateBinding(texCoordBinding);
		setShadowMapSize(shadowMapSize);
	}
	
	public SceneCompositorFactory setVertexBinding(String vertexBinding) {
		this.vertexBinding = vertexBinding;
		return this;
	}
	
	public SceneCompositorFactory setNormalBinding(String normalBinding) {
		this.normalBinding = normalBinding;
		return this;
	}
	
	public SceneCompositorFactory setTextureCoordinateBinding(String texCoordBinding) {
		this.texCoordBinding = texCoordBinding;
		return this;
	}
	
	public SceneCompositorFactory setFramework(Framework framework) {
		if (framework != null && !framework.getCapabilities().hasFixedFunctionRenderer())
			throw new IllegalArgumentException("Framework does not support a FixedFunctionRenderer");
		this.framework = framework;
		return this;
	}
	
	public SceneCompositorFactory setShadowMapSize(int size) {
		shadowMapSize = size;
		return this;
	}
	
	public SceneCompositor build() {
		if (framework == null)
			throw new IllegalStateException("Framework has not been configured");
		
		if (vertexBinding == null)
			vertexBinding = Geometry.DEFAULT_VERTICES_NAME;
		if (normalBinding == null)
			normalBinding = Geometry.DEFAULT_NORMALS_NAME;
		if (texCoordBinding == null)
			texCoordBinding = Geometry.DEFAULT_TEXCOORD_NAME;
		
		return new SceneCompositorImpl(framework, shadowMapSize, vertexBinding, normalBinding, texCoordBinding);
	}
	
	private static class SceneCompositorImpl implements SceneCompositor {
		private final SceneController sceneController;
		private final ViewNodeController viewNodeController;
		private final ShadowMapFrustumController shadowMapController;
		private final FixedFunctionRenderController renderController;
		
		private final Framework framework;
		private final EntitySystem system;
		
		public SceneCompositorImpl(Framework framework, int shadowMapSize, 
								   String vertexBinding, String normalBinding, String texCoordBinding) {
			this.framework = framework;
			
			system = new EntitySystem();
			sceneController = new SceneController(system);
			viewNodeController = new ViewNodeController(system);
			renderController = new FixedFunctionRenderController(system, framework, shadowMapSize, vertexBinding, normalBinding, texCoordBinding);
			
			if (renderController.getShadowMap() != null)
				shadowMapController = new ShadowMapFrustumController(system);
			else
				shadowMapController = null;
		}

		@Override
		public EntitySystem getEntitySystem() {
			return system;
		}

		@Override
		public Framework getFramework() {
			return framework;
		}

		@Override
		public FrameStatistics render(boolean queueOnly) {
			// process everything
			if (sceneController != system.getController(SceneController.class))
				throw new IllegalStateException("Managed SceneController has been removed");
			sceneController.process();
			
			if (viewNodeController != system.getController(ViewNodeController.class))
				throw new IllegalStateException("Managed ViewNodeController has been removed");
			viewNodeController.process();
			
			if (shadowMapController != system.getController(ShadowMapFrustumController.class))
				throw new IllegalStateException("Managed ShadowMapFrustumController has been removed");
			if (shadowMapController != null)
				shadowMapController.process();
			
			if (renderController != system.getController(FixedFunctionRenderController.class))
				throw new IllegalStateException("Managed FixedFunctionRenderController has been removed");
			renderController.process();
			
			if (!queueOnly) {
				try {
					return framework.render().get();
				} catch (InterruptedException e) {
					throw new RuntimeException("Exception occured while rendering", e);
				} catch (ExecutionException e) {
					throw new RuntimeException("Exception occured while rendering", e);
				}
			} else
				return null;
		}
	}
}
