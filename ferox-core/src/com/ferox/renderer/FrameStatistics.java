package com.ferox.renderer;


/**
 * Holds timings and counts for a single frame of rendering.
 * 
 * @author Michael Ludwig
 */
public class FrameStatistics {
    private int totalMeshCount;
    private int totalVertexCount;
    private int totalPolygonCount;

    private long renderTime;

    public FrameStatistics() {
        totalMeshCount = 0;
        totalVertexCount = 0;
        totalPolygonCount = 0;
        
        renderTime = 0;
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
     * Set the amount of nanoseconds that this Framework spend rendering This
     * will be the value returned by getRenderTime() and should not be negative.
     * 
     * @param ns The render time, in nanoseconds
     */
    public void setRenderTime(long ns) {
        renderTime = ns;
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
}
