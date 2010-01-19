package com.ferox.scene;

public class Fog {
	// FIXME: we must figure out what is controllable for a fog component
	// i'd like to describe density in terms of units until opaque and then
	// a fall-off equation, we'd also need fog colors
	//
	// fog w/o scenelement is complete fog everywhere
	// w/ scenelement is limited to that location and renderer must figure out how to address that
	// w/ sceneelement and shape, the fog is constrained to shape if possible (really only useful
	//   in the deferred and maybe glsl renderer)
	//
	// extra components like light transmittance properties can be added later,
	// and be independent or we can just add variables to this class as I learn about them
}
