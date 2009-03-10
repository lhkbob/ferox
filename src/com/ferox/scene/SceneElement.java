package com.ferox.scene;

import com.ferox.renderer.RenderQueue;
import com.ferox.renderer.View;

/** Common interface that all scene elements must implement. Only current implementations are 
 * Node-based classes.
 * 
 * @author Michael Ludwig
 *
 */
// TODO: add pick requests
public interface SceneElement{
	/** Specifies policy on the results of visit(...) and how to proceed.
	 * 
	 * FAIL = The calling element's visit(...) achieved no real end (e.g. didn't modify RenderQueue) and didn't proceed to any children.
	 * SUCCESS_ALL = The calling element's visit(...) was successful and forced all (if it had any) of it's children to successfully visit, too.
	 * SUCCESS_PARTIAL = The calling element's visit(...) was successful, but it's children determine their own visit results. */
	public static enum VisitResult {
		FAIL, SUCCESS_ALL, SUCCESS_PARTIAL
	}
	
	/** Updates this scene element, and any children/related objects it may have  */
	public void update(boolean initiator);
	
	/** Called when this element is visited during a scene traversal.  parentResult is the ViewResult returned
	 * by this element's parent, if it has one (otherwise pass in null). Must return a non-null result. 
	 * Implementations can assume that the view's world cache's are up to date. */
	public VisitResult visit(RenderQueue RenderQueue, View view, VisitResult parentResult);
}
