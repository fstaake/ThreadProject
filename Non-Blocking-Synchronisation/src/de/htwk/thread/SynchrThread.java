package de.htwk.thread;

public class SynchrThread extends Thread {
	private LockFreeList<Integer> list;
	private boolean add;
	
	public SynchrThread(LockFreeList<Integer> list, boolean add) {
		this.list = list;
		this.add = add;
	}
	
	@Override
	public void run() {
		if (this.add) {
			this.list.add(5);
		} else {
			this.list.remove(3);
		}
	}
}
