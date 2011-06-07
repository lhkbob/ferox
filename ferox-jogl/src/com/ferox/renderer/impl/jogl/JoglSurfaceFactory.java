package com.ferox.renderer.impl.jogl;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLProfile;

import com.ferox.renderer.DisplayMode;
import com.ferox.renderer.DisplayMode.PixelFormat;
import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.renderer.GlslRenderer;
import com.ferox.renderer.OnscreenSurfaceOptions;
import com.ferox.renderer.RenderCapabilities;
import com.ferox.renderer.SurfaceCreationException;
import com.ferox.renderer.TextureSurfaceOptions;
import com.ferox.renderer.impl.AbstractFramework;
import com.ferox.renderer.impl.AbstractOnscreenSurface;
import com.ferox.renderer.impl.AbstractTextureSurface;
import com.ferox.renderer.impl.OpenGLContext;
import com.ferox.renderer.impl.RendererProvider;
import com.ferox.renderer.impl.SurfaceFactory;

/**
 * JoglSurfaceFactory is a SurfaceFactory implementation for the JOGL OpenGL
 * wrapper. It uses {@link JoglOnscreenSurface}, {@link JoglFboTextureSurface},
 * {@link JoglPbufferTextureSurface} for its surface implementations. It uses
 * the {@link JoglFixedFunctionRenderer} and {@link JoglGlslRenderer} for its
 * renderer implementations.
 * 
 * @author Michael Ludwig
 */
public class JoglSurfaceFactory implements SurfaceFactory {
    private final int capBits;
    private final GLProfile profile;
    
    private final DisplayMode defaultMode;
    private final DisplayMode[] availableModes;

    /**
     * Create a new JoglSurfaceFactory that will use the given profile and
     * capability bits. The bit mask uses the bit flags defined in
     * {@link JoglRenderCapabilities}.
     * 
     * @param profile The GLProfile
     * @param capBits The forced capabilities
     * @throws NullPointerException if profile is null
     */
    public JoglSurfaceFactory(GLProfile profile, int capBits) {
        if (profile == null)
            throw new NullPointerException("GLProfile cannot be null");
        
        this.capBits = capBits;
        this.profile = profile;
        
        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        Set<DisplayMode> modes = new HashSet<DisplayMode>();
        for (java.awt.DisplayMode awtMode: device.getDisplayModes()) {
            DisplayMode mode = convert(awtMode);
            if (mode != null)
                modes.add(mode);
        }
        availableModes = modes.toArray(new DisplayMode[modes.size()]);
        defaultMode = convert(device.getDisplayMode());
    }

    /**
     * Return an AWT DisplayMode that exactly matches the given DisplayMode, or
     * null if there was no exact match.
     * 
     * @param mode The mode to "convert"
     * @return The AWT DisplayMode matching mode, or null
     */
    public java.awt.DisplayMode getAWTDisplayMode(DisplayMode mode) {
        java.awt.DisplayMode[] awtModes = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayModes();
        for (java.awt.DisplayMode awtMode: awtModes) {
            if (mode.getWidth() == awtMode.getWidth() && mode.getHeight() == awtMode.getHeight()
                && mode.getPixelFormat().getBitDepth() == awtMode.getBitDepth()) {
                return awtMode;
            }
        }
        
        // no match, so return null
        return null;
    }
    
    private static DisplayMode convert(java.awt.DisplayMode awtMode) {
        PixelFormat pixFormat;
        switch(awtMode.getBitDepth()) {
        case 16:
            pixFormat = PixelFormat.RGB_16BIT;
            break;
        case 24:
            pixFormat = PixelFormat.RGB_24BIT;
            break;
        case 32:
            pixFormat = PixelFormat.RGBA_32BIT;
            break;
        default:
            pixFormat = PixelFormat.UNKNOWN;
            break;
        }
        
        return new DisplayMode(awtMode.getWidth(), awtMode.getHeight(), pixFormat);
    }
    
    @Override
    public AbstractTextureSurface createTextureSurface(AbstractFramework framework,
                                                       TextureSurfaceOptions options,
                                                       OpenGLContext sharedContext) {
        if (framework.getCapabilities().getFboSupport())
            return new JoglFboTextureSurface(framework, this, options);
        else if (framework.getCapabilities().getPbufferSupport())
            return new JoglPbufferTextureSurface(framework, this, options, (JoglContext) sharedContext, new JoglRendererProvider());
        else
            throw new SurfaceCreationException("No render-to-texture support on current hardware");
    }

    @Override
    public AbstractOnscreenSurface createOnscreenSurface(AbstractFramework framework,
                                                         OnscreenSurfaceOptions options,
                                                         OpenGLContext sharedContext) {
        return new JoglOnscreenSurface(framework, this, options, (JoglContext) sharedContext,
                                       new JoglRendererProvider());
    }

    @Override
    public OpenGLContext createShadowContext(OpenGLContext sharedContext) {
        if ((capBits & JoglRenderCapabilities.FORCE_NO_PBUFFER) == 0 
            && GLDrawableFactory.getFactory(profile).canCreateGLPbuffer(null))
            return PbufferShadowContext.create(this, (JoglContext) sharedContext, new JoglRendererProvider());
        else
            return OnscreenShadowContext.create(this, (JoglContext) sharedContext, new JoglRendererProvider());
    }
    
    /**
     * @return The GLProfile selected by this factory
     */
    public GLProfile getGLProfile() {
        return profile;
    }

    /**
     * @return The capabilities bits this factory was created with, to be passed
     *         into the constructor of all related
     *         {@link JoglRenderCapabilities}
     */
    public int getCapabilityForceBits() {
        return capBits;
    }
    
    @Override
    public DisplayMode getDefaultDisplayMode() {
        return defaultMode;
    }

    @Override
    public DisplayMode[] getAvailableDisplayModes() {
        return Arrays.copyOf(availableModes, availableModes.length);
    }
    
    private class JoglRendererProvider implements RendererProvider {
        private FixedFunctionRenderer ffp;
        private GlslRenderer glsl;
        
        private JoglRendererDelegate sharedDelegate;
        
        @Override
        public FixedFunctionRenderer getFixedFunctionRenderer(RenderCapabilities caps) {
            if (ffp == null) {
                if (caps.hasFixedFunctionRenderer()) {
                    if (sharedDelegate == null)
                        sharedDelegate = new JoglRendererDelegate();
                    ffp = new JoglFixedFunctionRenderer(sharedDelegate);
                }
            }
            
            return ffp;
        }

        @Override
        public GlslRenderer getGlslRenderer(RenderCapabilities caps) {
            if (glsl == null) {
                if (caps.hasGlslRenderer()) {
                    if (sharedDelegate == null)
                        sharedDelegate = new JoglRendererDelegate();
                    glsl = new JoglGlslRenderer2(sharedDelegate);
                }
            }
            
            return glsl;
        }
    }
}
