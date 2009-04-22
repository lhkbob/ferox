package com.ferox.renderer.impl.jogl.drivers;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.media.opengl.GL;

import com.ferox.renderer.RenderAtom;
import com.ferox.renderer.View;
import com.ferox.renderer.impl.AbstractRenderer;
import com.ferox.renderer.impl.CompiledGeometryDriver;
import com.ferox.renderer.impl.GeometryDriver;
import com.ferox.renderer.impl.ResourceData;
import com.ferox.renderer.impl.ResourceData.Handle;
import com.ferox.renderer.impl.jogl.JoglSurfaceFactory;
import com.ferox.renderer.util.StateSortingRenderQueue;
import com.ferox.resource.Geometry;
import com.ferox.resource.Resource;
import com.ferox.resource.Resource.Status;
import com.ferox.state.Appearance;
import com.ferox.state.State;
import com.ferox.state.State.Role;

/** Uses display lists to compile a list of render atoms into one Geometry.
 * 
 * @author Michael Ludwig
 *
 */
public class JoglDisplayListGeometryDriver implements CompiledGeometryDriver, GeometryDriver {
	/* Represents a display list in jogl. */
	private static class DisplayListHandle implements Handle {
		private static int displayListIdCounter = 1;

		private Role[] modifiedRoles;
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
		// prepare for submission to the display list
		View dummyView = new View();
		dummyView.updateView();
		
		Set<Role> allRoles = new HashSet<Role>();
		StateSortingRenderQueue queue = new StateSortingRenderQueue();
		
		// compute the used Roles, and put everything in a sorting queue
		Appearance a;
		List<State> s;
		int numAtoms = atoms.size();
		for (int i = 0; i < numAtoms; i++) {
			queue.add(atoms.get(i));
			
			// add all roles
			a = atoms.get(i).getAppearance();
			if (a != null) {
				// use roles present
				s = a.getStates();
				for (int u = 0; u < s.size(); u++)
					allRoles.add(s.get(u).getRole());
			}
		}
		
		// add roles for the default appearance
		allRoles.add(Role.MATERIAL);
		allRoles.add(Role.DEPTH_TEST);
		allRoles.add(Role.POLYGON_DRAW_STYLE);
		
		DisplayListGeometry geom = new DisplayListGeometry(atoms);
		DisplayListHandle handle = new DisplayListHandle();
		handle.modifiedRoles = allRoles.toArray(new Role[allRoles.size()]);
		data.setHandle(handle);
		
		AbstractRenderer renderer = this.factory.getRenderer();
		GL gl = this.factory.getGL();

		// we have a clean slate, so just make the list
		gl.glNewList(handle.id, GL.GL_COMPILE);
			handle.polyCount = queue.flush(renderer, dummyView);
			// reset the record in the dl, so that we have a predictable record when rendering
			renderer.resetGeometryDriver();
			renderer.restoreDefaultState(handle.modifiedRoles);
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
		
		// make necessary state back to the default
		this.factory.getRenderer().restoreDefaultState(h.modifiedRoles);
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
