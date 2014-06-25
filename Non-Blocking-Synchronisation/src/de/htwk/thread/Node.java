package de.htwk.thread;

import java.util.concurrent.atomic.AtomicMarkableReference;


public class Node<T> {   
	public int key;
	public T item;
	public AtomicMarkableReference<Node<T>> next;
	
	public Node(T item){
		this.item = item;
	}
	
}
