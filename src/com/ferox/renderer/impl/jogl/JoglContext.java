package com.ferox.renderer.impl.jogl;

import javax.media.opengl.GLContext;

import com.ferox.renderer.impl.jogl.record.JoglStateRecord;

/** Represents the union of an actual low-level context
 * and the JoglStateRecord that parallels the state modifications
 * of the context.
 * 
 * @author Michael Ludwig
 *
 */
public class JoglContext {
	private final GLContext context;
	private final JoglStateRecord record;
	
	/** These values should not be null. */
	public JoglContext(GLContext context, JoglStateRecord record) {
		this.context = context;
		this.record = record;
	}
	
	public GLContext getContext() {
		return this.context;
	}
	
	public JoglStateRecord getStateRecord() {
		return this.record;
	}
}
