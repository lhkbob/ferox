package com.ferox.renderer.impl.jogl.record;

/**
 * This represents the part of the PixelRecord pertaining to packing and
 * unpacking blocks of pixel data. The rest of the pixel record is not included
 * because it's unlikely to be used, and much of it is color convolution.
 * 
 * @author Michael Ludwig
 * 
 */
public class PackUnpackRecord {
	public boolean unpackSwapBytes = false;
	public boolean unpackLsbFirst = false;
	public int unpackImageHeight = 0;
	public int unpackSkipImages = 0;
	public int unpackRowLength = 0;
	public int unpackSkipRows = 0;
	public int unpackSkipPixels = 0;
	public int unpackAlignment = 4;

	public boolean packSwapBytes = false;
	public boolean packLsbFirst = false;
	public int packImageHeight = 0;
	public int packSkipImages = 0;
	public int packRowLength = 0;
	public int packSkipRows = 0;
	public int packSkipPixels = 0;
	public int packAlignment = 4;
}
