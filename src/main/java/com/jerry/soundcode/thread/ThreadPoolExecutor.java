package com.jerry.soundcode.thread;

import java.util.concurrent.TimeUnit;

import com.jerry.soundcode.concurrent.collection.BlockingQueue;
import com.jerry.soundcode.concurrent.collection.DelayQueue;
import com.jerry.soundcode.concurrent.locks.Condition;
import com.jerry.soundcode.concurrent.locks.ReentrantLock;
import com.jerry.soundcode.list.Collection;
import com.jerry.soundcode.list.List;
import com.jerry.soundcode.set.HashSet;

public class ThreadPoolExecutor extends AbstractExecutorService {

	private static final RuntimePermission shutdownPerm = new RuntimePermission("modifyThread");
	
	volatile int runState;
	static final int RUNNING 	= 0;
	static final int SHUTDOWN 	= 1;
	static final int STOP		= 2;
	static final int TERMINATED = 3;
	
	private final BlockingQueue<Runnable> workQueue;
	
	private final ReentrantLock mainLock = new ReentrantLock();
	
	private final Condition termination = mainLock.newCondition();
	
	private final HashSet<Worker> workers = new HashSet<Worker>();
	
	private volatile long keepAliveTime;
	
	private volatile boolean allowCoreThreadTimeOut;
	
	private volatile int corePoolSize;
	
	private volatile int maximumPoolSize;
	
	private volatile int poolSize;
	
	private volatile RejectedExecutionHandler handler;
	
	private volatile ThreadFactory threadFactory;
	
	private int largestPoolSize;
	
	private long completedTaskCount;
	
	private static final RejectedExecutionHandler defaultHandler = new AbortPolicy();
	
	public ThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime,
			TimeUnit unit, BlockingQueue<Runnable> workQueue) {
		this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, Executors.defaultThreadFactory(), defaultHandler);
	}
	
	public ThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime,
			TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
		this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, defaultHandler);
	}
	
	public ThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, 
			TimeUnit unit, BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler) {
		this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, Executors.defaultThreadFactory(), handler);
	}
	
	public ThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, 
			TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
		if(corePoolSize < 0 || maximumPoolSize <= 0 || maximumPoolSize < corePoolSize || keepAliveTime < 0) {
			throw new IllegalArgumentException();
		}
		if(workQueue == null || threadFactory == null || handler == null) {
			throw new NullPointerException();
		}
		
		this.corePoolSize = corePoolSize;
		this.maximumPoolSize = maximumPoolSize;
		this.workQueue = workQueue;
		this.keepAliveTime = unit.toNanos(keepAliveTime);
		this.threadFactory = threadFactory;
		this.handler = handler;
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
	
	private final class Worker implements Runnable {
		
		private final ReentrantLock runLock = new ReentrantLock();
		
		private Runnable firstTask;
		
		volatile long competedTasks;
		
		Thread thread;
		
		volatile boolean hasRun = false;
		
		Worker(Runnable firstTask) {
			this.firstTask = firstTask;
		}
		
		boolean isActive() {
			return runLock.isLocked();
		}
		
		void interruptIfdle() {
			final ReentrantLock lock = this.runLock;
			if(runLock.tryLock()) {
				try {
					if(hasRun && thread != Thread.currentThread()) {
						thread.interrupt();
					}
				} finally {
					runLock.unlock();
				}
			}
		}
		
		void interruptNow() {
			if(hasRun) {
				thread.interrupt();
			}
		}
		
		private void runTask(Runnable task) {
			final ReentrantLock runLock = this.runLock;
			runLock.lock();
			
			try {
				if((runState >= STOP || (Thread.interrupted() && runState >= STOP)) && hasRun) {
					thread.interrupt();
				}
				
				boolean ran = false;
				beforeExecute(thread, task);
				
				try {
					
				} catch (RuntimeException ex) {
					if(!ran) {
						afterExecute(task, ex);
					}
					throw ex;
				}
				
			} catch(Exception e) {
				runLock.unlock();
			}
		}
		
		Runnable getTask() {
			
			for(;;) {
				try {
					int state = runState;
					if(state > SHUTDOWN) {
						return null;
					}
					
					Runnable r;
					if(state == SHUTDOWN) {
						// TOTO
//						r = workQueue.poll();
					}
					
				} finally {
					
				}
			}
			
		}
		
		private void afterExecute(Runnable task, RuntimeException ex) {
			// TODO Auto-generated method stub
			
		}

		private void beforeExecute(Thread thread2, Runnable task) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void run() {
			
		}
	}
	
	public static class AbortPolicy implements RejectedExecutionHandler {

		public AbortPolicy() {}
		
		@Override
		public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
			
		}
		
	}
	
}
