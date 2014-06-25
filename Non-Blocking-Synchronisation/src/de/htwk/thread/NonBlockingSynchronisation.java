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
		
		SynchrThread threads[] = new SynchrThread[2];
		
		threads[0] = new SynchrThread(lockFreeList, false);
		threads[1] = new SynchrThread(lockFreeList, true);
		
		for (int i = 0; i < 2; i++) {
			threads[i].start();
		}
		
		for (int i = 0; i < 2; i++) {
			threads[i].join();
		}
		
		lockFreeList.print();
	}

}
