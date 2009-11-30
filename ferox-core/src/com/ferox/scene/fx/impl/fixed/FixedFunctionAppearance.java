package com.ferox.scene.fx.impl.fixed;

import com.ferox.scene.fx.Appearance;
import com.ferox.scene.fx.LightingModel;
import com.ferox.scene.fx.PhongLightingModel;
import com.ferox.scene.fx.ShadowCaster;
import com.ferox.scene.fx.SolidLightingModel;
import com.ferox.scene.fx.TextureUnit;
import com.ferox.scene.fx.TexturedMaterial;
import com.ferox.scene.fx.impl.fixed.FixedFunctionSceneCompositor.RenderMode;
import com.ferox.shader.Material;
import com.ferox.shader.MultiTexture;
import com.ferox.shader.TextureEnvironment;
import com.ferox.shader.TextureEnvironment.EnvironmentMode;
import com.ferox.shader.TextureEnvironment.TexCoordGeneration;

public class FixedFunctionAppearance {
	private final RenderMode renderMode;
	
	private Material material;
	private MultiTexture textures;
	
	private boolean castsShadows;
	private boolean receivesShadows;
	
	private boolean enableLighting;
	
	public FixedFunctionAppearance(RenderMode rm, Appearance a) {
		if (rm == null)
			throw new NullPointerException("Must specify non-null RenderMode");
		if (a == null)
			throw new NullPointerException("Cannot compile a null Appearance");
		
		renderMode = rm;
		this.recompile(a);
	}
	
	public void clean() {
		material = null;
		textures = null;
	}
	
	public void recompile(Appearance a) {
		if (a == null)
			throw new NullPointerException("Cannot compile a null appearance");
		
		LightingModel lightModel = a.get(LightingModel.class);
		TexturedMaterial mat = a.get(TexturedMaterial.class);
		
		// control parameters
		castsShadows = renderMode.getShadowsEnabled() && a.get(ShadowCaster.class) != null;
		receivesShadows = renderMode.getShadowsEnabled() && lightModel != null && lightModel.isShadowReceiver();
		enableLighting = lightModel instanceof PhongLightingModel;
		
		// evaluate textures
		if (mat != null && renderMode.getMinimumTextures() > 0) {
			if (textures == null)
				textures = new MultiTexture();
			
			TextureUnit newPrimary = mat.getPrimaryTexture();
			TextureUnit newDecal = mat.getDecalTexture();
			
			// try to re-use texture objects
			TextureEnvironment primary = (newPrimary == null ? null : textures.getTexture(newPrimary.getUnit()));
			TextureEnvironment decal = (newDecal == null ? null : textures.getTexture(newDecal.getUnit()));
			
			// clear out any old ones
			textures.clearTextures();
			
			if (newPrimary != null) {
				primary = (primary == null ? new TextureEnvironment() : primary);
				primary.setTexture(newPrimary.getTexture());
				primary.setTextureEnvMode(TextureEnvMode.MODULATE);
				primary.setTexCoordGenSTR(TexCoordMode.NONE);
				
				textures.setTexture(newPrimary.getUnit(), primary);
			}
			
			if (newDecal != null && renderMode.getMinimumTextures() > 1) {
				decal = (decal == null ? new TextureEnvironment() : decal);
				decal.setTexture(newDecal.getTexture());
				decal.setTextureEnvMode(TextureEnvMode.DECAL);
				decal.setTexCoordGenSTR(TexCoordMode.NONE);
				
				textures.setTexture(newDecal.getUnit(), decal);
			}
		} else {
			textures = null;
		}
		
		// evaluate lighting model
		if (material == null)
			material = new Material();
		
		if (lightModel instanceof PhongLightingModel) {
			// set up the material to mimic the light model
			PhongLightingModel lm = (PhongLightingModel) lightModel;
			material.setAmbient(lm.getAmbient());
			material.setDiffuse(lm.getDiffuse());
			material.setSpecular(lm.getSpecular());
			material.setShininess(convertShininess(lm.getShininess()));
			material.setSmoothShaded(true);
		} else if (lightModel instanceof SolidLightingModel) {
			// only really care about the diffuse
			material.setDiffuse(((SolidLightingModel) lightModel).getColor());
		} else {
			// reset to the default
			material.setAmbient(null);
			material.setDiffuse(null);
			material.setSpecular(null);
			material.setShininess(5f);
			material.setSmoothShaded(true);
		}
	}
	
	private float convertShininess(float shiny) {
		return (1 - Math.min(shiny, .95f)) * 128;
	}
	
	public Material getMaterial() {
		return material;
	}
	
	public MultiTexture getTextures() {
		return textures;
	}
	
	public boolean isShadowCaster() {
		return castsShadows;
	}
	
	public boolean isShadowReceiver() {
		return receivesShadows;
	}
	
	public boolean isLightingEnabled() {
		return enableLighting;
	}
}
