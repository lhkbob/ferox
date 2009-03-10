package com.ferox.renderer.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import com.ferox.math.Transform;
import com.ferox.renderer.DisplayOptions;
import com.ferox.renderer.FrameStatistics;
import com.ferox.renderer.FullscreenSurface;
import com.ferox.renderer.InfluenceAtom;
import com.ferox.renderer.OnscreenSurface;
import com.ferox.renderer.RenderAtom;
import com.ferox.renderer.RenderCapabilities;
import com.ferox.renderer.RenderException;
import com.ferox.renderer.RenderPass;
import com.ferox.renderer.RenderSurface;
import com.ferox.renderer.Renderer;
import com.ferox.renderer.TextureSurface;
import com.ferox.renderer.View;
import com.ferox.renderer.WindowSurface;
import com.ferox.renderer.impl.ResourceData.Handle;
import com.ferox.renderer.util.DefaultResourceManager;
import com.ferox.resource.Geometry;
import com.ferox.resource.Resource;
import com.ferox.resource.ResourceManager;
import com.ferox.resource.TextureImage;
import com.ferox.resource.Resource.Status;
import com.ferox.resource.TextureImage.TextureTarget;
import com.ferox.state.Appearance;
import com.ferox.state.DepthTest;
import com.ferox.state.Material;
import com.ferox.state.PolygonStyle;
import com.ferox.state.State;
import com.ferox.state.State.Role;

/** AbstractRenderer implements all of the Renderer interface assuming
 * an OpenGL-like system.  All a subclass requires are implementations of various
 * Drivers to perform low-level graphics calls and a SurfaceFactory.
 * 
 * AbstractRenderer properly makes sure that there is only AbstractRenderer instance
 * at one time.
 * 
 * If this renderer is destroyed, any subsequent calls to its methods will throw
 * an exception.  In addition, there are restrictions imposed upon the Renderer
 * interface as to when certain methods can be called.  See specific methods for details.
 * 
 * Subclasses must not set the render data on States, Geometries or Resources, because
 * AbstractRenderer relies on them for its functionality.  AbstractRenderer attempts to
 * implement all higher-level functionality and bookkeeping of the Renderer interface.
 * Because of this complexity, all methods of the Renderer and set to final; needed 
 * functionality for low-level work should done via the drivers and surface factory 
 * implementations.
 * 
 * @author Michael Ludwig
 *
 */
public class AbstractRenderer implements Renderer {
	/** Represents all possible states of an AbstractRenderer. */
	public static enum RenderState {
		WAITING_INIT, /** State of the renderer before init() is called */
		IDLE,		  /** State of the renderer when it's not doing anything */
		RESOURCE,	  /** State of the renderer when it's managing resources or creating surfaces */
		PIPELINE,	  /** State of the renderer when it's okay to call applyInfluence and renderAtom */ 
		DESTROYED 	  /** State of the renderer after destroy() is called */
	}
	
	/* Class used in get/setStateData() in State */
	private static class StateData {
		AbstractRenderer renderer;
		StateDriver driver;
		int typeId;
	}
	
	private static AbstractRenderer instance = null;
	
	/* AbstractRenderer misc variables */
	private RenderState renderState;
	private RenderCapabilities renderCaps;
	
	private RenderQueuedSurfacesAction renderQueueAction;
	private ManageResourcesAction manageResourceAction;
	
	/* State management variables */
	private DriverFactory<Role, StateDriver> stateFactory; // factory to get drivers for roles not in already active
	private Map<Role, Integer> stateTypeIdMap; // mapping between state role and id
	private StateDriver[] stateDrivers; // all active drivers for roles that are in use
	private int stateTypeCounter; // counter for new state types
	private int roleMasks; // masked if typeId'th bit is a 1
	
	/* Geometry variables */
	private DriverFactory<Class<? extends Geometry>, GeometryDriver> geomFactory;
	private Map<Class<? extends Geometry>, GeometryDriver> geomDrivers;
	
	/* Resource variables */
	private DriverFactory<Class<? extends Resource>, ResourceDriver> resourceFactory;
	private Map<Class<? extends Resource>, ResourceDriver> resourceDrivers;
	private IdentityHashMap<Resource, Integer> resourceLocks;
	
	private DefaultResourceManager dfltManager; // dfltManager is 0th child of resourceManagers
	private List<ResourceManager> resourceManagers;
	private Stack<Resource> resourceProcessStack;
	
	/* Surface variables */
	private SurfaceFactory factory;
	private ContextRecordSurface[] surfaces; // access by surfaceId
	
	/* Rendering frame variables */
	private RenderPass currentPass;
	private GeometryDriver lastDriver;
	
	// used to modify the gl state to match expected defaults
	private final Appearance dfltAppearance;
	
	private FrameStatistics frameStats;
	private long lastFrameTime;
	private IdentityHashMap<RenderPass, View> preparedPasses;
	private List<ContextRecordSurface> queuedSurfaces;
	private TransformDriver transform;
	
	/** Create a new AbstractRenderer.  This constructor does not completely configure
	 * the renderer for use.  Subclasses must also invoke init() before their constructor
	 * completes or before they call any methods implemented in AbstractRenderer.
	 * 
	 * Throws an exception if there is a non-null AbstractRenderer that hasn't been
	 * destroyed yet, guaranteeing that there is only one active renderer at a time
	 * (at least that sub-class AbstractRenderer). */
	protected AbstractRenderer() throws RenderException {
		if (instance != null)
			throw new RenderException("Cannot create another AbstractRenderer, there is already an undestroyed AbstractRenderer in existence");
		instance = this;
		
		// misc ...
		this.renderState = RenderState.WAITING_INIT;
		
		this.manageResourceAction = new ManageResourcesAction(this);
		this.renderQueueAction = new RenderQueuedSurfacesAction(this);
		
		this.lastFrameTime = -1;
		this.preparedPasses = new IdentityHashMap<RenderPass, View>();
		
		this.dfltAppearance = new Appearance(new DepthTest(), new Material(), new PolygonStyle());
		
		// state ...
		this.stateTypeIdMap = new HashMap<Role, Integer>();
		this.stateDrivers = new StateDriver[0];
		this.stateTypeCounter = 0;
		this.roleMasks = 0;
		
		// geometry ...
		this.geomDrivers = new HashMap<Class<? extends Geometry>, GeometryDriver>();
		
		// resource ...
		this.resourceDrivers = new HashMap<Class<? extends Resource>, ResourceDriver>();
		this.resourceLocks = new IdentityHashMap<Resource, Integer>(64); // two textures for 32 surfaces
		
		this.dfltManager = new DefaultResourceManager();
		this.resourceManagers = new ArrayList<ResourceManager>();
		this.resourceManagers.add(this.dfltManager); // now we can simplify the flushRenderer code
		
		this.resourceProcessStack = new Stack<Resource>();
		
		// surface ...
		this.surfaces = new ContextRecordSurface[0];
		this.queuedSurfaces = new ArrayList<ContextRecordSurface>();
	}
	
	/* Idle operations. */
	
	@Override
	public final WindowSurface createWindowSurface(DisplayOptions options, int x, int y, int width, int height,
											 boolean resizable, boolean undecorated) throws RenderException {
		this.ensure(RenderState.IDLE);
		WindowSurface surface = this.validateCreatedSurface(this.factory.createWindowSurface(options, x, y, width, height, resizable, undecorated));
		return surface;
	}
	
	@Override
	public final FullscreenSurface createFullscreenSurface(DisplayOptions options, int width, int height) throws RenderException {
		this.ensure(RenderState.IDLE);
		FullscreenSurface surface = this.validateCreatedSurface(this.factory.createFullscreenSurface(options, width, height));
		return surface;
	}
	
	@Override
	public final TextureSurface createTextureSurface(DisplayOptions options, TextureTarget target, int width, int height, int depth, int layer,
											   int numColorTargets, boolean useDepthRenderBuffer) throws RenderException {
		this.ensure(RenderState.IDLE);
		TextureSurface surface = this.validateCreatedSurface(this.factory.createTextureSurface(options, target, width, height, depth, layer, numColorTargets, useDepthRenderBuffer));
		this.lock(surface);
		return surface;
	}
	
	@Override
	public final TextureSurface createTextureSurface(TextureSurface share, int layer) throws RenderException {
		this.ensure(RenderState.IDLE);
		ContextRecordSurface validShare = this.validateSurface(share);
		if (validShare == null)
			throw new RenderException("Invalid texture surface for sharing: " + share);
		TextureSurface surface = this.validateCreatedSurface(this.factory.createTextureSurface(share, layer));
		this.lock(surface);
		return surface;
	}
	
	@Override
	public final void destroy(RenderSurface surface) throws RenderException {
		this.ensure(RenderState.IDLE);
		ContextRecordSurface s = this.validateSurface(surface);
		if (s == null)
			throw new RenderException("Cannot destroy an invalid surface: " + surface);
		int id = s.getSurfaceId();

		if (s instanceof TextureSurface) {
			// unlock the surface's textures
			this.unlock((TextureSurface) s);
		}

		this.factory.destroy(s);
		this.surfaces[id] = null;
	}
	
	@Override
	public final void destroy() throws RenderException {
		this.ensure(RenderState.IDLE);
		for (int i = 0; i < this.surfaces.length; i++) {
			if (this.surfaces[i] != null && !this.surfaces[i].isDestroyed())
				this.destroy(this.surfaces[i]);
		}
		this.factory.destroy();
		
		// set a lot of the fields to be null, so gc can work even
		// if the renderer isn't discarded
		this.renderCaps = null;
		this.frameStats = null;
		this.transform = null;
		
		// states ...
		this.stateDrivers = null;
		this.stateFactory = null;
		this.stateTypeIdMap = null;
		
		// geom ...
		this.geomDrivers = null;
		this.geomFactory = null;
		
		// resource ...
		this.resourceDrivers = null;
		this.resourceFactory = null;
		this.dfltManager = null;
		this.resourceManagers = null;
		
		// surfaces ...
		this.factory = null;
		this.surfaces = null;
		this.queuedSurfaces = null;
		
		// allow other renderers to be created again
		this.renderState = RenderState.DESTROYED;
		if (instance == this)
			instance = null;
	}
	
	/*
	 * ResourceManager methods
	 */
	
	@Override
	public final void addResourceManager(ResourceManager manager) throws RenderException {
		this.ensure(RenderState.IDLE);
		if (manager != null && !this.resourceManagers.contains(manager))
			this.resourceManagers.add(manager);
	}
	
	@Override
	public final void removeResourceManager(ResourceManager manager) throws RenderException {
		this.ensure(RenderState.IDLE);
		if (manager != null)
			this.resourceManagers.remove(manager);
	}
	
	@Override
	public final void requestUpdate(Resource resource, boolean forceFullUpdate) throws RenderException {
		this.ensure(RenderState.IDLE);
		if (resource == null)
			throw new RenderException("Cannot request the update of a null resource");
		this.dfltManager.requestUpdate(resource, forceFullUpdate);
	}
	
	@Override
	public final void requestCleanUp(Resource resource) throws RenderException {
		this.ensure(RenderState.IDLE);
		if (resource == null)
			throw new RenderException("Cannot request the cleanup of a null resource");
		this.dfltManager.requestCleanup(resource);
	}
	
	/*
	 * Rendering methods and helper classes
	 */
	
	@Override
	public final Renderer queueRender(RenderSurface surface) throws RenderException {
		this.ensure(RenderState.IDLE);
		ContextRecordSurface s = this.validateSurface(surface);
		if (s == null)
			throw new RenderException("Surface is invalid and cannot be queued: " + surface);
		
		if (!this.queuedSurfaces.contains(s)) // avoid duplicates
			this.queuedSurfaces.add(s);
		return this;
	}
	
	/* Class that performs all of the graphical execution in
	 * the flushRenderer() method. */
	private static class RenderQueuedSurfacesAction implements Runnable {
		private AbstractRenderer renderer;
		
		public RenderQueuedSurfacesAction(AbstractRenderer renderer) {
			this.renderer = renderer;
		}
		
		@Override
		public void run() {
			try {
				int numSurfaces = this.renderer.queuedSurfaces.size();
				int numPasses;
				
				View view;
				ContextRecordSurface surface;
				List<RenderPass> passes;
				RenderPass pass;
				for (int i = 0; i < numSurfaces; i++) {
					surface = this.renderer.queuedSurfaces.get(i);
					this.renderer.factory.makeCurrent(surface);
					if (i == 0)
						this.renderer.manageResources();
					this.renderer.factory.clearBuffers();
					
					passes = surface.getAllRenderPasses();
					numPasses = passes.size();
					for (int p = 0; p < numPasses; p++) {
						pass = passes.get(p);
						this.renderer.currentPass = pass;
						view = this.renderer.preparedPasses.get(pass);
						if (view != null) {
							// render the pass
							this.renderer.transform.setView(view);
							pass.getRenderQueue().flush(this.renderer, view); // throws NullPointerException if there's no render queue
							this.renderer.transform.resetView();
						}
					}
					
					// swap buffers and reset drivers
					this.renderer.factory.swapBuffers();
					this.renderer.resetForNextSurface();
				}
				
				this.renderer.factory.release();
			} finally {
				this.renderer.lastDriver = null;
				for (int i = 0; i < this.renderer.stateTypeCounter; i++)
					this.renderer.stateDrivers[i].reset();
			}
		}
		
	}
	
	/* Class that performs only the managing of resources during a 
	 * frame.  This should only be used in the event that 
	 * RenderQueuedSurfacesAction wouldn't be executed. */
	private static class ManageResourcesAction implements Runnable {
		private AbstractRenderer renderer;
		
		public ManageResourcesAction(AbstractRenderer renderer) {
			this.renderer = renderer;
		}
		
		@Override
		public void run() {
			ContextRecordSurface candidate = null;
			for (int i = 0; i < this.renderer.surfaces.length; i++) {
				if (this.renderer.surfaces[i] != null) {
					candidate = this.renderer.surfaces[i];
					break;
				}
			}
			
			if (candidate == null)
				this.renderer.factory.makeShadowContextCurrent();
			else
				this.renderer.factory.makeCurrent(candidate);
			this.renderer.manageResources();

			this.renderer.factory.release();

		}
	}
	
	/** Will leave the last surface current if the AbstractRenderer was told to do so at
	 * construction time.  Similarly, the returned frame statistics will be either be
	 * the single frame statistics used, or a new each frame, depending on the constructor argument. */
	@Override
	public final FrameStatistics flushRenderer(FrameStatistics store) throws RenderException {
		this.ensure(RenderState.IDLE);
		
		// reset the frame statistics
		if (store == null)
			store = new FrameStatistics();

		this.frameStats = store;
		this.frameStats.reset();

		long now = System.nanoTime();
		if (this.lastFrameTime < 0)
			this.frameStats.setIdleTime(0);
		else
			this.frameStats.setIdleTime(now - this.lastFrameTime);
		
		long prepareStart = now;
		// make sure all surfaces are valid
		int numSurfaces = this.queuedSurfaces.size();
		this.factory.startFrame(this.queuedSurfaces); // must be called now
		
		if (numSurfaces == 0) {
			// just run the managers on the shadow context (or another surface)
			try {
				this.factory.runOnWorkerThread(this.manageResourceAction);
				this.frameStats.setPrepareTime(System.nanoTime() - prepareStart);	
			} finally {
				this.lastFrameTime = System.nanoTime();
			}
		} else {
			// we're rendering an entire frame now, resource managers will be done on 1st surface
			for (int i = 0; i < numSurfaces; i++) {
				if (this.validateSurface(this.queuedSurfaces.get(i)) == null)
					throw new RenderException("Cannot render an invalid surface: " + this.queuedSurfaces.get(i));
			}
			
			try {
				int numPasses;
				List<RenderPass> passes;
				RenderPass pass;
				// prepare every render pass present in all queued surfaces
				for (int i = 0; i < numSurfaces; i++) {
					passes = this.queuedSurfaces.get(i).getAllRenderPasses();
					numPasses = passes.size();
					
					for (int p = 0; p < numPasses; p++) {
						pass = passes.get(p);
						if (!this.preparedPasses.containsKey(pass))
							this.preparedPasses.put(pass, pass.preparePass(this));
					}
				}
				now = System.nanoTime();
				this.frameStats.setPrepareTime(now - prepareStart);
				
				long renderStart = now;
				this.renderState = RenderState.PIPELINE;
				
				// render each surface, and the passes within it
				this.factory.runOnWorkerThread(this.renderQueueAction);
				
				now = System.nanoTime();
				this.frameStats.setRenderTime(now - renderStart);
			} catch (Exception e) {
				throw new RenderException("Exception occurred during flushRenderer()", e);
			} finally {
				// restore the state of the renderer and prepare for the next frame
				this.currentPass = null;
				this.renderState = RenderState.IDLE;
				this.preparedPasses.clear();
				this.lastFrameTime = now;
			}
		}
		
		// return the stats, since the frame is done
		this.frameStats = null;
		return store;
	}
	
	/* Anytime operations. */
	
	@Override
	public final boolean isStateMasked(Role role) throws RenderException {
		this.ensure(null); // can be called in any active state
		if (role != null) {
			int id = this.getStateTypeId(role);
			return (id < 0 ? false : !this.canApplyState(id));
		} else
			return false;
	}
	
	@Override
	public final void setRoleMasked(Role role, boolean mask) throws RenderException {
		this.ensure(null);
		if (role != null) {
			int id = this.getStateTypeId(role);
			if (id >= 0) {
				int bitMask = 1 << id;
				if (mask)
					this.roleMasks |= bitMask;
				else
					this.roleMasks &= ~bitMask;
			} // else driver didn't have a StateDriver for the given Role -> do nothing
		}
	}
	
	@Override
	public final Status getStatus(Resource resource) throws RenderException {
		this.ensure(null);
		if (resource != null) {
			ResourceData data = this.getResourceData(resource, false); // don't want to create it
			if (data != null) {
				return data.getStatus(); // will be ERROR, OK, or DIRTY
			} else {
				// we don't know about, have to check if it's supported
				if (this.getResourceDriver(resource.getClass()) != null)
					return Status.CLEANED;
			}
		}
		return null; // null resource or unsupported resource
	}
	
	@Override
	public final String getStatusMessage(Resource resource) throws RenderException {
		this.ensure(null);
		if (resource != null) {
			ResourceData data = this.getResourceData(resource, false); // don't want to create it
			if (data != null) {
				return data.getStatusMessage(); // will not be null
			} else {
				// we don't know about, have to check if it's supported
				if (this.getResourceDriver(resource.getClass()) != null)
					return "Resource is cleaned-up";
			}
		}
		return null; // null resource or unsupported resource
	}
	
	/** Returns the RenderCapabilities constructed by the CapabilitiesDetector passed to init(). */
	@Override
	public final RenderCapabilities getCapabilities() throws RenderException {
		return this.renderCaps;
	}
	
	/* Resource operations. */
	
	@Override
	public final Status update(Resource resource, boolean forceFullUpdate) throws RenderException {
		this.ensure(RenderState.RESOURCE);
		return this.doUpdate(resource, forceFullUpdate, this.factory);
	}
	
	@Override
	public final void cleanUp(Resource resource) throws RenderException {
		this.ensure(RenderState.RESOURCE);
		this.doCleanUp(resource, this.factory);
	}
	
	/* Rendering operations. */
	
	@Override
	public final void applyInfluence(InfluenceAtom atom, float influence) throws RenderException {
		this.ensure(RenderState.PIPELINE);
		if (atom == null)
			throw new RenderException("Cannot call applyInfluence with a null InfluenceAtom");
		
		State s = atom.getState();
		if (influence > 0 && s != null) {
			StateData data = this.getStateData(s); // fails here if inf's state is unsupported
			if (this.canApplyState(data.typeId) && !this.currentPass.isRoleMasked(s.getRole()))
				data.driver.queueInfluenceState(s, Math.min(influence, 1f));
		}
	}
	
	@Override
	public final void renderAtom(RenderAtom atom) throws RenderException {
		this.ensure(RenderState.PIPELINE);
		if (atom == null)
			throw new RenderException("Cannot call renderAtom with a null RenderAtom");
		
		Transform model = atom.getTransform();
		Geometry geom = atom.getGeometry();
		
		ResourceData gd = this.getResourceData(geom, false);
		if (gd != null && gd.getStatus() != Status.ERROR) {
			// configure the next geom to render, since geom is for sure valid
			GeometryDriver driver = (GeometryDriver) gd.driver;
			if (this.lastDriver != null && this.lastDriver != driver)
				this.lastDriver.reset();
			this.lastDriver = driver;

			this.queueAppearance(this.dfltAppearance);
			Appearance app = atom.getAppearance();
			if (app != null) {
				this.queueAppearance(app);
			} // else... no queuing implies use default state

			// perform queued state changes
			for (int i = 0; i < this.stateTypeCounter; i++)
				this.stateDrivers[i].doApply();

			// set the model transform and render
			this.transform.setModelTransform(model); 
			this.frameStats.add(1, geom.getVertexCount(), driver.render(geom, gd)); // update stats
			this.transform.resetModel();
			
			return; // end now
		}
		
		if (this.getResourceDriver(geom.getClass()) == null)
			throw new RenderException("Cannot render an unsupported geometry type: " + geom.getClass());
		else {
			// reset the influence atoms for the next render atom
			for (int i = 0; i < this.stateTypeCounter; i++)
				this.stateDrivers[i].reset();
		}
	}
	
	/* New method hooks. */
	
	/** Configure the rest of the internal structures of the AbstractRenderer.
	 * This method must be called within the constructor of a subclass.  It
	 * is not done within AbstractRenderer's constructor to allow for subclasses
	 * to detect and configure themselves.  
	 * 
	 * After this method is returned, the methods of of AbstractRenderer may be
	 * used, and the renderer is put in the IDLE state.
	 * 
	 * Throws an exception if init() is called more than once, or if any argument
	 * is null, or if the capabilities detector returns a null render capabilities
	 * object from its detect() method. */
	protected final void init(SurfaceFactory surfaceFactory,
							  TransformDriver transformDriver,
							  DriverFactory<Class<? extends Geometry>, GeometryDriver> geomDrivers,
							  DriverFactory<Class<? extends Resource>, ResourceDriver> resourceDrivers,
							  DriverFactory<Role, StateDriver> stateDrivers,
							  CapabilitiesDetector detector) throws RenderException {
		if (this.renderState != RenderState.WAITING_INIT)
			throw new RenderException("Method init() cannot be called more than once in AbstractRenderer");
		
		if (surfaceFactory == null)
			throw new RenderException("Must pass in a non-null SurfaceFactory");
		if (detector == null)
			throw new RenderException("Must pass in a non-null capabilities detector");
		
		if (transformDriver == null)
			throw new RenderException("Must pass in a non-null TransformDriver");
		if (geomDrivers == null)
			throw new RenderException("Must pass in a non-null geometry driver factory");
		if (resourceDrivers == null)
			throw new RenderException("Must pass in a non-null resource driver factory");
		if (stateDrivers == null)
			throw new RenderException("Must pass in a non-null state driver factory");
		
		this.renderCaps = detector.detect();
		if (this.renderCaps == null)
			throw new RenderException("CapabilitiesDetector must not detect a null RenderCapabilities");
		
		this.factory = surfaceFactory;
		this.transform = transformDriver;
		this.geomFactory = geomDrivers;
		this.resourceFactory = resourceDrivers;
		this.stateFactory = stateDrivers;
		
		this.renderState = RenderState.IDLE;
	}
	
	/** Utility method that can be called to get the Handle object of the
	 * given Resource's associated ResourceData.  It will return null
	 * if the resource has a status of CLEANED or ERROR, or if the resource
	 * is a geometry (e.g. situations in which the handle's id would be meaningless),
	 * or if the resource instance is null.
	 * 
	 * Otherwise the handle will be for an OK or DIRTY resource and should be usable.
	 * 
	 * Throws an exception if not called during rendering or resource management.
	 * Also fails if the resource type is unsupported. */
	public final Handle getHandle(Resource resource) throws RenderException {
		this.ensureNot(null); // only called for PIPELINE and RESOURCE
		if (resource == null)
			return null;
		
		ResourceData data = this.getResourceData(resource, false);
		if (data == null || data.isGeometry() || data.getStatus() == Status.ERROR) {
			if (this.getResourceDriver(resource.getClass()) == null) // fail if unsupported
				throw new RenderException("Unsupported resource type: " + resource.getClass() + ", no suitable driver");
			return null;
		} else
			return data.getHandle();
	}
	
	/** Identical to the method update() except it has relaxed status requirements
	 * (it just can't be DESTROYED or WAITING_INIT).  However, it will throw an
	 * exception if the given surface factory is not the surface factory for this renderer.
	 * This is to prevent anyone from using this method; it is intended as a method
	 * hook allowing surface factories the ability to update resources outside of the RESOURCE
	 * state.
	 * 
	 * When a surface factory uses this, they should pass themselves into it.  However, they
	 * are responsible for making sure that a surface or context is current (as if it were
	 * about to be rendered to or have resources managed). */
	public final Status doUpdate(Resource resource, boolean forceFullUpdate, SurfaceFactory key) throws RenderException {
		this.ensure(null); // must make sure it's still okay to call
		
		if (key != this.factory)
			throw new RenderException("Illegal to call this method without the correct surface factory: " + key);
		if (resource == null)
			throw new RenderException("Cannot call update with a null resource");
		if (this.resourceProcessStack.contains(resource))
			throw new RenderException("Cannot call update on a resource actively being cleaned or updated: " + resource);
		
		ResourceData data = this.getResourceData(resource, true); // will fail here if unsupported
		try {
			this.resourceProcessStack.push(resource);
			data.driver.update(resource, data, forceFullUpdate);
			return data.getStatus();
		} finally {
			this.resourceProcessStack.pop();
		}
	}
	
	/** As doUpdate(), but mirroring the cleanUp() method. */
	public final void doCleanUp(Resource resource, SurfaceFactory key) throws RenderException {
		this.ensure(null); // must make sure it's still okay to call
		
		if (key != this.factory)
			throw new RenderException("Illegal to call this method without the correct surface factory: " + key);
		if (resource == null)
			throw new RenderException("Cannot call cleanUp with a null resource");
		if (this.resourceProcessStack.contains(resource))
			throw new RenderException("Cannot call cleanUp on a resource actively being cleaned or updated: " + resource);
		if (this.resourceLocks.containsKey(resource))
			throw new RenderException("Cannot call cleanUp on a resource that is locked by a one or more surfaces: " + resource);
		
		ResourceData data = this.getResourceData(resource, false);
		if (data != null) {
			try {
				this.resourceProcessStack.push(resource);
				data.driver.cleanUp(resource, data);
			} finally {
				this.resourceProcessStack.pop();
				resource.setResourceData(null);
			}
		} else {
			// nothing to clean up, just check if type is supported
			if (this.getResourceDriver(resource.getClass()) == null)
				throw new RenderException("Cannot call cleanUp on an unsupported resource type: " + resource.getClass());
		}
	}
	
	/** Return the current state of the AbstractRenderer.  This will
	 * not fail if the renderer is destroyed. */
	public final RenderState getRenderState() {
		return this.renderState;
	}
	
	/** Must only be called by the surface factory (or the surfaces created by it) to
	 * notify the Renderer when an onscreen surface was closed by the user (e.g. it's
	 * been implicitly destroyed).  This will then remove any references the AbstractRenderer
	 * had to the given surface, and assumes that the surface factory will correctly dispose
	 * of any other dangling resources of the surface.
	 * 
	 * This method does nothing if the surface is null, or wasn't created by this renderer, or if
	 * the surface has already had its references cleaned.  It is imperative that this method
	 * is only called at the appropriate times because it causes the given surface to become
	 * unusable as an argument to any of the renderer's methods. */
	public void notifyOnscreenSurfaceClosed(OnscreenSurface surface) {
		if (surface != null && surface.getRenderer() == this) {
			ContextRecordSurface target = (ContextRecordSurface) surface;
			if (this.surfaces[target.getSurfaceId()] == surface)
				this.surfaces[target.getSurfaceId()] = null; // clean-up old reference to it
		}
	}
	
	/* Internal operations. */
	
	private void queueAppearance(Appearance app) {
		// queue states present in the appearance
		List<State> states = app.getStates();
		int size = states.size();

		State s;
		StateData d;
		for (int i = 0; i < size; i++) {
			s = states.get(i);
			d = this.getStateData(s);
			if (this.canApplyState(d.typeId) && !this.currentPass.isRoleMasked(s.getRole()))
				d.driver.queueAppearanceState(s);
		}
	}
	
	// Expects there to be a current context when this is called
	// (either a surface or the shadow context).  Otherwise, this method
	// properly sets the render state and processes all resource managers.
	private void manageResources() {
		RenderState old = this.renderState;
		try {
			this.renderState = RenderState.RESOURCE;
			int numManagers = this.resourceManagers.size();
			for (int i = 0; i < numManagers; i++)
				this.resourceManagers.get(i).manage(this);
		} finally {
			this.renderState = old;
		}
	}
	
	// Reset the state and geometry drivers for the next surface
	private void resetForNextSurface() {
		if (this.lastDriver != null)
			this.lastDriver.reset();
		this.lastDriver = null;
		for (int i = 0; i < this.stateTypeCounter; i++) {
			this.stateDrivers[i].reset();
			this.stateDrivers[i].doApply(); // now make sure everything is the "default"
		}
	}
	
	// Locks the surface's texture buffers so they won't be updated or cleaned.
	// Expects a valid, non-null surface and assumes the surface's textures
	// haven't already been locked on this surface.
	private void lock(TextureSurface surface) {
		TextureImage t;
		for (int i = 0; i < surface.getNumColorTargets(); i++) {
			t = surface.getColorBuffer(i);
			if (t != null) {
				Integer prevCount = this.resourceLocks.get(t);
				if (prevCount == null)
					this.resourceLocks.put(t, 1);
				else
					this.resourceLocks.put(t, prevCount.intValue() + 1);
			}
		}
		t = surface.getDepthBuffer();
		if (t != null) {
			Integer prevCount = this.resourceLocks.get(t);
			if (prevCount == null)
				this.resourceLocks.put(t, 1);
			else
				this.resourceLocks.put(t, prevCount.intValue() + 1);
		}
	}
	
	// Unlocks the surface's texture buffers so that they can be cleaned up.
	// Expects a valid, non-null surface.  Assumes the surface's textures haven't
	// already been unlocked from this surface.
	private void unlock(TextureSurface surface) {
		TextureImage t;
		for (int i = 0; i < surface.getNumColorTargets(); i++) {
			t = surface.getColorBuffer(i);
			if (t != null) {
				int count = this.resourceLocks.get(t).intValue() - 1;
				if (count == 0)
					this.resourceLocks.remove(t);
				else
					this.resourceLocks.put(t, count);
			}
		}
		t = surface.getDepthBuffer();
		if (t != null) {
			int count = this.resourceLocks.get(t).intValue() - 1;
			if (count == 0)
				this.resourceLocks.remove(t);
			else
				this.resourceLocks.put(t, count);
		}
	}
	
	// Returns the given surface cast as a ContextRecordSurface if it was a 
	// surface created by this AbstractRenderer and hasn't been destroyed yet.
	// If it's invalid for any reason, returns null.
	private ContextRecordSurface validateSurface(RenderSurface surface) {
		if (surface == null || surface.getRenderer() != this || surface.isDestroyed())
			return null;
		ContextRecordSurface s = (ContextRecordSurface) surface;
		return (this.surfaces[s.getSurfaceId()] == s ? s : null); // just in case isDestroyed() lied.
	}
	
	// Expects a newly created surface by the surface factory.  Throws an exception
	// if the surface isn't a CRS, or it's id doesn't match, or if it's null.
	// If the surface is valid, it updates the surfaces array of the renderer.
	private <T extends RenderSurface> T validateCreatedSurface(T surface) {
		if (surface == null || !(surface instanceof ContextRecordSurface))
			throw new RenderException("Surface factory created invalid window surface");
		
		ContextRecordSurface crs = (ContextRecordSurface) surface;
		int id = crs.getSurfaceId();
		if (this.surfaces.length > id && this.surfaces[id] != null)
			throw new RenderException("Surface factory did not respect unique surface id constraint");
		if (id >= this.surfaces.length) {
			ContextRecordSurface[] temp = new ContextRecordSurface[id + 1];
			System.arraycopy(this.surfaces, 0, temp, 0, this.surfaces.length);
			this.surfaces = temp;
		}
		this.surfaces[id] = crs;
		// we're all good ...
		return surface;
	}
	
	/* Expects a non-null resource object.  Utility function to return the resource data
	 * of the given resource (or geometry).  
	 * 
	 * Returns null if the resource doesn't have a valid resource data for
	 * this resource if createOnNew is false.  If createOnNew is true and it needs a valid
	 * data, then a new one is created and set on the resource, and then returned. 
	 * 
	 * Throws an exception if createOnNew is true, and the resource type is unsupported. */
	private ResourceData getResourceData(Resource resource, boolean createOnNew) throws RenderException {
		Object d = resource.getResourceData();
		if (d == null || !(d instanceof ResourceData) || ((ResourceData) d).renderer != this) {
			if (createOnNew) {
				ResourceDriver driver = this.getResourceDriver(resource.getClass());
				if (driver == null)
					throw new RenderException("Unsupported resource type: " + resource.getClass());
				ResourceData newData = new ResourceData(this, driver);
				resource.setResourceData(newData);
				return newData; // newly created
			} else
				return null; // not a valid data, and don't return a new one
		} else
			return (ResourceData) d; // we're good to go
	}
	
	/* Expects a non-null class for a resource type.  Returns the resource driver
	 * that is associated with the type.  If the type hasn't been seen before, properly
	 * determines the driver from the various factories (resource, texture, or geometry).
	 * Correctly works with geometries.
	 * 
	 * If the type is a geometry type, the returned driver will be an instance of GeometryDriver.
	 * 
	 * Returns null if the type's driver isn't supported. */
	@SuppressWarnings("unchecked")
	private ResourceDriver getResourceDriver(Class<? extends Resource> type) {
		if (Geometry.class.isAssignableFrom(type)) {
			// handle geometries
			Class<? extends Geometry> t = (Class<? extends Geometry>) type;
			GeometryDriver d = this.geomDrivers.get(type);
			if (d == null) {
				d = this.geomFactory.getDriver(t);
				if (d != null)
					this.geomDrivers.put(t, d);
			}
			return d; // will be null if factory didn't create a new driver
		} else {
			// handle a standard resource
			ResourceDriver d = this.resourceDrivers.get(type);
			if (d == null) {
				d = this.resourceFactory.getDriver(type);
				if (d != null)
					this.resourceDrivers.put(type, d);
			}
			return d; // will be null if either factory type didn't find a driver
		}
	}
	
	// Expects a valid type id.  Return true if the type isn't masked by the Renderer.
	// In some cases, you still need to check for the render pass's state mask.
	private boolean canApplyState(int typeId) {
		return (this.roleMasks & (1 << typeId)) == 0;
	}
	
	// Expects a non-null state object.  Utility to function to make sure the State's
	// StateData is valid and then return the data.  Fails if the state type is unsupported. 
	private StateData getStateData(State state) throws RenderException {
		Object d = state.getStateData();
		if (d == null || !(d instanceof StateData) || ((StateData) d).renderer != this) {
			StateData newData = new StateData();
			newData.typeId = this.getStateTypeId(state.getRole());
			if (newData.typeId < 0) // it's unsupported
				throw new RenderException("Unsupported state type: " + state.getRole() + ", Unable to find a suitable driver");
			
			newData.driver = this.stateDrivers[newData.typeId];
			newData.renderer = this;
			state.setStateData(newData);
			
			return newData;
		} else {
			// the state is good to go
			return (StateData) d;
		}
	}
	
	/* Expects a non-null class for a state type.  Returns the int
	 * id that has been associated with that type.  If the type hasn't
	 * been seen before, properly updates the state management variables
	 * and finds a state driver for it.
	 * 
	 * Returns -1 if the type's driver isn't supported. */
	private int getStateTypeId(Role type) throws RenderException {
		Integer id = this.stateTypeIdMap.get(type);
		if (id != null) {
			return id.intValue();
		} else {
			StateDriver driver = this.stateFactory.getDriver(type);
			if (driver == null)
				return -1;
			
			// at this point, we know we can use the state type so install it
			int newId = this.stateTypeCounter++;
			this.stateTypeIdMap.put(type, newId);
			
			// store the driver
			StateDriver[] drivers = new StateDriver[this.stateTypeCounter];
			System.arraycopy(this.stateDrivers, 0, drivers, 0, newId);
			drivers[newId] = driver;
			this.stateDrivers = drivers;
			
			return newId;
		}
	}
	
	// Throws an exception if the render state is DESTROYED or WAITING_INIT
	// If expected != null, throws an exception if the current state doesn't match
	private void ensure(RenderState expected) throws RenderException {
		if (this.renderState == RenderState.WAITING_INIT || this.renderState == RenderState.DESTROYED)
			throw new RenderException("Method call invalid when Renderer is in state: " + this.renderState);
		
		if (expected != null && expected != this.renderState)
			throw new RenderException("Method call expected the Renderer to be in state: " + expected + ", but it was in state: " + this.renderState);
	}
	
	// Throws an exception if the render state is DESTROYED or WAITING_INIT
	// If unexpected != null, throws an exception if the current state matches
	private void ensureNot(RenderState unexpected) throws RenderException {
		if (this.renderState == RenderState.WAITING_INIT || this.renderState == RenderState.DESTROYED)
			throw new RenderException("Method call invalid when Renderer is in state: " + this.renderState);
		
		if (unexpected != null && unexpected == this.renderState)
			throw new RenderException("Method call invalid, Renderer shoudln't be in state: " + unexpected);
	}
}
