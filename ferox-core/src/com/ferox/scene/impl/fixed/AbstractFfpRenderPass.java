package com.ferox.scene.impl.fixed;

import com.ferox.math.Color4f;
import com.ferox.math.Frustum;
import com.ferox.math.Matrix4f;
import com.ferox.math.Vector3f;
import com.ferox.math.Vector4f;
import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.renderer.RenderPass;
import com.ferox.renderer.RenderSurface;
import com.ferox.renderer.Renderer;
import com.ferox.resource.Geometry;
import com.ferox.util.Bag;
import com.ferox.util.geom.Box;

public abstract class AbstractFfpRenderPass implements RenderPass {
	// constant configuration variables
	private final String vertexBinding;
	private final String normalBinding;
	private final String texCoordBinding;
	
	protected final RenderMode renderMode;

	// cached variables
	private final Vector4f lightPos;
	private final Vector3f lightDir;
	private final Color4f black;
	private final Color4f dimmedAmbient;
	private final Color4f dimmedLight;
	
	private final Matrix4f view;
	private final Matrix4f modelView;
	private final Matrix4f projection;
	
	private final Geometry defaultGeometry;

	// per-pass variables
	private FixedFunctionRenderer renderer;
	
	protected LightAtom shadowLight;
	protected Bag<RenderAtom> renderAtoms;
	protected Bag<LightAtom> lightAtoms;
	protected Frustum frustum;
	
	public AbstractFfpRenderPass(RenderMode renderMode, String vertexBinding, String normalBinding, 
								 String texCoordBinding) {
		this.renderMode = renderMode;

		this.vertexBinding = vertexBinding;
		this.normalBinding = normalBinding;
		this.texCoordBinding = texCoordBinding;

		// cached variables for computing lighting, etc.
		lightPos = new Vector4f();
		lightDir = new Vector3f();

		black = new Color4f();
		dimmedAmbient = new Color4f();
		dimmedLight = new Color4f();
		
		modelView = new Matrix4f();
		view = new Matrix4f();
		projection = new Matrix4f();
		
		defaultGeometry = new Box(.5f);
	}
	
	public void setPass(Frustum frustum, Bag<RenderAtom> renderAtoms, Bag<LightAtom> lightAtoms, LightAtom shadowLight) {
		this.frustum = frustum;
		this.renderAtoms = renderAtoms;
		this.lightAtoms = lightAtoms;
		this.shadowLight = shadowLight;
	}

	@Override
	public void render(Renderer renderer, RenderSurface surface) {
		if (renderer instanceof FixedFunctionRenderer && frustum != null && renderAtoms != null) {
			this.renderer = (FixedFunctionRenderer) renderer;
			init();
			render(this.renderer);
			this.renderer = null;
		}
		
		frustum = null;
		renderAtoms = null;
		lightAtoms = null;
		shadowLight = null;
	}
	
	private void init() {
		// setup attribute bindings
		renderer.setVertexBinding(vertexBinding);
		renderer.setNormalBinding(normalBinding);
		
		switch(renderMode.getMinimumTextures()) {
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
		
		// get matrix forms from the frustum
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
		renderer.setMaterial(atom.ambient, atom.diffuse, atom.specular, black);
		
		// set textures
		switch(renderMode.getMinimumTextures()) {
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
		renderer.render((geom == null ? defaultGeometry : geom));
		renderer.setModelViewMatrix(view);
		
		// restore view
		renderer.setModelViewMatrix(view);
	}
	
	protected void setLight(int light, LightAtom atom) {
		setLight(light, atom, atom == shadowLight);
	}
	
	protected void setLight(int light, LightAtom atom, boolean asShadow) {
		if (atom == null) {
			// just disable the light and return
			renderer.setLightEnabled(light, false);
			return;
		}
		
		setLightColor(light, atom, asShadow);
		
		if (atom.position != null) {
			if (atom.direction == null) {
				// position light
				lightPos.set(atom.position.x, atom.position.y, atom.position.z, 1f);
				lightDir.set(0f, 0f, -1f);
				
				renderer.setLightPosition(light, lightPos);
				renderer.setSpotlight(light, lightDir, 180f);
			} else if (atom.cutoffAngle >= 0) {
				// spotlight
				lightPos.set(atom.position.x, atom.position.y, atom.position.z, 1f);
				lightDir.set(atom.direction.x, atom.direction.y, atom.direction.z);
				lightDir.normalize();
				
				renderer.setLightPosition(light, lightPos);
				renderer.setSpotlight(light, lightDir, atom.cutoffAngle);
			} else {
				// direction light
				lightPos.set(-atom.direction.x, -atom.direction.y, -atom.direction.z, 0f);
				renderer.setLightPosition(light, lightPos);
			}
		} else if (atom.direction != null) {
			// direction light
			lightPos.set(-atom.direction.x, -atom.direction.y, -atom.direction.z, 0f);
			renderer.setLightPosition(light, lightPos);
		} else {
			// ambient light, which doesn't really use an index, so disable it
			renderer.setLightEnabled(light, false);
		}
	}
	
	protected void setLightColor(int light, LightAtom atom, boolean asShadow) {
		if (atom.position == null && atom.direction == null) {
			// global ambient light
			renderer.setGlobalAmbientLight(atom.diffuse);
		} else {
			// normal light
			if (asShadow) {
				// no specular and slightly dimmed
				dimmedLight.set(.1f * atom.diffuse.getRed(), .1f * atom.diffuse.getGreen(), .1f * atom.diffuse.getBlue(), 1f);
				dimmedAmbient.set(.2f * atom.diffuse.getRed(), .2f * atom.diffuse.getGreen(), .2f * atom.diffuse.getBlue(), 1f);
				renderer.setLightColor(light, black, dimmedLight, black);
			} else {
				// set colors as is, but leave out the ambient
				renderer.setLightColor(light, black, atom.diffuse, atom.specular);
			}
		}
	}
	
	protected abstract void render(FixedFunctionRenderer renderer);
}
