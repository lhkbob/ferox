import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.media.opengl.DebugGL2;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
import javax.media.opengl.glu.GLU;
import javax.swing.JFrame;

import com.jogamp.opengl.util.FPSAnimator;
 
public class RealMain{   
    public static void main(String[] args)    {       
        JFrame frame = new JFrame("JOGL test");
        frame.setSize(640, 480);
        GLProfile glProfile = GLProfile.getDefault();
        GLCapabilities glCapabilities = new GLCapabilities(glProfile);
//        GLCanvas canvas = new PaintDisabledGLCanvas(glCapabilities);
        GLCanvas canvas = new GLCanvas(glCapabilities);       
        frame.add(canvas);      
        canvas.addGLEventListener(new JOGLRenderer());  
        
        frame.setVisible(true);  
        
        FPSAnimator animator = new FPSAnimator(canvas, 60);  
        animator.add(canvas);
        animator.start();      
        frame.addWindowListener(new WindowAdapter(){          
            public void windowClosing(WindowEvent e)            { 
                System.exit(0);            }        });  
    }  
    private static class JOGLRenderer implements GLEventListener    {
        ShaderProgram shader;
        int vertexVBO;
        int indexVBO;
        
        public void init(GLAutoDrawable drawable){
 
 
 
//            shader = new ShaderProgram(drawable, new File("bin/vert.txt"), new File("bin/frag.txt"));
            shader = new ShaderProgram(drawable);
 
            //setup opengl
            GL2 gl = new DebugGL2(drawable.getGL().getGL2());      
            gl.glClearColor(0.2f, 0.2f, 0.0f, 0.0f);       
            gl.glClearDepth(1.0f);         
            gl.glEnable(GL.GL_DEPTH_TEST);        
            gl.glDepthFunc(GL.GL_LEQUAL);
            gl.glHint(GL2.GL_PERSPECTIVE_CORRECTION_HINT, GL.GL_NICEST); 
 
            // Create vertex data          
            float[] vertexData = new float[]{
                    -1f, 1f, -2.0f, //0
                    1f, 1f, -2.0f, //1
                    -1f, -1f, -2.0f, //2
                    1f, -1f, -2.0f, //3
            };
            ByteBuffer vertexByteBuffer = ByteBuffer.allocateDirect(vertexData.length * 4);           
            vertexByteBuffer.order(ByteOrder.nativeOrder());     
            FloatBuffer vertexBuffer = vertexByteBuffer.asFloatBuffer();         
            vertexBuffer.put(vertexData);
 
            // Create vertex buffer                
            int[] vertexBufferId = new int[1];   
            gl.glGenBuffers(1, vertexBufferId, 0);  
            
            vertexVBO = vertexBufferId[0];

            System.out.println(vertexByteBuffer.capacity());
            gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, vertexBufferId[0]);     
            gl.glBufferData(GL2.GL_ARRAY_BUFFER, 48, null, GL2.GL_DYNAMIC_DRAW);
            // Load vertex data into vertex buffer         
            gl.glBufferSubData(GL2.GL_ARRAY_BUFFER, 0, vertexByteBuffer.capacity(), vertexByteBuffer.rewind());
 
            gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);
            
            // Create index data          
            int[] indexData = new int[]{0, 1, 2, 3};
 
            ByteBuffer indexByteBuffer = ByteBuffer.allocateDirect(4 * 4);    
            indexByteBuffer.order(ByteOrder.nativeOrder());
            IntBuffer indexBuffer = indexByteBuffer.asIntBuffer();      
            indexBuffer.put(indexData);
 
            // Create index buffer    
            int[] indexBufferId = new int[1];   
            gl.glGenBuffers(1, indexBufferId, 0);
            
            indexVBO = indexBufferId[0];

            gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, indexBufferId[0]);         
            gl.glBufferData(GL2.GL_ELEMENT_ARRAY_BUFFER, 16, null, GL2.GL_DYNAMIC_DRAW);      
            // Load index data into index buffer         
            gl.glBufferSubData(GL2.GL_ELEMENT_ARRAY_BUFFER, 0, indexByteBuffer.capacity(), indexByteBuffer.rewind());
 
             gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, 0);
 
        }
        public void dispose(GLAutoDrawable drawable){}
        public void display(GLAutoDrawable drawable){        
            GL2 gl = new DebugGL2(drawable.getGL().getGL2());  
            gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);      
 
//            gl.glColor3f(1f, 1f, 1f);
            //gl.glMatrixMode(GL2.GL_MODELVIEW);
            //gl.glLoadIdentity();         
            int stride = 3 * 4; //3 floats per vert * 4 bytes per float
 
            gl.glUseProgram(shader.p);
 
            gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, vertexVBO);
            gl.glEnableVertexAttribArray(gl.glGetAttribLocation(shader.p, "vertex"));
            gl.glVertexAttribPointer(gl.glGetAttribLocation(shader.p, "vertex"), 3, gl.GL_FLOAT, false, 0, 0);
            gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);
             
            //shader code
            gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, indexVBO);
            gl.glDrawRangeElements(GL2.GL_TRIANGLE_STRIP, 0, 3, 4, GL2.GL_UNSIGNED_INT, 0); //start render at 0 verts go to 3 for a count of 4
            gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, 0);
            gl.glUseProgram(0);
        }   
 
 
        public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height)        {         
            GL2 gl = drawable.getGL().getGL2();    
            GLU glu = new GLU();
            gl.glViewport(0, 0, width, height);  
            gl.glMatrixMode(GL2.GL_PROJECTION);
            gl.glLoadIdentity();
            glu.gluPerspective(60.0f, (float) width / (float) height, 0.1f, 100.0f);
            gl.glMatrixMode(GL2.GL_MODELVIEW);
            gl.glLoadIdentity();
//            gl.glTranslatef(0f, 0f, -10f);
        }
    }
}