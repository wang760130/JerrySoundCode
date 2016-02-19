package com.jerry.soundcode.set;

import com.jerry.soundcode.list.Comparator;

public interface SortedSet<T> extends Set<T> {
	
	Comparator<? super T> comparator();
	
	SortedSet<T> subSet(T fromElement, T toElement);
	
	SortedSet<T> headSet(T toElement);
	
	SortedSet<T> tailSet(T fromElement);
	
	T first();
	
	T last();
}
