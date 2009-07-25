package com.ferox.scene2;

/**
 * UpdateControllers can be used to provide custom update behavior for
 * SceneElements. One example of this is the HierarchicalTransformController
 * which emulates transform nodes in more conventional scene graphs.
 * 
 * @author Michael Ludwig
 */
public interface UpdateController {
	/**
	 * Perform an update on the given SceneElement. This method is called during
	 * a SceneElement's update() method and replaces the duties of computing the
	 * world transform.
	 * 
	 * @param element The SceneElement that's being updated
	 * @param timeDelta The time since the last update, in seconds
	 * @return True if the SceneElement needs to be reassigned to a cell
	 */
	public boolean update(SceneElement element, float timeDelta);
}
