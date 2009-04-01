package com.ferox.resource.text;

/** RectanglePacker provides a simple algorithm for packing
 * smaller rectangles into a larger rectangle.  The algorithm
 * will expand the containing rectangle's dimensions as necessary
 * to contain new entries.
 * 
 * The algorithm is a java port of the C++ implementation described here:
 * <http://www.blackpawn.com/texts/lightmaps/default.html>
 * 
 * with modifications to allow the top-level rectangle to expand as
 * more items are added in.  Although the entire rectangle will
 * grow, items placed at rectangles will not be moved or resized.
 * 
 * Its primary purpose is to provide Glyph packing for CharacterSets
 * but it could prove useful in other contexts.
 * 
 * @author Michael Ludwig
 *
 */
public class RectanglePacker<T> {
	/** A simple Rectangle class that has no dependencies.
	 * It performs no logic checking, so it is possible for
	 * Rectangles to have negative dimensions, if they
	 * were created as such.
	 * 
	 * Although not publicly modifiable, RectanglePacker
	 * may modify Rectangles that represent the top-level
	 * container. */
	public static class Rectangle {
		private int x, y;
		private int width, height;
		
		public Rectangle(int x, int y, int width, int height) {
			this.x = x;
			this.y = y;
			this.width = width;
			this.height = height;
		}
		
		/** Methods to get the location and dimensions
		 * of the contained rectangle. 
		 * 
		 * The actual rectangle is represented by the 
		 * four vertices created by getX(), getY(),
		 * (getX() + getWidth()), and (getY() + getHeight()). */
		public int getX() { return this.x; }
		public int getY() { return this.y; }
		public int getWidth() { return this.width; }
		public int getHeight() { return this.height; }
	}
	
	// as per the algorithm described above
	private static class Node<T> {
		private Rectangle rc;
		private Node<T> child1;
		private Node<T> child2;
		
		private T data;
		
		public Node<T> get(T data) {
			// check if we match
			if (this.data == data)
				return this;
			
			Node<T> n = null;
			if (!this.isLeaf()) {
				// we're not a leaf, so check children
				n = this.child1.get(data);
				if (n == null)
					n = this.child2.get(data);
			}
			
			return n;
		}
		
		public Node<T> insert(T data, int width, int height) {
			if (!this.isLeaf()) {
				// test first child
				Node<T> n = this.child1.insert(data, width, height);
				if (n == null) // first failed, so check second
					n = this.child2.insert(data, width, height);
				return n;
			} else {
				if (this.data != null) 
					return null; // already filled up
				
				if (this.rc.width < width || this.rc.height < height)
					return null; // we're too small
				
				// check if we fit perfectly
				if (this.rc.width == width && this.rc.height == height)
					return this;
				
				// split this node, to form two children
				this.child1 = new Node<T>();
				this.child2 = new Node<T>();
				
				int dw = this.rc.width - width;
				int dh = this.rc.height - height;
				
				// create rectangles
				if (dw > dh) {
					this.child1.rc = new Rectangle(this.rc.x, this.rc.y,
												   width, this.rc.height);
					this.child2.rc = new Rectangle(this.rc.x + width, this.rc.y,
												   this.rc.width - width, this.rc.height);
				} else {
					this.child1.rc = new Rectangle(this.rc.x, this.rc.y,
												   this.rc.width, height);
					this.child2.rc = new Rectangle(this.rc.x, this.rc.y + height,
												   this.rc.width, this.rc.height - height);
				}
				
				return this.child1.insert(data, width, height);
			}
		}
		
		public boolean isLeaf() {
			return this.child1 == null && this.child2 == null;
		}
	}
	
	private Node<T> root;
	
	/** Create a RectanglePacker with the initial sizes for the
	 * top-level container rectangle.
	 * 
	 * Throws an IllegalArgumentException if they aren't positive. */
	public RectanglePacker(int startWidth, int startHeight) throws IllegalArgumentException {
		if (startWidth <= 0 || startHeight <= 0)
			throw new IllegalArgumentException("Starting dimensions must be positive: " + startWidth + " " + startHeight);
		
		Rectangle rootBounds = new Rectangle(0, 0, startWidth, startHeight);
		this.root = new Node<T>();
		this.root.rc = rootBounds;
	}
	
	/** Return the current width of the top-level container. */
	public int getWidth() {
		return this.root.rc.width;
	}
	
	/** Return the current height of the top-level container. */
	public int getHeight() {
		return this.root.rc.height;
	}
	
	/** Return the Rectangle that was previously returned
	 * by a call to insert(data) for this instance.
	 * If data has been inserted more than once, it is
	 * undefined which Rectangle will be returned, since
	 * it is still technically stored in multiple places.
	 * 
	 * Returns null if data is null, or if there is no
	 * Rectangle associated with it. */
	public Rectangle get(T data) {
		if (data == null)
			return null;
		
		Node<T> n = this.root.get(data);
		return (n == null ? null : n.rc);
	}
	
	/** Insert the given object, that requires a rectangle
	 * with the given dimensions.  The containing rectangle
	 * will be enlarged if necessary to contain it.
	 * 
	 * This does nothing if data is null.  If data has already
	 * been inserted, then this packer will contain multiple
	 * rectangles referencing the given object.  This results
	 * in undefined behavior with get(data) and should be avoided
	 * if get() is necessary. 
	 * 
	 * If the dimensions are not > 0, then an IllegalArgumentException
	 * is thrown. */
	public Rectangle insert(T data, int width, int height) throws IllegalArgumentException {
		if (width <= 0 || height <= 0)
			throw new IllegalArgumentException("Dimensions must be > 0, " + width + "x" + height);
		if (data == null)
			return null;
		
		Node<T> n = null;
		while((n = this.root.insert(data, width, height)) == null) {
			// we must expand it, choose the option that keeps
			// the dimension smallest
			if (this.root.rc.width + width <= this.root.rc.height + height)
				this.expandWidth(width);
			else
				this.expandHeight(height);
		}
			
		// assign the data and return
		n.data = data;
		return n.rc;
	}
	
	// internal method to expand the width of the top-level
	// container by dw.
	private void expandWidth(int dw) {
		Rectangle oldBounds = this.root.rc;
		
		int newW = oldBounds.width + dw;

		if (this.root.isLeaf() && this.root.data == null) {
			// just expand the rectangle
			this.root.rc.width = newW;
		} else {
			// create a new root node
			Node<T> n = new Node<T>();
			n.rc = new Rectangle(0, 0, newW, oldBounds.height);

			n.child1 = this.root; // first child is old root
			n.child2 = new Node<T>(); // second child is leaf with left-over space
			n.child2.rc = new Rectangle(oldBounds.width, 0, newW - oldBounds.width, oldBounds.height);

			this.root = n;
		}
	}

	// internal method to expand the height of the top-level
	// container by dh.
	private void expandHeight(int dh) {
		Rectangle oldBounds = this.root.rc;
		
		int newH = oldBounds.height + dh;
		
		if (this.root.isLeaf() && this.root.data == null) {
			// just expand the rectangle
			this.root.rc.height = newH;
		} else {
			// create a new root node
			Node<T> n = new Node<T>();
			n.rc = new Rectangle(0, 0, oldBounds.width, newH);

			n.child1 = this.root; // first child is old root
			n.child2 = new Node<T>(); // second child is leaf with left-over space
			n.child2.rc = new Rectangle(0, oldBounds.height, oldBounds.width, newH - oldBounds.height);

			this.root = n;
		}
	}
}
