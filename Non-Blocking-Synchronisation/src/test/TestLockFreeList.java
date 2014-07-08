package test;

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
	private static final String TEST = "{}";
	
	@Before
	public void before() {
		this.lockFreeList = new LockFreeList<>();
	}
	
	@BenchmarkOptions(benchmarkRounds = 500, warmupRounds = 0)
	@Test
	public void testLockFreeList() throws InterruptedException {
		IntStream.rangeClosed(1, 500).parallel().forEach(this.lockFreeList::add);
		IntStream.rangeClosed(1, 500).parallel().forEach(this.lockFreeList::remove);

		Assert.assertEquals(TEST, this.lockFreeList.toString());
	}
}
