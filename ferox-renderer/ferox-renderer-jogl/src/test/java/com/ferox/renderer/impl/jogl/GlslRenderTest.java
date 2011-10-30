package com.ferox.renderer.impl.jogl;

import java.util.Map;
import java.util.Map.Entry;

import com.ferox.math.Vector3f;
import com.ferox.math.Vector4f;
import com.ferox.math.bounds.Frustum;
import com.ferox.renderer.Context;
import com.ferox.renderer.Framework;
import com.ferox.renderer.GlslRenderer;
import com.ferox.renderer.HardwareAccessLayer;
import com.ferox.renderer.OnscreenSurface;
import com.ferox.renderer.OnscreenSurfaceOptions;
import com.ferox.renderer.OnscreenSurfaceOptions.DepthFormat;
import com.ferox.renderer.Surface;
import com.ferox.renderer.Task;
import com.ferox.resource.BufferData;
import com.ferox.resource.GlslShader;
import com.ferox.resource.GlslShader.ShaderType;
import com.ferox.resource.GlslUniform;
import com.ferox.resource.Mipmap;
import com.ferox.resource.Resource.Status;
import com.ferox.resource.Texture;
import com.ferox.resource.Texture.Filter;
import com.ferox.resource.Texture.Target;
import com.ferox.resource.TextureFormat;
import com.ferox.util.geom.Box;
import com.ferox.util.geom.Geometry;

public class GlslRenderTest {
    private static final String VERTEX_SHADER = 
        "uniform mat4 projection;" + 
        "uniform mat4 modelview;" + 
        "uniform vec2 transform;" + 
        
        "attribute vec3 vertex;" + 
        "varying vec3 tcs;" +
        
        "void main() {" +
        "   tcs = vec3((vertex.x + transform.x) * transform.y, (vertex.y + transform.x) * transform.y, (vertex.z + transform.x) * transform.y);" +
        "   gl_Position = projection * modelview * vec4(vertex, 1.0);" +
        "}";
    private static final String FRAGMENT_SHADER = 
        "uniform vec4 color;" +
        "uniform sampler3D texture;" +
        
        "varying vec3 tcs;" +
        
        "void main() {" +
        "   gl_FragColor = texture3D(texture, tcs) * color;" +
        "}";
    
    public static void main(String[] args) throws Exception {
        Framework framework = JoglFramework.create(1, false, false, true, false);
        System.out.println(framework.getCapabilities().getGlslVersion() + " " + framework.getCapabilities().getMaxTexture3DSize());
        OnscreenSurface window = framework.createSurface(new OnscreenSurfaceOptions().setWidth(800)
                                                                                     .setHeight(600)
                                                                                     .setResizable(false)
                                                                                     .setDepthFormat(DepthFormat.DEPTH_24BIT));
        window.setVSyncEnabled(true);
        
        GlslPass pass = new GlslPass(window);
        Status status = framework.update(pass.shader);
        if (status != Status.READY) {
            System.out.println("Shader: " + status + " " + framework.getStatusMessage(pass.shader));
            framework.destroy();
            System.exit(0);
        }
        
        try {
            while(true) {
                if (window.isDestroyed())
                    break;
                framework.queue(pass, "render");
                framework.flush(window, "render");
                framework.sync("render");
            }
        } finally {
            framework.destroy();
        }
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
            
            shape = new Box(4f);
            shader = new GlslShader();
            
            shader.setShader(ShaderType.VERTEX, VERTEX_SHADER);
            shader.setShader(ShaderType.FRAGMENT, FRAGMENT_SHADER);
            
            f = new Frustum(60f, 1f, 1f, 100f);
            f.setOrientation(new Vector3f(0f, 5f, 10f), new Vector3f(0f, 0f, -1f), new Vector3f(0f, 1f, 0f));
            
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
            GlslRenderer g = context.getGlslRenderer();
            
            if (g != null) {
                g.clear(true, true, true, new Vector4f(.2f, .2f, .2f, 1f), 1, 0);
                
                g.setShader(shader);
                g.bindAttribute("vertex", shape.getVertices());
                
                g.setUniform("projection", f.getProjectionMatrix());
                g.setUniform("modelview", f.getViewMatrix());
                
                g.setUniform("color", new Vector4f(1f, 1f, 1f, 1f));
                g.setUniform("texture", volume);
                
                g.setUniform("transform", 2f, .25f);
                
                g.render(shape.getPolygonType(), shape.getIndices(), shape.getIndexOffset(), shape.getIndexCount());
                
                if (!statusChecked) {
                    statusChecked = true;
                    
                    Status shaderStatus = surface.getFramework().getStatus(shader);
                    String shaderMsg = surface.getFramework().getStatusMessage(shader);
                    
                    System.out.println(shaderStatus + " " + shaderMsg);
                    
                    System.out.println("uniforms:");
                    Map<String, GlslUniform> uniforms = g.getUniforms();
                    for (Entry<String, GlslUniform> u: uniforms.entrySet()) {
                        GlslUniform uniform = u.getValue();
                        System.out.println(uniform.getName() + " " + uniform.getType() + " " + uniform.getLength());
                    }
                    
                    System.out.println("\nattributes:");
                    System.out.println(g.getAttributes());
                    
                    System.out.println("\ntexture status: " + surface.getFramework().getStatus(volume) + " " + surface.getFramework().getStatusMessage(volume));
                }
            }
            
            return null;
        }
    }
}
