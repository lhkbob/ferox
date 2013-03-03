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
import com.ferox.math.Vector3;
import com.ferox.math.Vector4;
import com.ferox.math.bounds.Frustum;
import com.ferox.renderer.*;
import com.ferox.resource.*;
import com.ferox.resource.GlslShader.ShaderType;
import com.ferox.resource.Resource.Status;
import com.ferox.resource.Texture.Filter;
import com.ferox.resource.Texture.Target;
import com.ferox.resource.geom.Box;
import com.ferox.resource.geom.Geometry;
import com.ferox.util.ApplicationStub;

import java.util.Map;
import java.util.Map.Entry;

public class GlslRenderTest extends ApplicationStub {
    private static final String VERTEX_SHADER =
            "uniform mat4 projection;" + "uniform mat4 modelview;" +
            "uniform vec2 transform;" +

            "attribute vec3 vertex;" + "varying vec3 tcs;" +

            "void main() {" +
            "   tcs = vec3((vertex.x + transform.x) * transform.y, (vertex.y + transform.x) * transform.y, (vertex.z + transform.x) * transform.y);" +
            "   gl_Position = projection * modelview * vec4(vertex, 1.0);" + "}";
    private static final String FRAGMENT_SHADER =
            "uniform vec4 color;" + "uniform sampler3D texture;" +

            "varying vec3 tcs;" +

            "void main() {" + "   gl_FragColor = texture3D(texture, tcs) * color;" + "}";

    private GlslPass pass;

    public GlslRenderTest() {
        super(JoglFramework.create());
    }

    @Override
    protected void installInputHandlers(InputManager io) {
    }

    @Override
    protected void init(OnscreenSurface surface) {
        Framework framework = surface.getFramework();
        pass = new GlslPass(surface);
        Status status = framework.update(pass.shader);
        if (status != Status.READY) {
            System.out.println(
                    "Shader: " + status + " " + framework.getStatusMessage(pass.shader));
            framework.destroy();
            System.exit(0);
        }
    }

    @Override
    protected void renderFrame(OnscreenSurface surface) {
        surface.getFramework().queue(pass);
    }

    public static void main(String[] args) throws Exception {
        new GlslRenderTest().run();
    }

    private static class GlslPass implements Task<Void> {
        final GlslShader shader;
        final Geometry shape;

        final Texture volume;

        final Frustum f;

        boolean statusChecked;
        final Surface surface;

        public GlslPass(Surface surface) {
            this.surface = surface;

            shape = Box.create(4.0);
            shader = new GlslShader();

            shader.setShader(ShaderType.VERTEX, VERTEX_SHADER);
            shader.setShader(ShaderType.FRAGMENT, FRAGMENT_SHADER);

            f = new Frustum(60f, surface.getWidth() / (float) surface.getHeight(), 1f,
                            100f);
            f.setOrientation(new Vector3(0f, 3f, 10f), new Vector3(0f, 0f, -1f),
                             new Vector3(0f, 1f, 0f));

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

            Mipmap data = new Mipmap(new BufferData(volumeBuffer), width, height, depth,
                                     TextureFormat.RGBA);
            volume = new Texture(Target.T_3D, data);
            volume.setFilter(Filter.NEAREST);
        }

        @Override
        public Void run(HardwareAccessLayer access) {
            Context context = access.setActiveSurface(surface);
            if (context == null) {
                return null;
            }

            GlslRenderer g = context.getGlslRenderer();
            if (g != null) {
                g.clear(true, true, true, new Vector4(.2f, .2f, .2f, 1f), 1, 0);

                g.setShader(shader);
                g.bindAttribute("vertex", shape.getVertices());

                g.setUniform("projection", f.getProjectionMatrix());
                g.setUniform("modelview", f.getViewMatrix());

                g.setUniform("color", new Vector4(1f, 1f, 1f, 1f));
                g.setUniform("texture", volume);

                g.setUniform("transform", 2f, .25f);

                g.setIndices(null);
                int rendered = g.render(shape.getPolygonType(), shape.getIndexOffset(),
                                        shape.getIndexCount());

                if (!statusChecked) {
                    statusChecked = true;

                    System.out.println("Rendered count: " + rendered);

                    Status shaderStatus = surface.getFramework().getStatus(shader);
                    String shaderMsg = surface.getFramework().getStatusMessage(shader);

                    System.out.println(shaderStatus + " " + shaderMsg);

                    System.out.println("uniforms:");
                    Map<String, GlslUniform> uniforms = g.getUniforms();
                    for (Entry<String, GlslUniform> u : uniforms.entrySet()) {
                        GlslUniform uniform = u.getValue();
                        System.out
                              .println(uniform.getName() + " " + uniform.getType() + " " +
                                       uniform.getLength());
                    }

                    System.out.println("\nattributes:");
                    System.out.println(g.getAttributes());

                    System.out.println("\ntexture status: " +
                                       surface.getFramework().getStatus(volume) + " " +
                                       surface.getFramework().getStatusMessage(volume));
                }
            }

            return null;
        }
    }
}
