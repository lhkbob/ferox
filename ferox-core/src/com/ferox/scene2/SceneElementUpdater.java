package com.ferox.scene2;

import com.ferox.entity.Entity;

/**
 * SceneElementUpdater can be used to provide custom update behavior for
 * SceneElements. One example of this is the HierarchicalTransformUpdater
 * which emulates transform nodes in more conventional scene graphs.
 * 
 * @author Michael Ludwig
 */
public interface SceneElementUpdater {
	/**
	 * Perform an update on the given Entity that has a SceneElement component
	 * that returns this SceneElementUpdater from
	 * {@link SceneElement#getUpdater()}. The update() method is invoked by the
	 * SceneController when appropriate. If the SceneElementUpdater modifies the
	 * element's transform or local bounds, it is responsible for also setting
	 * it as updated.
	 * 
	 * @param entity The Entity that's associated with <tt>el</tt>
	 * @param controller The SceneController that's processing the element
	 *            updates
	 * @param dt The time since the last update, in seconds
	 */
	public void update(Entity entity, SceneController controller, float dt);
}
