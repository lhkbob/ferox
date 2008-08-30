package com.ferox.core.tasks;

public interface TaskExecutor {
	public void attachTask(Task task, int attachPoint);
	public int[] getAttachPoints();
	public String getAttachPointDescriptor(int attachPoint);
	
	public void addTaskCompleteListener(TaskCompleteListener l);
	public void removeTaskCompleteListener(TaskCompleteListener l);
}
