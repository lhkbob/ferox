package com.ferox.core.system;

public class DisplayOptions {
	private int redBits;
	private int greenBits;
	private int blueBits;
	private int alphaBits;

	private int depthBits;
	private int stencilBits;
	
	private boolean stereo;
	private boolean doubleBuffered;
	private int multiSampleBuffers;
	
	private boolean headless;
	private boolean heavyweight;
	
	private int width;
	private int height;
	
	private boolean readOnly;
	
	public DisplayOptions() {
		this.setRedBits(8);
		this.setGreenBits(8);
		this.setBlueBits(8);
		this.setAlphaBits(8);
		
		this.setStereo(false);
		this.setDoubleBuffered(true);
		this.setHeadless(false);
		this.setHeavyweight(true);
		
		this.setWidth(640);
		this.setHeight(480);
		
		this.readOnly = false;
	}
	
	public static DisplayOptions createReadOnly(int red, int green, int blue, int alpha, int depth, int stencil, int samples,
												boolean stereo, boolean doubleBuff, boolean headless, boolean heavyweight) {
		DisplayOptions o = new DisplayOptions();
		o.setRedBits(red);
		o.setGreenBits(green);
		o.setBlueBits(blue);
		o.setAlphaBits(alpha);
		o.setDepthBits(depth);
		o.setStencilBits(stencil);
		o.setStereo(stereo);
		o.setDoubleBuffered(doubleBuff);
		o.setHeadless(headless);
		o.setHeavyweight(heavyweight);
		
		o.setNumMultiSamples(samples);
		
		o.readOnly = true;
		return o;
	}
	
	public int getWidth() {
		return this.width;
	}
	
	public void setWidth(int width) throws UnsupportedOperationException {
		if (!this.readOnly)
			this.width = width;
		else
			throw new UnsupportedOperationException("Can't set on a read-only DisplayOptions");
	}
	
	public int getHeight() {
		return this.height;
	}
	
	public void setHeight(int height) throws UnsupportedOperationException {
		if (!this.readOnly)
			this.height = height;
		else
			throw new UnsupportedOperationException("Can't set on a read-only DisplayOptions");
	}
	
	public int getRedBits() {
		return this.redBits;
	}
	
	public void setRedBits(int redBits) throws UnsupportedOperationException {
		if (!this.readOnly)
			this.redBits = redBits;
		else
			throw new UnsupportedOperationException("Can't set on a read-only DisplayOptions");
	}
	
	public int getGreenBits() {
		return this.greenBits;
	}
	
	public void setGreenBits(int greenBits) throws UnsupportedOperationException {
		if (!this.readOnly)
			this.greenBits = greenBits;
		else
			throw new UnsupportedOperationException("Can't set on a read-only DisplayOptions");
	}
	
	public int getBlueBits() {
		return this.blueBits;
	}
	
	public void setBlueBits(int blueBits) throws UnsupportedOperationException {
		if (!this.readOnly)
			this.blueBits = blueBits;
		else
			throw new UnsupportedOperationException("Can't set on a read-only DisplayOptions");
	}
	
	public int getAlphaBits() {
		return this.alphaBits;
	}
	
	public void setAlphaBits(int alphaBits) throws UnsupportedOperationException {
		if (!this.readOnly)	
			this.alphaBits = alphaBits;
		else
			throw new UnsupportedOperationException("Can't set on a read-only DisplayOptions");
	}
	
	public boolean isStereo() {
		return this.stereo;
	}
	
	public void setStereo(boolean stereo) {
		if (!this.readOnly)
			this.stereo = stereo;
	}
	
	public boolean isDoubleBuffered() {
		return this.doubleBuffered;
	}
	
	public void setDoubleBuffered(boolean doubleBuffered) throws UnsupportedOperationException {
		if (!this.readOnly)
			this.doubleBuffered = doubleBuffered;
		else
			throw new UnsupportedOperationException("Can't set on a read-only DisplayOptions");
	}
	
	public int getNumMultiSamples() {
		return this.multiSampleBuffers;
	}
	
	public void setNumMultiSamples(int multiSamples) throws UnsupportedOperationException {
		if (!this.readOnly)
			this.multiSampleBuffers = multiSamples;
		else
			throw new UnsupportedOperationException("Can't set on a read-only DisplayOptions");
	}
	
	public boolean isHeadless() {
		return this.headless;
	}
	
	public void setHeadless(boolean headless) throws UnsupportedOperationException {
		if (!this.readOnly)
			this.headless = headless;
		else
			throw new UnsupportedOperationException("Can't set on a read-only DisplayOptions");
	}
	
	public boolean isHeavyweight() {
		return this.heavyweight;
	}
	
	public void setHeavyweight(boolean heavyweight) throws UnsupportedOperationException {
		if (!this.readOnly)
			this.heavyweight = heavyweight;
		else
			throw new UnsupportedOperationException("Can't set on a read-only DisplayOptions");
	}
	
	public int getStencilBits() {
		return this.stencilBits;
	}
	
	public void setStencilBits(int bits) throws UnsupportedOperationException {
		if (!this.readOnly)
			this.stencilBits = bits;
		else
			throw new UnsupportedOperationException("Can't set on a read-only DisplayOptions");
	}
	
	public void setDepthBits(int bits) throws UnsupportedOperationException {
		if (!this.readOnly)
			this.depthBits = bits;
		else
			throw new UnsupportedOperationException("Can't set on a read-only DisplayOptions");
	}
	
	public int getDepthBits() {
		return this.depthBits;
	}
}
