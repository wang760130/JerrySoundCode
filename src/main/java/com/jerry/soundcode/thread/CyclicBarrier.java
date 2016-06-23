package com.jerry.soundcode.thread;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.jerry.soundcode.concurrent.locks.Condition;
import com.jerry.soundcode.concurrent.locks.ReentrantLock;

public class CyclicBarrier {

	private static class Generation {
		boolean broken = false;
	}
	
	private final ReentrantLock lock = new ReentrantLock();
	private final Condition trip = lock.newCondition();
	private final int parties;
	private final Runnable barrierCommand;
	private Generation generation = new Generation();
	
	private int count;
	
	private void nextGeneation() {
		trip.signalAll();
		count = parties;
		generation = new Generation();
	}
	
	private void breakBarrier() {
		generation.broken = true;
		count = parties;
		trip.signalAll();
	}
	
	private int dowait(boolean timed, long nanos)  throws InterruptedException, BrokenBarrierException, TimeoutException {
		
		final ReentrantLock lock = this.lock;
		lock.lock();
		
		try {
			final Generation g = generation;
			
			if(g.broken) {
				throw new BrokenBarrierException();
			}
			
			if(Thread.interrupted()) {
				breakBarrier();
				throw new InterruptedException();
			}
			
			int index = --count;
			if(index == 0) {
				boolean ranAction = false;
				try {
					final Runnable command = barrierCommand;
					if(command != null) {
						command.run();
						nextGeneation();
						return 0;
					}
				} finally {
					if(!ranAction) {
						breakBarrier();
					}
				}
			}
			
			for(;;) {
				try {
					if(!timed) {
						trip.await();
					} else if(nanos > 0L) {
						nanos = trip.awaitNanos(nanos);
					}
				} catch(InterruptedException e) {
					if(g == generation && !g.broken) {
						breakBarrier();
						throw e;
					} else {
						Thread.currentThread().interrupt();
					}
				}
				
				if(g.broken) {
					throw new BrokenBarrierException();
				}
				
				if(g != generation) {
					return index;
				}
				
				if(timed && nanos <= 0L) {
					breakBarrier();
					throw new TimeoutException();
				}
			}
		} finally {
			lock.unlock();
		}
		
	}
	
	public CyclicBarrier(int parties, Runnable barrierAction) {
		if(parties <= 0) {
			throw new IllegalArgumentException();
		}
		this.parties = parties;
		this.count = parties;
		this.barrierCommand = barrierAction;
	}
	
	public CyclicBarrier(int parties) {
		this(parties, null);
	}
	
	public int getParties() {
		return parties;
	}
	
	public int await() throws InterruptedException, BrokenBarrierException {
		try {
			return dowait(false, 0L);
		} catch (TimeoutException e) {
			throw new Error(e);
		}
	}
	
	public int await(long timeout, TimeUnit unit) throws InterruptedException, BrokenBarrierException, TimeoutException{
		return dowait(true, unit.toNanos(timeout));
	}
	
	public boolean isBroken() {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			return generation.broken;
		} finally {
			lock.unlock();
		}
	}
	
	public void reset() {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			breakBarrier();
			nextGeneation();
		} finally {
			lock.unlock();
		}
	}
	
	public int getNumberWaiting() {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			return parties - count;
		} finally {
			lock.unlock();
		}
	}
}
