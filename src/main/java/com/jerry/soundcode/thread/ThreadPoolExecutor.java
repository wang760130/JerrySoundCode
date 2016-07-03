package com.jerry.soundcode.thread;

import java.util.concurrent.TimeUnit;

import com.jerry.soundcode.concurrent.collection.BlockingQueue;
import com.jerry.soundcode.concurrent.collection.DelayQueue;
import com.jerry.soundcode.list.Collection;
import com.jerry.soundcode.list.List;

public class ThreadPoolExecutor extends AbstractExecutorService {

	public ThreadPoolExecutor(int corePoolSize, int maxValue, int i,
			TimeUnit nanoseconds, DelayQueue delayQueue) {
	}

	public ThreadPoolExecutor(int corePoolSize, int maxValue, int i,
			TimeUnit nanoseconds, BlockingQueue delayedWorkQueue,
			ThreadFactory threadFactory) {
		// TODO Auto-generated constructor stub
	}

	public ThreadPoolExecutor(int corePoolSize, int maxValue, int i,
			TimeUnit nanoseconds, BlockingQueue delayQueue,
			RejectedExecutionHandler handler) {
	}

	public ThreadPoolExecutor(int corePoolSize, int maxValue, int i,
			TimeUnit nanoseconds, BlockingQueue delayedWorkQueue,
			ThreadFactory threadFactory, RejectedExecutionHandler handler) {
	}

	@Override
	public void shutdown() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<Runnable> shutdownNow() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isShutdown() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isTerminated() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean awaitTermination(long timeout, TimeUnit unit)
			throws InterruptedException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void execute(Runnable command) {
		// TODO Auto-generated method stub
		
	}
	
	public long tiggerTime(long l) {
		// TODO Auto-generated method stub
		return 0;
	}

	public boolean isStopped() {
		// TODO Auto-generated method stub
		return false;
	}

	public BlockingQueue<Runnable> getQueue() {
		// TODO Auto-generated method stub
		return null;
	}
	
	public void purge() {
		
	}
	
	protected void prestartCoreThread() {
		// TODO Auto-generated method stub
		
	}
	
	protected int getPoolSize() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	protected int getCorePoolSize() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	protected void reject(Runnable command) {
		// TODO Auto-generated method stub
		
	}
}
