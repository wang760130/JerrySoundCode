package com.jerry.soundcode.thread;

import java.util.concurrent.TimeUnit;

import com.jerry.soundcode.concurrent.atomic.AtomicLong;
import com.jerry.soundcode.concurrent.collection.BlockingQueue;
import com.jerry.soundcode.concurrent.collection.DelayQueue;
import com.jerry.soundcode.concurrent.collection.Delayed;
import com.jerry.soundcode.list.AbstractCollection;
import com.jerry.soundcode.list.Collection;
import com.jerry.soundcode.list.Iterator;
import com.jerry.soundcode.list.List;

public class ScheduledThreadPoolExecutor extends ThreadPoolExecutor implements ScheduledExecutorService {
	
    private volatile boolean continueExistingPeriodicTasksAfterShutdown;

    private volatile boolean executeExistingDelayedTasksAfterShutdown = true;

    private static final AtomicLong seqyecer = new AtomicLong(0);
    
    private static final long NANO_ORIGIN = System.nanoTime();
    
    final long now() {
    	return System.nanoTime() - NANO_ORIGIN;
    }
    
    private class ScheduledFutureTask<V> extends FutureTask<V> implements RunnableScheduledFuture<V> {
    	
    	private final long sequenceNumber;
    	
    	private long time;
    	
    	private final long period;
    	
    	ScheduledFutureTask(Runnable r, V result, long ns) {
    		super(r, result);
    		this.time = ns;
    		this.period = 0;
    		this.sequenceNumber = seqyecer.getAndIncrement();
    	}
    	
    	ScheduledFutureTask(Runnable r, V result, long ns, long period) {
    		super(r, result);
    		this.time = ns;
    		this.period = period;
    		this.sequenceNumber = seqyecer.getAndIncrement();
    	}
    	
		ScheduledFutureTask(Callable<V> callable, long ns) {
			super(callable);
			this.time = ns;
			this.period = 0;
			this.sequenceNumber = seqyecer.getAndIncrement();
		}
		
		@Override
		public long getDelay(TimeUnit unit) {
			return unit.convert(time - now(), TimeUnit.NANOSECONDS);
		}
		
		@Override
		public int compareTo(Delayed other) {
			if(other == this) {
				return 0;
			}
			
			if(other instanceof ScheduledFutureTask) {
				ScheduledFutureTask<?> x = (ScheduledFutureTask<?>)other;
				long diff = time - x.time;
				if(diff < 0) {
					return -1;
				} else if(diff > 0) {
					return 1;
				} else if(sequenceNumber < x.sequenceNumber) {
					return -1;
				} else {
					return 1;
				}
			}
			long d = (getDelay(TimeUnit.NANOSECONDS) - other.getDelay(TimeUnit.NANOSECONDS));
			return (d == 0) ? 0 : ((d < 0) ? -1 : 1);
		}

		@Override
		public boolean isPeriodic() {
			return period != 0;
		}
		
		private void runPeriodic() {
			boolean ok = ScheduledFutureTask.super.runAndReset();
			boolean down = isShutdown();
			
			if(ok && (!down || (getContinueExistingPeriodicTasksAfterShutdownPolicy() && !isStopped()))) {
				long p = period;
				if(p > 0) {
					time += p;
				} else {
					time = tiggerTime(-p);
				}
				ScheduledThreadPoolExecutor.super.getQueue().add(this);
			}
		}
		
		public void run() {
			if(isPeriodic()) {
				runPeriodic();
			} else {
				ScheduledFutureTask.super.run();
			}
		}
    }
    
    private void delayedExecute(Runnable command) {
    	if(isShutdown()) {
    		reject(command);
    		return ;
    	}
    	
    	if(getPoolSize() < getCorePoolSize()) {
    		prestartCoreThread();
    	}
    	super.getQueue().add(command);
    }
    
	private void cancelUnwantedTasks() {
    	boolean keepDelayed = getExecuteExistingDelayedTasksAfterShutdownPolicy();
    	boolean keepPeriodic = getContinueExistingPeriodicTasksAfterShutdownPolicy();
    	
    	if(!keepDelayed && !keepPeriodic) {
    		super.getQueue().clear();
    	} else if(keepDelayed || keepPeriodic) {
    		Object[] entries = super.getQueue().toArray();
    		for(int i = 0; i < entries.length; ++i) {
    			Object e = entries[i];
    			if(e instanceof RunnableScheduledFuture) {
    				RunnableScheduledFuture<?> t = (RunnableScheduledFuture<?>) e;
    				if(t.isPeriodic() ? !keepPeriodic : !keepDelayed) {
    					t.cancel(false);
    				}
    			}
    		}
    		
    		entries = null;
    		purge();
    	}
    }
    
    public boolean remove(Runnable task) {
    	if(!(task instanceof RunnableScheduledFuture)) {
    		return false;
    	}
    	return getQueue().remove(task);
    }
    
    protected <V> RunnableScheduledFuture<V> decorateTask(Runnable runnable, RunnableScheduledFuture<V> task) {
    	return task;
    }
    
    protected <V> RunnableScheduledFuture<V> decorateTask(Callable<V> callable, RunnableScheduledFuture<V> task) {
    	return task;
    }
    
    @SuppressWarnings("rawtypes")
	public ScheduledThreadPoolExecutor(int corePoolSize) {
    	super(corePoolSize, Integer.MAX_VALUE, 0, TimeUnit.NANOSECONDS, new DelayQueue());
    }
    
    public ScheduledThreadPoolExecutor(int corePoolSize, ThreadFactory threadFactory) {
    	super(corePoolSize, Integer.MAX_VALUE, 0, TimeUnit.NANOSECONDS, new DelayedWorkQueue(), threadFactory);
    }
    
    public ScheduledThreadPoolExecutor(int corePoolSize, RejectedExecutionHandler handler) {
    	super(corePoolSize, Integer.MAX_VALUE, 0, TimeUnit.NANOSECONDS, new DelayedWorkQueue(), handler);
    }
    
    private long triggerTime(long delay, TimeUnit unit) {
    	return triggerTime(unit.toNanos((delay < 0) ? 0 : delay));
    }
    
    long triggerTime(long delay) {
    	return now() + ((delay < (Long.MAX_VALUE >> 1)) ? delay : overflowFree(delay));
    }
    
	private long overflowFree(long delay) {
		Delayed head = (Delayed) super.getQueue().peek();
		if(head != null) {
			long headDelay = head.getDelay(TimeUnit.NANOSECONDS);
			if(headDelay < 0 && (delay - headDelay < 0)) {
				delay = Long.MAX_VALUE + headDelay;
			}
		}
		return delay;
	}
	
	public ScheduledThreadPoolExecutor(int corePoolSize, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
		super(corePoolSize, Integer.MAX_VALUE, 0, TimeUnit.NANOSECONDS, new DelayedWorkQueue(), threadFactory, handler);
	}
	
	@Override
	public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
		if(command == null || unit == null) {
			throw new NullPointerException();
		}
		RunnableScheduledFuture<?> t = decorateTask(command, new ScheduledFutureTask<Void>(command, null, triggerTime(delay, unit)));
		delayedExecute(t);
		return t;
	}
	
	@Override
	public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
		if(callable == null || unit == null) {
			throw new NullPointerException();
		}
		
		RunnableScheduledFuture<V> t = decorateTask(callable, new ScheduledFutureTask<V>(callable, triggerTime(delay, unit)));
		delayedExecute(t);
		return t;
	}
	
	@Override
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, int initialDelay, long period, TimeUnit unit) {
		if(command == null || unit == null) {
			throw new NullPointerException();
		}
		
		if(period <= 0) {
			throw new IllegalArgumentException();
		}
		
		RunnableScheduledFuture<?> t = decorateTask(command, new ScheduledFutureTask<Object>(command, null, triggerTime(initialDelay, unit), unit.toNanos(period)));
		delayedExecute(t);
		return t;
	}
	
	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
		if(command == null || unit == null) {
			throw new NullPointerException();
		}
		if(delay < 0) {
			throw new IllegalArgumentException();
		}
		
		RunnableScheduledFuture<?> t = decorateTask(command, new ScheduledFutureTask<Boolean> (command, null, triggerTime(initialDelay, unit), unit.toNanos(-delay)));
		delayedExecute(t);
		return t;	
	}
	
	@Override
	public void execute(Runnable command) {
		if(command == null) {
			throw new NullPointerException();
		}
		schedule(command, 0, TimeUnit.NANOSECONDS);
	}
	
	@Override
	public Future<?> submit(Runnable task) {
		return schedule(task, 0, TimeUnit.NANOSECONDS);
	}
	
	@Override
	public <T> Future<T> submit(Runnable task, T result) {
		return schedule(Executors.callable(task, result), 0, TimeUnit.NANOSECONDS);
	}
	
	@Override
	public <T> Future<T> submit(Callable<T> task) {
		return schedule(task, 0, TimeUnit.NANOSECONDS);
	}
	
	public void setContinueExistingPeriodicTasksAfterShutdownPolicy(
			boolean value) {
		this.continueExistingPeriodicTasksAfterShutdown = value;
		if(!value && isShutdown()) {
			cancelUnwantedTasks();
		}
	}

	public boolean getContinueExistingPeriodicTasksAfterShutdownPolicy() {
		return continueExistingPeriodicTasksAfterShutdown;
	}
	
	public void setExecuteExistingDelayedTasksAfterShutdownPolicy(boolean value) {
        executeExistingDelayedTasksAfterShutdown = value;
        if (!value && isShutdown())
            cancelUnwantedTasks();
    }
	
	public boolean getExecuteExistingDelayedTasksAfterShutdownPolicy() {
        return executeExistingDelayedTasksAfterShutdown;
    }
	
	@Override
	public void shutdown() {
		cancelUnwantedTasks();
		super.shutdown();
	}
	
	@Override
	public List<Runnable> shutdownNow() {
		return super.shutdownNow();
	}

	public BlockingQueue<Runnable> getQueue() {
		return super.getQueue();
	}
	
	private static class DelayedWorkQueue extends AbstractCollection<Runnable> implements BlockingQueue<Runnable> {
		
		@SuppressWarnings("rawtypes")
		private final DelayQueue<RunnableScheduledFuture> dp = new DelayQueue<RunnableScheduledFuture>();
		
		@Override
		public Runnable poll() {
			return dp.poll();
		}
		
		@Override
		public Runnable peek() {
			return dp.peek();
		}
		
		@Override
		public Runnable take() throws InterruptedException {
			return dp.take();
		}
		
		@Override
		public Runnable poll(long timeout, TimeUnit unit) throws InterruptedException {
			return dp.poll(timeout, unit);
		}
		
		@SuppressWarnings("rawtypes")
		@Override
		public boolean add(Runnable x) {
			return dp.add((RunnableScheduledFuture)x);
		}
		
		@SuppressWarnings("rawtypes")
		@Override
		public boolean offer(Runnable x) {
			return dp.offer((RunnableScheduledFuture)x);
		}
		
		@SuppressWarnings("rawtypes")
		@Override
		public void put(Runnable x) {
			dp.put((RunnableScheduledFuture)x);
		}

		@SuppressWarnings("rawtypes")
		@Override
		public boolean offer(Runnable x, long timeout, TimeUnit unit) {
			return dp.offer((RunnableScheduledFuture)x, timeout, unit);
		}
		
		@Override
		public Runnable remove() {
			return dp.remove();
		}

		@Override
		public Runnable element() {
			return dp.element();
		}

		@Override
		public void clear() {
			dp.clear();
		}
		
		@Override
		public int drainTo(Collection<? super Runnable> c) {
			return dp.drainTo(c);
		}
		
		@Override
		public int drainTo(Collection<? super Runnable> c, int maxElements) {
			return dp.drainTo(c, maxElements);
		}
		
		@Override
		public int remainingCapacity() {
			return dp.remainingCapacity();
		}
		
		@Override
		public boolean remove(Object x) {
			return dp.remove(x);
		}
		
		@Override
		public boolean contains(Object x) {
			return dp.contains(x);
		}
		
		@Override
		public int size() {
			return dp.size();
		}
		
		@Override
		public boolean isEmpty() {
			return dp.isEmpty();
		}
		
		@Override
		public Object[] toArray() {
			return dp.toArray();
		}
		
		@Override
		public <T> T[] toArray(T[] array) {
			return dp.toArray(array);
		}
		
		@Override
		public Iterator<Runnable> iterator() {
            return new Iterator<Runnable>() {
//                private Iterator<RunnableScheduledFuture> it = dq.iterator();
                
                public boolean hasNext() { 
//                	return it.hasNext(); 
                	return false;
                }
                
                public Runnable next() { 
//                	return it.next(); 
                	return null;
                }
                
                public void remove() { 
//                	it.remove(); 
                }
            };
        }

	}
}
