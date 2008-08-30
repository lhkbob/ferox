package com.ferox.core.renderer;

/**
 * Holds statistics for frames and the simulation. 
 * @author Michael Ludwig
 *
 */
public class FrameStatistics {
	private int totalSpatialAtom;
	private int totalVertices;
	private int totalPolygons;
	private long duration;
	
	public FrameStatistics() {
		this.reset();
	}
	
	/**
	 * The total polygons, spatial atoms, and vertices from stat to this.
	 */
	public void add(FrameStatistics stat) {
		this.totalPolygons = stat.totalPolygons;
		this.totalSpatialAtom = stat.totalSpatialAtom;
		this.totalVertices = stat.totalVertices;
	}
	
	/**
	 * Add the given counts of atoms, vertices, and polygons to this's totals.
	 */
	public void add(int atomCount, int vertCount, int polyCount) {
		this.totalPolygons += polyCount;
		this.totalSpatialAtom += atomCount;
		this.totalVertices += vertCount;
	}
	
	/**
	 * The duration in ns of the last frame.
	 */
	public long getDuration() {
		return this.duration;
	}

	/**
	 * The FPS of the last frame. 
	 */
	public float getFPS() {
		return 1e9f / this.getDuration();
	}

	/**
	 * The PPS of the last frame. 
	 */
	public float getPolygonPerSecond() {
		return this.getFPS() * this.totalPolygons;
	}

	/**
	 * The total polygons rendered in the last scene.
	 */
	public int getTotalPolygons() {
		return this.totalPolygons;
	}

	/**
	 * The number of objects rendered in the last scene.
	 */
	public int getTotalSpatialAtom() {
		return this.totalSpatialAtom;
	}

	/**
	 * The number of vertices rendered in the last scene.
	 */
	public int getTotalVertices() {
		return this.totalVertices;
	}

	/**
	 * Resets the values in FrameStatistics back to 0.
	 */
	public void reset() {
		this.totalPolygons = 0;
		this.totalVertices = 0;
		this.totalSpatialAtom = 0;
		this.duration = 0;
	}

	/**
	 * Set the duration (in ns) of the frame.
	 */
	public void setDuration(long duration) {
		this.duration = duration;
	}

	/**
	 * Set the total number of polygons rendered.
	 */
	public void setTotalPolygons(int totalIndices) {
		this.totalPolygons = totalIndices;
	}
	
	/**
	 * Set the total number of spatial atoms rendered.
	 */
	public void setTotalSpatialAtom(int totalSpatialAtom) {
		this.totalSpatialAtom = totalSpatialAtom;
	}
	
	/**
	 * Set the total number of vertices rendered.
	 */
	public void setTotalVertices(int totalVertices) {
		this.totalVertices = totalVertices;
	}
	
	@Override
	public String toString() {
		return "Frame Statistics (Duration: " + (this.getDuration() / 1e9f) + " s, Polygons / sec: " + this.getPolygonPerSecond()
			   + "\nMeshes: " + this.getTotalSpatialAtom() + ", Vertices: " + this.getTotalVertices() + ", Polygons: " + this.getTotalPolygons() + ")";
	}
}
