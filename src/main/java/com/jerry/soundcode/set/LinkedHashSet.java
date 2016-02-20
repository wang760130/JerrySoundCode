package com.jerry.soundcode.set;

import java.io.Serializable;

import com.jerry.soundcode.list.Collection;

public class LinkedHashSet<T> extends HashSet<T> 
	implements Set<T>, Cloneable, Serializable {

	private static final long serialVersionUID = 1L;

	public LinkedHashSet(int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor, true);
	}
	
	public LinkedHashSet(int initialCapacity) {
		super(initialCapacity, 0.75f, true);
	}
	
	public LinkedHashSet() {
		super(16, 0.75f, true);
	}
	
	public LinkedHashSet(Collection<? extends T> c) {
		super(Math.max(2 * c.size(), 11), 0.75f, true);
		addAll(c);
	}
}
