package com.jerry.soundcode.thread;

import java.util.ConcurrentModificationException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import com.jerry.soundcode.concurrent.collection.BlockingQueue;
import com.jerry.soundcode.concurrent.locks.Condition;
import com.jerry.soundcode.concurrent.locks.ReentrantLock;
import com.jerry.soundcode.list.ArrayList;
import com.jerry.soundcode.list.Iterator;
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

		public long completedTasks;
		
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
				workerDone(this);
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
					r = workQueue.poll();
				} else if(poolSize > corePoolSize || allowCoreThreadTimeOut) {
					r = workQueue.poll(keepAliveTime, TimeUnit.NANOSECONDS);
				} else {
					r = workQueue.take();
				} 
				
				if(r != null) {
					return r;
				}
				
				if(workerCanExit()) {
					if(runState >= SHUTDOWN) {
						interruptIdleWorders();
					}
					return null;
				}
				
			} catch (InterruptedException e) {
			}
		}
		
	}

	private boolean workerCanExit() {
		final ReentrantLock mainLock = this.mainLock;
		mainLock.lock();
		boolean canExit;
		try {
			canExit = runState >= STOP || workQueue.isEmpty() || (allowCoreThreadTimeOut && poolSize > Math.max(1,  corePoolSize));
		} finally {
			mainLock.unlock();
		}
		return canExit;
	}
	
	private void interruptIdleWorders() {
		final ReentrantLock mainLock = this.mainLock;
		mainLock.lock();
		try {
//			for(Worker w : workers) {
//				w.interruptIfdle();
//			}
		} finally {
			mainLock.unlock();
		}
	}
	
	void workerDone(Worker w) {
		final ReentrantLock mainLock = this.mainLock;
		mainLock.lock();
		try {
			completedTaskCount += w.completedTasks;
			workers.remove(w);
			if(--poolSize == 0) {
				tryTermainte();
			}
		} finally {
			mainLock.unlock();
		}
	}
	

	private void tryTermainte() {
		if(poolSize == 0) {
			int state = runState;
			if(state < STOP && !workQueue.isEmpty()) {
				state = RUNNING;
			}
			
			if(state == STOP || state == SHUTDOWN) {
				runState = TERMINATED;
				termination.signalAll();
				terminated();
			}
		}
	}
	
	@Override
	public void shutdown() {
		SecurityManager security = System.getSecurityManager();
		if(security != null) {
			security.checkPermission(shutdownPerm);
		}
		
		final ReentrantLock mainLock = this.mainLock;
		mainLock.lock();
		
		try {
			if(security != null) {
//				for(Worker w : workers) {
//					security.checkAccess(w.thread);
//				}
				
				int state = runState;
				if(state <  SHUTDOWN) {
					runState = SHUTDOWN;
				}
				
				try {
//					for(Worker w : workers) {
//						w.interruptIfdle();
//					}
				} catch (SecurityException e) {
					runState = state;
					throw e;
				}
			}
			
			tryTermainte();
		} finally {
			mainLock.unlock();
		}
	}
	
	@Override
	public List<Runnable> shutdownNow() {
		SecurityManager security = System.getSecurityManager();
		if(security != null) {
			security.checkPermission(shutdownPerm);
		}
		
		final ReentrantLock mainLock = this.mainLock;
		mainLock.lock();
		
		try {
			if(security != null) {
//				for(Worker w : workers) {
//					security.checkAccess(w.thread);
//				}
					
				int state = runState;
				if(state < STOP) {
					runState = STOP;
				}
				
				try {
//					for(Worker w : workers) {
//						w.interruptNow();
//					}
				} catch (SecurityException e) {
					runState = state;
					throw e;
				}
			}
			
			List<Runnable> tasks = drainQueue();
			tryTermainte();
			return tasks;
		} finally {
			mainLock.unlock();
		}
	}
	
	
	private List<Runnable> drainQueue() {
		List<Runnable> taskList = new ArrayList<Runnable>();
		workQueue.drainTo(taskList);
		
		while(!workQueue.isEmpty()) {
			Iterator<Runnable> it = workQueue.iterator();
			try {
				if(it.hasNext()) {
					Runnable r = it.next();
					if(workQueue.remove(r)) {
						taskList.add(r);
					}
				}
			} catch (ConcurrentModificationException ignore) {
				
			}
		}
		
		return taskList;
	}
	
	@Override
	public boolean isShutdown() {
		return runState != RUNNING;
	}
	
	boolean isStopped() {
		return runState == STOP;
	}

	public boolean isTerminating() {
		int state = runState;
		return state == SHUTDOWN || state == STOP;
	}
	
	@Override
	public boolean isTerminated() {
		return runState == TERMINATED;
	}
	
	@Override
	public boolean awaitTermination(long timeot, TimeUnit unit) throws InterruptedException {
		long nanos = unit.toNanos(timeot);
		final ReentrantLock mainLock = this.mainLock;
		mainLock.lock();
		try {
			for(;;) {
				if(runState == TERMINATED) {
					return true;
				}
				if(nanos <= 0) {
					return false;
				}
				nanos = termination.awaitNanos(nanos);
			}
		} finally {
			mainLock.unlock();
		}
	}
	
	@Override
	protected void finalize() {
		shutdown();
	}
	
	public void setThreadFactory(ThreadFactory threadFactory) {
		if(threadFactory == null) {
			throw new NullPointerException();
		}
		this.threadFactory = threadFactory;
	}
	
	public ThreadFactory getThreadFactory() {
		return threadFactory;
	}
	
	public void setRejectedExecutionHandler(RejectedExecutionHandler handler) {
		if(handler == null) {
			throw new NullPointerException();
		}
		this.handler = handler;
	}
	
	public RejectedExecutionHandler getRejectedExecutionHandler() {
		return handler;
	}
	
	public void setCorePoolSize(int corePoolSize) {
		if(corePoolSize < 0) {
			throw new IllegalArgumentException();
		}
		
		final ReentrantLock mainLock = this.mainLock;
		mainLock.lock();
		
		try {
			int extra = this.corePoolSize - corePoolSize;
			this.corePoolSize = corePoolSize;
			if(extra < 0) {
				int n = workQueue.size();
				while(extra ++ < 0 && n-- > 0 && poolSize < corePoolSize) {
					Thread t = addThread(null);
					if(t == null) {
						break;
					}
				}
			} else if(extra > 0 && poolSize > corePoolSize) {
				try {
					Iterator<Worker> it = workers.iterator();
					while(it.hasNext() && extra-- > 0 && poolSize > corePoolSize && workQueue.remainingCapacity() == 0) {
						it.next().interruptIfdle();
					}
				} catch(SecurityException ignore) {
					
				}
			}
		} finally {
			mainLock.unlock();
		}
	}
	
	public int getCorePoolSize() {
		return corePoolSize;
	}
	
	public boolean prestartCoreThread() {
		return addIfUnderCorePoolSize(null);
	}
	
	public int prestartAllCoreThreads() {
		int n = 0;
		while(addIfUnderCorePoolSize(null)) {
			++n;
		}
		return n;
	}
	
	public boolean allowsCoreThreadTimeOut() {
		return allowCoreThreadTimeOut;
	}
	
	public void allowCoreThreadTimeOut(boolean value) {
		if(value && keepAliveTime <= 0) {
			throw new IllegalArgumentException("Core threads must have nonzero keep alive times");
		}
		
		allowCoreThreadTimeOut = value;
	}
	
	public void setMaximumPoolSize(int maximumPoolSize) {
		if(maximumPoolSize <= 0 || maximumPoolSize < corePoolSize) {
			throw new IllegalArgumentException();
		}
		
		final ReentrantLock mainLock = this.mainLock;
		mainLock.lock();
		
		try {
			int extra = this.maximumPoolSize - maximumPoolSize;
			this.maximumPoolSize = maximumPoolSize;
			if(extra > 0 && poolSize > maximumPoolSize) {
				try {
					Iterator<Worker> it = workers.iterator();
					while(it.hasNext() && extra > 0 && poolSize > maximumPoolSize) {
						it.next().interruptIfdle();
						--extra;
					}
				} catch(SecurityException ignore) {
					
				}
			}
		} finally {
			mainLock.unlock();
		}
	}
	
	public int getMaximumPoolSize() {
		return maximumPoolSize;
	}
	
	public void setKeepAliveTime(long time, TimeUnit unit) {
		if(time < 0) {
			throw new IllegalArgumentException();
		}
		
		if(time == 0 && allowCoreThreadTimeOut) {
			throw new IllegalArgumentException("Core threads must have nonzero keep alive times");
		}
		
		this.keepAliveTime = unit.toNanos(time);
	}
	
	public long getKeepAliveTime(TimeUnit unit) {
		return unit.convert(keepAliveTime, TimeUnit.NANOSECONDS);
	}
	
	public BlockingQueue<Runnable> getQueue() {
		return workQueue;
	}
	
	public boolean remove(Runnable task) {
		return getQueue().remove(task);
	}
	
	public void purge() {
		try {
			Iterator<Runnable> it = getQueue().iterator();
			while(it.hasNext()) {
				Runnable r = it.next();
				if(r instanceof Future<?>) {
					Future<?> c = (Future<?>) r;
					if(c.isCancelled()) {
						it.remove();
					}
				}
			}
		} catch(ConcurrentModificationException e) {
			return;
		}
	}
	
	public int getPoolSize() {
		return poolSize;
	}
	
	public int getActiveCount() {
		final ReentrantLock mainLock = this.mainLock;
		mainLock.lock();
		
		try {
			int n = 0;
//			for(Worker w : workers) {
//				if(w.isActive()) {
//					++n;
//				}
//			}
			return n;
		} finally {
			mainLock.unlock();
		}
	}
	
	public int getLargestPoolSize() {
		final ReentrantLock mainLock = this.mainLock;
		mainLock.lock();
		try {
			return largestPoolSize;
		} finally {
			mainLock.unlock();
		}
	}
	
	public long getTaskCount() {
		final ReentrantLock mainLock = this.mainLock;
		mainLock.lock();
		try {
			long n = completedTaskCount;
			/*for(Worker w : workers) {
				n += w.competedTasks;
				if(w.isActive()) {
					++n;
				}
			}*/
			return n + workQueue.size();
		} finally {
			mainLock.unlock();
		}
	}
	
	public long getCompletedTaskCount() {
		final ReentrantLock mainLock = this.mainLock;
		mainLock.lock();
		try {
			long n = completedTaskCount;
			/*for(Worker w : workers) {
				n += w.completedTasks;
			}*/
			return n;
		} finally {
			mainLock.unlock();
		}
	}
	
	protected void beforeExecute(Thread t, Runnable r) {}
	
	protected void afterExecute(Runnable task, RuntimeException ex) { }
	
	protected void terminated() { }

	public static class CallerRunsPolicy implements RejectedExecutionHandler {

		public CallerRunsPolicy() {}
		
		@Override
		public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
			if(!executor.isShutdown()) {
				r.run();
			}
		}
	}
	
	public static class AbortPolicy implements RejectedExecutionHandler {

		public AbortPolicy() {}
		
		@Override
		public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
			throw new RejectedExecutionException();
		}
		
	}
	
	public static class DiscardPolicy implements RejectedExecutionHandler {

		public DiscardPolicy() {}
		
		@Override
		public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
	
		}
		
	}	
	
	public static class DiscardOldestPolicy implements RejectedExecutionHandler {
		
		public DiscardOldestPolicy() {}
		
		@Override
		public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
			if(!executor.isShutdown()) {
				executor.getQueue().poll();
				executor.execute(r);
			}
		}
		
	}	
	
}
