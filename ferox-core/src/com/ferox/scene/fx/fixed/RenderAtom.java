package com.ferox.scene.fx.fixed;

import java.util.Comparator;

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
	
	public float shininess;
	
	public Color4f diffuse;
	public Color4f specular;
	public Color4f ambient;
	
	public DrawStyle front;
	public DrawStyle back;
	
	public TextureImage primaryTexture;
	public TextureImage decalTexture;
	
	public static final Comparator<RenderAtom> COMPARATOR = new Comparator<RenderAtom>() {
		@Override
		public int compare(RenderAtom o1, RenderAtom o2) {
			// sort first on geometry
			if (o1.geometry != o2.geometry)
				return System.identityHashCode(o1) - System.identityHashCode(o2);
			
			// then on textures
			if (o1.primaryTexture != o2.primaryTexture)
				return System.identityHashCode(o1) - System.identityHashCode(o2);
			if (o1.decalTexture != o2.decalTexture)
				return System.identityHashCode(o1) - System.identityHashCode(o2);
			
			return System.identityHashCode(o1) - System.identityHashCode(o2);
		}
	};
}
