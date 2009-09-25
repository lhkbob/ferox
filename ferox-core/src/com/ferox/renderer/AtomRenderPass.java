package com.ferox.renderer;

import java.util.ArrayList;
import java.util.List;

public class AtomRenderPass implements RenderPass {
	private View view;
	private final List<RenderAtom> atoms;
	
	public AtomRenderPass() {
		this(new View());
	}
	
	public AtomRenderPass(View view, RenderAtom... atoms) {
		this.atoms = new ArrayList<RenderAtom>();
		if (atoms != null) {
			for (RenderAtom a: atoms)
				addRenderAtom(a);
		}
		
		setView(view);
	}
	
	public View getView() {
		return view;
	}
	
	public void setView(View view) {
		this.view = view;
	}
	
	public void addRenderAtom(RenderAtom atom) {
		if (atom != null)
			atoms.add(atom);
	}
	
	public void removeRenderAtom(RenderAtom atom) {
		if (atom != null)
			atoms.remove(atom);
	}
	
	@Override
	public View preparePass() {
		view.updateView();
		return view;
	}

	@Override
	public void render(Renderer renderer, View view) {
		int ct = atoms.size();
		for (int i = 0; i < ct; i++) {
			// render each atom
			renderer.renderAtom(atoms.get(i));
		}
	}
}
