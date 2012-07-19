package com.ferox.renderer.impl.lwjgl;

import com.ferox.math.Matrix4;
import com.ferox.math.Vector3;
import com.ferox.math.Vector4;
import com.ferox.math.bounds.Frustum;
import com.ferox.renderer.Context;
import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.renderer.FixedFunctionRenderer.TexCoord;
import com.ferox.renderer.FixedFunctionRenderer.TexCoordSource;
import com.ferox.renderer.Framework;
import com.ferox.renderer.HardwareAccessLayer;
import com.ferox.renderer.OnscreenSurface;
import com.ferox.renderer.OnscreenSurfaceOptions;
import com.ferox.renderer.OnscreenSurfaceOptions.DepthFormat;
import com.ferox.renderer.Renderer.DrawStyle;
import com.ferox.renderer.Surface;
import com.ferox.renderer.Task;
import com.ferox.resource.BufferData;
import com.ferox.resource.Mipmap;
import com.ferox.resource.Texture;
import com.ferox.resource.Texture.Filter;
import com.ferox.resource.Texture.Target;
import com.ferox.resource.TextureFormat;
import com.ferox.resource.VertexBufferObject.StorageMode;
import com.ferox.util.geom.Box;
import com.ferox.util.geom.Geometry;
import com.ferox.util.geom.Sphere;
import com.ferox.util.geom.Teapot;

public class FixedFunctionRenderTest {
    
    public static void main(String[] args) throws Exception {
        Framework framework = LwjglFramework.create(1);
        System.out.println(framework.getCapabilities().getGlslVersion() + " " + framework.getCapabilities().getMaxTexture3DSize());
        OnscreenSurface window = framework.createSurface(new OnscreenSurfaceOptions().setWidth(800)
                                                                                     .setHeight(600)
//                                                                                     .setFullscreenMode(new DisplayMode(1440, 900, PixelFormat.RGB_24BIT))
                                                                                     .setResizable(false)
                                                                                     .setDepthFormat(DepthFormat.DEPTH_24BIT));
        window.setVSyncEnabled(true);
        FixedFunctionPass pass = new FixedFunctionPass(window);
        try {
            long now = System.currentTimeMillis();
            int frames = 0;
            while(true) {
                if (window.isDestroyed())
                    break;
                framework.queue(pass).get();
                framework.flush(window);
                framework.sync();
                
                frames++;
                if (System.currentTimeMillis() - now > 1000) {
                    Runtime r = Runtime.getRuntime();
                    System.out.printf("Memory: %.2f of %.2f\n", (r.totalMemory() - r.freeMemory()) / (1024f * 1024f), r.totalMemory() / (1024f * 1024f));
                    System.out.println("FPS: " + frames);
                    frames = 0;
                    now = System.currentTimeMillis();
                }
            }
        } finally {
            framework.destroy();
        }
    }
    
    private static class FixedFunctionPass implements Task<Void> {
        final Geometry box;
        final Geometry sphere;
        final Geometry teapot;
        
        final Texture volume;
        
        final Frustum f;
        
        boolean statusChecked;
        final Surface surface;
        
        public FixedFunctionPass(Surface surface) {
            this.surface = surface;
            
            box = new Box(2f, StorageMode.GPU_STATIC);
            sphere = new Sphere(2f, 32, StorageMode.GPU_STATIC);
            teapot = new Teapot(.5f, StorageMode.GPU_STATIC);
            
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
            if (context == null)
                return null;
            
            FixedFunctionRenderer g = context.getFixedFunctionRenderer();
            if (g != null) {
                g.clear(true, true, true, new Vector4(.2f, .2f, .2f, 1f), 1, 0);
                
                g.setDrawStyle(DrawStyle.SOLID);
                
                
                
                g.setTexture(0, volume);
                g.setTextureCoordGeneration(0, TexCoord.R, TexCoordSource.OBJECT);
                
                g.setProjectionMatrix(f.getProjectionMatrix());
                
                Geometry shape;
                Matrix4 t = new Matrix4();
                int rendered = 0;
                int num = 10000;
                int thirds = num / 3;
                for (int i = 0; i < num; i++) {
                    t.setIdentity();
                    t.set(0, 3, (float) Math.random() * 100 - 50);
                    t.set(1, 3, (float) Math.random() * 100 - 50);
                    t.set(2, 3, (float) Math.random() * 100 - 50);

                    g.setModelViewMatrix(f.getViewMatrix().mul(t, t));

//                    switch(i % 3) {
                    switch(i / thirds) {
//                    switch((int) (Math.random() * 3)) {
                    case 0: shape = box; break;
                    case 1: shape = sphere; break;
                    case 2: shape = teapot; break;
                    default: shape = sphere; break;
                    }
                    
                    g.setVertices(shape.getVertices());
                    g.setNormals(shape.getNormals());
                    g.setTextureCoordinates(0, shape.getTextureCoordinates());
                    
                    if (shape.getIndices() != null)
                        rendered += g.render(shape.getPolygonType(), shape.getIndices(), shape.getIndexOffset(), shape.getIndexCount());
                    else
                        rendered += g.render(shape.getPolygonType(), shape.getIndexOffset(), shape.getIndexCount());
                }
                
                if (!statusChecked) {
                    statusChecked = true;
                    System.out.println("Rendered count: " + rendered);
                }
            }
            
            return null;
        }
    }
}
