package com.jerry.soundcode.list;

public interface Collection<T> extends Iterable<T> {
	
	int size();
	
	boolean isEmpty();
	
	boolean contains(Object o);
	
	Iterator<T> iterator();
	
	Object[] toArray();
	
	<T> T[] toArray(T[] a);
	
	boolean add(T t);
	
	boolean remove(Object o);
	
	boolean containsAll(Collection<?> c);
	
	boolean addAll(Collection<? extends T> c);

	boolean removeAll(Collection<?> c);
	
	boolean retainAll(Collection<?> c);
	
	void clear();
	
	boolean equals(Object o);
	
	int hashCode();
	
}
