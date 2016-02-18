package com.jerry.soundcode.list;

public interface Deque<T> extends Queue<T> {
	
	void addFirst(T t);
	
	void addLast(T t);
	
	boolean offerFirst(T t);
	
	boolean offerLast(T t);
	
	T removeFirst();
	
	T removeLast();
	
	T pollFirst();
	
	T pollLast();
	
	T getFirst();
	
	T getLast();
	
	T peekFirst();
	
	T peekLast();
	
	boolean removeFirstOccurrence(Object o);
	
	boolean removeLastOccurrence(Object o);
	
	boolean add(T t);
	
	boolean offer(T t);
	
	T remove();
	
	T poll();
	
	T element();
	
	T peek();
	
	void push(T t);
	
	T pop();
	
	boolean remove(Object o);
	
	boolean contains(Object o);
	
	int size();
	
	Iterator<T> iterator();
	
	Iterator<T> descendingIterator();
}
