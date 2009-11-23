package com.ferox.renderer.impl.jogl;

import javax.media.opengl.GL2;

import com.ferox.renderer.Framework;
import com.ferox.renderer.Renderer;
import com.ferox.renderer.UnsupportedShaderException;
import com.ferox.renderer.impl.ResourceHandle;
import com.ferox.renderer.impl.jogl.resource.GeometryHandle;
import com.ferox.resource.Geometry;
import com.ferox.resource.Resource.Status;
import com.ferox.shader.FixedFunctionShader;
import com.ferox.shader.Shader;

public class FixedFunctionRenderer implements Renderer {
	private final JoglFramework framework;
	private final JoglContext context;
	
	public FixedFunctionRenderer(JoglFramework framework, JoglContext context) {
		if (framework == null || context == null)
			throw new NullPointerException("Framework and JoglContext can't be null");
		this.framework = framework;
		this.context = context;
	}
	
	@Override
	public Framework getFramework() {
		return framework;
	}

	@Override
	public int render(Geometry geom, Shader shader) {
		if (Thread.interrupted())
			throw new RenderInterruptedException();
		if (!(shader instanceof FixedFunctionShader))
			throw new UnsupportedShaderException("Renderer only supports FixedFunctionShaders, not: " + shader.getClass());
		
		ResourceHandle handle = framework.getResourceManager().getHandle(geom);
		if (handle.getStatus() == Status.READY)
			return renderImpl((GeometryHandle) handle, (FixedFunctionShader) shader);
		else
			return 0;
	}
	
	private int renderImpl(GeometryHandle handle, FixedFunctionShader shader) {
		GL2 gl = context.getGL2();
		// FIXME: modify Renderer to take an int that represents a set of dirty bits for the shader
		// and then have this renderer track the last shader, and base off of the set bits
		// modify only portions of the shader
		// FixedFunctionShader and OpenGlShader will both define some set of bits
		return 0;
	}
}
