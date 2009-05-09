package com.ferox.scene;

import java.util.ArrayList;
import java.util.List;

import com.ferox.effect.Effect;
import com.ferox.effect.EffectSet;
import com.ferox.math.AxisAlignedBox;
import com.ferox.math.BoundVolume;
import com.ferox.renderer.RenderAtom;
import com.ferox.renderer.RenderQueue;
import com.ferox.renderer.View;
import com.ferox.resource.Geometry;

/**
 * <p>
 * A Shape represents the basic visual element of a scene, the union of a
 * Geometry and an Appearance (describing how the Geometry is rendered).
 * </p>
 * <p>
 * Shape provides the functionality to auto-compute its local bounds based off
 * of its geometry. If local bounds is never set, or set to null, and its
 * auto-computing, then it will create an AxisAlignedBox for its use.
 * </p>
 * <p>
 * Appearances and geometry should be shared whenever possible to minimize state
 * changes and geometry updates.
 * </p>
 * 
 * @author Michael Ludwig
 */
public class Shape extends Leaf {
	private static final Object renderAtomKey = new Object();

	private boolean autoBound;

	private Geometry geom;
	private Appearance appearance;
	private EffectSet effects;

	private RenderAtom renderAtom;

	/**
	 * Construct a shape with the given appearance and geometry, and set to
	 * automatically compute bounds.
	 * 
	 * @param geom The Geometry to use, if it's null then this Shape will not be
	 *            rendered
	 * @param app The Appearance to use, if null then this Shape will be
	 *            rendered with the default appearance for a Renderer
	 */
	public Shape(Geometry geom, Appearance app) {
		// enable lighting
		lights = new ArrayList<LightNode<?>>();
		
		setAppearance(app);
		setGeometry(geom);
		setAutoComputeBounds(true);
	}

	/**
	 * Set the appearance to use for this shape. If app is null, the default
	 * appearance is used when rendering.
	 * 
	 * @param app The new Appearance
	 */
	public void setAppearance(Appearance app) {
		appearance = app;
	}

	/**
	 * @return The appearance used by this Shape
	 */
	public Appearance getAppearance() {
		return appearance;
	}

	/**
	 * Set whether or not to compute local bounds based on the Shape's assigned
	 * geometry when being updated.
	 * 
	 * @param auto The auto-compute policy for this Shape
	 */
	public void setAutoComputeBounds(boolean auto) {
		autoBound = auto;
	}

	/**
	 * Set the Geometry that is rendered as this Shape. If null, the shape will
	 * not submit a render atom to the RenderQueue.
	 * 
	 * @param geom The Geometry to use
	 */
	public void setGeometry(Geometry geom) {
		this.geom = geom;
	}

	/**
	 * @return The geometry used by this Shape.
	 */
	public Geometry getGeometry() {
		return geom;
	}

	/**
	 * Returns whether or not this Shape's local bounds are updated to enclose
	 * the Shape's geometry. If false, the local bounds are kept at whatever
	 * they were last set to.
	 * 
	 * @return The auto complete policy for this Shape
	 */
	public boolean getAutoComputeBounds() {
		return autoBound;
	}

	/** 
	 * Override visit to submit a render atom to the RenderQueue if necessary. */
	@Override
	public VisitResult visit(RenderQueue renderQueue, View view,
		VisitResult parentResult) {
		if (geom == null)
			return VisitResult.FAIL;

		VisitResult sp = super.visit(renderQueue, view, parentResult);
		if (sp != VisitResult.FAIL) {
			// make sure the render atom isn't null and
			// assign the geometry
			if (renderAtom == null)
				renderAtom =
					new RenderAtom(worldTransform, null, geom, renderAtomKey);
			else
				renderAtom.setGeometry(geom, renderAtomKey);

			// update the atom's effect set based on appearance, lights, and fog
			updateEffectSet();

			// finally add it to the queue
			renderQueue.add(renderAtom);
		}

		return sp;
	}

	/**
	 * Overridden to clean up old references of lights and fogs in the Shape's
	 * EffectSet.
	 * 
	 * @param lights
	 * @param fogs
	 */
	@Override
	protected void prepareLightsAndFog(List<LightNode<?>> sceneLights,
		List<FogNode> fogs) {
		// clean up fog and lights first
		if (fog != null)
			effects.remove(fog.getFog());
		
		int size = lights.size();
		for (int i = 0; i < size; i++)
			effects.remove(lights.get(i).getLight());
		
		// now continue preparing
		super.prepareLightsAndFog(sceneLights, fogs);
	}

	/*
	 * Internal method to make sure that effectSet has the correct Effects added
	 * to it.
	 */
	private void updateEffectSet() {
		if (appearance != null) {
			renderAtom.setEffects(effects, renderAtomKey);
			
			List<Effect> appEffects = appearance.getEffects();
			effects.clear();
			
			int size = appEffects.size();
			for (int i = 0; i < size; i++)
				effects.add(appEffects.get(i));
			
			if (appearance.getFogEnabled() && fog != null)
				effects.add(fog.getFog());

			if (appearance.getGlobalLighting() != null) {
				size = lights.size();
				for (int i = 0; i < size; i++)
					effects.add(lights.get(i).getLight());
			}
		} else
			renderAtom.setEffects(null, renderAtomKey);
	}

	/**
	 * Override to store the geometry's bounds into local. Only do it if flag is
	 * set and has a non-null geometry present. If local is null and we're
	 * auto-updating, create a new bound volume to store the bounds in.
	 * 
	 * @param local
	 */
	@Override
	protected BoundVolume adjustLocalBounds(BoundVolume local) {
		if (autoBound && geom != null) {
			if (local == null)
				local = new AxisAlignedBox();
			geom.getBounds(local);
		}
		return local;
	}
}
