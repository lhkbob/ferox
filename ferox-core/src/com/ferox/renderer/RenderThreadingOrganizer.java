package com.ferox.renderer;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import com.ferox.math.Color4f;

public class RenderThreadingOrganizer {
	public static final String DEFAULT_SURFACE_GROUP = "";
	
	private final Framework framework;
	
	private final WeakHashMap<RenderSurface, String> surfaceGroups;
	private final Map<String, List<QueueEvent>> groupQueues;
	
	private final Object lock = new Object();
	
	public RenderThreadingOrganizer(Framework framework) {
		if (framework == null)
			throw new NullPointerException("Framework cannot be null");
		this.framework = framework;
		surfaceGroups = new WeakHashMap<RenderSurface, String>();
		groupQueues = new HashMap<String, List<QueueEvent>>();
	}
	
	public Framework getFramework() {
		return framework;
	}
	
	public void queue(RenderSurface surface, RenderPass pass) {
		queue(getSurfaceGroup(surface), new QueueEvent(framework, surface, pass));
	}
	
	public void queue(RenderSurface surface, RenderPass pass, 
					  boolean clearColor, boolean clearDepth, boolean clearStencil) {
		queue(getSurfaceGroup(surface), new QueueEvent(framework, surface, pass, clearColor, clearDepth, clearStencil));
	}
	
	public void queue(RenderSurface surface, RenderPass pass, 
					  boolean clearColor, boolean clearDepth, boolean clearStencil, 
					  Color4f color, float depth, int stencil) {
		queue(getSurfaceGroup(surface), new QueueEvent(framework, surface, pass, clearColor, clearDepth, clearStencil,
									  				   color, depth, stencil));
	}
	
	public void queue(String group, RenderSurface surface, RenderPass pass,
					  boolean clearColor, boolean clearDepth, boolean clearStencil,
					  Color4f color, float depth, int stencil) {
		if (group == null)
			throw new NullPointerException("Group cannot be null");
		
		queue(group, new QueueEvent(framework, surface, pass, clearColor, clearDepth, clearStencil, 
									color, depth, stencil));
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
	
	public String getSurfaceGroup(RenderSurface surface) {
		if (surface == null)
			throw new NullPointerException("Surface cannot be null");
		synchronized(lock) {
			String group = surfaceGroups.get(surface);
			return (group == null ? DEFAULT_SURFACE_GROUP : group);
		}
	}
	
	public void setSurfaceGroup(RenderSurface surface, String group) {
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
		private static final int BASIC = 0;
		private static final int BOOLEAN_ONLY = 1;
		private static final int BOOLEAN_AND_COLOR = 2;
		
		private final Framework framework;
		private final RenderSurface surface;
		private final RenderPass pass;
		
		private final boolean clearColor;
		private final boolean clearDepth;
		private final boolean clearStencil;
		
		private final Color4f color;
		private final float depth;
		private final int stencil;
		
		private final int mode;
		
		public QueueEvent(Framework framework, RenderSurface surface, RenderPass pass) {
			this.framework = framework;
			this.surface = surface;
			this.pass = pass;
			
			clearColor = false;
			clearDepth = false;
			clearStencil = false;
			
			color = null;
			depth = 0f;
			stencil = 0;
			
			mode = BASIC;
			validate();
		}
		
		public QueueEvent(Framework framework, RenderSurface surface, RenderPass pass, 
			 			  boolean clearColor, boolean clearDepth, boolean clearStencil) {
			this.framework = framework;
			this.surface = surface;
			this.pass = pass;
			
			this.clearColor = clearColor;
			this.clearDepth = clearDepth;
			this.clearStencil = clearStencil;
			
			color = null;
			depth = 0f;
			stencil = 0;
			
			mode = BOOLEAN_ONLY;
			validate();
		}
		
		public QueueEvent(Framework framework, RenderSurface surface, RenderPass pass,
						  boolean clearColor, boolean clearDepth, boolean clearStencil,
						  Color4f color, float depth, int stencil) {
			this.framework = framework;
			this.surface = surface;
			this.pass = pass;
			
			this.clearColor = clearColor;
			this.clearDepth = clearDepth;
			this.clearStencil = clearStencil;
			
			this.color = color;
			this.depth = depth;
			this.stencil = stencil;
			
			mode = BOOLEAN_AND_COLOR;
			validate();
		}
		
		private void validate() {
			// note that this switch purposely falls through
			switch(mode) {
			case BOOLEAN_AND_COLOR:
				if (color == null)
					throw new NullPointerException("Color cannot be null");
			case BOOLEAN_ONLY:
				// no validation on booleans
			case BASIC:
				if (depth < 0f || depth > 1f)
					throw new IllegalArgumentException("Clear depth is outside of valid range [0, 1], not: " + depth);
				if (surface == null)
					throw new NullPointerException("RenderSurface cannot be null");
				if (pass == null)
					throw new NullPointerException("RenderPass cannot be null");
				if (surface.getFramework() != framework)
					throw new IllegalArgumentException("RenderSurface was not created by appropriate Framework");
				break;
			}
		}
		
		public void execute() {
			switch(mode) {
			case BASIC:
				framework.queue(surface, pass);
				break;
			case BOOLEAN_ONLY:
				framework.queue(surface, pass, clearColor, clearDepth, clearStencil);
				break;
			case BOOLEAN_AND_COLOR:
				framework.queue(surface, pass, clearColor, clearDepth, clearStencil, color, depth, stencil);
				break;
			}
		}
	}
}
