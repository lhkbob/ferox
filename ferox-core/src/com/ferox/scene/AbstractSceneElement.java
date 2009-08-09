package com.ferox.scene;

import com.ferox.math.Matrix3f;
import com.ferox.math.ReadOnly;
import com.ferox.math.Transform;
import com.ferox.math.Vector3f;
import com.ferox.math.bounds.BoundVolume;

/**
 * AbstractSceneElement provides a complete implementation of SceneElement so
 * that subclasses can focus on providing meaningful descriptions of what they
 * represent.
 * 
 * @author Michael Ludwig
 */
public abstract class AbstractSceneElement implements SceneElement {
	private UpdateController controller;
	private boolean dirty;
	
	private Cell cell;
	private Scene scene;
	
	protected final Transform worldTransform;
	protected final Transform localTransform;
	
	protected BoundVolume localBounds;
	protected BoundVolume worldBounds;
	
	/**
	 * Will configure the SceneElement to have identity transforms, no bounds,
	 * no UpdateController and is not assigned to any Scene or Cell.
	 */
	public AbstractSceneElement() {
		localTransform = new Transform();
		worldTransform = new Transform();
	}
	
	/**
	 * Convenience function to return the translation vector of the transform
	 * returned by getLocalTransform(). For the same reasons that
	 * getLocalTransform() is read-only, so is the returned vector. Any changes
	 * to it should be followed by a call to setDirty().
	 * 
	 * @return The local translation
	 */
	@ReadOnly
	public Vector3f getTranslation() {
		return localTransform.getTranslation();
	}
	
	/**
	 * Convenience function to return the rotation matrix of the transform
	 * returned by getLocalTransform(). For the same reasons that
	 * getLocalTransform() is read-only, so is the returned matrix. Any changes
	 * to it should be followed by a call to setDirty().
	 * 
	 * @return The local rotation matrix
	 */
	@ReadOnly
	public Matrix3f getRotation() {
		return localTransform.getRotation();
	}
	
	/**
	 * Set the local translation of this SceneElement to the vector <x, y, z>.
	 * This will flag the element as dirty.
	 * 
	 * @param x The new x-coordinate
	 * @param y The new y-coordinate
	 * @param z The new z-coordinate
	 */
	public void setTranslation(float x, float y, float z) {
		localTransform.setTranslation(x, y, z);
		setDirty();
	}
	
	/**
	 * Copy t into the local translation of this SceneElement. If t is null, it
	 * will be set to <0, 0, 0>. This will flag the element as dirty.
	 * 
	 * @param t The new translation vector
	 */
	public void setTranslation(Vector3f t) {
		localTransform.setTranslation(t);
		setDirty();
	}
	
	/**
	 * Copy m into the rotation of the local transform for this SceneElement. If
	 * m is null, then it is set to the identity rotation matrix. This will flag
	 * the element as dirty.
	 * 
	 * @param m The new local rotation
	 */
	public void setRotation(Matrix3f m) {
		localTransform.setRotation(m);
		setDirty();
	}
	
	/**
	 * Set the BoundVolume used to store local bounds. The coordinate system
	 * used for local bounds is the same as the space used for object
	 * coordinates in the geometry of the element. Null may be specified, but it
	 * may limit a Scene's query effectiveness. Calling this method will also
	 * invoke setDirty().
	 * 
	 * @param bounds The BoundVolume instance to use
	 */
	public void setBounds(BoundVolume bounds) {
		localBounds = bounds;
		setDirty();
	}
	
	/**
	 * Copy t into this SceneElement's local or primary transform. If there is
	 * no UpdateController, this will be used as the element's final transform
	 * after an update(). If t is null, the transform will be set to the
	 * identity. Calling this method will also invoke setDirty().
	 * 
	 * @param t The new local transform values
	 */
	public void setTransform(Transform t) {
		localTransform.set(t);
		setDirty();
	}
	
	/**
	 * Set the BoundVolume used to store world bounds. This is intended for use
	 * by UpdateControllers when assigning the world bounds during the update
	 * process. The specified instance will continue to be used if the
	 * UpdateController is removed.
	 * 
	 * @param volume The new BoundVolume instance to use
	 */
	public void setWorldBounds(BoundVolume volume) {
		worldBounds = volume;
	}

	/**
	 * Copy the given transform into this SceneElement's world transform. This
	 * does not mark the SceneElement as dirty and is intended for use by
	 * UpdateControllers to specify the element's world transform after an
	 * update.
	 * 
	 * @param t The new world transform
	 */
	public void setWorldTransform(Transform t) {
		worldTransform.set(t);
	}
	
	@Override
	public void setDirty() {
		dirty = true;
	}
	
	@Override
	public boolean isDirty() {
		return dirty;
	}

	@Override
	public Cell getCell() {
		return cell;
	}

	@Override
	public BoundVolume getLocalBounds() {
		return localBounds;
	}

	@Override
	public Transform getLocalTransform() {
		return localTransform;
	}

	@Override
	public Scene getScene() {
		return scene;
	}

	@Override
	public BoundVolume getWorldBounds() {
		return worldBounds;
	}

	@Override
	public Transform getWorldTransform() {
		return worldTransform;
	}

	@Override
	public void setCell(Cell cell) {
		this.cell = cell;
	}

	@Override
	public void setScene(Scene scene) {
		this.scene = scene;
	}

	@Override
	public boolean update(float timeDelta) {
		boolean reassign = false;
		
		if (controller != null) {
			reassign = dirty || controller.update(this, timeDelta);
		} else if (dirty) {
			worldTransform.set(localTransform);
			reassign = true;
		}
		
		if (reassign) {
			if (localBounds != null)
				worldBounds = localBounds.transform(worldTransform, worldBounds);
			else
				worldBounds = null;
		}
		
		dirty = false;
		return reassign;
	}

	@Override
	public UpdateController getController() {
		return controller;
	}

	@Override
	public void setController(UpdateController controller) {
		this.controller = controller;
	}
}
