package de.htwk.thread;

import java.util.concurrent.atomic.AtomicMarkableReference;

public class LockFreeList<T> implements Set<T> {
	
	private Node<T> head;

	public LockFreeList() {
		this.head = new Node<>(null, Integer.MIN_VALUE);
		this.head.next = new AtomicMarkableReference<>(new Node<>(null, Integer.MAX_VALUE), false);
		this.head.next.getReference().next = new AtomicMarkableReference<>(this.head, false);
	}

	/** Data class that represents a section of the list, delimited by the nodes {@code previous}
	 * and {@code current}. Objects of this class are returned by the {@code find} method.
	 * 
	 * @author Felix
	 *
	 */
	class Window {
		public final Node<T> previous;
		public final Node<T> current;
		
		Window(Node<T> previous, Node<T> current) {
			this.previous = previous;
			this.current  = current;
		}
	}

	@Override
	public boolean add(T item) {
		int key = Node.getKey(item);

		while (true) {
			Window window = find(this.head, key);

			Node<T> pred = window.previous;
			Node<T> curr = window.current;

			if (curr.key == key) {
				return false;
			} else {
				if (insertNewNodeBetweenGivenNodes(pred, curr, item, key)) {
					return true;
				}
			}
		}
	}
	
	private boolean insertNewNodeBetweenGivenNodes(Node<T> previousNode, Node<T> currentNode,
                                                   T item, int key) {
		Node<T> node = new Node<T>(item, key);
		node.next = new AtomicMarkableReference<Node<T>>(currentNode, false);
		
		return previousNode.next.compareAndSet(currentNode, node, false, false);
	}
	
	public Window find(Node<T> head, int key) {
		Node<T> pred = null;
		Node<T> curr = null;
		Node<T> succ = null;

		boolean[] marked = { false };
		boolean snip = true;
		
		while (true) {
			
			pred = head;
			curr = pred.next.getReference();

			while (true) {
				succ = curr.next.get(marked);

				while (marked[0]) {

					snip = pred.next.compareAndSet(curr, succ, false, false);

					if (snip) {
						curr = succ;
						succ = curr.next.get(marked);
					}
				}
				
				if (snip) {
    				if (curr.key >= key) {
    					return new Window(pred, curr);
    				}
    				
    				pred = curr;
    				curr = succ;
				}
			}
		}
	}
	
	/** Removes the specified item from the list.<br><br>
	 * This method tries to locate {@code item} in the list using the {@code find} method,
	 * which, as a side effect, physically removes all nodes marked as deleted on the way.
	 * If the {@code current} node returned by {@code find} doesn't have a key that matches
	 * that of {@code item}, {@code item} wasn't found in the list, and the method terminates.<br>
	 * Otherwise, the node containing {@code item} is marked as deleted; if this fails
	 * because the list has changed in the meantime, the method starts over, calling {@code find}
	 * again. Finally, one try to physically remove the node is made and the method ends.
	 * 
	 * @param item The item to be removed.
	 * @return {@code true} if the item was successfully removed by the method, {@code false}
	 * if it could not be found in the list.
	 */
	@Override
	public boolean remove(T item) {
		int key = Node.getKey(item);
		
		while (true) {
			Window window = find(this.head, key);
			Node<T> previous = window.previous;
			Node<T> current  = window.current;
			
			if (current.key != key) {
				// a node containing the requested item was not found -> nothing to delete
				return false;
			} else {
				Node<T> next = current.next.getReference();
				
				// attempt to mark the node, making sure the reference to next hasn't changed
				// (otherwise find has to be done anew)
				if (current.next.attemptMark(next, true) == true) {
					// a single attempt to physically remove the node; failure gets ignored,
					// as it will be removed as soon as the find method iterates over it anyway 
					previous.next.compareAndSet(current, next, false, false);
					return true;
				}
			}
		}
	}
	
	@Override
	public boolean contains(T item) {
		boolean[] marked = { false };
		int key = Node.getKey(item);
		Node<T> curr = this.head;

		while (curr.key < key) {
			curr = curr.next.getReference();
			curr.next.get(marked);
		}

		return (curr.key == key && !marked[0]);
	}
	
	/** Returns a String representation of the list.<br><br>
	 * 
	 * This is done by traversing the list from start to end, appending the {@code item}
	 * String representations of any unmarked nodes encountered, separated by a comma, to a
	 * {@code StringBuilder} instance. The resulting String, enclosed in braces, is then returned. 
	 * 
	 * @return String representation of the list.
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("{");
		
		boolean[] marked = { false };
		Node<T> current = this.head;
		
		boolean firstElement = true;
		// iterate over the nodes in the list; abort when sentinel node at the end is reached
		while (current.key < Integer.MAX_VALUE) {
			current = current.next.getReference();
			current.next.get(marked);
			
			// add item to the string representation if node is neither marked nor the tail sentinel
			if (!marked[0] && current.key < Integer.MAX_VALUE) {
				if (!firstElement) builder.append(", ");
				builder.append(current.item.toString());
				firstElement = false;
			}
		}
		builder.append("}");
		
		return builder.toString();
	}
}
