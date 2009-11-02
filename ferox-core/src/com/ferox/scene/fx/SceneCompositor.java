package com.ferox.scene.fx;

import com.ferox.renderer.Framework;
import com.ferox.renderer.RenderSurface;
import com.ferox.scene.Scene;
import com.ferox.shader.View;

public interface SceneCompositor {
	// attach a surface to this SC, can happen any time
	// if view == null, same as detach
	// else overwrites any old one
	public void attach(RenderSurface surface, View view);
	
	public void detach(RenderSurface surface);
	
	public boolean isAttached(RenderSurface surface);
	
	// cache a compiled version of a for later use, will not change
	// FIXME: how to resolve vector changes, etc. that are fast w/o changing struct of appearance
	public void compile(Appearance a);
	
	// cleans up compiled version, must be explicit
	public void clean(Appearance a);

	/**
	 * Queue all currently attached RenderSurfaces, in the order that they were
	 * attached. This has the same effect as queue(s) for each RenderSurface
	 * that was passed to attach(). Any attached RenderSurface that has been
	 * destroyed will be implicitly detached.
	 */
	public void queueAll();

	/**
	 * <p>
	 * Queue up the specified attached RenderSurface so that the next time this
	 * SceneCompositor's Framework is rendered, all necessary RenderPasses will
	 * be rendered to draw this SceneCompositor's Scene into s. The scene will
	 * be rendered from the View that was specified when s was originally
	 * attached.
	 * </p>
	 * <p>
	 * In addition to queuing RenderPass's specific to the surface s,
	 * implementations may also queue passes associated with internal
	 * RenderSurfaces needed to achieve special effects for the final render.
	 * One such example of this is filling a shadow map.
	 * </p>
	 * <p>
	 * If s has been destroyed, it will be implicitly detached from this
	 * SceneCompositor and this queue will be ignored. If s is null or not
	 * attached, this queue request is ignored.
	 * </p>
	 */
	public void queue(RenderSurface s);

	/**
	 * <p>
	 * Return the Framework that is used by the SceneCompositor. Any attached
	 * RenderSurfaces must have been created by this Framework.
	 * </p>
	 * <p>
	 * This must return null if the SceneCompositor is destroyed, otherwise it
	 * must be non-null. It is recommended that SceneCompositor implementations
	 * take a Framework as a constructor argument.
	 * </p>
	 * 
	 * @return The Framework used by this SceneCompositor, null if destroyed
	 */
	public Framework getFramework();

	/**
	 * <p>
	 * Return the Scene instance that will be used as a source for content by
	 * the SceneCompositor implementation when rendering. Each SceneCompositor
	 * will render the Scene in a potentially unique manner, and may not be able
	 * to be fully faithful to the scene's description.
	 * </p>
	 * <p>
	 * Like getFramework(), this should only return null when the
	 * SceneCompositor is destroyed. The Scene should also be part a constructor
	 * argument. Implementations may allow the scene to be changed at a later
	 * point.
	 * </p>
	 * 
	 * @return The Scene that's to be rendered, null if destroyed
	 */
	public Scene getScene();

	/**
	 * <p>
	 * Destroy the SceneCompositor so that it is no longer usable and no longer
	 * keeps a hold on its internal resources. Destroying a SceneCompositor will
	 * detach any attached RenderSurfaces, clean all currently compiled
	 * Appearances and clean-up any other resources used internally by the
	 * SceneCompositor.
	 * </p>
	 * <p>
	 * Successfully destroying the SceneCompositor will likely require the
	 * Framework to be usable, so the Framework should not be destroyed until
	 * after the SceneCompositor has been. After destruction, many methods will
	 * throw exceptions or change their return values.
	 * </p>
	 * 
	 * @see #isDestroyed()
	 */
	public void destroy();

	/**
	 * Return true if destroy() has been called, at which point the
	 * SceneCompositor becomes unusable. If isDestroyed() returns true, then
	 * calling the following methods will throw exceptions:
	 * <ul>
	 * <li>{@link #attach(RenderSurface, View)}</li>
	 * <li>{@link #clean(Appearance)}</li>
	 * <li>{@link #compile(Appearance)}</li>
	 * <li>{@link #detach(RenderSurface)}</li>
	 * <li>{@link #destroy()}</li>
	 * <li>{@link #queue(RenderSurface)}</li>
	 * <li>{@link #queueAll()}</li>
	 * </ul>
	 * The remaining methods may return different values if the SceneCompositor
	 * is destroyed.
	 * 
	 * @see #destroy()
	 * @return True if the SceneCompositor is destroyed and unusable, false
	 *         otherwise
	 */
	public boolean isDestroyed();
}
