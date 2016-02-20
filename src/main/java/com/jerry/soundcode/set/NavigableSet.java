package com.jerry.soundcode.set;

import com.jerry.soundcode.list.Iterator;

public interface NavigableSet<T> extends SortedSet<T> {

	T lower(T t);
	
	T floor(T t);
	
	T ceiling(T t);
	
	T higher(T t);
	
	T pollFirst();
	
	T pollLast();
	
	Iterator<T> iterator();
	
	NavigableSet<T> descendingSet();
	
	Iterator<T> descendingIterator();
	
	NavigableSet<T> subSet(T fromElement, boolean fromInclusive, T toElement, boolean toInclusive);
	
	NavigableSet<T> headSet(T toElement, boolean inclusive);
	
	NavigableSet<T> tailSet(T fromElement, boolean inclusive);
	
	SortedSet<T> subSet(T fromElement, T toElement);
	
	SortedSet<T> headSet(T toElement);
}
