package com.jerry.soundcode.set;

import com.jerry.soundcode.list.Collection;
import com.jerry.soundcode.list.Iterator;

public interface Set<T> extends Collection<T> {
	
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
	
	boolean retain(Collection<?> c);
	
	boolean removeAll(Collection<?> c);
	
	void clear();
	
	boolean equals(Object o);
	
	int hashCode();
	
} 
