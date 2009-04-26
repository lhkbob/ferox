package com.ferox.renderer;

import com.ferox.resource.Resource;
import com.ferox.resource.ResourceManager;
import com.ferox.resource.Resource.Status;
import com.ferox.resource.texture.TextureImage.TextureTarget;

/**
 * <p>
 * The Renderer is the core component of rendering a scene with Ferox-gl. It
 * controls the creation of RenderSurfaces, which store the final render
 * outputs, organizes the resources and performs the actual rendering.
 * </p>
 * <p>
 * Renderer is not meant to be a thread-safe interface, and unless an
 * implementation specifically declares itself as such, the methods of a
 * Renderer should only be called from a single thread. In a related note, many
 * operations that the Renderer can perform expect the Renderer to be in a
 * certain state. For example, the methods update() and cleanUp() can only be
 * called while the Renderer is processing its resource managers.
 * </p>
 * <p>
 * All methods will through an exception if the Renderer is not in an
 * appropriate state to execute the method.
 * </p>
 * <p>
 * There are three primary interfaces that Renderer's rely on to describe the
 * specifics of a scene:<br>
 * 1. Resource - fairly static data stored on the graphics card. <br>
 * 2. Geometry - describes the shape of something to be rendered (it's also a
 * Resource).<br>
 * 3. Effect - modifies how the Geometry is rendered (e.g. surface description).
 * </p>
 * <p>
 * Because these interfaces are not specific, a Renderer implementation may not
 * support some types of implementations. Renderers must document which types
 * they support, and how to easily add support for new types. To allow for
 * efficient caching and bookkeeping, these interfaces also provide methods
 * allowing a Renderer to associate internal object with specific instances.
 * <b>UNDER NO CIRCUMSTANCES SHOULD THESE METHODS BE CALLED OR USED EXTERNALLY
 * TO RENDERER IMPLS.</b>
 * </p>
 * <p>
 * Another consequence of the get/set[Resource/Effect]() methods is that there
 * should only be one Renderer at a time. Otherwise, the renderers may compete
 * and overwrite the objects attached to the resources and states. If this is
 * the case, there very well might be memory leaks or extraneous low-level
 * graphics objects created (in effect wiping out its cache-effectiveness).
 * Renderer implementations that "wrap" actual renderers to provide some extra
 * functionality (such as logging or multi-threading) are acceptable if they
 * don't modify the attached, internal objects.
 * </p>
 * 
 * @author Michael Ludwig
 */
public interface Renderer {

	/* Idle operations. */

	/**
	 * <p>
	 * Create a WindowSurface with the given options, and flags for resizable
	 * and undecorated. These parameters are requests to the underlying
	 * Renderer, which will try its best to follow them. When the window surface
	 * is returned, it will be visible and on screen.
	 * </p>
	 * <p>
	 * The window is positioned at the given coordinates (relative to screen
	 * space) with the given dimensions. As stated in WindowSurface, these
	 * dimensions may not represent the drawable area, but can include the
	 * frame, border and insets used by the operating system.
	 * </p>
	 * <p>
	 * If options is null, or any of the other parameters have unsupported
	 * values, the Renderer may change them to successfully create a surface.
	 * </p>
	 * <p>
	 * There are two types of onscreen surfaces supported by a Renderer, either
	 * windows or fullscreen. There may be multiple window surfaces in use at
	 * the same time, but the Renderer cannot mix window and fullscreen
	 * surfaces. Surfaces of one type must be destroyed before surfaces of the
	 * other type can be created successfully.
	 * </p>
	 * 
	 * @param options Requested display options
	 * @param x Requested x location of the window
	 * @param y Requested y location of the window
	 * @param width Requested width of the window
	 * @param height Requested height of the window
	 * @param resizable Whether or not the created window should be resizable
	 * @param undecorated Whether or not the created window is undecorated
	 * @throws SurfaceCreationException if the Renderer cannot create the
	 *             WindowSurface, such as if there is an active
	 *             FullscreenSurface
	 * @throws RenderStateException if the Renderer isn't idle, or if the
	 *             renderer is destroyed
	 */
	public WindowSurface createWindowSurface(DisplayOptions options, int x,
			int y, int width, int height, boolean resizable, boolean undecorated);

	/**
	 * <p>
	 * Create a surface that puts the application into exclusive fullscreen
	 * mode. The renderer will choose a screen resolution that is closest to the
	 * given dimensions in width/height.
	 * </p>
	 * <p>
	 * If options is null, or the parameters aren't directly supported, the
	 * Renderer may change them to create a fullscreen surface.
	 * </p>
	 * <p>
	 * There may only be one fullscreen surface at one time. An exception will
	 * be thrown if another fullscreen surface exists from this Renderer and
	 * that surface isn't destroyed. Similarly, the fullscreen surface can't be
	 * created until all window surfaces are destroyed, too.
	 * </p>
	 * 
	 * @param options The requested DisplayOptions
	 * @param width The requested screen width
	 * @param height The requested screen height
	 * @throws SurfaceCreationException if the Renderer cannot create the
	 *             FullscreenSurface, such as if there's an active WindowSurface
	 *             already
	 * @throws RenderStateException if the Renderer isn't idle, or if the
	 *             renderer is destroyed
	 */
	public FullscreenSurface createFullscreenSurface(DisplayOptions options,
			int width, int height);

	/**
	 * <p>
	 * Create a texture surface that can be used to render into textures. The
	 * Renderer will create new textures that can be retrieved by calling the
	 * returned surface's getColorBuffer(target) and getDepthBuffer() methods.
	 * The size of the texture is determined by the width, height, and depth
	 * values and its formatting and type is chosen by the display options. In
	 * options, if the pixel format is NONE, then no textures will be created
	 * for the color buffers. Similarly, if the depth format is NONE, no texture
	 * is created for the depth buffer. (see about useDepthRenderBuffer below).
	 * </p>
	 * <p>
	 * Like the other surface creation methods, the renderer is free to change
	 * the parameters to make a surface created.
	 * </p>
	 * <p>
	 * The target value tells the Renderer what type of texture to use (for all
	 * buffers of the surface). If the target is T_1D, or T_CUBEMAP then height,
	 * depth and layer are ignored. If target is T_2D or T_RECT, then depth and
	 * layer are ignored. If target is T_3D, nothing is ignored.
	 * </p>
	 * <p>
	 * The layer value has the same meaning as it does in getLayer() of
	 * TextureSurface, which is why it's only used if target is T_3D or
	 * T_CUBEMAP.
	 * </p>
	 * <p>
	 * numColorTargets requests the surface to use the given number of color
	 * targets. For glsl shader writers, the textures returned by
	 * getColorBuffer(target) correspond to the low-level color targets
	 * supported by glsl. If options has a pixel format of NONE, then
	 * numColorTargets is ignored and will be 0 for the returned surface. If
	 * numColorTargets is not ignored, then it will be clamped between [1,
	 * maxColorTargets].
	 * </p>
	 * <p>
	 * If options has a depth format of NONE, then normally no depth buffer
	 * would be allocated and no texture would be created for it. However, if
	 * useDepthRenderBuffer is true in this case, the Renderer will allocate a
	 * depth buffer of its choosing for use with the surface. This allows
	 * correct z-ordering without the overhead of rendering into a texture. If a
	 * depth texture would be created, then useDepthRenderBuffer is ignored.
	 * </p>
	 * <p>
	 * If target is one of T_CUBEMAP or T_3D, cubemap and 3d textures aren't
	 * supported with a DEPTH format. If the requested depth format is not NONE,
	 * then a Texture2D/Rectangle will be created that will be shared by each
	 * layer of the color buffers. If separate depth data is necessary (e.g. for
	 * point shadow mapping), you should do cubemap unrolling into a Texture2D.
	 * </p>
	 * <p>
	 * All texture images created for the TextureSurface (and then returned by
	 * its getXBuffer() methods) must be updated and ready to use (e.g. it's not
	 * necessary to update the textures by a resource manager unless you change
	 * the texture parameters of the image). Each texture image should return a
	 * null BufferData for its image data.
	 * </p>
	 * 
	 * @param options The requested DisplayOptions, affects the chosen texture
	 *            formats
	 * @param target The type of color and depth TextureImages created
	 * @param width The width of the created image, must be valid as determined
	 *            by target
	 * @param height The height of the created image, must be valid as
	 *            determined by target. It is ignored for T_1D targets
	 * @param depth The depth of the created image, must be valid if target is
	 *            T_3D
	 * @param layer Integer parameter specifying 2D slice updated by this
	 *            surface. Only used if target is T_CUBEMAP or T_3D
	 * @param numColorTargets The requested number of color texture images to
	 *            render into simultaneously
	 * @param userDepthRenderBuffer If no depth image is used, true signals to
	 *            store depth information outside of a texture
	 * @throws SurfaceCreationException if the TextureSurface can't be created,
	 *             because dimensions were invalid for the target, the layer was
	 *             invalid (if not ignored), or if they are unsupported, etc.
	 * @throws RenderStateException if the renderer isn't idle or if it's
	 *             destroyed
	 */
	public TextureSurface createTextureSurface(DisplayOptions options,
			TextureTarget target, int width, int height, int depth, int layer,
			int numColorTargets, boolean useDepthRenderBuffer);

	/**
	 * <p>
	 * Create a texture surface that uses the exact same textures as the given
	 * surface (e.g. its getXBuffer() methods return the same texture images).
	 * The new surface will have the same format, same number of color targets,
	 * and the same depth buffer (including hidden depth render buffers, if they
	 * were created for the original surface).
	 * </p>
	 * <p>
	 * The layer value can be used to tell the renderer to render into a
	 * different layer of the same textures. This allows 3d rendering, or
	 * dynamic reflections to easily be implemented (for targets of T_3D or
	 * T_CUBEMAP). If layer would be ignored in the other createTextureSurface()
	 * method, it is ignored here. In this case, another surface is created that
	 * references the exact same texture data, which could cause weird results
	 * if rendering isn't done with care.
	 * </p>
	 * 
	 * @param share The TextureSurface whose images are attached to for
	 *            rendering purposes
	 * @param layer The new layer to use for this surface (e.g. a different cube
	 *            face)
	 * @throws SurfaceCreationException if share is null, it's already
	 *             destroyed, or it wasn't created by this renderer. Also fail
	 *             if layer is invalid for the share's target type
	 * @throws RenderStateException if the renderer isn't idle or if it's been
	 *             destroyed
	 */
	public TextureSurface createTextureSurface(TextureSurface share, int layer);

	/**
	 * <p>
	 * Destroy the given RenderSurface. After a call to this method, the surface
	 * can no longer be used for calls to queueRender() and its isDestroyed()
	 * method will return true. The behavior of other methods of the surface are
	 * undefined after a call to this method.
	 * </p>
	 * <p>
	 * OnscreenSurfaces will no longer be visible after they are destroyed. When
	 * a FullscreenSurface is destroyed, the monitor should be restored to the
	 * original resolution when the surface was first created.
	 * </p>
	 * 
	 * @param surface The RenderSurface to destroy
	 * @throws NullPointerException if surface is null
	 * @throws IllegalArgumentException if the surface is already destroyed or
	 *             wasn't created by this Renderer
	 * @throws RenderStateException if this renderer isn't idle or if it's been
	 *             destroyed
	 */
	public void destroy(RenderSurface surface);

	/**
	 * <p>
	 * Destroy all remaining, undestroyed RenderSurfaces created by this
	 * Renderer. This method also cleans up the remaining internal resources and
	 * any information stored directly on the graphics card that wasn't cleaned
	 * up as the direct result of a ResourceManager.
	 * </p>
	 * <p>
	 * After one Renderer is destroyed, it is acceptable to create another
	 * Renderer.
	 * </p>
	 * <p>
	 * There is no matching isDestroyed() method because it is assumed that the
	 * Renderer will be discarded after it is destroyed.
	 * </p>
	 * 
	 * @throws RenderStateException if the Renderer isn't idle, or if it's
	 *             already been destroyed
	 */
	public void destroy();

	/**
	 * <p>
	 * Add the given ResourceManager to the Renderer's list of ResourceManagers.
	 * ResourceManagers have their manage() method called at the beginning of
	 * each flushRenderer() call. The managers will be processed in the order
	 * they were added (and taking into account any removals).
	 * </p>
	 * <p>
	 * Renderer implementations must provide a default ResourceManager that is
	 * used by calls to requestUpdate() and requestCleanUp(). This default
	 * ResourceManager is invoked before any other managers.
	 * </p>
	 * <p>
	 * Don't do anything if the manager is null, or if it's already been added
	 * to this Renderer.
	 * </p>
	 * 
	 * @param manager The ResourceManager to add to the list of processed
	 *            managers
	 * @throws RenderStateException if the Renderer isn't idle or if it's
	 *             already been destroyed
	 */
	public void addResourceManager(ResourceManager manager);

	/**
	 * Remove the given ResourceManager from the Renderer's list of managers.
	 * This method does nothing if the manager is null, or if it's not in the
	 * renderer's list.
	 * 
	 * @param manager The ResourceManage to remove from the list of processed
	 *            managers
	 * @throws RenderStateException if the Renderer isn't idle or if it's
	 *             already been destroyed
	 */
	public void removeResourceManager(ResourceManager manager);

	/**
	 * <p>
	 * Request the given resource to be updated. If forceFullUpdate is true,
	 * then the eventual update will force a full update.
	 * <p>
	 * <p>
	 * Every Renderer implementation must provide a default ResourceManager that
	 * is used by calls to requestUpdate() and requestCleanUp(). It is
	 * responsible for making sure that a specific resource instance is only
	 * updated or cleaned once per frame.
	 * </p>
	 * <p>
	 * Requested updates and clean-ups will be performed in the order of the
	 * method calls (updates and clean-ups may then be interleaved in some
	 * cases, then). Resources that rely on other resources must be updated
	 * after the required resources have been updated.
	 * </p>
	 * <p>
	 * A call to this method will override an old update or clean-up request.
	 * The request, if it's not overridden later, will be completed at the start
	 * of the next call to flushRenderer().
	 * </p>
	 * <p>
	 * Note that an exception may be thrown later on if the resource would fail
	 * the requirements for the actual update() method.
	 * </p>
	 * 
	 * @see update()
	 * @param resource The resource that's to be updated next frame
	 * @param forceFullUpdate Whether or not the resource should have a complete
	 *            update
	 * @throws NullPointerException if resource is null
	 * @throws RenderStateException if the Renderer isn't idle or if it's
	 *             already been destroyed
	 */
	public void requestUpdate(Resource resource, boolean forceFullUpdate);

	/**
	 * Request the given resource to be cleaned up. This is the counterpart to
	 * requestUpdate() and functions similarly, except it will eventually clean
	 * the resource instead of update it.
	 * 
	 * @see cleanUp()
	 * @param resource The resource that should have internal resources cleaned
	 *            up
	 * @throws NullPointerException if resource is null
	 * @throws RenderStateException if the Renderer isn't idle or if it's
	 *             already been destroyed
	 */
	public void requestCleanUp(Resource resource);

	/**
	 * <p>
	 * Queue the given RenderSurface to be rendered during the next call to
	 * flushRenderer. RenderSurfaces are rendered in the order that they were
	 * queued, so it is the applications responsibility for queuing them in a
	 * reliable and efficient order.
	 * </p>
	 * <p>
	 * One likely trend to stick to is to have all of the TextureSurfaces
	 * grouped at the beginning, with the OnscreenSurfaces coming later. A
	 * low-level texture surface may be implemented as an fbo, allowing the
	 * Renderer to avoid many low-level context switches. In the worst case,
	 * each TextureSurface will require a context switch, just like each
	 * OnscreenSurface.
	 * </p>
	 * <p>
	 * If a surface is queued multiple times, been destroyed, or null then it
	 * will be ignored.
	 * </p>
	 * 
	 * @param surface The render surface that should be queued for rendering
	 * @return This Renderer
	 * @throws IllegalArgumentException if surface wasn't created by this
	 *             renderer
	 * @throws RenderStateException if the Renderer isn't idle or if it's
	 *             already been destroyed
	 */
	public Renderer queueRender(RenderSurface surface);

	/**
	 * <p>
	 * Render a single frame. The renderer must invoke manage() on its default
	 * ResourceManager and then any custom managers. After the managers are
	 * processed, the renderer should, for each queued surface, clear the
	 * surface based on its settings and render each attached render pass.
	 * Queued surfaces that have been destroyed should be ignored.
	 * </p>
	 * <p>
	 * The resource managers must be processed even if there are no queued
	 * render surfaces.
	 * </p>
	 * <p>
	 * The renderer is allowed to prepare the render passes at any time before
	 * the pass must be rendered, including before the managers are processed.
	 * No matter when they are prepared, a Renderer must be sure to respect the
	 * result of pass's prepare() method.
	 * </p>
	 * <p>
	 * If an exception is thrown, the rendering of the frame stops. Because of
	 * this, ResourceManagers, RenderPasses and other objects used in
	 * flushRenderer should be careful when they decide to throw an exception.
	 * </p>
	 * 
	 * @param store The FrameStatistics instance to use for stats accumulation
	 * @return The stats for this frame (in store, or a new instance if store
	 *         was null).
	 * @throws RenderException wrapping any exception that occurs while rendering
	 * @throws RenderStateException if the Renderer isn't idle or if it's
	 *             already been destroyed
	 */
	public FrameStatistics flushRenderer(FrameStatistics store);

	/* Anytime operations. */

	/**
	 * <p>
	 * Get the current status of the given resource. Return null if resource is
	 * null or if the resource type is unsupported.
	 * </p>
	 * <p>
	 * If ERROR is returned, the resource will be ignored when rendering.
	 * </p>
	 * 
	 * @param resource The Resource whose status is requested
	 * @return The Status of resource
	 * @throws RenderStateException if the Renderer isn't idle or if it's already
	 *             been destroyed
	 */
	public Status getStatus(Resource resource);

	/**
	 * Get a Renderer status message that is more informative about the given
	 * resources's status. Return null if the resource is null or its type is
	 * unsupported.
	 * 
	 * @param resource The Resource whose status message is requested
	 * @return The status message for resource
	 * @throws RenderStateException if the Renderer isn't idle or if it's already
	 *             been destroyed
	 */
	public String getStatusMessage(Resource resource);

	/**
	 * Get the capabilities of this Renderer.
	 * 
	 * @return The RenderCapabilities for this Renderer, will not be null
	 * @throws RenderStateException if the Renderer isn't idle or if it's already
	 *             been destroyed
	 */
	public RenderCapabilities getCapabilities();

	/* Resource operations. */

	/**
	 * <p>
	 * Perform an update of the given resource. Every resource must be updated
	 * before it can be used by the Renderer (including Geometries). If the
	 * resource has never been updated before by this renderer, or if
	 * forceFullUpdate is true, the resource will ignore the object returned by
	 * getDirtyDescriptor(). Otherwise, the renderer can decide to abide by the
	 * dirty description.
	 * </p>
	 * <p>
	 * An important note about the update/clean-up process of the Renderer: If a
	 * resource hasn't been used before or is cleaned-up, but is needed when
	 * rendering a RenderAtom, it will be automatically updated. If the resource
	 * is subsequently modified, it must be updated as normal. The auto-updating
	 * is purely a convenience and shouldn't be used in high-performance
	 * applications. Resource updates in the middle of the frame have more
	 * impact on render speed than updates done at the beginning, before any
	 * rendering has begun.
	 * </p>
	 * <p>
	 * It is the resource manager implementations responsibility that a resource
	 * is not updated or cleaned up more than once per frame, otherwise it will
	 * be inefficient and a waste. Application programmers that mix using
	 * requestX() and custom resource managers must then ensure that the default
	 * and custom managers both don't try to update or clean a resource.
	 * </p>
	 * <p>
	 * If update is called in the same frame on the same resource, it is as if
	 * it were updated multiple times. If it is updated and then cleaned, the
	 * update will have no effect.
	 * </p>
	 * 
	 * @param resource The Resource to be updated
	 * @param forceFullUpdate Whether or not the update should ignore dirty
	 *            descriptions of the resource
	 * @return The new status of the resource after the update has completed.
	 * @throws NullPointerException if resource is null
	 * @throws UnsupportedResourceException if the resource implementation is
	 *             unsupported by this Renderer
	 * @throws RenderException if this is called from within the stack of a
	 *             cleanUp() or update() call already processing resource
	 * @throws RenderStateException if the Renderer isn't idle or if it's
	 *             already been destroyed
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
	 * or it was never updated by this renderer before.
	 * </p>
	 * <p>
	 * All warnings from update() apply here as well.
	 * </p>
	 * 
	 * @param resource The Resource to have its internal resources cleaned-up
	 * @throws NullPointerException if resource is null
	 * @throws IllegalArgumentException if the resource is a TextureImage being
	 *             used by an undestroyed TextureSurface for this Renderer
	 * @throws UnsupportedResourceException if the resource implementation is
	 *             unsupported by this Renderer
	 * @throws RenderException if this is called from within the stack of a
	 *             cleanUp() or update() call already processing resource
	 * @throws RenderStateException if the Renderer isn't idle or if it's
	 *             already been destroyed
	 */
	public void cleanUp(Resource resource);

	/* Rendering operations. */

	/**
	 * <p>
	 * Render the given RenderAtom. This involves applying the atom's
	 * appearance, adjusting the model transform and actually rendering the
	 * geometry. It should take into account any applied influences since the
	 * last call to renderAtom(). After rendering is finished, it should clear
	 * the list of applied influences.
	 * </p>
	 * <p>
	 * When applying the atom's appearance, there is a notion of a default
	 * appearance: if an Effect isn't present in the atom's EffectSet, then it
	 * should be rendered as if an Effect was created with the default
	 * constructor (for continuous modifiers such as Material) or as if it were
	 * disabled and not used (such as BlendMode or LightReceiver.
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
	 *             this Renderer
	 * @throws UnsupportedEffectException if the atom requires the use of Effect
	 *             implementations that aren't supported by this Renderer
	 * @throws RenderStateException if the Renderer isn't prepared to render
	 *             atoms from this thread
	 */
	public int renderAtom(RenderAtom atom);
}
