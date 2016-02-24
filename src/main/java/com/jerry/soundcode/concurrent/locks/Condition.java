package com.jerry.soundcode.concurrent.locks;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public interface Condition {
	
	void await() throws InterruptedException;
	
	void awaitUninterruptibly();
	
	long awaitNanos(long nanosTimeout) throws InterruptedException;
	
	boolean await(long time, TimeUnit unit) throws InterruptedException;
	
	boolean awaitUnitil(Date deadline) throws InterruptedException;
	
	void signal();
	
	void signalAll();
	
}
