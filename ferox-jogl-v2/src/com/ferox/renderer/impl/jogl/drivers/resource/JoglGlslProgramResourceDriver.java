package com.ferox.renderer.impl.jogl.drivers.resource;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2ES2;

import com.ferox.renderer.Renderer;
import com.ferox.renderer.impl.ResourceData;
import com.ferox.renderer.impl.ResourceDriver;
import com.ferox.renderer.impl.ResourceData.Handle;
import com.ferox.renderer.impl.jogl.JoglContextManager;
import com.ferox.renderer.impl.jogl.JoglUtil;
import com.ferox.resource.GlslProgram;
import com.ferox.resource.GlslUniform;
import com.ferox.resource.GlslVertexAttribute;
import com.ferox.resource.Resource;
import com.ferox.resource.GlslProgram.GlslProgramDirtyDescriptor;
import com.ferox.resource.GlslUniform.UniformType;
import com.ferox.resource.GlslVertexAttribute.AttributeType;
import com.ferox.resource.Resource.Status;

public class JoglGlslProgramResourceDriver implements ResourceDriver {
	/* Handle subclass used for GlslPrograms. */
	private static class GlslProgramHandle implements Handle {
		private int programId;
		private int vertexId;
		private int fragmentId;

		@Override
		public int getId() {
			return programId;
		}
	}

	/* GlslUniform substitute. */
	private static class Uniform {
		private UniformType type;
		private String name;
		private int length;

		public boolean equals(GlslUniform other) {
			return other.getType() == type && 
				   other.getName().equals(name) && 
				   other.getLength() == length;
		}
	}

	/* GlslVertexAttribute substitute. */
	private static class Attribute {
		private AttributeType type;
		private String name;
		private int bindSlot;

		public boolean equals(GlslVertexAttribute other) {
			return other.getType() == type && 
				   other.getName().equals(name) && 
				   other.getBindingSlot() == bindSlot;
		}
	}

	private final JoglContextManager factory;
	private final boolean glslSupport;
	private final int maxAttributes;

	public JoglGlslProgramResourceDriver(JoglContextManager factory) {
		this.factory = factory;
		glslSupport = factory.getFramework().getCapabilities().getGlslSupport();
		maxAttributes = factory.getFramework().getCapabilities().getMaxVertexAttributes();
	}

	@Override
	public void cleanUp(Renderer renderer, Resource resource, ResourceData data) {
		GL2ES2 gl = factory.getGL().getGL2ES2();
		GlslProgramHandle handle = (GlslProgramHandle) data.getHandle();

		if (handle != null) {
			// not null implies glsl support

			if (handle.vertexId > 0)
				gl.glDeleteShader(handle.vertexId);
			if (handle.fragmentId > 0)
				gl.glDeleteShader(handle.fragmentId);
			if (handle.programId > 0)
				gl.glDeleteProgram(handle.programId);
		}
	}

	@Override
	public void update(Renderer renderer, Resource resource, 
					   ResourceData data, boolean fullUpdate) {
		GL2ES2 gl = factory.getGL().getGL2ES2();
		GlslProgramHandle handle = (GlslProgramHandle) data.getHandle();
		GlslProgram program = (GlslProgram) resource;

		if (handle == null) {
			if (!glslSupport) {
				// glsl is not supported, so abort the update
				data.setStatus(Status.ERROR);
				data.setStatusMessage("GLSL shader programs aren't supported on this hardware");

				for (GlslUniform u : program.getUniforms().values())
					// this will set status to ERROR, too
					renderer.update(u, fullUpdate);
				return; // abort
			}

			handle = new GlslProgramHandle();
			data.setHandle(handle);

			// we always have a program handle, since GlslProgram doesn't allow
			// FFP shaders
			handle.programId = gl.glCreateProgram();

			// mark these as invalid so they will be created if need be
			handle.fragmentId = -1;
			handle.vertexId = -1;

			fullUpdate = true;
		}

		GlslProgramDirtyDescriptor dirty = program.getDirtyDescriptor();

		if (fullUpdate || dirty.getAttributesDirty() || dirty.getGlslCodeDirty()) {
			// re-bind the attribute locations
			int slot;
			for (Entry<String, GlslVertexAttribute> entry : program.getAttributes().entrySet()) {
				slot = entry.getValue().getBindingSlot();
				if (slot < maxAttributes)
					gl.glBindAttribLocation(handle.programId, entry.getValue().getBindingSlot(), 
											entry.getKey());
				// else ... ignore it since it's invalid
			}
		}

		if (fullUpdate || dirty.getGlslCodeDirty()) {
			// set the glsl code that will be executed for the program
			String[] fragmentCode = program.getFragmentShader();
			int fragmentObject = (fragmentCode.length == 0 ? -1 
														   : compileShader(gl, handle.fragmentId, fragmentCode, GL2.GL_FRAGMENT_SHADER));

			String[] vertexCode = program.getVertexShader();
			int vertexObject = (vertexCode.length == 0 ? -1 
													   : compileShader(gl, handle.vertexId, vertexCode, GL2.GL_VERTEX_SHADER));

			// attach the shaders
			handle.fragmentId = setShader(gl, handle.programId, handle.fragmentId, fragmentObject);
			handle.vertexId = setShader(gl, handle.programId, handle.vertexId, vertexObject);

			// check compile status
			boolean fragCompiled = (fragmentObject <= 0 ? true : isCompiled(gl, fragmentObject));
			boolean vertCompiled = (vertexObject <= 0 ? true : isCompiled(gl, vertexObject));

			// variables used to set the status and status message
			String fragMsg = "Fragment shader, Compiled: " + fragCompiled + " Log:\n" + getShaderLog(gl, handle.fragmentId);
			String vertMsg = "Vertex shader, Compiled: " + vertCompiled + " Log:\n" + getShaderLog(gl, handle.vertexId);

			String progMsg;
			Status status;

			if (vertCompiled && fragCompiled) {
				// we can link the shader program now
				gl.glLinkProgram(handle.programId);
				boolean linked = isLinked(gl, handle.programId);

				progMsg = "Glsl program, Linked: " + linked + " Log:\n" + getProgramLog(gl, handle.programId);
				status = (linked ? Status.OK : Status.ERROR);
			} else {
				// something failed to compile, set status to ERROR
				status = Status.ERROR;
				progMsg = "Unable to link shaders because of compile errors";
			}

			data.setStatus(status);
			data.setStatusMessage(vertMsg.trim() + "\n" + fragMsg.trim() + "\n" + progMsg.trim());

			if (status == Status.ERROR)
				// iterate through bound uniforms and update them
				// to set their status to ERROR
				for (Entry<String, GlslUniform> e : program.getUniforms().entrySet())
					renderer.update(e.getValue(), false);
		}

		// now we have to configure the uniforms and attributes

		if ((fullUpdate || dirty.getGlslCodeDirty() || 
			dirty.getUniformsDirty()) && data.getStatus() != Status.ERROR)
			// have to update the uniforms
			detectUniforms(gl, renderer, program, handle);

		if ((fullUpdate || dirty.getGlslCodeDirty() || 
			dirty.getAttributesDirty()) && data.getStatus() != Status.ERROR)
			// have to update the attributes and make sure slots don't overlap
			detectAttributes(gl, program, handle);

		program.clearDirtyDescriptor();
	}

	/*
	 * Fetch declared shader attributes, unbind attributes that don't exist.
	 * Attach any new ones. Update the slot bindings for the given program.
	 */
	private void detectAttributes(GL2ES2 gl, GlslProgram program, GlslProgramHandle handle) {
		int[] totalUniforms = new int[1];
		gl.glGetProgramiv(handle.programId, GL2.GL_ACTIVE_ATTRIBUTES, totalUniforms, 0);

		int[] maxNameLength = new int[1];
		gl.glGetProgramiv(handle.programId, GL2.GL_ACTIVE_ATTRIBUTE_MAX_LENGTH, maxNameLength, 0);

		int[] nameLength = new int[1];
		int[] size = new int[1];
		int[] type = new int[1];
		byte[] chars = new byte[maxNameLength[0]];

		Map<String, Attribute> declaredAttributes = new HashMap<String, Attribute>();
		for (int i = 0; i < totalUniforms[0]; i++) {
			// read in the attrs from OpenGL
			gl.glGetActiveAttrib(handle.programId, i, maxNameLength[0], 
								 nameLength, 0, size, 0, type, 0, chars, 0);

			Attribute a = new Attribute();
			a.type = JoglUtil.getAttributeType(type[0]);
			a.name = createString(chars, nameLength[0]);

			// ignore gl_X variables
			if (!a.name.startsWith("gl")) {
				a.bindSlot = gl.glGetAttribLocation(handle.programId, a.name);
				declaredAttributes.put(a.name, a);
			}
		}

		// now we have to clean-up bad attrs, attach new ones, and update good vars
		Map<String, GlslVertexAttribute> existingAttrs = new HashMap<String, GlslVertexAttribute>(program.getAttributes());
		Attribute declared;
		GlslVertexAttribute existing;
		for (Entry<String, GlslVertexAttribute> entry : existingAttrs.entrySet()) {
			existing = entry.getValue();
			declared = declaredAttributes.get(existing.getName());
			if (declared == null || !declared.equals(existing))
				// a bad bound attrs
				program.unbindAttribute(existing.getName());
			else
				// a good, already attached attr
				declaredAttributes.remove(existing.getName());
		}

		// at this point, only un-attached valid uniforms are in this map
		for (Entry<String, Attribute> entry : declaredAttributes.entrySet()) {
			declared = entry.getValue();
			// we can use this attribute
			program.bindAttribute(declared.name, declared.type, declared.bindSlot);
		}
	}

	/*
	 * Fetch declared uniforms from the gfx card. Detach uniforms that no longer
	 * exist from the program and clean them up. Attach new uniforms, and update
	 * all valid uniforms (including ones attached previously).
	 */
	private void detectUniforms(GL2ES2 gl, Renderer renderer, 
								GlslProgram program, GlslProgramHandle handle) {
		int[] totalUniforms = new int[1];
		gl.glGetProgramiv(handle.programId, GL2.GL_ACTIVE_UNIFORMS, totalUniforms, 0);

		int[] maxNameLength = new int[1];
		gl.glGetProgramiv(handle.programId, GL2.GL_ACTIVE_UNIFORM_MAX_LENGTH, maxNameLength, 0);

		int[] nameLength = new int[1];
		int[] size = new int[1];
		int[] type = new int[1];
		byte[] chars = new byte[maxNameLength[0]];

		Map<String, Uniform> declaredUniforms = new HashMap<String, Uniform>();
		for (int i = 0; i < totalUniforms[0]; i++) {
			// read in the uniform from OpenGL
			gl.glGetActiveUniform(handle.programId, i, maxNameLength[0], nameLength, 
								  0, size, 0, type, 0, chars, 0);

			Uniform u = new Uniform();
			u.length = size[0];
			u.type = JoglUtil.getUniformType(type[0]);
			u.name = createString(chars, nameLength[0]);

			// ignore gl_X variables
			if (!u.name.startsWith("gl"))
				declaredUniforms.put(u.name, u);
		}

		// now we have to clean-up bad uniforms, attach new ones, and update
		// good vars
		Map<String, GlslUniform> existingUniforms = new HashMap<String, GlslUniform>(program.getUniforms());
		Uniform declared;
		GlslUniform existing;
		for (Entry<String, GlslUniform> entry : existingUniforms.entrySet()) {
			existing = entry.getValue();
			declared = declaredUniforms.get(existing.getName());
			if (declared == null || !declared.equals(existing)) {
				// a bad attached uniform
				program.detachUniform(existing.getName());
				renderer.cleanUp(existing);
			} else {
				// a good, already attached uniform
				declaredUniforms.remove(existing.getName());
				renderer.update(existing, true);
			}
		}

		// at this point, only un-attached valid uniforms are in this map
		for (Entry<String, Uniform> entry : declaredUniforms.entrySet()) {
			declared = entry.getValue();
			existing = program.attachUniform(declared.name, declared.type, declared.length);
			renderer.update(existing, true);
		}
	}

	/*
	 * If oldShader differs from newShader, it will detach and delete oldShader
	 * (if oldShader existed), and attach newShader (if newShader exists).
	 * Returns newShader.
	 */
	private int setShader(GL2ES2 gl, int programId, int oldShader, int newShader) {
		if (oldShader != newShader) {
			if (oldShader > 0) {
				// clean-up old shader object
				gl.glDetachShader(programId, oldShader);
				gl.glDeleteShader(oldShader);
			}

			if (newShader > 0)
				// add new shader object
				gl.glAttachShader(programId, newShader);
		}

		return newShader;
	}

	/*
	 * Assumes that the given source code is not empty. This does not error
	 * checking on the compile status.
	 * 
	 * Returns the id of the shader object, which will be new if objectId isn't
	 * positive.
	 */
	private int compileShader(GL2ES2 gl, int objectId, String[] sourceCode, int type) {
		if (objectId <= 0)
			objectId = gl.glCreateShader(type);

		int[] lineLengths = new int[sourceCode.length];
		for (int i = 0; i < lineLengths.length; i++)
			lineLengths[i] = sourceCode[i].length();

		gl.glShaderSource(objectId, sourceCode.length, sourceCode, lineLengths, 0);
		gl.glCompileShader(objectId);

		return objectId;
	}

	/* Return the string representation of the shader object's log. */
	private String getShaderLog(GL2ES2 gl, int objectId) {
		int[] logSize = new int[1];
		gl.glGetShaderiv(objectId, GL2.GL_INFO_LOG_LENGTH, logSize, 0);

		if (logSize[0] > 0) {
			byte[] chars = new byte[logSize[0]];
			gl.glGetShaderInfoLog(objectId, logSize[0], null, 0, chars, 0);

			return createString(chars, chars.length);
		} else
			return "";
	}

	/* Return true if the given shader object was compiled successfully. */
	private boolean isCompiled(GL2ES2 gl, int objectId) {
		int[] status = new int[1];
		gl.glGetShaderiv(objectId, GL2.GL_COMPILE_STATUS, status, 0);
		return status[0] == GL.GL_TRUE;
	}

	/* Return the string representation of the shader program's log. */
	private String getProgramLog(GL2ES2 gl, int programId) {
		int[] logSize = new int[1];
		gl.glGetProgramiv(programId, GL2.GL_INFO_LOG_LENGTH, logSize, 0);

		if (logSize[0] > 0) {
			byte[] chars = new byte[logSize[0]];
			gl.glGetProgramInfoLog(programId, logSize[0], null, 0, chars, 0);

			return createString(chars, chars.length);
		} else
			return "";
	}

	/* Return true if the given shader program was linked successfully. */
	private boolean isLinked(GL2ES2 gl, int programId) {
		int[] status = new int[1];
		gl.glGetProgramiv(programId, GL2.GL_LINK_STATUS, status, 0);
		return status[0] == GL.GL_TRUE;
	}

	/* Build a string from an opengl byte array. */
	private String createString(byte[] chars, int length) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < length; i++)
			builder.append((char) chars[i]);
		return builder.toString();
	}
}
