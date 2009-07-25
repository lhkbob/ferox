package com.ferox.scene2;

public abstract class AbstractCell implements Cell {
	private int priority;
	private Scene scene;

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
	 * Overridden to do nothing by default.
	 */
	@Override
	public boolean update(float timeDelta) {
		return false;
	}
}
