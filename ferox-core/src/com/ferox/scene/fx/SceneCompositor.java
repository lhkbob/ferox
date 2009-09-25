package com.ferox.scene.fx;

import com.ferox.renderer.FrameStatistics;
import com.ferox.renderer.Framework;
import com.ferox.renderer.RenderSurface;
import com.ferox.renderer.View;
import com.ferox.scene.Scene;

public interface SceneCompositor {
	public static final int SM_G_SIMPLE = 0x10;
	public static final int SM_G_PERSPECTIVE = 0x20;
	public static final int SM_G_CASCADE = 0x40;
	
	public static final int SM_L_PCF = 0x100;
	public static final int SM_L_VARIANCE = 0x200;
	public static final int SM_L_PCSS = 0x400;
	
	public static final int FS_BLOOM = 0x1000;
	public static final int FS_HDR = 0x2000;
	public static final int FS_MOTION_BLUR = 0x4000;
	public static final int FS_DEPTH_OF_FIELD = 0x8000;
	public static final int FS_AMBIENT_OCCLUSION = 0x10000;
	
	// FIXME: document this:
	// we want clear/compile to use weak references, so that when an Appearance
	// should be removed, it becomes un-compiled
	// we want to fail when attaching destroyed surfaces
	// initialization must require a surface
	// what to do when rendering, and we realize that a surface is destroyed??
	// - we could fail, or detach it
	// - i think we should detach it, but we'll have to make sure that doesn't cause
	// - problems with more advanced compositors which use multiple surfaces
	// what to do when framework is destroyed?
	// - things will throw an exception, we could try to catch it and then rethrow it
	// - i think we should just not catch anything and let it fail
	
	// initialization must check attached surface's to make sure they're compatible
	// with the requested features, if not -> throw an exception

	// FIXME: - this should maintain order for when the surfaces get queued
	public void attach(RenderSurface surface, View view);

	/**
	 * <p>
	 * Initialize the SceneCompositor for use with the given Scene. The
	 * GeometryProfile notifies what geometry attributes are being used by
	 * explicitly by any geometries or the Components in Appearances for the
	 * Scene. Any unused attribute in the GeometryProfile can be used by the
	 * SceneCompositor in an implementation dependent to achieve the correct
	 * results for an Appearance. If geomProfile is null, it is assumed no
	 * attributes are explicitly used.
	 * </p>
	 * <p>
	 * Upon a successful return from this method, the SceneCompositor will be
	 * ready for use. The isInitialized() method must return true and it will be
	 * valid to call compile(), clear() and render(). It is highly likely that
	 * an implementation will have to add RenderPasses to the attached
	 * RenderSurfaces. It is guaranteed that any RenderPasses already manually
	 * added to the RenderSurface will be executed before the compositor's
	 * passes. Any RenderPasses after initialization will be executed after the
	 * compositor's passes.
	 * </p>
	 * <p>
	 * If, at the time of initialization, there are unresolvable problems with
	 * the attached RenderSurfaces, then an exception should be thrown. These
	 * problems should include:
	 * <ul>
	 * <li>No attached RenderSurfaces.</li>
	 * <li>Attached RenderSurfaces that are from different Frameworks.</li>
	 * <li>RenderSurfaces that have been destroyed.</li>
	 * </ul>
	 * With the capBits parameter, it is possible to request a set of features
	 * and visual effects (such as shadow mapping) to be used when rendering the
	 * scene. These capabilities are generally global effects that modify
	 * everything rendered in the scene.A SceneCompositor should strive to
	 * respect the requested capabilities bits, but it is better to ignore
	 * options that are unsupported instead of failing completely.
	 * </p>
	 * <p>
	 * A SceneCompositor can only be initialized once.
	 * </p>
	 * 
	 * @param scene The Scene that will be rendered by this SceneCompositor
	 * @param geomProfile The GeometryProfile describing the configuration of a
	 *            scene's geometry
	 * @param capBits An or'd set of bits that request features and visual
	 *            effects for a SceneCompositor to use when rendering
	 * @throws NullPointerException if scene is null
	 * @throws IllegalStateException if the attached RenderSurfaces are somehow
	 *             invalid, if it's already been initialized, or if it's been
	 *             destroyed
	 */
	public void initialize(Scene scene, GeometryProfile geomProfile, int capBits);
	
	/**
	 * Return true if and only if this SceneCompositor has been successfully
	 * initialized. This should still return false after the compositor has been
	 * destroyed.
	 * 
	 * @return True if the compositor is initialized
	 */
	public boolean isInitialized();
	
	/**
	 * 
	 * @param a
	 */
	public void compile(Appearance a);
	
	public void clean(Appearance a);
	
	public FrameStatistics render(FrameStatistics stats);
	
	public Framework getFramework(); // return null until init
	
	public Scene getScene(); // return null until initialization

	/**
	 * <p>
	 * Destroy and cleanup internal implementation details of this
	 * SceneCompositor. These might include Resource instances (such as
	 * generated GlslPrograms) or additional TextureSurfaces used for special
	 * effects. All RenderPasses that were added to the attached RenderSurfaces
	 * should be removed.
	 * </p>
	 * <p>
	 * Once a SceneCompositor has been destroyed, its isDestroyed() must return
	 * true and isInitialized() should return false again. After destruction a
	 * SceneCompositor cannot be used anymore. It is strongly suggested that all
	 * SceneCompositors are destroyed before they're discarded for garbage
	 * collection.
	 * </p>
	 * <p>
	 * Invoking destroy() will not destroy any of the attached RenderSurfaces or
	 * their associated Framework.
	 * </p>
	 * 
	 * @throws IllegalStateException if this SceneCompositor hasn't been
	 *             initialized yet, or if it's already been destroyed
	 */
	public void destroy();

	/**
	 * Return true if this SceneCompositor has been destroyed via a call to
	 * destroy(). If this returns true, then isInitialized() returns false and
	 * it's invalid to use this SceneCompositor further.
	 * 
	 * @return True if the compositor is destroyed
	 */
	public boolean isDestroyed();
}
