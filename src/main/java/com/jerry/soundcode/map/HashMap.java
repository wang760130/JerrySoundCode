package com.jerry.soundcode.map;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;

import com.jerry.soundcode.list.AbstractCollection;
import com.jerry.soundcode.list.Collection;
import com.jerry.soundcode.list.Iterator;
import com.jerry.soundcode.set.AbstractSet;
import com.jerry.soundcode.set.Set;

public class HashMap<K, V> extends AbstractMap<K, V> 
	implements Map<K, V>, Cloneable, Serializable {

	private static final long serialVersionUID = 1L;

	static final int DEFAULT_INITIAL_CAPACITY = 16;
	static final int MAXIMUM_CAPACITY = 1 << 30;
	static final float DEFAULT_LOAD_FACTOR = 0.75f;
	
	transient Entry[] table;
	transient int size;
	
	int threshold;
	final float loadFactor;
	
	transient volatile int modCount;
	
	public HashMap(int initialCapacity, float loadFactor) {
		if(initialCapacity < 0) 
			throw new IllegalArgumentException("Illegal initial capacity: " +
                    initialCapacity);
		
		if(initialCapacity > MAXIMUM_CAPACITY) 
			initialCapacity = MAXIMUM_CAPACITY;
		
		if(loadFactor <= 0 || Float.isNaN(loadFactor)) {
			throw new IllegalArgumentException("Illegal load factor: " +
                    loadFactor);
		}
		
		int capacity = 1;
		while(capacity < initialCapacity)
			capacity <<= 1;
		
		this.loadFactor = loadFactor;
		threshold = (int)(capacity * loadFactor);
		table = new Entry[capacity];
		init();
	}
	
	public HashMap(int initialCapacity) {
		this(initialCapacity, DEFAULT_LOAD_FACTOR);
	}
	
	public HashMap() {
		this.loadFactor = DEFAULT_LOAD_FACTOR;
		threshold = (int)(DEFAULT_INITIAL_CAPACITY * DEFAULT_LOAD_FACTOR);
		table = new Entry[DEFAULT_INITIAL_CAPACITY];
		init();
	}
	
	public HashMap(Map<? extends K, ? extends V> map) {
		this(Math.max((int) (map.size() / DEFAULT_LOAD_FACTOR) + 1, DEFAULT_INITIAL_CAPACITY), DEFAULT_LOAD_FACTOR);
		putAllForCreate(map);
	}
	
	private void init() {
		
	}
	
	static int hash(int h) {
		h ^= (h >>> 20) ^ (h >>> 12);
		return h ^ (h >>> 7) ^ (h >>> 4);
	}
	
	static int indexFor(int h, int length) {
		return h & (length - 1);
	}
	
	@Override
	public int size() {
		return size;
	}
	
	@Override
	public boolean isEmpty() {
		return size == 0;
	}
	
	@Override
	public V get(Object key) {
		if(key == null) {
			return getForNullKey();
		}
		int hash = hash(key.hashCode());
		
		for(Entry<K,V> e = table[indexFor(hash, table.length)]; e != null; e = e.next) {
			Object k;
			if(e.hash == hash && ((k = e.key) == key || key.equals(k))) 
				return e.value;
		}
		return null;
	} 
	
	private V getForNullKey() {
		for(Entry<K, V> e = table[0]; e != null; e = e.next) {
			if(e.key == null) 
				return e.value;
		}
		return null;
	}
	
	@Override
	public boolean containsKey(Object key) {
		return getEntry(key) != null;
	}
	
	final Object getEntry(Object key) {
		int hash = (key == null) ? 0 : hash(key.hashCode());
		for(Entry<K,V> e = table[indexFor(hash, table.length)]; e != null; e = e.next) {
			Object k;
			if(e.hash == hash && ((k = e.key) == key || (key != null && key.equals(k)))) 
				return e;
		}
		return null;
	}

	@Override
	public V put(K key, V value) {
		if(key == null) 
			return putForNullKey(value);
		
		int hash = hash(key.hashCode());
		int i = indexFor(hash, table.length);
		for(Entry<K, V> e = table[i]; e != null; e = e.next) {
			Object k ;
			if(e.hash == hash && ((k = e.key) == key || key.equals(k))) {
				V oldValue = e.value;
				e.value = value;
				e.recordAccess(this);
				return oldValue;
			}
		}
		
		modCount ++;
		addEntry(hash, key, value, i);
		return null;
	}
	
	private V putForNullKey(V value) {
		for(Entry<K, V> e = table[0]; e != null; e = e.next) {
			if(e.key == null) {
				V oldValue = e.value;
				e.value = value;
				e.recordAccess(this);
				return oldValue;
			}
		}
		modCount ++;
		addEntry(0, null, value, 0);
		return null;
	}
	
	private void putForCreate(K key, V value) {
		int hash = (key == null) ? 0 : hash(key.hashCode());
		int i = indexFor(hash, table.length);
		
		for(Entry<K, V> e = table[i]; e != null; e = e.next) {
			Object k ;
			if (e.hash == hash && ((k = e.key) == key || (key != null && key.equals(k)))) {
				 e.value = value;
		         return;
		    }
		}
		createEntry(hash, key, value, i);
		
	}
	
	private void putAllForCreate(Map<? extends K, ? extends V> map) {
		for(Iterator<? extends Map.Entry<? extends K, ? extends V>> it = map.entrySet().iterator(); it.hasNext();) {
			Map.Entry<? extends K, ? extends V> e = it.next();
			putForCreate(e.getKey(), e.getValue());
		}
	}
	
	void resize(int newCapacity) {
		Entry[] oldTable = table;
		int oldCapacity = oldTable.length;
		if(oldCapacity == MAXIMUM_CAPACITY) {
			threshold = Integer.MAX_VALUE;
			return ;
		}
		
		Entry[] newTable = new Entry[newCapacity];
		transfer(newTable);
		table = newTable;
		threshold = (int)(newCapacity * loadFactor);
	}
	
	void transfer(Entry[] newTable) {
		Entry[] src = table;
		int newCapacity = newTable.length;
		for(int j = 0; j < src.length; j++) {
			Entry<K, V> e = src[j];
			if(e != null) {
				src[j] = null;
				do{
					Entry<K,V> next = e.next;
					int i = indexFor(e.hash, newCapacity);
					e.next = newTable[i];
					newTable[i] = e;
					e = next;
				} while(e != null);
			}
		}
	}
	
	@Override
	public void putAll(Map<? extends K, ? extends V> map) {
		int numKeysToBeAdded = map.size();
		if(numKeysToBeAdded == 0) {
			return ;
		}
		
		if(numKeysToBeAdded > threshold) {
			int targetCapacity = (int)(numKeysToBeAdded / loadFactor + 1);
			if(targetCapacity > MAXIMUM_CAPACITY) {
				targetCapacity = MAXIMUM_CAPACITY;
			}
			int newCapacity = table.length;
			while(newCapacity < targetCapacity) {
				newCapacity <<= 1;
			}
			
			if(newCapacity > table.length) {
				resize(newCapacity);
			}
			
			for(Iterator<? extends Map.Entry<? extends K, ? extends V>> it = map.entrySet().iterator(); it.hasNext(); ) {
				Map.Entry<? extends K, ? extends V> e = it.next();
				put(e.getKey(), e.getValue());
			}
		}
	}
	
	@Override
	public V remove(Object key) {
		Entry<K, V> e = removeEntryForKey(key);
		return (e == null ? null : e.value);
	}
	
	protected Entry<K, V> removeEntryForKey(Object key) {
		int hash = (key == null) ? 0 : hash(key.hashCode());
		int i = indexFor(hash, table.length);
		
		Entry<K, V> prev = table[i];
		Entry<K, V> e = prev;
		
		while(e != null) {
			Entry<K, V> next = e.next;
			Object k;
			
			if(e.hash == hash && ((k = e.key) == key || (key != null && key.equals(k)))) {
				modCount ++;
				size --;
				if(prev == e) {
					table[i] = next;
				} else {
					prev.next = next;
				}
				e.recordAccess(this);
				return e;
			}
			prev = e;
			e = next;
		}
		
		return e;
	}

	final Entry<K, V> removeMapping(Object o) {
		if(!(o instanceof Map.Entry)) 
			return null;
		
		Map.Entry<K, V> entry = (Map.Entry<K, V>) o;
		Object key = entry.getKey();
		int hash = (key == null) ? 0 : hash(key.hashCode());
		int i = indexFor(hash, table.length);
		
		Entry<K, V> prev = table[i];
		Entry<K, V> e = prev;
		
		while(e != null) {
			Entry<K, V> next = e.next;
			if(e.hash == hash && e.equals(entry)) {
				modCount ++;
				size --;
				if(prev == e) {
					table[i] = next;
				} else {
					prev.next = next;
				}
				e.recordAccess(this);
				return e;
			}
			prev = e;
			e = next;
		}
		
		return e;
	}
	
	public void clear() {
		modCount ++;
		Entry[] tab = table;
		for(int i = 0; i < tab.length; i++) {
			tab[i] = null;
		}
		size = 0;
	}
	
	@Override
	public boolean containsValue(Object value) {
		if(value == null) {
			return containsNullValue();
		}
		
		Entry[] tab = table;
		for(int i = 0; i < tab.length; i++) {
			for(Entry e = tab[i]; e != null; e = e.next) {
				if(value.equals(e.value)) {
					return true;
				}
			}
		}
		return false;
	}
	
	private boolean containsNullValue() {
		Entry[] tab = table;
		for(int i = 0; i < tab.length; i++) {
			for(Entry e = tab[i]; e != null; e = e.next) {
				if(e.value == null) {
					return true;
				}
			}
		}
		return false;
	}
	
	@Override
	public Object clone() {
		HashMap<K, V> result = null;
		
		try {
			result = (HashMap<K, V>) super.clone();
		} catch (CloneNotSupportedException e) {
		}
		
		result.table = new Entry[table.length];
		result.entrySet = null;
		result.modCount = 0;
		result.size = 0;
		result.init();
		result.putAllForCreate(this);
		
		return result;
	}

	static class Entry<K, V> implements Map.Entry<K, V> {
		final K key;
		V value;
		Entry<K, V> next;
		final int hash;
		
		Entry(int h, K k, V v, Entry<K, V> n) {
			value = v;
			next = n;
			key = k;
			hash = h;
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
		public V setValue(V newValue) {
			V oldValue = value;
			value = newValue;
			return oldValue;
		}
		
		@Override
		public final boolean equals(Object o) {
			if(!(o instanceof Map.Entry)) {
				return false;
			}
			
			Map.Entry e = (Map.Entry) o;
			Object k1 = getKey();
			Object k2 = e.getKey();
			
			if(k1 == k2 || (k1 != null && k1.equals(k2))) {
				Object v1 = getValue();
				Object v2 = e.getValue();
				
				if(v1 == v2 || (v1 != null && v1.equals(v2))) {
					return true;
				}
			}
			return false;
		}
		
		@Override
		public final int hashCode() {
			return (key == null ? 0 : key.hashCode()) ^ (value == null ? 0 : value.hashCode());
		}
		
		@Override
		public final String toString() {
			return getKey() + "=" + getValue();
		}
		
		void recordAccess(HashMap<K,V> m) {
        }
		
		void recordRemval(HashMap<K, V> m) {
			
		}
	}

	private void addEntry(int hash, K key, V value, int bucketIndex) {
		Entry<K, V> e = table[bucketIndex];
		table[bucketIndex] = new Entry<K, V>(hash, key, value, e);
		if(size++ >= threshold) {
			resize(2 * table.length);
		}
	}
	
	private void createEntry(int hash, K key, V value, int bucketIndex) {
		Entry<K, V> e = table[bucketIndex];
		table[bucketIndex] = new Entry(hash, key, value, e);
		size ++;
		
	}
	
	private abstract class HashIterator<T> implements Iterator<T> {
		Entry<K, V> next;
		int expectedModCount;
		int index;
		Entry<K, V> current;
		
		HashIterator() {
			expectedModCount = modCount;
			if(size > 0) {
				Entry[] t = table;
				while(index < t.length && (next = t[index++]) == null);
			}
		}
		
		@Override
		public final boolean hasNext() {
			return next != null;
		}
		
		final Entry<K, V> nextEntry() {
			if(modCount != expectedModCount) {
				throw new ConcurrentModificationException();
			}
			
			Entry<K, V> e = next;
			if(e == null) {
				throw new NoSuchElementException();
			}
			
			if((next = e.next) == null) {
				Entry[] t = table;
				while(index < t.length && (next = t[index++]) == null);
			}
			
			current = e;
			return e;
		}
		
		@Override
		public void remove() {
			if(current == null) {
				throw new IllegalStateException();
			}
			
			if(modCount != expectedModCount) {
				throw new ConcurrentModificationException();
			}
			
			Object key = current.key;
			current = null;
			
			HashMap.this.removeEntryForKey(key);
			expectedModCount = modCount;
		}
		
	}
	
	private final class ValueIterator extends HashIterator<V> {
		
		@Override
		public V next() {
			return nextEntry().value;
		}
	}
	
	private final class KeyIterator extends HashIterator<K> {
		
		@Override
		public K next() {
			return nextEntry().getKey();
		}
	}
	
	private final class EntryIterator extends HashIterator<Map.Entry<K, V>> {

		@Override
		public Map.Entry<K, V> next() {
			return nextEntry();
		}
		
	}
	
	Iterator<K> newKeyIterator() {
		return new KeyIterator();
	}
	
	Iterator<V> newValueIterator() {
		return new ValueIterator();
	}
	
	Iterator<Map.Entry<K, V>> newEntryIterator() {
		return new EntryIterator();
	}
	
	private transient Set<Map.Entry<K,V>> entrySet = null;
	
	public Set<K> keySet() {
		Set<K> set = keySet;
		return (set != null ? set : (keySet = new KeySet()));
	}
	
	private final class KeySet extends AbstractSet<K> {

		@Override
		public Iterator<K> iterator() {
			return newKeyIterator();
		}

		@Override
		public int size() {
			return size;
		}
		
		@Override
		public boolean contains(Object o) {
			return containsKey(o);
		}
		
		@Override
		public boolean remove(Object o) {
			return HashMap.this.removeEntryForKey(o) != null;
		}
		
		@Override
		public void clear() {
			HashMap.this.clear();
		}
	}
	
	public Collection<V> values() {
		Collection<V> value = values;
		return (value != null ? value : (values = new Values()));
	}

	private final class Values extends AbstractCollection<V> {

		@Override
		public Iterator<V> iterator() {
			return newValueIterator();
		}
		
		@Override
		public int size() {
			return size;
		}
		
		@Override
		public boolean contains(Object o) {
			return containsValue(o);
		}
		
		@Override
		public void clear() {
			HashMap.this.clear();
		}
	}
	
	@Override
	public Set<Map.Entry<K, V>> entrySet() {
		return entrySet0();
	}

	private Set<Map.Entry<K, V>> entrySet0() {
		Set<Map.Entry<K, V>> set = entrySet;
		return set != null ? set : (entrySet = new EntrySet());
	}
	
	private final class EntrySet extends AbstractSet<Map.Entry<K, V>> {

		@Override
		public Iterator<Map.Entry<K, V>> iterator() {
			return newEntryIterator();
		}
		
		@Override
		public boolean contains(Object o) {
			if(!(o instanceof Map.Entry)) {
				return false;
			}
			
			Map.Entry<K, V> e = (Entry<K, V>) o;
			Entry<K, V> condidate = (Entry<K, V>) getEntry(e.getKey());
			return condidate != null && condidate.equals(e);
		}

		@Override
		public int size() {
			return size;
		}
		
		@Override
		public boolean remove(Object o) {
			return removeMapping(o) != null;
		}
		
		@Override
		public void clear() {
			HashMap.this.clear();
		}
		
	}
	
	private void writeObject(ObjectOutputStream s) throws IOException {
		Iterator<Map.Entry<K, V>> it = (size > 0) ? entrySet0().iterator() :  null;
		
		s.defaultWriteObject();
		s.writeInt(table.length);
		s.writeInt(size);
		
		if(it != null) {
			while(it.hasNext()) {
				Map.Entry<K, V> e = it.next();
				s.writeObject(e.getKey());
				s.writeObject(e.getValue());
			}
		}
	}
	
	private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
		s.defaultReadObject();
		
		int numBuckeds = s.readInt();
		table = new Entry[numBuckeds];
		
		init();
		
		int size = s.readInt();
		
		for(int i = 0; i < size; i++) {
			K key = (K) s.readObject();
			V value = (V) s.readObject();
			putForCreate(key, value);
		}
	}
	
	public int capacity() {
		return table.length;
	}
	
	public float loadFactor() {
		return loadFactor;
	}
}
