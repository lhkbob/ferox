package com.ferox.util.entity;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unchecked")
public final class ProcessResults {
	// FIXME: remove object results, it gets to complicated to document what each type, etc. that a controller uses
	// it should be renamed to ProcessContext, controller implementations, hmmm....
	// we have a problem with scene controller, which doesn't process results but does modify local state during each
	// process.  it has to be local though, not per-context since users must configure the cell config
	
	// okay, we'll just have ProcessContext describe which controllers ran, and document in Controller that each
	// instance is intended to be used for one process at a time.  can manually add a controller to a context if
	// you don't want it to run again -> maybe have this as another method in Controller? or a parameter to process()
	private final Map<Class<? extends Controller<?>>, Controller<?>> typeControllerMap;
	
	private final Map<Class<? extends Controller<?>>, Object> typeResultMap;
	private final Map<Controller<?>, Object> controllerResultMap;
	
	private final EntitySystem system;
	
	public ProcessResults(EntitySystem system) {
		if (system == null)
			throw new NullPointerException("EntitySystem cannot be null");
		this.system = system;
		
		typeControllerMap = new HashMap<Class<? extends Controller<?>>, Controller<?>>();
		
		typeResultMap = new HashMap<Class<? extends Controller<?>>, Object>();
		controllerResultMap = new HashMap<Controller<?>, Object>();
	}
	
	public <T extends Controller<?>> T getController(Class<T> type) {
		if (type == null)
			throw new NullPointerException("Class type cannot be null");
		if (!Controller.class.isAssignableFrom(type))
			throw new IllegalArgumentException("Class type must implement Controller: " + type);
		return (T) typeControllerMap.get(type);
	}
	
	public void addController(Controller<?> controller) {
		if (controller == null)
			throw new NullPointerException("Controller cannot be null");
		// here's where java generics falls down
		Class<? extends Controller<?>> type = (Class<? extends Controller<?>>) controller.getClass();
		typeControllerMap.put(type, controller);
	}
	
	public <R> R getResult(Controller<R> controller) {
		if (controller == null)
			throw new NullPointerException("Controller cannot be null");
		return (R) controllerResultMap.get(controller);
	}
	
	public <R, T extends Controller<R>> R getResult(Class<T> type) {
		if (type == null)
			throw new NullPointerException("Class type cannot be null");
		if (!Controller.class.isAssignableFrom(type))
			throw new IllegalArgumentException("Class type must implement Controller: " + type);
		return (R) typeResultMap.get(type);
	}
	
	public <R, T extends Controller<R>> void setResult(T controller, R result) {
		if (controller == null)
			throw new NullPointerException("Controller cannot be null");
		
		Class<T> type = (Class<T>) controller.getClass();
		if (result == null) {
			// remove result
			typeResultMap.remove(type);
			typeControllerMap.remove(type);
			
			controllerResultMap.remove(controller);
		} else {
			typeResultMap.put(type, result);
			typeControllerMap.put(type, controller);
			
			controllerResultMap.put(controller, result);
		}
	}
	
	public void clear() {
		typeResultMap.clear();
		typeControllerMap.clear();
		controllerResultMap.clear();
	}
	
	public EntitySystem getSystem() {
		return system;
	}
}
