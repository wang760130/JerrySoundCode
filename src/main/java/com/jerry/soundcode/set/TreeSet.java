package com.jerry.soundcode.set;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import com.jerry.soundcode.list.Collection;
import com.jerry.soundcode.list.Comparator;
import com.jerry.soundcode.list.Iterator;
import com.jerry.soundcode.map.Map;
import com.jerry.soundcode.map.NavigableMap;
import com.jerry.soundcode.map.TreeMap;

public class TreeSet<T> extends AbstractSet<T> 
	implements NavigableSet<T>, Cloneable, Serializable {

	private static final long serialVersionUID = 1L;

	private transient NavigableMap<T, Object> map;
	
	private static final Object PERSENT = new Object();
	
	public TreeSet(NavigableMap<T, Object> map) {
		this.map = map;
	}
	
	public TreeSet() {
		this(new TreeMap<T, Object>());
	}
	
	public TreeSet(Comparator<? super T> comparator) {
		this(new TreeMap<T, Object>(comparator));
	}
	
	public TreeSet(Collection<? extends T> c) {
		this();
		addAll(c);
	}
	
	public TreeSet(SortedSet<T> s) {
		this(s.comparator());
		addAll(s);
	}
	
	@Override
	public Iterator<T> iterator() {
		return map.navigableKeySet().iterator();
	}
	
	@Override
	public Iterator<T> descendingIterator() {
		return map.descendingKeySet().iterator();
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public NavigableSet<T> descendingSet() {
		return new TreeSet(map.descendingMap());
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
	
	@Override
	public boolean add(T t) {
		return map.put(t, PERSENT) == null;
	}
	
	@Override
	public boolean remove(Object o) {
		return map.remove(o) == PERSENT;
	}
	
	@Override
	public void clear() {
		map.clear();
	}
	
	@SuppressWarnings("unchecked")
	public boolean addAll(Collection<? extends T> c) {
		if(map.size() == 0 && c.size() > 0 && 
				c instanceof SortedSet && map instanceof TreeMap) {
			
			SortedSet<? extends T> set = (SortedSet<? extends T>) c;
			TreeMap<T, Object> m = (TreeMap<T, Object>) map;
			
			Comparator<? super T> cc = (Comparator<? super T>) set.comparator();
			Comparator<? super T> mc = map.comparator();
			
			if(cc == mc || (cc != null && cc.equals(mc))) {
				m.addAllForTreeSet(set, PERSENT);
				return true;
			}
		}	
		
		return super.addAll(c);
	}
	
	public NavigableSet<T> subSet(T fromElement, boolean fromInclusive,
								  T toElement,   boolean toInclusive) {
		return new TreeSet<T> (map.subMap(fromElement, fromInclusive, toElement, toInclusive));
	}
	
	@Override
	public NavigableSet<T> headSet(T toElement, boolean inclusive) {
		return new TreeSet<T> (map.headMap(toElement, inclusive));
	}
	
	@Override
	public NavigableSet<T> tailSet(T fromElement, boolean inclusive) {
		return new TreeSet<T> (map.tailMap(fromElement, inclusive));
	}
	
	
	@Override
	public SortedSet<T> subSet(T fromElement, T toElement) {
		return subSet(fromElement, true, toElement, false);
	}

	@Override
	public SortedSet<T> headSet(T toElement) {
		return headSet(toElement, false);
	}
	
	
	@Override
	public SortedSet<T> tailSet(T fromElement) {
		return tailSet(fromElement, true);
	}
	
	@Override
	public Comparator<? super T> comparator() {
		return map.comparator();
	}

	@Override
	public T first() {
		return map.firstKey();
	}

	@Override
	public T last() {
		return map.lastKey();
	}

	@Override
	public T lower(T t) {
		return map.lowerKey(t);
	}

	@Override
	public T floor(T t) {
		return map.floorKey(t);
	}

	@Override
	public T ceiling(T t) {
		return map.ceilingKey(t);
	}

	@Override
	public T higher(T t) {
		return map.higherKey(t);
	}

	@Override
	public T pollFirst() {
		Map.Entry<T, ?> e = map.pollFirstEntry(); 
		return (e == null) ? null : e.getKey();
	}

	@Override
	public T pollLast() {
		Map.Entry<T, ?> e = map.pollLastEntry();
		return (e == null) ? null : e.getKey();
	}

	
	@SuppressWarnings("unchecked")
	@Override
	public Object clone() {
		TreeSet<T> clone = null;
		
		try {
			clone = (TreeSet<T>) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new InternalError();
		}
		
		clone.map = new TreeMap<T, Object>(map);
		return clone;
	}
	
	@SuppressWarnings("rawtypes")
	private void writeObject(ObjectOutputStream s) throws IOException {
		s.defaultWriteObject();
		s.writeObject(map.comparator());
		s.writeInt(map.size());
		
		for(Iterator it = map.keySet().iterator(); it.hasNext();) {
			s.writeObject(it.next());
		} 
	}
	
	@SuppressWarnings("unchecked")
	private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
		s.defaultReadObject();
		
		Comparator<? super T> c = (Comparator<? super T>) s.readObject();
		
		TreeMap<T, Object> tm;
		if(c == null) {
			tm = new TreeMap<T, Object>();
		} else {
			tm = new TreeMap<T, Object>(c);
		}
		map = tm;
		
		int size = s.readInt();
		tm.readTreeSet(size, s, PERSENT);
	}
}
