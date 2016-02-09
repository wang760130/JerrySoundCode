package com.jerry.soundcode.list;

public interface ListIterator<T> extends Iterator<T> {
	
	boolean hasNext();
	
	T next();
	
	boolean hasPrevious();
	
	T previous();
	
	int nextIndex();
	
	int previousIndex();
	
	void remove();
	
	void set(T t);
	
	void add(T t);
}
