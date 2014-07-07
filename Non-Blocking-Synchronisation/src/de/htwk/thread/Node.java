package de.htwk.thread;

import java.util.concurrent.atomic.AtomicMarkableReference;

public class Node<T> {
	public final int key;
	public final T item;
	public AtomicMarkableReference<Node<T>> next;

	public Node(T item, int key) {
		this.item = item;
		this.key = key;
	}
	
	public static <T> int getKey(T item) {
		return item.hashCode();
	}
}
