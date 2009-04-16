package com.ferox.renderer.impl.jogl;

import com.ferox.renderer.RenderException;
import com.ferox.renderer.impl.AbstractRenderer;
import com.ferox.renderer.impl.jogl.drivers.JoglDisplayListGeometryDriver;

/** Provides a full implementation of Renderer that is based on
 * the functionality provided by AbstractRenderer.
 * 
 * This Renderer is strictly single-threaded and undefined results
 * will occur if any of its methods are called outside of the thread
 * it was created in.  Because of this requirement, it must not be
 * created or used in the AWT event threads.
 * 
 * This Renderer uses the JOGL binding for the low-level calls. 
 * OnscreenSurfaces created by this renderer will return Frame objects 
 * from their getWindowImpl() methods.
 * 
 * @author Michael Ludwig
 *
 */
public class BasicJoglRenderer extends AbstractRenderer {
	/** Construct a renderer that does no debugging. */
	public BasicJoglRenderer() {
		this(false);
	}
	
	/** Construct a renderer that uses the JOGL bindings for OpenGL.
	 * debugGL is true, the renderer will  check openGL error conditions after every gl call, 
	 * and will throw an exception if there's a problem.  This can be useful when debugging, but
	 * will slow things down. */
	public BasicJoglRenderer(boolean debugGL) throws RenderException {
		super();

		JoglCapabilitiesDetector detector = new JoglCapabilitiesDetector();

		JoglSurfaceFactory factory = new JoglSurfaceFactory(this, detector.detect(), debugGL);
		
		JoglDisplayListGeometryDriver compiler = new JoglDisplayListGeometryDriver(factory);
		
		BasicGeometryDriverFactory gdf = new BasicGeometryDriverFactory(factory);
		BasicResourceDriverFactory rdf = new BasicResourceDriverFactory(factory);
		BasicStateDriverFactory sdf = new BasicStateDriverFactory(factory);	
		
		this.init(factory, factory.getTransformDriver(), compiler, gdf, rdf, sdf, detector);
	}
}
