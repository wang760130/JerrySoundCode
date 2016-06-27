package com.jerry.soundcode.thread;

import java.util.concurrent.TimeUnit;

public interface ScheduledExecutorService extends ExecutorService{

	public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit);
	
	public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit);
	
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, int initialDelay, long period, TimeUnit unit);
	
	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit);
}
