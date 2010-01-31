package com.ferox.scene.fx.fixed;

import java.util.ArrayList;
import java.util.List;

import com.ferox.util.entity.Component;
import com.ferox.util.entity.Entity;

@SuppressWarnings("unchecked")
public class EntityAtomBuilder {
	public static interface AtomModifier<T extends Component> {
		public int getComponentType();
		
		public RenderAtom modifyAtom(RenderAtom atom, T component);
		
		public RenderAtom modifyAtom(RenderAtom atom);
	}
	
	private final List<AtomModifier> modifiers;
	
	public EntityAtomBuilder() {
		this(null);
	}
	
	public EntityAtomBuilder(List<AtomModifier<?>> modifiers) {
		this.modifiers = new ArrayList<AtomModifier>();
		
		// add in default modifiers
		this.modifiers.add(new RenderableAtomModifier());
		this.modifiers.add(new SceneElementAtomModifier());
		this.modifiers.add(new ShapeAtomModifier());
		this.modifiers.add(new ShadowCasterAtomModifier());
		this.modifiers.add(new ShadowReceiverAtomModifier());
		this.modifiers.add(new BlinnPhongAtomModifier());
		this.modifiers.add(new SolidColorAtomModifier());
		this.modifiers.add(new TexturedMaterialAtomModifier());
		
		// append any custom modifiers
		if (modifiers != null) {
			int sz = modifiers.size();
			for (int i = 0; i < sz; i++) {
				if (modifiers.get(i) != null)
					this.modifiers.add(modifiers.get(i));
			}
		}
	}
	
	public RenderAtom build(Entity e, RenderAtom atom) {
		if (e == null)
			throw new NullPointerException("Entity cannot be null");
		
		AtomModifier modifier;
		Component component;
		
		int sz = modifiers.size();
		for (int i = 0; i < sz; i++) {
			modifier = modifiers.get(i);
			component = e.get(modifier.getComponentType());
			
			if (component == null)
				atom = modifier.modifyAtom(atom);
			else
				atom = modifier.modifyAtom(atom, component);
			
			if (atom == null)
				break; // entity is invalid
		}
		
		return atom;
	}
}
