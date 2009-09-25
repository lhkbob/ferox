package com.ferox.scene.fx.impl.fixed;

import com.ferox.scene.fx.Appearance;
import com.ferox.scene.fx.impl.AppearanceCompiler;
import com.ferox.scene.fx.impl.fixed.FixedFunctionSceneCompositor.RenderMode;

public class FixedFunctionAppearanceCompiler implements AppearanceCompiler<FixedFunctionAppearance> {
	private final RenderMode renderMode;
	
	public FixedFunctionAppearanceCompiler(RenderMode renderMode) {
		if (renderMode == null)
			throw new NullPointerException("Must specify non-null RenderMode");
		this.renderMode = renderMode;
	}

	@Override
	public void clean(FixedFunctionAppearance compiled) {
		if (compiled != null)
			compiled.clean();
	}

	@Override
	public FixedFunctionAppearance compile(Appearance a, FixedFunctionAppearance previous) {
		if (previous != null)
			previous.recompile(a);
		else
			previous = new FixedFunctionAppearance(renderMode, a);
		return previous;
	}
}
