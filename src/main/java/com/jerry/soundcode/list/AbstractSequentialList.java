package com.jerry.soundcode.list;

import java.util.NoSuchElementException;

public abstract class AbstractSequentialList<T> extends AbstractList<T> {

	public AbstractSequentialList() {
	}

	@Override
	public T get(int index) {
		try {
			return listIterator(index).next();
		 } catch (NoSuchElementException exc) {
            throw new IndexOutOfBoundsException("Index: "+index);
        }
	}

	@Override
	public T set(int index, T t) {
		try {
			ListIterator<T> list = listIterator(index);
			T oldVal = list.next();
			list.set(t);
			return oldVal;
		} catch (NoSuchElementException e) {
		    throw new IndexOutOfBoundsException("Index: "+index);
		}
	}
	
	@Override
	public void add(int index, T t) {
		try {
			listIterator(index).add(t);
		} catch (NoSuchElementException exc) {
		    throw new IndexOutOfBoundsException("Index: "+index);
		}
	}
	
	@Override
	public T remove(int index) {
		try {
			ListIterator<T> list = listIterator(index);
			T outCast = list.next();
			list.remove();
			return outCast;
		} catch (NoSuchElementException exc) {
		    throw new IndexOutOfBoundsException("Index: "+index);
		}
	}
	
	@Override
	public boolean addAll(int index, Collection<? extends T> c) {
		try {
			boolean modified = false;
			ListIterator<T> list = listIterator(index);
			Iterator<? extends T> it = c.iterator();
			while(it.hasNext()) {
				list.add(it.next());
				modified = true;
			}
			return modified;
		} catch (NoSuchElementException exc) {
		    throw new IndexOutOfBoundsException("Index: "+index);
		}
	}

	@Override
	public Iterator<T> iterator() {
		return listIterator();
	}
	
	public abstract ListIterator<T> listIterator(int index);
}
