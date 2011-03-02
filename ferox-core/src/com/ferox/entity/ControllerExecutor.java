package com.ferox.entity;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ControllerExecutor {
    private final ExecutorService service;
    
    public ControllerExecutor() {
        this(Runtime.getRuntime().availableProcessors());
    }
    
    public ControllerExecutor(int numThreads) {
        if (numThreads <= 0)
            numThreads = 1;
        service = Executors.newFixedThreadPool(numThreads);
    }
    
    public void shutdown() {
        service.shutdown();
    }
    
    public void process(Controller... controllers) {
        if (service.isShutdown())
            return;
        if (controllers == null || controllers.length == 0)
            return;
        
        Map<Controller, Future<?>> tasks = new IdentityHashMap<Controller, Future<?>>();
        Map<Controller, List<Future<?>>> dependencies = new IdentityHashMap<Controller, List<Future<?>>>();
        List<Future<?>> allTasks = new ArrayList<Future<?>>();
        
        for (int i = 0; i < controllers.length; i++) {
            Controller ci = controllers[i];
            Parallel pi = ci.getClass().getAnnotation(Parallel.class);
            
            List<Controller> controllerDeps = new ArrayList<Controller>();
            for (int j = i - 1; j >= 0; j--) {
                Controller cj = controllers[j];
                Parallel pj = cj.getClass().getAnnotation(Parallel.class);
                
                // Determine if read/writes overlap between the two Controllers.
                // If either has no Parallel annotation, assume an overlap exists.
                if (pi == null || pj == null || 
                    intersects(pi.writes(), pj.writes()) ||
                    intersects(pi.reads(), pj.writes()) || 
                    intersects(pi.writes(), pj.reads())) {
                    // Controller J writes or reads a Component type before Controller I
                    // reads or writes the same type, so we can't reorder them.
                    controllerDeps.add(controllers[j]);
                }
            }
            
            // At this point, controllerDeps has all Controllers that must complete before
            // this Controller (ci) can be executed.
            int depSize = controllerDeps.size();
            List<Future<?>> taskDeps = new ArrayList<Future<?>>();
            for (int j = 0; j < depSize; j++) {
                // Add in Future tracking the dependency
                taskDeps.add(tasks.get(controllerDeps.get(j)));
            }
            
            // Now remove all overlapping dependencies (e.g. C -> B, C -> A and B -> A, 
            // C only really needs to depend on B).
            for (int j = 0; j < depSize; j++) {
                taskDeps.removeAll(dependencies.get(controllerDeps.get(j)));
            }
            
            // Now schedule the task, ControllerTask takes care of waiting until its
            // dependencies have completed (including when they're run on separate threads)
            ControllerTask task = new ControllerTask(ci, taskDeps);
            dependencies.put(ci, taskDeps);
            
            Future<?> future = service.submit(task);
            tasks.put(ci, future);
            allTasks.add(future);
        }
        
        // Now wait on all tasks before returning
        List<Throwable> exceptions = wait(allTasks);
        if (exceptions != null) {
            // Build up a wrapper exception and throw it
            throw new ControllerExecutionException(exceptions);
        }
    }
    
    private static boolean intersects(Class<? extends Component>[] a, Class<? extends Component>[] b) {
        for (int i = 0; i < a.length; i++) {
            for (int j = 0; j < b.length; j++) {
                if (a[i].equals(b[j])) {
                    // An intersection only requires one shared Class
                    return true;
                }
            }
        }
        
        // If we've walked through both without finding a matching class,
        // then there can't be an intersection
        return false;
    }
    
    private static List<Throwable> wait(List<Future<?>> tasks) {
        List<Throwable> exceptions = null;
        
        int size = tasks.size();
        for (int i = 0; i < size; i++) {
            try {
                tasks.get(i).get();
            } catch (InterruptedException e) {
                // Ignore, there's nothing we can do at this point
                // returning control will still mean that executor is borked
            } catch (ExecutionException e) {
                // Record exception and continue waiting on others
                if (exceptions == null)
                    exceptions = new ArrayList<Throwable>();
                
                // We're only queuing Runnables so the only thrown exceptions can
                // be RuntimeExceptions so this is a safe case.
                exceptions.add(e.getCause());
            }
        }
        
        return exceptions;
    }

    /**
     * Internal class that handles running a Controller after the controller's
     * dependencies have completed.
     */
    private static class ControllerTask implements Runnable {
        private final Controller controller;
        private final List<Future<?>> dependencies;
        
        public ControllerTask(Controller controller, List<Future<?>> dependencies) {
            this.controller = controller;
            this.dependencies = dependencies;
        }
        
        @Override
        public void run() {
            // Loop through all dependencies, blocking on each get()
            // - When the loop is ended, we know that all dependencies have completed
            // - If exceptions are returned, we skip the controller since the state
            //   is most likely corrupted
            if (ControllerExecutor.wait(dependencies) != null)
                return;
            
            // Run the controller and that's it
            controller.execute();
        }
    }
}
