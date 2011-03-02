package com.ferox.entity;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Parallel is an annotation that can be added to the type definition of a
 * {@link Controller} to allow it to be run in parallel with other Controllers
 * when using a {@link ControllerExecutor}. The Parallel annotation specifies
 * the Component types that are read and written by the Controller during its
 * execution, which can be used to determine when Controllers can be re-ordered
 * or executed in parallel without changing the outcome.
 * 
 * @author Michael Ludwig
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Parallel {
    /**
     * <p>
     * Return the set of Component types that are read from by the Controller.
     * Reading a Component type usually consists of iterating over the Entities
     * with that type (via {@link EntitySystem#iterator(TypedId)}), by getting
     * the Component from an Entity (via {@link Entity#get(TypedId)}), or
     * reading a property of the Component.
     * </p>
     * <p>
     * These reads can be in any manner or order with respect to each other or
     * the {@link #writes() writes}.
     * </p>
     * 
     * @return Components read during Controller execution
     */
    public Class<? extends Component>[] reads();

    /**
     * Return the set of Component types that are written to by the Controller.
     * Writing a Component type usually consists of adding (
     * {@link Entity#add(Component)}) or removing (
     * {@link Entity#remove(Component)} or {@link Entity#remove(TypedId)}) a
     * Component of that type from an Entity, or setting a property of the
     * Component. </p>
     * <p>
     * These writes can be in any manner or order with respect to each other or
     * the {@link #reads() reads}.
     * 
     * @return Components written to during Controller execution
     */
    public Class<? extends Component>[] writes();
}
