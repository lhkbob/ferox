/**
 * 
 */
package com.ferox.scene.impl.fixed;

enum RenderMode {
	NO_TEXTURE_NO_SHADOWMAP(0, false),
	SINGLE_TEXTURE_NO_SHADOWMAP(1, false),
	DUAL_TEXTURE_NO_SHADOWMAP(2, false),
	SINGLE_TEXTURE_SHADOWMAP(1, true),
	DUAL_TEXTURE_SHADOWMAP(2, true);
	
	private int numTex; private boolean shadows;
	private RenderMode(int numTex, boolean shadows) {
		this.numTex = numTex; this.shadows = shadows;
	}
	
	public int getMinimumTextures() { return numTex; }
	
	public boolean isShadowsEnabled() { return shadows; }
}