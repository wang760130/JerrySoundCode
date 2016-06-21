package com.jerry.soundcode.concurrent.collection;

import java.io.Serializable;

import com.jerry.soundcode.list.Collection;
import com.jerry.soundcode.list.Iterator;
import com.jerry.soundcode.set.AbstractSet;
import com.jerry.soundcode.set.Set;

public class CopyOnWriteArraySet<E> extends AbstractSet<E> implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private final CopyOnWriteArrayList<E> al;
	
	public CopyOnWriteArraySet() {
		al = new CopyOnWriteArrayList<E>();
	}
	
	public CopyOnWriteArraySet(Collection<? extends E> c) {
		al = new CopyOnWriteArrayList<E>();
		al.addAllAbsent(c);
	}
	
	public int size() {
		return al.size();
	}
	
	public boolean isEmpty() {
		return al.isEmpty();
	}
	
	public boolean contains(Object o) {
		return al.contains(o);
	}
	
	public Object[] toArray() {
		return al.toArray();
	}
	
	public <T> T[] toArray(T[] a) {
		return al.toArray(a);
	}
	
	public void clear() {
		al.clear();
	}
	
	public boolean remove(Object o) {
		return al.remove(o);
	}
	
	public boolean add(E e) {
		return al.addIfAbsent(e);
	}
	
	public boolean containsAll(Collection<?> c) {
		return al.containsAll(c);
	}
	
	public boolean addAll(Collection<? extends E> c) {
		return al.addAllAbsent(c) > 0;
	}
	
	public boolean removeAll(Collection<?> c) {
		return al.removeAll(c);
	}
	
	public boolean retainAll(Collection<?> c) {
		return al.removeAll(c);
	}
	
	@Override
	public Iterator<E> iterator() {
		return al.iterator();
	}
	
	@SuppressWarnings("unused")
	public boolean equals(Object o) {
		if(o == this) {
			return true;
		}
		if(!(o instanceof Set)) {
			return false;
		}
		Set<?> set = (Set<?>)(o);
		Iterator<?> it = set.iterator();
		
		Object[] elements = al.getArray();
		int len = elements.length;
		
		boolean[] matched = new boolean[len];
		int k = 0;
		outer : while(it.hasNext()) {
			if(++k > len) {
				return false;
			}
			Object x = it.next();
			for(int i = 0; i < len; ++i) {
				if(!matched[i] && eq(x, elements[i])) {
					matched[i] = true;
				}
				continue outer;
			}
			return false;
		}
		return k == len;
	}
	
	private static boolean eq(Object o1, Object o2) {
		return (o1 == null ? o2 == null : o1.equals(o2));
	}
}
