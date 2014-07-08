package de.htwk.thread;

import java.util.concurrent.atomic.AtomicMarkableReference;

/**
 * 
 * @author Denny Hecht, Franziska Staake, Felix Hain
 *
 * @param <T>
 */
public class LockFreeList<T> implements Set<T> {

	private Node<T> head;

	/**
	 * Creates a new list containing sentinel nodes but no data.
	 */
	public LockFreeList() {
		// head sentinel
		this.head = new Node<>(null, Integer.MIN_VALUE);
		// tail sentinel
		this.head.next = new AtomicMarkableReference<>(new Node<>(null, Integer.MAX_VALUE), false);
		this.head.next.getReference().next = new AtomicMarkableReference<>(this.head, false);
	}

	/**
	 * Data class that represents a section of the list, delimited by the nodes {@code previous} and {@code current}. Objects of this class are returned by the
	 * {@code find} method.
	 * 
	 * @author Denny Hecht, Franziska Staake, Felix Hain
	 */
	class Window {
		public final Node<T> previous;
		public final Node<T> current;

		Window(Node<T> previous, Node<T> current) {
			this.previous = previous;
			this.current = current;
		}
	}

	/**
	 * Adds a new Node with the given value between the appropriate nodes. Returns false if a node with the given value is already inserted into the set or the
	 * value couldn't be added. Returns true otherwise.
	 * 
	 * @item given value of the node, that should be added
	 * @return States weather the new node could be added.
	 */
	@Override
	public boolean add(T item) {
		int key = Node.createKey(item);

		/*
		 * Searches for the node with a greater value than the given one. If insertion doesn't succeeded caused by an access of another thread, process is tried
		 * again. Loop is terminated by return.
		 */
		while (true) {
			// Searching for the node with a smaller value
			Window window = find(this.head, key);

			Node<T> previous = window.previous;
			Node<T> current = window.current;

			// Given value already exists in set?
			if (current.key == key) {
				// new node can't be inserted
				return false;
			} else {
				// insert a new node and return true, if succeeded
				if (insertNewNodeBetweenGivenNodes(previous, current, item, key)) {
					return true;
				}
			}
		}
	}

	/**
	 * Tries to insert a new node between two appropriate nodes. Creates new node and sets its reference to given current node. After that tries to redirect
	 * reference of previous node from current node to new node. Returns true, if succeeding. Returns false otherwise.
	 * 
	 * @param previous
	 * @param current
	 * @param item
	 *            given value of new node
	 * @param key
	 *            key of given value of new node
	 * @return
	 */
	private boolean insertNewNodeBetweenGivenNodes(Node<T> previous, Node<T> current, T item, int key) {
		Node<T> newNode = new Node<>(item, key);
		newNode.next = new AtomicMarkableReference<Node<T>>(current, false);

		return tryRedirectLinkToNextNode(previous, current, newNode);
	}
	
	/**
	 * Tries to redirect the reference of the previous node to the next node. Returns true, if succeeding. Returns false otherwise.
	 * 
	 * @param previous
	 * @param current
	 * @param next
	 * @return
	 */
	private boolean tryRedirectLinkToNextNode(Node<T> previous, Node<T> current, Node<T> next) {
		return previous.next.compareAndSet(current, next, false, false);
	}

	/**
	 * Returns a pair of nodes containing the previous and next node, that goes with the key. It Removes marked nodes when it founds them.
	 * 
	 * @param head
	 *            node, where traversing begins
	 * @param key
	 *            hash code of item of the node, that should be find
	 * @return
	 */
	private Window find(Node<T> head, int key) {
		Node<T> previous = null;
		Node<T> current = null;
		Window w = null;
		/*
		 * Traverses the list, searching for a previous node, with a key less than given key, and a next node, with a key greater than or equal to given key.
		 */
		while (w == null) {
			previous = head;
			current = previous.next.getReference();

			w = findAppropriateNode(previous, current, key);
		}
		
		return w;
	}

	/**
	 * Searches for the appropriate node with the smallest key greater or equal to the given key. Returns a pair of nodes containing the previous and next node,
	 * that goes with the key. Returns null, if a problem occurs.
	 * 
	 * @param previous
	 * @param current
	 * @param key
	 * @return
	 */
	private Window findAppropriateNode(Node<T> previous, Node<T> current, int key) {
		boolean[] marked = { false };
		boolean deleted;
		Node<T> next = null;
		
		/*
		 * Traverses the list and proves every current node whether its reference is marked or not.
		 */
		while (true) {
			next = current.next.get(marked);

			/*
			 * If reference is marked, it tries to redirect the reference of the previous node to the next node.
			 */
			while (marked[0]) {
				deleted = tryRedirectLinkToNextNode(previous, current, next);

				// If deletion failed, it returns null.
				if (!deleted) {
					return null;
				}

				current = next;
				next = current.next.get(marked);
			}

			/*
			 * If deletion succeeded and the current nodes key is
			 * greater than or equal to the given key, it returns
			 * the founded pair of nodes. 
			 * Otherwise the traversal continues.
			 */
			if (current.key >= key) {
				return new Window(previous, current);
			}

			previous = current;
			current = next;
		}

	}

	/**
	 * Removes the specified item from the list.<br>
	 * <br>
	 * This method tries to locate {@code item} in the list using the {@code find} method, which, as a side effect, physically removes all nodes marked as
	 * deleted on the way. If the {@code current} node returned by {@code find} doesn't have a key that matches that of {@code item}, {@code item} wasn't found
	 * in the list, and the method terminates.<br>
	 * Otherwise, the node containing {@code item} is marked as deleted; if this fails because the list has changed in the meantime, the method starts over,
	 * calling {@code find} again. Finally, one try to physically remove the node is made and the method ends.
	 * 
	 * @param item
	 *            The item to be removed.
	 * @return {@code true} if the item was successfully removed by the method, {@code false} if it could not be found in the list.
	 */
	@Override
	public boolean remove(T item) {
		int key = Node.createKey(item);

		while (true) {
			Window window = find(this.head, key);
			Node<T> previous = window.previous;
			Node<T> current = window.current;

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
					tryRedirectLinkToNextNode(previous, current, next);
					return true;
				}
			}
		}
	}

	/**
	 * Traverses the list, searching for a node with an item equal to the given item. Returns true if the node it was searching for is present and unmarked.
	 * Returns false otherwise.
	 */
	@Override
	public boolean contains(T item) {
		boolean[] marked = { false };
		int key = Node.createKey(item);

		Node<T> current = this.head;

		if (current.key < key) {
			/*
			 * Traversing list while founded items are less than given items
			 */
			while (current.key < key) {
				current = current.next.getReference();
				current.next.get(marked);
			}
		}

		/*
		 * Returns whether the current nodes key is equal to the given key and the current node is not marked.
		 */
		return (current.key == key && !marked[0]);
	}

	/**
	 * prints all nodes of the list, also logically deleted nodes
	 */
	public String printList() {
		StringBuilder builder = new StringBuilder("{");
		AtomicMarkableReference<Node<T>> reference;
		Node<T> current = this.head;

		boolean firstElement = true;
		while (current.key < Integer.MAX_VALUE) {
			current = current.next.getReference();
			reference = current.next;

			if (current.key < Integer.MAX_VALUE) {
				if (!firstElement) {
					builder.append(", ");
				}

				builder.append(current.item.toString());
				builder.append("(");
				builder.append(reference.isMarked());
				builder.append(")");
				firstElement = false;
			}
		}

		builder.append("}");

		return builder.toString();
	}

	/**
	 * Returns a String representation of the list.<br>
	 * <br>
	 * 
	 * This is done by traversing the list from start to end, appending the {@code item} String representations of any unmarked nodes encountered, separated by a
	 * comma, to a {@code StringBuilder} instance. The resulting String, enclosed in braces, is then returned.
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
				if (!firstElement)
					builder.append(", ");
				builder.append(current.item.toString());
				firstElement = false;
			}
		}
		builder.append("}");

		return builder.toString();
	}
}
