package de.htwk.thread;

/**
 * 
 * 
 * Interface for a list.
 *
 * @param <T>
 *            the type of elements in this list
 * @author Denny Hecht, Franziska Staake, Felix Hain
 */
public interface Set<T> {
	/**
	 * Appends the specified element to the end of this list.
	 * 
	 * @param item
	 *            element to be appended to this list
	 * @return <tt>true</tt> if successful<br>
	 *         <tt>false</tt> otherwise
	 */
	public boolean add(T item);

	/**
	 * Removes the element at the specified position in this list.
	 * 
	 * @param item
	 *            element to be removed from this list, if present
	 * @return <tt>true</tt> if successful<br>
	 *         <tt>false</tt> otherwise
	 */
	public boolean remove(T item);

	/**
	 * 
	 * @param item
	 *            element whose presence in this list is to be tested
	 * @return <tt>true</tt> if this list contains the specified element<br>
	 *         <tt>false</tt> otherwise
	 */
	public boolean contains(T item);
}
