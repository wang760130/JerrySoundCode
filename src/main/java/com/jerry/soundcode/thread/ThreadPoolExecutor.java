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
	
	public void execute(Runnable command) {
		if(command == null) {
			throw new NullPointerException();
		}
		
		if(poolSize >= corePoolSize || !addIfUnderCorePoolSize(command)) {
			if(runState == RUNNING && workQueue.offer(command)) {
				if(runState != RUNNING || poolSize == 0) {
					ensureQueuedTaskHandled(command);
				}
			} else if(!addIfUnderCorePoolSize(command)) {
				reject(command);
			}
		}
	}
	
	private Thread addThread(Runnable firstTask) {
		Worker w = new Worker(firstTask);
		Thread t = threadFactory.newThread(w);
		boolean workerStarted = false;
		if(t != null) {
			if(t.isAlive()) {
				throw new IllegalArgumentException();
			}
			w.thread = t;
			workers.add(w);
			int nt = ++ poolSize;
			if(nt > largestPoolSize) {
				largestPoolSize = nt;
			}
			
			try {
				t.start();
				workerStarted = true;
			} finally {
				if(!workerStarted) {
					workers.remove(w);
				}
			}
		}
		
		return t;
	}
	
	private boolean addIfUnderCorePoolSize(Runnable firstTask) {
		Thread t = null;
		final ReentrantLock mainLock = this.mainLock;
		mainLock.lock();
		try {
			if(poolSize < corePoolSize && runState == RUNNING) {
				t = addThread(firstTask);
			}
		} finally {
			mainLock.unlock();
		}
		
		return t != null;
	}
	
	private boolean addIfUnderMaximumPoolSize(Runnable firstTask) {
		Thread t = null;
		final ReentrantLock mainLock = this.mainLock;
		mainLock.lock();
		try {
			if(poolSize < maximumPoolSize && runState == RUNNING) {
				t = addThread(firstTask);
			}
		} finally {
			mainLock.unlock();
		}
		
		return t != null;
	}
	
	private void ensureQueuedTaskHandled(Runnable command) {
		final ReentrantLock mainLock = this.mainLock;
		mainLock.lock();
		
		boolean reject = false;
		Thread t = null;
		
		try {
			int state = runState;
			if(state != RUNNING && workQueue.remove(command)) {
				reject = true;
			} else if(state < STOP && poolSize < Math.max(corePoolSize, 1) && !workQueue.isEmpty()) {
				t = addThread(null);
			}
					
		} finally {
			mainLock.unlock();
		}
		
		if(reject) {
			reject(command);
		}
	}

	void reject(Runnable command) {
		handler.rejectedExecution(command, this);
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
					task.run();
					ran = false;
					afterExecute(task, null);
					++competedTasks;
				} catch (RuntimeException ex) {
					if(!ran) {
						afterExecute(task, ex);
					}
					throw ex;
				}
				
			} finally {
				runLock.unlock();
			}
		}
		
		@Override
		public void run() {
			try {
				hasRun = true;
				Runnable task = firstTask;
				firstTask = null;
				while(task != null || (task = getTask()) != null) {
					runTask(task);
					task = null;
				}
			} finally {
				workQueue(this);
			}
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
//					r = workQueue.poll();
				}
				
			} finally {
				
			}
		}
		
	}

	public static class AbortPolicy implements RejectedExecutionHandler {

		public AbortPolicy() {}
		
		@Override
		public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
			
		}
		
	}
	
	private void workQueue(Worker worker) {
		// TODO Auto-generated method stub
		
	}

	
	
	private void afterExecute(Runnable task, RuntimeException ex) {
		// TODO Auto-generated method stub
		
	}

	private void beforeExecute(Thread thread2, Runnable task) {
		// TODO Auto-generated method stub
		
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
}
