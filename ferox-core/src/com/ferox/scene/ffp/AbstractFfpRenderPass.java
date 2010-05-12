package com.ferox.scene.ffp;

import com.ferox.math.Color4f;
import com.ferox.math.Matrix4f;
import com.ferox.math.Vector3f;
import com.ferox.math.Vector4f;
import com.ferox.math.bounds.Frustum;
import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.renderer.RenderPass;
import com.ferox.renderer.RenderSurface;
import com.ferox.renderer.Renderer;
import com.ferox.resource.Geometry;

public abstract class AbstractFfpRenderPass implements RenderPass {
	protected static final Color4f BLACK = new Color4f(0f, 0f, 0f, 1f);
	
	// constant configuration variables
	protected final String vertexBinding;
	protected final String normalBinding;
	protected final String texCoordBinding;
	
	protected final int maxMaterialTexUnits;
	
	// cached variables
	private final Vector4f lightPos;
	private final Vector3f lightDir;
	private final Color4f dimmedAmbient;
	private final Color4f dimmedLight;
	
	private final Matrix4f view;
	private final Matrix4f modelView;
	private final Matrix4f projection;
	
	// per-pass variables
	private FixedFunctionRenderer renderer;
	private Fence fence;
	protected RenderDescription renderDescr;
	
	public AbstractFfpRenderPass(int maxMaterialTexUnits, String vertexBinding, String normalBinding, 
								 String texCoordBinding) {
		this.maxMaterialTexUnits = maxMaterialTexUnits;

		this.vertexBinding = vertexBinding;
		this.normalBinding = normalBinding;
		this.texCoordBinding = texCoordBinding;

		// cached variables for computing lighting, etc.
		lightPos = new Vector4f();
		lightDir = new Vector3f();

		dimmedAmbient = new Color4f();
		dimmedLight = new Color4f();
		
		modelView = new Matrix4f();
		view = new Matrix4f();
		projection = new Matrix4f();
	}
	
	public void setFence(Fence fence) {
	    this.fence = fence;
	}
	
	public void setRenderDescription(RenderDescription renderDescr) {
		this.renderDescr = renderDescr;
	}

	@Override
	public void render(Renderer renderer, RenderSurface surface) {
	    if (fence != null)
            fence.onRenderBatchStart();
	    
		if (renderer instanceof FixedFunctionRenderer && getFrustum() != null) {
			// render only when we have an ffp renderer, the description is complete
			// and this pass can determine a proper frustum to use
			this.renderer = (FixedFunctionRenderer) renderer;
			init(surface);
			render(this.renderer);
			this.renderer = null;
		}
		
		renderDescr = null; // reset
		if (fence != null)
            fence.onRenderBatchEnd();
	}
	
	private void init(RenderSurface surface) {
		// setup attribute bindings
		renderer.setVertexBinding(vertexBinding);
		renderer.setNormalBinding(normalBinding);
		
		switch(maxMaterialTexUnits) {
		case 2:
			renderer.setTextureCoordinateBinding(1, texCoordBinding);
		case 1:
			renderer.setTextureCoordinateBinding(0, texCoordBinding);
			break;
		case 0:
			// no textures, so just disable everything
			renderer.setTextureCoordinateBinding(0, null);
			break;
		}
		
		// delegate to configure viewport, and then possibly clear
		if (initViewport(renderer))
			renderer.clear(true, true, true, surface.getClearColor(), surface.getClearDepth(), surface.getClearStencil());
		
		// get matrix forms from the frustum
		Frustum frustum = getFrustum();
		frustum.getProjectionMatrix(projection);
		frustum.getViewMatrix(view);
		
		// set projection matrix and view matrices on renderer
		renderer.setProjectionMatrix(projection);
		renderer.setModelViewMatrix(view);
	}
	
	protected void renderAtom(RenderAtom atom) {
		// set draw mode
		renderer.setDrawStyle(atom.front, atom.back);
		
		// set materials
		renderer.setLightingEnabled(atom.lit);
		renderer.setMaterialShininess(atom.shininess);
		renderer.setMaterial(atom.ambient, atom.diffuse, atom.specular, BLACK);
		
		// set textures
		switch(maxMaterialTexUnits) {
		case 2:
			if (atom.decalTexture != null) {
				renderer.setTexture(1, atom.decalTexture);
				renderer.setTextureEnabled(1, true);
			} else
				renderer.setTextureEnabled(1, false);
			// fall through
		case 1:
			if (atom.primaryTexture != null) {
				renderer.setTexture(0, atom.primaryTexture);
				renderer.setTextureEnabled(0, true);
			} else
				renderer.setTextureEnabled(0, false);
			break;
		}
		
		// render it
		renderGeometry(atom.geometry, atom.worldTransform);
	}
	
	protected void renderGeometry(Geometry geom, Matrix4f toWorld) {
		// compute the whole transform
		view.mul(toWorld, modelView);
		renderer.setModelViewMatrix(modelView);
		
		// render the geom
		renderer.render(geom);
		
		// restore view
		renderer.setModelViewMatrix(view);
	}
	
	protected void setLight(int light, LightAtom atom) {
		setLight(light, atom, atom == renderDescr.getShadowCaster());
	}
	
	protected void setLight(int light, LightAtom atom, boolean asShadow) {
		if (atom == null) {
			// just disable the light and return
			renderer.setLightEnabled(light, false);
			return;
		}
		
		switch(atom.type) {
		case AMBIENT:
			// ambient lights don't use an index so disable the unit
			renderer.setGlobalAmbientLight(atom.diffuse);
			renderer.setLightEnabled(light, false);
			break;
		case DIRECTION:
			setLightColor(light, atom, asShadow);
			lightPos.set(-atom.direction.x, -atom.direction.y, -atom.direction.z, 0f);
			renderer.setLightPosition(light, lightPos);
			renderer.setLightEnabled(light, true);
			break;
		case SPOTLIGHT:
			// this can also be a point light, in which case direction is ignored by OpenGL
			setLightColor(light, atom, asShadow);
			lightPos.set(atom.position.x, atom.position.y, atom.position.z, 1f);
			lightDir.set(atom.direction.x, atom.direction.y, atom.direction.z).normalize();

			renderer.setLightPosition(light, lightPos);
			renderer.setSpotlight(light, lightDir, atom.cutoffAngle);
			renderer.setLightEnabled(light, true);
			break;
		}
	}
	
	protected void setLightColor(int light, LightAtom atom, boolean asShadow) {
		if (asShadow) {
			// no specular highlight and dim other light contributions
			dimmedLight.set(.1f * atom.diffuse.getRed(), .1f * atom.diffuse.getGreen(), .1f * atom.diffuse.getBlue(), 1f);
			dimmedAmbient.set(.2f * atom.diffuse.getRed(), .2f * atom.diffuse.getGreen(), .2f * atom.diffuse.getBlue(), 1f);
			renderer.setLightColor(light, BLACK, BLACK, BLACK);
		} else {
			// set colors
			renderer.setLightColor(light, BLACK, atom.diffuse, atom.specular);
		}
		
		// set linear attenuation
		renderer.setLightAttenuation(light, atom.constCutoff, atom.linCutoff, atom.quadCutoff);
	}
	
	protected abstract void render(FixedFunctionRenderer renderer);
	
	protected abstract Frustum getFrustum();
	
	protected abstract boolean initViewport(FixedFunctionRenderer renderer);
}
