package com.ferox.renderer.impl.jogl.drivers.resource;

import javax.media.opengl.GL;

import com.ferox.renderer.Renderer;
import com.ferox.renderer.impl.ResourceData;
import com.ferox.renderer.impl.ResourceDriver;
import com.ferox.renderer.impl.ResourceData.Handle;
import com.ferox.renderer.impl.jogl.JoglUtil;
import com.ferox.renderer.impl.jogl.JoglContextManager;
import com.ferox.resource.GlslUniform;
import com.ferox.resource.Resource;
import com.ferox.resource.GlslUniform.UniformType;
import com.ferox.resource.Resource.Status;

/**
 * JoglGlslUniformResourceDriver detects a uniform's index within its owning
 * GlslProgram and verifies that its type and length are as expected.
 * 
 * @author Michael Ludwig
 * 
 */
public class JoglGlslUniformResourceDriver implements ResourceDriver {
	/* Very simple handle, all it is, is the id for the uniform. */
	private static class GlslUniformHandle implements Handle {
		private int id;

		@Override
		public int getId() {
			return id;
		}
	}

	private final JoglContextManager factory;
	private final boolean glslSupport;

	public JoglGlslUniformResourceDriver(JoglContextManager factory) {
		this.factory = factory;
		glslSupport = factory.getFramework().getCapabilities().getGlslSupport();
	}

	@Override
	public void cleanUp(Renderer renderer, Resource resource, ResourceData data) {
		// nothing to clean-up
	}

	@Override
	public void update(Renderer renderer, Resource resource, ResourceData data, boolean fullUpdate) {
		GL gl = factory.getGL();

		GlslUniformHandle handle = (GlslUniformHandle) data.getHandle();
		if (handle == null) {
			// check support
			if (!glslSupport) {
				data.setStatus(Status.ERROR);
				data.setStatusMessage("GLSL is not supported on this hardware");
				return; // abort
			}

			handle = new GlslUniformHandle();
			data.setHandle(handle);
		}

		GlslUniform uniform = (GlslUniform) resource;
		Handle program = factory.getFramework().getHandle(uniform.getOwner(), factory);

		if (program == null) {
			// program is bad, so all uniforms are invalid
			data.setStatus(Status.ERROR);
			data
					.setStatusMessage("Owning GlslProgram has a status of ERROR or CLEANED, cannot have a valid GlslUniform");
		} else {
			handle.id = gl.glGetUniformLocation(program.getId(), uniform
					.getName());

			if (handle.id < 0) {
				// uniform doesn't exist in shader
				data.setStatus(Status.ERROR);
				data.setStatusMessage("Uniform with a name of "
						+ uniform.getName()
						+ " is not an active uniform in its glsl program");
			} else {
				// uniform name is exists, now we have to query and match other
				// properties
				int[] type = new int[1];
				int[] size = new int[1];
				gl.glGetActiveUniform(program.getId(), handle.id, 0, null, 0,
						size, 0, type, 0, null, 0);

				UniformType expectedType = JoglUtil.getUniformType(type[0]);
				if (expectedType != uniform.getType()) {
					data.setStatus(Status.ERROR);
					data.setStatusMessage("Expected uniform type is "
							+ expectedType + ", not " + uniform.getType());
				} else if (size[0] != uniform.getLength()) {
					data.setStatus(Status.ERROR);
					data.setStatusMessage("Expected uniform length is "
							+ size[0] + ", not " + uniform.getLength());
				} else {
					// everything else matches, so we're valid
					data.setStatus(Status.OK);
					data.setStatusMessage("");
				}
			}
		}

		// just to finish things off
		resource.clearDirtyDescriptor();
	}
}