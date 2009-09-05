package com.ferox.scene.octree;

import com.ferox.math.Frustum;
import com.ferox.math.bounds.BoundVolume;
import com.ferox.scene.AbstractCell;
import com.ferox.scene.SceneElement;
import com.ferox.util.Bag;

public class BoundedOctreeCell extends AbstractCell {

	@Override
	public boolean add(SceneElement element) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void clear() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void query(Frustum query, Class<? extends SceneElement> index, Bag<SceneElement> result) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void query(BoundVolume query, Class<? extends SceneElement> index, Bag<SceneElement> result) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void remove(SceneElement element) {
		// TODO Auto-generated method stub
		
	}
}
