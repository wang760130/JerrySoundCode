package com.jerry.soundcode.list;

import java.util.ListIterator;

public interface List<T> extends Collection<T> {
	
	T get(int index);
	
	T set(int index, T element);
	
	void add(int index, T element);
	
	T remove(int index);
	
	int indexOf(Object o);
	
	int lastIndexOf(Object o);
	
	ListIterator<T> listIterator();
	
	ListIterator<T> listIterator(int index);
	
	List<T> subList(int fromIndex, int toIndex);
}
