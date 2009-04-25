package com.ferox.renderer.impl.jogl;

import java.awt.Frame;

import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLContext;

import com.ferox.renderer.RenderCapabilities;
import com.ferox.renderer.RenderException;
import com.ferox.renderer.impl.jogl.record.JoglStateRecord;

/** OnscreenShadowContext provides a GLContext from a GLCanvas, and when
 * necessary controls making the canvas visible.  It attempts to keep 
 * the canvas hidden unless GL calls actually need to be made (in which case,
 * it makes it visible only when necessary).
 * 
 * This is intended to be a fall-back shadow context when a PbufferShadowContext
 * can't be used.
 * 
 * @author Michael Ludwig
 *
 */
public class OnscreenShadowContext extends AbstractShadowContext {
	// to be executed on the EDT before the canvas has display() called
	private Runnable showFrame;
	private Runnable hideFrame;
	
	private Frame frame;
	private GLCanvas canvas;
	
	private JoglStateRecord record;
	private RenderCapabilities caps;
	
	public OnscreenShadowContext(RenderCapabilities caps) {
		this.canvas = new GLCanvas();
		this.canvas.addGLEventListener(this);
		
		this.caps = caps;
		this.record = new JoglStateRecord(caps); // init here just so we won't return a null state record ever
		
		this.frame = null;
		
		this.showFrame = new Runnable() {
			public void run() {
				if (OnscreenShadowContext.this.frame == null) {
					Frame f = new Frame();
					f.setSize(1, 1);
					f.setResizable(false);
					f.setUndecorated(true);
					f.setTitle("");
					f.add(OnscreenShadowContext.this.canvas);
					
					OnscreenShadowContext.this.frame = f;
				}
				
				OnscreenShadowContext.this.frame.setVisible(true);
			}
		};
		
		this.hideFrame = new Runnable() {
			public void run() {
				OnscreenShadowContext.this.frame.setVisible(false);
			}
		};
	}
	
	@Override
	public void render() throws RenderException {
		// must make the frame visible so that the context is valid
		// for gl execution (instead of just context sharing)
		JoglSurfaceFactory.invokeOnAwtThread(this.showFrame);
		
		this.record = new JoglStateRecord(this.caps);
		
		try {
			super.render();
		} finally {
			// must always hide the frame, even when an exception is thrown
			JoglSurfaceFactory.invokeOnAwtThread(this.hideFrame);
		}
	}
	
	@Override
	public void destroy() {
		if (this.frame != null) {
			JoglSurfaceFactory.invokeOnAwtThread(new Runnable() {
				public void run() {
					OnscreenShadowContext.this.frame.setVisible(false);
					OnscreenShadowContext.this.frame.dispose();
				}
			});
			this.frame = null;
		}
		
		this.canvas.getContext().destroy();
	}

	@Override
	public GLContext getContext() {
		return this.canvas.getContext();
	}

	@Override
	public GLAutoDrawable getGLAutoDrawable() {
		return this.canvas;
	}

	@Override
	public JoglStateRecord getStateRecord() {
		return this.record;
	}
}
