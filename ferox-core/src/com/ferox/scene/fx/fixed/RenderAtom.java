package com.ferox.scene.fx.fixed;

import com.ferox.math.Color4f;
import com.ferox.math.Matrix4f;
import com.ferox.renderer.Renderer.DrawStyle;
import com.ferox.resource.Geometry;


public class RenderAtom {
	public Geometry geometry;
	public Matrix4f worldTransform;
	
	public boolean lit;
	public boolean castsShadow;
	public boolean receivesShadow;
	
	public Color4f diffuse;
	public Color4f specular;
	public Color4f ambient;
	
	public DrawStyle front;
	public DrawStyle back;
}
