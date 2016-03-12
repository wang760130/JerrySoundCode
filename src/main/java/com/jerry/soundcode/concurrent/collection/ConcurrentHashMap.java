package com.jerry.soundcode.concurrent.collection;

import java.io.Serializable;
import java.util.concurrent.locks.ReentrantLock;

import com.jerry.soundcode.list.Collection;
import com.jerry.soundcode.map.AbstractMap;
import com.jerry.soundcode.map.Map;
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
	
//	final int segmentMask;
	
//	final int segemtnShift;
	
//	final Segment<K, V>[] segments;
	
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
	
	
	@Override
	public V putIfAbsent(K key, V value) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean remove(Object key, Object value) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean replace(K key, V oldValue, V newValue) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public V replace(K key, V value) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<com.jerry.soundcode.map.Map.Entry<K, V>> entrySet() {
		// TODO Auto-generated method stub
		return null;
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
		
		
	}
}
