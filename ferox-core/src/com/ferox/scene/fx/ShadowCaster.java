package com.ferox.scene.fx;

import com.ferox.util.entity.Component;

/**
 * ShadowCaster is a Component that declares an Entity to be a shadow caster.
 * When ShadowCaster is combined with a Light, that light will be considered
 * when computing shadows. When ShadowCaster is combined with rendered geometry,
 * that geometry obscures the light and becomes the direct cause of the shadows.
 * In this way ShadowCaster has two related but opposite roles in shadow
 * creation.
 * 
 * @author Michael Ludwig
 */
public class ShadowCaster extends Component {
	private static final String DESCR = "The Entity casts shadows";

	/**
	 * Create a new ShadowCaster component. Because the ShadowCaster is a tag
	 * component, only one instance is necessary.
	 */
	public ShadowCaster() {
		super(DESCR);
	}
}
