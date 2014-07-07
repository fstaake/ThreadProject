package de.htwk.thread;

import java.util.concurrent.atomic.AtomicMarkableReference;

public class LockFreeList<T> implements Set<T> {
	
	private Node<T> head;

	/**
	 * Constructor
	 */
	public LockFreeList() {
		//head
		this.head = new Node<T>(null, Integer.MIN_VALUE);
		//guard
		this.head.next = new AtomicMarkableReference<>(new Node<>(null, Integer.MAX_VALUE), false);
		this.head.next.getReference().next = new AtomicMarkableReference<>(this.head, false);
	}

	class Window {
		
		public Node<T> pred;
		public Node<T> curr;

		Window(Node<T> pred, Node<T> curr) {
			this.pred = pred;
			this.curr = curr;
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
		int key = item.hashCode();

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

			Node<T> previous = window.pred;
			Node<T> current = window.curr;

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
	 * 			hashcode of given value of new node
	 * @return
	 */
	private boolean insertNewNodeBetweenGivenNodes(Node<T> previous, Node<T> current,
                                                   T item, int key) {
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
	
	private boolean tryRedirectLinkToNextNode(Node<T> previous, Node<T> current,
			Node<T> next) {
		
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
		Node<T> current = null;

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
		 * traverses the set and proofs ....
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

	@Override
	public boolean remove(T item) {
		int key = item.hashCode();
		boolean snip;

		while (true) {
			Window window = find(this.head, key);
			Node<T> pred = window.pred;
			Node<T> curr = window.curr;

			if (curr.key != key) {
				return false;
			} else {
				Node<T> succ = curr.next.getReference();
				snip = curr.next.attemptMark(succ, true);

				if (snip) {
					tryRedirectLinkToNextNode(pred, curr, succ);
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
		int key = item.hashCode();
		
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
		Node<T> curr = this.head;
		
		boolean firstElement = true;
		while (curr.key < Integer.MAX_VALUE) {
			curr = curr.next.getReference();
			reference = curr.next;
			
			if (curr.key < Integer.MAX_VALUE) {
				if (!firstElement) {
					builder.append(", ");
				}
				
				builder.append(curr.item.toString());
				builder.append("(");
				builder.append(reference.isMarked());
				builder.append(")");
				firstElement = false;
			}
		}
		
		builder.append("}");
		return builder.toString();
	}
}
