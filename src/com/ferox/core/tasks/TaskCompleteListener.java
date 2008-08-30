package com.ferox.core.tasks;

public interface TaskCompleteListener {
	public void taskComplete(Task task, TaskExecutor executor);
}
