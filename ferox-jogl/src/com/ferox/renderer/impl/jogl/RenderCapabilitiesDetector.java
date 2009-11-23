package com.ferox.renderer.impl.jogl;

import java.awt.Frame;

import javax.media.opengl.DefaultGLCapabilitiesChooser;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLPbuffer;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;

import com.ferox.renderer.RenderCapabilities;

public class RenderCapabilitiesDetector {
	private static final boolean FORCE_NO_FBO = false;
	private static final boolean FORCE_NO_PBUFFER = false;

	private RenderCapabilities capabilities;

	public RenderCapabilities detect(GLProfile profile) {
		if (capabilities == null) {
			if (!FORCE_NO_PBUFFER && GLDrawableFactory.getFactory(profile).canCreateGLPbuffer()) {
				// use a pbuffer to query the capabilities
				GLPbuffer pbuffer = GLDrawableFactory.getFactory(profile).createGLPbuffer(new GLCapabilities(profile), 
																						  new DefaultGLCapabilitiesChooser(), 
																						  1, 1, null);
				pbuffer.getContext().makeCurrent();
				queryCapabilities(pbuffer.getGL());
				pbuffer.getContext().release();
				pbuffer.destroy();
			} else {
				// quick make a window visible to get a GLContext
				GLCanvas canvas = new GLCanvas(new GLCapabilities(profile), new DefaultGLCapabilitiesChooser(), null, null);
				
				Frame window = new Frame();
				window.add(canvas);
				
				window.setSize(1, 1);
				window.setVisible(true);
				
				canvas.getContext().makeCurrent();
				queryCapabilities(canvas.getGL());
				canvas.getContext().release();
				
				window.setVisible(false);
				window.dispose();
				canvas.destroy();
			}
		}

		return capabilities;
	}

	private static float formatVersion(String glv) {
		glv = glv.trim();
		char[] c = glv.toCharArray();
		boolean dotFound = false;
		char h;
		String v = "";
		for (int i = 0; i < c.length; i++) {
			h = c[i];
			if (!Character.isDigit(h)) {
				if (dotFound || h != '.')
					break;
				dotFound = true;
			}
			v += h;
		}

		return Float.parseFloat(v);
	}

	private void queryCapabilities(GL gl) {
		int[] store = new int[1];

		String vendor = gl.glGetString(GL2.GL_VENDOR);
		float vNum = formatVersion(gl.glGetString(GL2.GL_VERSION));

		// FIXME: these functions won't be used, check for non-arb version
		boolean vboSupported = gl.isFunctionAvailable("glGenBuffersARB") && 
							   gl.isFunctionAvailable("glBindBufferARB") && 
							   gl.isFunctionAvailable("glDeleteBuffersARB") && 
							   gl.isExtensionAvailable("GL_ARB_vertex_buffer_object");

		gl.glGetIntegerv(GL2.GL_MAX_VERTEX_ATTRIBS, store, 0);
		int maxVertexAttributes = store[0];
		gl.glGetIntegerv(GL2.GL_MAX_ELEMENTS_INDICES, store, 0);
		int maxIndices = store[0];
		gl.glGetIntegerv(GL2.GL_MAX_ELEMENTS_VERTICES, store, 0);
		int maxVertices = store[0];

		gl.glGetIntegerv(GL2.GL_MAX_LIGHTS, store, 0);
		int maxLights = store[0];

		// FIXME: do more version checking, too since these 
		boolean pointSpriteSupported = gl.isExtensionAvailable("GL_ARB_point_sprite") || vNum >= 2f;
		boolean glslSupported = gl.isExtensionAvailable("GL_ARB_shading_language_100") || vNum >= 2f;
		boolean fboSupported = !FORCE_NO_FBO && gl.isExtensionAvailable("GL_EXT_framebuffer_object");
		boolean pbufferSupported = !FORCE_NO_PBUFFER && GLDrawableFactory.getFactory(gl.getGLProfile()).canCreateGLPbuffer();

		boolean multiTexSupported = vNum >= 1.3f || gl.isExtensionAvailable("GL_ARB_multitexture");
		boolean npotTextures = vNum >= 2.0f || gl.isExtensionAvailable("GL_ARB_texture_non_power_of_two");
		boolean rectTextures = gl.isExtensionAvailable("GL_ARB_texture_rectangle");
		boolean fpTextures = gl.isExtensionAvailable("GL_ARB_texture_float");
		boolean s3tcTex = gl.isExtensionAvailable("GL_EXT_texture_compression_s3tc");

		int maxFFPTextureUnits;
		int maxVertexShaderTextureUnits;
		int maxFragmentShaderTextureUnits;
		int maxTextureCoordinates;
		int maxCombinedTextureUnits;

		if (multiTexSupported)
			gl.glGetIntegerv(GL2.GL_MAX_TEXTURE_UNITS, store, 0);
		else
			store[0] = 1;
		maxFFPTextureUnits = store[0];

		if (glslSupported) {
			gl.glGetIntegerv(GL2.GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS, store, 0);
			maxVertexShaderTextureUnits = store[0];
			gl.glGetIntegerv(GL2.GL_MAX_TEXTURE_IMAGE_UNITS, store, 0);
			maxFragmentShaderTextureUnits = store[0];
			gl.glGetIntegerv(GL2.GL_MAX_TEXTURE_COORDS, store, 0);
			maxTextureCoordinates = store[0];
			gl.glGetIntegerv(GL2.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS, store, 0);
			maxCombinedTextureUnits = store[0];
		} else {
			maxVertexShaderTextureUnits = 0;
			maxFragmentShaderTextureUnits = 0;
			maxCombinedTextureUnits = 0;
			maxTextureCoordinates = maxFFPTextureUnits;
		}

		int maxAniso = 0;
		if (gl.isExtensionAvailable("GL_EXT_texture_filter_anisotropic")) {
			gl.glGetIntegerv(GL2.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT, store, 0);
			maxAniso = store[0];
		}

		int maxColorAttachments = 1;
		if (fboSupported) {
			gl.glGetIntegerv(GL2.GL_MAX_COLOR_ATTACHMENTS, store, 0);
			maxColorAttachments = store[0];
		}

		gl.glGetIntegerv(GL2.GL_MAX_TEXTURE_SIZE, store, 0);
		int maxTextureSize = store[0];
		gl.glGetIntegerv(GL2.GL_MAX_CUBE_MAP_TEXTURE_SIZE, store, 0);
		int maxTextureCubeMapSize = store[0];
		gl.glGetIntegerv(GL2.GL_MAX_3D_TEXTURE_SIZE, store, 0);
		int maxTexture3DSize = store[0];

		int maxTextureRectSize = 0;
		if (rectTextures) {
			gl.glGetIntegerv(GL2.GL_MAX_RECTANGLE_TEXTURE_SIZE_ARB, store, 0);
			maxTextureRectSize = store[0];
		}
		int maxRenderbufferSize = maxTextureSize;
		if (fboSupported) {
			gl.glGetIntegerv(GL2.GL_MAX_RENDERBUFFER_SIZE, store, 0);
			maxRenderbufferSize = store[0];
		}

		capabilities = new RenderCapabilities(maxVertexShaderTextureUnits, maxFragmentShaderTextureUnits, 
											  maxFFPTextureUnits, maxCombinedTextureUnits, maxAniso,
											  maxTextureSize, maxTextureRectSize, maxTextureCubeMapSize, 
											  maxTexture3DSize, maxRenderbufferSize, fpTextures, npotTextures, 
											  rectTextures, s3tcTex, maxVertexAttributes, maxTextureCoordinates, 
											  maxIndices, maxVertices, vboSupported, maxLights,
											  pointSpriteSupported, glslSupported, fboSupported,
											  pbufferSupported, maxColorAttachments, vendor, vNum);
	}
}
