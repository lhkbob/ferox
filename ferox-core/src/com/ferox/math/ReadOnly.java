package com.ferox.math;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * <p>
 * Many of the com.ferox.math objects are fully mutable or can be used as the
 * result parameter for computations. However, it may be useful for other
 * classes to control or regulate when the math objects are updated to minimize
 * the work done.
 * </p>
 * <p>
 * To avoid defensive copies or duplicate instances, the ReadOnly annotation can
 * be used to declare a returned instance as read-only. It is then the
 * programmer's responsibility to follow that contract, or undefined/inefficient
 * behavior might result. Classes which use ReadOnly should document how failing
 * to follow the contract will affect things, or provide a way to inform the
 * class/object of changes to the read-only object.
 * </p>
 * 
 * @author Michael Ludwig
 */
@Retention(RetentionPolicy.SOURCE)
@Documented
@Inherited
public @interface ReadOnly {

}
