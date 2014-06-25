package de.htwk.thread;

/**
 * 
 * @author Denny Hecht
 *
 * @param <T>
 */
public interface Set<T> {
	/**
	 * 
	 * @param item
	 * @return
	 */
	public boolean add(T item);
	
	/**
	 * 
	 * @param item
	 * @return
	 */
	public boolean remove(T item);
	
	/**
	 * 
	 * @param item
	 * @return
	 */
	public boolean contains(T item);
}
