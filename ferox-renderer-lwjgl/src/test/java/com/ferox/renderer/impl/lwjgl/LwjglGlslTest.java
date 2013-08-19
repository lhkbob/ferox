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
public class LwjglGlslTest {
    public static final String VERTEX_120 = "#version 120\n" +
                                            "attribute vec4 vertex;\n" +
                                            "attribute vec4 normal;\n" +
                                            "varying vec4 color;\n" +
                                            "uniform mat4 projection;\n" +
                                            "uniform mat4 modelview;\n" +
                                            "void main() {\n" +
                                            "color = normal * 0.5 + vec4(0.5);\n" +
                                            "gl_Position = projection * modelview * vertex;\n" +
                                            "}\n";
    public static final String FRAGMENT_120 = "#version 120\n" +
                                              "varying vec4 color;\n" +
                                              "void main() {\n" +
                                              "gl_FragColor = color;\n" +
                                              "}\n";

    public static final String VERTEX_150 = "#version 150\n" +
                                            "in vec4 vertex;\n" +
                                            "in vec4 normal;\n" +
                                            "out vec4 color;\n" +
                                            "uniform mat4 projection;\n" +
                                            "uniform mat4 modelview;\n" +
                                            "void main() {\n" +
                                            "color = normal * 0.5 + vec4(0.5);\n" +
                                            "gl_Position = projection * modelview * vertex;\n" +
                                            "}\n";
    public static final String FRAGMENT_150 = "#version 150\n" +
                                              "in vec4 color;\n" +
                                              "out vec4 fColor;\n" +
                                              "void main() {\n" +
                                              "fColor = color;\n" +
                                              "}\n";

    static double x = 0;

    public static void main(String[] args) throws Exception {
        final Framework framework = Framework.Factory.create();
        final OnscreenSurface s = framework
                .createSurface(new OnscreenSurfaceOptions().windowed(500, 500).withDepthBuffer(24));
        s.setTitle("Hello World");
        s.setVSyncEnabled(true);

        //        final Geometry box = Sphere.create(framework, 1.5, 8);
        final Geometry box = Box.create(framework, 3.0);

        final Shader shader;

        if (framework.getCapabilities().getMajorVersion() >= 3) {
            shader = framework.newShader().withVertexShader(VERTEX_150).withFragmentShader(FRAGMENT_150)
                              .build();
        } else {
            shader = framework.newShader().withVertexShader(VERTEX_120).withFragmentShader(FRAGMENT_120)
                              .bindColorBuffer("fColor", 0).build();
        }
        final Shader.Uniform modelview = shader.getUniform("modelview");
        final Shader.Uniform projection = shader.getUniform("projection");
        final Shader.Attribute vertex = shader.getAttribute("vertex");
        final Shader.Attribute normal = shader.getAttribute("normal");

        try {
            while (!s.isDestroyed()) {
                framework.invoke(new Task<Void>() {
                    @Override
                    public Void run(HardwareAccessLayer access) {
                        Context c = access.setActiveSurface(s);
                        GlslRenderer r = c.getGlslRenderer();
                        r.clear(true, true, true);

                        Frustum view = new Frustum(60.0, 1.0, 1.0, 25.0);
                        view.setOrientation(new Vector3(0, 0, 25), new Vector3(0, 0, -1),
                                            new Vector3(0, 1, 0));

                        r.setShader(shader);
                        r.setUniform(modelview, view.getViewMatrix());
                        r.setUniform(projection, view.getProjectionMatrix());

                        r.bindAttribute(vertex, box.getVertices());
                        r.bindAttribute(normal, box.getNormals());
                        r.setIndices(box.getIndices());

                        r.setDrawStyle(Renderer.DrawStyle.SOLID, Renderer.DrawStyle.LINE);
                        //                        r.setDepthTest(Renderer.Comparison.ALWAYS);

                        Matrix4 m = new Matrix4().setIdentity();
                        Vector4 t = new Vector4();

                        for (int z = 0; z < 5; z++) {
                            for (int y = 0; y < 5; y++) {
                                for (int x = 0; x < 5; x++) {
                                    t.set(LwjglGlslTest.x + 3.5 * (x - 2), 3.5 * (y - 2), 3.5 * (z - 2), 1);
                                    m.setCol(3, t);

                                    r.setUniform(modelview, m.mul(view.getViewMatrix(), m));
                                    r.render(box.getPolygonType(), box.getIndexOffset(), box.getIndexCount());
                                }
                            }
                        }
                        x += 0.001;
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
