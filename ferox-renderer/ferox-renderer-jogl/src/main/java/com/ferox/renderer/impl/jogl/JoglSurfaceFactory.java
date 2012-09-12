package com.ferox.renderer.impl.jogl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.media.nativewindow.util.SurfaceSize;
import javax.media.opengl.GLCapabilities;
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
import com.jogamp.newt.Display;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Screen;
import com.jogamp.newt.ScreenMode;

/**
 * JoglSurfaceFactory is a SurfaceFactory implementation for the JOGL OpenGL
 * wrapper. It uses {@link JoglOnscreenSurface}, {@link JoglFboTextureSurface},
 * {@link JoglPbufferTextureSurface} for its surface implementations. It uses
 * the {@link JoglFixedFunctionRenderer} and {@link JoglGlslRenderer} for its
 * renderer implementations.
 * 
 * @author Michael Ludwig
 */
public class JoglSurfaceFactory extends SurfaceFactory {
    private static final int TARGET_REFRESH_RATE = 60;

    private final int capBits;
    private final GLProfile profile;
    
    private final DisplayMode defaultMode;
    private final DisplayMode[] availableModes;
    
    private final Map<DisplayMode, ScreenMode> convertMap;
    
    // the Display and Screen used by all windows created by this factory
    private final Display display;
    private final Screen screen;

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
        
        display = NewtFactory.createDisplay(null);
        display.addReference();
        
        screen = NewtFactory.createScreen(display, 0);
        screen.addReference();
        
        convertMap = new HashMap<DisplayMode, ScreenMode>();
        
        List<ScreenMode> modes = screen.getScreenModes();
        for (ScreenMode joglMode: modes) {
            DisplayMode feroxMode = convert(joglMode);
            if (convertMap.containsKey(feroxMode)) {
                // compare refresh rates and pick the one closest to target
                if (Math.abs(TARGET_REFRESH_RATE - joglMode.getMonitorMode().getRefreshRate()) < Math.abs(TARGET_REFRESH_RATE - convertMap.get(feroxMode).getMonitorMode().getRefreshRate())) {
                    convertMap.put(feroxMode, joglMode);
                }
            } else {
                // no refresh rate overlap
                convertMap.put(feroxMode, joglMode);
            }
        }
        
        availableModes = convertMap.keySet().toArray(new DisplayMode[convertMap.size()]);
        defaultMode = convert(screen.getOriginalScreenMode());
    }
    
    @Override
    public void destroy() {
        screen.removeReference();
        display.removeReference();
    }
    
    public Screen getScreen() {
        return screen;
    }
    
    public Display getDisplay() {
        return display;
    }
    
    public GLCapabilities chooseCapabilities(OnscreenSurfaceOptions request) {
        GLCapabilities caps = new GLCapabilities(profile);
        
        // update the caps fields
        PixelFormat pf;
        if (request.getFullscreenMode() != null) {
            pf = request.getFullscreenMode().getPixelFormat();
        } else {
            pf = getDefaultDisplayMode().getPixelFormat();
        }
        
        switch (pf) {
        case RGB_16BIT:
            caps.setRedBits(5);
            caps.setGreenBits(6);
            caps.setBlueBits(5);
            caps.setAlphaBits(0);
            break;
        case RGB_24BIT: case UNKNOWN:
            caps.setRedBits(8);
            caps.setGreenBits(8);
            caps.setBlueBits(8);
            caps.setAlphaBits(0);
            break;
        case RGBA_32BIT:
            caps.setRedBits(8);
            caps.setGreenBits(8);
            caps.setBlueBits(8);
            caps.setAlphaBits(8);
            break;
        }

        // FIXME On Mac, requesting any other depth than 16-bit causes a JVM crash
        caps.setDepthBits(16);
//        switch (request.getDepthFormat()) {
//        case DEPTH_16BIT:
//            caps.setDepthBits(16);
//            break;
//        case DEPTH_24BIT: case UNKNOWN:
//            caps.setDepthBits(24);
//            break;
//        case DEPTH_32BIT:
//            caps.setDepthBits(32);
//            break;
//        case NONE:
//            caps.setDepthBits(0);
//            break;
//        }

        switch (request.getStencilFormat()) {
        case STENCIL_16BIT:
            caps.setStencilBits(16);
            break;
        case STENCIL_8BIT:
            caps.setStencilBits(8);
            break;
        case STENCIL_4BIT:
            caps.setStencilBits(4);
            break;
        case STENCIL_1BIT:
            caps.setStencilBits(1);
            break;
        case NONE: case UNKNOWN:
            caps.setStencilBits(0);
            break;
        }

        switch (request.getMultiSampling()) {
        case EIGHT_X:
            caps.setNumSamples(8);
            caps.setSampleBuffers(true);
            break;
        case FOUR_X:
            caps.setNumSamples(4);
            caps.setSampleBuffers(true);
            break;
        case TWO_X:
            caps.setNumSamples(2);
            caps.setSampleBuffers(true);
            break;
        case NONE: case UNKNOWN:
            caps.setNumSamples(0);
            caps.setSampleBuffers(false);
            break;
        }
        
        return caps;
    }
    
    /**
     * Return an JOGL ScreenMode that exactly matches the given DisplayMode, or
     * null if there was no exact match.
     * 
     * @param mode The mode to "convert"
     * @return The JOGL DisplayMode matching mode, or null
     */
    public ScreenMode getScreenMode(DisplayMode mode) {
        return convertMap.get(mode);
    }
    
    private static DisplayMode convert(ScreenMode mode) {
        SurfaceSize realMode = mode.getMonitorMode().getSurfaceSize();
        PixelFormat pixFormat;
        switch(realMode.getBitsPerPixel()) {
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
        
        return new DisplayMode(realMode.getResolution().getWidth(), realMode.getResolution().getHeight(), pixFormat);
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
        return new JoglNEWTSurface(framework, this, options, (JoglContext) sharedContext,
                                   new JoglRendererProvider());
    }

    @Override
    public OpenGLContext createOffscreenContext(OpenGLContext sharedContext) {
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
                    glsl = new JoglGlslRenderer(sharedDelegate);
                }
            }
            
            return glsl;
        }
    }
}
