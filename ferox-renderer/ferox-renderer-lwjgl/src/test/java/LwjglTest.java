import java.awt.Canvas;
import java.awt.Frame;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.ARBTextureFloat;
import org.lwjgl.opengl.AWTGLCanvas;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;


public class LwjglTest {
    
    // THINGS I'VE LEARNED:
    // 1. Using pure Display system, I can get fullscreen windows or decorated, non-resizable windows
    // 2. Using the Display system with a Frame and Canvas parenting, I can get a resizable or undecorated window via Display
    // 3. If I want more windows after using Display, I need to use a AWTGLCanvas and override its paintGL() method
    //    and call repaint() to trigger a render
    // 4. In #2 and #3, I have to add window listeners to properly dispose of the window on closing request
    // 5. Mac's quit feature doesn't seem to work :(
    // 6. Pure Display for #1 must check if close is requested and destroy the window manually
    // 7. I don't know how to handle fullscreen support if using a parented Display, or with an AWTGLCanvas,
    //    when parenting, fullscreen requests via Display are ignored, my guess is I'll have to use AWT's fullscreen support
    // 8. Calling setDisplayMode on a windowed static Display changes the size of it
    // 9. Fullscreen support just seems plain old broken in Display (and I believe it was with AWT too, so maybe the Mac JVM is wrong?)
    public static void main(String[] args) throws Exception {
        System.out.println(ARBTextureFloat.GL_RGB32F_ARB + " " + GL30.GL_RGB32F);
        
        /*Frame frame = new Frame();
        frame.setSize(640, 480);
        frame.setTitle("LWJGL - parent mode");
        
        Canvas canvas = new Canvas();
//        frame.setUndecorated(true);
//        frame.setResizable(false);
        frame.add(canvas);
//        frame.setVisible(true);
        
        frame.addWindowListener(new WindowListener() {
            @Override
            public void windowOpened(WindowEvent e) {
            }
            
            @Override
            public void windowIconified(WindowEvent e) {
            }
            
            @Override
            public void windowDeiconified(WindowEvent e) {
            }
            
            @Override
            public void windowDeactivated(WindowEvent e) {
            }
            
            @Override
            public void windowClosing(WindowEvent e) {
                System.out.println("Close requested");
                e.getWindow().setVisible(false);
                e.getWindow().dispose();
            }
            
            @Override
            public void windowClosed(WindowEvent e) {
                System.out.println("Closed");
            }
            
            @Override
            public void windowActivated(WindowEvent e) {
            }
        });*/
        
        // use setParent to get a native display
        Display.setTitle("LWJGL");
        Display.setDisplayMode(new DisplayMode(1024, 768));
        Display.setFullscreen(true);
//        Display.setParent(canvas);
        Display.create();
        
        // auxiliary window using AWTGLCanvas
        /*Frame frame2 = new Frame();
        frame2.setSize(640, 480);
        frame2.setTitle("LWJGL - parent mode 2");
        
        AWTGLCanvas glCanvas = new AWTGLCanvas() {
            float c = 0f;
            protected void paintGL() {
                c += .0001f;
                if (c > 1f)
                    c = 0f;
                GL11.glClearColor(c, c, c, 1f);
                GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
                IntBuffer buff = BufferUtils.createIntBuffer(2);
                GL15.glGenBuffers(buff);
                GL15.glDeleteBuffers(buff);
//                System.out.println(buff.get(0));
                try {
                    swapBuffers();
                } catch (LWJGLException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        };
        frame2.add(glCanvas);
        //frame2.setVisible(true);
        
        frame2.addWindowListener(new WindowListener() {
            @Override
            public void windowOpened(WindowEvent e) {
            }
            
            @Override
            public void windowIconified(WindowEvent e) {
            }
            
            @Override
            public void windowDeiconified(WindowEvent e) {
            }
            
            @Override
            public void windowDeactivated(WindowEvent e) {
            }
            
            @Override
            public void windowClosing(WindowEvent e) {
                System.out.println("Close requested");
                e.getWindow().setVisible(false);
                e.getWindow().dispose();
            }
            
            @Override
            public void windowClosed(WindowEvent e) {
                System.out.println("Closed");
            }
            
            @Override
            public void windowActivated(WindowEvent e) {
            }
        });*/
        
//        Display.setFullscreen(true);
//        Display.setFullscreen(true);
        long now = System.nanoTime();
        int frameCount = 0;
        while(true) {
//            if (!frame.isVisible() && !frame2.isVisible())
//                break;
            if (!update())
                break;
            //updateGLCanvas(glCanvas);
            frameCount++;
            
            if (frameCount > 1000) {
                System.out.println("avg fps: " + (1e9f / ((System.nanoTime() - now) / frameCount))    );
                frameCount = 0;
                now = System.nanoTime();
            }
        }
        
        Display.destroy();
    }
    
    private static boolean updateGLCanvas(AWTGLCanvas glCanvas) throws Exception {
        glCanvas.repaint();
        
        return glCanvas.isVisible();
    }
    
    private static boolean update() throws Exception {
        if (!Display.isCreated())
            return false;
        
        Display.update();
        if (Display.isCloseRequested()) {
            Display.destroy();
            return false;
        }
        
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        
        ByteBuffer data = ByteBuffer.allocateDirect(16 * 16 * 4);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, 16, 16, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, data);

        return true;
    }
}
