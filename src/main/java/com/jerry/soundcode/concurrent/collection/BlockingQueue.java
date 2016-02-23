package com.jerry.soundcode.concurrent.collection;

import java.util.concurrent.TimeUnit;

import com.jerry.soundcode.list.Collection;
import com.jerry.soundcode.list.Queue;

public interface BlockingQueue<T> extends Queue<T> {
	
	boolean add(T t);
	
	boolean offer(T t);
	
	void put(T t) throws InterruptedException;
	
	boolean offer(T t, long timeout, TimeUnit unit) throws InterruptedException;
	
	T take() throws InterruptedException;
	
	T poll(long timeout, TimeUnit unit) throws InterruptedException;
	
	int remainingCapacity();
	
	boolean remove(Object o);
	
	boolean contains(Object o);
	
	int drainTo(Collection<? super T> c);
	
	int drainTo(Collection<? super T> c, int maxElements);
}
