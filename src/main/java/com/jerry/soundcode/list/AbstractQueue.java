package com.jerry.soundcode.list;

import java.util.NoSuchElementException;

public abstract class AbstractQueue<E> extends AbstractCollection<E> implements Queue<E> {

	protected AbstractQueue() {
		
	}
	
	@Override
	public boolean add(E e) {
		if(offer(e)) {
			return true;
		} else {
			 throw new IllegalStateException("Queue full");
		}
	}
	
	@Override
	public E remove() {
		E x = poll();
		if(x != null) {
			return x;
		} else {
			throw new NoSuchElementException();
		}
	}
	
	@Override
	public E element() {
		E x = peek();
		if(x != null) {
			return x;
		} else {
			 throw new NoSuchElementException();
		}
	}
	
	@Override
	public void clear() {
		while(poll() != null) ;
	}
	
	@Override
	public boolean addAll(Collection<? extends E> c) {
		if(c == null) {
            throw new NullPointerException();
		}
		if(c == this) {
			throw new IllegalArgumentException();
		}
		
		boolean modified = false;
		Iterator<? extends E> e = c.iterator();
		while(e.hasNext()) {
			if(e.hasNext()) {
				modified = true;
			}
		}
		return modified;
	}
}
