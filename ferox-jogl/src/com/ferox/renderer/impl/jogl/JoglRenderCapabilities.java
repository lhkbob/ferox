package com.ferox.renderer.impl.jogl;

import java.awt.Frame;
import java.util.EnumSet;

import javax.media.opengl.DefaultGLCapabilitiesChooser;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLPbuffer;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;

import com.ferox.renderer.RenderCapabilities;
import com.ferox.resource.Texture.Target;

public class JoglRenderCapabilities extends RenderCapabilities {
    /**
     * Force the returned RenderCapabilities to report no support for fbos.
     */
    public static final int FORCE_NO_FBO = 0x1;
    /**
     * Force the returned RenderCapabilities to report no support for pbuffers.
     */
    public static final int FORCE_NO_PBUFFER = 0x2;
    /**
     * Force the returned RenderCapabilities to report no support for
     * programmable shaders.
     */
    public static final int FORCE_NO_GLSL = 0x4;

    
    private final GLProfile profile;
    private final int forceBits;
    
    public JoglRenderCapabilities(GLProfile profile, int forceBits) {
        this.profile = profile;
        this.forceBits = forceBits;
        
        if (!isSet(FORCE_NO_PBUFFER) && GLDrawableFactory.getFactory(profile).canCreateGLPbuffer(null)) {
         // use a pbuffer to query the capabilities
            GLPbuffer pbuffer = GLDrawableFactory.getFactory(profile).createGLPbuffer(new GLCapabilities(profile), 
                                                                                      new DefaultGLCapabilitiesChooser(), 
                                                                                      1, 1, null);
            pbuffer.getContext().makeCurrent();
            query(pbuffer.getGL());
            pbuffer.getContext().release();
            pbuffer.destroy();
        } else {
         // quick make a window visible to get a GLContext
            GLCanvas canvas = new GLCanvas(new GLCapabilities(profile), new DefaultGLCapabilitiesChooser(), null, null);

            Frame window = new Frame();
            window.add(canvas);

            window.setSize(1, 1);
            window.setUndecorated(true);
            window.setVisible(true);

            canvas.getContext().makeCurrent();
            query(canvas.getGL());
            canvas.getContext().release();

            window.setVisible(false);
            window.dispose();
            canvas.destroy();
        }
    }
    
    private boolean isSet(int bit) {
        return (bit & forceBits) == bit;
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
    
    private void query(GL gl) {
        int[] store = new int[1];

        vendor = gl.glGetString(GL2GL3.GL_VENDOR) + "-" + gl.glGetString(GL2GL3.GL_RENDERER);
        version = formatVersion(gl.glGetString(GL2GL3.GL_VERSION));
        if (version >= 2f & !isSet(FORCE_NO_GLSL))
            glslVersion = formatVersion(gl.glGetString(GL2GL3.GL_SHADING_LANGUAGE_VERSION));

        hasFfpRenderer = true; // there is always support, it might just be emulated by a shader
        hasGlslRenderer = glslVersion >= 1f;
        pbuffersSupported = !isSet(FORCE_NO_PBUFFER) && GLDrawableFactory.getFactory(profile).canCreateGLPbuffer(null);
        
        fboSupported = !isSet(FORCE_NO_FBO) && (version >= 3f || gl.isExtensionAvailable("GL_EXT_framebuffer_object"));
        if (fboSupported) {
            gl.glGetIntegerv(GL2.GL_MAX_COLOR_ATTACHMENTS, store, 0);
            maxColorTargets = store[0];
        } else
            maxColorTargets = 0;
        
        hasSeparateBlend = version >= 2f || gl.isExtensionAvailable("GL_EXT_blend_equation_separate");
        hasSeparateStencil = version >= 2f || gl.isExtensionAvailable("GL_EXT_stencil_two_side");
        blendSupported = version >= 1.4f;
        
        gl.glGetIntegerv(GL2.GL_MAX_LIGHTS, store, 0);
        maxActiveLights = store[0];
        
        vboSupported = version >= 1.5f || gl.isExtensionAvailable("GL_ARB_vertex_buffer_object");

        if (hasGlslRenderer) {
            gl.glGetIntegerv(GL2.GL_MAX_VERTEX_ATTRIBS, store, 0);
            maxVertexAttributes = store[0];
        } else
            maxVertexAttributes = 0;

        boolean multiTexture = version >= 1.3f || gl.isExtensionAvailable("GL_ARB_multitexture");
        if (multiTexture) {
            gl.glGetIntegerv(GL2.GL_MAX_TEXTURE_UNITS, store, 0);
            maxFixedPipelineTextures = store[0];
        } else
            maxFixedPipelineTextures = 1;
        
        if (hasGlslRenderer) {
            gl.glGetIntegerv(GL2.GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS, store, 0);
            maxVertexShaderTextures = store[0];
            gl.glGetIntegerv(GL2.GL_MAX_TEXTURE_IMAGE_UNITS, store, 0);
            maxFragmentShaderTextures = store[0];
            gl.glGetIntegerv(GL2.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS, store, 0);
            maxCombinedTextures= store[0];
            gl.glGetIntegerv(GL2.GL_MAX_TEXTURE_COORDS, store, 0);
            maxTextureCoordinates = store[0];
        } else {
            maxVertexShaderTextures = 0;
            maxFragmentShaderTextures = 0;
            maxCombinedTextures = 0;
            maxTextureCoordinates = maxFixedPipelineTextures;
        }
        
        npotTextures = version >= 2f || gl.isExtensionAvailable("GL_ARB_texture_non_power_of_two");
        fpTextures = version >= 3f || gl.isExtensionAvailable("GL_ARB_texture_float");
        s3tcTextures = gl.isExtensionAvailable("GL_EXT_texture_compression_s3tc");
        
        hasDepthTextures = version >= 1.4f || (gl.isExtensionAvailable("GL_ARB_depth_texture") && gl.isExtensionAvailable("GL_ARB_shadow"));
        hasEnvCombine = version >= 1.3f || (gl.isExtensionAvailable("GL_ARB_texture_env_comibe") && gl.isExtensionAvailable("GL_ARB_texture_env_dot3"));
        hasMirrorRepeat = version >= 1.4f || gl.isExtensionAvailable("GL_texture_mirrored_repeat");
        hasClampEdge = version >= 1.2f;
        
        if (gl.isExtensionAvailable("GL_EXT_texture_filter_anisotropic")) {
            gl.glGetIntegerv(GL2.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT, store, 0);
            maxAnisoLevel = store[0];
        } else
            maxAnisoLevel = 0f;

        boolean hasCubeMaps = version >= 1.3f || gl.isExtensionAvailable("GL_ARB_texture_cube_map");
        boolean has3dTextures = version >= 1.2f || gl.isExtensionAvailable("GL_EXT_texture_3d");
        supportedTargets = EnumSet.of(Target.T_1D, Target.T_2D);
        if (hasCubeMaps)
            supportedTargets.add(Target.T_CUBEMAP);
        if (has3dTextures)
            supportedTargets.add(Target.T_3D);
        
        gl.glGetIntegerv(GL2.GL_MAX_TEXTURE_SIZE, store, 0);
        maxTextureSize = store[0];
        if (hasCubeMaps) {
            gl.glGetIntegerv(GL2.GL_MAX_CUBE_MAP_TEXTURE_SIZE, store, 0);
            maxTextureCubeMapSize = store[0];
        }
        if (has3dTextures) {
            gl.glGetIntegerv(GL2.GL_MAX_3D_TEXTURE_SIZE, store, 0);
            maxTexture3DSize = store[0];
        }

        if (fboSupported) {
            gl.glGetIntegerv(GL2.GL_MAX_RENDERBUFFER_SIZE, store, 0);
            maxRenderbufferSize = store[0];
        } else
            maxRenderbufferSize = maxTextureSize;
    }
}
