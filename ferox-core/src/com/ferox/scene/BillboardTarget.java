package com.ferox.scene;

import com.ferox.util.entity.Component;

/**
 * BillboardTarget is a modifier Component that is used by SceneController to
 * know if a SceneElement Entity is the source for other SceneElement's
 * billboarding. If this is the case, it's necessary for target to be updated
 * first or the dependent elements may use out of date information when they are
 * processed by the SceneController. To achieve this efficiently, any Entity
 * that is a SceneElement that will have its translation used as the billboard
 * point or constraint vector should also be flagged as a BillboardTarget.</p>
 * <p>
 * Problems can still occur if multiple BillboardTargets are dependent on each
 * other. The final billboarding depends on the order in which the
 * SceneController processes each SceneElement. Situations such as these may
 * require an additional Controller that executes before the SceneController to
 * manage things properly.
 * </p>
 * <p>
 * BillboardTarget holds little known value if assigned to an Entity that is not
 * also a SceneElement.
 * </p>
 * 
 * @author Michael Ludwig
 */
public final class BillboardTarget extends Component {
	private static final String DESCR = "Flag indicating that the Entity is a dynamic billboarding target";

	/**
	 * Create a new BillboardTarget. Because BillboardTarget contains no data
	 * and is a flag Component, generally only one instance is needed.
	 */
	public BillboardTarget() {
		super(DESCR);
	}
}