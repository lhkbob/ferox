package com.ferox.entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * ControllerExecutionException is a RuntimeException that wraps exceptions
 * thrown by Controllers when they are processed or executed. When executing a
 * set of Controllers with the {@link ControllerExecutor}, multiple controllers
 * may fail, in which case this exception contains all caught exceptions. These
 * can be fetched using {@link #getCauses()}. However, for printing purposes,
 * the first caught exception is used as the primary cause (i.e. it is returned
 * by {@link #getCause()} and is used in printing).
 * 
 * @author Michael Ludwig
 */
public class ControllerExecutionException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final Collection<Throwable> exceptions;

    /**
     * Create a new ControllerExecutionExceptiont that uses the given List as a
     * source of problems. This assumes that problems has at least one element
     * in it.
     * 
     * @param problems The collection of exceptions that occurred when the
     *            controllers were executed
     */
    public ControllerExecutionException(Collection<? extends Throwable> problems) {
        // Use 1st problem as the root cause, although we override the other
        // methods to print things out better
        super("Exceptions occurred while running Controllers: " + problems, problems.iterator().next());
        List<Throwable> e = new ArrayList<Throwable>();
        e.addAll(problems);
        exceptions = Collections.unmodifiableCollection(e);
    }
    
    /**
     * @return All Throwables that were thrown when the Controllers were
     *         executed
     */
    public Collection<Throwable> getCauses() {
        return exceptions;
    }
}
