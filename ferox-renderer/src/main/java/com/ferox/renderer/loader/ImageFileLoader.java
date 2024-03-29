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
import com.ferox.renderer.Sampler;
import com.ferox.renderer.builder.Builder;

import java.io.BufferedInputStream;
import java.io.IOException;

/**
 * <p/>
 * ImageFileLoader is a simple interface that provides loading capabilities for various types of image data,
 * to then convert them into TextureProxies.
 * <p/>
 * To keep the interface simple, it only deals in streams, if a file format cannot be determined using just
 * streams, then this interface is not suitable for that type.
 *
 * @author Michael Ludwig
 */
public interface ImageFileLoader {
    /**
     * <p/>
     * Process the data from the given stream and return a TextureProxy representing its contents.
     * <p/>
     * Return null if the stream doesn't represent an image of any supported format. If it is an image of the
     * expected type, but is otherwise invalid or unsupported, then throw an exception.
     * <p/>
     * If null is returned, the stream should not have its position modified. <br> <i>The stream should not be
     * closed</i>
     *
     * @param framework The framework that will use the built texture
     * @param stream    The InputStream to attempt to read an image from
     *
     * @return The read TextureProxy, or null if this stream doesn't match the expected format
     *
     * @throws IOException if there are any problems reading the texture
     */
    public Builder<? extends Sampler> read(Framework framework, BufferedInputStream stream)
            throws IOException;
}
