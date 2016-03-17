package com.jerry.soundcode.concurrent.collection;

import java.io.Serializable;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.ReentrantLock;

import com.jerry.soundcode.list.AbstractCollection;
import com.jerry.soundcode.list.Collection;
import com.jerry.soundcode.list.Enumeration;
import com.jerry.soundcode.list.Iterator;
import com.jerry.soundcode.map.AbstractMap;
import com.jerry.soundcode.map.Map;
import com.jerry.soundcode.set.AbstractSet;
import com.jerry.soundcode.set.Set;

public class ConcurrentHashMap<K, V> extends AbstractMap<K, V> 
	implements ConcurrentMap<K, V>, Serializable {

	private static final long serialVersionUID = 1L;

	static final int DEFAULT_INITIAL_CAPACITY = 16;
	
	static final float DEFAULT_LOAD_FACTOR = 0.75f;
	
	static final int DEFAULT_CONCURRENCY_LEVEL = 16;
	
	static final int MAXIMUM_CAPACITY = 1 << 30;
	
	static final int MAX_SEGMENTS = 1 << 16;
	
	static final int RETRIES_BEFORE_LOCK = 2;
	
	final int segmentMask;
	
	final int segemtnShift;
	
	final Segment<K, V>[] segments;
	
	transient Set<K> keySet;
	transient Set<Map.Entry<K, V>> entrySet;
	transient Collection<V> values;
	
	private static int hash(int h) {
		h += (h <<  15) ^ 0xffffcd7d;
        h ^= (h >>> 10);
        h += (h <<   3);
        h ^= (h >>>  6);
        h += (h <<   2) + (h << 14);
        return h ^ (h >>> 16);
	}
	
	final Segment<K, V> segmentFor(int hash) {
		return segments[(hash >>> segemtnShift) & segmentMask];
	}
	
	static final class HashEntry<K, V> {
		final K key;
		final int hash;
		volatile V value;
		final HashEntry<K, V> next;
		
		HashEntry(K key, int hash, HashEntry<K, V> next, V value) {
			this.key = key;
			this.hash = hash;
			this.next = next;
			this.value = value;
		}
		
		static final <K, V> HashEntry<K, V>[] newArray(int i) {
			return new HashEntry[i];
		}
	}
	
	static final class Segment<K, V> extends ReentrantLock implements Serializable {

		private static final long serialVersionUID = 1L;
		
		transient volatile int count;
		
		transient int modCount;
		
		transient int threshold;
		
		transient volatile HashEntry<K, V>[] table;
		
		final float loadFactor;
		
		Segment(int initialCapacity, float lf) {
			loadFactor = lf;
			setTable(HashEntry.<K,V>newArray(initialCapacity));
		}
		
		static final <K, V> Segment<K, V>[] newArray(int i) {
			return new Segment[i];
		}
		
		void setTable(HashEntry<K, V>[] newTable) {
			threshold = (int)(newTable.length * loadFactor);
			table = newTable;
		}
		
		HashEntry<K, V> getFirst(int hash) {
			HashEntry<K,V>[] tab = table;
			return tab[hash & (tab.length - 1)];
		}
		
		V readValueUnderLock(HashEntry<K, V> e) {
			lock();
			try {
				return e.value;
			} finally {
				unlock();
			}
		}
		
		V get(Object key, int hash) {
			if(count != 0) {
				HashEntry<K, V> e = getFirst(hash);
				while(e != null) {
					if(e.hash == hash && key.equals(e.key)) {
						V v = e.value;
						if(v != null) {
							return v;
						}
						return readValueUnderLock(e);
					}
					e = e.next;
				}
			}
			return null;
		}
		
		boolean containsKey(Object key, int hash) {
			if(count != 0) {
				HashEntry<K, V> e = getFirst(hash);
				while(e != null) {
					if(e.hash == hash && key.equals(e.key)) {
						return true;
					}
					e = e.next;
				}
			}
			return false;
		}
		
		boolean containsValue(Object value) {
			if(count != 0) {
				HashEntry<K,V>[] tab = table;
				int len = tab.length;
				for(int i = 0; i < len; i++) {
					for(HashEntry<K, V> e = tab[i]; e != null; e = e.next) {
						V v = e.value;
						if(v == null) {
							v = readValueUnderLock(e);
						}
						if(value.equals(v)) {
							return true;
						}
					}
				}
			}
			return false;
		}
		
		boolean replace(K key, int hash, V oldValue, V newValue) {
			lock();
			try {
				HashEntry<K, V> e = getFirst(hash);
				while(e != null && (e.hash != hash || !key.equals(e.key))) {
					e = e.next;
				}
				
				boolean replaced = false;
				if(e != null && oldValue.equals(e.value)) {
					replaced = true;
					e.value = newValue;
				}
				return replaced;
			} finally {
				unlock();
			}
		}
		
		V replace(K key, int hash, V newValue) {
			lock();
		
			try {
				HashEntry<K, V> e = getFirst(hash);
				while(e != null && (e.hash != hash || !key.equals(e.key))) {
					e = e.next;
				}
				
				V oldValue = null;
				if(e != null) {
					oldValue = e.value;
					e.value = newValue;
				}
				return oldValue;
			} finally {
				unlock();
			}
		}
		
		V put(K key, int hash, V value, boolean onlyIfAbsent) {
			lock();
			
			try {
				int c = count;
				if(c++ > threshold) {
					rehash();
				}
				HashEntry<K, V>[] tab = table;
				int index = hash & (tab.length - 1);
				HashEntry<K, V> first = tab[index];
				HashEntry<K, V> e = first;
				
				while(e != null && (e.hash != hash || !key.equals(e.key))) {
					e = e.next;
				}
				
				V oldValue;
				if(e != null) {
					oldValue = e.value;
					if(!onlyIfAbsent) {
						e.value = value;
					}
				} else {
					oldValue = null;
					++modCount;
					tab[index] = new HashEntry<K, V>(key, hash, first, value);
					count = c;
				}
				return oldValue;
			} finally {
				unlock();
			}
			
		}
		
		void rehash() {
			HashEntry<K, V>[] oldTable = table;
			int oldCapacity = oldTable.length;
			if(oldCapacity >= MAXIMUM_CAPACITY) {
				return ;
			}
			
			HashEntry<K, V>[] newTable = HashEntry.newArray(oldCapacity << 1);
			threshold = (int)(newTable.length * loadFactor);
			int sizeMask = newTable.length - 1;
			
			for(int i = 0; i < oldCapacity; i++) {
				HashEntry<K, V> e = oldTable[i];
				
				if(e != null) {
					HashEntry<K, V> next = e.next;
					int idx = e.hash & sizeMask;
					
					if(next == null) {
						newTable[idx] = e;
					} else {
						HashEntry<K, V> lastRun = e;
						int lastIdx = idx;
						for(HashEntry<K, V> last = next; last != null; last = last.next) {
							int k = last.hash * sizeMask;
							if(k != lastIdx) {
								lastIdx = k;
								lastRun = last;
							}
						}
						newTable[lastIdx] = lastRun;
						
						for(HashEntry<K, V> p = e; p != lastRun; p = p.next) {
							int k = p.hash & sizeMask;
							HashEntry<K, V> n = newTable[k];
							newTable[k] = new HashEntry<K, V> (p.key, p.hash, n, p.value);
						}
					}
				}
			}
			table = newTable;
		}
		
		V remove(Object key, int hash, Object value) {
			lock();
			try {
				int c = count - 1;
				HashEntry<K, V>[] tab = table;
				int index = hash & (tab.length - 1);
				HashEntry<K, V> first = tab[index];
				HashEntry<K, V> e = first;
				
				while(e != null && (e.hash != hash || !key.equals(e.key))) {
					e = e.next;
				}
				
				V oldValue = null;
				if(e != null) {
					V v = e.value;
					if(value == null || value.equals(v)) {
						oldValue = v;
						++modCount;
						HashEntry<K, V> newFirst = e.next;
						
						for(HashEntry<K, V> p = first; p != e; p = p.next) {
							newFirst = new HashEntry<K, V> (p.key, p.hash, newFirst, p.value);
						}
						
						tab[index] = newFirst;
						count = c;
					}
				}
				return oldValue;
			} finally {
				unlock();
			}
		}

		void clear() {
			if(count != 0) {
				lock();
				
				try {
					HashEntry<K, V>[] tab = table;
					for(int i = 0; i < tab.length; i++) {
						tab[i] = null;
					}
					++modCount;
					count = 0;
				} finally {
					unlock();
				}
			}
		}
		
		
 	}
	
	public ConcurrentHashMap(int initialCapacity, float loadFactor, int concurrencyLeven) {
		if(!(loadFactor > 0) || initialCapacity < 0 || concurrencyLeven <= 0) {
			throw new IllegalArgumentException();
		}
		
		if(concurrencyLeven > MAX_SEGMENTS) {
			concurrencyLeven = MAX_SEGMENTS;
		}
		
		int sshift = 0;
		int ssize = 1;
		while(ssize < concurrencyLeven) {
			++ sshift;
			ssize <<= 1;
		}
		
		segemtnShift = 32 - sshift;
		segmentMask = ssize - 1;
		this.segments = Segment.newArray(ssize);
		
		if(initialCapacity > MAXIMUM_CAPACITY) {
			initialCapacity = MAX_SEGMENTS;
		}
		
		int c = initialCapacity / ssize;
		if(c * ssize < initialCapacity) {
			++c;
		}
		int cap = 1;
		while(cap < c) {
			cap <<= 1;
		}
		
		for(int i = 0; i < this.segments.length; ++i) {
			this.segments[i] = new Segment<K, V>(cap, loadFactor);
		}
	}
	
	public ConcurrentHashMap(int initialCapacity, float loadFactor) {
		this(initialCapacity, loadFactor, DEFAULT_CONCURRENCY_LEVEL);
	}
	
	public ConcurrentHashMap(int initialCapacity) {
		this(initialCapacity, DEFAULT_LOAD_FACTOR, DEFAULT_CONCURRENCY_LEVEL);
	}
	
	public ConcurrentHashMap() {
		this(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR, DEFAULT_CONCURRENCY_LEVEL);
	}
	
	public ConcurrentHashMap(Map<? extends K, ? extends V> m) {
		this(Math.max((int)(m.size() / DEFAULT_LOAD_FACTOR) + 1, DEFAULT_INITIAL_CAPACITY), DEFAULT_LOAD_FACTOR, DEFAULT_CONCURRENCY_LEVEL);
	}
	
	@Override
	public boolean isEmpty() {
		final Segment<K, V>[] segments = this.segments;
		
		int[] mc = new int[segments.length];
		int mcsum = 0;
		for(int i = 0; i < segments.length; ++i) {
			if(segments[i].count != 0) {
				return false;
			} else {
				mcsum += mc[i] = segments[i].modCount;
			}
		}
		
		if(mcsum != 0) {
			for(int i = 0; i < segments.length; ++i) {
				if(segments[i].count != 0 || mc[i] != segments[i].modCount) {
					return false;
				}
			}
		}
		
		return true;
	}
	
	@Override
	public int size() {
		final Segment<K,V>[] segments = this.segments;
		long sum = 0;
		long check = 0;
		
		int[] mc = new int[segments.length];
		
		for(int k = 0; k < RETRIES_BEFORE_LOCK; ++k) {
			check = 0;
			sum = 0;
			int mcsum = 0;
			for(int i = 0; i < segments.length; ++i) {
				sum += segments[i].count;
				mcsum += mc[i] = segments[i].modCount;
			}
			if(mcsum != 0) {
				for(int i = 0; i < segments.length; ++i) {
					check += segments[i].count;
					if(mc[i] != segments[i].modCount) {
						check = -1;
						break;
					}
				}
				
				if(check == sum) {
					break;
				}
			}
		}
		
		if(check != sum) {
			sum = 0;
			for(int i = 0; i < segments.length; ++i) {
				segments[i].lock();
			}
			
			for(int i = 0; i < segments.length; ++i) {
				sum += segments[i].count;
			}
			
			for(int i = 0; i < segments.length; ++i) {
				segments[i].unlock();
			}
		}
		
		if(sum > Integer.MAX_VALUE) {
			return Integer.MAX_VALUE;
		} else {
			return (int)sum;
		}
	}
	
	@Override
	public V get(Object key) {
		int hash = hash(key.hashCode());
		return segmentFor(hash).get(key, hash);
	}
	
	@Override
	public boolean containsKey(Object key) {
		int hash = hash(key.hashCode());
		return segmentFor(hash).containsKey(key, hash);
	}
	
	@Override
	public boolean containsValue(Object value) {
		if(value == null) {
			throw new NullPointerException();
		}
		
		final Segment<K, V>[] segments = this.segments;
		int[] mc = new int[segments.length];
		
		for(int k = 0; k < RETRIES_BEFORE_LOCK; ++k) {
			int sum = 0;
			int mcsum = 0;
			
			for(int i = 0; i < segments.length; ++i) {
				int c = segments[i].count;
				mcsum += mc[i] = segments[i].modCount;
				if(segments[i].containsValue(value)) {
					return true;
				}
			}
			
			boolean cleanSweep = true;
			if(mcsum != 0) {
				for(int i = 0; i < segments.length; ++i) {
					int c = segments[i].count;
					if(mc[i] != segments[i].modCount) {
						cleanSweep = false;
						break;
					}
				}
			}
			
			if(cleanSweep) {
				return false;
			}
		}
		
		for(int i = 0; i < segments.length; i++) {
			segments[i].lock();
		}
		boolean found = false;
		
		try {
			for(int i = 0; i < segments.length; ++i) {
				if(segments[i].containsValue(value)) {
					found = true;
					break;
				}
			}
		} finally {
			for(int i = 0; i < segments.length; ++i) {
				segments[i].unlock();
			}
		}
		return found;
	}
	
	public boolean contains(Object value) {
		return containsValue(value);
	}
	
	@Override
	public V put(K key, V value) {
		if(value == null) {
			throw new NullPointerException();
		}
		int hash = hash(key.hashCode());
		return segmentFor(hash).put(key, hash, value, false);
	}
	
	@Override
	public V putIfAbsent(K key, V value) {
		if(value == null) {
			throw new NullPointerException();
		}
		int hash = hash(key.hashCode());
		return segmentFor(hash).put(key, hash, value, true);
	}
	
	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
//		for(Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
//			put(e.getKey(), e.getValue());
//		}
	}
	
	@Override
	public V remove(Object key) {
		int hash = hash(key.hashCode());
		return segmentFor(hash).remove(key, hash, null);
	
	}

	@Override
	public boolean remove(Object key, Object value) {
		int hash = hash(key.hashCode());
		if(value == null) {
			return false;
		}
		return segmentFor(hash).remove(key, hash, value) != null;
	}

	@Override
	public boolean replace(K key, V oldValue, V newValue) {
		if(oldValue == null || newValue == null) {
			throw new NullPointerException();
		}
		int hash = hash(key.hashCode());
		return segmentFor(hash).replace(key, hash, oldValue, newValue);
	}

	@Override
	public V replace(K key, V value) {
		if(value == null) {
			throw new NullPointerException();
		}
		int hash = hash(key.hashCode());
		return segmentFor(hash).replace(key, hash, value);
	}
	
	@Override
	public void clear() {
		for(int i = 0; i < segments.length; ++i) {
			segments[i].clear();
		}
	}
	
	@Override
	public Set<K> keySet() {
		Set<K> ks = keySet;
		return (ks != null) ? ks : (keySet = new KeySet());
	}
	
	@Override
	public Collection<V> values() {
		Collection<V> vs = values;
		return (vs != null) ? vs : (values = new Values());
	}
	
	// TODO
	@Override
	public Set<Map.Entry<K, V>> entrySet() {
		// TODO Auto-generated method stub
		return null;
	}
	
	abstract class HashIterator {
		int nextSegmentIndex;
		int nextTableIndex;
		
		HashEntry<K, V>[] currentTable;
		HashEntry<K, V> nextEntry;
		HashEntry<K, V> lastReturned;
		
		HashIterator() {
			nextSegmentIndex = segments.length - 1;
			nextTableIndex = -1;
			advance();
		}
		
		public boolean hasMoreElements() {
			return hasNext();
		}
		
		final void advance() {
			if(nextEntry != null && (nextEntry = nextEntry.next) != null) {
				return ;
			}
			
			while(nextTableIndex >= 0) {
				if( (nextEntry = currentTable[nextTableIndex--]) != null) {
					return ;
				}
			}
			
			while(nextSegmentIndex >= 0) {
				Segment<K, V> seg = segments[nextSegmentIndex--];
				if(seg.count != 0) {
					currentTable = seg.table;
					for(int j = currentTable.length - 1; j >= 0; --j) {
						if( (nextEntry = currentTable[j]) != null) {
							nextTableIndex = j - 1;
							return ;
						}
					}
				}
			}
		}
		
		public boolean hasNext() {
			return nextEntry != null;
		}
		
		HashEntry<K, V> nextEntry() {
			if(nextEntry == null) {
				throw new NoSuchElementException();
			}
			lastReturned = nextEntry;
			advance();
			return lastReturned;
		}
		
		public void remove() {
			if(lastReturned == null) {
				throw new IllegalArgumentException();
			}
			ConcurrentHashMap.this.remove(lastReturned.key);
			lastReturned = null;
		}
	}
	
	final class KeyIterator extends HashIterator implements Iterator<K>, Enumeration<K> {

		@Override
		public K next() {
			return super.nextEntry().key;
		}

		@Override
		public K nextElement() {
			return super.nextEntry().key;
		}
	}
	
	
	final class ValueIterator extends HashIterator implements Iterator<V>, Enumeration<V> {

		@Override
		public V next() {
			return super.nextEntry().value;
		}
		
		@Override
		public V nextElement() {
			return super.nextEntry().value;
		}
	}
	
	final class WriteThroughEntry extends AbstractMap.SimpleEntry<K, V> {

		public WriteThroughEntry(K k, V v) {
			super(k, v);
		}
		
		@Override
		public V setValue(V value) {
			if(value == null) {
				throw new NullPointerException();
			}
			
			V v = super.setValue(value);
			ConcurrentHashMap.this.put(getKey(), value);
			return v;
		}
	}
	
	final class EntryIterator extends HashIterator implements Iterator<Entry<K,V>> {

		@Override
		public Map.Entry<K, V> next() {
			HashEntry<K, V> e = super.nextEntry();
			return new WriteThroughEntry(e.key, e.value);
		}
	}
	
	final class KeySet extends AbstractSet<K> {

		@Override
		public Iterator<K> iterator() {
			return new KeyIterator();
		}

		@Override
		public int size() {
			return ConcurrentHashMap.this.size();
		}
		
		@Override
		public boolean contains(Object o) {
			return ConcurrentHashMap.this.containsKey(o);
		}
		
		@Override
		public boolean remove(Object o) {
			return ConcurrentHashMap.this.remove(o) != null;
		}
		
		@Override
		public void clear() {
			ConcurrentHashMap.this.clear();
		}
		
	}
 	
	final class Values extends AbstractCollection<V> {

		@Override
		public Iterator<V> iterator() {
			return new ValueIterator();
		}

		@Override
		public int size() {
			return ConcurrentHashMap.this.size();
		}
		
		@Override
		public boolean contains(Object o) {
			return ConcurrentHashMap.this.containsValue(o);
		}
		
		@Override
		public void clear() {
			ConcurrentHashMap.this.clear();
		}
	}
	
	// TODO
}
