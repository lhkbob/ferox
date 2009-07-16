package com.ferox.scene;

import com.ferox.math.bounds.PlaneState;
import com.ferox.renderer.EffectSortingRenderQueue;
import com.ferox.renderer.RenderPass;
import com.ferox.renderer.RenderQueue;
import com.ferox.renderer.Renderer;
import com.ferox.renderer.View;

/**
 * This class provides a basic implementation of RenderPass. It allows
 * programmers to set the root scene element (their responsibility to make sure
 * it's actually the root) and the view. Applications can specify whether or not
 * the scene is updated during preparePass(). By default it will not update the
 * pass.
 * 
 * @author Michael Ludwig
 */
public class SceneRenderPass implements RenderPass {
	private final RenderQueue renderQueue;
	private final PlaneState planeState;
	
	private Node scene;
	private View view;
	
	public boolean updateScene;

	/**
	 * Creates a SceneRenderPass with the given root element and view. This
	 * constructor uses an EffectSortingRenderQueue.
	 * 
	 * @param scene The initial scene that is to be rendered
	 * @param view The initial view that will be used
	 */
	public SceneRenderPass(Node scene, View view) {
		this(scene, view, null, false);
	}

	/**
	 * Creates a render pass with the given root element and view. It also uses
	 * the given RenderQueue if it's not null. It is the application
	 * programmer's responsibility that the given RenderQueue isn't used by any
	 * other render pass.
	 * 
	 * @param scene The initial scene that is to be rendered
	 * @param view The initial view that will be used
	 * @param renderQueue The RenderQueue to use during visiting and flushing to
	 *            the render; null defaults to a new EffectSortingRenderQueue
	 */
	public SceneRenderPass(Node scene, View view, RenderQueue renderQueue, boolean updateScene) {
		this.renderQueue = (renderQueue == null ? new EffectSortingRenderQueue() : renderQueue);
		this.planeState = new PlaneState();
		
		setScene(scene);
		setView(view);
		setSceneUpdated(updateScene);
	}

	/**
	 * @return Whether or not the scene element will be updated in
	 *         preparePass().
	 */
	public boolean isSceneUpdated() {
		return updateScene;
	}

	/**
	 * Set whether or not the scene element rendered by this render pass should
	 * be updated by this pass's preparePass() method. If it's false, it is the
	 * application's responsibility to update() the scene when needed.
	 * 
	 * @param update The update policy for this pass's root Node
	 */
	public void setSceneUpdated(boolean update) {
		updateScene = update;
	}

	/**
	 * Set the scene to use for this pass. If it's null, this implementation
	 * will return false in preparePass() so that this pass is ignored by the
	 * renderer.
	 * 
	 * @param scene The new scene that this pass will render
	 */
	public void setScene(Node scene) {
		this.scene = scene;
	}

	/**
	 * Set the view to use for this pass. If it's null, null will be returned by
	 * preparePass().
	 * 
	 * @param view The new View to use (may be attached to a ViewNode in this
	 *            pass's scene)
	 */
	public void setView(View view) {
		this.view = view;
	}

	/**
	 * Return the View object that was last set with setView(). This view will
	 * be returned by preparePass().
	 * 
	 * @return The View that this scene is rendered from
	 */
	public View getView() {
		return view;
	}

	/**
	 * Return the SceneElement that will be used to fill this render pass's
	 * RenderQueue object.
	 * 
	 * @return The scene tree that will be rendered by this pass
	 */
	public Node getScene() {
		return scene;
	}

	/**
	 * If getView() and getScene() aren't null, then this method will clear the
	 * queue, update the view's cache, update the scene if isSceneUpdate()
	 * returns true, and then visit the scene with a RenderQueue. Finally it
	 * will return the view object. If either of the view or scene are null,
	 * then null is returned (effectively the pass will be ignored).
	 * 
	 * @return View returned by getView(), or null if getScene() returns null
	 */
	@Override
	public View preparePass() {
		if (scene == null || view == null)
			return null;
		renderQueue.clear();
		view.updateView();

		if (updateScene)
			scene.update();
		
		planeState.reset();
		scene.visit(renderQueue, view, planeState, null);
		return view;
	}

	@Override
	public void render(Renderer renderer, View view) {
		renderQueue.flush(renderer, view);
	}
}
