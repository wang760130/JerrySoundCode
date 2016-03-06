package com.jerry.soundcode.map;

import com.jerry.soundcode.set.Set;

public class WeakHashMap<K, V> extends AbstractMap<K, V> implements Map<K,V>{
	
	private static final int DEFAULT_INITIAL_CAPACITY = 16;
	
	private static final int MAXIMUM_CAPACITY = 1 << 30;
	
	private static final float DEFAULT_LOAD_FACEOR = 0.75f;
	
	private Entry[] table;
	
	private int size;
	
	private int threshold;
	
//	private final float loadFactor;
	
	
	@Override
	public Set<Map.Entry<K, V>> entrySet() {
		return null;
	}
	

}
