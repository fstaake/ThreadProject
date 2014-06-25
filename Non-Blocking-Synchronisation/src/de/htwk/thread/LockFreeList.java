package de.htwk.thread;

/**
 * 
 * @author Denny Hecht
 *
 * @param <T>
 */
public class LockFreeList<T> implements Set<T> {
	private Node head;
	
	
	@Override
	public boolean add(T item) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean remove(T item) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean contains(T item) {
		boolean marked[] = false{};
		int key = item.hashCode();
		Node curr = head;
		
		while (curr.key < key) {
			Node succ = curr.next.get(marked);
		}
		
		return (curr.key == key && !marked[0]);
	}
	
}
