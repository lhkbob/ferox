package com.ferox.core.tasks;

public interface Task {
	public void performTask();
	public void notifyTaskComplete(TaskExecutor exec);
	
	public void addTaskCompleteListener(TaskCompleteListener l);
	public void removeTaskCompleteListener(TaskCompleteListener l);
}
