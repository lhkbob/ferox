package com.ferox.scene.fx;

import com.ferox.util.entity.Component;

public class ShadowReceiver extends Component {
	private static final String DESCR = "Signals that rendered entities should receive cast shadows";
	
	public ShadowReceiver() {
		super(DESCR);
	}
}
