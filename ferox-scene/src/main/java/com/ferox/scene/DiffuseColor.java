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
package com.ferox.scene;

import com.lhkbob.entreri.TypeId;

/**
 * <p>
 * DiffuseColor specifies the color of the diffuse reflection of a material. The
 * percent of light reflected is stored in each of the three color components.
 * This is combined with the lights in a scene and a lighting model that uses
 * diffuse reflection to determine a final color. Because the colors represent
 * amounts of reflection, HDR values are not used.
 * </p>
 * <p>
 * The diffuse color of a material is the primary source of "color" for an
 * object. If a renderable Entity does not provide a Component describing a
 * lighting model, the diffuse color should be used to render the Entity as a
 * solid without lighting.
 * </p>
 * 
 * @see DiffuseColorMap
 * @author Michael Ludwig
 */
public final class DiffuseColor extends ColorComponent<DiffuseColor> {
    /**
     * The shared TypedId representing DiffuseColor.
     */
    public static final TypeId<DiffuseColor> ID = TypeId.get(DiffuseColor.class);

    private DiffuseColor() {}
}
