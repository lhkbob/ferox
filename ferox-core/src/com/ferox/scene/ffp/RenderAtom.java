package com.ferox.scene.ffp;

import com.ferox.math.Color4f;
import com.ferox.math.Matrix4f;
import com.ferox.math.bounds.AxisAlignedBox;
import com.ferox.renderer.Renderer.DrawStyle;
import com.ferox.resource.Geometry;
import com.ferox.resource.TextureImage;

public class RenderAtom {
	public Geometry geometry;
	public Matrix4f worldTransform;
	public AxisAlignedBox worldBounds;
	
	public boolean lit;
	public boolean receivesShadow;
	
	public float shininess;
	
	public Color4f diffuse;
	public Color4f specular;
	public Color4f ambient;
	
	public DrawStyle front;
	public DrawStyle back;
	
	public TextureImage primaryTexture;
	public TextureImage decalTexture;
	
	// flag for render passes saying if the atom needs an
	// additional pass for shadowing
	public boolean requiresShadowPass;
}
