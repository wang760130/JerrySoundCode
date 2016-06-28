package com.jerry.soundcode.thread;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.jerry.soundcode.concurrent.atomic.AtomicLong;
import com.jerry.soundcode.concurrent.collection.Delayed;
import com.jerry.soundcode.list.Collection;
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
    	
    }
    
	@Override
	public void shutdown() {
//		boolean ok = ScheduledFutureTask.super.runAndReset();
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
	public <T> Future<T> submit(Callable<T> task) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> Future<T> submit(Runnable tsk, T result) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<?> submit(Runnable task) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
			throws InterruptedException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> List<Future<T>> invokeAll(
			Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
			throws InterruptedException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
			throws InterruptedException, ExecutionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks,
			long timeout, TimeUnit unit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void execute(Runnable command) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ScheduledFuture<?> schedule(Runnable command, long delay,
			TimeUnit unit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay,
			TimeUnit unit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable command,
			int initialDelay, long period, TimeUnit unit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command,
			long initialDelay, long delay, TimeUnit unit) {
		// TODO Auto-generated method stub
		return null;
	}

	
}
