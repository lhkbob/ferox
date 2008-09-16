import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCanvas;
import javax.swing.JFrame;

import com.sun.opengl.util.FPSAnimator;


public class FBOTest implements javax.media.opengl.GLEventListener {
	public static void main(String[] args) {
		JFrame frame = new JFrame();
		GLCanvas canvas = new GLCanvas();
		frame.add(canvas);
		canvas.addGLEventListener(new FBOTest());
		FPSAnimator anim = new FPSAnimator(canvas, 60);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(500, 500);
		frame.setVisible(true);
		canvas.display();
		System.exit(0);
	}
	
	public void display(GLAutoDrawable arg0) {
		
	}

	public void displayChanged(GLAutoDrawable arg0, boolean arg1, boolean arg2) {
		// TODO Auto-generated method stub
		
	}

	public void init(GLAutoDrawable arg0) {
		GL gl = arg0.getGL();
		int[] fboID = new int[1];
		gl.glGenFramebuffersEXT(1, fboID, 0);
		gl.glBindFramebufferEXT(GL.GL_FRAMEBUFFER_EXT, fboID[0]);
		int[] rbs = new int[2];
		/*gl.glGenRenderbuffersEXT(2, rbs, 0);
		gl.glBindRenderbufferEXT(GL.GL_RENDERBUFFER_EXT, rbs[0]);
		gl.glRenderbufferStorageEXT(GL.GL_RENDERBUFFER_EXT, GL.GL_STENCIL_INDEX, 512, 512);
		gl.glFramebufferRenderbufferEXT(GL.GL_FRAMEBUFFER_EXT, GL.GL_STENCIL_ATTACHMENT_EXT, GL.GL_RENDERBUFFER_EXT, rbs[0]);
		*/
		gl.glBindRenderbufferEXT(GL.GL_RENDERBUFFER_EXT, rbs[1]);
		gl.glRenderbufferStorageEXT(GL.GL_RENDERBUFFER_EXT, GL.GL_DEPTH_COMPONENT, 512, 512);
		gl.glFramebufferRenderbufferEXT(GL.GL_FRAMEBUFFER_EXT, GL.GL_DEPTH_ATTACHMENT_EXT, GL.GL_RENDERBUFFER_EXT, rbs[1]);
		
		gl.glBindRenderbufferEXT(GL.GL_RENDERBUFFER_EXT, 0);
		
		int[] tex = new int[1];
		gl.glGenTextures(1, tex, 0);
		gl.glBindTexture(GL.GL_TEXTURE_2D, tex[0]);
		gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL.GL_RGB8, 512, 512, 0, GL.GL_RGB, GL.GL_UNSIGNED_BYTE, null);
		gl.glBindTexture(GL.GL_TEXTURE_2D, 0);

		gl.glFramebufferTexture2DEXT(GL.GL_FRAMEBUFFER_EXT, GL.GL_COLOR_ATTACHMENT0_EXT, GL.GL_TEXTURE_2D, tex[0], 0);
		
		int status = gl.glCheckFramebufferStatusEXT(GL.GL_FRAMEBUFFER_EXT);
		if (status != GL.GL_FRAMEBUFFER_COMPLETE_EXT) {
			String msg = "FBO failed completion test, unable to render";
			switch(status) {
			case GL.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT_EXT: msg = "FBO attachments aren't complete"; break;
			case GL.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT_EXT: msg = "FBO needs at least one attachment"; break;
			case GL.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER_EXT: msg = "FBO draw buffers improperly enabled"; break;
			case GL.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER_EXT: msg = "FBO read buffer improperly enabled"; break;
			case GL.GL_FRAMEBUFFER_UNSUPPORTED_EXT: msg = "Texture attachment formats aren't supported by this vendor"; break;
			}
			System.err.println("NOT COMPLETE: " + msg);
		} else
			System.out.println("FBO complete");
	}

	public void reshape(GLAutoDrawable arg0, int arg1, int arg2, int arg3, int arg4) {
		// TODO Auto-generated method stub
		
	}

}
