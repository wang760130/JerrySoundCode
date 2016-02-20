package com.jerry.soundcode.set;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import com.jerry.soundcode.list.Collection;
import com.jerry.soundcode.list.Iterator;
import com.jerry.soundcode.map.HashMap;
import com.jerry.soundcode.map.LinkedHashMap;

public class HashSet<T> extends AbstractSet<T> 
	implements Set<T>, Cloneable, Serializable {

	private static final long serialVersionUID = 1L;

	private transient HashMap<T, Object> map;
	
	private static final Object PERSENT = new Object();
	
	public HashSet() {
		map = new HashMap<T, Object>();
	}
	
	public HashSet(Collection<? extends T> c) {
		map = new HashMap<T, Object>(Math.max((int)(c.size() / 0.75f + 1), 16));
		addAll(c);
	}
	
	public HashSet(int initialCapacity) {
		map = new HashMap<T, Object>(initialCapacity);
	}
	
	HashSet(int initialCapacity, float loadFactor, boolean dummy) {
		new LinkedHashMap<T, Object>(initialCapacity, loadFactor);
	}
	
	@Override
	public Iterator<T> iterator() {
		return map.keySet().iterator();
	}

	@Override
	public int size() {
		return map.size();
	}

	@Override
	public boolean isEmpty() {
		return map.isEmpty();
	}
	
	@Override
	public boolean contains(Object o) {
		return map.containsKey(o);
	}
	
	public boolean add(T t) {
		return map.put(t, PERSENT) == null;
	}
	
	public boolean remove(Object o) {
		return map.remove(o) == PERSENT;
	}
	
	public void clear() {
		map.clear();
	}
	
	public Object clone() {
		HashSet<T> newSet;
		try {
			newSet = (HashSet<T>) super.clone();
			newSet.map = (HashMap<T, Object>) map.clone();
			return newSet;	
		} catch (CloneNotSupportedException e) {
			throw new InternalError();
		}
	}
	
	private void writeObject(ObjectOutputStream s) throws IOException {
		s.defaultWriteObject();
		s.writeInt(map.capacity());
		s.writeFloat(map.loadFactor());
		s.writeInt(map.size());
		
		for(Iterator it = map.keySet().iterator(); it.hasNext(); ) {
			s.writeObject(it.next());
		}
	}
	
	private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
		s.defaultReadObject();
		
		int capacity = s.readInt();
		float loadFactor = s.readFloat();
		
		map = (((HashSet)this) instanceof LinkedHashSet ?
	               new LinkedHashMap<T,Object>(capacity, loadFactor) :
	               new HashMap<T,Object>(capacity, loadFactor));
		int size = s.readInt();
		
		for(int i = 0; i < size; i++) {
			T t = (T) s.readObject();
			map.put(t, PERSENT);
		}
	}
}
