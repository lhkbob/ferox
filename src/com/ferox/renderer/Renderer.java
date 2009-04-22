package com.ferox.renderer;

import java.util.List;

import com.ferox.resource.Geometry;
import com.ferox.resource.Resource;
import com.ferox.resource.ResourceManager;
import com.ferox.resource.Resource.Status;
import com.ferox.resource.texture.TextureImage.TextureTarget;
import com.ferox.state.State.Role;

/** The Renderer is the core component of rendering a scene with
 * Ferox-gl.  It controls the creation of RenderSurfaces, which store
 * the final render outputs, organizes the resources and performs
 * the actual rendering.
 * 
 * Renderer is not meant to be a thread-safe interface, and unless an
 * implementation specifically declares itself as such, the methods of
 * a Renderer should only be called from a single thread.  In a related
 * note, many operations that the Renderer can perform expect the Renderer
 * to be in a certain state.  For example, the methods update() and cleanUp()
 * can only be called while the Renderer is processing its resource managers.
 * 
 * All methods will through an exception if the Renderer is not in an
 * appropriate state to execute the method.
 * 
 * There are three primary interfaces that Renderer's rely on to describe the specifics
 * of a scene:
 *   1. Resource - fairly static data stored on the graphics card.
 *   2. Geometry - describes the shape of something to be rendered (it's also a Resource).
 *   3. State - modifies how the Geometry is rendered (e.g. surface description).
 *   
 * Because these interfaces are not specific, a Renderer implementation may not support some
 * types of implementations.  Renderers must document which types they support, and how to easily
 * add support for new types.  To allow for efficient caching and bookkeeping, these interfaces
 * also provide methods allowing a Renderer to associate internal object with specific instances.
 * UNDER NO CIRCUMSTANCES SHOULD THESE METHODS BE CALLED OR USED EXTERNALLY TO RENDERER IMPLS.
 * 
 * Another consequence of the get/set[Resource/State]() methods is that there should only be
 * one Renderer at a time.  Otherwise, the renderers may compete and overwrite the objects
 * attached to the resources and states.  If this is the case, there very well might be memory leaks
 * or extraneous low-level graphics objects created (in effect wiping out its cache-effectiveness).
 * Renderer implementations that "wrap" actual renderers to provide some extra functionality 
 * (such as logging or multi-threading) are acceptable if they don't modify the attached, internal objects.
 * 
 * @author Michael Ludwig
 *
 */
public interface Renderer {
	
	/* Idle operations. */
	
	/** Create a WindowSurface with the given options, and flags for resizable and undecorated.
	 * These parameters are requests to the underlying Renderer, which will try its best to
	 * follow them.  When the window surface is returned, it will be visible and on screen.
	 * 
	 * The window is positioned at the given coordinates (relative to screen space) with the given
	 * dimensions.  As stated in WindowSurface, these dimensions may not represent the drawable area,
	 * but can include the frame, border and insets used by the operating system.
	 * 
	 * If options is null, or any of the other parameters have unsupported values, the Renderer
	 * may change them to successfully create a surface.
	 * 
	 * There are two types of onscreen surfaces supported by a Renderer, either windows or fullscreen.
	 * There may be multiple window surfaces in use at the same time, but the Renderer cannot mix
	 * window and fullscreen surfaces.  Surfaces of one type must be destroyed before surfaces of
	 * the other type can be created successfully.
	 * 
	 * Throw an exception if the Renderer cannot create another surface, for any other reason,
	 * if the renderer isn't idle, or if the renderer is destroyed. */
	public WindowSurface createWindowSurface(DisplayOptions options, int x, int y, int width, int height,
											 boolean resizable, boolean undecorated) throws RenderException;
	
	/** Create a surface that puts the application into exclusive fullscreen mode.  The renderer will
	 * choose a screen resolution that is closest to the given dimensions in width/height.
	 * 
	 * If options is null, or the parameters aren't directly supported, the Renderer may change them
	 * to create a fullscreen surface.
	 * 
	 * There may only be one fullscreen surface at one time.  An exception will be thrown if another
	 * fullscreen surface exists from this Renderer and that surface isn't destroyed.  Similarly,
	 * the fullscreen surface can't be created until all window surfaces are destroyed, too.
	 * 
	 * Throw an exception if the Renderer cannot create another surface for any other reason,
	 * if the renderer isn't idle, or if the renderer is destroyed. */
	public FullscreenSurface createFullscreenSurface(DisplayOptions options, int width, int height) throws RenderException;
	
	/** Create a texture surface that can be used to render into textures.  The Renderer will create new
	 * textures that can be retrieved by calling the returned surface's getColorBuffer(target) and getDepthBuffer()
	 * methods.  The size of the texture is determined by the width, height, and depth values and its formatting and
	 * type is chosen by the display options.  In options, if the pixel format is NONE, then no textures will be
	 * created for the color buffers.  Similarly, if the depth format is NONE, no texture is created for the depth buffer.
	 * (see useDepthRenderBuffer below).
	 * 
	 * Like the other surface creation methods, the renderer is free to change the parameters to make a surface created.
	 * 
	 * The target value tells the Renderer what type of texture to use (for all buffers of the surface).  If the target
	 * is T_1D, or T_CUBEMAP then height, depth and layer are ignored.  If target is T_2D or T_RECT, then depth and layer are ignored.
	 * If target is T_3D, nothing is ignored.
	 * 
	 * The layer value has the same meaning as it does in getLayer() of TextureSurface, which is why it's only used if
	 * target is T_3D or T_CUBEMAP.
	 * 
	 * numColorTargets requests the surface to use the given number of color targets.  For glsl shader writers, the 
	 * textures returned by getColorBuffer(target) correspond to the low-level color targets supported by glsl.
	 * If options has a pixel format of NONE, then numColorTargets is ignored and will be 0 for the returned surface.
	 * If numColorTargets is not ignored, then it will be clamped between [1, maxColorTargets].
	 * 
	 * If options has a depth format of NONE, then normally no depth buffer would be allocated and no texture would be created for it.
	 * However, if useDepthRenderBuffer is true in this case, the Renderer will allocate a depth buffer of its choosing for
	 * use with the surface.  This allows correct z-ordering without the overhead of rendering into a texture.
	 * If a depth texture would be created, then useDepthRenderBuffer is ignored.
	 * 
	 * If target is one of T_CUBEMAP or T_3D, cubemap and 3d textures aren't supported with a DEPTH format.  If the requested
	 * depth format is not NONE, then a Texture2D/Rectangle will be created that will be shared by each layer of the color buffers.
	 * If separate depth data is necessary (e.g. for point shadow mapping), you should do cubemap unrolling into a Texture2D.
	 * 
	 * All texture images created for the TextureSurface (and then returned by its getXBuffer() methods) must be updated
	 * and ready to use (e.g. it's not necessary to update the textures by a resource manager unless you change the 
	 * texture parameters of the image).  Each texture image should return a null BufferData for its image data.
	 * 
	 * Throw an exception if target is null.
	 * Throw an exception if the dimensions for the requested target would be illegal (according to the rules defined
	 * in the target's class file).  Throw an exception if the layer argument is illegal if the layer isn't ignored.
	 * Throw an exception if the renderer isn't idle, if it's destroyed, or if it can't create the texture surface for any reason. */
	public TextureSurface createTextureSurface(DisplayOptions options, TextureTarget target, int width, int height, int depth, int layer,
											   int numColorTargets, boolean useDepthRenderBuffer) throws RenderException;
	
	/** Create a texture surface that uses the exact same textures as the given surface (e.g. its getXBuffer() methods return
	 * the same texture images).  The new surface will have the same format, same number of color targets, and the same
	 * depth buffer (including hidden depth render buffers, if they were created for the original surface).
	 * 
	 * The layer value can be used to tell the renderer to render into a different layer of the same textures.  This allows
	 * 3d rendering, or dynamic reflections to easily be implemented (for targets of T_3D or T_CUBEMAP).  If layer would be ignored
	 * in the other createTextureSurface() method, it is ignored here.  In this case, another surface is created that references
	 * the exact same texture data, which could cause weird results if rendering isn't done with care.
	 * 
	 * Throw an exception if the given surface is null, it's been destroyed, or it wasn't created by this renderer.
	 * Throw an exception if the layer is invalid if the layer isn't ignored.  Throw an exception if the renderer isn't
	 * idle, if it's destroyed, or if it can't create the surface for any other reason. */
	public TextureSurface createTextureSurface(TextureSurface share, int layer) throws RenderException;
	
	/** Return a single Geometry instance that represents the list of render atoms.  If
	 * submitted with renderAtom(), this rendered Geometry will represent the what would
	 * be rendered if the list of atoms were submitted to renderAtom() in order (accounting
	 * for the current viewport and transform).  Any subsequent changes to the appearances,
	 * positions, or geometries will not be visible in the returned Geometry.
	 * 
	 * The returned Geometry can usually be rendered much faster than if the list were
	 * rendered.  It is intended for static atom's, and because of this, updating the returned
	 * geometry has no effect (it must still be cleaned-up, but subsequent updates will then
	 * fail).  
	 * 
	 * If the specified list is empty, return null.
	 * 
	 * Because the returned Geometry uses the Appearances stored in atoms, it must return
	 * true from isAppearanceIgnored().  Depending on what is submitted to the Renderer,
	 * influence atoms can still affect the rendering.  If a state's role is never used during
	 * the compile process, it will not be ignored.
	 * 
	 * Depending on the implementation, there may be undefined results with states that rely,
	 * implicitly on the View's current view transform (such as individual lights, or EYE
	 * texture coord generation).  This is because the low-level operations apply the current
	 * view transform when the call is executed.  Thus changes to the view will not be visible
	 * in the returned Geometry's appearances.
	 * 
	 * Implementations do not need to provide a useful implementation of getVertex(), so
	 * programmers should use getBounds() instead of bounds.enclose() to get the BoundVolume
	 * for this Geometry.
	 * 
	 * Throw an exception if atoms is null, any atom would cause an exception to be thrown
	 * if it were rendered normally, if the renderer isn't idle, or if it's been destroyed. */
	public Geometry compile(List<RenderAtom> atoms) throws RenderException;
	
	/** Destroy the given RenderSurface.  After a call to this method, the
	 * surface can no longer be used for calls to queueRender() and its
	 * isDestroyed() method will return true.  The behavior of other methods
	 * of the surface are undefined after a call to this method.
	 * 
	 * OnscreenSurfaces will no longer be visible after they are destroyed.
	 * When a FullscreenSurface is destroyed, the monitor should be restored
	 * to the original resolution when the surface was first created.
	 * 
	 * The renderer will throw an exception if the surface is null, it's
	 * already been destroyed, or it wasn't created by this renderer.
	 * It also expects the renderer to be idle when this is called, if
	 * not then an exception is thrown. */
	public void destroy(RenderSurface surface) throws RenderException;
	
	/** Destroy all remaining, undestroyed RenderSurfaces created by this
	 * Renderer.  This method also cleans up the remaining internal
	 * resources and any information stored directly on the graphics card
	 * that wasn't cleaned up as the direct result of a ResourceManager.
	 * 
	 * After one Renderer is destroyed, it is acceptable to create another
	 * Renderer.
	 * 
	 * There is no matching isDestroyed() method because it is assumed
	 * that the Renderer will be discarded after it is destroyed.
	 * 
	 * The renderer will throw an exception if it's already been destroyed,
	 * or if it's not idle. */
	public void destroy() throws RenderException;
	
	/** Add the given ResourceManager to the Renderer's list of ResourceManagers.
	 * ResourceManagers have their manage() method called at the beginning of
	 * each flushRenderer() call.  The managers will be processed in the order
	 * they were added (and taking into account any removals).
	 * 
	 * Renderer implementations must provide a default ResourceManager that is
	 * used by calls to requestUpdate() and requestCleanUp().  This default
	 * ResourceManager is invoked before any other managers.
	 * 
	 * Don't do anything if the manager is null, or if it's already been added
	 * to this Renderer.
	 * 
	 * Throw an exception if the renderer isn't idle, or if it's destroyed. */
	public void addResourceManager(ResourceManager manager) throws RenderException;
	
	/** Remove the given ResourceManager from the Renderer's list of managers.
	 * This method does nothing if the manager is null, or if it's not in
	 * the renderer's list.
	 * 
	 * Throw an exception if the renderer isn't idle, or if it's destroyed. */
	public void removeResourceManager(ResourceManager manager) throws RenderException;
	
	/** Request the given resource to be updated.  If forceFullUpdate is true,
	 * then the eventual update will force a full update.
	 * 
	 * Every Renderer implementation must provide a default ResourceManager that
	 * is used by calls to requestUpdate() and requestCleanUp().  It is responsible
	 * for making sure that a specific resource instance is only updated or cleaned
	 * once per frame.  
	 * 
	 * Requested updates and clean-ups will be performed in the order of the method
	 * calls (updates and clean-ups may then be interleaved in some cases, then).  
	 * Resources that rely on other resources must be updated after the required
	 * resources have been updated. 
	 * 
	 * A call to this method will override an old update or clean-up request.
	 * The request, if it's not overridden later, will be completed at the start
	 * of the next call to flushRenderer().
	 * 
	 * Throw an exception if the renderer isn't idle, if it's destroyed,
	 *  or if the resource is null.  Note that an exception may be thrown later on
	 *  if the resource would fail the requirements for the actual update() method. */
	public void requestUpdate(Resource resource, boolean forceFullUpdate) throws RenderException;
	
	/** Request the given resource to be cleaned up.  This is the counterpart
	 * to requestUpdate() and functions similarly, except it will eventually
	 * clean the resource instead of update it.
	 * 
	 * Throw an exception for the same reasons as requestUpdate(). */
	public void requestCleanUp(Resource resource) throws RenderException;
	
	/** Queue the given RenderSurface to be rendered during the next call
	 * to flushRenderer.  RenderSurfaces are rendered in the order that they
	 * were queued, so it is the applications responsibility for queuing them
	 * in a reliable and efficient order.  
	 * 
	 * One likely trend to stick to is to have all of the TextureSurfaces grouped
	 * at the beginning, with the OnscreenSurfaces coming later.  A low-level
	 * texture surface may be implemented as an fbo, allowing the Renderer to avoid
	 * many low-level context switches.  In the worst case, each TextureSurface will require
	 * a context switch, just like each OnscreenSurface.
	 * 
	 * If a surface is queued multiple times or has been destroyed, it will be ignored.
	 * Return this Renderer object so queue requests can be chained together.
	 * 
	 * Throw an exception if the renderer is destroyed, the renderer isn't idle,
	 * or the surface wasn't created by this renderer. */
	public Renderer queueRender(RenderSurface surface) throws RenderException;
	
	/** Render a single frame.  The renderer must invoke manage() on
	 * its default ResourceManager and then any custom managers.  After the 
	 * managers are processed, the renderer should, for each queued surface,
	 * clear the surface based on its settings and render each attached 
	 * render pass.  Queued surfaces that have been destroyed should be ignored.
	 * 
	 * The resource managers must be processed even if there are no queued
	 * render surfaces.
	 * 
	 * The renderer is allowed to prepare the render passes at any time
	 * before the pass must be rendered, including before the managers are 
	 * processed.  No matter when they are prepared, a Renderer must be sure
	 * to respect the result of pass's prepare() method.
	 * 
	 * On a side note, because render passes are not required to
	 * update their scene elements, application programmers must make sure that
	 * is done before they call flushRenderer().
	 * 
	 * The FrameStatistics store will contain the statistics for the just
	 * completed frame.  If store is null, a new FrameStatistics object
	 * should be created to hold the info.  
	 * 
	 * Return store (or the new instance if store is null).
	 * 
	 * Throw an exception if the renderer is destroyed, it's not idle, or  if render passes
	 * return a null RenderQueue.  Throw a RenderException that wraps any
	 * exception that happened to occur while rendering. 
	 * 
	 * If an exception is thrown, the rendering of the frame stops.  Because of this,
	 * ResourceManagers, RenderPasses and other objects used in flushRenderer should
	 * be careful when they decide to throw an exception. */
	public FrameStatistics flushRenderer(FrameStatistics store) throws RenderException;
	
	/* Anytime operations. */
	
	/** Determine if the given type of state is masked.  If this state has never
	 * had setStateMasked() called with it, then this method should return false.
	 * Return false if type is null.
	 * 
	 * If this method returns true, then the default state (see renderAtom()) of the given type 
	 * will always be used, regardless of any state attached to an appearance being rendered.
	 * 
	 * If this method returns false, but a render passes's isStateMasked() returns true,
	 * then the state type will still be masked. 
	 * 
	 * Throws an exception if the renderer is destroyed. */
	public boolean isStateMasked(Role role) throws RenderException;
	
	/** Set whether or not a state type is masked when rendering appearances with this renderer.
	 * See isStateMasked() for more details. 
	 * 
	 * Do nothing if type is null.
	 * 
	 * Throws an exception if the renderer is destroyed. */
	public void setRoleMasked(Role type, boolean mask) throws RenderException;
	
	/** Get the current status of the given resource.  Return null if resource is null
	 * or if the resource type is unsupported.
	 * 
	 * If ERROR is returned, the resource will be treated internally just as if
	 * it had never been updated when trying to use it for rendering.
	 * 
	 * Fail if the renderer has been destroyed. */
	public Status getStatus(Resource resource) throws RenderException;
	
	/** Get a Renderer status message that is more informative about the given resources's status.
	 * Return null if the resource is null or its type is unsupported. 
	 * 
	 * Fail if the renderer is destroyed. */
	public String getStatusMessage(Resource resource) throws RenderException;
	
	/** Get the non-null capabilities of this Renderer.
	 * Fail if the renderer is destroyed. */
	public RenderCapabilities getCapabilities() throws RenderException;
	
	/* Resource operations. */
	
	/** Perform an update of the given resource.  Every resource must be updated
	 * before it can be used by the Renderer (including Geometries).  If 
	 * the resource has never been updated before by this renderer, or if
	 * forceFullUpdate is true, the resource will ignore the object returned by
	 * getDirtyDescriptor().  Otherwise, the renderer can decide to abide by the
	 * dirty description.
	 * 
	 * It is the resource manager implementations responsibility that a resource
	 * is not updated or cleaned up more than once per frame, otherwise it will
	 * be inefficient and a waste.  Application programmers that mix using requestX()
	 * and custom resource managers must ensure that the default and custom managers
	 * both don't try to update or clean a resource. 
	 * 
	 * If update is called in the same frame on the same resource, it is as if it 
	 * were updated multiple times.  If it is updated and then cleaned, the update
	 * will have no effect.
	 * 
	 * Return the new status of the resource after the update has completed.
	 * 
	 * Throw an exception if the renderer is destroyed, if it's not processing
	 * its ResourceManagers the resource is null, or the  resource type is unsupported. 
	 * Throw an exception if this called from within the stack 
	 * of a cleanUp or update call already handling this resource. */
	public Status update(Resource resource, boolean forceFullUpdate) throws RenderException;
	
	/** Cleanup the low-level, graphics hardware related data for the given
	 * resource.  After a call to this method, the resource will no longer
	 * be usable (just as if it were never updated).
	 * 
	 * Unlike the destroy() methods, a resource is still usable after it has been 
	 * cleaned.  Its attributes and data can be modified; the only requirement
	 * is that it must be updated again before it can be used correctly for rendering.
	 * 
	 * This method should do nothing if the resource has already been cleaned
	 * up or it was never updated by this renderer before.
	 * 
	 * All warnings from update() apply here as well and exceptions will be thrown for the same reasons. 
	 * Additionally, throw an exception if the resource is locked because it is directly attached
	 * to an undestroyed surface (such as a TextureSurface's images).  These locked resources won't be
	 * cleaned when the surface is destroyed, but afterwards they may be if they're no longer in use, too. */
	public void cleanUp(Resource resource) throws RenderException;
	
	/* Rendering operations. */
	
	/** Apply an influence to the next atom to be rendered with renderAtom(atom).  The affects of this method
	 * will not be shown until renderAtom() is called.  Hardware and implementation limits may cause the renderer
	 * to discard the influence atom, however if discarding is necessary, the renderer should make all efforts to
	 * discard an atom that influences the RenderAtom least.  The value passed in represents the influence
	 * the InfluenceAtom has on the atom to be next rendered with a call to renderAtom (the same number 
	 * returned by the atom's getInfluence(renderAtom) method).
	 *  
	 * This method should not be called directly, it is intended for use by RenderQueue implementations.
	 * Do nothing if inf's state is null or if value <= 0. If value > 0, it will be clamped to be <= 1.
	 * 
	 * Throw an exception if the renderer is destroyed, if it's not rendering render passes, if atom
	 * is null, or if atom's state is not supported. */
	public void applyInfluence(InfluenceAtom atom, float influence) throws RenderException;
	
	/** Render the given RenderAtom.  This involves applying the atom's appearance, adjusting the model transform
	 * and actually rendering the geometry.  It should take into account any applied influences since the last
	 * call to renderAtom().  After rendering is finished, it should clear the list of applied influences.
	 * 
	 * When applying the atom's appearance, there is a notion of a default appearance:
	 *  1. If not present in atom's appearance, the atom should have a material, draw styles, and depth test
	 *  	as created by those state's default constructors.
	 *  2. If any other state isn't present, that state type should not effect the rendering of the atom,
	 *  	unless there is a special contract with an implementation of the renderer.
	 *  
	 * If the Geometry to be rendered returns true from isAppearanceIgnored(), only the default Appearance
	 * should be used.
	 * 
	 * This method should not be called directly, instead it is to be used by RenderQueue implementations.
	 * The render atom will not be rendered if it has a null geometry or null transform.  However, influences
	 * must still be cleared in this case.
	 * 
	 * Return the number of polygons rendered for this atom.
	 * 
	 * Throw an exception if the renderer is destroyed, if it's not rendering render passes, if
	 * atom is null, or if any geometries, resources or states used by the atom are unsupported. */
	public int renderAtom(RenderAtom atom) throws RenderException;
}
