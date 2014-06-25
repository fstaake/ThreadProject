/**
 * 
 */
package de.htwk.thread;

/**
 * @author Denny Hecht
 *
 */
public class NonBlockingSynchronisation {

	/**
	 * @param args
	 *            not in use
	 */
	public static void main(String[] args) {
		Set<Integer> lockFreeList = new LockFreeList<>();
		
		System.out.println(lockFreeList.add(1));
		System.out.println(lockFreeList.add(3));
		System.out.println(lockFreeList.add(2));
		boolean result = lockFreeList.add(1);
		
		System.out.println(result);
	}

}
