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

	private Appearance appearance;
	private final EffectSet effects;
	private final RenderAtom renderAtom;

	/**
	 * Construct a shape with the given appearance and geometry, and set to
	 * automatically compute bounds.
	 * 
	 * @param geom The Geometry to use, cannot be null
	 * @param app The Appearance to use, if null then this Shape will be
	 *            rendered with the default appearance for a Framework
	 * @throws NullPointerException if geom is null
	 */
	public Shape(Geometry geom, Appearance app) {
		effects = new ShapeEffectSet();
		// enable lighting
		lights = new ArrayList<LightNode<?>>();

		// will fail here if geom is null
		renderAtom =
			new RenderAtom(worldTransform, geom, effects, renderAtomKey);

		setAppearance(app);
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
	 * Set the Geometry that is rendered as this Shape.
	 * 
	 * @param geom The Geometry to use
	 * @throws NullPointerException if geom is null
	 */
	public void setGeometry(Geometry geom) {
		renderAtom.setGeometry(geom, renderAtomKey);
	}

	/**
	 * @return The geometry used by this Shape.
	 */
	public Geometry getGeometry() {
		return renderAtom.getGeometry();
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
	 * Override visit to submit a render atom to the RenderQueue if necessary.
	 */
	@Override
	public VisitResult visit(RenderQueue renderQueue, View view,
		VisitResult parentResult) {
		VisitResult sp = super.visit(renderQueue, view, parentResult);
		if (sp != VisitResult.FAIL) {
			// finally add it to the queue
			renderQueue.add(renderAtom);
		}

		return sp;
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
		if (autoBound) {
			if (local == null)
				local = new AxisAlignedBox();
			getGeometry().getBounds(local);
		}
		return local;
	}
	
	/*
	 * EffectSet implementation that dynamically merges the List<Effect>
	 * returned from the current appearance, the list of lights and the fog of
	 * the Shape.
	 */
	private class ShapeEffectSet implements EffectSet {
		private int pos;
		
		public ShapeEffectSet() {
			pos = 0;
		}

		private Effect getAppearanceEffect(int pos) {
			if (appearance != null) {
				List<Effect> app = appearance.getEffects();
				if (pos < app.size())
					return app.get(pos);
			}
			
			return null;
		}
		
		private Effect getFogEffect(int pos) {
			if (fog != null) {
				if (appearance != null) {
					if (pos == appearance.getEffects().size())
						return fog.getFog();
				} else if (pos == 0)
					return fog.getFog();
			}
			
			return null;
		}
		
		private Effect getLightEffect(int pos) {
			if (fog != null)
				pos--;
			if (appearance != null)
				pos -= appearance.getEffects().size();
			
			if (pos >= 0 && pos < lights.size())
				return lights.get(pos).getLight();
			else
				return null;
		}
		
		@Override
		public Effect next() {
			Effect e = getAppearanceEffect(pos);
			if (e == null)
				e = getFogEffect(pos);
			if (e == null)
				e = getLightEffect(pos);
			
			if (e != null)
				pos++;
			
			return e;
		}

		@Override
		public int position() {
			return pos;
		}

		@Override
		public void position(int pos) {
			int size = lights.size();
			if (appearance != null)
				size += appearance.getEffects().size();
			if (fog != null)
				size += 1;

			if (pos < 0 || pos >= size)
				pos = 0;
			this.pos = pos;
		}

		@Override
		public void reset() {
			position(0);
		}
	}
}
