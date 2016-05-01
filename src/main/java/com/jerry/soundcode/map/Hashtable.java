package com.jerry.soundcode.map;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;

import com.jerry.soundcode.list.AbstractCollection;
import com.jerry.soundcode.list.Collection;
import com.jerry.soundcode.list.Enumeration;
import com.jerry.soundcode.list.Iterator;
import com.jerry.soundcode.set.AbstractSet;
import com.jerry.soundcode.set.Set;

public class Hashtable<K, V> extends Dictionary<K, V> 
	implements Map<K, V>, Cloneable, Serializable {

	private static final long serialVersionUID = 1L;

	@SuppressWarnings("rawtypes")
	private transient Entry[] table;
	
	private transient int count;
	
	private int threshold;
	
	private float loadFactor;
	
	private transient int modCount = 0;
	
	public Hashtable(int initialCapacity, float loadFactor) {
		if(initialCapacity < 0) {
			throw new IllegalArgumentException("Illegal Capacity: "+ initialCapacity);
		}
		
		if(loadFactor <= 0 || Float.isNaN(loadFactor)) {
			throw new IllegalArgumentException("Illegal Load: "+loadFactor);
		}
		
		if(initialCapacity == 0) {
			initialCapacity = 1;
		}
		this.loadFactor = loadFactor;
		table = new Entry[initialCapacity];
		threshold = (int)(initialCapacity * loadFactor);
	}
	
	public Hashtable(int initailCapacity) {
		this(initailCapacity, 0.75f);
	}
	
	public Hashtable() {
		this(11, 0.75f);
	}
	
	public Hashtable(Map<? extends K, ? extends V> map) {
		this(Math.max(2 * map.size(),11), 0.75f);
		putAll(map);
	}
	
	@Override
	public synchronized int size() {
		return count;
	}
	
	@Override
	public boolean isEmpty() {
		return count == 0;
	}
	
	@Override
	public synchronized Enumeration<K> keys() {
		return this.<K>getEnumeration(KEYS);
	}
	
	@Override
	public synchronized Enumeration<V> emelemts() {
		return this.<V>getEnumeration(VALUES);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public synchronized boolean contains(Object value) {
		if(value == null) {
			throw new NullPointerException();
		}
		
		Entry tab[] = table;
		for(int i = tab.length; i-- > 0;) {
			for(Entry<K, V> e = tab[i]; e != null; e = e.next) {
				if(e.value.equals(value)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public boolean containsValue(Object value) {
		return contains(value);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public synchronized boolean containsKey(Object key) {
		Entry tab[] = table;
		
		int hash = key.hashCode();
		int index = (hash & 0x7FFFFFFF) % tab.length;
		
		for(Entry<K, V> e = tab[index]; e != null; e = e.next) {
			if((e.hash == hash) && e.key.equals(key)) {
				return true;
			}
		}
		return false;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public synchronized V get(Object key) {
		Entry tab[] = table;
		int hash = key.hashCode();
		int index = (hash & 0x7FFFFFFF) % tab.length;
		for(Entry<K, V> e = tab[index]; e !=  null; e = e.next) {
			if((e.hash == hash) && e.key.equals(key)) {
				return e.value;
			}
		}
		return null;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected void rehash() {
		int oldCapactiy = table.length;
		Entry[] oldMap = table;
		
		int newCapactiy = oldCapactiy * 2 + 1;
		Entry[] newMap = new Entry[newCapactiy];
		
		modCount ++;
		threshold = (int)(newCapactiy * loadFactor);
		table = newMap;
		
		for(int i = oldCapactiy; i-- > 0;) {
			for(Entry<K, V> old = oldMap[i]; old != null;) {
				Entry<K, V> e = old;
				old = old.next;
				
				int index = (e.hash & 0x7FFFFFFF) % newCapactiy;
				e.next = newMap[index];
				newMap[index] = e;
			}
		}
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public synchronized V put(K key, V value) {
		if(value == null) {
			throw new NullPointerException();
		}
		
		Entry tab[] =  table;
		int hash = key.hashCode();
		int index = (hash & 0x7FFFFFFF) % tab.length;
		
		for(Entry<K, V> e = tab[index]; e != null; e = e.next) {
			if((e.hash == hash) && e.key.equals(key)) {
				V old = e.value;
				e.value = value;
				return old;
			}
		}
		
		modCount ++;
		if(count >= threshold) {
			rehash();
			
			tab = table;
			index = (hash & 0x7FFFFFFF) % tab.length;
		}
		
		Entry<K, V> e = tab[index];
		tab[index] = new Entry<K, V>(hash, key, value, e);
		count++;
		
		return null;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public synchronized V remove(Object key) {
		Entry tab[] = table;
		int hash = key.hashCode();
		int index = (hash & 0x7FFFFFFF) % tab.length;

		for(Entry<K, V> e = tab[index], prev = null; e != null; prev = e, e = e.next) {
			if((e.hash == hash) && e.key.equals(key)) {
				modCount++;
				if(prev != null) {
					prev.next = e.next;
				} else {
					tab[index] = e.next;
				}
				
				count --;
				V oldValue = e.value;
				e.value = null;
				return oldValue;
				
			}
		}
		
		return null;
	}
	
	@Override
	public synchronized void putAll(Map<? extends K, ? extends V> m) {
//		for(Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
//			put(e.getKey(), e.getValue());
//		}
 	}

	@SuppressWarnings("rawtypes")
	@Override
	public synchronized void clear() {
		Entry tab[] = table;
		modCount ++;
		for(int index = tab.length; --index >= 0;) 
			tab[index] = null;
		count = 0;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public synchronized Object clone() {
		try {
			Hashtable<K, V> t = (Hashtable<K, V>) super.clone();
			t.table = new Entry[table.length];
			for(int i = table.length; i-- > 0;) {
//				t.table[i] = (table[i] != null) ? (Entry<K,V>) table[i].clone() : null;
			}
			t.keySet = null;
			t.entrySet = null;
			t.values = null;
			t.modCount = 0;
			
			return t;
		} catch (CloneNotSupportedException e) {
			throw new InternalError();
		}
	}
	
	@Override
	public synchronized String toString() {
		int max = size() - 1;
		if (max == -1)
			return "{}";

		StringBuilder sb = new StringBuilder();
		Iterator<Map.Entry<K, V>> it = entrySet().iterator();

		sb.append('{');
		for (int i = 0;; i++) {
			Map.Entry<K, V> e = it.next();
			K key = e.getKey();
			V value = e.getValue();
			sb.append(key == this ? "(this Map)" : key.toString());
			sb.append('=');
			sb.append(value == this ? "(this Map)" : value.toString());

			if (i == max)
				return sb.append('}').toString();
			sb.append(", ");
		}
	}

	@SuppressWarnings("unchecked")
	private <T> Enumeration<T> getEnumeration(int type) {
		if(count == 0) {
			return (Enumeration<T>)emptyEnumerator;
		} else {
			return new Enumerator<T> (type, false);
		}
	}

	@SuppressWarnings("unchecked")
	private <T> Iterator<T> getIterator(int type) {
		if(count == 0) {
			return (Iterator<T>)emptyIterator;
		} else {
			return new Enumerator<T> (type, true);
		}
	}
	
	private transient volatile Set<K> keySet = null;
	private transient volatile Set<Map.Entry<K, V>> entrySet = null;
	private transient volatile Collection<V> values = null;
	
	
	
	@Override
	public Set<K> keySet() {
		if(keySet == null) {
//			keySet = Collections.synchronizedSet(new KeySet(), this);
		}
		return keySet;
	}
	
	@SuppressWarnings("unused")
	private class KeySet extends AbstractSet<K> {
		public Iterator<K> iterator() {
			return getIterator(KEYS);
		}

		@Override
		public int size() {
			return count;
		}
		
		@Override
		public boolean contains(Object o) {
			return containsKey(o);
		}
		
		@Override
		public boolean remove(Object o) {
			return Hashtable.this.remove(o) != null;
		}
		
		@Override
		public void clear() {
			Hashtable.this.clear();
		}
	}
	
	@Override
	public Set<Map.Entry<K, V>> entrySet() {
		if(entrySet == null) {
//			entrySet = Collections.synchronizedSet(new EntrySet(), this);
		}
		return entrySet;
	}
	
	@SuppressWarnings("unused")
	private class EntrySet extends AbstractSet<Map.Entry<K, V>> {
		
		@Override
		public Iterator<Map.Entry<K, V>> iterator() {
			return getIterator(ENTRIES);
		}
		
		@SuppressWarnings("rawtypes")
		@Override
		public boolean contains(Object o) {
			if( !(o instanceof Map.Entry)) {
				return false;
			}
			
			Map.Entry entry = (Map.Entry) o;
			Object key = entry.getKey();
			Entry[] tab = table;
			
			int hash = key.hashCode();
			int index = (hash & 0x7FFFFFFF) % tab.length;
			
			for(Entry e = tab[index]; e != null; e = e.next) {
				if(e.hash == hash && e.equals(entry)) {
					return true;
				}
			}
			
			return false;
		}
		
		@SuppressWarnings({ "rawtypes", "unchecked" })
		@Override
		public boolean remove(Object o) {
			if( ! (o instanceof Map.Entry)) {
				return false;
			}
			
			Map.Entry entry = (Map.Entry) o;
			K key = (K) entry.getKey();
			Entry[] tab = table;
			
			int hash = key.hashCode();
			int index = (hash & 0x7FFFFFFF) % tab.length;
			
			for(Entry<K, V> e = tab[index], prev = null; e != null; prev = e, e = e.next) {
				if(e.hash == hash && e.equals(entry)) {
					modCount ++;
					if(prev != null) {
						prev.next = e.next;
					} else {
						tab[index] = e.next;
					}
					count--;
					e.value = null;
					return true;
				} 
			}
			return false;
 		}
		
		@Override
		public int size() {
			return count;
		}
		
		@Override
		public void clear() {
			Hashtable.this.clear();
		}
		
	}
	

	@Override
	public Collection<V> values() {
		if(values == null) {
//			values = Collections.synchronizedCollection(new ValueCollection(),this);
		}
		
		return null;
	}

	@SuppressWarnings("unused")
	private class ValueCollection extends AbstractCollection<V> {

		@Override
		public Iterator<V> iterator() {
			return getIterator(VALUES);
		}

		@Override
		public int size() {
			return count;
		}
		
		@Override
		public boolean contains(Object o) {
			return containsValue(o);
		}
		
		@Override
		public void clear() {
			Hashtable.this.clear();
		}
		
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public synchronized boolean equals(Object o) {
		if(o == this) {
			return true;
		}
		
		if(! (o instanceof Map)) {
			return false;
		}
		
		Map<K, V> map = (Map<K, V>) o;
		if(map.size() != size()) {
			return false;
		}
		
		Iterator<Map.Entry<K, V>> it = entrySet.iterator();
		while(it.hasNext()) {
			Map.Entry<K, V> e = it.next();
			K key = e.getKey();
			V value = e.getValue();
			if(value == null) {
				if(! (map.get(key) == null && map.containsKey(key))) {
					return false;
				}
			} else {
				if(! value.equals(map.get(key))) {
					return false;
				}
			}
		}
		
		return true;
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public synchronized int hashCode() {
		int h = 0;
		if(count == 0 || loadFactor < 0) {
			return h;
		}
		
		loadFactor = -loadFactor;
		
		Entry[] tab = table;
		for(int i = 0; i < tab.length; i++) {
			for(Entry e = tab[i]; e != null; e = e.next) {
				h += e.key.hashCode() ^ e.value.hashCode();
			}
		}
		loadFactor = -loadFactor;
		return h;
	}
	
	@SuppressWarnings("rawtypes")
	private synchronized void writeObject(ObjectOutputStream s) throws IOException {
		s.defaultWriteObject();
		s.writeInt(table.length);
		s.writeInt(count);
		
		for(int index = table.length - 1; index >= 0; index--) {
			Entry entry = table[index];
			while(entry != null) {
				s.writeObject(entry.key);
				s.writeObject(entry.value);
				entry = entry.next;
			}
		}
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
		s.defaultReadObject();
		
		int origlength = s.readInt();
		int elements = s.readInt();
		
		int length = (int)(elements * loadFactor) + (elements / 20) + 3;
		if(length > elements && (length & 1) == 0) {
			length --;
		} 
		
		if(origlength > 0 && length > origlength) {
			length = origlength;
		}
		
		Entry[] table = new Entry[length];
		count = 0;
		
		for(; elements > 0; elements --) {
			K key = (K) s.readObject();
			V value = (V) s.readObject();
			reconstitutionPut(table, key, value);
		}
		this.table = table;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void reconstitutionPut(Entry[] tab, K key, V value) throws StreamCorruptedException {
		if(value == null) {
			throw new StreamCorruptedException();
		}
		
		int hash = key.hashCode();
		int index = (hash & 0x7FFFFFFF) % tab.length;
		
		for(Entry<K,V> e = tab[index]; e != null; e = e.next) {
			if((e.hash == hash) && e.key.equals(key)) {
				throw new StreamCorruptedException();
			}
		}
		
		Entry<K, V> e = tab[index];
		tab[index] = new Entry<K, V> (hash, key, value, e);
		count++;
	}
	
	private static class Entry<K, V> implements Map.Entry<K, V> {
		int hash;
		K key;
		V value;
		Entry<K, V> next;
		
		public Entry(int hash, K key, V value, Entry<K, V> next) {
			this.hash = hash;
			this.key = key;
			this.value = value;
			this.next = next;
		}
		
		@SuppressWarnings("unchecked")
		@Override
		protected Object clone() {
			return new Entry<K, V> (hash, key, value, (next == null ? null : (Entry<K, V>) next.clone()));
		}
		
		@Override
		public K getKey() {
			return key;
		}
		
		@Override
		public V getValue() {
			return value;
		}
		
		@Override
		public V setValue(V value) {
			if(value == null) {
				throw new NullPointerException();
			}
			V oldValue = this.value;
			this.value = value;
			return oldValue;
		}
		
		@SuppressWarnings("rawtypes")
		@Override
		public boolean equals(Object o) {
			if(!(o instanceof Map.Entry)) {
				return false;
			}
			
			Map.Entry e = (Map.Entry) o;
			
			return (key == null ? e.getKey() == null : key.equals(e.getKey()) && 
					(value == null ? e.getValue() == null : value.equals(e.getValue())));
		}
		
		@Override
		public int hashCode() {
			return hash ^ (value == null ? 0 : value.hashCode());
		}
		
		@Override
		public String toString() {
			return key.toString() + "=" + value.toString();
		}
	}

	private static final int KEYS = 0;
	private static final int VALUES = 1;
	private static final int ENTRIES = 2;
	
	private class Enumerator<T> implements Enumeration<T>, Iterator<T> {
		
		@SuppressWarnings("rawtypes")
		Entry[] table = Hashtable.this.table;
		int index = table.length;
		Entry<K, V> entry = null;
		Entry<K, V> lastReturned = null;
		
		int type;
		boolean iterator;
		
		protected int expectedModCount = modCount;
		
		Enumerator(int type, boolean iterator) {
			this.type = type;
			this.iterator = iterator;
		}
		
		@SuppressWarnings({ "rawtypes", "unchecked" })
		@Override
		public boolean hasMoreElements() {
			Entry<K, V> e = entry;
			int i = index;
			Entry[] t = table;
			 
			while(e == null && i > 0) {
				e = t[--i];
			}
			entry = e;
			index = i;
			return e != null;
		}
		
		@SuppressWarnings({ "rawtypes", "unchecked" })
		@Override
		public T nextElement() {
			Entry<K, V> et = entry;
			int i = index;
			Entry[] t = table;
			
			while(et == null && i > 0) {
				et = t[--i];
			}
			entry = et;
			index = i;
			
			if(et != null) {
				Entry<K, V> e = lastReturned = entry;
				entry = e.next;
				return type == KEYS ? (T)e.key : (type == VALUES ? (T)e.value : (T)e);
			}
			throw new NoSuchElementException("Hashtable Enumerator");
		}
		
		@Override
		public boolean hasNext() {
			return hasMoreElements();
		}

		@Override
		public T next() {
			if(modCount != expectedModCount) {
				throw new ConcurrentModificationException();
			}
			return nextElement();
		}

		@SuppressWarnings({ "rawtypes", "unchecked" })
		@Override
		public void remove() {
			if(!iterator) {
				throw new UnsupportedOperationException();
			}
			
			if(lastReturned == null) {
				throw new IllegalStateException("Hashtable Enumerator");
			}
			
			if(modCount != expectedModCount) {
				throw new ConcurrentModificationException();
			}
			
			synchronized (Hashtable.this) {
				Entry[] tab = Hashtable.this.table;
				int index = (lastReturned.hash & 0x7FFFFFFF) % tab.length;
				
				for(Entry<K, V> e = tab[index], prev = null; e != null; prev = e, e = e.next) {
					if(e == lastReturned) {
						modCount ++;
						expectedModCount ++;
						if(prev == null) {
							tab[index] = e.next;
						} else {
							prev.next = e.next;
						}
						
						count --;
						lastReturned = null;
						return ;
					}
				} 
				throw new ConcurrentModificationException();
			}
		}
	}
	
	@SuppressWarnings("rawtypes")
	private static Enumeration emptyEnumerator = new EmptyEnumerator();
	@SuppressWarnings("rawtypes")
	private static Iterator emptyIterator = new EmptyIterator();
	
	private static class EmptyEnumerator implements Enumeration<Object> {
		
		EmptyEnumerator() {
			
		}
	
		@Override
		public boolean hasMoreElements() {
			return false;
		}

		@Override
		public Object nextElement() {
			throw new NoSuchElementException("Hashtable Enumerator");
		}
		
	}
	
	private static class EmptyIterator implements Iterator<Object> {
		
		EmptyIterator() {
			
		}
		
		@Override
		public boolean hasNext() {
			return false;
		}

		@Override
		public Object next() {
			 throw new NoSuchElementException("Hashtable Iterator");
		}

		@Override
		public void remove() {
			 throw new NoSuchElementException("Hashtable Iterator");
		}
		
	}
	
}
