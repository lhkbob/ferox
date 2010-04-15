package com.ferox.scene.ffp;

import java.util.Comparator;

import com.ferox.math.Frustum;
import com.ferox.util.Bag;

public class RenderDescription {
	private static final Comparator<RenderAtom> RA_COMPARATOR = new Comparator<RenderAtom>() {
		@Override
		public int compare(RenderAtom o1, RenderAtom o2) {
			// sort first on geometry
			if (o1.geometry != o2.geometry)
				return System.identityHashCode(o1.geometry) - System.identityHashCode(o2.geometry);
			
			// then on textures
			if (o1.primaryTexture != o2.primaryTexture)
				return System.identityHashCode(o1.primaryTexture) - System.identityHashCode(o2.primaryTexture);
			if (o1.decalTexture != o2.decalTexture)
				return System.identityHashCode(o1.decalTexture) - System.identityHashCode(o2.decalTexture);
			
			return System.identityHashCode(o1) - System.identityHashCode(o2);
		}
	};
	
	private static final Comparator<ShadowAtom> SA_COMPARATOR = new Comparator<ShadowAtom>() {
		@Override
		public int compare(ShadowAtom o1, ShadowAtom o2) {
			if (o1.geometry != o2.geometry)
				return System.identityHashCode(o1.geometry) - System.identityHashCode(o2.geometry);
			return System.identityHashCode(o1) - System.identityHashCode(o2);
		}
	};
	
	private final Bag<RenderAtom> renderAtoms;
	private final Bag<LightAtom> lightAtoms;
	private final Bag<FogAtom> fogAtoms;
	private final Bag<ShadowAtom> shadowAtoms;
	
	private final LightAtom shadowCaster;
	private final Frustum shadowMapFrustum;
	private final Frustum viewFrustum;
	
	private final int x, y, width, height;
	
	public RenderDescription(Bag<RenderAtom> renderAtoms, Bag<LightAtom> lightAtoms, 
							 Bag<FogAtom> fogAtoms, Bag<ShadowAtom> shadowAtoms, 
							 LightAtom shadowCaster, Frustum shadowMapFrustum, Frustum viewFrustum,
							 int x, int y, int width, int height) {
		if (renderAtoms == null || lightAtoms == null || fogAtoms == null || shadowAtoms == null)
			throw new NullPointerException("Atom lists cannot be null");
		if (viewFrustum == null)
			throw new NullPointerException("View Frustum cannot be null");
		if ((shadowMapFrustum == null && shadowCaster != null) || (shadowCaster == null && shadowMapFrustum != null))
			throw new IllegalArgumentException("Shadow caster and shadow map Frustum must both be null, or both non-null");
		
		this.renderAtoms = renderAtoms;
		this.lightAtoms = lightAtoms;
		this.fogAtoms = fogAtoms;
		this.shadowAtoms = shadowAtoms;
		this.shadowCaster = shadowCaster;
		this.shadowMapFrustum = shadowMapFrustum;
		this.viewFrustum = viewFrustum;
		
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		
		this.shadowAtoms.sort(SA_COMPARATOR);
		this.renderAtoms.sort(RA_COMPARATOR);
	}
	
	public int getViewportX() {
		return x;
	}
	
	public int getViewportY() {
		return y;
	}
	
	public int getViewportWidth() {
		return width;
	}
	
	public int getViewportHeight() {
		return height;
	}

	public Bag<RenderAtom> getRenderAtoms() {
		return renderAtoms;
	}

	public Bag<LightAtom> getLightAtoms() {
		return lightAtoms;
	}

	public Bag<FogAtom> getFogAtoms() {
		return fogAtoms;
	}

	public Bag<ShadowAtom> getShadowAtoms() {
		return shadowAtoms;
	}

	public LightAtom getShadowCaster() {
		return shadowCaster;
	}

	public Frustum getShadowMapFrustum() {
		return shadowMapFrustum;
	}

	public Frustum getViewFrustum() {
		return viewFrustum;
	}
}
