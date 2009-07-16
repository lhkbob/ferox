package com.ferox.math.bounds;

import com.ferox.math.Transform;

/**
 * Provides implementations of applyTransform(trans, result), enclose(bv,
 * result), enclose(va, result) so that subclasses have to implement three less
 * methods.
 * 
 * @author Michael Ludwig
 */
public abstract class AbstractBoundVolume implements BoundVolume {

	@Override
	public BoundVolume transform(Transform trans, BoundVolume result) {
		result = this.clone(result);
		result.transform(trans);
		return result;
	}

	@Override
	public BoundVolume enclose(BoundVolume toEnclose, BoundVolume result) {
		result = this.clone(result);
		result.enclose(toEnclose);
		return result;
	}
}
