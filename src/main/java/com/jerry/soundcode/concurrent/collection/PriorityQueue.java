package com.jerry.soundcode.concurrent.collection;

import java.io.Serializable;
import java.util.Arrays;

import com.jerry.soundcode.list.AbstractQueue;
import com.jerry.soundcode.list.Collection;
import com.jerry.soundcode.list.Comparator;
import com.jerry.soundcode.list.Iterator;
import com.jerry.soundcode.set.SortedSet;

public class PriorityQueue<E> extends AbstractQueue<E> implements Serializable {

	private static final long serialVersionUID = 1L;

	private static final int DEFAULT_INITIAL_CAPACITY = 11;
	
	private transient Object[] queue;
	
	private int size = 0;
	
	private Comparator<? super E> comparator;
	
	private transient int modCount = 0;
	
	PriorityQueue() {
		this(DEFAULT_INITIAL_CAPACITY, null);
	}
	
	PriorityQueue(int initialCapacity) {
		this(initialCapacity, null);
	}
	
	public PriorityQueue(int initialCapacity, Comparator<? super E> comparator) {
		if(initialCapacity < 1) {
			throw new IllegalArgumentException();
		}
		this.queue = new Object[initialCapacity];
		this.comparator = comparator;
	}
	
	public PriorityQueue(Collection<? extends E> c) {
		initFromCollection(c);
		if (c instanceof SortedSet) {
//            comparator = (Comparator<? super E>)
//                ((SortedSet<? extends E>)c).comparator();
		} else if (c instanceof PriorityQueue) {
//            comparator = (Comparator<? super E>)
//                ((PriorityQueue<? extends E>)c).comparator();
		} else {
            comparator = null;
            heapify();
        }
		comparator = null;
	}
	
	private void initFromCollection(Collection<? extends E> c) {
		Object[] a = c.toArray();
		if(a.getClass() != Object[].class) {
			a = Arrays.copyOf(a, a.length, Object[].class);
		}
		queue = a;
		size = a.length;
	}
	
	private void siftDown(int k, E x) {
		if(comparator != null) {
			siftDownUsingComparable(k, x);
		} else {
			siftDownComparable(k, x);
		}
	}
	
	private void siftDownUsingComparable(int k, E x) {
		// TODO Auto-generated method stub
		
	}

	private void siftDownComparable(int k, E x) {
		// TODO Auto-generated method stub
		
	}

	

	private void heapify() {
		for(int i = (size >>> 1) - 1; i >=0; i--) {
			siftDown(i, (E) queue[i]);
		}
	}
	
	@Override
	public boolean offer(E t) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public E poll() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public E peek() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterator<E> iterator() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int size() {
		// TODO Auto-generated method stub
		return 0;
	}


}
