package com.ferox.renderer.impl.lwjgl;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.Pbuffer;

import com.ferox.renderer.DisplayMode;
import com.ferox.renderer.DisplayMode.PixelFormat;
import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.renderer.GlslRenderer;
import com.ferox.renderer.OnscreenSurfaceOptions;
import com.ferox.renderer.RenderCapabilities;
import com.ferox.renderer.RenderException;
import com.ferox.renderer.SurfaceCreationException;
import com.ferox.renderer.TextureSurfaceOptions;
import com.ferox.renderer.impl.AbstractFramework;
import com.ferox.renderer.impl.AbstractOnscreenSurface;
import com.ferox.renderer.impl.AbstractTextureSurface;
import com.ferox.renderer.impl.OpenGLContext;
import com.ferox.renderer.impl.RendererProvider;
import com.ferox.renderer.impl.SurfaceFactory;

/**
 * LwjglSurfaceFactory is a SurfaceFactory implementation for the JOGL OpenGL
 * wrapper. It uses {@link LwjglAWTSurface}, {@link LwjglFboTextureSurface},
 * {@link LwjglPbufferTextureSurface} for its surface implementations. It uses
 * the {@link LWJGLFixedFunctionRenderer} and {@link LwjglGlslRenderer} for its
 * renderer implementations.
 * 
 * @author Michael Ludwig
 */
public class LwjglSurfaceFactory implements SurfaceFactory {
    private final int capBits;
    
    private final DisplayMode defaultMode;
    private final DisplayMode[] availableModes;

    /**
     * Create a new LwjglSurfaceFactory that will use the given profile and
     * capability bits. The bit mask uses the bit flags defined in
     * {@link LwjglRenderCapabilities}.
     * 
     * @param profile The GLProfile
     * @param capBits The forced capabilities
     * @throws NullPointerException if profile is null
     */
    public LwjglSurfaceFactory(int capBits) {
        this.capBits = capBits;
        
        // For now we'll use AWT to determine available graphics devices, but
        // must keep track if there are incompatibilities with LWJGL's display list
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
     * Return an LWJGL DisplayMode that exactly matches the given DisplayMode, or
     * null if there was no exact match.
     * 
     * @param mode The mode to "convert"
     * @return The AWT DisplayMode matching mode, or null
     */
    public org.lwjgl.opengl.DisplayMode getLWJGLDisplayMode(DisplayMode mode) {
        org.lwjgl.opengl.DisplayMode[] modes;
        try {
            modes = Display.getAvailableDisplayModes();
        } catch (LWJGLException e) {
            throw new RenderException("Unable to query available DisplayModes through LWJGL", e);
        }
        
        for (org.lwjgl.opengl.DisplayMode lwjglMode: modes) {
            if (mode.getWidth() == lwjglMode.getWidth() && mode.getHeight() == lwjglMode.getHeight()
                && mode.getPixelFormat().getBitDepth() == lwjglMode.getBitsPerPixel()
                && lwjglMode.isFullscreenCapable()) {
                return lwjglMode;
            }
        }
        
        // no match, so return null
        return null;
    }
    
    public DisplayMode chooseCompatibleDisplayMode(DisplayMode requested) {
        // we assume there is at least 1 (would be the default)
        DisplayMode best = availableModes[0];
        int reqArea = requested.getWidth() * requested.getHeight();
        int bestArea = best.getWidth() * best.getHeight();
        for (int i = 1; i < availableModes.length; i++) {
            int area = availableModes[i].getWidth() * availableModes[i].getHeight();
            if (Math.abs(area - reqArea) <= Math.abs(bestArea - reqArea)) {
                // available[i] has a better or same match with screen resolution,
                // now evaluate pixel format
                
                if (availableModes[i].getPixelFormat() == requested.getPixelFormat()) {
                    // exact match on format, go with available[i]
                    best = availableModes[i];
                    bestArea = area;
                } else {
                    // go with the highest bit depth pixel format
                    // PixelFormat's declared ordering is by bit depth so we can use compareTo
                    if (availableModes[i].getPixelFormat().compareTo(best.getPixelFormat()) >= 0) {
                        best = availableModes[i];
                        bestArea = area;
                    }
                }
            }
        }
        
        return best;
    }
    
    public org.lwjgl.opengl.PixelFormat choosePixelFormat(OnscreenSurfaceOptions request) {
        PixelFormat pf;
        if (request.getFullscreenMode() != null) {
            pf = request.getFullscreenMode().getPixelFormat();
        } else {
            pf = getDefaultDisplayMode().getPixelFormat();
        }
        
        org.lwjgl.opengl.PixelFormat caps = new org.lwjgl.opengl.PixelFormat();
        
        switch (pf) {
        case RGB_16BIT:
            caps = caps.withBitsPerPixel(16).withAlphaBits(0);
            break;
        case RGB_24BIT: case UNKNOWN:
            caps = caps.withBitsPerPixel(24).withAlphaBits(0);
            break;
        case RGBA_32BIT:
            caps = caps.withBitsPerPixel(24).withAlphaBits(8);
            break;
        }

        switch (request.getDepthFormat()) {
        case DEPTH_16BIT:
            caps = caps.withDepthBits(16);
            break;
        case DEPTH_24BIT: case UNKNOWN:
            caps = caps.withDepthBits(24);
            break;
        case DEPTH_32BIT:
            caps = caps.withDepthBits(32);
            break;
        case NONE:
            caps = caps.withDepthBits(0);
            break;
        }

        switch (request.getStencilFormat()) {
        case STENCIL_16BIT:
            caps = caps.withStencilBits(16);
            break;
        case STENCIL_8BIT:
            caps = caps.withStencilBits(8);
            break;
        case STENCIL_4BIT:
            caps = caps.withStencilBits(4);
            break;
        case STENCIL_1BIT:
            caps = caps.withStencilBits(1);
            break;
        case NONE: case UNKNOWN:
            caps = caps.withStencilBits(0);
            break;
        }

        switch (request.getAntiAliasMode()) {
        case EIGHT_X:
            caps = caps.withSamples(8);
            break;
        case FOUR_X:
            caps = caps.withSamples(4);
            break;
        case TWO_X:
            caps = caps.withSamples(2);
            break;
        case NONE: case UNKNOWN:
            caps = caps.withSamples(0);
            break;
        }
        
        return caps;
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
            return new LwjglFboTextureSurface(framework, this, options);
        else if (framework.getCapabilities().getPbufferSupport())
            return new LwjglPbufferTextureSurface(framework, this, options, (LwjglContext) sharedContext, new LwjglRendererProvider());
        else
            throw new SurfaceCreationException("No render-to-texture support on current hardware");
    }

    @Override
    public AbstractOnscreenSurface createOnscreenSurface(AbstractFramework framework,
                                                         OnscreenSurfaceOptions options,
                                                         OpenGLContext sharedContext) {
        LwjglStaticDisplaySurface surface = new LwjglStaticDisplaySurface(framework, this, options, (LwjglContext) sharedContext,
                                                                          new LwjglRendererProvider());
        surface.initialize();
        return surface;
    }

    @Override
    public OpenGLContext createShadowContext(OpenGLContext sharedContext) {
        if ((capBits & LwjglRenderCapabilities.FORCE_NO_PBUFFER) == 0 
            && (Pbuffer.getCapabilities() | Pbuffer.PBUFFER_SUPPORTED) != 0)
            return PbufferShadowContext.create(this, (LwjglContext) sharedContext, new LwjglRendererProvider());
        else
            throw new RenderException("No Pbuffer support, and LWJGL framework cannot do onscreen shadow contexts");
    }

    /**
     * @return The capabilities bits this factory was created with, to be passed
     *         into the constructor of all related
     *         {@link LwjglRenderCapabilities}
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
    
    private class LwjglRendererProvider implements RendererProvider {
        private FixedFunctionRenderer ffp;
        private GlslRenderer glsl;
        
        private LwjglRendererDelegate sharedDelegate;
        
        @Override
        public FixedFunctionRenderer getFixedFunctionRenderer(RenderCapabilities caps) {
            if (ffp == null) {
                if (caps.hasFixedFunctionRenderer()) {
                    if (sharedDelegate == null)
                        sharedDelegate = new LwjglRendererDelegate();
                    ffp = new LwjglFixedFunctionRenderer(sharedDelegate);
                }
            }
            
            return ffp;
        }

        @Override
        public GlslRenderer getGlslRenderer(RenderCapabilities caps) {
            if (glsl == null) {
                if (caps.hasGlslRenderer()) {
                    if (sharedDelegate == null)
                        sharedDelegate = new LwjglRendererDelegate();
                    glsl = new LwjglGlslRenderer(sharedDelegate);
                }
            }
            
            return glsl;
        }
    }
}