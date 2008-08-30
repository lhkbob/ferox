package com.ferox.core.tasks;

public abstract class SilentTask implements Task {

	public void addTaskCompleteListener(TaskCompleteListener l) {
		// do nothing
	}

	public void notifyTaskComplete(TaskExecutor exec) {
		// do nothing
	}

	public void removeTaskCompleteListener(TaskCompleteListener l) {
		// do nothing
	}
}
