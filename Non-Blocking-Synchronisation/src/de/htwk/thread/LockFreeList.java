package de.htwk.thread;

import java.util.concurrent.atomic.AtomicMarkableReference;

public class LockFreeList<T> implements Set<T> {
	private Node<T> head;
	
	
	@Override
	public boolean add(T item) {
		
		int key = item.hashCode();
		Node<T> head = null;

		while (true) {
			Window window = find(head, key);

			Node<T> pred = window.pred, curr = window.curr;

			if (curr.key == key) {
				return false;
			} else {
				Node<T> node = new Node<T>(item);

				node.next = new AtomicMarkableReference<Node<T>>(curr, false);

				if (pred.next.compareAndSet(curr, node, false, false)) {
					return true;
				}
			}
		}
	}
	
	public Window find(Node<T> head, int key) {
		
		Node<T> pred = null, curr = null, succ = null;

		boolean[] marked = {false};
		boolean snip;

		retry: while (true) {

			pred = head;
			curr = pred.next.getReference();

			while (true) {

				succ = curr.next.get(marked);

				while (marked[0]) {

					snip = pred.next.compareAndSet(curr, succ, false, false);

					if (!snip){
						continue retry;
					}

					curr = succ;
					succ = curr.next.get(marked);
				}

				if (curr.key >= key){

					return new Window(pred, curr);
				}

				pred = curr;
				curr = succ;
			}
		}
	}
	

	@Override
	public boolean remove(T item) {
		// TODO Auto-generated method stub
		return false;
	}
	
	

	@Override
	public boolean contains(T item) {
		boolean[] marked = {false};
		int key = item.hashCode();
		Node<T> curr = head;
		
		while (curr.key < key) {
			Node<T> succ = curr.next.get(marked);
		}
		
		return (curr.key == key && !marked[0]);
	}
	
}
