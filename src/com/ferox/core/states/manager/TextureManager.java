package com.ferox.core.states.manager;

import java.util.ArrayList;
import java.util.Iterator;

import com.ferox.core.renderer.RenderContext;
import com.ferox.core.renderer.RenderManager;
import com.ferox.core.renderer.RenderPass;
import com.ferox.core.states.*;
import com.ferox.core.states.atoms.Texture;
import com.ferox.core.system.SystemCapabilities;
import com.ferox.core.util.FeroxException;
import com.ferox.core.util.io.InputChunk;
import com.ferox.core.util.io.OutputChunk;

public class TextureManager extends StateManager {
	private static int maxTextureUnits = -1;
	private static boolean maxTUSet = false;
	private static int maxFFPTextureUnits = -1;
	private static boolean maxFFPTUSet = false;
	private static int maxFSTextureUnits = -1;
	private static boolean maxFSTUSet = false;
	private static int maxVSTextureUnits = -1;
	private static boolean maxVSTUSet = false;
	
	private static class TextureUnit {
		Texture tex;
		int unit;
		boolean invalid;
	}
	
	private Quality filter;
	
	private ArrayList<TextureUnit> textures;
	private Texture[] tu;
	
	public TextureManager() {
		super();
		this.textures = new ArrayList<TextureUnit>();
		this.filter = Quality.NICEST;
	}
	
	public Quality getTextureFilterHint() {
		return this.filter;
	}
	
	public void setTextureFilterHint(Quality hint) throws NullPointerException {
		if (hint == null)
			throw new NullPointerException("Can't have a null quality");
		if (hint != this.filter) {
			this.filter = hint;
			this.invalidateAssociatedStateTrees();
		}
	}
	
	private void updateArray() {
		this.tu = new Texture[TextureManager.getMaxTextureUnits()];
		
		Iterator<TextureUnit> it = this.textures.iterator();
		while(it.hasNext()) {
			TextureUnit t = it.next();
			if (t.unit < this.tu.length && t.unit >= 0)
				this.tu[t.unit] = t.tex;
		}
	}
	
	public void setTexture(int unit, Texture tex) {
		if (this.tu == null && TextureManager.getMaxTextureUnits() >= 0) 
			this.updateArray();
		
		Texture prev = this.getTexture(unit); // fails if they ask for a too large unit
		if (this.tu != null)
			this.tu[unit] = tex;
		if (prev != null && tex == null) {
			int removeIndex = 0;
			for (removeIndex = 0; removeIndex < this.textures.size(); removeIndex++) {
				if (this.textures.get(removeIndex).unit == unit)
					break;
			}
			this.textures.remove(removeIndex);
		} else if (prev != null && tex != null) {
			for (int i = 0; i < this.textures.size(); i++) {
				if (this.textures.get(i).unit == unit) {
					this.textures.get(i).tex = tex;
					break;
				}
			}
		} else if (prev == null && tex != null) {
			TextureUnit n = new TextureUnit();
			n.tex = tex;
			n.unit = unit;
			this.textures.add(n);
		}
		this.invalidateAssociatedStateTrees();
	}
	
	public Texture getTexture(int unit) {
		if (this.tu != null)
			return this.tu[unit];
		else {
			for (int i = 0; i < this.textures.size(); i++)
				if (this.textures.get(i).unit == unit)
					return this.textures.get(i).tex;
		}
		return null;
	}
	
	public static int getMaxTextureUnits() {
		return Math.max(getMaxFixedFunctionTextureUnits(), Math.max(getMaxFragmentShaderTextureUnits(), getMaxVertexShaderTextureUnits()));
	}
	
	public static int getMaxCombinedTextureUnits() {
		if (!maxTUSet) {
			SystemCapabilities caps = RenderManager.getSystemCapabilities();
			if (caps != null) {
				maxTextureUnits = caps.getMaxCombinedTextureUnits();
				maxTUSet = true;
			}	
		}
		return maxTextureUnits;
	}
	
	public static int getMaxFixedFunctionTextureUnits() {
		if (!maxFFPTUSet) {
			SystemCapabilities caps = RenderManager.getSystemCapabilities();
			if (caps != null) {
				maxFFPTextureUnits = caps.getMaxFixedFunctionTextureUnits();
				maxFFPTUSet = true;
			}	
		}
		return maxFFPTextureUnits;
	}
	
	public static int getMaxFragmentShaderTextureUnits() {
		if (!maxFSTUSet) {
			SystemCapabilities caps = RenderManager.getSystemCapabilities();
			if (caps != null) {
				maxFSTextureUnits = caps.getMaxFragmentShaderTextureUnits();
				maxFSTUSet = true;
			}	
		}
		return maxFSTextureUnits;
	}
	
	public static int getMaxVertexShaderTextureUnits() {
		if (!maxVSTUSet) {
			SystemCapabilities caps = RenderManager.getSystemCapabilities();
			if (caps != null) {
				maxVSTextureUnits = caps.getMaxVertexShaderTextureUnits();
				maxVSTUSet = true;
			}	
		}
		return maxVSTextureUnits;
	}
	
	@Override
	protected void applyStateAtoms(StateManager previous, RenderManager manager, RenderPass pass) {
		if (previous == this)
			return;
		
		if (this.tu == null)
			this.updateArray();
		
		TextureManager prev = (TextureManager)previous;
		RenderContext context = manager.getRenderContext();
		StateAtomPeer peer = context.getStateAtomPeer(Texture.class);
		
		peer.prepareManager(this, prev);
		TextureUnit l;
		int max = getMaxTextureUnits();
		for (int i = 0; i < this.textures.size(); i++) {
			l = this.textures.get(i);
			if (l.unit >= 0 && l.unit < max)
				pass.applyState(manager, l.tex, Texture.class, NumericUnit.get(l.unit));
		}
		if (previous != null) {
			for (int i = 0; i < prev.textures.size(); i++) {
				l = prev.textures.get(i);
				if (l.unit >= 0 && l.unit < max && this.tu[l.unit] == null) 
					pass.applyState(manager, null, Texture.class, NumericUnit.get(l.unit));
			}
		}
	}

	@Override
	public int getSortingIdentifier() {
		int hash = 0;
		if (this.textures.size() > 0) {
			hash = this.textures.get(0).hashCode();
			for (int i = 1; i < this.textures.size(); i++)
				hash ^= this.textures.get(i).hashCode();
		} 
		return hash;
	}

	@Override
	public Class<? extends StateAtom> getAtomType() {
		return Texture.class;
	}

	@Override
	protected void restoreStateAtoms(RenderManager manager, RenderPass pass) {
		RenderContext context = manager.getRenderContext();
		StateAtomPeer peer = context.getStateAtomPeer(Texture.class);
		
		TextureUnit t; 
		int max = getMaxTextureUnits();
		for (int i = 0; i < this.textures.size(); i++) {
			t = this.textures.get(i);
			if (t.unit >= 0 && t.unit < max)
				pass.applyState(manager, null, Texture.class, NumericUnit.get(t.unit));
		}
		peer.disableManager(this);
	}

	@Override
	public StateManager merge(StateManager man) throws FeroxException {
		if (getMaxTextureUnits() < 0)
			throw new FeroxException("Can't merge texture states if the context hasn't created any capabilities");
		TextureManager manager = (TextureManager)man;
		if (this.tu == null)
			this.updateArray();
		if (manager.tu == null)
			manager.updateArray();
		
		TextureManager nt, dom, ndom;
		
		switch(this.getMergeMode()) {
		case HIGHER:
			dom = manager;
			ndom = this;
			break;
		case LOWER:
			dom = this;
			ndom = manager;
			break;
		case REPLACE:
			return this;
		default:
			throw new FeroxException("Illegal merge mode in texture manager");
		}
		
		if (dom.textures.size() == getMaxTextureUnits())
			return dom;
		nt = new TextureManager();
		nt.tu = new Texture[dom.tu.length];
		for (int i = 0; i < dom.tu.length; i++)
			nt.tu[i] = (dom.tu[i] != null ? dom.tu[i] : ndom.tu[i]);
		nt.filter = dom.filter;
		nt.setMergeMode(dom.getMergeMode());
		
		for (int i = 0; i < nt.tu.length; i++) {
			if (nt.tu[i] != null) {
				TextureUnit t = new TextureUnit();
				t.tex = nt.tu[i];
				t.unit = i;
				nt.textures.add(t);
			}
		}
		return nt;
	}
	
	@Override
	public void readChunk(InputChunk in) {
		super.readChunk(in);
		
		int c = in.getInt("count");
		for (int i = 0; i < c; i++) {
			this.setTexture(in.getInt("unit_" + i), (Texture)in.getObject("tex_" + i));
		}
		this.filter = in.getEnum("hint", Quality.class);
	}
	
	@Override
	public void writeChunk(OutputChunk out) {
		super.writeChunk(out);
		out.setEnum("hint", this.filter);
		out.setInt("count", this.textures.size());
		for (int i = 0; i < this.textures.size(); i++) {
			out.setInt("unit_" + i, this.textures.get(i).unit);
			out.setObject("tex_" + i, this.textures.get(i).tex);
		}
	}
}
