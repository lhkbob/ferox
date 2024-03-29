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
package com.ferox.renderer.loader;

import com.ferox.renderer.Framework;
import com.ferox.renderer.geom.Geometry;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public final class GeometryLoader {
    private static final List<GeometryFileLoader> loaders = new ArrayList<>();

    // register some default loaders
    static {
        registerLoader(new ASCIIPLYFileLoader());
    }

    private GeometryLoader() {
    }

    /**
     * <p/>
     * Register the given loader, so that it can be used in subsequent readGeometry() calls. The newer loaders
     * are favored when resolving conflicts between loaders that are capable of loading the same file.
     * <p/>
     * Does nothing if e is null. If e has already been registered, then e becomes the "newest" with regards
     * to resolving conflicts.
     *
     * @param e A GeometryFileLoader to register for use
     */
    public static void registerLoader(GeometryFileLoader e) {
        synchronized (loaders) {
            if (e != null) {
                int index = loaders.indexOf(e);
                if (index >= 0) {
                    loaders.remove(index);
                }
                loaders.add(e);
            }
        }
    }

    /**
     * Remove the given loader. Does nothing if it's null or was never registered. After a call to this
     * method, that loader instance will not be used in calls to readGeometry().
     *
     * @param e A GeometryFileLoader that should no longer be used
     */
    public static void unregisterLoader(GeometryFileLoader e) {
        synchronized (loaders) {
            if (e != null) {
                loaders.remove(e);
            }
        }
    }

    /**
     * Read the geometry from the given file, functions identically to readGeometry(stream).
     *
     * @param framework The Framework using the created geometry buffers
     * @param file      The File to read a mesh from
     *
     * @return The read mesh
     *
     * @throws java.io.IOException if the file can't be read, if it's unsupported, etc.
     */
    public static Geometry readGeometry(Framework framework, File file) throws IOException {
        if (file == null) {
            throw new IOException("Cannot load a geometry from a null file");
        }

        try (InputStream stream = new FileInputStream(file)) {
            return readGeometry(framework, stream);
        }
    }

    /**
     * Read the geometry from the given URL, functions identically to readGeometry(stream).
     *
     * @param framework The Framework using the created geometry buffers
     * @param url       The URL to read a mesh from
     *
     * @return The read mesh
     *
     * @throws IOException if the url couldn't be read, if it's unsupported or invalid, etc.
     */
    public static Geometry readGeometry(Framework framework, URL url) throws IOException {
        if (url == null) {
            throw new IOException("Cannot read from a null URL");
        }
        try (InputStream urlStream = url.openStream()) {
            return readGeometry(framework, urlStream);
        }
    }

    /**
     * <p/>
     * Read a mesh from the given stream. This assumes that the mesh begins with the next bytes read from the
     * stream, and that the stream is already opened.
     * <p/>
     * If the read geometry does not come with normal vectors, smooth normals are computed. The same goes for
     * tangent vectors.
     * <p/>
     * This method does not close the stream, in case it's to be used later on.
     *
     * @param framework The Framework using the created geometry buffers
     * @param stream    The InputStream to read the mesh from
     *
     * @return The read geometry
     *
     * @throws IOException if the stream can't be read from, it represents an invalid or unsupported mesh file
     *                     type, etc.
     */
    public static Geometry readGeometry(Framework framework, InputStream stream) throws IOException {
        // make sure we're buffered
        BufferedInputStream in;
        if (stream instanceof BufferedInputStream) {
            in = (BufferedInputStream) stream;
        } else {
            in = new BufferedInputStream(stream);
        }

        // load the file
        Geometry t;

        synchronized (loaders) {
            for (int i = loaders.size() - 1; i >= 0; i--) {
                t = loaders.get(i).read(framework, in);
                if (t != null) {
                    return t; // we've loaded it
                }
            }
        }

        throw new IOException("Unable to load the given geometry, no registered loader with support");
    }
}
