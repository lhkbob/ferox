package com.ferox.scene2.fx;

public class LightingModel {
	// for simple renderings, or ui elements, i'll just use render atoms and bag<effects> directly

	// for things that take textures, I think they should just be TextureImages since 
	// the environment is defined by the containing component
	
	// the primary passes will have to take into account the majority of these components
	// and how to combine them: (lighting model, shadows, surface bumping, textures + effects) go together
	// transparency + similar also goes together with above (but split into a later pass)
	// shadow mapping must take into account effects + vertex components, etc. etc.
	
	// then there are the fullscene passes, some of which can be merged with the primary
	// rendering for efficiency, others are just on their own.
	
	// super class allows for shadow receiving and that's it
	// possible subclasses include:
	// light models must contain material color
	// - PhongLightingModel (other lighting models?)
	// - ToonLightingModel

	// other components will be:
	// - ShadowCaster - it's just a tag component (type determined by system config)
	// - BumpmapSurface - holds onto the normal map
	// --- ParallaxBumpmapSurface - adds a depth map??
	
	// components directly mapping onto effects:
	// - depth testing / stencils / polygon styles (some of this goes away in newer gl's I think)
	
	// components highly related to bumpmapping
	// - SpecularTexture - only 1 tex - holds specularity factor/rgb
	// - DiffuseTextures - allows for multiple tex's
	
	// - ReflectionMappedSurface - reflectence properties (can be used for both opaque and transparent surfaces)
	
	// - Glow? - this is related to bloom and hdr (could just be part of that)
	// - TransparentSurface -> signals that it should be rendered specially
	// ---- SubsurfacePenetrationTransparentSurface??
	// ---- WaterBasedTransparentSurface?? - also adds foam texture? + mix with reflection surface component
	//         to achieve good rendering
	
	// of course there will also be vertex deformations (or similar):
	// - billboarded entity
	// - vertex skinned entity
	// - water wave specifier - could be part of the water-based component that describes the wave animated texture
	//       water about projected grid systems??, that's a mesh update seems like a Geometry type and could come with
	//       a parallel-grid-wave-component that animates the projected grid for the waves (would have the same family as above)
}
