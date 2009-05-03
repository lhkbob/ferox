package com.ferox.renderer;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * RequiresState is a utility annotation to reinforce when methods of the
 * interfaces defined in the com.ferox.renderer package are allowed to be used.
 * </p>
 * <p>
 * If a method in one of the core interfaces is not annotated, it can be assumed
 * there are no key restrictions (except if the object in question isn't usable
 * - e.g. a Renderer has been destroyed).
 * </p>
 * 
 * @author Michael Ludwig
 */
@Inherited
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface RequiresState {
	/**
	 * RenderState is an enum representing, in very broad terms, the different
	 * states of a usable Renderer. Because we assume a usable Renderer, there
	 * is no DESTROYED enum value.
	 */
	public static enum RenderState {
		/**
		 * Requires the Renderer to be actively rendering, and for operation to
		 * be called from the Thread used for low level graphics operations
		 * (this is implementation dependent).
		 */
		RENDERING,
		/**
		 * Requires the Renderer to be in a waiting or idle state, e.g. not
		 * rendering or managing resources.
		 */
		WAITING
	}

	/** The RenderState that is required. */
	RenderState value();
}
