import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

import javax.media.opengl.DebugGL2;
import javax.media.opengl.DebugGL3;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL3;
import javax.media.opengl.GLAutoDrawable;
 
public class ShaderProgram {
 
 
    int p;
    boolean valid = false;
 
    File vertf;
    File fragf;
     
//    String []vertS = {"#version 150\nin vec3 vertex;\nvoid main()\n{\ngl_Position = vec4(vertex,1.0);\n}"};
//    String []fragS = {"#version 150\nout vec4 fragmentColor;\nvoid main()\n{\nfragmentColor = vec4(0.0, 1.0, 0.0, 1.0);\n}"};
//    String[] vertS = {"#version 120\nattribute vec3 vertex;\nvoid main()\n{\ngl_Position = vec4(vertex, 1.0);\n}"};
    String[] vertS = {"#version 120\nattribute vec3 vertex;\nvoid main()\n{\ngl_Position = gl_ProjectionMatrix * gl_ModelViewMatrix * vec4(vertex, 1.0);\n}"};
    String[] fragS = {"#version 120\nvoid main()\n{\ngl_FragColor = vec4(0.0, 1.0, 0.0, 1.0);\n}"};
     
    public ShaderProgram(GLAutoDrawable drawable) {//, File vertf, File fragf) {
//        this.vertf = vertf;
//        this.fragf = fragf;        
        DebugGL2 gl = new DebugGL2( drawable.getGL().getGL2());
 
        p = gl.glCreateProgram();
 
        int v = gl.glCreateShader(gl.GL_VERTEX_SHADER);    
        int f = gl.glCreateShader(gl.GL_FRAGMENT_SHADER);
        readAndCompileShader(v, vertS, gl);
        readAndCompileShader(f, fragS, gl);
 
        gl.glLinkProgram(p);      
 
 
 
        // check link status
        int[] query = new int[1];
        gl.glGetProgramiv(p, GL2.GL_LINK_STATUS, query, 0);
        if (query[0] == GL.GL_TRUE){
            System.out.println("P: successful"); // program linked successfully
            valid = true;
        }else{
            // link failed, read the log and return it
            gl.glGetProgramiv(p, GL3.GL_INFO_LOG_LENGTH, query, 0);
            int maxLogLength = query[0];
            if (maxLogLength > 0) {
                byte[] log = new byte[maxLogLength];
                gl.glGetProgramInfoLog(p, maxLogLength, query, 0, log, 0);
 
                valid = false;
                System.out.println(new String(log, 0, query[0]));
            } else{
                valid = false;
                System.out.println("unknown link error");
            }
        }
    }
     
    void readAndCompileShader(int shader, String[] source, DebugGL2 gl){
//        String [] source = new String[1];
//        try {
//            source[0] = readFile(sourceF);
//        } catch (IOException e) {
//            System.out.println("Could not find shader file");
//        }
//        source[0] = source[0].replaceAll("\r\n", "\n");
        //System.out.print(source[0]);
         
        int[] lengths = new int[]{source[0].length()};
 
        gl.glShaderSource(shader, 1, source, lengths, 0);       
        gl.glCompileShader(shader);
        gl.glAttachShader(p,shader);
 
        // query compile status and possibly read log
        int[] status = new int[1];
        gl.glGetShaderiv(shader, GL2.GL_COMPILE_STATUS, status, 0);
        if (status[0] == GL.GL_TRUE){
            System.out.println("shader: succesful");
            // everything compiled successfully, no log
        }else{
            // compile failed, read the log and return it
            gl.glGetShaderiv(shader, GL3.GL_INFO_LOG_LENGTH, status, 0);
            int maxLogLength = status[0];
            if (maxLogLength > 0) {
                byte[] log = new byte[maxLogLength];
                gl.glGetShaderInfoLog(shader, maxLogLength, status, 0, log, 0);
 
                System.out.println("shader: " + new String(log, 0, status[0]));
            } else
                System.out.println("shader: "+ "unknown compilation error");
        }
    }
     
    private static String readFile(File file) throws IOException {
          FileInputStream stream = new FileInputStream(file);
          try {
            FileChannel fc = stream.getChannel();
            MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
            /* Instead of using default, pass in a decoder. */
            return Charset.forName("UTF-8").decode(bb).toString();
          }
          finally {
            stream.close();
          }
        }
}