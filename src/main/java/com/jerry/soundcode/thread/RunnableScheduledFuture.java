package com.jerry.soundcode.thread;

public interface RunnableScheduledFuture<V> extends RunnableFuture<V>, ScheduledFuture<V> {

	boolean isPeriodic();
	
}
