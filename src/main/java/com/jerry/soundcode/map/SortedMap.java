package com.jerry.soundcode.map;

import com.jerry.soundcode.list.Collection;
import com.jerry.soundcode.list.Comparator;
import com.jerry.soundcode.set.Set;

public interface SortedMap<K, V> extends Map<K, V> {
	
	Comparator<? super K> comparator();
	
	SortedMap<K, V> subMap(K fromKey, K toKey);
	
	SortedMap<K, V> headMap(K toKey);
	
	SortedMap<K, V> tailMap(K fromKey);
	
	K firstKey();
	
	K lastKey();
	
	Set<K> keySet();
	
	Collection<V> values();
	
	Set<Map.Entry<K, V>> entrySet();
}
