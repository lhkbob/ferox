/*
 * Ferox, a graphics and game library in Java
 *
 * Copyright (c) 2012, Michael Ludwig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright notice,
 *         this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright notice,
 *         this list of conditions and the following disclaimer in the
 *         documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.ferox.renderer;

import com.ferox.renderer.builder.*;
import com.ferox.renderer.impl.FrameworkImpl;
import com.ferox.renderer.impl.ResourceFactory;
import com.ferox.renderer.impl.SurfaceFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Enumeration;
import java.util.concurrent.Future;

/**
 * <p/>
 * The Framework is the main entry point to the rendering API. It controls the creation of {@link Surface
 * surfaces}, which store the final render outputs, and provides {@link HardwareAccessLayer} and {@link
 * Context} implementations that allow actual use of Renderers and Resources. A Framework acts as an advanced
 * task execution service that queues up {@link Task tasks} to run on an internal thread that can communicate
 * with low-level graphics drivers.
 * <p/>
 * A Framework is created by calling {@code Framework.Factory.create();}. Generally there should be only one
 * framework instance at a time.
 * <p/>
 * Framework implementations are thread safe so that a single Framework instance can be used from multiple
 * threads. Generally, a single Framework should active at a time. Renderers, Contexts and
 * HardwareAccessLayers are not thread safe and it is only valid to use them within the scope of the task
 * execution on the framework thread.
 * <p/>
 * Resources are constructed from the Framework using the builder pattern. Framework exposes builder creation
 * methods for the currently supported resource types. The builders perform little to no validation until the
 * actual {@code build()} method is invoked. At this point, synchronization is done with the framework thread
 * to construct the resource on the GPU. Invoking builders on the actual framework thread can avoid this
 * blocking because they will run immediately.
 * <p/>
 * If a particular resource or resource configuration is unsupported or fails to be created (such as a
 * compilation error in the shader, or out of memory when allocating a buffer) a ResourceException is thrown
 * at build time. Changing the state of the builder from multiple threads is not safe, but it is safe to
 * mutate a builder's configuration after invoking build() to configure a second resource.
 * <p/>
 * Resources created by one Framework cannot be used with any other Framework instance and will be
 * automatically destroyed when the framework is destroyed. All non-destroyed surfaces will be destroyed when
 * the framework is destroyed.
 * <p/>
 * Framework implementations are not required to function correctly when the OpenGL version is less than 2.0.
 * This means it can assume that vertex buffer objects, earlier GLSL language versions, and a number of buffer
 * operations related to blending and stenciling are available. All VertexBuffer, ElementBuffer, Shaders with
 * GLSL 1.0, Texture1D, Texture2D, TextureCubeMap, Texture3D, and DepthMap2D with non-integer data are always
 * supported. More modern shader language versions, geometry shaders, integer textures, texture arrays, and
 * depth cube maps may only be supported on newer hardware.
 *
 * @author Michael Ludwig
 */
public interface Framework extends Destructible {
    /**
     * Factory class for Frameworks
     */
    public static class Factory {
        private static final String FACTORY_DIR = "META-INF/ferox/renderer/";

        public static Framework create() {
            Class<? extends ResourceFactory> resourceFactory = getImplementation(ResourceFactory.class);
            Class<? extends SurfaceFactory> surfaceFactory = getImplementation(SurfaceFactory.class);

            try {
                FrameworkImpl f = new FrameworkImpl(surfaceFactory.newInstance(),
                                                    resourceFactory.newInstance());
                f.initialize();
                return f;
            } catch (InstantiationException | IllegalAccessException e) {
                throw new FrameworkException("Unable to create surface and resource factories via reflection",
                                             e);
            }
        }

        @SuppressWarnings("unchecked")
        private static <T> Class<? extends T> getImplementation(Class<T> superType) {
            ClassLoader loader = Factory.class.getClassLoader();
            try {
                // otherwise check if we have a properties file to load
                Enumeration<URL> urls = loader.getResources(FACTORY_DIR + superType.getCanonicalName());
                if (urls.hasMoreElements()) {
                    URL mapping = urls.nextElement();
                    if (urls.hasMoreElements()) {
                        throw new FrameworkException("Multiple mapping files for " + superType +
                                                     ". Only one of LWJGL or JOGL backends should be in classpath.");
                    }

                    BufferedReader in = new BufferedReader(new InputStreamReader(mapping.openStream()));
                    String line;
                    StringBuilder className = new StringBuilder();
                    // be somewhat permissive of whitespace (any other input most likely
                    // will fail to load a class)
                    while ((line = in.readLine()) != null) {
                        className.append(line);
                    }
                    in.close();

                    try {
                        return (Class<? extends T>) loader.loadClass(className.toString().trim());
                    } catch (ClassNotFoundException e) {
                        throw new FrameworkException("Unable to load implementation for " + superType, e);
                    }
                } else {
                    throw new FrameworkException("No mapping in META-INF found for " + superType +
                                                 ". Make sure one of the LWJGL or JOGL" +
                                                 "backends are on the classpath.");
                }
            } catch (IOException e) {
                throw new FrameworkException("Error reading META-INF mapping for class: " + superType, e);
            }
        }
    }

    /**
     * Return the DisplayMode representing the default display mode selected when the surface is no longer
     * fullscreen. This will be the original display mode selected by the user before they started the
     * application.
     *
     * @return The default DisplayMode.
     */
    public DisplayMode getDefaultDisplayMode();

    /**
     * <p/>
     * Return the current exclusive fullscreen surface. There can only be one fullscreen surface at a time.
     * While this returns a non-null value, attempts to create new OnscreenSurfaces will fail. Null is
     * returned if there is no fullscreen surface or after the exclusive surface gets destroyed.
     * <p/>
     * If a non-null surface is returned, its {@link OnscreenSurface#isFullscreen() isFullscreen()} method
     * will return true.
     *
     * @return The current fullscreen surface, or null
     */
    public OnscreenSurface getFullscreenSurface();

    /**
     * <p/>
     * Create a OnscreenSurface with the given options. If the Framework cannot create a surface satisfying
     * the options an exception is thrown. To prevent this from occurring, use display modes, depth buffer
     * sizes, stencil buffer sizes, and sample counts that were reported by {@link #getCapabilities()}. When
     * the window surface is returned, it will be visible and on screen.
     * <p/>
     * If there is already a fullscreen surface  an exception is thrown.
     * <p/>
     * Some Frameworks may not support multiple OnscreenSurfaces depending on their windowing libraries.
     *
     * @param options Requested pixel format and initial configuration of the surface
     *
     * @return The created surface, or null if the Framework has been destroyed
     *
     * @throws NullPointerException     if options is null
     * @throws SurfaceCreationException if the Framework cannot create the OnscreenSurface
     */
    public OnscreenSurface createSurface(OnscreenSurfaceOptions options);

    /**
     * <p/>
     * Create a TextureSurface that can be used to render into textures. All created texture surfaces can have
     * up to the maximum number of color targets at a time, and their configured render targets are fluid.
     * They can be changed by invoking {@link HardwareAccessLayer#setActiveSurface(TextureSurface,
     * com.ferox.renderer.Sampler.RenderTarget[], com.ferox.renderer.Sampler.RenderTarget)} and related
     * methods.
     * <p/>
     * If render targets are provided in the options, they will represent the initial target configuration of
     * the surface. Regardless of when the targets are specified, targets must have the same 2D dimensions as
     * the created texture surface and all color targets must have the same data and base format and mipmap
     * configuration.
     *
     * @param options The requested options for configuring the created surface
     *
     * @return The created surface, or null if the Framework has been destroyed
     *
     * @throws NullPointerException     if options is null
     * @throws SurfaceCreationException if the TextureSurface can't be created
     */
    public TextureSurface createSurface(TextureSurfaceOptions options);

    /**
     * <p/>
     * Run the given Task on the internal threads managed by the Framework. The Framework must support
     * receiving tasks from multiple threads safely. Ordering of tasks from across multiple threads will be
     * queued arbitrarily based on thread scheduling. Tasks invoked directly on the internal thread will be
     * run immediately and the returned future will have already completed.
     * <p/>
     * If the Framework is destroyed before a Task has started, the returned Future will be canceled. Calls to
     * this method are ignored if the Framework is already destroyed.
     *
     * @param <T>  The return type of the Task
     * @param task The task to run with a valid context
     *
     * @return A future linked to the given task
     *
     * @throws NullPointerException if the task is null
     */
    public <T> Future<T> invoke(Task<T> task);

    /**
     * <p/>
     * Convenience method to queue a Task that will flush the provided surface. If the flush is not being
     * performed by queued tasks, this is needed to ensure that any rendering is made visible to the surface.
     * If the surface is a TextureSurface, it is flushed to its last used render target configuration. If
     * finer control is needed, a custom task will need to be queued instead.
     * <p/>
     * An exception is thrown if the surface is not owned by the Framework. If the provided surface has been
     * destroyed, this method will do nothing. It is not best practice to queue or use surfaces that have been
     * destroyed, but this behavior is safe in order to play nicely with onscreen surfaces that can be closed
     * by the user at any time. If the Framework is destroyed, this will do nothing and return immediately.
     *
     * @param surface The surface to flush
     *
     * @throws NullPointerException     if surface is null
     * @throws IllegalArgumentException if the surface was not created by this Framework
     */
    public Future<Void> flush(Surface surface);

    /**
     * <p/>
     * Block the calling thread until all tasks queued prior to this method call have completed. Tasks queued
     * from other threads after this is invoked will not be processed until this method returns.
     * <p/>
     * If the Framework is destroyed, this will return immediately. This will also return as soon as a
     * Framework is destroyed if this was actively blocking.
     */
    public void sync();

    /**
     * @return A new builder for creating a {@link VertexBuffer}
     */
    public VertexBufferBuilder newVertexBuffer();

    /**
     * @return A new builder for creating a {@link ElementBuffer}
     */
    public ElementBufferBuilder newElementBuffer();

    /**
     * @return A new builder for creating a {@link Shader}
     */
    public ShaderBuilder newShader();

    /**
     * @return A new builder for creating a {@link Texture1D}
     */
    public Texture1DBuilder newTexture1D();

    /**
     * @return A new builder for creating a {@link Texture2D}
     */
    public Texture2DBuilder newTexture2D();

    /**
     * @return A new builder for creating a {@link TextureCubeMap}
     */
    public TextureCubeMapBuilder newTextureCubeMap();

    /**
     * @return A new builder for creating a {@link Texture3D}
     */
    public Texture3DBuilder newTexture3D();

    /**
     * @return A new builder for creating a {@link Texture1DArray}
     */
    public Texture1DArrayBuilder newTexture1DArray();

    /**
     * @return A new builder for creating a {@link Texture2DArray}
     */
    public Texture2DArrayBuilder newTexture2DArray();

    /**
     * @return A new builder for creating a {@link DepthMap2D}
     */
    public DepthMap2DBuilder newDepthMap2D();

    /**
     * @return A new builder for creating a {@link DepthCubeMap}
     */
    public DepthCubeMapBuilder newDepthCubeMap();

    /**
     * Get the capabilities of this Framework. This is allowed to return null after the Framework is destroyed
     * although Frameworks might not behave this way.
     *
     * @return The Capabilities for this Framework
     */
    public Capabilities getCapabilities();
}
