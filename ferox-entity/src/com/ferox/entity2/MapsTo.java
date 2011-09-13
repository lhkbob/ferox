package com.ferox.entity2;

import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import java.lang.annotation.Retention;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface MapsTo {
    /**
     * The Class type that the property values eventually represent,
     * as exposed by the public API of the Component definition.
     * @return Final Class type of a property
     */
    Class<?> value();
}
