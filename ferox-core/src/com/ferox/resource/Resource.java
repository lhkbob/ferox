package com.ferox.resource;

/**
 * <p>
 * An abstract class that represents some type of data stored on the graphics
 * card. A resource is fairly abstract so there many things can be represented
 * (assuming there are hardware capabilities supporting it). Some examples
 * include Texture, Geometry, and GlslProgram.
 * </p>
 * <p>
 * There are multiple ways that a Resource can be managed with a Framework. A
 * Resource cannot be used until its been updated by a Framework. There are
 * multiple ways that a Resource can be updated, some of which are automatic:
 * <ol>
 * <li>Implement some manager to call update() and dispose() with the necessary
 * Resources, and monitor the returned Futures</li>
 * <li>Use a Framework's update() and dispose() methods and trust that they
 * complete as needed (per their contract).</li>
 * <li>Rely on the Framework automatically updating a Resource if it's never
 * seen the Resource before, or if the Resource has a non-null dirty descriptor</li>
 * </ol>
 * When a Resource can only be accessed by weak references, a Framework will
 * automatically schedule it for disposal. A Framework that's destroyed will
 * have any remaining Resource's internal data disposed, too.
 * </p>
 * <p>
 * It is not required that Resource implementations be thread-safe. Instead,
 * each Thread that intends to modify or use the Resource (e.g. worker threads
 * of a Framework) must synchronize on the Resource instance. To prevent
 * deadlocks, each Thread should only acquire one lock at a time.
 * </p>
 * 
 * @author Michael Ludwig
 */
// FIXME: thread safety of resource impl's should be improved, simple functions, etc should
// have locking present since it won't add overhead -> but we then must just document special
// case for when long-term lock must be acquired
public abstract class Resource {
	/**
	 * Each resource will have a status with the active renderer. A Resource is
	 * usable if it has a status of READY. Resources that are DISPOSED will be
	 * auto-updated when used. A Resource that has a status of ERROR is unusable
	 * until it's been repaired.
	 */
	public static enum Status {
		/** The resource has been updated successfully and is ready to use. */
		READY,
		/**
		 * The Framework has tried to update the resource and there may be
		 * internal data for the Resource, but something is wrong and the
		 * Resource isn't usable.
		 */
		ERROR,
		/**
		 * The Framework has no support for the Resource sub-class. Like ERROR
		 * it means the Resource is unusable. Unlike ERROR, the Resource cannot
		 * be used without an exception being thrown, and it's impossible to
		 * modify the Resource to change this status.
		 */
		UNSUPPORTED,
        /**
         * The Framework's connection to the graphics card was lost and the
         * Resource's internal data has been invalidated. The Framework will
         * automatically change the Resource's status back to READY when
         * possible.
         */
		DISCONNECTED,
		/**
		 * The Framework has no internal representations of the Resource (never
		 * updated, or it's been disposed).
		 */
		DISPOSED
	}
	
	private static int resourceId = 0;
	private static final Object ID_LOCK = new Object();
	
	private final int id;
	
	public Resource() {
		synchronized(ID_LOCK) {
			id = resourceId++;
		}
	}

	/**
	 * Return a unique numeric id that's assigned to this Resource instance.
	 * Each instantiated Resource is assigned an id, starting at 0, which is
	 * valid only for the lifetime of the current JVM.
	 * 
	 * @return This Resource's unique id
	 */
	public final int getId() {
		return id;
	}

	/**
	 * <p>
	 * Return an object that describes what regions of the Resource are dirty.
	 * When this returns a non-null instance, and the Resource is used in a
	 * frame, then the Framework should automatically update the Resource based
	 * on the returned dirty state. If null is returned, then this Resource
	 * has not be modified (or marked as modified).
	 * </p>
	 * <p>
	 * Implementations should document what type of object is returned, and
	 * override the return type. The returned dirty state must be an
	 * immutable object. Every time the dirty state must be expanded to
	 * represent more state, a new instance should be created that has a
	 * superset of the dirty attributes of the previous instance.
	 * </p>
	 * <p>
	 * Because there is only one dirty state per Resource, care must be
	 * given when using multiple Frameworks at the same time.
	 * </p>
	 * <p>
	 * The state is the minimal set of values needed to be updated.
	 * Frameworks should not update less than what is described by the object.
	 * If a Resource is manually updated and it's state is null, the entire
	 * Resource should be updated, for lack of a better alternative.
	 * </p>
	 * <p>
	 * Invoking this method resets the dirty state of a Resource, so a second
	 * call to this method will return null, until the Resource has again
	 * been flagged as dirty. Because of this, this should only be called by
	 * Framework implementations at the appropriate time to look up the dirty
	 * state.
	 * </p>
	 * 
	 * @return Implementations specific object describing what parts of the
	 *         Resource are dirty
	 */
	public abstract DirtyState<?> getDirtyState();
}
