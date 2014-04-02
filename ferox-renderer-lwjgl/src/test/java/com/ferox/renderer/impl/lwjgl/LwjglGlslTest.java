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
import com.ferox.renderer.geom.Geometry;
import com.ferox.renderer.geom.Shapes;

/**
 *
 */
public class LwjglGlslTest {
    public static final String VERTEX_120 = "#version 120\n" +
                                            "attribute vec4 vertex;\n" +
                                            "attribute vec4 normal;\n" +
                                            "attribute vec4 tangent;\n" +
                                            "attribute vec4 tc;\n" +
                                            "varying vec4 color;\n" +
                                            "uniform mat4 projection;\n" +
                                            "uniform mat4 modelview;\n" +
                                            "void main() {\n" +
                                            "color = tangent * 0.5 + vec4(0.5);\n" +
                                                    //                                            "color = normal * 0.5 + vec4(0.5);\n" +
                                                    //                                            "color = tc;\n" +
                                                    //                                            "color = vec4(1.0);\n" +
                                            "gl_Position = projection * modelview * vertex;\n" +
                                            "}\n";
    public static final String FRAGMENT_120 = "#version 120\n" +
                                              "varying vec4 color;\n" +
                                              "void main() {\n" +
                                              "gl_FragColor = color;\n" +
                                              "}\n";

    static double x = 0;

    public static void main(String[] args) throws Exception {
        Framework.Factory.enableDebugMode();
        final Framework framework = Framework.Factory.create();
        final OnscreenSurface s = framework.createSurface(new OnscreenSurfaceOptions().windowed(500, 500)
                                                                                      .withDepthBuffer(24));
        s.setTitle("Hello World");
        s.setVSyncEnabled(true);

        //        final Geometry box = Sphere.create(framework, 0.5, 128);
        //                final Geometry box = Box.create(framework, 1.0);
        //        final Geometry box = Cylinder.create(framework, 0.5, 1, 64);
        final Geometry box = Shapes.createArmadillo(framework);


        final Shader shader;

        if (framework.getCapabilities().getMajorVersion() >= 3) {
            System.out.println("ONLY #120 defined");
            return;
        } else {
            shader = framework.newShader().withVertexShader(VERTEX_120).withFragmentShader(FRAGMENT_120)
                              .build();
        }
        final Shader.Uniform modelview = shader.getUniform("modelview");
        final Shader.Uniform projection = shader.getUniform("projection");
        final Shader.Attribute vertex = shader.getAttribute("vertex");
        final Shader.Attribute normal = shader.getAttribute("normal");
        final Shader.Attribute tangent = shader.getAttribute("tangent");
        final Shader.Attribute tc = shader.getAttribute("tc");

        try {
            while (!s.isDestroyed()) {
                framework.invoke(new Task<Void>() {
                    @Override
                    public Void run(HardwareAccessLayer access) {
                        Context c = access.setActiveSurface(s);
                        GlslRenderer r = c.getGlslRenderer();
                        r.clear(true, true, true);

                        Frustum view = new Frustum(60.0, 1.0, 1.0, 50.0);
                        view.setOrientation(new Matrix4().lookAt(new Vector3(), new Vector3(0, 2, 2),
                                                                 new Vector3(0, 1, 0)));

                        r.setShader(shader);
                        r.setUniform(modelview, view.getViewMatrix());
                        r.setUniform(projection, view.getProjectionMatrix());

                        r.bindAttribute(vertex, box.getVertices());
                        if (normal != null) {
                            r.bindAttribute(normal, box.getNormals());
                        }
                        if (tangent != null) {
                            r.bindAttribute(tangent, box.getTangents());
                        }
                        if (tc != null) {
                            r.bindAttribute(tc, box.getTextureCoordinates());
                        }
                        r.setIndices(box.getIndices());

                        r.setDrawStyle(Renderer.DrawStyle.SOLID, Renderer.DrawStyle.LINE);
                        //                        r.setDepthTest(Renderer.Comparison.ALWAYS);

                        Matrix4 m = new Matrix4().setIdentity();
                        Vector4 t = new Vector4();

                        for (int z = 0; z < 1; z++) {
                            for (int y = 0; y < 1; y++) {
                                for (int x = 0; x < 1; x++) {
                                    //                                    t.set(LwjglGlslTest.x + 3.5 * (x - 2), 3.5 * (y - 2), 3.5 * (z - 2), 1);
                                    //                                    m.setIdentity().setCol(3, t);

                                    r.setUniform(modelview, m.mul(view.getViewMatrix(), m));
                                    r.render(box.getPolygonType(), box.getIndexOffset(), box.getIndexCount());
                                }
                            }
                        }
                        x += 0.03;
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
