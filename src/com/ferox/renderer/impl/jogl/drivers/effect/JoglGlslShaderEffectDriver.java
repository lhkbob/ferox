package com.ferox.renderer.impl.jogl.drivers.effect;

import java.util.IdentityHashMap;
import java.util.List;

import javax.media.opengl.GL;

import com.ferox.effect.GlslShader;
import com.ferox.effect.GlslShader.UniformBinding;
import com.ferox.renderer.impl.ResourceData.Handle;
import com.ferox.renderer.impl.jogl.JoglContextManager;
import com.ferox.renderer.impl.jogl.record.GlslShaderRecord;
import com.ferox.renderer.impl.jogl.record.JoglStateRecord;
import com.ferox.resource.GlslUniform;

public class JoglGlslShaderEffectDriver extends SingleEffectDriver<GlslShader> {
	private final IdentityHashMap<GlslUniform, GlslUniform> perFrameUniforms;

	public JoglGlslShaderEffectDriver(JoglContextManager factory) {
		super(null, GlslShader.class, factory);
		perFrameUniforms = new IdentityHashMap<GlslUniform, GlslUniform>();
	}

	@Override
	public void reset() {
		super.reset();
		// we use reset() as a proxy for each frame
		// although it's really just each surface
		perFrameUniforms.clear();
	}

	@Override
	protected void apply(GL gl, JoglStateRecord record, GlslShader nextState) {
		GlslShaderRecord sr = record.shaderRecord;

		if (nextState == null) {
			// disable shaders and use the fixed-function pipeline
			if (sr.glslProgramBinding != 0) {
				gl.glUseProgram(0);
				sr.glslProgramBinding = 0;
			}
		} else {
			// bind the GlslProgram and set variables as needed
			Handle p = factory.getRenderer().getHandle(nextState.getProgram(), factory);
			if (p == null) {
				// the program can't be used
				if (sr.glslProgramBinding != 0) {
					gl.glUseProgram(0);
					sr.glslProgramBinding = 0;
				}
			} else {
				// bind the program
				if (sr.glslProgramBinding != p.getId()) {
					sr.glslProgramBinding = p.getId();
					gl.glUseProgram(sr.glslProgramBinding);
				}

				// set the uniforms
				List<UniformBinding> uniforms = nextState.getSetUniforms();
				int size = uniforms.size();
				for (int i = 0; i < size; i++)
					setUniform(gl, uniforms.get(i));
			}
		}
	}

	private void setUniform(GL gl, UniformBinding binding) {
		GlslUniform u = binding.getUniform();

		// determine if it's actually necessary to set the uniform
		switch (u.getValueUpdatePolicy()) {
		case MANUAL:
			// only set it if we're dirty
			if (binding.isDirty()) {
				setUniformValue(gl, u, binding.getValue());
				binding.setDirty(false);
			}
			break;
		case PER_FRAME:
			// only set it if we haven't seen since last reset()
			if (!perFrameUniforms.containsKey(u)) {
				setUniformValue(gl, u, binding.getValue());
				perFrameUniforms.put(u, u);
			}
			break;
		case PER_INSTANCE:
			// set it all the time
			setUniformValue(gl, u, binding.getValue());
			break;
		}
	}

	private void setUniformValue(GL gl, GlslUniform uniform, Object value) {
		Handle h = factory.getRenderer().getHandle(uniform, factory);
		if (h != null)
			switch (uniform.getType()) {
			case BOOL:
			case INT:
			case SAMPLER_1D:
			case SAMPLER_1D_SHADOW:
			case SAMPLER_2D:
			case SAMPLER_2D_SHADOW:
			case SAMPLER_RECT:
			case SAMPLER_RECT_SHADOW:
			case SAMPLER_3D:
			case SAMPLER_CUBEMAP: {
				// all of these use glUniform1i{v}
				int[] val = (int[]) value;
				if (val.length == 1)
					gl.glUniform1i(h.getId(), val[0]);
				else
					gl.glUniform1iv(h.getId(), uniform.getLength(), val, 0);
				break;
			}
			case BOOL_VEC2:
			case INT_VEC2: {
				int[] val = (int[]) value;
				if (val.length == 1)
					gl.glUniform2i(h.getId(), val[0], val[1]);
				else
					gl.glUniform2iv(h.getId(), uniform.getLength(), val, 0);
				break;
			}
			case BOOL_VEC3:
			case INT_VEC3: {
				int[] val = (int[]) value;
				if (val.length == 1)
					gl.glUniform3i(h.getId(), val[0], val[1], val[2]);
				else
					gl.glUniform3iv(h.getId(), uniform.getLength(), val, 0);
				break;
			}
			case BOOL_VEC4:
			case INT_VEC4: {
				int[] val = (int[]) value;
				if (val.length == 1)
					gl.glUniform4i(h.getId(), val[0], val[1], val[2], val[3]);
				else
					gl.glUniform4iv(h.getId(), uniform.getLength(), val, 0);
				break;
			}
			case FLOAT: {
				float[] val = (float[]) value;
				if (val.length == 1)
					gl.glUniform1f(h.getId(), val[0]);
				else
					gl.glUniform1fv(h.getId(), uniform.getLength(), val, 0);
				break;
			}
			case FLOAT_VEC2: {
				float[] val = (float[]) value;
				if (val.length == 1)
					gl.glUniform2f(h.getId(), val[0], val[1]);
				else
					gl.glUniform2fv(h.getId(), uniform.getLength(), val, 0);
				break;
			}
			case FLOAT_VEC3: {
				float[] val = (float[]) value;
				if (val.length == 1)
					gl.glUniform3f(h.getId(), val[0], val[1], val[2]);
				else
					gl.glUniform3fv(h.getId(), uniform.getLength(), val, 0);
				break;
			}
			case FLOAT_VEC4: {
				float[] val = (float[]) value;
				if (val.length == 1)
					gl.glUniform4f(h.getId(), val[0], val[1], val[2], val[3]);
				else
					gl.glUniform4fv(h.getId(), uniform.getLength(), val, 0);
				break;
			}
			case FLOAT_MAT2: {
				float[] val = (float[]) value;
				gl.glUniformMatrix2fv(h.getId(), uniform.getLength(), false,
						val, 0);
				break;
			}
			case FLOAT_MAT3: {
				float[] val = (float[]) value;
				gl.glUniformMatrix3fv(h.getId(), uniform.getLength(), false,
						val, 0);
				break;
			}
			case FLOAT_MAT4: {
				float[] val = (float[]) value;
				gl.glUniformMatrix4fv(h.getId(), uniform.getLength(), false,
						val, 0);
				break;
			}
			}
	}
}
