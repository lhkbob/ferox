package com.ferox.core.tasks;

import java.util.ArrayList;

public abstract class AbstractTask implements Task {
	private ArrayList<TaskCompleteListener> listeners;
	
	public AbstractTask() {
		this.listeners = null;
	}
	
	public void addTaskCompleteListener(TaskCompleteListener l) {
		if (this.listeners == null)
			this.listeners = new ArrayList<TaskCompleteListener>();
		
		if (!this.listeners.contains(l))
			this.listeners.add(l);
	}
	
	public void removeTaskCompleteListener(TaskCompleteListener l) {
		this.listeners.remove(l);
		
		if (this.listeners.size() == 0)
			this.listeners = null;
	}

	public void notifyTaskComplete(TaskExecutor exec) {
		for (int i = this.listeners.size() - 1; i >= 0; i--)
			this.listeners.get(i).taskComplete(this, exec);
	}
}
