package com.ferox.scene.fx.fixed;

import com.ferox.math.Color4f;
import com.ferox.math.Matrix4f;
import com.ferox.math.bounds.BoundVolume;
import com.ferox.renderer.Renderer.DrawStyle;
import com.ferox.resource.Geometry;
import com.ferox.resource.TextureImage;


public class RenderAtom {
	public Geometry geometry;
	public Matrix4f worldTransform;
	public BoundVolume worldBounds;
	
	public boolean lit;
	public boolean castsShadow;
	public boolean receivesShadow;
	
	public Color4f diffuse;
	public Color4f specular;
	public Color4f ambient;
	
	public DrawStyle front;
	public DrawStyle back;
	
	public TextureImage primaryTexture;
	public TextureImage decalTexture;
}
