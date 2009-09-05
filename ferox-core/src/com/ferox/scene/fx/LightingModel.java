package com.ferox.scene.fx;

public class LightingModel implements Component {
	private boolean shadowReceiver;
	
	public LightingModel(boolean shadowReceiver) {
		setShadowReceiver(shadowReceiver);
	}
	
	public boolean isShadowReceiver() {
		return shadowReceiver;
	}
	
	public void setShadowReceiver(boolean shadowReceiver) {
		this.shadowReceiver = shadowReceiver;
	}

	@Override
	public final Class<LightingModel> getType() {
		return LightingModel.class;
	}
}
