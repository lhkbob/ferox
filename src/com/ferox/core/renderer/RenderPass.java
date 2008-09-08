package com.ferox.core.renderer;

import java.util.ArrayList;

import com.ferox.core.scene.SpatialTree;
import com.ferox.core.scene.View;
import com.ferox.core.states.StateAtom;
import com.ferox.core.states.StateUnit;
import com.ferox.core.tasks.Task;
import com.ferox.core.tasks.TaskCompleteListener;
import com.ferox.core.tasks.TaskExecutor;

/**
 * A RenderPass renders a SpatialTree from a given View.  To be rendered, it must be added to a RenderManager 
 * where it is rendered after any previously added passes (unless they are removed).  The default implementation
 * renders to the back (or front if not double-buffered) buffer of the screen.  Subclasses can be used in conjunction
 * with RenderPassPeers (for context specific code) to provide Render-to-texture functionality.
 * 
 * As a task executor, RenderPass supports adding tasks at the beginning of the pass's rendering (before any 
 * buffers have been cleared and before the peer prepares this pass), and after all rendering and finalization
 * has completed.
 * 
 * Default starting settings for a RenderPass are to have only color and depth cleared, a depth clear value of 1, 
 * a clear color of black, and a stencil clear value of 0.
 * @author Michael Ludwig
 *
 */
public class RenderPass implements TaskExecutor {
	public static final int AP_BEFORE_PASS_PREP = 0;
	public static final int AP_AFTER_PASS_FINISH = 1;
	
	private static final int[] aPs = new int[] {AP_BEFORE_PASS_PREP, AP_AFTER_PASS_FINISH};
	
	private boolean stencilCleared;
	private boolean depthCleared;
	private boolean colorCleared;
	
	private int stencilClearValue;
	private float depthClearValue;
	private float[] colorClearValue;
	
	private SpatialTree scene;
	private View view;
	private RenderAtomMask raMask;
	private StateAtomFilter saMask;
	
	private ArrayList<Task>[] taskStore;
	private ArrayList<Task>[] tasks;
	private ArrayList<TaskCompleteListener> listeners;
	
	private byte[] mask;
	
	/**
	 * Creates a RenderPass without a scene or view and default settings.
	 */
	public RenderPass() {
		this(null, null);
	}
	
	/**
	 * Creates a pass with the given scene and view and default settings.
	 */
	@SuppressWarnings("unchecked")
	public RenderPass(SpatialTree scene, View view) {
		this.setSpatialTree(scene);
		this.setView(view);
		
		this.stencilCleared = false;
		this.depthCleared = true;
		this.colorCleared = true;
		
		this.stencilClearValue = 0;
		this.depthClearValue = 1f;
		this.colorClearValue = new float[] {0f, 0f, 0f, 1f};;
		
		this.tasks = new ArrayList[2];
		this.tasks[0] = new ArrayList<Task>();
		this.tasks[1] = new ArrayList<Task>();
		
		this.taskStore = new ArrayList[2];
		this.taskStore[0] = new ArrayList<Task>();
		this.taskStore[1] = new ArrayList<Task>();									   
	}
	
	/**
	 * Whether or not the stencil buffer is cleared each time before this pass is to be rendered.
	 * Default is false.
	 */
	public boolean isStencilBufferCleared() {
		return this.stencilCleared;
	}
	
	/**
	 * Get the value that the stencil buffer is cleared to if stencil clearing is enabled.
	 * Default is 0.
	 */
	public int getClearedStencil() {
		return this.stencilClearValue;
	}
	
	/**
	 * Get the value that the depth buffer is cleared to if depth clearing is enabled.
	 * Default is 1.
	 */
	public float getClearedDepth() {
		return this.depthClearValue;
	}
	
	/**
	 * Get the value that the color buffer is cleared to if color clearing is enabled.
	 * Default is [0, 0, 0, 1] or black.
	 */
	public float[] getClearedColor() {
		return this.colorClearValue;
	}
	
	/**
	 * Set stencil value that the stencil buffer will be cleared to if stencil clearing is enabled.
	 * The value is treated as an unsigned integer.
	 */
	public void setClearedStencil(int value) {
		this.stencilClearValue = value;
	}
	
	/**
	 * Set the depth value that the depth buffer will be cleared to if depth clearing is enabled.
	 * The value is clamped between 0 and 1.
	 */
	public void setClearedDepth(float value) {
		this.depthClearValue = Math.max(0f, Math.min(value, 1f));
	}
	
	/**
	 * Set the clear color, can't be null and must have 4 elements to it.  The elements should be arranged
	 * by increasing index as: red, green, blue, alpha as per all color arrays used in Ferox.
	 */
	public void setClearedColor(float[] color) throws IllegalArgumentException {
		if (color == null || color.length != 4)
			throw new IllegalArgumentException("Clear color must be non-null and have 4 elements");
		this.colorClearValue = color;
	}
	
	/**
	 * Whether or not the depth buffer is cleared at each render.
	 * Defaults to true.
	 */
	public boolean isDepthBufferCleared() {
		return this.depthCleared;
	}
	
	/**
	 * Whether or not the color buffer is cleared at each render.
	 * Defaults to true.
	 */
	public boolean isColorBufferCleared() {
		return this.colorCleared;
	}
	
	/**
	 * Enable/disable clearing of the stencil buffer for this pass.
	 */
	public void setStencilBufferCleared(boolean clear) {
		this.stencilCleared = clear;
	}
	
	/**
	 * Enable/disable clearing of the depth buffer for this pass.
	 */
	public void setDepthBufferCleared(boolean clear) {
		this.depthCleared = clear;
	}
	
	/**
	 * Enable/disable clearing of the color buffer for this pass.
	 */
	public void setColorBufferCleared(boolean clear) {
		this.colorCleared = clear;
	}
	
	/**
	 * StateManagers should call this method instead of a StateAtom's apply() method directly because this method
	 * correctly handles StateAtom filtering.  Also, if atom is null, the previous atom is instead restored.
	 * atom should be a subclass of the given type (not verified) and unit must be valid for the atom (fails later on).
	 */
	@SuppressWarnings("unchecked")
	public void applyState(RenderManager manager, StateAtom atom, Class<? extends StateAtom> type, StateUnit unit) {
		if (this.saMask != null && atom != null)
			atom = this.saMask.filterAtom(manager, this, atom);

		if (atom == null) {
			StateAtom prev = manager.getRenderContext().getActiveStateAtom(type, unit);
			if (prev != null && atom == null) 
				prev.restoreState(manager, unit);
		} else
			atom.applyState(manager, unit);
	}
	
	/**
	 * Set whether or not the given class of StateAtom will be masked (by not applying associated StateManagers).
	 */
	public void setStateManagerMasked(Class<? extends StateAtom> type, boolean mask) {
		this.setStateManagerMasked(RenderContext.registerStateAtomType(type), mask);
	}
	
	/**
	 * Set whether or not the given type (found by registering a type with RenderContext, or by calling
	 * getDynamicType()) will be masked.  It is preferable to use the Class method variant because
	 * dynamic types aren't always consistent between executions.
	 */
	public void setStateManagerMasked(int type, boolean mask) {
		int index = (type >> 3);
		if (this.mask == null || this.mask.length <= index) {
			byte[] t = new byte[index + 1];
			if (this.mask != null)
				System.arraycopy(this.mask, 0, t, 0, this.mask.length);
			this.mask = t;
		}
		if (mask)
			this.mask[index] |= (byte)(1 << (type - (index << 3)));
		else
			this.mask[index] &= ~(byte)(1 << (type - (index << 3)));
	}
	
	/**
	 * Whether or not the given StateAtom type will have its associated state managers masked.
	 */
	public boolean isStateManagerMasked(Class<? extends StateAtom> type) {
		return this.isStateManagerMasked(RenderContext.registerStateAtomType(type));
	}
	
	/**
	 * Whether or not the given dynamic type of state atom is masked. Recommended to use the class variant instead
	 * because of readability.
	 */
	public boolean isStateManagerMasked(int type) {
		int index = (type >> 3);
		if (this.mask == null || this.mask.length <= index)
			return false;
		return (this.mask[index] & (byte)(1 << (type - (index << 3)))) != 0;
	}
	
	/**
	 * Set the render atom mask to use with this pass when rendering a scene.  If set to null, then all
	 * atoms will be rendered.
	 */
	public void setRenderAtomMask(RenderAtomMask mask) {
		this.raMask = mask;
	}
	
	/**
	 * Get the render atom mask used for this pass. Default is null.
	 */
	public RenderAtomMask getRenderAtomMask() {
		return this.raMask;
	}
	
	/**
	 * Set the state atom filter to use when applying states through this pass.  If set to null, then 
	 * state atoms aren't filtered.
	 */
	public void setStateAtomFilter(StateAtomFilter filter) {
		this.saMask = filter;
	}
	
	/**
	 * Get the state atom filter for this pass.  Default is null.
	 */
	public StateAtomFilter getStateAtomFilter() {
		return this.saMask;
	}
	
	/**
	 * Get the spatial tree rendered by this pass.  For correct results, the tree should have its update() method
	 * called once before rendering to correctly compute world transforms and bounds.  Multiple passes can render
	 * the same scene, the tree only needs to be updated once.
	 */
	public SpatialTree getSpatialTree() {
		return this.scene;
	}

	/**
	 * Set the scene to use for this pass.  If it is null, this pass will not render anything.  See getSpatialTree().
	 * Multiple passes can share spatial trees.
	 */
	public void setSpatialTree(SpatialTree scene) {
		this.scene = scene;
	}

	/**
	 * Get the view the from which this pass's spatial tree is rendered.
	 */
	public View getView() {
		return this.view;
	}

	/**
	 * Set the view to use for this pass.  View's can be shared between pass's.  If null, then this pass will
	 * not render anything.  If view has been attached to a view node, then it is recommended that that view node
	 * be a part of this pass's spatial tree or the results will not make as much sense visually.
	 */
	public void setView(View view) {
		this.view = view;
	}

	/**
	 * Render the given pass to the manager's context if this pass has a spatial tree and view.
	 * This method calls prepareRenderPass after any before tasks are executed, but before the enabled buffers are cleared.
	 * It calls finalizeRenderPass after the rendering has completed, but before any end tasks are executed.
	 * This method clears the spatial tree's render atom bin and calls submit() on the tree.  It then renders the
	 * contents of the bin.  The spatial tree's update() method should have been called before in the frame for
	 * correct results.  Similarly, the state tree(s) used should also have been updated, it also recommended that
	 * the spatial leaves in this scene only reference a single state tree.
	 */
	public void renderPass(RenderManager manager) {
		if (this.isValid()) {
			ArrayList<Task>[] t = this.tasks;
			this.tasks = this.taskStore;
			this.taskStore = t;
			
			this.taskStore[0].clear();
			this.taskStore[1].clear();
			
			if (this.tasks[AP_BEFORE_PASS_PREP].size() > 0) {
				Task task;
				for (int i = 0; i < this.tasks[AP_BEFORE_PASS_PREP].size(); i++) {
					task = this.tasks[AP_BEFORE_PASS_PREP].get(i);
					task.performTask();
					task.notifyTaskComplete(this);
					for (int u = this.listeners.size() - 1; u >= 0; u--)
						this.listeners.get(u).taskComplete(task, this);
				}
			}
			
			this.prepareRenderPass(manager);
			
			manager.getRenderContext().clearBuffers(this.colorCleared, this.colorClearValue, this.depthCleared, this.depthClearValue, this.stencilCleared, this.stencilClearValue);
			this.view.updateWorldValues();
			
			manager.getRenderContext().setViewport(this.view.getViewLeft(), this.view.getViewRight(), this.view.getViewTop(), this.view.getViewBottom());
			manager.getRenderContext().setProjectionViewTransform(this.view);
			this.scene.submit(this.view, manager);
			this.scene.getRenderAtomBin().renderAtoms(manager, this);
			this.finalizeRenderPass(manager);
			
			if (this.tasks[AP_AFTER_PASS_FINISH].size() > 0) {
				Task task;
				for (int i = 0; i < this.tasks[AP_AFTER_PASS_FINISH].size(); i++) {
					task = this.tasks[AP_AFTER_PASS_FINISH].get(i);
					task.performTask();
					task.notifyTaskComplete(this);
					for (int u = this.listeners.size() - 1; u >= 0; u--)
						this.listeners.get(u).taskComplete(task, this);
				}
			}
		}
	}
	
	/**
	 * Returns false if the scene is null or the view is null or the view's view node is not within the scene.
	 */
	private boolean isValid() {
		if (this.scene == null || this.view == null)
			return false;
		return true;
	}
	
	/**
	 * Called before anything is rendered or prepared.  Meant to prepare the render context for whatever
	 * special means of rendering this pass uses.  Called after any before tasks are executed, but before rendering
	 * and submitting has begun.  Currently calls the default RenderPassPeer's prepare method.
	 */
	protected void prepareRenderPass(RenderManager manager) {
		manager.getRenderContext().getDefaultRenderPassPeer().prepareRenderPass(this, manager.getRenderContext());
	}
	
	/**
	 * Restore the manager's context back to default rendering behavior (ie draw everything to the back
	 * buffer in an opengl double-buffered system),  Called after rendering, but before any end tasks are executed.
	 * Currently calls the default RenderPassPeer's finish method.
	 */
	protected void finalizeRenderPass(RenderManager manager) {
		manager.getRenderContext().getDefaultRenderPassPeer().finishRenderPass(this, manager.getRenderContext());
	}
	
	public int[] getAttachPoints() {
		return aPs;
	}
	
	public String getAttachPointDescriptor(int point) {
		switch(point) {
		case AP_AFTER_PASS_FINISH: 
			return "After the RenderPass has ended";
		case AP_BEFORE_PASS_PREP:
			return "Before the RenderPass begins";
		default:
			return "Undefined attach point";
		}
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
}
