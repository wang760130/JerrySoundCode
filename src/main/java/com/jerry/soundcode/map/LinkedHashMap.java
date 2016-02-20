package com.jerry.soundcode.map;


public class LinkedHashMap<K, V> extends HashMap<K, V> implements Map<K,V> {

	private static final long serialVersionUID = 1L;

	private transient Entry<K,V> header;

	private final boolean accessOrder;
	
	public LinkedHashMap(int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
		accessOrder = false;
	}
	
	public LinkedHashMap(int initialCapacity) {
		super(initialCapacity);
		accessOrder = false;
	}
	
	public LinkedHashMap() {
		super();
		accessOrder = false;
	}
	
	public LinkedHashMap(Map<? extends K, ? extends V> m) {
		super(m);
		accessOrder = false;
	}
	
	public LinkedHashMap(int initialCapacity, float loadFactor, boolean accessOrder) {
		super(initialCapacity, loadFactor);
		this.accessOrder = accessOrder;
	}
	
	private static class Entry<K, V> extends HashMap.Entry<K, V> {

		Entry<K, V> before, after;
		
		Entry(int hash, K key, V value, HashMap.Entry<K, V> next) {
			super(hash, key, value, next);
		}
	}

}
