package test;

import java.util.Arrays;
import java.util.stream.IntStream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.carrotsearch.junitbenchmarks.AbstractBenchmark;
import com.carrotsearch.junitbenchmarks.BenchmarkOptions;

import de.htwk.thread.LockFreeList;

/**
 * 
 * @author Denny Hecht
 *
 */
public class TestLockFreeList extends AbstractBenchmark {
	private LockFreeList<Integer> lockFreeList;
//	private static final String FELIX = "{2, 4, 6, 8, 10}";
	
	@Before
	public void before() {
		this.lockFreeList = new LockFreeList<>();
		//initList();
	}
	
	private void initList() {
		this.lockFreeList.add(1);
		this.lockFreeList.add(3);
		this.lockFreeList.add(5);
		this.lockFreeList.add(7);
		this.lockFreeList.add(9);
	}
	
	@BenchmarkOptions(benchmarkRounds = 500, warmupRounds = 0)
	@Test
	public void testLockFreeList() throws InterruptedException {
		Thread[] threads = new Thread[100];
		//Arrays.setAll(threads, i -> new Thread(() -> lockFreeList.add(i)));
		//IntStream.rangeClosed(1, 100).parallel().forEach(lockFreeList::add);
		
		for (Thread thread : threads) {
			thread.start();
		}

		for (Thread thread : threads) {
			thread.join();
		}
		
		System.out.println(this.lockFreeList);
//		Assert.assertEquals(FELIX, this.lockFreeList.toString());
	}
	
	private void operationsThread1() {
		this.lockFreeList.add(2);
		this.lockFreeList.add(10);
		this.lockFreeList.add(8);
		this.lockFreeList.add(4);
		this.lockFreeList.add(6);
	}
	
	private void operation() {
		this.lockFreeList.add(1);
		this.lockFreeList.add(3);
		this.lockFreeList.add(5);
		this.lockFreeList.add(7);
		this.lockFreeList.add(9);
	}
	
	private void operationsThread2() {
		this.lockFreeList.remove(1);
		this.lockFreeList.remove(9);
		this.lockFreeList.remove(7);
		this.lockFreeList.remove(3);
		this.lockFreeList.remove(5);
	}
}
