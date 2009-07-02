package com.ferox.renderer;

/**
 * <p>
 * A ResourceManager can be used to have application specific control over how
 * resources and geometries are updated and cleaned. It provides much greater
 * flexibility than just using the Framework's requestUpdate() and
 * requestCleanUp() methods.
 * </p>
 * <p>
 * Multiple resource managers can be used in conjunction, perhaps one specific
 * to a type. It is theoretically possible to implement a streaming system with
 * the control that the resource manager gives you.
 * </p>
 * 
 * @author Michael Ludwig
 */
public interface ResourceManager {
	/**
	 * This is invoked every frame by the renderer. When this method is called,
	 * it is ok to call the actual update() and cleanUp() methods of the given
	 * renderer. These methods will perform the low-level operations
	 * immediately, so care should be given to make sure work isn't duplicated
	 * or wasted (e.g. update something, then clean it up in the same frame).
	 * 
	 * @param renderer The Renderer to manage resources for
	 */
	public void manage(Renderer renderer);
}
