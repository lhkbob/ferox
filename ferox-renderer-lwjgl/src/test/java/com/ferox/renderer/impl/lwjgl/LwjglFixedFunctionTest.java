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
package com.ferox.renderer.impl.lwjgl;

import com.ferox.math.Matrix4;
import com.ferox.math.Vector3;
import com.ferox.math.Vector4;
import com.ferox.math.bounds.Frustum;
import com.ferox.renderer.*;
import com.ferox.renderer.geom.Box;
import com.ferox.renderer.geom.Geometry;

/**
 *
 */
public class LwjglFixedFunctionTest {
    static double x = 0;

    public static void main(String[] args) throws Exception {
        final Framework framework = Framework.Factory.create();
        final OnscreenSurface s = framework
                .createSurface(new OnscreenSurfaceOptions().windowed(500, 500).withDepthBuffer(24));
        s.setTitle("Hello World");

        //                final Geometry box = Sphere.create(framework, 1.5, 16);
        final Geometry box = Box.create(framework, 3.0);

        try {
            while (!s.isDestroyed()) {
                framework.invoke(new Task<Void>() {
                    @Override
                    public Void run(HardwareAccessLayer access) {
                        Context c = access.setActiveSurface(s);
                        FixedFunctionRenderer r = c.getFixedFunctionRenderer();
                        r.clear(true, true, true, new Vector4(.3, .2, .5, 1), 1.0, 0);

                        Frustum view = new Frustum(60.0, 1.0, 1.0, 1000.0);
                        view.setOrientation(new Vector3(0, 0, 25), new Vector3(0, 0, -1),
                                            new Vector3(0, 1, 0));
                        r.setProjectionMatrix(view.getProjectionMatrix());
                        r.setModelViewMatrix(view.getViewMatrix());

                        r.setLightingEnabled(true);
                        r.setLightEnabled(0, true);
                        //                        r.setGlobalAmbientLight(new Vector4(.3, .3, .3, 1.0));

                        r.setLightColor(0, new Vector4(1, 1, 1, 1), new Vector4(1, 1, 1, 1),
                                        new Vector4(1, 1, 1, 1));
                        r.setLightPosition(0, new Vector4(0, 0, 25, 1));
                        //                        r.setSpotlight(0, new Vector3(0, 0, -1), 15, 40);

                        r.setMaterialDiffuse(new Vector4(.5, 0, .5, 1));
                        r.setMaterialAmbient(new Vector4(.2, .2, .2, 1));
                        r.setMaterialSpecular(new Vector4(.2, .9, .2, 1));
                        r.setMaterialShininess(10.0);

                        r.setNormals(box.getNormals());
                        r.setVertices(box.getVertices());
                        r.setIndices(box.getIndices());

                        r.setDrawStyle(Renderer.DrawStyle.SOLID, Renderer.DrawStyle.LINE);

                        Matrix4 m = new Matrix4().setIdentity();
                        Vector4 t = new Vector4();

                        for (int z = 0; z < 5; z++) {
                            for (int y = 0; y < 5; y++) {
                                for (int x = 0; x < 5; x++) {
                                    t.set(LwjglFixedFunctionTest.x + 3.5 * (x - 2), 3.5 * (y - 2),
                                          3.5 * (z - 2), 1);
                                    m.setCol(3, t);

                                    r.setModelViewMatrix(m.mul(view.getViewMatrix(), m));
                                    r.render(box.getPolygonType(), box.getIndexOffset(), box.getIndexCount());
                                }
                            }
                        }
                        x += 0.01;
                        if (x > 20) {
                            x = -20;
                        }

                        c.flush();

                        return null;
                    }
                }).get();
            }
        } finally {
            framework.destroy();
        }
    }
}
