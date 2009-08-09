package com.ferox.scene.fx;

public interface SceneCompositor {
	// this is what will be implemented by the deferred/forward/fixed systems
	// but what general interface will they share?
	// definately a way to specify the scene to be rendered.
	
	// hook it up to a renderer -> probably shouldn't be part of the render()
	// method since we'll want a compositor to be per 1 renderer -> setup at its lifetime
	
	// also required is a way to specify the final RenderSurface, and a way to
	// specify the intermediate surfaces for use -> not needed, handled internally by impls.
	// how about specifying multiple final render targets -> useful, could just be a mapping
	// between views and a surface -> then go through each one, performing all offscreen rendering
	// as needed and then bam! do both
}
