package de.htwk.thread;

import java.util.concurrent.atomic.AtomicMarkableReference;

public class LockFreeList<T> implements Set<T> {

	private Node<T> head;

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

		boolean[] marked = { false };
		boolean snip;

		retry: while (true) {

			pred = head;
			curr = pred.next.getReference();

			while (true) {

				succ = curr.next.get(marked);

				while (marked[0]) {

					snip = pred.next.compareAndSet(curr, succ, false, false);

					if (!snip) {
						continue retry;
					}

					curr = succ;
					succ = curr.next.get(marked);
				}

				if (curr.key >= key) {

					return new Window(pred, curr);
				}

				pred = curr;
				curr = succ;
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

				if (!snip) {
					continue;
				}

				pred.next.compareAndSet(curr, succ, false, false);

				return true;
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

}
