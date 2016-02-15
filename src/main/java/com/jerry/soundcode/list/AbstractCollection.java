package com.jerry.soundcode.list;

import java.lang.reflect.Array;
import java.util.Arrays;

public abstract class AbstractCollection<T> implements Collection<T> {

	protected AbstractCollection() {}
	
	public abstract Iterator<T> iterator();
	
	public abstract int size();
	
	@Override
	public boolean isEmpty() {
		return size() == 0;
	}
	
	@Override
	public boolean contains(Object o) {
		Iterator<T> it = iterator();
		if(o == null) {
			while(it.hasNext()) {
				if(it.next() == null)
					return true;
			}
		} else {
			while(it.hasNext()) {
				if(o.equals(it.next())) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public Object[] toArray() {
		Object[] r = new Object[size()];
		Iterator<T> it = iterator();
		for(int i = 0; i < r.length; i++) {
			if( !it.hasNext()) 
				return Arrays.copyOf(r, i);
			r[i] = it.next();
		}
		return it.hasNext() ? finishToArray(r, it) : r;
	}
	
	@Override
	public <T> T[] toArray(T[] a) {
		int size = size();
		T[] r = a.length >= size ? a : (T[]) Array.newInstance(a.getClass().getComponentType(), size);
		Iterator<T> it = (Iterator<T>) iterator();
		
		for(int i = 0; i < r.length; i++) {
			if(! it.hasNext()) {
				if(a != r) {
					return Arrays.copyOf(r, i);
				}
				r[i] = null;
				return r;
			}
			r[i] = (T)it.next();
		}
		
		return it.hasNext() ? finishToArray(r, it) : r;
	}
	
	private static <T> T[] finishToArray(T[] r, Iterator<?> it) {
		int i = r.length;
		while(it.hasNext()) {
			int cap = r.length;
			if(i == cap) {
				int newCap = ((cap / 2) + 1) * 3;
				/*if(newCap <= cap) {
					if(cap == Integer.MAX_VALUE) 
						throw new OutOfMemoryError("Required array size too large");
					newCap = Integer.MAX_VALUE;
				}*/
				r = Arrays.copyOf(r, newCap);
			}
			r[i++] = (T) it.next();
		}
		return (i == r.length) ? r : Arrays.copyOf(r, i);
	}

	@Override
	public boolean add(T t) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public boolean remove(Object o) {
		Iterator<T> it = iterator();
		if(o == null) {
			while(it.hasNext()) {
				if(it.next() == null) {
					it.remove();
					return true;
				}
			}
		} else {
			while(it.hasNext()) {
				if(o.equals(it.next())) {
					it.remove();
					return true;
				}
			}
		}
		 
		return false;
	}	
	
	@Override
	public boolean containsAll(Collection<?> c) {
		Iterator<?> it = c.iterator();
		while(it.hasNext()) {
			if(!contains(it.next()))
				return false;
		}
		return true;
	}
	
	@Override
	public boolean addAll(Collection<? extends T> c) {
		boolean modified = false;
		Iterator<? extends T> it = c.iterator();
		while(it.hasNext()) {
			if(add(it.next()))
				modified = true;
		}
		return modified;
	}
	
	@Override
	public boolean removeAll(Collection<?> c) {
		boolean modified = false;
		Iterator<?> it = iterator();
		while(it.hasNext()) {
			if(c.contains(it.next())) {
				it.remove();
				modified = true;
			}
		}
		
		return modified;
	}
	
	@Override
	public boolean retainAll(Collection<?> c) {
		boolean modified = false;
		Iterator<?> it = iterator();
		while(it.hasNext()) {
			if(!c.contains(it.next())) {
				it.remove();
				modified = true;
			}
		}
		return modified;
	}

	@Override
	public void clear() {
		Iterator<T> it = iterator();
		while(it.hasNext()) {
			it.next();
			it.remove();
		}
	}
	
	@Override
	public String toString() {
		Iterator<T> it = iterator();
		if (!it.hasNext())
			return "[]";

		StringBuilder sb = new StringBuilder();
		sb.append('[');
		for (;;) {
			T t = it.next();
			sb.append(t == this ? "(this Collection)" : t);
			if (!it.hasNext())
				return sb.append(']').toString();
			sb.append(", ");
		}
	}
}
