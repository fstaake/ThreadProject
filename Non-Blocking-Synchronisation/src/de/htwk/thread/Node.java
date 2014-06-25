package de.htwk.thread;

import java.util.concurrent.atomic.AtomicMarkableReference;


public class Node<T>
{   
	public T item;
	public int key;
	public AtomicMarkableReference<Node<T>> next;
}
