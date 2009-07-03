package com.ferox.renderer.impl.jogl.record;

/**
 * Class encapsulating the state record for the vertex arrays and vbo's. This
 * record does not track the client bindings for pointers when vbos aren't being
 * used. This is because there is no good way of releasing a pointer, so we
 * would be holding onto references when it could be garbage collected. Also,
 * there is no tracking of the pointers stride or offset. This is to simplify
 * rendering. Because of this, it is recommended to always set the pointers,
 * unless it is known that the pointers and their accessors will not have
 * changed.
 * 
 * @author Michael Ludwig
 */
public class VertexArrayRecord {
	/* Vertex array variables */
	public boolean enableVertexArray = false;

	public boolean enableNormalArray = false;

	public boolean enableFogCoordArray = false;

	public boolean enableColorArray = false;

	public boolean enableSecondaryColorArray = false;

	public boolean enableEdgeFlagArray = false;

	public boolean enableIndexArray = false;

	public final boolean[] enableTexCoordArrays;

	/**
	 * Much like activeTexture in TextureRecord, this is valued from 0 to
	 * maxUnits to be used as an index into the tex coord bindings. The actual
	 * client active texture must have GL_TEXTURE0 added to it.
	 */
	public int clientActiveTexture = 0;

	public final boolean[] enableVertexAttribArrays;

	/* Buffer bindings before glXPointer() call. */
	public int arrayBufferBinding = 0;
	public int elementBufferBinding = 0;

	public VertexArrayRecord(int maxTexCoords, int maxVertexAttribs) {
		enableTexCoordArrays = new boolean[maxTexCoords];

		enableVertexAttribArrays = new boolean[maxVertexAttribs];
	}
}
