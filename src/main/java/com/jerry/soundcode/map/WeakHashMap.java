package com.jerry.soundcode.map;

import com.jerry.soundcode.list.ReferenceQueue;
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
	
	@Override
	public Set<Map.Entry<K, V>> entrySet() {
		return null;
	}
	

}
