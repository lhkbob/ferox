package com.ferox.resource.text;

public class RectanglePacker<T> {
	public static class Rectangle {
		private int x, y;
		private int width, height;
		
		public Rectangle(int x, int y, int width, int height) {
			this.x = x;
			this.y = y;
			this.width = width;
			this.height = height;
		}
		
		public int getX() { return this.x; }
		public int getY() { return this.y; }
		public int getWidth() { return this.width; }
		public int getHeight() { return this.height; }
	}
	
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
	
	public RectanglePacker(int startWidth, int startHeight) {
		if (startWidth <= 0 || startHeight <= 0)
			throw new IllegalArgumentException("Starting dimensions must be positive: " + startWidth + " " + startHeight);
		startWidth = ceilPot(startWidth);
		startHeight = ceilPot(startHeight);
		
		Rectangle rootBounds = new Rectangle(0, 0, startWidth, startHeight);
		this.root = new Node<T>();
		this.root.rc = rootBounds;
	}
	
	public int getWidth() {
		return this.root.rc.width;
	}
	
	public int getHeight() {
		return this.root.rc.height;
	}
	
	public Rectangle get(T data) {
		Node<T> n = this.root.get(data);
		return (n == null ? null : n.rc);
	}
	
	public Rectangle insert(T data, int width, int height) {
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
	
	private void expandWidth(int dw) {
		Rectangle oldBounds = this.root.rc;
		
		int newW = oldBounds.width + dw;
		newW = ceilPot(newW);

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
	
	private void expandHeight(int dh) {
		Rectangle oldBounds = this.root.rc;
		
		int newH = oldBounds.height + dh;
		newH = ceilPot(newH);
		
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
	
	// Return smallest POT >= num
	private static int ceilPot(int num) {
		int pot = 1;
		while(pot < num)
			pot = pot << 1;
		return pot;
	}
}
