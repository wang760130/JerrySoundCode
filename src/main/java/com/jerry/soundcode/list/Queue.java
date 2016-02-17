package com.jerry.soundcode.list;

public interface Queue<T> extends Collection<T> {

	boolean add(T t);
	
	boolean offer(T t);
	
	T remove();
	
	T poll();
	
	T element();
	
	T peek();
}
