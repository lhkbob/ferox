package com.ferox.renderer.impl;

import java.util.List;
import java.util.concurrent.Future;

import com.ferox.renderer.FrameStatistics;

public interface RenderManager {
	public void destroy();
	
	public Future<FrameStatistics> render(List<Action> actions);
}
