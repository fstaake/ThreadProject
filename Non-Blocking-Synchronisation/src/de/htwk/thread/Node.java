package de.htwk.thread;

import java.util.concurrent.atomic.AtomicMarkableReference;

/**
 * A node that contains an item, the corresponding hash key and a refernece to the next node.
 * 
 * @author Denny Hecht, Franziska Staake, Felix Hain
 *
 * @param <T>
 *            the type of elements in this list
 */
public class Node<T> {
	/**
	 * The hash key from the item.
	 */
	public int key;
	/**
	 * The value of the node.
	 */
	public T item;
	/**
	 * Reference to the next node.
	 */
	public AtomicMarkableReference<Node<T>> next;

	/**
	 * Creates a new node with a specific value and the corresponding hash key.
	 * 
	 * @param item
	 *            a specific value
	 * @param key
	 *            the hash code of the item
	 */
	public Node(T item, int key) {
		this.item = item;
		this.key = key;
	}
	
	public static <T> int getKey(T item) {
		return item.hashCode();
	}
}
