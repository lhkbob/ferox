package com.ferox.renderer;

import com.ferox.resource.Geometry;
import com.ferox.shader.Shader;

/**
 * <p>
 * A Renderer is an interface that allows actual rendering to be performed. It
 * is intended that Framework implementations internally create a Renderer that
 * they use as needed when implementing render().
 * </p>
 * <p>
 * Internally it may be required by a Framework for Renderer code to be
 * executed on a separate thread from the Framework's.  Because of this Renderer instances should
 * not be held onto externally, but should only be used in method calls that
 * take them as arguments (e.g. in RenderPass).
 * </p>
 * 
 * @author Michael Ludwig
 */
public interface Renderer {
	/**
	 * Return the Framework that's associated with this Renderer. Each Renderer
	 * will only ever have one Framework, but it is possible for a Framework to
	 * use multiple Renderer's.
	 * 
	 * @return The Framework associated with this Renderer.
	 */
	public Framework getFramework();

	/**
	 * <p>
	 * Render the given Geometry using the specified Shader. Neither geom nor
	 * shader can be null. If geom has a status of ERROR then nothing will be
	 * rendered.
	 * </p>
	 * <p>
	 * This operation will render geom immediately. Modifications to the same
	 * Shader instance must be respected if a Shader is modified and used
	 * multiple times within the same frame. It is undefined what Shader state
	 * is used if the Shader is modified while being used in a call to render().
	 * </p>
	 * 
	 * @param geom The Geometry that will be rendered
	 * @param shader The Shader that describes how to draw geom
	 * @return The number of polygons rendered
	 * @throws NullPointerException if geom or shader are null
	 * @throws UnsupportedResourceException if the atom requires the use of
	 *             Resource and Geometry implementations that are unsupported by
	 *             this Framework
	 * @throws UnsupportedShaderException if the Shader implementation is
	 *             unsupported
	 */
	public int render(Geometry geom, Shader shader);
}
