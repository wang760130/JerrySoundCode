package com.jerry.soundcode.map;

import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;

import com.jerry.soundcode.list.AbstractCollection;
import com.jerry.soundcode.list.ArrayList;
import com.jerry.soundcode.list.Collection;
import com.jerry.soundcode.list.Iterator;
import com.jerry.soundcode.list.List;
import com.jerry.soundcode.list.ReferenceQueue;
import com.jerry.soundcode.ref.WeakReference;
import com.jerry.soundcode.set.AbstractSet;
import com.jerry.soundcode.set.Set;

public class WeakHashMap<K, V> extends AbstractMap<K, V> implements Map<K,V>{
	
	private static final int DEFAULT_INITIAL_CAPACITY = 16;
	
	private static final int MAXIMUM_CAPACITY = 1 << 30;
	
	private static final float DEFAULT_LOAD_FACEOR = 0.75f;
	
	private Entry[] table;
	
	private int size;
	
	private int threshold;
	
	private final float loadFactor;
	
	private final ReferenceQueue<K> queue = new ReferenceQueue<K>();
	
	private volatile int modCount ;
	
	public WeakHashMap(int initialCapacity, float loadFactor) {
		if(initialCapacity < 0) {
			 throw new IllegalArgumentException("Illegal Initial Capacity: "+
                     initialCapacity);
		}
		
		if(initialCapacity > MAXIMUM_CAPACITY) {
			initialCapacity = MAXIMUM_CAPACITY;
		}
		
		if(loadFactor <= 0 || Float.isNaN(loadFactor)) {
			throw new IllegalArgumentException("Illegal Load factor: "+
                    loadFactor);
		}
		
		int capacity = 1;
		while(capacity < initialCapacity) {
			capacity <<= 1;
		}
		
		table = new Entry[capacity];
		this.loadFactor = loadFactor;
		threshold = (int)(capacity * loadFactor);
	}
	
	public WeakHashMap(int initialCapacity) {
		this(initialCapacity, DEFAULT_LOAD_FACEOR);
	}
	
	public WeakHashMap() {
		this.loadFactor = DEFAULT_LOAD_FACEOR;
		threshold = (int)(DEFAULT_INITIAL_CAPACITY);
		table = new Entry[DEFAULT_INITIAL_CAPACITY];
	}
	
	public WeakHashMap(Map<? extends K, ? extends V> m) {
		this(Math.max((int) (m.size() / DEFAULT_LOAD_FACEOR) + 1, 16), DEFAULT_LOAD_FACEOR);
		putAll(m);
	}
	
	private static final Object NULL_KEY = new Object();
	
	private static Object maskNull(Object key) {
		return (key == null ? NULL_KEY : key);
	}
	
	private static <K> K unmaskNull(Object key) {
		return (K)(key == NULL_KEY ?  null : key);
	}
	
	static boolean eq(Object x, Object y) {
		return x == y || x.equals(y);
	}
	
	static int indexFor(int h, int length) {
		return h & (length - 1);
	}
	
	private void expungeStaleEntries() {
		Entry<K, V> e;
		
		while((e = (Entry<K, V>) queue.poll()) != null) {
			int h = e.hash;
			int i = indexFor(h, table.length);
			
			Entry<K,V> prev = table[i];
			Entry<K,V> p = prev;
			
			while(p != null) {
				Entry<K,V> next = p.next;
				if(p == e) {
					if(prev == e) {
						table[i] = next;
					} else {
						prev.next = next;
					}
					e.next = null;
					e.value = null;
					size --;
					break;
				}
				prev = p;
				p = next;
			}
			
		}
	}
	
	private Entry[] getTable() {
		expungeStaleEntries();
		return table;
	}
	
	public int size() {
		if(size == 0) {
			return 0;
		}
		expungeStaleEntries();
		return size;
	}
	
	public boolean isEmpty() {
		return size() == 0;
	}
	
	public V get(Object key) {
		Object k = maskNull(key);
		int h = HashMap.hash(k.hashCode());
		Entry[] tab = getTable();
		int index = indexFor(h, tab.length);
		Entry<K, V> e = tab[index];
		while(e != null) {
			if(e.hash == h && eq(k, e.get())) {
				return e.value;
			}
			e = e.next;
		}
		
		return null;
	}
	
	public boolean containsKey(Object key) {
		return getEntry(key) != null;
	}
	
	Entry<K, V> getEntry(Object key) {
		Object k = maskNull(key);
		int h = HashMap.hash(k.hashCode());
		Entry[] tab = getTable();
		int index = indexFor(h, tab.length);
		Entry<K, V> e = tab[index];
		while(e != null && !(e.hash == h && eq(k, e.get()))) {
			e = e.next;
		}
		
		return e;
	}
	
	public V put(K key, V value) {
		K k = (K)maskNull(key);
		int h = HashMap.hash(k.hashCode());
		Entry[] tab = getTable();
		int i = indexFor(h, tab.length);
		
		for(Entry<K, V> e = tab[i]; e != null; e = e.next) {
			if(h == e.hash && eq(k, e.get())) {
				V oldValue = e.value;
				if(value != oldValue) {
					e.value = value;
				}
				return oldValue;
			}
		}
		
		modCount++;
		Entry<K, V> e = tab[i];
//		tab[i] = new Entry<K,V>(k, value, queue, h, e);
		if(++size >= threshold) {
			resize(tab.length * 2);
		}
		return null;
	}
	
	void resize(int newCapacity) {
		Entry[] oldTable = getTable();
		int oldCapacity = oldTable.length;
		if(oldCapacity == MAXIMUM_CAPACITY) {
			threshold = Integer.MAX_VALUE;
			return ;
		}
				
		Entry[] newTable = new Entry[newCapacity];
		transfer(oldTable, newTable);
		table = newTable;
		
		if(size >= threshold / 2) {
			threshold = (int)(newCapacity * loadFactor);
		} else {
			expungeStaleEntries();
			transfer(newTable, oldTable);
			table = oldTable;
		}
		
	}
	
	private void transfer(Entry[] src, Entry[] dest) {
		for(int j = 0; j < src.length; ++j) {
			Entry<K, V> e = src[j];
			src[j] = null;
			while(e != null) {
				Entry<K,V> next = e.next;
				Object key = e.get();
				if(key == null) {
					e.next = null;
					e.value = null;
					size --;
				} else {
					int i = indexFor(e.hash, dest.length);
					e.next = dest[i];
					dest[i] = e;
				}
				e = next;	
			}
		}
	}
	
	public void putAll(Map<? extends K, ? extends V> m) {
		int numKeysToBeAdded = m.size();
		if(numKeysToBeAdded == 0) {
			return;
		}
		
		if(numKeysToBeAdded > threshold) {
			int targetCapacity = (int) (numKeysToBeAdded / loadFactor + 1);
			if(targetCapacity > MAXIMUM_CAPACITY) {
				targetCapacity = MAXIMUM_CAPACITY;
			}
			int newCapacity = table.length;
			while(newCapacity < targetCapacity){
				newCapacity <<= 1;
			}
			if(newCapacity > table.length) {
				resize(newCapacity);
			}
		}
		
//		for(Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
//			put(e.getKey(), e.getValue());
//		}
			
	} 
	
	public V remove(Object key) {
		Object k = maskNull(key);
		int h = HashMap.hash(k.hashCode());
		Entry[] tab = getTable();
		int i = indexFor(h,tab.length);
		
		Entry<K, V> prev = tab[i];
		Entry<K, V> e = prev;
		
		while(e != null) {
			Entry<K, V> next = e.next;
			if(h == e.hash && eq(k, e.get())) {
				modCount ++;
				size --;
				if(prev == e) {
					tab[i] = next;
				} else {
					prev.next = next;
				}
				return e.value;
			}
			prev = e;
			e = next;
		}
		return null;
	}
	
	Entry<K, V> removeMapping(Object o) {
		if(! (o instanceof Map.Entry)) {
			return null;
		}
		Entry[] tab = getTable();
		Map.Entry entry = (Map.Entry) o;
		Object k = maskNull(entry.getKey());
		int h = HashMap.hash(k.hashCode());
		int i = indexFor(h, tab.length);
		
		Entry<K, V> prev = tab[i];
		Entry<K, V> e = prev;
		
		while(e != null) {
			Entry<K, V> next = e.next;
			if(h == e.hash && e.equals(entry)) {
				modCount ++;
				size --;
				if(prev == e) {
					tab[i] = next;
				} else {
					prev.next = next;
				}				
				return e;
			}
			prev = e;
			e = next;
		}
		
		return null;
	}
	
	public void clear() {
		while(queue.poll() != null)
			;
		
		modCount ++;
		Entry[] tab = table;
		for(int i = 0; i < tab.length; ++i) {
			tab[i] = null;
		}
		size = 0;
		while(queue.poll() != null)
			;
	}
	
	public boolean containsValue(Object value) {
		if(value == null) {
			return containsNullValue();
		}
		
		Entry[] tab = getTable();
		for(int i = tab.length; i-- > 0;) {
			for(Entry e = tab[i]; e != null; e = e.next) {
				if(value.equals(e.value)) {
					return true;
				}
			}
		}
		return false;
	}
	
	private boolean containsNullValue() {
		Entry[] tab = getTable();
		for(int i = tab.length; i-- >  0;) {
			for(Entry e = tab[i]; e != null; e = e.next) {
				if(e.value == null) {
					return true;
				}
			}
		}
		return false;
	}

	private static class Entry<K, V> extends WeakReference<K> {
		
		private V value;
		private int hash;
		private Entry<K,V> next;
		
		public Entry(K key, V value, ReferenceQueue<K> queue) {
			super(key, queue);
			this.value = value;
			this.hash = hash;
			this.next = next;
		}

		public K getKey() {
			return WeakHashMap.<K>unmaskNull(get());
		}
		
		public V getValue() {
			return value;
		}
		
		public V setValue(V newValue) {
			V oldValue = value;
			value = newValue;
			return oldValue;
		}
		
		public boolean equals(Object o) {
			if(! (o instanceof Map.Entry)) {
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
		
		public int hashCode() {
			Object k = getKey();
			Object v = getValue();
			
			return ((k == null ? 0 : k.hashCode())) ^ (v == null ? 0 : v.hashCode());
		}
		
		public String toString() {
			return getKey() + "=" + getValue();
		}
	}
	
	private abstract class HashIterator<T> implements Iterator<T> {
		int index;
		Entry<K, V> entry = null;
		Entry<K, V> lastReturned = null;
		int expectedModCount = modCount;
		
		Object nextKey = null;
		Object currentKey = null;
		
		HashIterator() {
			index = (size() != 0 ? table.length : 0 );
		}
		
		public boolean hasNext() {
			Entry[] t = table;
			
			while(nextKey == null) {
				Entry<K, V> e = entry;
				int i = index;
				
				while(e == null && i > 0) {
					e = t[--i];
				}
				
				entry = e;
				index = i;
				
				if(e == null) {
					currentKey = null;
					return false;
				}
				
				nextKey = e.get();
				if(nextKey == null) {
					entry = entry.next;
				}
			}
			return true;
		}
		
		protected Entry<K, V> nextEntry() {
			if(modCount != expectedModCount) {
                throw new ConcurrentModificationException();
			}
			
			if(nextKey == null && !hasNext()) {
                throw new NoSuchElementException();
			}
			
			lastReturned = entry;
			entry = entry.next;
			currentKey = nextKey;
			nextKey = null;
			return lastReturned;
		}
				
		public void remove() {
			if (lastReturned == null)
	                throw new IllegalStateException();
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            
            WeakHashMap.this.remove(currentKey);
            expectedModCount = modCount;
            lastReturned = null;
            currentKey = null;
		}
	}
	
	private class ValueIterator extends HashIterator<V> {
		public V next() {
			return nextEntry().value;
		}
	}
	
	private class KeyIterator extends HashIterator<K> {
		public K next() {
			return nextEntry().getKey();
		}
	}
	
	private class EntryIterator extends HashIterator<Map.Entry<K,V>> {
		public Map.Entry<K, V> next() {
			return (Map.Entry<K, V>) nextEntry();
		}
		
	}
	
	private transient Set<Map.Entry<K, V>> entrySet = null;
	
	public Set<K> keySet() {
		Set<K> ks = keySet;
		return (ks != null) ? ks : (keySet = new KeySet());
	}
	
	private class KeySet extends AbstractSet<K> {

		@Override
		public Iterator<K> iterator() {
			return new KeyIterator();
		}

		@Override
		public int size() {
			return WeakHashMap.this.size();
		}
		
		public boolean contains(Object o) {
			return containsKey(o);
		}
		
		public boolean remove(Object o) {
			if(containsKey(o)) {
				WeakHashMap.this.remove(o);
				return true;
			} else {
				return false;
			}
		}
		
		public void clear() {
			WeakHashMap.this.clear();
		}
		
	}
	
	public Collection<V> values() {
		Collection<V> vs = values;
		return (vs != null ? vs : (values = new Values()));
	}
	
	private class Values extends AbstractCollection<V> {

		@Override
		public Iterator<V> iterator() {
			return new ValueIterator();
		}

		@Override
		public int size() {
			return WeakHashMap.this.size;
		}
		
		@Override
		public boolean contains(Object o) {
			return containsValue(o);
		}
		
		@Override
		public void clear() {
			WeakHashMap.this.clear();
		}
	}
	
	@Override
	public Set<Map.Entry<K, V>> entrySet() {
		Set<Map.Entry<K, V>> es = entrySet;
		return es != null ? es : (entrySet = new EntrySet());
	}
	
	private class EntrySet extends AbstractSet<Map.Entry<K, V>> {

		@Override
		public Iterator<Map.Entry<K, V>> iterator() {
			return new EntryIterator();
		}
		
		public boolean contains(Object o) {
			if(!(o instanceof Map.Entry)) {
				return false;
			}
			Map.Entry e = (Map.Entry) o;
			Object k = e.getKey();
			Entry candidate = getEntry(e.getKey());
			return candidate != null && candidate.equals(e);
		}
		
		public boolean remove(Object o) {
			return removeMapping(o) != null;
		}
		
		@Override
		public int size() {
			return WeakHashMap.this.size();
		}
		
		public void clear() {
			WeakHashMap.this.clear();
		}
	}
	
	private List<Map.Entry<K, V>> deepCopy() {
		List<Map.Entry<K, V>> list = new ArrayList<Map.Entry<K, V>>(size());
//		for(Map.Entry<K, V> e : this) {
//			list.add(new AbstractMap.SimpleEntry<K, V>(e));
//		}
		return list; 
	}
	
	public Object[] toArray() {
		return deepCopy().toArray();
	}
	
	public <T> T[] toArray(T[] a) {
		return deepCopy().toArray(a);
	}
	
}
