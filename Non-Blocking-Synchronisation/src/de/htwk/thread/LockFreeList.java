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
	 * Constructor
	 */
	public LockFreeList() {
		// head sentinel
		this.head = new Node<>(null, Integer.MIN_VALUE);
		// tail sentinel
		this.head.next = new AtomicMarkableReference<>(new Node<>(null, Integer.MAX_VALUE), false);
		this.head.next.getReference().next = new AtomicMarkableReference<>(this.head, false);
	}

	/** Data class that represents a section of the list, delimited by the nodes {@code previous}
	 * and {@code current}. Objects of this class are returned by the {@code find} method.
	 * 
	 * @author Felix
	 */
	class Window {
		public final Node<T> previous;
		public final Node<T> current;
		
		Window(Node<T> previous, Node<T> current) {
			this.previous = previous;
			this.current  = current;
		}
	}

	/**
	 * Adds a new Node with the given value between the 
	 * appropriate nodes.
	 * Returns false if a node with the given value is already 
	 * inserted into the set or the value couldn't be added.
	 * Otherwise returns true. 
	 * 
	 * @item	
	 * 		given value of the node, that should be added
	 * @return
	 * 		States weather the new node could be added.
	 */
	@Override
	public boolean add(T item) {
		int key = Node.createKey(item);
		
		/**
		 * Searches for the node with a greater value
		 * than the given one.
		 * If insertion doesn't succeeded caused by an access
		 * of another thread, process is tried again.
		 * Loop is terminated by return. 
		 */
		while (true) {
			//Searching for the node with a smaller value
			Window window = find(this.head, key);
			
			Node<T> previous = window.previous;
			Node<T> current  = window.current;
			
			//Given value already exists in set?
			if (current.key == key) {
				//new node can't be inserted
				return false;
			} else {
				//insert a new node and return true, if succeeded
				if (insertNewNodeBetweenGivenNodes(previous, current, item, key)) {
					return true;
				}
			}
		}
	}
	
	/**
	 * Tries to insert a new node between two appropriate nodes.
	 * @param previous
	 * @param current
	 * @param item
	 * 			given value of new node
	 * @param key
	 * 			key of given value of new node
	 * @return
	 */
	private boolean insertNewNodeBetweenGivenNodes(Node<T> previous, Node<T> current, T item, int key) {
		Node<T> node = new Node<>(item, key);
		node.next = new AtomicMarkableReference<Node<T>>(current, false);
		
		/**
		 * "Atomically sets the value of both the reference and 
		 * mark to the given update values if the current 
		 * reference is == to the expected reference and the 
		 * current mark is equal to the expected mark."
		 * Returns true if succeeded, otherwise false.
		 */
		return tryRedirectLinkToNextNode(previous, current, node);
	}
	
	private boolean tryRedirectLinkToNextNode(Node<T> previous, Node<T> current, Node<T> next) {
		/**
		 * "call[ing] compareAndSet() to attempt to physically 
		 * remove the node by setting [...] 
		 * next field [of the previous node] to 
		 * [the current nodes] next field."
		 */
		return previous.next.compareAndSet(current, next, false, false);
	}

	/**
	 * "Returns a structure containing the nodes on 
	 * either side of the key. 
	 * It Removes marked nodes when it encounters them."
	 *  
	 * @param head
	 * 			node, where the traverses begins
	 * @param key
	 * 			hashcode of value of the node, that should be find
	 * @return
	 */
	private Window find(Node<T> head, int key) {
		Node<T> previous = null;
		Node<T> current  = null;
		
		/**
		 * "Traverses the list, seeking to set the previous node to the 
		 * node with the largest key less than [the given key], and the current node
		 * to the node with the least key greater than or equal to [the given key]."
		 */
		while (true) {
			previous = head;
			current = previous.next.getReference();

			Window w = findAppropriateNode(previous, current, key);
				
			if(w != null) {
				return w;
			}
		}
	}
	
	/**
	 * 
	 * @param previous
	 * @param current
	 * @param key
	 * @return
	 */
	private Window findAppropriateNode (Node<T> previous, Node<T> current, int key) {
		boolean[] marked = {false};
		boolean deleted;	
		Node<T> next = null;
		
		/**
		 * traverses the set and proves...
		 */
		while (true) {
			next = current.next.get(marked);
			
			/** 
			 * "If [current node is marked], it 
			 * [...] tr[ies] to redirect the next field 
			 * [of the previous node]" to the next node.
			 */
			while (marked[0]) {
				deleted = tryRedirectLinkToNextNode(previous, current, next);
				
				if (!deleted) {
					return null;
				}

				current = next;
				next = current.next.get(marked);
			}

			/**
			 * If the deletion succeeded and the current nodes key is
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
		int key = Node.createKey(item);
		
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
					tryRedirectLinkToNextNode(previous, current, next);
					return true;
				}
			}
		}
	}
	
	/**
	 * Traverses the list once [...] and
	 * returns true if the node it was searching 
	 * for is present and unmarked, and false otherwise.  
	 */
	@Override
	public boolean contains(T item) {
		boolean[] marked = { false };
		int key = Node.createKey(item);
		
		Node<T> current = this.head;
		
		if(current.key < key){
			traversingSetForKey(current, key, marked);
		}

		/**
		 * Returns whether the current nodes key is equal
		 * to the given key and the current node is not marked.
		 */		
		return (current.key == key && !marked[0]);
	}
	
	
	/**
	 * Traversal moves to the next node while  
	 * next nodes key is less then given key.
	 * While traversing it tests whether current 
	 * node is marked.
	 */
	private void traversingSetForKey(Node<T> current, int key, boolean[] marked) {
		while (current.key < key) {
			current = current.next.getReference();
			current.next.get(marked);
		}		
	}

	/**
	 * 
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
