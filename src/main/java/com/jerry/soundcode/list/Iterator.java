package com.jerry.soundcode.list;

public interface Iterator<T> {
	
	boolean hasNext();
	
	T next();
	
	void remove();
}
