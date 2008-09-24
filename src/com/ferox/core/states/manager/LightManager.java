package com.ferox.core.states.manager;

import java.util.Arrays;

import com.ferox.core.renderer.RenderContext;
import com.ferox.core.renderer.RenderManager;
import com.ferox.core.renderer.RenderPass;
import com.ferox.core.scene.states.Light;
import com.ferox.core.states.StateAtom;
import com.ferox.core.states.StateAtomPeer;
import com.ferox.core.states.StateManager;
import com.ferox.core.system.SystemCapabilities;
import com.ferox.core.util.FeroxException;
import com.ferox.core.util.io.InputChunk;
import com.ferox.core.util.io.OutputChunk;

public class LightManager extends StateManager {
	private static int maxLights = -1;
	private static boolean maxLightsChecked = false;
		
	private boolean seperateSpecular;
	private boolean localViewer;
	private float[] ambientLight;
	
	public LightManager() {
		super();
		this.seperateSpecular = false;
		this.localViewer = false;
		this.ambientLight = new float[] {.2f, .2f, .2f, 1f};
	}
	
	public static int getMaxNumLights() {
		if (!maxLightsChecked) {
			SystemCapabilities caps = RenderManager.getSystemCapabilities();
			if (caps != null) {
				maxLights = caps.getMaxVertexAttributes();
				maxLightsChecked = true;
			}
		}
		return maxLights;
	}
	
	public float[] getGlobalAmbientLight() {
		return this.ambientLight;
	}
	
	public void setGlobalAmbientLight(float[] gl) throws IllegalArgumentException {
		if (gl == null || gl.length != 4)
			throw new IllegalArgumentException("Incorrect number of color components");
		this.ambientLight = gl;
		this.invalidateAssociatedStateTrees();
	}
	
	public void setSeperateSpecularHighlight(boolean sep) {
		if (sep != this.seperateSpecular) {
			this.seperateSpecular = sep;
			this.invalidateAssociatedStateTrees();
		}
	}
	
	public boolean isSeperateSpecularHighlight() {
		return this.seperateSpecular;
	}
	
	public void setLocalViewer(boolean local) {
		if (local != this.localViewer) {
			this.localViewer = local;
			this.invalidateAssociatedStateTrees();
		}
	}
	
	public boolean isLocalViewer() {
		return this.localViewer;
	}

	@Override
	public StateManager merge(StateManager manager) throws FeroxException {
		LightManager man = (LightManager)manager;
		
		switch(this.getMergeMode()) {
		case HIGHER: 
			return man;
		case LOWER: 
			return this;
		case REPLACE:
			return this;
		default:
			throw new FeroxException("Illegal merge mode set in light manager");		
		}
	}

	@Override
	protected void applyStateAtoms(StateManager previous, RenderManager manager, RenderPass pass) {
		RenderContext context = manager.getRenderContext();
		StateAtomPeer peer = context.getStateAtomPeer(Light.class);
		
		LightManager prev = (LightManager)previous;
		peer.prepareManager(this, prev);
	}
	
	@Override
	public Class<? extends StateAtom> getAtomType() {
		return Light.class;
	}

	@Override
	protected void restoreStateAtoms(RenderManager manager, RenderPass pass) {
		RenderContext context = manager.getRenderContext();
		StateAtomPeer peer = context.getStateAtomPeer(Light.class);
		peer.disableManager(this);
	}
	
	@Override
	public int getSortingIdentifier() {
		int hash = ((this.localViewer ? 1 : 0) << 1) | (this.seperateSpecular ? 1 : 0);
		hash ^= Arrays.hashCode(this.ambientLight);
		return hash;
	}
	
	@Override
	public void readChunk(InputChunk in) {
		super.readChunk(in);
		
		this.seperateSpecular = in.getBoolean("ss");
		this.localViewer = in.getBoolean("lv");
		this.ambientLight = in.getFloatArray("ambient");
	}
	
	@Override
	public void writeChunk(OutputChunk out) {
		super.writeChunk(out);
		
		out.set("ss", this.seperateSpecular);
		out.set("lv", this.localViewer);
		out.set("ambient", this.ambientLight);
	}
}
