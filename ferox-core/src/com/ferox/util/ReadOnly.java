package com.ferox.util;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
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
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
public @interface ReadOnly {

}
