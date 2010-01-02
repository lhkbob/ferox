package com.ferox.util;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class CompressedArray<T> implements Iterable<T> {
	// maximum space between adjacent ids before a new sub-array is needed
	private static final int MAX_ID_SEPARATION = 50;
	
	private final Class<T> type;
	
	private SubArray data;
	
	public CompressedArray(Class<T> type) {
		if (type == null)
			throw new NullPointerException("Class type cannot be null");
		this.type = type;
	}
	
	public T set(int index, T element) {
		if (element == null) {
			return innerRemove(index);
		} else {
			if (!type.isInstance(element))
				throw new ClassCastException("Inappropriate element class type, expected: " + type + ", but was: " + element.getClass());
			return innerSet(index, element);
		}
	}
	
	// assumes that element is not null
	private T innerSet(int index, T element) {
		if (data == null) {
			// this is the first set, so we need a new SubArray
			data = new SubArray(index, element);
			return null;
		} else {
			SubArray prev = null;
			SubArray next = data;
			boolean fitsPrev = false;
			boolean fitsNext = false;
			
			int min, max;
			while(next != null) {
				min = next.linkedHead.index - MAX_ID_SEPARATION;
				max = next.linkedTail.index + MAX_ID_SEPARATION;
				
				if (index >= min && index <= max)
					fitsNext = true;
				
				if (fitsNext && fitsPrev) {
					// merge prev and next together
					SubArray merged = new SubArray(prev, next);
					merged.previous = prev.previous;
					merged.next = next.next;
					
					// splice merged array into list
					if (prev.previous != null)
						prev.previous.next = merged;
					else
						data = merged;
					if (next.next != null)
						next.next.previous = merged;
					
					return merged.set(index, element);
				} else if (!fitsNext && fitsPrev) {
					// add element to prev
					return prev.set(index, element);
				} else {
					if (!fitsNext && next.linkedHead.index > index) {
						// we're in the right spot, but we need a new sub-array
						SubArray newSa = new SubArray(index, element);
						if (prev == null)
							data = newSa;
						else
							prev.next = newSa;
						newSa.previous = prev;
						newSa.next = next;
						next.previous = newSa;
						
						return null;
					}
					
					// otherwise continue to the next iteration
					fitsPrev = fitsNext;
					fitsNext = false;
					
					prev = next;
					next = next.next;
				}
			}
			
			if (fitsPrev) {
				// just stick it into prev
				return prev.set(index, element);
			} else {
				// if we're here, we need a new sub-array at the end
				SubArray newSa = new SubArray(index, element);
				prev.next = newSa;
				newSa.previous = prev;
				return null;
			}
		}
	}
	
	private T innerRemove(int index) {
		T old = null;
		
		SubArray curr = data;
		
		int min, max;
		while(curr != null) {
			min = curr.linkedHead.index;
			max = curr.linkedTail.index;
			
			if (min <= index && max >= index) {
				old = curr.remove(index);
				break;
			}
			curr = curr.next;
		}
		
		return old;
	}
	
	@SuppressWarnings("unchecked")
	public T get(int index) {
		SubArray current = data;
		int max;
		while(current != null) {
			max = current.indexOffset + current.raElements.length;
			if (index >= current.indexOffset && index < max)
				return (T) current.raElements[index - current.indexOffset];
			current = current.next;
		}
		
		return null;
	}

	@Override
	public Iterator<T> iterator() {
		return new CompressedArrayIterator();
	}
	
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		SubArray curr = data;
		b.append('{');
		while(curr != null) {
			b.append(curr.toString());
			curr = curr.next;
			if (curr != null)
				b.append(", ");
		}
		b.append('}');
		return b.toString();
	}
	
	private class SubArray {
		SubArray next;
		SubArray previous;
		
		Object[] raElements;
		Node linkedHead;
		Node linkedTail;
		
		int indexOffset;
		
		SubArray(int index, T element) {
			next = null;
			previous = null;
			
			raElements = new Object[] { element };
			linkedHead = new Node(element, index, null, null);
			linkedTail = linkedHead;
			
			indexOffset = index;
		}
		
		SubArray(SubArray array, Node newTail) {
			next = null;
			previous = null;
			
			linkedHead = array.linkedHead;
			linkedTail = newTail;
			newTail.next = null;
			
			raElements = new Object[linkedTail.index - linkedHead.index + 1];
			System.arraycopy(array.raElements, linkedHead.index - array.indexOffset, raElements, 
							 0, linkedTail.index - linkedHead.index + 1);
			indexOffset = linkedHead.index;
		}
		
		SubArray(Node newHead, SubArray array) {
			next = null;
			previous = null;
			
			newHead.previous = null;
			linkedHead = newHead;
			linkedTail = array.linkedTail;
			
			raElements = new Object[linkedTail.index - linkedHead.index + 1];
			System.arraycopy(array.raElements, linkedHead.index - array.indexOffset, raElements, 
							 0, linkedTail.index - linkedHead.index + 1);
			indexOffset = linkedHead.index;
		}
		
		SubArray(SubArray left, SubArray right) {
			next = null;
			previous = null;
			
			linkedHead = left.linkedHead;
			linkedTail = right.linkedTail;
			
			left.linkedTail.next = right.linkedHead;
			right.linkedHead.previous = left.linkedTail;
			
			raElements = new Object[linkedTail.index - linkedHead.index + 1];
			System.arraycopy(left.raElements, left.linkedHead.index - left.indexOffset, raElements, 
							 0, left.linkedTail.index - left.linkedHead.index + 1);
			System.arraycopy(right.raElements, right.linkedHead.index - right.indexOffset, raElements, 
							 right.linkedHead.index - linkedHead.index, right.linkedTail.index - right.linkedHead.index + 1);
			indexOffset = linkedHead.index;
		}
		
		@Override
		public String toString() {
			StringBuilder b = new StringBuilder();
			b.append('[');
			
			Node curr = linkedHead;
			while(curr != null) {
				b.append(curr.toString());
				curr = curr.next;
				if (curr != null)
					b.append(", ");
			}
			b.append(']');
			return b.toString();
		}
		
		void trim() {
			Object[] newE = new Object[linkedTail.index - linkedHead.index + 1];
			System.arraycopy(raElements, linkedHead.index - indexOffset, newE, 0, newE.length);
			raElements = newE;
			indexOffset = linkedHead.index;
		}
		
		@SuppressWarnings("unchecked")
		T remove(int index) {
			int arrayIndex = index - indexOffset;
			T old = (T) raElements[arrayIndex];
			raElements[arrayIndex] = null;
			
			if (old != null) {
				// we must clean-up linked list, too
				Node curr = linkedHead;
				while(curr != null && curr.index != index) {
					curr = curr.next;
				}
				
				if (curr == null) {
					System.out.println("bad");
				}
				
				if (curr.previous != null)
					curr.previous.next = curr.next;
				if (curr.next != null)
					curr.next.previous = curr.previous;
				
				if (curr == linkedHead && curr == linkedTail) {
					// must get rid of this SubArray since it's now empty
					if (next != null)
						next.previous = previous;
					if (previous != null)
						previous.next = next;
					else
						data = next;
				} else if (curr == linkedHead) {
					// update linkedHead and possibly trim the array
					linkedHead = curr.next;
					if (linkedHead.index - indexOffset > MAX_ID_SEPARATION)
						trim();
				} else if (curr == linkedTail) {
					// updated linkedTail and possibly trim the array
					linkedTail = curr.previous;
					if (raElements.length - linkedTail.index - 1 > MAX_ID_SEPARATION)
						trim();
				} else {
					// check distance between previous and next and 
					// possibly split the sub-array
					if (curr.next.index - curr.previous.index > MAX_ID_SEPARATION) {
						SubArray p = new SubArray(this, curr.previous);
						SubArray n = new SubArray(curr.next, this);
						
						p.previous = previous;
						if (previous != null)
							previous.next = p;
						else
							data = p;
						p.next = n;
						n.previous = p;
						n.next = next;
						if (next != null)
							next.previous = n;
					}
				}
			}
			
			return old;
		}
		
		@SuppressWarnings("unchecked")
		T set(int index, T element) {
			int arrayIndex = index - indexOffset;
			if (arrayIndex < 0) {
				// this element is the new head and we have to grow the array
				Node n = new Node(element, index, null, linkedHead);
				linkedHead.previous = n;
				linkedHead = n;
				
				Object[] newE = new Object[raElements.length - arrayIndex + 1];
				System.arraycopy(raElements, 0, newE, -arrayIndex, raElements.length);
				newE[0] = element;
				
				raElements = newE;
				indexOffset = index;
				return null;
			} else if (arrayIndex >= raElements.length) {
				// this element is at the tail and we have to grow the array
				Node n = new Node(element, index, linkedTail, null);
				linkedTail.next = n;
				linkedTail = n;
				
				raElements = Arrays.copyOf(raElements, arrayIndex + 1);
				raElements[arrayIndex] = element;
				
				return null;
			} else {
				// element can fit within existing array, but we must also insert
				// it into the linked list
				Node curr = linkedHead;
				while(curr != null && curr.index < index) {
					curr = curr.next;
				}
				
				if (curr == null) {
					// we're at the end of the list
					Node n = new Node(element, index, linkedTail, null);
					linkedTail.next = n;
					linkedTail = n;
				} else {
					if (curr.index == index) {
						// re-use existing node
						curr.element = element;
					} else {
						Node n = new Node(element, index, curr.previous, curr);
						if (curr.previous == null)
							linkedHead = n;
						else
							curr.previous.next = n;
						curr.previous = n;
					}
				}
				
				T old = (T) raElements[arrayIndex];
				raElements[arrayIndex] = element;
				return old;
			}
		}
	}
	
	private class Node {
		Node next;
		Node previous;
		
		T element;
		int index;
		
		Node(T data, int index, Node p, Node n) {
			element = data;
			this.index = index;
			
			next = n;
			previous = p;
		}
		
		@Override
		public String toString() {
			return index + ": " + element;
		}
	}
	
	private class CompressedArrayIterator implements Iterator<T> {
		private SubArray currentRange;
		private Node currentNode;
		
		public CompressedArrayIterator() {
			currentRange = null;
			currentNode = null;
		}
		
		@Override
		public boolean hasNext() {
			if (currentRange == null)
				return data != null && data.linkedHead != null;
			else
				return currentNode.next != null || (currentRange.next != null && currentRange.next.linkedHead != null);
		}

		@Override
		public T next() {
			if (!hasNext())
				throw new NoSuchElementException();
			if (currentRange == null) {
				currentRange = data;
				currentNode = data.linkedHead;
			} else {
				currentNode = currentNode.next;
				if (currentNode == null) {
					currentRange = currentRange.next;
					currentNode = currentRange.linkedHead;
				}
			}
			
			return currentNode.element;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
}
