package com.ferox.renderer;

import java.util.ArrayList;
import java.util.List;

import com.ferox.resource.Geometry;
import com.ferox.shader.Shader;

/**
 * The SimpleRenderPass enables you to add pairs (Geometry, Shader) to it so
 * that all added pairs are rendered in the order that they were added. This
 * implementations is thread safe.
 * 
 * @author Michael Ludwig
 */
public class SimpleRenderPass implements RenderPass {
	private final List<Geometry> geoms;
	private final List<Shader> shaders;
	
	/**
	 * Create a new SimpleRenderPass that initially has no 
	 * Geometry's or Shader's in it.
	 */
	public SimpleRenderPass() {
		geoms = new ArrayList<Geometry>();
		shaders = new ArrayList<Shader>();
	}

	/**
	 * <p>
	 * Add the given Geometry and Shader pair to be rendered when this
	 * RenderPass's {@link #render(Renderer, RenderSurface)} method is invoked.
	 * Added pairs will passed into {@link Renderer#render(Geometry, Shader)}
	 * unchanged and in the order that they were added.
	 * </p>
	 * <p>
	 * Neither geom nor shader can be null. If the pair has already been added,
	 * the pair is still added and will be rendered multiple times.
	 * </p>
	 * 
	 * @param geom The Geometry that's rendered with shader
	 * @param shader The Shader that renders the given Geometry
	 */
	public void add(Geometry geom, Shader shader) {
		if (geom == null)
			throw new NullPointerException("Cannot add a null Geometry");
		if (shader == null)
			throw new NullPointerException("Cannot add a null Shader");
		
		// in case we're being rendered
		synchronized(geoms) {
			geoms.add(geom);
			shaders.add(shader);
		}
	}

	/**
	 * Remove the given pair from this RenderPass so that it's no longer
	 * rendered. This will remove the first Geometry/Shader pair of the given
	 * instances, if the pair occurs multiple times. Other pairs with the same
	 * Geometry but different Shader, or different Geometry and same Shader will
	 * remain unaffected.
	 * 
	 * @param geom The Geometry to be removed
	 * @param shader The Shader to be removed
	 */
	public void remove(Geometry geom, Shader shader) {
		if (geom == null)
			throw new NullPointerException("Cannot remove a null Geometry");
		if (shader == null)
			throw new NullPointerException("Cannot remove a null Shader");
		
		// in case we're rendering
		synchronized(geoms) {
			int size = geoms.size();
			for (int i = 0; i < size; i++) {
				if (geoms.get(i) == geom && shaders.get(i) == shader) {
					geoms.remove(i);
					shaders.remove(i);
					break;
				}
			}
		}
	}

	@Override
	public void render(Renderer renderer, RenderSurface surface) {
		// lock the pass so that it doesn't change while we're rendering
		synchronized(geoms) {
			int size = geoms.size();
			for (int i = 0; i < size; i++) {
				// render each pair
				renderer.render(geoms.get(i), shaders.get(i));
			}
		}
	}
}
