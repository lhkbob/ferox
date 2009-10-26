package com.ferox.renderer.impl;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Stack;

import com.ferox.renderer.DefaultResourceManager;
import com.ferox.renderer.DisplayOptions;
import com.ferox.renderer.FrameStatistics;
import com.ferox.renderer.Framework;
import com.ferox.renderer.FullscreenSurface;
import com.ferox.renderer.RenderAtom;
import com.ferox.renderer.RenderCapabilities;
import com.ferox.renderer.RenderException;
import com.ferox.renderer.RenderPass;
import com.ferox.renderer.RenderStateException;
import com.ferox.renderer.RenderSurface;
import com.ferox.renderer.Renderer;
import com.ferox.renderer.ResourceManager;
import com.ferox.renderer.SurfaceCreationException;
import com.ferox.renderer.TextureSurface;
import com.ferox.renderer.UnsupportedEffectException;
import com.ferox.renderer.UnsupportedResourceException;
import com.ferox.renderer.View;
import com.ferox.renderer.WindowSurface;
import com.ferox.renderer.impl.ResourceData.Handle;
import com.ferox.resource.Geometry;
import com.ferox.resource.Resource;
import com.ferox.resource.TextureImage;
import com.ferox.resource.Resource.Status;
import com.ferox.resource.TextureImage.TextureTarget;
import com.ferox.shader.DepthTest;
import com.ferox.shader.Effect;
import com.ferox.shader.EffectType;
import com.ferox.shader.Material;
import com.ferox.shader.PolygonStyle;
import com.ferox.util.Bag;

/**
 * <p>
 * AbstractFramework implements all of the Framework interface assuming an
 * OpenGL-like system. All a subclass requires are implementations of various
 * Drivers to perform low-level graphics calls and a ContextManager.
 * </p>
 * <p>
 * Subclasses must not set the render data on Geometries or Resources, because
 * AbstractFramework relies on them for its functionality. AbstractFramework
 * attempts to implement all higher-level functionality and bookkeeping of the
 * Framework interface. Most low-level operations should be possible using the
 * defined interfaces for Effect, Resource and Geometry drivers.
 * </p>
 * <p>
 * The methods implemented by AbstractFramework are not thread-safe, so
 * programmers should use sub-classes of AbstractFramework on a single thread.
 * With careful use, the internal drivers of the renderer may be multi-threaded.
 * </p>
 * 
 * @author Michael Ludwig
 */

public abstract class AbstractFramework implements Framework {
	/* Represents all possible states of an AbstractFramework. */
	private static enum RenderState {
		/** State of the renderer before init() is called */
		WAITING_INIT,
		/** State of the renderer when it's not doing anything */
		IDLE,
		/** State of the renderer when it's in flushRenderer(). */
		RENDERING,
		/** State of the renderer after destroy() is called */
		DESTROYED
	}

	/* Misc variables */
	private RenderState renderState;
	private RenderCapabilities renderCaps;
	private TransformDriver transform;
	private ContextManager contextManager;

	private EffectType[] supportedEffects;

	/* Resource management variables */
	private final ManageResourcesAction manageResourceAction;
	private final IdentityHashMap<Resource, Integer> resourceLocks;
	private final DefaultResourceManager dfltManager;
	// dfltManager is 0th element
	private final List<ResourceManager> resourceManagers;
	private final Stack<Resource> resourceProcessStack;

	/* Rendering frame variables */
	private final RendererImpl renderer;

	private Action actionHead;
	private Action actionTail;

	private long lastFrameTime;
	private FrameStatistics frameStats;

	/**
	 * Create a new AbstractFramework. This constructor does not completely
	 * configure the renderer for use. Subclasses must also invoke init() before
	 * their constructor completes and before they call any other methods in
	 * AbstractFramework.
	 */
	protected AbstractFramework() {
		renderState = RenderState.WAITING_INIT;

		lastFrameTime = -1;
		renderer = new RendererImpl();

		dfltManager = new DefaultResourceManager();
		resourceManagers = new ArrayList<ResourceManager>();
		// now we can simplify the manager code
		resourceManagers.add(dfltManager);
		manageResourceAction = new ManageResourcesAction();

		resourceProcessStack = new Stack<Resource>();
		resourceLocks = new IdentityHashMap<Resource, Integer>();
	}

	/* Idle operations. */

	@Override
	public final WindowSurface createWindowSurface(DisplayOptions options, 
												   int x, int y, int width, int height, 
												   boolean resizable, boolean undecorated) {
		ensure(RenderState.IDLE);
		WindowSurface surface = contextManager.createWindowSurface(options, 
																   x, y, width, height, resizable, undecorated);
		return surface;
	}

	@Override
	public final FullscreenSurface createFullscreenSurface(DisplayOptions options, 
														   int width, int height) {
		ensure(RenderState.IDLE);
		FullscreenSurface surface = contextManager.createFullscreenSurface(options,
																		   width, height);
		return surface;
	}

	@Override
	public final TextureSurface createTextureSurface(DisplayOptions options, TextureTarget target, 
													 int width, int height, int depth, int layer, 
													 int numColorTargets, boolean useDepthRenderBuffer) {
		ensure(RenderState.IDLE);
		TextureSurface surface = contextManager.createTextureSurface(options, target, 
																	 width, height, depth, layer, 
																	 numColorTargets, useDepthRenderBuffer);
		lock(surface);
		return surface;
	}

	@Override
	public final TextureSurface createTextureSurface(TextureSurface share, int layer) {
		ensure(RenderState.IDLE);
		if (share == null || share.isDestroyed() || share.getFramework() != this)
			throw new SurfaceCreationException("Invalid texture surface for sharing: " + share);

		TextureSurface surface = contextManager.createTextureSurface(share, layer);
		lock(surface);
		return surface;
	}

	@Override
	public void destroy(RenderSurface surface) {
		ensure(RenderState.IDLE);
		if (surface == null)
			throw new NullPointerException("Cannot destroy a null surface");
		if (surface.getFramework() != this)
			throw new IllegalArgumentException("Cannot destroy a surface created by another renderer: " + surface);

		if (!surface.isDestroyed()) {
			if (surface instanceof TextureSurface)
				// unlock the surface's textures
				unlock((TextureSurface) surface);

			contextManager.destroy(surface);
		}
	}

	@Override
	public void destroy() {
		ensure(RenderState.IDLE);

		// destroy all currently active surfaces
		List<RenderSurface> surfaces = new ArrayList<RenderSurface>(contextManager.getActiveSurfaces());
		for (int i = 0; i < surfaces.size(); i++)
			this.destroy(surfaces.get(i));
		contextManager.destroy();

		// allow other renderers to be created again
		renderState = RenderState.DESTROYED;
	}

	/*
	 * ResourceManager methods
	 */

	@Override
	public void addResourceManager(ResourceManager manager) {
		ensure(RenderState.IDLE);
		if (manager != null && !resourceManagers.contains(manager))
			resourceManagers.add(manager);
	}

	@Override
	public void removeResourceManager(ResourceManager manager) {
		ensure(RenderState.IDLE);
		if (manager != null)
			resourceManagers.remove(manager);
	}

	@Override
	public void requestUpdate(Resource resource, boolean forceFullUpdate) {
		ensure(RenderState.IDLE);
		if (resource == null)
			throw new NullPointerException("Cannot request the update of a null resource");
		dfltManager.requestUpdate(resource, forceFullUpdate);
	}

	@Override
	public void requestCleanUp(Resource resource) {
		ensure(RenderState.IDLE);
		if (resource == null)
			throw new NullPointerException("Cannot request the cleanup of a null resource");
		dfltManager.requestCleanup(resource);
	}

	/*
	 * Rendering methods and helper classes
	 */
	
	@Override
	public Framework queue(RenderSurface surface, RenderPass pass) {
		ensure(RenderState.IDLE);
		
		boolean found = false;
		Action n = actionHead;
		while(n != null) {
			if (n.getRenderSurface() == surface) {
				found = true;
				break;
			}
			n = n.next();
		}
		
		// let this function handle everything
		return queue(surface, pass, !found, !found, !found);
	}
	
	@Override
	public Framework queue(RenderSurface surface, RenderPass pass, boolean clearColor, boolean clearDepth, boolean clearStencil) {
		ensure(RenderState.IDLE);
		if (surface == null || pass == null || surface.isDestroyed())
			return this; // no-op
		if (surface.getFramework() != this)
			throw new IllegalArgumentException("RenderSurface must be constructed by this Framework");
		
		if (clearColor || clearDepth || clearStencil) {
			// add a ClearSurfaceAction
			ClearSurfaceAction c = new ClearSurfaceAction(surface, clearColor, clearDepth, clearStencil);
			addLast(c);
		}
		
		RenderPassAction p = new RenderPassAction(surface, pass);
		addLast(p);
		
		return this;
	}

	@Override
	public FrameStatistics renderFrame(FrameStatistics store) {
		ensure(RenderState.IDLE);

		// reset the frame statistics
		if (store == null)
			store = new FrameStatistics();

		frameStats = store;
		frameStats.reset();

		long now = System.nanoTime();
		if (lastFrameTime < 0)
			frameStats.setIdleTime(0);
		else
			frameStats.setIdleTime(now - lastFrameTime);

		try {
			long prepareStart = now;
			renderState = RenderState.RENDERING;

			// prepend the ManageResourcesAction
			addFirst(manageResourceAction);
			
			// prepare all actions
			Action c = actionHead;
			Action p = null;
			while(c != null) {
				Action n = c.prepare(p);
				p = c;
				c = n;
			}
			now = System.nanoTime();
			frameStats.setPrepareTime(now - prepareStart);

			long renderStart = now;
			contextManager.runOnGraphicsThread(actionHead);

			now = System.nanoTime();
			frameStats.setRenderTime(now - renderStart);
		} catch (Exception e) {
			throw new RenderException("Exception occurred during flushRenderer()", e);
		} finally {
			// restore the state of the renderer and prepare for the next frame
			renderState = RenderState.IDLE;
			lastFrameTime = now;
			clearActions();
		}

		// return the stats, since the frame is done
		frameStats = null;
		return store;
	}

	/* Anytime operations. */

	@Override
	public Status getStatus(Resource resource) {
		ensure(null);
		if (resource != null) {
			ResourceData data = (ResourceData) resource.getRenderData(this);
			if (data != null)
				return data.getStatus(); // will be ERROR, OK, or DIRTY
			else if (getResourceDriver(resource.getClass()) != null)
				return Status.CLEANED;
		}
		return null; // null resource or unsupported resource
	}

	@Override
	public String getStatusMessage(Resource resource) {
		ensure(null);
		if (resource != null) {
			ResourceData data = (ResourceData) resource.getRenderData(this);
			if (data != null)
				return data.getStatusMessage(); // will not be null
			else if (getResourceDriver(resource.getClass()) != null)
				return "Resource is not in use by this renderer: CLEANED";
		}
		return null; // null resource or unsupported resource
	}

	@Override
	public RenderCapabilities getCapabilities() {
		ensure(null);
		return renderCaps;
	}

	/* New method hooks. */

	/**
	 * <p>
	 * Utility method that can be called to get the Handle object of the given
	 * Resource's associated ResourceData. It will return null if the resource
	 * has a status of ERROR, if the resource is a geometry (e.g. situations in
	 * which the handle's id would be meaningless), or if the resource instance
	 * is null.
	 * </p>
	 * <p>
	 * If the given resource's status is CLEANED, the resource will be updated
	 * first, and the returned Handle is dependent on the results of that
	 * update. The update is achieved with doUpdate(resource, true, key), which
	 * is why a key must be specified.
	 * </p>
	 * <p>
	 * If null is not returned, it is guaranteed the resource has a status of OK
	 * or DIRTY, so the resource should be usable.
	 * </p>
	 * 
	 * @param resource The Resource whose handle is returned
	 * @param key The ContextManager passed into doUpdate()
	 * @return The Handle for the Resource if it has a usable handle
	 * @throws RenderStateException if graphics operations can't be performed or
	 *             if this renderer has been destroyed
	 * @throws IllegalArgumentException if key is incorrect
	 * @throws UnsupportedResourceException if resource isn't supported
	 */
	public Handle getHandle(Resource resource, ContextManager key) {
		ensure(null);
		if (!contextManager.isGraphicsThread())
			throw new RenderStateException("Graphics operations can't be performed on this thread");
		if (key != contextManager)
			throw new IllegalArgumentException("Key is incorrect: " + key);

		ResourceData data = (ResourceData) resource.getRenderData(this);
		if (data == null) {
			// will throw an UnsupportedResourceException
			doUpdate(resource, true, key);
			data = (ResourceData) resource.getRenderData(this);
		}

		return (data.isGeometry() || data.getStatus() == Status.ERROR ? null : data.getHandle());
	}

	/**
	 * <p>
	 * Identical to the method update() in Renderer except it doesn't require a
	 * Renderer to achieve it. It will throw an exception if the given
	 * ContextManager is not the ContextManager for this renderer. This is to
	 * prevent anyone from using this method; it is intended as a method hook to
	 * allow ContextManager's the ability to update resources outside of the
	 * standard Renderer paradigm.
	 * </p>
	 * <p>
	 * When a ContextManager uses this, they should pass themselves into it.
	 * However, they are responsible for making sure that a surface or context
	 * is current (as if it were about to be rendered to or have resources
	 * managed).
	 * </p>
	 * 
	 * @param resource The Resource to be updated
	 * @param forceFullUpdate True if resource's dirty descriptor is ignored
	 * @param key Must be the ContextManager used by this AbstractFramework
	 * @return The new Status of resource
	 * @throws NullPointerException if resource is null
	 * @throws IllegalArgumentException if key is incorrect
	 * @throws UnsupportedResourceException if resource is unsupported and has
	 *             no driver
	 * @throws RenderException if resource is already being updated in the call
	 *             stack
	 * @throws RenderStateException if the calling thread can't perform graphics
	 *             operations, or if the the framework is destroyed
	 */
	public Status doUpdate(Resource resource, boolean forceFullUpdate, ContextManager key) {
		ensure(null); // must make sure it's still okay to call
		if (!contextManager.isGraphicsThread())
			throw new RenderStateException("doUpdate() cannot be invoked on this thread");

		if (key != contextManager)
			throw new IllegalArgumentException("Illegal to call this method without the correct surface contextManager: " + key);
		if (resource == null)
			throw new NullPointerException("Cannot call update with a null resource");
		if (resourceProcessStack.contains(resource))
			throw new RenderException("Cannot call update on a resource actively being cleaned or updated: " + resource);

		ResourceData data = (ResourceData) resource.getRenderData(this);
		if (data == null) {
			// getResourceDriver fails if it's unsupported
			data = new ResourceData(getResourceDriver(resource.getClass()));
			resource.setRenderData(this, data);
		}

		try {
			resourceProcessStack.push(resource);
			data.driver.update(renderer, resource, data, forceFullUpdate);
			return data.getStatus();
		} finally {
			resourceProcessStack.pop();
		}
	}

	/**
	 * As doUpdate(), but mirroring the cleanUp() method.
	 * 
	 * @param resource The Resource to clean up
	 * @param key The ContextManager used by this renderer
	 * @throws NullPointerException if resource is null
	 * @throws IllegalArgumentException if key is incorrect or if the resource
	 *             is locked by a TextureSurface
	 * @throws RenderException if this resource is being cleaned in the
	 *             call-stack
	 * @throws RenderStateException if this isn't the graphics thread or if it's
	 *             destroyed
	 * @throws UnsupportedResourceException if resource is unsupported
	 */
	public void doCleanUp(Resource resource, ContextManager key) {
		ensure(null); // must make sure it's still okay to call
		if (!contextManager.isGraphicsThread())
			throw new RenderStateException("doCleanUp() cannot be invoked on this thread");

		if (key != contextManager)
			throw new IllegalArgumentException("Illegal to call this method without the correct surface contextManager: " + key);
		if (resourceLocks.containsKey(resource))
			throw new IllegalArgumentException("Cannot call cleanUp on a resource that is locked by a one or more surfaces: " + resource);
		if (resource == null)
			throw new NullPointerException("Cannot call cleanUp with a null resource");
		if (resourceProcessStack.contains(resource))
			throw new RenderException("Cannot call cleanUp on a resource actively being cleaned or updated: " + resource);

		ResourceData data = (ResourceData) resource.getRenderData(this);
		if (data != null)
			try {
				resourceProcessStack.push(resource);
				data.driver.cleanUp(renderer, resource, data);
			} finally {
				resourceProcessStack.pop();
				resource.setRenderData(this, null);
			}
		else
			// call getResourceDriver() to make sure it's supported
			getResourceDriver(resource.getClass());
	}

	/**
	 * <p>
	 * Configure the rest of the internal structures of the AbstractFramework.
	 * This method must be called within the constructor of a subclass. It is
	 * not done within AbstractFramework's constructor to allow for subclasses
	 * to detect and configure themselves.
	 * </p>
	 * <p>
	 * After this method is returned, the methods of of AbstractFramework may be
	 * used, and the renderer is put in the IDLE state.
	 * </p>
	 * 
	 * @param surfaceFactory The ContextManager to use for rendering and surface
	 *            creation
	 * @param transformDriver The TransformDriver that controls matrix
	 *            transforms
	 * @param renderCaps The RenderCapabilities of this renderer
	 * @param supportedEffects An array of Types that are supported by
	 *            getEffectDriver(), should not contain any null elements
	 * @throws NullPointerException if any of the arguments are null
	 * @throws RenderStateException if this renderer isn't in the WAITING_INIT
	 *             state.
	 */
	protected final void init(ContextManager surfaceFactory, TransformDriver transformDriver,
							  RenderCapabilities renderCaps, EffectType[] supportedEffects) {
		if (renderState != RenderState.WAITING_INIT)
			throw new RenderStateException("Method init() cannot be called more than once in AbstractFramework");

		if (surfaceFactory == null)
			throw new NullPointerException("Must pass in a non-null ContextManager");
		if (transformDriver == null)
			throw new NullPointerException("Must pass in a non-null TransformDriver");
		if (renderCaps == null)
			throw new NullPointerException("Cannot specify a non-null RenderCapabilities");
		if (supportedEffects == null)
			throw new NullPointerException("Must pass in a non-null EffectType array");

		this.renderCaps = renderCaps;
		this.supportedEffects = supportedEffects;

		contextManager = surfaceFactory;
		transform = transformDriver;

		renderState = RenderState.IDLE;
	}

	/**
	 * <p>
	 * Return the ResourceDriver associated with the given resource type. If
	 * this is called multiple times with the same class type, then this method
	 * must return the same driver previously returned. This method will be
	 * called many times when using resources, so it is best to make it as fast
	 * as possible.
	 * </p>
	 * <p>
	 * If resourceType implements Geometry, then the returned ResourceDriver
	 * must be a GeometryDriver, too.
	 * </p>
	 * 
	 * @param resourceType The class type of the Resource that should be used
	 *            with this driver, might be a Geometry class
	 * @return The ResourceDriver (or GeometryDriver) associated with
	 *         resourceType
	 * @throws UnsupportedResourceException if no driver is available for the
	 *             given resourceType
	 */
	protected abstract ResourceDriver getResourceDriver(Class<? extends Resource> resourceType);

	/**
	 * <p>
	 * Return the EffectDriver associated with the given effect type. If this is
	 * called multiple times with the same type enum, then this method must
	 * return the same driver previously returned. This method will be called
	 * many times when apply effects, so it is best to make it as fast as
	 * possible.
	 * </p>
	 * 
	 * @param effectType The EffectType that all Effects used with the returned
	 *            EffectDriver will be
	 * @return The EffectDriver associated with effectType
	 * @throws UnsupportedEffectException if no driver is available for the
	 *             given effectType
	 */
	protected abstract EffectDriver getEffectDriver(EffectType effectType);

	/* Internal operations. */
	
	// Utility to add an Action to the end of the linked-list of actions
	private void addLast(Action a) {
		if (actionTail != null)
			actionTail.setNext(a);
		actionTail = a;
		if (actionHead == null)
			actionHead = a;
	}
	
	private void addFirst(Action a) {
		a.setNext(actionHead);
		actionHead = a;
		if (actionTail == null)
			actionTail = a;
	}
	
	private void clearActions() {
		actionTail = null;
		actionHead = null;
		manageResourceAction.setNext(null);
	}

	// Locks the surface's texture buffers so they won't be updated or cleaned.
	// Expects a valid, non-null surface and assumes the surface's textures
	// haven't already been locked on this surface.
	private void lock(TextureSurface surface) {
		TextureImage t;
		for (int i = 0; i < surface.getNumColorTargets(); i++) {
			t = surface.getColorBuffer(i);
			if (t != null) {
				Integer prevCount = resourceLocks.get(t);
				if (prevCount == null)
					resourceLocks.put(t, 1);
				else
					resourceLocks.put(t, prevCount.intValue() + 1);
			}
		}
		t = surface.getDepthBuffer();
		if (t != null) {
			Integer prevCount = resourceLocks.get(t);
			if (prevCount == null)
				resourceLocks.put(t, 1);
			else
				resourceLocks.put(t, prevCount.intValue() + 1);
		}
	}

	// Unlocks the surface's texture buffers so that they can be cleaned up.
	// Expects a valid, non-null surface. Assumes the surface's textures haven't
	// already been unlocked from this surface.
	private void unlock(TextureSurface surface) {
		TextureImage t;
		for (int i = 0; i < surface.getNumColorTargets(); i++) {
			t = surface.getColorBuffer(i);
			if (t != null) {
				int count = resourceLocks.get(t).intValue() - 1;
				if (count == 0)
					resourceLocks.remove(t);
				else
					resourceLocks.put(t, count);
			}
		}
		t = surface.getDepthBuffer();
		if (t != null) {
			int count = resourceLocks.get(t).intValue() - 1;
			if (count == 0)
				resourceLocks.remove(t);
			else
				resourceLocks.put(t, count);
		}
	}

	// Throws an exception if the render state is DESTROYED or WAITING_INIT
	// If expected != null, throws an exception if the current state doesn't
	// match
	private void ensure(RenderState expected) {
		if (renderState == RenderState.WAITING_INIT || renderState == RenderState.DESTROYED)
			throw new RenderStateException("Method call invalid when Framework is in state: " + renderState);

		if (expected != null && expected != renderState)
			throw new RenderStateException("Method call expected the Framework to be in state: " + 
										   expected + ", but it was in state: " + renderState);
	}
	
	/* The action that will clear each surface. */
	private class ClearSurfaceAction extends Action {
		private final boolean clearColor;
		private final boolean clearDepth;
		private final boolean clearStencil;
		
		public ClearSurfaceAction(RenderSurface surface, boolean clearColor, boolean clearDepth, boolean clearStencil) {
			super(surface);
			this.clearColor = clearColor;
			this.clearDepth = clearDepth;
			this.clearStencil = clearStencil;
		}

		@Override
		public void perform() {
			contextManager.clear(getRenderSurface(), clearColor, clearDepth, clearStencil);
		}
	}

	/* The render action for each RenderPass. */
	private class RenderPassAction extends Action {
		private final RenderPass pass;
		private View view;

		public RenderPassAction(RenderSurface surface, RenderPass pass) {
			super(surface);
			this.pass = pass;
		}
		
		@Override
		public Action prepare(Action previous) {
			view = pass.getView();
			if (view == null) {
				// splice out
				return splice(previous);
			} else {
				// move on normally
				view.updateView();
				return super.prepare(previous);
			}
		}

		@Override
		public void perform() {
			try {
				// set at the beginning so each renderAtom() doesn't have
				// to queue up the default appearance, too
				renderer.setAppearance(renderer.dfltAppearance);

				if (view != null) {
					transform.setView(view, getRenderSurface().getWidth(), getRenderSurface().getHeight());
					pass.render(renderer);
					transform.resetView();
				}
			} finally {
				// reset everything
				renderer.reset();
			}
		}
	}

	/*
	 * Class that performs only the managing of resources during a frame. This
	 * is passed in as the 2nd argument to renderFrame() of the surface
	 * contextManager.
	 */
	private class ManageResourcesAction extends Action {
		public ManageResourcesAction() {
			super(null);
		}
		
		@Override
		public void perform() {
			int numManagers = resourceManagers.size();
			for (int i = 0; i < numManagers; i++)
				resourceManagers.get(i).manage(renderer);

			// restore state if someone invoked renderAtom(), too
			renderer.reset();
		}
	}

	/* Internal implementation of Renderer */
	private class RendererImpl implements Renderer {
		private final Bag<Effect> dfltAppearance;
		private GeometryDriver lastGeometryDriver;

		public RendererImpl() {
			dfltAppearance = new Bag<Effect>(3);
			dfltAppearance.add(new DepthTest());
			dfltAppearance.add(new Material());
			dfltAppearance.add(new PolygonStyle());
		}

		@Override
		public Status update(Resource resource, boolean forceFullUpdate) {
			return doUpdate(resource, forceFullUpdate, contextManager);
		}

		@Override
		public void cleanUp(Resource resource) {
			doCleanUp(resource, contextManager);
		}

		@Override
		public int renderAtom(RenderAtom atom) {
			if (atom == null)
				throw new NullPointerException("Cannot call renderAtom with a null RenderAtom");
			if (!contextManager.isGraphicsThread())
				throw new RenderStateException("renderAtom() cannot be invoked on this thread");

			Geometry geom = atom.getGeometry();
			ResourceData gd = (ResourceData) geom.getRenderData(AbstractFramework.this);
			if (gd == null) {
				// haven't seen it before, so do an update and then get
				// the resource data again
				update(geom, true);
				gd = (ResourceData) geom.getRenderData(AbstractFramework.this);
			}

			if (gd.getStatus() != Status.ERROR) {
				// configure the next geom to render, since geom is for sure
				// valid
				GeometryDriver driver = (GeometryDriver) gd.driver;
				if (lastGeometryDriver != null && lastGeometryDriver != driver)
					lastGeometryDriver.reset();
				lastGeometryDriver = driver;

				// queue up the effects
				setAppearance(atom.getEffects());

				// set the model transform and render
				transform.setModelTransform(atom.getTransform());
				int polyCount = driver.render(geom, gd);
				frameStats.add(1, geom.getVertexCount(), polyCount);
				transform.resetModel();

				return polyCount; // end now
			}

			return 0;
		}

		@Override
		public Framework getFramework() {
			return AbstractFramework.this;
		}
		// Reset the effect state to that of the dflt appearance
		// and reset the geometry drivers
		private void reset() {
			EffectDriver e;
			for (int i = 0; i < supportedEffects.length; i++) {
				e = getEffectDriver(supportedEffects[i]);
				e.reset();
				e.doApply();
			}
			
			if (lastGeometryDriver != null) {
				lastGeometryDriver.reset();
				lastGeometryDriver = null;
			}
		}

		// Must only use this method for setting the active effect set
		private void setAppearance(Bag<Effect> app) {
			// queue states present in the appearance
			if (app != null) {
				Effect e;
				
				int count = app.size();
				for (int i = 0; i < count; i++) {
					e = app.get(i);
					getEffectDriver(e.getType()).queueEffect(e);
				}
			}

			// perform queued state changes, anything not queued
			// above will be reset (assuming setAppearance is the
			// only source of effect changes
			for (int i = 0; i < supportedEffects.length; i++)
				getEffectDriver(supportedEffects[i]).doApply();
		}
	}
}
