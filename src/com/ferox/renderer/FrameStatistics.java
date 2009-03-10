package com.ferox.renderer;

import java.io.PrintStream;

/** Holds timings and counts for a single frame of rendering.
 * 
 * @author Michael Ludwig
 *
 */
public class FrameStatistics {
	private int totalMeshCount;
	private int totalVertexCount;
	private int totalPolygonCount;
	
	private long waitTime;
	private long prepareTime;
	private long renderTime;
	
	public FrameStatistics() {
		this.reset();
	}
	
	/** Add the given counts of meshes, vertices, and polygons to this FrameStatistic's totals.
	 * Expects values > 0 (inaccurate/misleading results if not positive). */
	public void add(int meshCount, int vertCount, int polyCount) {
		this.totalPolygonCount += polyCount;
		this.totalMeshCount += meshCount;
		this.totalVertexCount += vertCount;
	}
	
	/** The total duration (ns) of this frame. */
	public long getFrameTime() {
		return this.prepareTime + this.renderTime;
	}
	
	/** Get the total time from the end of last frame to the end of this frame. */
	public long getTotalTime() {
		return this.getFrameTime() + this.waitTime;
	}
	
	/** The total time (ns) spent preparing the render passes. */
	public long getPrepareTime() {
		return this.prepareTime;
	}
	
	/** The total time (ns) spent rendering the previously updated/visited scenes. */
	public long getRenderTime() {
		return this.renderTime;
	}
	
	/** The time (ns) spent outside of the Renderer, e.g. time spent between frames. */
	public long getIdleTime() {
		return this.waitTime;
	}
	
	/** The frames/sec of this frame, including time spent external to rendering. */
	public float getFramesPerSecond() {
		return 1e9f / this.getTotalTime();
	}

	/** The polygon/sec of this frame, including time spent external to rendering. */
	public float getPolygonsPerSecond() {
		return this.getFramesPerSecond() * this.totalPolygonCount;
	}

	/** The total number of polygons rendered in this frame */
	public int getPolygonCount() {
		return this.totalPolygonCount;
	}

	/** The number of objects rendered in this frame. */
	public int getMeshCount() {
		return this.totalMeshCount;
	}

	/** The number of vertices rendered in this frame. */
	public int getVertexCount() {
		return this.totalVertexCount;
	}

	/**
	 * Resets the values in FrameStatistics back to 0.
	 */
	public void reset() {
		this.totalPolygonCount = 0;
		this.totalVertexCount = 0;
		this.totalMeshCount = 0;
		
		this.prepareTime = 0;
		this.renderTime = 0;
		this.waitTime = 0;
	}
	
	/** Set the time (ns) spent prepareing the render passes this frame. */
	public void setPrepareTime(long prepareTime) {
		this.prepareTime = prepareTime;
	}
	
	/** Set the time (ns) spent rendering the scenes this frame. */
	public void setRenderTime(long renderTime) {
		this.renderTime = renderTime;
	}
	
	/** Set the time (ns) spent between this frame and the last. */
	public void setIdleTime(long waitTime) {
		this.waitTime = waitTime;
	}

	/** Set the total number of polygons rendered. Expects value >= 0. */
	public void setPolygonCount(int polyCount) {
		this.totalPolygonCount = polyCount;
	}
	
	/** Set the total number of spatial atoms rendered. Expects value >= 0. */
	public void setMeshCount(int meshCount) {
		this.totalMeshCount = meshCount;
	}
	
	/** Set the total number of vertices rendered. Expects value >= 0. */
	public void setVertexCount(int vertexCount) {
		this.totalVertexCount = vertexCount;
	}
	
	/** Print a formatted block of lines summarizing this FrameStatistics 
	 * object to the given PrintStream. */
	public void reportStatistics(PrintStream out) {
		out.printf("Total Time: %.6f ms (idle: %.6f ms,\n", this.getTotalTime() / 1e6f, this.getIdleTime() / 1e6f);
		out.printf("                     in prepare: %.6f ms,\n", this.getPrepareTime() / 1e6f);
		out.printf("                     in render: %.6f ms)\n", this.getRenderTime() / 1e6f);
		out.printf("Frame/sec: %.4f fps, Poly/sec: %.4f\n", this.getFramesPerSecond(), this.getPolygonsPerSecond());
		out.printf("Mesh count: %d, Polygon count: %d Vertex count: %d\n", this.getMeshCount(), this.getPolygonCount(), this.getVertexCount());
	}
}
