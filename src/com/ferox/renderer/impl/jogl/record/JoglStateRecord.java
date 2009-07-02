package com.ferox.renderer.impl.jogl.record;

import com.ferox.renderer.RenderCapabilities;

/**
 * This class holds contains references to classes that encapsulate different
 * sections of the OpenGL state record. These records are close to complete, but
 * some states were left out because of their little value or unlikelihood of
 * use. Also, transforms and clip planes are not tracked because that is more
 * complicated, and the tracking would somewhat defeat the purpose of doing that
 * math on the graphics card. Because transform's are not tracked, drivers that
 * modify the matrix mode must be sure to change it back to GL_MODELVIEW, or it
 * will break the functionality of JoglTransformDriver.
 * 
 * @author Michael Ludwig
 */
public class JoglStateRecord {
	public final ColoringRecord colorRecord;
	public final FramebufferRecord frameRecord;
	public final HintRecord hintRecord;
	public final LightingRecord lightRecord;
	public final PackUnpackRecord packRecord;
	public final PixelOpRecord pixelOpRecord;
	public final RasterizationRecord rasterRecord;
	public final GlslShaderRecord shaderRecord;
	public final TextureRecord textureRecord;
	public final VertexArrayRecord vertexArrayRecord;

	/**
	 * Create and initialize all of the sub-records, with the appropriate unit
	 * sizes taken from the RenderCapabilities.
	 */
	public JoglStateRecord(RenderCapabilities caps) {
		colorRecord = new ColoringRecord();
		frameRecord = new FramebufferRecord();
		hintRecord = new HintRecord();
		lightRecord = new LightingRecord(caps.getMaxActiveLights());
		pixelOpRecord = new PixelOpRecord();
		packRecord = new PackUnpackRecord();
		rasterRecord = new RasterizationRecord();
		shaderRecord = new GlslShaderRecord();

		int maxTU = Math.max(caps.getMaxFixedPipelineTextures(), Math.max(caps.getMaxVertexShaderTextures(), 
																		  caps.getMaxFragmentShaderTextures()));
		textureRecord = new TextureRecord(maxTU);
		vertexArrayRecord = new VertexArrayRecord(caps.getMaxTextureCoordinates(), 
												  caps.getMaxVertexAttributes());
	}
}
