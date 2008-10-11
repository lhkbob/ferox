package com.ferox.impl.jsr231;

import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;

import com.ferox.core.system.SystemCapabilities;

class JOGLCapabilitiesFetcher implements GLEventListener {
	private SystemCapabilities caps;
	
	public void display(GLAutoDrawable glAD) {
		// do nothing
	}

	public void displayChanged(GLAutoDrawable glAD, boolean par1, boolean par2) {
		// do nothing
	}

	private String formatVersion(String glv) {
		glv = glv.trim();
		char[] c = glv.toCharArray();
		boolean dotFound = false;
		char h;
		String v = "";
		for (int i = 0; i < c.length; i++) {
			h = c[i];
			if (!Character.isDigit(h)) {
				if (dotFound || h != '.') {
					break;
				}
				dotFound = true;
			}
			v += h;
		}
		
		return v;
	}
	
	public void init(GLAutoDrawable glAD) {
		GL gl = glAD.getGL();
		int[] store = new int[1];
		
		String version = this.formatVersion(gl.glGetString(GL.GL_VERSION));
		float vNum = Float.parseFloat(version);
		
		boolean vboSupported = gl.isFunctionAvailable("glGenBuffersARB") && 
							   gl.isFunctionAvailable("glBindBufferARB") &&
							   gl.isFunctionAvailable("glDeleteBuffersARB") &&
							   gl.isExtensionAvailable("GL_ARB_vertex_buffer_object");
		boolean glslSupported = gl.isExtensionAvailable("GL_ARB_shading_language_100") || vNum >= 2f;
		boolean multiTexSupported = vNum >= 1.3f || gl.isExtensionAvailable("GL_ARB_multitexture");
		boolean cubeMapSupport = vNum >= 1.3f || gl.isExtensionAvailable("GL_ARB_texture_cube_map");
		boolean threeDSupport = vNum >= 1.2f;
		boolean fboSupported = gl.isExtensionAvailable("GL_EXT_framebuffer_object");
		boolean separateSpecularLightingSupported = vNum >= 1.2f;
		
		boolean npotTextures = vNum >= 2.0f || gl.isExtensionAvailable("GL_ARB_texture_non_power_of_two");
		boolean rectTextures = gl.isExtensionAvailable("GL_ARB_texture_rectangle");
		boolean fpTextures = gl.isExtensionAvailable("GL_ARB_texture_float");
		boolean s3tcTex = gl.isExtensionAvailable("GL_EXT_texture_compression_s3tc");
		
		gl.glGetIntegerv(GL.GL_MAX_LIGHTS, store, 0);
		int maxLights = store[0];
		
		int maxFFPTextureUnits;
		int maxVertexShaderTextureUnits;
		int maxFragmentShaderTextureUnits;
		int maxTextureCoordinates;
		int maxCombinedTextureUnits;
		
		if (multiTexSupported)
			gl.glGetIntegerv(GL.GL_MAX_TEXTURE_UNITS, store, 0);
		else
			store[0] = 1;
		maxFFPTextureUnits = store[0];
		
		if (glslSupported) {
			gl.glGetIntegerv(GL.GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS, store, 0);
			maxVertexShaderTextureUnits = store[0];
			gl.glGetIntegerv(GL.GL_MAX_TEXTURE_IMAGE_UNITS, store, 0);
			maxFragmentShaderTextureUnits = store[0];
			gl.glGetIntegerv(GL.GL_MAX_TEXTURE_COORDS, store, 0);
			maxTextureCoordinates = store[0];
			gl.glGetIntegerv(GL.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS, store, 0);
			maxCombinedTextureUnits = store[0];
		} else {
			maxVertexShaderTextureUnits = 0;
			maxFragmentShaderTextureUnits = 0;
			maxCombinedTextureUnits = 0;
			maxTextureCoordinates = maxFFPTextureUnits;
		}
		
		gl.glGetIntegerv(GL.GL_MAX_VERTEX_ATTRIBS, store, 0);
		int maxVertexAttributes = store[0];
		
		gl.glGetIntegerv(GL.GL_MAX_ELEMENTS_INDICES, store, 0);
		int maxIndices = store[0];
		
		gl.glGetIntegerv(GL.GL_MAX_ELEMENTS_VERTICES, store, 0);
		int maxVertices = store[0];
		
		int maxAniso = 0;
		if (gl.isExtensionAvailable("GL_EXT_texture_filter_anisotropic")) {
			gl.glGetIntegerv(GL.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT, store, 0);
			maxAniso = store[0];
		}
		
		boolean pboSupport = gl.isExtensionAvailable("GL_EXT_pixel_buffer");
		
		gl.glGetIntegerv(GL.GL_MAX_COLOR_ATTACHMENTS_EXT, store, 0);
		int maxColor = store[0];
		
		this.caps = new SystemCapabilities(maxVertexShaderTextureUnits,
				maxFragmentShaderTextureUnits, maxCombinedTextureUnits, maxFFPTextureUnits,
				maxLights, maxVertexAttributes,maxTextureCoordinates, 
				maxVertices, maxIndices, maxColor, maxAniso, version, vNum,
				fboSupported, glslSupported, vboSupported, pboSupport, multiTexSupported,
				separateSpecularLightingSupported, cubeMapSupport, threeDSupport,
				fpTextures, npotTextures, rectTextures, s3tcTex);
	}

	public void reshape(GLAutoDrawable glAD, int x, int y, int width, int height) {
		// do nothing
	}
	
	public SystemCapabilities getCapabilities() {
		while (this.caps == null) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException ie) {}
		}
		return this.caps;
	}
}
