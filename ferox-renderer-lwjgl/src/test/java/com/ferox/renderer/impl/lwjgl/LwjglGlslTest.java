package com.ferox.renderer.impl.lwjgl;

import com.ferox.renderer.*;

/**
 *
 */
public class LwjglGlslTest {
    public static void main(String[] args) throws Exception {
        Framework f = Framework.Factory.create();
        try {
            String vSrc = f.getCapabilities().getGlslVersion() == 150 ? VERTEX_SHADER_150 : VERTEX_SHADER_120;
            String fSrc =
                    f.getCapabilities().getGlslVersion() == 150 ? FRAGMENT_SHADER_150 : FRAGMENT_SHADER_120;

            Shader s = f.newShader().withVertexShader(vSrc).withFragmentShader(fSrc).build();

            for (Shader.Uniform u : s.getUniforms()) {
                System.out.println(u);
            }

            for (Shader.Attribute a : s.getAttributes()) {
                System.out.println(a);
            }

            if (f.getCapabilities().getMajorVersion() >= 3) {
                final OnscreenSurface w = f.createSurface(new OnscreenSurfaceOptions());
                f.invoke(new Task<Object>() {
                    @Override
                    public Object run(HardwareAccessLayer access) {
                        access.setActiveSurface(w).getFixedFunctionRenderer();
                        return null;
                    }
                }).get();
            }
        } catch (Exception e) {
            System.err.println("THIS IS THE MAIN ONE!");
            e.printStackTrace();
        } finally {
            f.destroy();
        }
    }

    public static final String VERTEX_SHADER_120 = "#version 120\n" +
                                                   "attribute vec3 vertices;\n" +
                                                   "attribute vec4 used;\n" +
                                                   "attribute vec2 moarattrs;\n" +
                                                   "void main() {\n" +
                                                   "gl_Position = used + vec4(moarattrs, 1.0, 1.0) + vec4(vertices, 1.0);\n" +
                                                   "}\n";

    public static final String FRAGMENT_SHADER_120 = "#version 120\n" +
                                                     "struct Foo {\n" +
                                                     "vec3 bar[3];\n" +
                                                     "vec2 arg;\n" +
                                                     "};\n" +
                                                     "uniform Foo vars[3];\n" +
                                                     "uniform vec3 moar[5];\n" +
                                                     "void main() {\n" +
                                                     "vec4 color = vec4(0.0, 0.0, 0.0, 1.0);\n" +
                                                     "color.x = vars[0].bar[0].x + vars[1].bar[1].x + vars[0].arg.x + vars[1].arg.x;\n" +
                                                     "color.y = vars[0].bar[1].y + vars[1].bar[2].y + vars[0].arg.y + vars[1].arg.y;\n" +
                                                     "color.z = vars[0].bar[2].z + vars[1].bar[0].z;\n" +
                                                     "color = color + vec4(moar[0], 1.0) + vec4(moar[3], 1.0);\n" +
                                                     "gl_FragColor = color;\n" +
                                                     "}\n";

    public static final String VERTEX_SHADER_150 = "#version 150\n" +
                                                   "in vec3 vertices[2];\n" +
                                                   "in vec4 used;\n" +
                                                   "in vec2 moarattrs;\n" +
                                                   "void main() {\n" +
                                                   "gl_Position = vec4(moarattrs, 1.0, 1.0) +vec4(vertices[0], 1.0) + used;\n" +
                                                   "}\n";

    public static final String FRAGMENT_SHADER_150 = "#version 150\n" +
                                                     "struct Foo {\n" +
                                                     "vec3 bar[3];\n" +
                                                     "vec2 arg;\n" +
                                                     "};\n" +
                                                     "uniform Foo vars[3];\n" +
                                                     "uniform vec3 moar[5];\n" +
                                                     "out vec4 color;\n" +
                                                     "void main() {\n" +
                                                     "vec4 color = vec4(0.0, 0.0, 0.0, 1.0);\n" +
                                                     "color.x = vars[0].bar[0].x + vars[1].bar[0].x + vars[0].arg.x + vars[1].arg.x;\n" +
                                                     "color.y = vars[0].bar[0].y + vars[1].bar[0].y + vars[0].arg.y + vars[1].arg.y;\n" +
                                                     "color.z = vars[0].bar[0].z + vars[1].bar[0].z;\n" +
                                                     "color = color + vec4(moar[0], 1.0) + vec4(moar[3], 1.0);\n" +
                                                     "}\n";
}
