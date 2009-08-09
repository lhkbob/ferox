package com.ferox.scene;

/**
 * Provide a simple starting point for Cell implementations. It implements the
 * most simple getters and setters, as well as the update logic for handling
 * priority changes.
 * 
 * @author Michael Ludwig
 */
public abstract class AbstractCell implements Cell {
	private int lastPriority;
	private int priority;
	private Scene scene;
	
	public AbstractCell() {
		lastPriority = 0;
		priority = 0;
		scene = null;
	}

	@Override
	public int getPriority() {
		return priority;
	}

	@Override
	public Scene getScene() {
		return scene;
	}

	@Override
	public void setPriority(int priority) {
		this.priority = priority;
	}

	@Override
	public void setScene(Scene scene) {
		this.scene = scene;
	}

	/**
	 * Overridden to do handle priority changes.
	 */
	@Override
	public boolean update(float timeDelta) {
		boolean update = lastPriority != priority;
		lastPriority = priority;
		return update;
	}
}
