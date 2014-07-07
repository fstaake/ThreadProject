/**
 * 
 */
package de.htwk.thread;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Denny Hecht
 *
 */
public class NonBlockingSynchronisation {
	private static LockFreeList<Integer> lockFreeList = null;

	/**
	 * @param args
	 *            not in use
	 * @throws InterruptedException
	 *             if any thread has interrupted the current thread.
	 */
	public static void main(String[] args) throws InterruptedException {
		lockFreeList = new LockFreeList<>();
		initLockFreeList();

		Thread[] threads = new Thread[] { new Thread(() -> operationsThread1()), new Thread(() -> operationsThread2()) };

		for (Thread thread : threads) {
			thread.start();
		}

		for (Thread thread : threads) {
			thread.join();
		}

		System.out.println(lockFreeList.printList());
	}

	private static void initLockFreeList() {
		System.out.println(lockFreeList.add(1));
		System.out.println(lockFreeList.add(3));
		System.out.println(lockFreeList.add(5));
		System.out.println(lockFreeList.add(7));
		System.out.println(lockFreeList.add(9));
	}

	private static void operationsThread1() {
		System.out.println(lockFreeList.add(2));
		System.out.println(lockFreeList.add(10));
		System.out.println(lockFreeList.add(8));
		System.out.println(lockFreeList.add(4));
		System.out.println(lockFreeList.add(6));
	}

	private static void operationsThread2() {
		System.out.println(lockFreeList.remove(1));
		System.out.println(lockFreeList.remove(9));
		System.out.println(lockFreeList.remove(7));
		System.out.println(lockFreeList.remove(3));
		System.out.println(lockFreeList.remove(5));
	}
}
