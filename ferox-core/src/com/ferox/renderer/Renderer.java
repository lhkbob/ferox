package com.ferox.renderer;

import com.ferox.resource.Resource;
import com.ferox.resource.Resource.Status;

/**
 * <p>
 * A Renderer is an interface that allows actual rendering to be performed. It
 * is intended that Framework implementations internally create a Renderer that
 * they use as needed when implementing renderFrame().
 * </p>
 * <p>
 * The Renderer interface provides three primary methods: update, cleanUp, and
 * renderAtom. Both update and cleanUp are designed for use with managing
 * Resources and should be called by ResourceManager implementations. The method
 * renderAtom is intended for RenderQueue and RenderPass implementations when
 * actually rendering. Although there is this slight difference in
 * responsibility, Renderer implementations must correctly handle method calls
 * at any time (assuming it's the correct thread for low-level operations).
 * </p>
 * <p>
 * Internally it may be required for a Framework for Renderer code to be
 * executed on a separate thread from the Framework's. This is because it is
 * low-level graphics operations are likely limited to a specific thread, and
 * during specific periods of time. Because of this Renderer instances should
 * not be held onto externally, but should only be used in method calls that
 * take them as arguments (such as in RenderPass, RenderQueue, and
 * ResourceManager).
 * </p>
 * <p>
 * Similarly, when it is allowed to use a Renderer's methods, its associated
 * Framework is considered to be rendering, causing many of its methods to
 * become unusable. When using the Framework returned by getFramework(), only
 * use those methods that don't throw a RenderStateException.
 * </p>
 * 
 * @author Michael Ludwig
 */
public interface Renderer {
	/**
	 * Return the Framework that's associated with this Renderer. Each Renderer
	 * will only ever have one Framework, but it is possible for a Framework to
	 * use multiple Renderer's if it's multi-threaded.
	 * 
	 * @return The Framework associated with this Renderer.
	 */
	public Framework getFramework();

	/**
	 * <p>
	 * Perform an update of the given resource. Every resource must be updated
	 * before it can be used by the Framework (including Geometries). If the
	 * resource has never been updated before by this Framework, or if
	 * forceFullUpdate is true, the resource will ignore the object returned by
	 * getDirtyDescriptor(). Otherwise, the Framework can decide to abide by the
	 * dirty description.
	 * </p>
	 * <p>
	 * An important note about the update/clean-up process of the Framework: If
	 * a resource hasn't been used before or is cleaned-up, but is needed when
	 * rendering a RenderAtom, it will be automatically updated. If the resource
	 * has a non-null dirty descriptor when it's needed, it will also be
	 * updated.
	 * </p>
	 * <p>
	 * It is the resource manager implementations responsibility that a resource
	 * is not updated or cleaned up more than once per frame, otherwise it will
	 * be inefficient and a waste. Application programmers that mix using
	 * requestX() and custom resource managers must then ensure that the default
	 * and custom managers both don't try to update or clean a resource.
	 * </p>
	 * <p>
	 * If update is called again in the same frame on the same resource, it is
	 * as if it were updated multiple times. If it is updated and then cleaned,
	 * the update will have no effect.
	 * </p>
	 * 
	 * @param resource The Resource to be updated
	 * @param forceFullUpdate Whether or not the update should ignore dirty
	 *            descriptions of the resource
	 * @return The new status of the resource after the update has completed
	 * @throws NullPointerException if resource is null
	 * @throws UnsupportedResourceException if the resource implementation is
	 *             unsupported by this Framework
	 * @throws RenderException if this is called from within the stack of a
	 *             cleanUp() or update() call already processing resource
	 * @throws RenderStateException if the calling thread can't perform
	 *             low-level graphics operations
	 */
	public Status update(Resource resource, boolean forceFullUpdate);

	/**
	 * <p>
	 * Cleanup the low-level, graphics hardware related data for the given
	 * resource. After a call to this method, the resource will no longer be
	 * usable (just as if it were never updated).
	 * </p>
	 * <p>
	 * Unlike the destroy() methods, a resource is still usable after it has
	 * been cleaned. Its attributes and data can be modified; the only
	 * requirement is that it must be updated again before it can be used
	 * correctly for rendering (explicitly or implicitly). Because resources are
	 * implicitly updated if they're cleaned-up, a resource should only be
	 * cleaned-up if it's not to be used for a while.
	 * </p>
	 * <p>
	 * This method should do nothing if the resource has already been cleaned up
	 * or it was never updated by this Framework before.
	 * </p>
	 * <p>
	 * All warnings from update() apply here as well.
	 * </p>
	 * 
	 * @param resource The Resource to have its internal resources cleaned-up
	 * @throws NullPointerException if resource is null
	 * @throws IllegalArgumentException if the resource is a TextureImage being
	 *             used by an undestroyed TextureSurface for this Framework
	 * @throws UnsupportedResourceException if the resource implementation is
	 *             unsupported by this Framework
	 * @throws RenderException if this is called from within the stack of a
	 *             cleanUp() or update() call already processing resource
	 * @throws RenderStateException if the calling thread can't perform
	 *             low-level graphics operations
	 */
	public void cleanUp(Resource resource);

	/**
	 * <p>
	 * Render the given RenderAtom. This involves applying the atom's
	 * appearance, adjusting the model transform and actually rendering the
	 * geometry.
	 * </p>
	 * <p>
	 * When applying the atom's appearance, there is a notion of a default
	 * appearance: if an Effect isn't present in the atom's EffectSet, then it
	 * should be rendered as if an Effect was created with the default
	 * constructor (for continuous modifiers such as Material) or as if it were
	 * disabled and not used (such as BlendMode or GlobalLighting).
	 * </p>
	 * <p>
	 * This method should not be called directly, instead it is to be used by
	 * RenderQueue implementations.
	 * </p>
	 * 
	 * @param atom The atom that should be rendered
	 * @return The number of polygons rendered by the atom
	 * @throws NullPointerException if atom is null
	 * @throws UnsupportedResourceException if the atom requires the use of
	 *             Resource and Geometry implementations that are unsupported by
	 *             this Framework
	 * @throws UnsupportedEffectException if the atom requires the use of Effect
	 *             implementations that aren't supported by this Framework
	 * @throws RenderStateException if the calling thread can't perform
	 *             low-level graphics operations
	 */
	public int renderAtom(RenderAtom atom);
}
