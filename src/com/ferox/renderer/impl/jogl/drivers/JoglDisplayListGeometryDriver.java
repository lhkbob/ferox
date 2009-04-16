package com.ferox.renderer.impl.jogl.drivers;

import java.util.List;

import javax.media.opengl.GL;

import com.ferox.renderer.RenderAtom;
import com.ferox.renderer.impl.AbstractRenderer;
import com.ferox.renderer.impl.CompiledGeometryDriver;
import com.ferox.renderer.impl.GeometryDriver;
import com.ferox.renderer.impl.ResourceData;
import com.ferox.renderer.impl.ResourceData.Handle;
import com.ferox.renderer.impl.jogl.JoglSurfaceFactory;
import com.ferox.resource.Geometry;
import com.ferox.resource.Resource;
import com.ferox.resource.Resource.Status;

/** Uses display lists to compile a list of render atoms into one Geometry.
 * 
 * @author Michael Ludwig
 *
 */
public class JoglDisplayListGeometryDriver implements CompiledGeometryDriver, GeometryDriver {
	/* Represents a display list in jogl. */
	private static class DisplayListHandle implements Handle {
		private static int displayListIdCounter = 1;

		private int id = displayListIdCounter++;
		private int polyCount;
		
		public int getId() { return this.id; }
	}
	
	private final JoglSurfaceFactory factory;
	
	public JoglDisplayListGeometryDriver(JoglSurfaceFactory factory) {
		this.factory = factory;
	}
	
	@Override
	public Geometry compile(List<RenderAtom> atoms, ResourceData data) {
		GL gl = this.factory.getGL();
		AbstractRenderer renderer = this.factory.getRenderer();

		
		DisplayListGeometry geom = new DisplayListGeometry(atoms);
		DisplayListHandle handle = new DisplayListHandle();
		data.setHandle(handle);
		
		// we have a clean slate, so just make the list
		gl.glNewList(handle.id, GL.GL_COMPILE);
			int numAtoms = atoms.size();
			for (int i = 0; i < numAtoms; i++) {
				renderer.renderAtom(atoms.get(i));
			}
			// reset the record in the dl, so that we have a predictable record when rendering
			renderer.resetGeometryDriver();
			renderer.setAppearance(null);
		gl.glEndList();
		return geom;
	}

	@Override
	public GeometryDriver getDriver() {
		return this;
	}

	@Override
	public int render(Geometry geom, ResourceData data) {
		GL gl = this.factory.getGL();
		DisplayListHandle h = (DisplayListHandle) data.getHandle();
		
		// we know the current record is the default appearance
		gl.glCallList(h.id);
		
		return h.polyCount;
	}

	@Override
	public void reset() {
		// do nothing
	}

	@Override
	public void cleanUp(Resource resource, ResourceData data) {
		GL gl = this.factory.getGL();
		DisplayListHandle h = (DisplayListHandle) data.getHandle();
		
		if (h != null)
			gl.glDeleteLists(h.getId(), 1);
	}

	@Override
	public void update(Resource resource, ResourceData data, boolean fullUpdate) {
		if (data.getHandle() == null) {
			// we've been cleaned up
			data.setStatus(Status.ERROR);
			data.setStatusMessage("Cannot update a compiled geometry after its been cleaned-up");
		} // else do nothing (effectively updated in compile())
	}
}
