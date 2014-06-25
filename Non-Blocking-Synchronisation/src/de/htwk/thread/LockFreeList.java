package de.htwk.thread;

import java.util.concurrent.atomic.AtomicMarkableReference;

public class LockFreeList<T> implements Set<T> {

	@Override
	public boolean add(T item) 
	{		
		int key = item.hashCode();
		Node head = null;

		while (true) {
			Window window = find(head, key);

			Node pred = window.pred, curr = window.curr;

			if (curr.key == key) {
				return false;
			} else {
				Node node = new Node(item);

				node.next = new AtomicMarkableReference<T>(curr, false);

				if (pred.next.compareAndSet(curr, node, false, false)) {
					return true;
				}
			}
		}
	}
	
	public Window find(Node head, int key) 
	{
		Node pred = null, curr = null, succ = null;

		Boolean marked = false;
		Boolean snip;

		retry: while (true) {

			pred = head;
			curr = pred.next.getReference();

			while (true) {

				succ = curr.next.get(marked);

				while (marked) {

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
		// TODO Auto-generated method stub
		return false;
	}
	
}
