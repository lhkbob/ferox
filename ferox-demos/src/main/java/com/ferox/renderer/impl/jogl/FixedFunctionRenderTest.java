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
package com.ferox.renderer.impl.jogl;

import com.ferox.input.logic.InputManager;
import com.ferox.math.Matrix4;
import com.ferox.math.Vector3;
import com.ferox.math.Vector4;
import com.ferox.math.bounds.Frustum;
import com.ferox.renderer.FixedFunctionRenderer.TexCoord;
import com.ferox.renderer.FixedFunctionRenderer.TexCoordSource;
import com.ferox.renderer.Renderer.DrawStyle;
import com.ferox.renderer.geom.Geometry;
import com.ferox.renderer.geom.Sphere;
import com.ferox.renderer.geom.VertexBufferObject.StorageMode;
import com.ferox.renderer.texture.Texture.Filter;
import com.ferox.renderer.texture.Texture.Target;
import com.ferox.renderer.texture.TextureFormat;
import com.ferox.resource.BufferData;
import com.ferox.resource.Mipmap;
import com.ferox.util.ApplicationStub;

public class FixedFunctionRenderTest extends ApplicationStub {
    private FixedFunctionPass pass;

    public FixedFunctionRenderTest() {
        super(JoglFramework.create());
    }

    @Override
    protected void installInputHandlers(InputManager io) {
    }

    @Override
    protected void init(OnscreenSurface surface) {
        pass = new FixedFunctionPass(surface);
    }

    @Override
    protected void renderFrame(OnscreenSurface surface) {
        surface.getFramework().queue(pass);
    }

    private static class FixedFunctionPass implements Task<Void> {
        final Geometry shape;

        final Texture volume;

        final Frustum f;

        boolean statusChecked;
        final Surface surface;

        public FixedFunctionPass(Surface surface) {
            this.surface = surface;

            shape = Sphere.create(2f, 32, StorageMode.GPU_STATIC);

            f = new Frustum(60f, surface.getWidth() / (float) surface.getHeight(), 1f, 100f);
            f.setOrientation(new Vector3(0f, 3f, 10f), new Vector3(0f, 0f, -1f), new Vector3(0f, 1f, 0f));

            int width = 256;
            int height = 256;
            int depth = 256;

            byte[] volumeBuffer = new byte[width * height * depth * 4];
            for (int z = 0; z < depth; z++) {
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        int index = z * width * height * 4 + y * width * 4 + x * 4;

                        volumeBuffer[index] = (byte) (z / (float) depth * 256);
                        volumeBuffer[index + 1] = (byte) (y / (float) height * 256);
                        volumeBuffer[index + 2] = (byte) (x / (float) width * 256);
                        volumeBuffer[index + 3] = (byte) 127;
                    }
                }
            }

            Mipmap data = new Mipmap(new BufferData(volumeBuffer), width, height, depth, TextureFormat.RGBA);
            volume = new Texture(Target.T_3D, data);
            volume.setFilter(Filter.NEAREST);
        }

        @Override
        public Void run(HardwareAccessLayer access) {
            Context context = access.setActiveSurface(surface);
            if (context == null) {
                return null;
            }

            FixedFunctionRenderer g = context.getFixedFunctionRenderer();
            if (g != null) {
                g.clear(true, true, true, new Vector4(.2f, .2f, .2f, 1f), 1, 0);

                g.setDrawStyle(DrawStyle.SOLID);

                g.setVertices(shape.getVertices());
                g.setNormals(shape.getNormals());
                g.setTextureCoordinates(0, shape.getTextureCoordinates());

                g.setTexture(0, volume);
                g.setTextureCoordGeneration(0, TexCoord.R, TexCoordSource.OBJECT);

                g.setProjectionMatrix(f.getProjectionMatrix());

                Matrix4 t = new Matrix4();
                int rendered = 0;
                for (int i = 0; i < 10000; i++) {
                    t.setIdentity();
                    t.set(0, 3, (float) Math.random() * 100 - 50);
                    t.set(1, 3, (float) Math.random() * 100 - 50);
                    t.set(2, 3, (float) Math.random() * 100 - 50);

                    g.setModelViewMatrix(f.getViewMatrix().mul(t, t));

                    g.setIndices(shape.getIndices());
                    rendered += g
                            .render(shape.getPolygonType(), shape.getIndexOffset(), shape.getIndexCount());
                }

                if (!statusChecked) {
                    statusChecked = true;

                    System.out.println("Rendered count: " + rendered);

                    System.out.println("\nvertices status: " +
                                       surface.getFramework().getStatus(shape.getVertices().getVBO()));
                    System.out.println("\nnormals status: " +
                                       surface.getFramework().getStatus(shape.getNormals().getVBO()));
                    System.out.println("\ntexcoords status: " + surface.getFramework().getStatus(
                            shape.getTextureCoordinates().getVBO()));

                    System.out.println("\ntexture status: " +
                                       surface.getFramework().getStatus(volume) + " " +
                                       surface.getFramework().getStatusMessage(volume));
                }
            }

            return null;
        }
    }

    public static void main(String[] args) {
        new FixedFunctionRenderTest().run();
    }
}
