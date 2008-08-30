package com.ferox.core.renderer;

import java.util.ArrayList;

import com.ferox.core.scene.SpatialTree;
import com.ferox.core.scene.View;
import com.ferox.core.system.RenderSurface;
import com.ferox.core.system.SystemCapabilities;
import com.ferox.core.tasks.Task;
import com.ferox.core.tasks.TaskCompleteListener;
import com.ferox.core.tasks.TaskExecutor;
import com.ferox.core.util.FeroxException;
import com.ferox.core.util.TimeSmoother;

/**
 * RenderManager is the glue that manages rendering a number of passes onto a context.  A RenderManager is
 * assigned a RenderContext at construction, and afterwards no other manager can use that context.
 * The RenderManager provides ways listen to various events: initialization, frame changes, and resizing of the
 * context.  It also lets you register Updatable's to be updated each frame.
 * 
 * Generally you will only need one RenderManager for an application, but if you need other windows, you will
 * have to create multiple RenderManagers and contexts.  However, you are able to uses scenes and states in
 * multiple RenderManagers (you might just run out of more graphics card memory if you have a large number of 
 * vbos and textures because each context needs a copy).
 * 
 * @author Michael Ludwig
 *
 */
public class RenderManager implements TaskExecutor {
	public static final int AP_START_FRAME = 0;
	public static final int AP_END_FRAME = 1;
	
	private static final int[] aPs = new int[] {AP_START_FRAME, AP_END_FRAME};
	
	private static int managerCounter = 0;
	private static SystemCapabilities caps;
	
	private int id;
	private ArrayList<RenderPass> passes;
	
	private ArrayList<Updatable> updates;
	
	private RenderContext renderContext;
	private int oldWidth, oldHeight;
	
	private ArrayList<FrameListener> frameListeners;
	private ArrayList<InitializationListener> initListeners;
	private ArrayList<InitializationListener> tempInitListeners;
	private ArrayList<ReshapeListener> reshapeListeners;
	
	private boolean isRendering;
	private boolean isDestroyed;
	
	private TimeSmoother timer;
	private long lastTime;
	private FrameStatistics stats;
	
	private ArrayList<Task>[] taskStore;
	private ArrayList<Task>[] tasks;
	private ArrayList<TaskCompleteListener> listeners;

	
	/**
	 * Creates a RenderManager with the given RenderContext.  An exception is thrown if the given
	 * context has been assigned to another render manager (ie its getRenderManager() method returns a non-null
	 * value).  The context can't be null, either.
	 */
	@SuppressWarnings("unchecked")
	public RenderManager(RenderContext context) {
		this.id = managerCounter++;
		this.passes = new ArrayList<RenderPass>();
		this.updates = new ArrayList<Updatable>();
		
		this.frameListeners = new ArrayList<FrameListener>();
		this.initListeners = new ArrayList<InitializationListener>();
		this.tempInitListeners = new ArrayList<InitializationListener>();
		this.reshapeListeners = new ArrayList<ReshapeListener>();
		
		this.oldHeight = -1;
		this.oldWidth = -1;
		
		this.isDestroyed = false;
		this.isRendering = false;
		this.timer = new TimeSmoother();
		
		this.tasks = new ArrayList[2];
		this.tasks[0] = new ArrayList<Task>();
		this.tasks[1] = new ArrayList<Task>();
		
		this.taskStore = new ArrayList[2];
		this.taskStore[0] = new ArrayList<Task>();
		this.taskStore[1] = new ArrayList<Task>();
		
		if (context == null)
			throw new NullPointerException("Can't create a manager with a null context");
		if (context.manager != null)
			throw new IllegalArgumentException("Can't reuse a RenderContext that is already attached to a RenderManager");
		this.renderContext = context;
		this.renderContext.manager = this;
		
		if (caps == null)
			caps = this.renderContext.getCapabilities();
	}
	
	/**
	 * Convenience method to get the spatial tree of the first pass.
	 */
	public SpatialTree getSpatialTree() {
		if (this.passes.size() > 0)
			return this.passes.get(0).getSpatialTree();
		return null;
	}
	
	/**
	 * Convenience method to set the spatial tree of the first pass.  If there is no
	 * pass, a new one is created with the given scene and a default view.  Disables 
	 * the previously set scene from being updated, and enables auto updating on the passed
	 * scene if its non-null.
	 */
	public void setSpatialTree(SpatialTree scene) {
		SpatialTree pt = null;
		if (this.passes.size() == 0)
			this.addRenderPass(new RenderPass(scene, new View()));
		else {
			pt = this.passes.get(0).getSpatialTree();
			this.passes.get(0).setSpatialTree(scene);
		}
		
		if (pt != null)
			this.disableUpdate(pt);
		if (scene != null)
			this.enableUpdate(scene);
	}
	
	/**
	 * Convenience method to get the View of the first pass.
	 */
	public View getView() {
		if (this.passes.size() > 0)
			return this.passes.get(0).getView();
		return null;
	}
	
	/**
	 * Convenience method to set the view of the first pass.  If no pass exists, creates a new
	 * pass with the view and a null spatial tree.
	 */
	public void setView(View view) {
		if (this.passes.size() == 0)
			this.addRenderPass(new RenderPass(null, view));
		else
			this.passes.get(0).setView(view);
	}
	
	/**
	 * Registers the Updatable to be updated right before passes are rendered, but after frame listeners
	 * have been notified.
	 */
	public void enableUpdate(Updatable up) {
		if (!this.updates.contains(up)) {
			this.updates.add(up);
		}
	}
	
	/**
	 * Removes the Updatable, so that it is no longer updated each frame.
	 */
	public boolean disableUpdate(Updatable up) {
		return this.updates.remove(up);
	}
	
	/**
	 * Whether or not up is updated.
	 */
	public boolean isUpdated(Updatable up) {
		return this.updates.contains(up);
	}
	
	/**
	 * Remove all registered Updatables.
	 */
	public void clearUpdates() {
		this.updates.clear();
	}
	
	/**
	 * Whether or not this RenderManager is actively rendering on its owning thread.
	 */
	public boolean isRendering() {
		return this.isRendering;
	}
	
	/**
	 * Whether or not this RenderManager has had it contents destroyed, it is illegal to attempt to
	 * render a destroyed RenderManager.
	 */
	public boolean isDestroyed() {
		return this.isDestroyed;
	}
	
	/**
	 * Destroy the contents of this RenderManager.  Clears listeners and passes and calls
	 * destroyContext() on its RenderContext.
	 */
	public void destroy() {
		if (!this.isDestroyed) {
			this.isDestroyed = true;
			
			this.renderContext.destroyContext();
			this.renderContext = null;
			
			this.reshapeListeners = null;
			this.frameListeners = null;
			this.initListeners = null;
			this.passes = null;
		}
	}
	
	/**
	 * Get the RenderContext used by this RenderManager.  If the RenderContext's isCurrent() method returns
	 * true, then it is valid to use the internal resources of the the context.  Any listener, atom or pass
	 * implementation will only have their worker methods called when the context is valid, so they don't
	 * need to waste time checking.
	 */
	public RenderContext getRenderContext() {
		return this.renderContext;
	}
	
	/**
	 * Get the frame rate of the simulation
	 */
	public float getFrameRate() {
		return this.timer.getFrameRate();
	}
	
	/**
	 * Get the RenderSurface that this manager renders to.
	 */
	public RenderSurface getRenderingSurface() {
		return this.renderContext.getRenderSurface();
	}
	
	/**
	 * Get the unique id for this manager.
	 */
	public int getManagerID() {
		return this.id;
	}
	
	/**
	 * Add a FrameListener to this RenderManager.  Returns true if it hasn't already been added.
	 */
	public boolean addFrameListener(FrameListener frame) {
		if (!this.frameListeners.contains(frame)) {
			this.frameListeners.add(frame);
			return true;
		}
		return false;
	}
	
	/**
	 * Add an InitializationListener to this RenderManager.  Returns true if it hasn't already
	 * been added.
	 */
	public boolean addInitializationListener(InitializationListener init) {
		if (!this.initListeners.contains(init)) {
			this.initListeners.add(init);
			if (this.getRenderContext().isInitialized())
				this.tempInitListeners.add(init);
			return true;
		}
		return false;
	}
	
	/**
	 * Add a RenderPass to this RenderManager.  Returns true if the pass hasn't already been added.
	 */
	public boolean addRenderPass(RenderPass pass) {
		if (!this.passes.contains(pass)) {
			this.passes.add(pass);
			return true;
		}
		return false;
	}
	
	/**
	 * Add a ReshapeListener to this RenderManager.  Returns true if the listener hasn't been added
	 * before.
	 */
	public boolean addReshapeListener(ReshapeListener shape) {
		if (!this.reshapeListeners.contains(shape)) {
			this.reshapeListeners.add(shape);
			return true;
		}
		return false;
	}
	
	/**
	 * True if the RenderManager has the given listener registered.
	 */
	public boolean containsFrameListener(FrameListener frame) {
		return this.frameListeners.contains(frame);
	}
	
	/**
	 * True if the RenderManager has the given listener registered.
	 */
	public boolean containsInitializationListener(InitializationListener init) {
		return this.initListeners.contains(init);
	}
	
	/**
	 * True if the RenderManager has the given RenderPass added.
	 */
	public boolean containsRenderPass(RenderPass pass) {
		return this.passes.contains(pass);
	}
	
	/**
	 * True if the RenderManager has the given listener registered.
	 */
	public boolean containsReshapeListener(ReshapeListener shape) {
		return this.reshapeListeners.contains(shape);
	}
	
	/**
	 * Get the listener in i'th position.
	 */
	public FrameListener getFrameListener(int i) {
		return this.frameListeners.get(i);
	}
	
	/**
	 * Get the number of registered FrameListeners.
	 */
	public int getNumFrameListeners() {
		return this.frameListeners.size();
	}
	
	/**
	 * Get the i'th InitializationListener.
	 */
	public InitializationListener getInitializationListener(int i) {
		return this.initListeners.get(i);
	}
	
	/**
	 * Get the number of InitializationListeners.
	 */
	public int getNumInitializationListeners() {
		return this.initListeners.size();
	}
	
	/**
	 * Get the i'th RenderPass.
	 */
	public RenderPass getRenderPass(int i) {
		return this.passes.get(i);
	}
	
	/**
	 * Get the number of RenderPasses added to the RenderManager.
	 */
	public int getNumRenderPasses() {
		return this.passes.size();
	}
	
	/**
	 * Get the i'th ReshapeListener.
	 */
	public ReshapeListener getReshapeListener(int i) {
		return this.reshapeListeners.get(i);
	}
	
	/**
	 * Get the number of ReshapeListeners.
	 */
	public int getNumReshapeListeners() {
		return this.reshapeListeners.size();
	}
	
	/**
	 * Remove the listener from this RenderManager. Returns true if actually removed.
	 */
	public boolean removeFrameListener(FrameListener frame) {
		return this.frameListeners.remove(frame);
	}
	
	/**
	 * Remove the listener from this RenderManager. Returns true if actually removed.
	 */
	public boolean removeInitializationListener(InitializationListener init) {
		return this.initListeners.remove(init);
	}
	
	/**
	 * Remove the RenderPass from this RenderManager. Returns true if actually removed.
	 */
	public boolean removeRenderPass(RenderPass pass) {
		return this.passes.remove(pass);
	}
	
	/**
	 * Remove the listener from this RenderManager.  Returns true if actually removed.
	 */
	public boolean removeReshapeListener(ReshapeListener shape) {
		return this.reshapeListeners.remove(shape);
	}
	
	/**
	 * Must be called by this manager's context when the context is reshaped.
	 * Not to be called by the application.
	 */
	public void notifyReshape() {
		if (this.getRenderContext().isCurrent()) {
			for (int i = this.reshapeListeners.size() - 1; i >= 0; i--) 
				this.reshapeListeners.get(i).onReshape(this, this.getRenderContext().getContextWidth(), 
													   this.getRenderContext().getContextHeight(), 
													   this.oldWidth, this.oldHeight);
			this.oldWidth = this.getRenderContext().getContextWidth();
			this.oldHeight = this.getRenderContext().getContextHeight();
		} else
			throw new FeroxException("Can't notify a RenderManager of a reshape if its context isn't current");
	}
	
	/**
	 * Must be called by this manager's context each time the context is initialized.
	 * Not to be called by the application.
	 */
	public void notifyInitialization() {
		if (this.getRenderContext().isCurrent()) {
			if (caps == null) {
				caps = this.getRenderContext().getCapabilities();
				if (caps == null)
					throw new FeroxException("Render Context must return a non-null context capabilities if it's been initialized");
			}
			for (int i = this.initListeners.size() - 1; i >= 0; i--) 
				this.initListeners.get(i).onInit(this);
			this.lastTime = System.nanoTime();
		} else
			throw new FeroxException("Can't notify a RenderManager of an initialization if its context isn't current");
	}
	
	/**
	 * Must be called by this manager's context when the context is capable of rendering on
	 * its specific thread and has set the proper default opengl state.
	 * Not to be called by the application, use render() instead.
	 * 
	 * Order of operations:
	 * 1. Execute start frame tasks
	 * 2. Execute any initialization listeners that were added after this manager's context was initialized.
	 * 3. Execute any frame listeners start frame methods
	 * 4. Update any registered Updatables
	 * 5. Render each pass in order that it was added
	 * 6. Execute any frame listeners end frame methods
	 * 7. Execute end frame tasks
	 */
	public void notifyRenderFrame() {
		if (this.getRenderContext().isCurrent()) {
			ArrayList<Task>[] t = this.tasks;
			this.tasks = this.taskStore;
			this.taskStore = t;
			
			this.taskStore[0].clear();
			this.taskStore[1].clear();
			
			if (this.tasks[AP_START_FRAME].size() > 0) {
				Task task;
				for (int i = 0; i < this.tasks[AP_START_FRAME].size(); i++) {
					task = this.tasks[AP_START_FRAME].get(i);
					task.performTask();
					task.notifyTaskComplete(this);
					for (int u = this.listeners.size() - 1; u >= 0; u--)
						this.listeners.get(u).taskComplete(task, this);
				}
			}
			
			if (this.tempInitListeners.size() > 0) {
				for (int i = this.tempInitListeners.size() - 1; i >= 0; i--)
					this.tempInitListeners.get(i).onInit(this);
				this.tempInitListeners.clear();
			}
			for (int i = this.frameListeners.size() - 1; i >= 0; i--) {
				this.frameListeners.get(i).startFrame(this);
			}
			
			for (int i = 0; i < this.updates.size(); i++)
				this.updates.get(i).update();
			for (int i = 0; i < this.passes.size(); i++) 
				this.passes.get(i).renderPass(this);
			
			for (int i = this.frameListeners.size() - 1; i >= 0; i--)
				this.frameListeners.get(i).endFrame(this);
			
			if (this.tasks[AP_END_FRAME].size() > 0) {
				Task task;
				for (int i = 0; i < this.tasks[AP_END_FRAME].size(); i++) {
					task = this.tasks[AP_END_FRAME].get(i);
					task.performTask();
					task.notifyTaskComplete(this);
					for (int u = this.listeners.size() - 1; u >= 0; u--)
						this.listeners.get(u).taskComplete(task, this);
				}
			}
		} else
			throw new FeroxException("Can't notify a RenderManager to proceed with rendering if its context isn't current");
	}
	
	public void attachTask(Task task, int attachPoint) {
		if (!this.taskStore[attachPoint].contains(task))
			this.taskStore[attachPoint].add(task);
	}
	
	public void addTaskCompleteListener(TaskCompleteListener l) {
		if (this.listeners == null)
			this.listeners = new ArrayList<TaskCompleteListener>();
		
		if (!this.listeners.contains(l))
			this.listeners.add(l);
	}
	
	public void removeTaskCompleteListener(TaskCompleteListener l) {
		this.listeners.remove(l);
		
		if (this.listeners.size() == 0)
			this.listeners = null;
	}	
	
	/**
	 * Renders a frame. Returns a new FrameStatistics object that stores the statistics about this frame.
	 */
	public FrameStatistics render() {
		if (this.isDestroyed)
			throw new FeroxException("Can't render a destroyed RenderManager");
		this.stats = new FrameStatistics();
		this.lastTime = System.nanoTime();
		this.isRendering = true;
		this.renderContext.render();
		this.isRendering = false;
		
		this.stats.setDuration(System.nanoTime() - this.lastTime);
		return this.stats;
	}
	
	/**
	 * Gets the FrameStatistics object used for the current (if isRendering() is true) or last frame.
	 */
	public FrameStatistics getFrameStatistics() {
		return this.stats;
	}
	
	public int[] getAttachPoints() {
		return aPs;
	}
	
	public String getAttachPointDescriptor(int attachPoint) {
		switch(attachPoint) {
		case AP_END_FRAME:
			return "End of a Frame";
		case AP_START_FRAME:
			return "Start of a Frame";
		default:
			return "Undefined attach point";
		}
	}
	
	/**
	 * Get the capabilities of the underlying graphics card.  Currently the capability fields are designed
	 * around an opengl standard since no directx java version exists.  Also, if null is returned, it means
	 * that no render manager was able to poll the capabilities, in which case some classes which depend on these
	 * values will behave irregularly.  Therefore it is recommended to create a RenderManager before building 
	 * any parts of the scenes.
	 */
	public static SystemCapabilities getSystemCapabilities() {
		return caps;
	}
}
