package com.ferox.scene.fx.impl.fixed;

import com.ferox.math.Frustum;
import com.ferox.math.Transform;
import com.ferox.renderer.RenderAtom;
import com.ferox.renderer.RenderPass;
import com.ferox.renderer.Renderer;
import com.ferox.resource.Geometry;
import com.ferox.scene.LightNode;
import com.ferox.scene.SceneElement;
import com.ferox.scene.Shape;
import com.ferox.scene.fx.Appearance;
import com.ferox.shader.Effect;
import com.ferox.shader.GlobalLighting;
import com.ferox.shader.Light;
import com.ferox.shader.View;
import com.ferox.util.Bag;

public class BaseRenderPass implements RenderPass {
	private static final Object KEY = new Object();
	private static final Transform DUMMY_TRANS = new Transform();
	private static final Geometry DUMMY_GEOM = new NullGeometry();
	
	private final FixedFunctionAttachedSurface surface;
	private final AppearanceSorter sorter;
	
	private final GlobalLighting globalLighting;
	private final RenderAtom atom;
	private final Bag<Effect> atomEffects;
	
	private Bag<Shape> shapes;
	private Bag<SceneElement> lights;
	
	public BaseRenderPass(FixedFunctionAttachedSurface surface) {
		globalLighting = new GlobalLighting();
		globalLighting.setSeparateSpecular(true);
		globalLighting.setLocalViewer(true);
		
		atomEffects = new Bag<Effect>();
		atom = new RenderAtom(DUMMY_TRANS, DUMMY_GEOM, atomEffects, KEY);
		
		sorter = new AppearanceSorter(surface.getCompositor());
		
		this.surface = surface;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public View preparePass() {
		Frustum view = surface.getView().getFrustum();
		
		// sort the shapes based on appearance
		shapes = (Bag<Shape>) (Bag) surface.getCompositor().query(view, Shape.class);
		sorter.sort(shapes);
		
		// now cache the lights
		lights = surface.getCompositor().query(view, LightNode.class);
		return surface.getView();
	}
	
	@Override
	public void render(Renderer renderer, View view) {
		// cache these values for later use
		int lightCount = lights.size();
		int shapeCount = shapes.size();
		Light shadowLight = surface.getShadowCastingLight();
		Light dimLight = surface.getDimmedShadowLight();
		
		// appearance control parameters for an atom
		Appearance lastAppearance = null;
		boolean shadowReceiver = false;
		
		// loop variables
		int addedEffects;
		Shape s;
		int j;
		Light light;
		for (int i = 0; i < shapeCount; i++) {
			s = shapes.get(i);
			// build the appearance
			if (s.getAppearance() != lastAppearance) {
				lastAppearance = s.getAppearance();
				shadowReceiver = convertAppearance(lastAppearance, atomEffects);
			}
			// add any needed lights
			addedEffects = 0;
			for (j = 0; j < lightCount; j++) {
				if (s.getWorldBounds() == null || s.getWorldBounds().intersects(lights.get(j).getWorldBounds())) {
					light = ((LightNode<?>) lights.get(j)).getLight();
					if (shadowReceiver && light == shadowLight)
						light = dimLight;
					
					atomEffects.add(light);
					addedEffects++;
				}
			}
			
			// render it
			atom.setGeometry(s.getGeometry(), KEY);
			atom.setTransform(s.getWorldTransform(), KEY);
			renderer.renderAtom(atom);
			
			// remove lights for next atom
			for (j = 0; j < addedEffects; j++)
				atomEffects.remove(atomEffects.size() - 1);
		}
		
		// reset everything
		atom.setGeometry(DUMMY_GEOM, KEY);
		atom.setTransform(DUMMY_TRANS, KEY);
		
		lights = null;
		shapes = null;
		atomEffects.clear(false);
	}
	
	/* Convert appearance into the given bag of effects.  Returns whether or not 
	 * it's a shadow receiver. */
	private boolean convertAppearance(Appearance appearance, Bag<Effect> results) {
		results.clear(true);
		FixedFunctionAppearance a = surface.getCompositor().get(appearance);
		
		// now build up the atom effects
		if (a.getMaterial() != null)
			results.add(a.getMaterial());
		if (a.getTextures() != null)
			results.add(a.getTextures());
		
		if (a.isLightingEnabled())
			results.add(globalLighting);
		
		return a.isShadowReceiver();
	}
}
