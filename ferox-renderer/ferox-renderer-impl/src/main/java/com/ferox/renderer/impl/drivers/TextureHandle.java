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
package com.ferox.renderer.impl.drivers;

import com.ferox.math.Vector4;
import com.ferox.renderer.Renderer.Comparison;
import com.ferox.resource.BufferData.DataType;
import com.ferox.resource.Texture;
import com.ferox.resource.Texture.Filter;
import com.ferox.resource.Texture.Target;
import com.ferox.resource.Texture.WrapMode;
import com.ferox.resource.TextureFormat;

/**
 * TextureHandle is the handle that represents the persisted state of a Texture, and is
 * used by any {@link AbstractTextureResourceDriver}.
 *
 * @author Michael Ludwig
 */
public class TextureHandle {
    public final Target target;
    public final int texID;

    public TextureFormat format;
    public DataType type;

    public boolean isMipmapped;

    public int lastSyncedVersion;
    public final Object[][] lastSyncedKeys;

    // mipmap region
    public int baseMipmap;
    public int maxMipmap;

    // texture parameters
    public Filter filter;

    public final Vector4 borderColor;

    public WrapMode wrapS;
    public WrapMode wrapT;
    public WrapMode wrapR;

    public Comparison depthTest;
    public Boolean enableDepthCompare;

    public float anisoLevel;

    public TextureHandle(Texture texture) {
        texID = texture.getId();
        target = texture.getTarget();
        lastSyncedKeys = new Object[texture.getNumLayers()][];

        // blank parameters
        baseMipmap = -1;
        maxMipmap = -1;

        borderColor = new Vector4();
        filter = null;
        wrapS = null;
        wrapT = null;
        wrapR = null;

        depthTest = null;
        enableDepthCompare = null;

        anisoLevel = -1f;

        format = null;
        type = null;
        lastSyncedVersion = -1;
    }
}
