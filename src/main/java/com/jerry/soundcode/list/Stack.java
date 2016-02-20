package com.jerry.soundcode.list;

import java.util.EmptyStackException;

public class Stack<T> extends Vector<T> {

	private static final long serialVersionUID = 1L;

	public Stack() {
		
	}
	
	public T pust(T item) {
		addElement(item);
		return item;
	}
	
	public synchronized T pop() {
		T obj;
		int len = size();
		
		obj = peek();
		removeElementAt(len - 1);
		
		return obj;
	}

	public synchronized T peek() {
		int len = size();
		
		if(len == 0) {
			throw new EmptyStackException();
		}
		return elementAt(len - 1);
	}
	
	public boolean empty() {
		return size() == 0;
	}
	
	public synchronized int search(Object o) {
		int i = lastIndexOf(o);
		
		if(i >= 0) {
			return size() - i;
		}
		return -1;
	}

}
