package com.ferox.renderer;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class ThreadQueueManager {
	public static final String DEFAULT_SURFACE_GROUP = "";
	
	private final Framework framework;
	
	private final WeakHashMap<Surface, String> surfaceGroups;
	private final Map<String, List<QueueEvent>> groupQueues;
	
	private final Object lock = new Object();
	
	public ThreadQueueManager(Framework framework) {
		if (framework == null)
			throw new NullPointerException("Framework cannot be null");
		this.framework = framework;
		surfaceGroups = new WeakHashMap<Surface, String>();
		groupQueues = new HashMap<String, List<QueueEvent>>();
	}
	
	public Framework getFramework() {
		return framework;
	}
	
	public void queue(Surface surface, RenderPass pass) {
		queue(getSurfaceGroup(surface), new QueueEvent(framework, surface, pass));
	}
	
	public void flush(String group) {
		if (group == null)
			throw new NullPointerException("Group cannot be null");
		synchronized(lock) {
			List<QueueEvent> queue = groupQueues.get(group);
			if (queue != null)
				flush(queue);
		}
	}
	
	public void flushAll() {
		synchronized(lock) {
			for (List<QueueEvent> queue: groupQueues.values())
				flush(queue);
		}
	}
	
	public String getSurfaceGroup(Surface surface) {
		if (surface == null)
			throw new NullPointerException("Surface cannot be null");
		synchronized(lock) {
			String group = surfaceGroups.get(surface);
			return (group == null ? DEFAULT_SURFACE_GROUP : group);
		}
	}
	
	public void setSurfaceGroup(Surface surface, String group) {
		if (surface == null || group == null)
			throw new NullPointerException("Arguments cannot be null");
		synchronized(lock) {
			surfaceGroups.put(surface, group);
		}
	}
	
	private void flush(List<QueueEvent> queue) {
		// assumes that the organizer is already locked
		for (QueueEvent e: queue)
			e.execute();
		queue.clear();
	}
	
	private void queue(String group, QueueEvent event) {
		synchronized(lock) {
			List<QueueEvent> queue = groupQueues.get(group);
			if (queue == null) {
				queue = new LinkedList<QueueEvent>();
				groupQueues.put(group, queue);
			}

			queue.add(event);
		}
	}
	
	private static class QueueEvent {
		private final Framework framework;
		private final Surface surface;
		private final RenderPass pass;
		
		public QueueEvent(Framework framework, Surface surface, RenderPass pass) {
		    if (surface == null)
                throw new NullPointerException("Surface cannot be null");
            if (pass == null)
                throw new NullPointerException("RenderPass cannot be null");
            if (surface.getFramework() != framework)
                throw new IllegalArgumentException("Surface was not created by appropriate Framework");
            
			this.framework = framework;
			this.surface = surface;
			this.pass = pass;
		}
		
		public void execute() {
		    framework.queue(surface, pass);
		}
	}
}
