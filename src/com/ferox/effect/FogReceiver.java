package com.ferox.effect;

/**
 * A FogReceiver is a state that allows Fog states to be applied to scene
 * elements. If it's not present on a scene element that intersects a fog node
 * (or has a fog in it's appearance), then the fog won't have any visual affect
 * until a FogReceiver is added to the appearance, too.
 * 
 * @author Michael Ludwig
 * 
 */
public class FogReceiver extends AbstractEffect {
	/**
	 * Represents the two options for where a fog gets a pixel's effective
	 * eye-space depth.
	 */
	public static enum FogCoordSource {
		FRAGMENT_DEPTH, /**
		 * Fog depth is taken to be the depth of the current
		 * pixel.
		 */
		FOG_COORDINATE
		/**
		 * Fog depth is taken to be the fog coordinate value supplied by the
		 * rendered geometry.
		 */
	}

	private FogCoordSource fogSource;

	/**
	 * Create a fog receiver with a fog coordinate source of FRAGMENT_DEPTH.
	 */
	public FogReceiver() {
		setFogCoordinateSource(null);
	}

	/**
	 * Return the fog coordinate source that is used by any rendered object
	 * affected by fog.
	 * 
	 * @return Current fog coord source
	 */
	public FogCoordSource getFogCoordinateSource() {
		return fogSource;
	}

	/**
	 * Set the fog coordinate source that is used for a rendered object with
	 * this FogReceiver, when it is affected by fog.
	 * 
	 * If null is given, it defaults to FRAGMENT_DEPTH. FOG_COORDINATE should
	 * only be used if the rendered object has fog coordinates to supply.
	 * 
	 * @param fogSource New fog coord source
	 */
	public void setFogCoordinateSource(FogCoordSource fogSource) {
		if (fogSource == null) {
			fogSource = FogCoordSource.FRAGMENT_DEPTH;
		}
		this.fogSource = fogSource;
	}

	@Override
	public String toString() {
		return "(FogReceiver coord: " + fogSource + ")";
	}
}
