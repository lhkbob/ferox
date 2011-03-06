package com.ferox.entity;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * ControllerExecutor is a utility to manage running many Controllers in row. It
 * has support to run Controllers in parallel across multiple threads if it can
 * determine that certain controllers do not depend on the components written to
 * by each other. See {@link #execute(Controller...)} and {@link Parallel} for
 * more details.
 * 
 * @author Michael Ludwig
 */
public class ControllerExecutor {
    private final ReadWriteLock submitLock;
    private final ExecutorService service;

    /**
     * Create a ControllerExecutor that will support a maximum parallelism equal
     * to the number of available processors.
     */
    public ControllerExecutor() {
        this(Runtime.getRuntime().availableProcessors());
    }

    /**
     * Create a ControllerExecutor that will support a maximum parallelism equal
     * to the given number of threads. If <tt>numThreads</tt> is less or equal
     * to zero, then one thread is used.
     * 
     * @param numThreads The number of threads that can run controllers in
     *            parallel
     */
    public ControllerExecutor(int numThreads) {
        if (numThreads <= 0)
            numThreads = 1;
        submitLock = new ReentrantReadWriteLock();
        service = Executors.newFixedThreadPool(numThreads);
    }

    /**
     * Shutdown this ControllerExecutor so that it will no longer execute any
     * Controllers and its internal Threads will be cleaned up. This will let
     * any Controllers current executing finish, but subsequent calls to
     * {@link #execute(Controller...)} will be ignored.
     */
    public void shutdown() {
        submitLock.writeLock().lock();
        try {
            service.shutdown();
        } finally {
            submitLock.writeLock().unlock();
        }
    }

    /**
     * <p>
     * Execute the given Controllers in the order provided. Duplicates are
     * permitted, but null controllers are not. This method will block the
     * current thread until all Controllers in <tt>controllers</tt> have
     * completed. The ControllerExecutor will attempt to run Controllers
     * annotated with the {@link Parallel} annotation in parallel while still
     * preserving correct behavior. If two Controllers do not write to Component
     * types that are read by the other, the ControllerExecutor can execute the
     * controllers in parallel without worrying about them interfering with each
     * other.
     * </p>
     * <p>
     * Of course, the situation may be more complicated than the one above. An
     * example would be if there is a third component that writes to types used
     * by both of the first two controllers but must be run after one and before
     * the other. This prevents the parallelization of the first two. It is
     * important to carefully consider the order that controllers are specified
     * to maximize parallelizable opportunities.
     * </p>
     * <p>
     * The ControllerExecutor is thread safe in that multiple threads can run
     * execute() at the same time. However, they will share the same internal
     * threads used by the executor so it is not always recommended. Also,
     * actual Controllers may not be thread safe, so a given controller instance
     * should not be submitted for execution multiple times in parallel.
     * </p>
     * <p>
     * Exceptions thrown by one controller do not necessarily prevent the
     * execution of future controllers. The ControllerExecutor will still run
     * controllers that did not depend on the component type written to by the
     * failed controller.
     * </p>
     * 
     * @param controllers The ordered list of controllers to execute
     * @throws NullPointerException if any controller is null
     * @throws ControllerExecutionException if any controller threw an exception
     *             while running
     */
    public void execute(Controller... controllers) {
        if (controllers == null || controllers.length == 0)
            return;
        for (int i = 0; i < controllers.length; i++) {
            if (controllers[i] == null)
                throw new NullPointerException("Null controller cannot be executed");
        }
        
        // Multiple threads can schedule controllers at a time so we use a read lock
        submitLock.readLock().lock();
        List<Future<?>> allTasks = new ArrayList<Future<?>>();

        try {
            if (service.isShutdown())
                return; // Leave early if no tasks are going to be added

            Map<Controller, Future<?>> tasks = new IdentityHashMap<Controller, Future<?>>();
            Map<Controller, List<Future<?>>> dependencies = new IdentityHashMap<Controller, List<Future<?>>>();

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
        } finally {
            submitLock.readLock().unlock();
        }
        
        // Now wait on all tasks before returning, this doesn't need to be within the
        // submit lock since shutdown() will allow any queued tasks to complete first.
        List<Throwable> exceptions = wait(allTasks);
        if (exceptions != null) {
            // Build up a wrapper exception and throw it
            throw new ControllerExecutionException(exceptions);
        }
    }
    
    /**
     * Utility to see if a and b share any Class types
     * @param a
     * @param b
     * @return True if a and b intersect
     */
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

    /**
     * Block until all of the given tasks have completed or thrown an exception.
     * If any task throws an exception, they will be caught and recorded to be
     * returned in returned List. If no exceptions occur, null is returned.
     * 
     * @param tasks The tasks to block on
     * @return A list of any thrown exceptions, or null
     */
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
