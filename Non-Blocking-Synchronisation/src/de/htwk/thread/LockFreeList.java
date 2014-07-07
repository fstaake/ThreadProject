package de.htwk.thread;

import java.util.concurrent.atomic.AtomicMarkableReference;

public class LockFreeList<T> implements Set<T> {
	
	private Node<T> head;

	public LockFreeList() {
		this.head = new Node<T>(null, Integer.MIN_VALUE);
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

	@Override
	public boolean add(T item) {
		int key = item.hashCode();

		while (true) {
			Window window = find(this.head, key);

			Node<T> pred = window.pred;
			Node<T> curr = window.curr;

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
					pred.next.compareAndSet(curr, succ, false, false);
					return true;
				}
			}
		}
	}

	@Override
	public boolean contains(T item) {
		boolean[] marked = { false };
		int key = item.hashCode();
		Node<T> curr = this.head;

		while (curr.key < key) {
			curr = curr.next.getReference();
			curr.next.get(marked);
		}

		return (curr.key == key && !marked[0]);
	}
		
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("{");
		
		boolean[] marked = { false };
		Node<T> curr = this.head;
		
		boolean firstElement = true;
		while (curr.key < Integer.MAX_VALUE) {
			curr = curr.next.getReference();
			curr.next.get(marked);
			
			if (!marked[0] && curr.key < Integer.MAX_VALUE) {
				if (!firstElement) {
					builder.append(", ");
				}
				
				builder.append(curr.item.toString());
				firstElement = false;
			}
		}
		
		builder.append("}");
		return builder.toString();
	}
}
