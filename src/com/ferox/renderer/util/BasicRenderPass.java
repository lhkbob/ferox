package com.ferox.renderer.util;

import java.util.EnumSet;

import com.ferox.renderer.RenderPass;
import com.ferox.renderer.RenderQueue;
import com.ferox.renderer.Renderer;
import com.ferox.renderer.View;
import com.ferox.scene.SceneElement;
import com.ferox.state.State.Role;

/** 
 * This class provides a basic implementation of RenderPass.  It
 * allows programmers to set the root scene element (their responsibility
 * to make sure it's actually the root) and the view.
 * 
 * It also provides the ability to set state masks (not part of the normal interface).
 * Applications can specify whether or not the scene is updated during preparePass().
 * By default it will not update the pass.
 * 
 * @author Michael Ludwig
 *
 */
public class BasicRenderPass implements RenderPass {
	private EnumSet<Role> maskedTypes;
	
	private RenderQueue renderQueue;
	private SceneElement scene;
	private View view;
	
	public boolean updateScene;
	
	/** Creates a BasicRenderPass with the given root element and view.
	 * Creates a new state sorting RenderQueue for its use. */
	public BasicRenderPass(SceneElement element, View view) {
		this(element, view, null, false);
	}
	
	/** Creates a render pass with the given root element and view.
	 * It also uses the given RenderQueue if it's not null.  It is the
	 * application programmer's responsibility that the given RenderQueue isn't
	 * used by any other render pass.
	 * 
	 * If the RenderQueue passed in is null, creates a new state sorting RenderQueue. */
	public BasicRenderPass(SceneElement element, View view, RenderQueue RenderQueue, boolean updateScene) {
		this.renderQueue = (RenderQueue == null ? new StateSortingRenderQueue() : RenderQueue);
		this.setScene(element);
		this.setView(view);
		this.setSceneUpdated(updateScene);
	}
	
	/** Get whether or not the scene element will be updated in preparePass().
	 * Default value is false. */
	public boolean isSceneUpdated() {
		return this.updateScene;
	}
	
	/** Set whether or not the scene element rendered by this render pass
	 * should be updated by this pass's preparePass() method.  If it's false,
	 * it is the application's responsibility to update() the scene when needed. */
	public void setSceneUpdated(boolean update) {
		this.updateScene = update;
	}
	
	/** Set the scene to use for this pass.  If it's null, this implementation will
	 * return false in preparePass() so that this pass is ignored by the renderer. */
	public void setScene(SceneElement scene) {
		this.scene = scene;
	}
	
	/** Set the view to use for this pass.  If it's null, null will be
	 * returned by preparePass(). */
	public void setView(View view) {
		this.view = view;
	}
	
	/** Return the View object that was last set with setView().  This view
	 * will be returned by preparePass(), too. */
	public View getView() {
		return this.view;
	}
	
	/** Return the RenderQueue passed into the constructor, or a StateSortingRenderQueue if the constructor arg was null. */
	@Override
	public RenderQueue getRenderQueue() {
		return this.renderQueue;
	}

	/** Return the SceneElement that will be used to fill this
	 * render pass's RenderQueue object. */
	public SceneElement getScene() {
		return this.scene;
	}

	/** If getView() and getScene() aren't null, then this method will clear
	 * the queue, update the view's cache, update the scene if isSceneUpdate()
	 * returns true, and then visit the scene with the render queue.  Finally
	 * it will return the view object.
	 * 
	 * If either of the view or scene are null, then null is returned. */
	@Override
	public View preparePass(Renderer renderer) {
		if (this.scene == null || this.view == null)
			return null;
		this.renderQueue.clear();
		this.view.updateView();
		if (this.updateScene)
			this.scene.update(true);
		
		this.scene.visit(this.renderQueue, this.view, null);
		return this.view;
	}
	
	@Override
	public boolean isRoleMasked(Role role) {
		return role != null && this.maskedTypes != null && this.maskedTypes.contains(role);
	}

	/** Set the masked boolean for the given state type. */
	public void setStateMasked(Role role, boolean mask) {
		if (role == null)
			return; 
		
		if (mask) {
			if (this.maskedTypes == null)
				this.maskedTypes = EnumSet.of(role);
			else
				this.maskedTypes.add(role);
		} else {
			if (this.maskedTypes != null)
				this.maskedTypes.remove(role);
		}
	}
}
