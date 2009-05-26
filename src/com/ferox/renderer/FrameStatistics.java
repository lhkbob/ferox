package com.ferox.renderer;

import java.io.PrintStream;

/**
 * Holds timings and counts for a single frame of rendering.
 * 
 * @author Michael Ludwig
 */
public class FrameStatistics {
	private int totalMeshCount;
	private int totalVertexCount;
	private int totalPolygonCount;

	private long waitTime;
	private long prepareTime;
	private long renderTime;

	public FrameStatistics() {
		reset();
	}

	/**
	 * Add the given counts of meshes, vertices, and polygons to this
	 * FrameStatistic's totals. Expects values > 0 (inaccurate/misleading
	 * results if not positive).
	 * 
	 * @param meshCount The number of meshes to increase counts by
	 * @param vertCount The number of vertices to increase counts by
	 * @param polyCount The number of polygons to increase counts by
	 */
	public void add(int meshCount, int vertCount, int polyCount) {
		totalPolygonCount += polyCount;
		totalMeshCount += meshCount;
		totalVertexCount += vertCount;
	}

	/**
	 * Set the amount of nanoseconds that this Framework has been idle. This will
	 * be the value returned by getIdleTime() and should not be negative.
	 * 
	 * @param ns The idle time, in nanoseconds
	 */
	public void setIdleTime(long ns) {
		waitTime = ns;
	}

	/**
	 * Set the amount of nanoseconds that this Framework has been spent preparing
	 * for rendering. This will be the value returned by getPrepareTime() and
	 * should not be negative.
	 * 
	 * @param ns The prepare time, in nanoseconds
	 */
	public void setPrepareTime(long ns) {
		prepareTime = ns;
	}

	/**
	 * Set the amount of nanoseconds that this Framework spend rendering This
	 * will be the value returned by getRenderTime() and should not be negative.
	 * 
	 * @param ns The render time, in nanoseconds
	 */
	public void setRenderTime(long ns) {
		renderTime = ns;
	}

	/**
	 * The total duration (ns) of this frame.
	 * 
	 * @return Total frame time in ns
	 */
	public long getFrameTime() {
		return prepareTime + renderTime;
	}

	/**
	 * Get the total time from the end of last frame to the end of this frame.
	 * 
	 * @return Total time in ns
	 */
	public long getTotalTime() {
		return getFrameTime() + waitTime;
	}

	/**
	 * The total time (ns) spent preparing the render passes.
	 * 
	 * @return Time spent preparing render passes in ns
	 */
	public long getPrepareTime() {
		return prepareTime;
	}

	/**
	 * The total time (ns) spent rendering the previously updated/visited
	 * scenes.
	 * 
	 * @return Time spent flusing render queues in ns
	 */
	public long getRenderTime() {
		return renderTime;
	}

	/**
	 * The time (ns) spent outside of the Framework, e.g. time spent between
	 * frames.
	 * 
	 * @return Time spent between end of the last frame and before this frame
	 *         started
	 */
	public long getIdleTime() {
		return waitTime;
	}

	/**
	 * The frames/sec of this frame, including time spent external to rendering.
	 * 
	 * @return The fps at this instance in time
	 */
	public float getFramesPerSecond() {
		return 1e9f / getTotalTime();
	}

	/**
	 * The polygon/sec of this frame, including time spent external to
	 * rendering.
	 * 
	 * @return The polys/s at this instance in time
	 */
	public float getPolygonsPerSecond() {
		return getFramesPerSecond() * totalPolygonCount;
	}

	/**
	 * The total number of polygons rendered in this frame.
	 * 
	 * @return Number of polygons rendered
	 */
	public int getPolygonCount() {
		return totalPolygonCount;
	}

	/**
	 * The number of objects rendered in this frame.
	 * 
	 * @return Number of meshes rendered
	 */
	public int getMeshCount() {
		return totalMeshCount;
	}

	/**
	 * The number of vertices rendered in this frame.
	 * 
	 * @return Number of vertices rendered
	 */
	public int getVertexCount() {
		return totalVertexCount;
	}

	/**
	 * Resets the values in FrameStatistics back to 0.
	 */
	public void reset() {
		totalPolygonCount = 0;
		totalVertexCount = 0;
		totalMeshCount = 0;

		prepareTime = 0;
		renderTime = 0;
		waitTime = 0;
	}

	/**
	 * Print a formatted block of lines summarizing this FrameStatistics object
	 * to the given PrintStream.
	 * 
	 * @param out The PrintStream that will display the stats summary
	 * @throws NullPointerException if out is null
	 */
	public void reportStatistics(PrintStream out) {
		out.printf("Total Time: %.6f ms (idle: %.6f ms,\n",
			getTotalTime() / 1e6f, getIdleTime() / 1e6f);
		out.printf("                     in prepare: %.6f ms,\n",
			getPrepareTime() / 1e6f);
		out.printf("                     in render: %.6f ms)\n",
			getRenderTime() / 1e6f);
		out.printf("Frame/sec: %.4f fps, Poly/sec: %.4f\n",
			getFramesPerSecond(), getPolygonsPerSecond());
		out.printf("Mesh count: %d, Polygon count: %d Vertex count: %d\n",
			getMeshCount(), getPolygonCount(), getVertexCount());
	}
}
