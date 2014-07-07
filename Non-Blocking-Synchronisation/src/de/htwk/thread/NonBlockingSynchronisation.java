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
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws InterruptedException {
		LockFreeList<Integer> lockFreeList = new LockFreeList<>();
		
		System.out.println(lockFreeList.add(1));
		System.out.println(lockFreeList.add(3));
		System.out.println(lockFreeList.add(2));
		System.out.println(lockFreeList.add(1));
		
		Thread[] threads = new Thread[] {
			new Thread(() -> lockFreeList.add(5)),
			new Thread(() -> lockFreeList.remove(3))
		};
		
		for (Thread thread : threads) {
			thread.start();
		}
		
		for (Thread thread : threads) {
			thread.join();
		}
		
		System.out.println(lockFreeList);
	}

}
