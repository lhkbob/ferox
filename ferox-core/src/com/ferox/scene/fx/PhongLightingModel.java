package com.ferox.scene.fx;

import com.ferox.math.Color4f;

public class PhongLightingModel extends LightingModel {
	private final Color4f ambient;
	private final Color4f diffuse;
	private final Color4f specular;
	
	private float shininess; // 0 to 1, 1 = fully shiny
	
	public PhongLightingModel(Color4f ambDiffuse, boolean shadowReceiver) {
		this(ambDiffuse, null, shadowReceiver);
	}
	
	public PhongLightingModel(Color4f ambDiffuse, Color4f specular, boolean shadowReceiver) {
		this(ambDiffuse, ambDiffuse, specular, shadowReceiver);
	}
	
	public PhongLightingModel(Color4f ambient, Color4f diffuse, Color4f specular, boolean shadowReceiver) {
		super(shadowReceiver);
		this.ambient = new Color4f();
		this.diffuse = new Color4f();
		this.specular = new Color4f();
		
		setAmbient(ambient);
		setDiffuse(diffuse);
		setSpecular(specular);
		
		setShininess(0f);
	}
	
	public void setAmbient(Color4f ambient) {
		if (ambient == null)
			this.ambient.set(.2f, .2f, .2f, 1f);
		else
			this.ambient.set(ambient.getRed(), ambient.getGreen(), ambient.getBlue(), ambient.getAlpha());
	}
	
	public Color4f getAmbient() {
		return ambient;
	}
	
	public void setDiffuse(Color4f diffuse) {
		if (diffuse == null)
			this.diffuse.set(.8f, .8f, .8f, 1f);
		else
			this.diffuse.set(diffuse.getRed(), diffuse.getGreen(), diffuse.getBlue(), diffuse.getAlpha());
	}
	
	public Color4f getDiffuse() {
		return diffuse;
	}
	
	public void setSpecular(Color4f specular) {
		if (specular == null)
			this.specular.set(.5f, .5f, .5f, 1f);
		else
			this.specular.set(specular.getRed(), specular.getGreen(), specular.getBlue(), specular.getAlpha());
	}
	
	public Color4f getSpecular() {
		return specular;
	}
	
	public void setShininess(float shininess) {
		this.shininess = Math.max(0f, Math.min(shininess, 1f));
	}
	
	public float getShininess() {
		return shininess;
	}
}
