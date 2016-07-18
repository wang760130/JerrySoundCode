package com.jerry.soundcode.map;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.channels.IllegalSelectorException;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;

import com.jerry.soundcode.list.AbstractCollection;
import com.jerry.soundcode.list.Collection;
import com.jerry.soundcode.list.Comparator;
import com.jerry.soundcode.list.Iterator;
import com.jerry.soundcode.set.AbstractSet;
import com.jerry.soundcode.set.NavigableSet;
import com.jerry.soundcode.set.Set;
import com.jerry.soundcode.set.SortedSet;
import com.jerry.soundcode.set.TreeSet;

/**
 * TreeMap特点 
 * 1.利用红黑树存储结点 
 * 2.插入、删除、查找时间复杂度都是O(logn) 
 * 3.没有实现同步方法线程不安全 ，效率较高 
 * 4.结点可以按照排序输出，默认排序是key值，可以自定义排序方法
 */
public class TreeMap<K, V> extends AbstractMap<K, V>
	implements NavigableMap<K, V>, Cloneable, Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 自定义比较器，默认null，表示用key自然排序
	 */
	private final Comparator<? super K> comparator;
	
	/**
	 * 根结点
	 */
	private transient Entry<K, V> root = null;
	
	/**
	 * 结点个数
	 */
	private transient int size = 0;
	
	/**
	 * 修改次数
	 */
	private transient int modCount = 0;
	
	/**
	 * 创建一个空的构造函数
	 */
	public TreeMap() {
		comparator = null;
	}
	
	/**
	 * 创建一个比较器是 Comparator的构造函数
	 * @param comparator
	 */
	public TreeMap(Comparator<? super K> comparator) {
		this.comparator = comparator;
	}
	
	/**
	 * map m中元素加入到当前map中
     * This method runs in n*log(n) time.
	 * @param map
	 */
	public TreeMap(Map<? extends K, ? extends V> map) {
		comparator = null;
		putAll(map);
	}
	
	/**
	 * SortedMap m中元素加入到当前map中
	 * @param map
	 */
	public TreeMap(SortedMap<K, ? extends V> map) {
		comparator = map.comparator();
		try {
			buildFromSorted(map.size(), map.entrySet().iterator(), null, null);
		} catch (IOException e) {
		} catch (ClassNotFoundException e) {
		}
	}
	
	@Override
	public int size() {
		return size;
	}
	
	@Override
	public boolean containsKey(Object key) {
		return getEntry(key) != null;
	}
	
	@Override
	public boolean containsValue(Object value) {
		for(Entry<K, V> e = getFirstEntry(); e != null; e = successor(e)) {
			if(valEquals(value, e.value)) {
				return true;
			}
		}
		return false;
	} 
	
	@Override
	public V get(Object key) {
		Entry<K, V> p = getEntry(key);
		return (p == null ? null : p.value);
	}
	
	@Override
	public Comparator<? super K> comparator() {
		return comparator;
	}
	
	@Override
	public K firstKey() {
		return key(getFirstEntry());
	}

	@Override
	public K lastKey() {
		return key(getLastEntry());
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void putAll(Map<? extends K, ? extends V> map) {
		int mapSize = map.size();
		if(size == 0 && mapSize != 0 && map instanceof SortedMap) {
			Comparator c = ((SortedMap)map).comparator();
			if(c == comparator || (c != null && c.equals(comparator))) {
				++modCount;
				try {
					buildFromSorted(mapSize, map.entrySet().iterator(), null, null);
				} catch (IOException e) {
				} catch (ClassNotFoundException e) {
				}
				return ;
			}
		}
		super.putAll(map);
	}
	
	@SuppressWarnings("unchecked")
	final Entry<K, V> getEntry(Object key) {
		if(comparator != null) {
			return getEntryUsingComparator(key);
		}
		if(key == null) {
			throw new NullPointerException();
		}
		
		Comparable<? super K> k = (Comparable<? super K>) key;
		Entry<K, V> p = root;
		while(p != null) {
			int cmp = k.compareTo(p.key);
			if(cmp < 0) {
				p = p.left;
			} else if(cmp > 0) {
				p = p.right;
			} else {
				return p;
			}
		}
		
		return null;
	}
	
	@SuppressWarnings("unchecked")
	final Entry<K, V> getEntryUsingComparator(Object key) {
		K k = (K) key;
		Comparator<? super K> cpr = comparator;
		if(cpr != null) {
			Entry<K, V> p = root;
			while(p != null) {
				int cmp = cpr.compare(k, p.key);
				if(cmp < 0) {
					p = p.left;
				} else if(cmp > 0) {
					p = p.right;
				} else {
					return p;
				}
			}
		}
		
		return null;
	}
	
	/**
	 * 寻找大于等于 key
     * 若都大于 key，返回最小的结点
     * 若都小于key， 返回最大的结点
	 * @param key
	 * @return
	 */
	final Entry<K, V> getCeilingEntry(K key) {
		Entry<K, V> p = root;
		while(p != null) {
			int cmp = compare(key, p.key);
			if(cmp < 0) {
				if(p.left != null) {
					p = p.left;
				} else {
					return p;
				}
			} else if(cmp > 0) {
				if(p.right != null) {
					p = p.right;
				} else {
					Entry<K, V> parent = p.parent;
					Entry<K, V> ch = p;
					while(parent != null && ch == parent.right) {
						ch = parent;
						parent = parent.parent;
					}
					return parent;
				}
			} else  {
				return p;
			}
		}
		
		return null;
	}
	
	/**
	 * 获取小于等于key的结点
     * 若都小于key，返回最大的结点
     * 若都大于key，返回最小的结点
	 * @param key
	 * @return
	 */
	final Entry<K, V> getFloorEntry(K key) {
		Entry<K, V> p = root;
		while(p != null) {
			int cmp = compare(key, p.key);
			if(cmp > 0) {
				if(p.right != null) {
					p = p.right;
				} else {
					return p;
				}
			} else if(cmp < 0) {
				if(p.left != null) {
					p = p.left;
				} else {
					Entry<K, V> parent = p.parent;
					Entry<K, V> ch = p;
					while(parent != null && ch == parent.left) {
						ch = parent;
						parent = parent.parent;
					}
					return parent;
				}
			} else {
				return p;
			}
		}
		
		return null;
	}
	
	/**
	 * 获取大于key的结点
     * 若都大于key，返回最小值
     * 若都小于key，返回最大值
	 * @param key
	 * @return
	 */
	final Entry<K, V> getHigherEntry(K key) {
		Entry<K, V> p = root;
		while(p != null) {
			int cmp = compare(key, p.key);
			if(cmp < 0) {
				if(p.left != null) {
					p = p.left;
				} else {
					return p;
				}
			} else {
				if(p.right != null) {
					p = p.right;
				} else {
					Entry<K, V> parent = p.parent;
					Entry<K, V> ch = p;
					while(parent != null && ch == parent.right) {
						ch = parent;
						parent = parent.parent;
					}
					return parent;
				}
			}
		}
		return null;
	}
	
	/**
	 * 获取小于key的结点
     * 若都小于key，返回最大结点
     * 若都大于key，返回最小结点
	 * @param key
	 * @return
	 */
	final Entry<K, V> getLowerEntry(K key) {
		Entry<K, V> p = root;
		while(p != null) {
			int cmp = compare(key, p.key);
			if(cmp > 0) {
				if(p.right != null) {
					p = p.right;
				} else {
					return p;
				}
			} else {
				if(p.left != null) {
					p = p.left;
				} else {
					Entry<K, V> parent = p.parent;
					Entry<K, V> ch = p;
					while(parent != null && ch == parent.left) {
						ch = parent;
						parent = parent.parent;
					}
					return parent;
				}
			}
		}
		return null;
	}
	  
	@SuppressWarnings("unchecked")
	@Override
	public V put(K key, V value) {
		Entry<K, V> t = root;
		if(t == null) {
			root = new Entry<K, V> (key, value, null);
			size = 1;
			modCount ++;
			return null;
		}
		
		int cmp;
		Entry<K, V> parent;
		Comparator<? super K> cpr = comparator;
		if(cpr != null) {
			do {
				parent = t;
				cmp = cpr.compare(key, t.key);
				if(cmp < 0) {
					t = t.left;
				} else if(cmp > 0) {
					t = t.right;
				} else {
					return t.setValue(value);
				}
			} while(t != null);
		} else  {
			if(key == null) {
				throw new NullPointerException();
			}
			
			Comparable<? super K> k = (Comparable<? super K>) key;
			do {
				parent = t;
				cmp = k.compareTo(t.key);
				if(cmp < 0) {
					t = t.left;
				} else if(cmp > 0){
					t = t.right;
				} else {
					return t.setValue(value);
				}
			} while(t != null);
		}
		
		Entry<K, V> e = new Entry<K, V> (key, value, parent);
		if(cmp < 0) {
			parent.left = e;
		} else {
			parent.right = e;
		}
		fixAfterInsertion(e);
		size ++;
		modCount ++;
		
		return null;
	}
	
	@Override
	public V remove(Object key) {
		Entry<K, V> p = getEntry(key);
		if(p == null) {
			return null;
		}
		V oldValue = p.value;
		deleteEntry(p);
		return oldValue;
	}

	@Override
	public void clear() {
		modCount ++;
		size = 0;
		root = null;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Object clone() {
		TreeMap<K, V> clone = null;
		try {
			clone = (TreeMap<K, V>) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new InternalError();
		}
		
		clone.root = null;
		clone.size = 0;
		clone.modCount = 0;
		clone.entrySet = null;
		clone.navigableKeySet = null;
		clone.descendingMap = null;
		
		try {
			clone.buildFromSorted(size, entrySet().iterator(), null, null);
		} catch (IOException e) {
		} catch (ClassNotFoundException e) {
		}
		
		return clone;
	}
	
	@Override
	public Map.Entry<K, V> firstEntry() {
		return exportEntry(getFirstEntry());
	}

	@Override
	public Map.Entry<K, V> lastEntry() {
		return exportEntry(getLastEntry());
	}
	
	@Override
	public Map.Entry<K, V> pollFirstEntry() {
		Entry<K, V> p = getFirstEntry();
		Map.Entry<K, V> result = exportEntry(p);
		if(p != null) {
			deleteEntry(p);
		}
		return result;
	}

	@Override
	public Map.Entry<K, V> pollLastEntry() {
		Entry<K, V> p = getLastEntry();
		Map.Entry<K, V> result = exportEntry(p);
		if(p != null) {
			deleteEntry(p);
		}
		return result;
	}
	
	@Override
	public Map.Entry<K, V> lowerEntry(K key) {
		return exportEntry(getLowerEntry(key));
	}

	/**
	 * 获取小于key的结点 若都小于key，返回最大结点 若都大于key，返回最小结点
	 * @param key
	 * @return
	 */
	@Override
	public K lowerKey(K key) {
		return keyOrNull(getLowerEntry(key));
	}
	
	@Override
	public Map.Entry<K, V> floorEntry(K key) {
		return exportEntry(getFloorEntry(key));
	}
	
	/**
	 * 获取小于等于key的结点 若都小于key，返回最大的结点 若都大于key，返回最小的结点
	 * @param key
	 * @return
	 */
	@Override
	public K floorKey(K key) {
		return keyOrNull(getFloorEntry(key));
	}

	@Override
	public Map.Entry<K, V> ceilingEntry(K key) {
		return exportEntry(getCeilingEntry(key));
	}

	@Override
	public K ceilingKey(K key) {
		return keyOrNull(getCeilingEntry(key));
	}

	@Override
	public Map.Entry<K, V> higherEntry(K key) {
		return exportEntry(getHigherEntry(key));
	}

	/**
	 * 获取大于key的结点 若都大于key，返回最小值 若都小于key，返回最大值
	 * @param key
	 * @return
	 */
	@Override
	public K higherKey(K key) {
		return keyOrNull(getHigherEntry(key));
	}
	
	private transient EntrySet entrySet = null;
	private transient KeySet<K> navigableKeySet = null;
	private transient NavigableMap<K, V> descendingMap = null;
	
	@Override
	public Set<K> keySet() {
		return navigableKeySet();
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public NavigableSet<K> navigableKeySet() {
		KeySet<K> nks = navigableKeySet;
		return (nks != null) ? nks : (navigableKeySet = new KeySet(this));
	}

	@Override
	public NavigableSet<K> descendingKeySet() {
		return descendingMap().navigableKeySet();
	}
	
	@Override
	public Collection<V> values() {
		Collection<V> vs = values;
		return (vs != null) ? vs : (values = new Values());
	}
	
	@Override
	public Set<Map.Entry<K, V>> entrySet() {
		EntrySet es = entrySet;
		return (es != null) ? es : (entrySet = new EntrySet());
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public NavigableMap<K, V> descendingMap() {
		NavigableMap<K, V> km = descendingMap;
		return (km != null) ? km : (descendingMap = new DescendingSubMap(this, true, null, true, true, null, true));
	}
 	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, 
			K toKey, boolean toInclusive) {
		return new AscendingSubMap(this, false, fromKey, fromInclusive, false, toKey, toInclusive);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
		return new AscendingSubMap(this, true, null, true, false, toKey, inclusive);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
		return new AscendingSubMap(this, false, fromKey, inclusive, true, null, true);
	}
	
	@Override
	public final SortedMap<K, V> subMap(K fromKey, K toKey) {
		return subMap(fromKey, true, toKey, false);
	}
	
	@Override
	public SortedMap<K, V> headMap(K toKey) {
		return headMap(toKey, false);
	}
	
	@Override
	public SortedMap<K, V> tailMap(K fromKey) {
		return tailMap(fromKey, true);
	}
	
	class Values extends AbstractCollection<V> {

		@SuppressWarnings("unchecked")
		@Override
		public Iterator<V> iterator() {
			return (Iterator<V>) new ValueIterator(getFirstEntry());
		}

		@Override
		public int size() {
			return TreeMap.this.size();
		}
		
		@Override
		public boolean contains(Object o) {
			return TreeMap.this.containsValue(o);
		}
		
		@Override
		public boolean remove(Object o) {
			for(Entry<K, V> e = getFirstEntry(); e != null; e = successor(e)) {
				if(valEquals(e.getValue(), o)) {
					deleteEntry(e);
					return true;
				}
			}
			return false;
		}
	}
	
	class EntrySet extends AbstractSet<Map.Entry<K, V>> {

		@Override
		public Iterator<Map.Entry<K, V>> iterator() {
			return new EntryIterator(getFirstEntry());
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public boolean contains(Object o) {
			if(! (o instanceof Map.Entry)) {
				return false;
			}
			
			Map.Entry<K, V> entry = (Map.Entry<K, V>) o;
			V value = entry.getValue();
			Entry<K, V> p = getEntry(entry.getKey());
			return p != null && valEquals(p.getValue(), value);
		}

		@SuppressWarnings("unchecked")
		@Override
		public boolean remove(Object o) {
			if(!(o instanceof Map.Entry)) {
				return false;
			}
			Map.Entry<K, V> entry = (Map.Entry<K, V>) o;
			V value = entry.getValue();
			Entry<K, V> p = getEntry(entry.getKey());
			if(p != null && valEquals(p.getValue(), value)) {
				deleteEntry(p);
				return true;
			}
			return false;
		}
		
		@Override
		public int size() {
			return TreeMap.this.size();
		}
		
		@Override
		public void clear() {
			TreeMap.this.clear();
		}
	}
	
	Iterator<K> keyIterator() {
		return new KeyIterator(null, getFirstEntry());
	}
	
	Iterator<K> descendingKeyIterator() {
		return new DescendingKeyIterator(getLastEntry());
	}
	
	static final class KeySet<T> extends AbstractSet<T> implements NavigableSet<T> {
		
		private final NavigableMap<T, Object> m;
		
		KeySet(NavigableMap<T, Object > map) { m = map; }
		
		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
		public Iterator<T> iterator() {
			if(m instanceof TreeMap) {
				return ((TreeMap<T,Object>) m).keyIterator();
			} else {
				return (Iterator<T>)(((TreeMap.NavigableSubMap)m).keyIterator());
			}
		}
		
		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
		public Iterator<T> descendingIterator() {
			if(m instanceof TreeMap) {
				return ((TreeMap<T, Object>) m).keyIterator();
			} else {
				return (Iterator<T>)(((TreeMap.NavigableSubMap)m).keyIterator()); 
			}

		}

		@Override
		public int size() {
			return m.size();
		}
		
		@Override
		public boolean isEmpty() {
			return m.isEmpty();
		}
		
		@Override
		public boolean contains(Object o) {
			return m.containsKey(o);
		}
		
		@Override
		public void clear() {
			m.clear();
		}
		
		@Override
		public T lower(T t) {
			return m.lowerKey(t);
		}
		
		@Override
		public T floor(T t) {
			return m.floorKey(t);
		}
		
		@Override
		public T ceiling(T t) {
			return m.ceilingKey(t);
		}
		
		@Override
		public T higher(T t) {
			return m.higherKey(t);
		}
		
		@Override
		public T first() {
			return m.firstKey();
		}
		
		@Override
		public T last() {
			return m.lastKey();
		}
		
		@Override
		public Comparator<? super T> comparator() {
			return m.comparator();
		}
		
		@Override
		public T pollFirst() {
			Map.Entry<T, Object> e = m.pollFirstEntry();
			return e == null ? null : e.getKey();
		}
		
		@Override
		public T pollLast() {
			Map.Entry<T, Object> e = m.pollLastEntry();
			return e == null ? null : e.getKey();
		}
		
		@Override
		public boolean remove(Object o) {
			int oldSize = size();
			m.remove(o);
			return size() != oldSize;
		}
		
		@Override
		public NavigableSet<T> subSet(T fromElement, boolean fromInclusive,
									  T toElement,   boolean toInclusive) {
			return new TreeSet<T> (m.subMap(fromElement, fromInclusive, toElement, toInclusive));
		}
		
		@Override
		public NavigableSet<T> headSet(T toElement, boolean inclusive) {
			return new TreeSet<T> (m.headMap(toElement, inclusive));
		}

		@Override
		public NavigableSet<T> tailSet(T fromElement, boolean inclusive) {
			return new TreeSet<T> (m.tailMap(fromElement, inclusive));
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
			return new TreeSet<T> (m.tailMap(fromElement, true));
		}

		
		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
		public NavigableSet<T> descendingSet() {
			return new TreeSet(m.descendingMap());
		}
	}
	
	abstract class PrivateEntryIterator<T> implements Iterator<T> {
		Entry<K, V> next;
		Entry<K, V> lastReturned;
		int expectedModCount;
		
		PrivateEntryIterator(Entry<K, V> first) {
			expectedModCount = modCount;
			lastReturned = null;
			next = first;
		}
		
		@Override
		public final boolean hasNext() {
			return next != null;
		}
		
		final Entry<K, V> nextEntry() {
			Entry<K, V> e = next;
			if(e == null) {
				throw new NoSuchElementException();
			}
			
			if(modCount != expectedModCount) {
				throw new ConcurrentModificationException();
			}
			
			next = successor(e);
			lastReturned = e;
			
			return e;
		}
		
		final Entry<K, V> prevEntry() {
			Entry<K, V> e = next;
			if(e == null) {
				throw new NoSuchElementException();
			} 
			
			if(modCount != expectedModCount) {
				throw new ConcurrentModificationException();
			}
			
			next = predecessor(e);
			lastReturned = e;
			return e;
		} 
		
		@Override
		public void remove() {
			if(lastReturned == null) {
				throw new IllegalSelectorException();
			}
			
			if(modCount != expectedModCount) {
				throw new ConcurrentModificationException();
			}
			
			if(lastReturned.left != null && lastReturned.right != null) {
				next = lastReturned;
			}
			
			deleteEntry(lastReturned);
			expectedModCount = modCount;
			lastReturned = null;
		}
	}
	
	
	final class EntryIterator extends PrivateEntryIterator<Map.Entry<K, V>> {

		EntryIterator(Entry<K, V> first) {
			super(first);
		}
		
		public Map.Entry<K, V> next() {
			return nextEntry();
		}
	}
	
	final class ValueIterator extends PrivateEntryIterator<K> {

		ValueIterator(Entry<K, V> first) {
			super(first);
		}

		@Override
		public K next() {
			return nextEntry().key;
		}
	}
	
	final class KeyIterator extends PrivateEntryIterator<K> {

		KeyIterator(TreeMap<K, V> treeMap, Entry<K, V> first) {
			super(first);
		}

		@Override
		public K next() {
			return nextEntry().key;
		}
		
	}
	
	final class DescendingKeyIterator extends PrivateEntryIterator<K> {

		DescendingKeyIterator(Entry<K, V> first) {
			super(first);
		}

		@Override
		public K next() {
			return prevEntry().key;
		}
	}
	
	@SuppressWarnings("unchecked")
	final int compare(Object k1, Object k2) {
		return comparator==null ? ((Comparable<? super K>)k1).compareTo((K)k2)
		            : comparator.compare((K)k1, (K)k2);
	}
	
	final static boolean valEquals(Object o1, Object o2) {
		return (o1 == null ? o2 == null : o1.equals(o2));
	}
	
	static <K, V> Map.Entry<K, V> exportEntry(TreeMap.Entry<K, V> e) {
		return e == null ? null : new AbstractMap.SimpleImmutableEntry<K, V>(e);
	}
	
	static <K, V> K keyOrNull(TreeMap.Entry<K, V> e) {
		return e == null ? null : e.key;
	}
	
	static <K> K key(Entry<K, ?> e) {
		if(e == null) {
			throw new NoSuchElementException();
		}
		return e.key;
	}
	
	static abstract class NavigableSubMap<K, V> extends AbstractMap<K, V> 
		implements NavigableMap<K, V>, Serializable {
		
		private static final long serialVersionUID = 1L;

		final TreeMap<K, V> m;
		
		final K lo, hi;
		final boolean fromStart, toEnd;
		final boolean loInclusive, hiInclusive;
		
		NavigableSubMap(TreeMap<K, V> m, 
				boolean fromStart, K lo, boolean loInclusive,
				boolean toEnd, 	   K hi, boolean hiInclusive) {
			
			if(!fromStart && !toEnd) {
				if(m.compare(lo, hi) > 0) {
					throw new IllegalArgumentException("fromKey > toKey");
				}
			} else {
				if(!fromStart) {
					m.compare(lo, lo);
				} 
				if(!toEnd) {
					m.compare(hi, hi);
				}
			}
			
			this.m = m;
			this.fromStart = fromStart;
			this.lo = lo;
			this.loInclusive = loInclusive;
			this.toEnd = toEnd;
			this.hi = hi;
			this.hiInclusive = hiInclusive;
		}
		
		final boolean tooLow(Object key) {
			if(!fromStart) {
				int c = m.compare(key, lo);
				if(c < 0 || (c == 0 && !loInclusive)) {
					return true;
				}
			}
			return false;
		}
		
		final boolean tooHigh(Object key) {
			if(!toEnd) {
				int c = m.compare(key, hi);
				if(c > 0 || (c == 0 && !hiInclusive)) {
					return true;
				}
			}
			return false;
		}
		
		final boolean inRange(Object key) {
			return !tooLow(key) && !tooHigh(key);
		}
		
		final boolean inClosedRange(Object key) {
			return (fromStart || m.compare(key, lo) >= 0) 
					&& (toEnd || m.compare(hi, key) >= 0);
		}
		
		final boolean inRange(Object key, boolean inclusive) {
			return inclusive ? inRange(key) : inClosedRange(key);
		}
		
		final TreeMap.Entry<K, V> absLowest() {
			TreeMap.Entry<K, V> e = (fromStart ? m.getFirstEntry() : 
				(loInclusive ? m.getCeilingEntry(lo) : m.getHigherEntry(lo)));
			return (e == null || tooHigh(e.key)) ? null : e;
		}
		
		final TreeMap.Entry<K, V> absHighest() {
			TreeMap.Entry<K, V> e = (toEnd ? m.getLastEntry() : 
				(hiInclusive ? m.getFloorEntry(hi) : m.getLowerEntry(hi)));
			return (e == null || tooLow(e.key)) ? null : e;
		}
		
		final TreeMap.Entry<K, V> absCeiling(K key) {
			if(tooLow(key)) {
				return absLowest();
			}
			TreeMap.Entry<K, V> e = m.getCeilingEntry(key);
			return (e == null || tooHigh(key)) ? null : e;
		}
		
		final TreeMap.Entry<K, V> absHigher(K key) {
			if(tooLow(key)) {
				return absLowest();
			}
			TreeMap.Entry<K, V> e = m.getHigherEntry(key);
			return (e == null || tooHigh(e.key)) ? null : e;
		}
		
		final TreeMap.Entry<K, V> absFloor(K key) {
			if(tooHigh(key)) {
				return absHighest();
			}
			
			TreeMap.Entry<K, V> e = m.getFloorEntry(key);
			return (e == null || tooLow(e.key)) ? null : e;
		}
		
		final TreeMap.Entry<K, V> absLower(K key) {
			if(tooHigh(key)) {
				return absHighest();
			}
			
			TreeMap.Entry<K, V> e = m.getLowerEntry(key);
			return (e == null || tooLow(e.key)) ? null : e;
		}
		
		final TreeMap.Entry<K, V> absHighFence() {
			return (toEnd ? null : (hiInclusive ? m.getHigherEntry(hi) : m.getCeilingEntry(hi)));
		}
		
		final TreeMap.Entry<K, V> absLowFence() {
			return (fromStart ? null : (loInclusive ? m.getLowerEntry(lo) : m.getFloorEntry(lo)));
		}
		
		abstract TreeMap.Entry<K, V> subLowest();
		abstract TreeMap.Entry<K, V> subHighest();
		abstract TreeMap.Entry<K, V> subCeiling(K key);
		abstract TreeMap.Entry<K, V> subHigher(K key);
		abstract TreeMap.Entry<K, V> subFloor(K key);
		abstract TreeMap.Entry<K, V> subLower(K key);
		
		abstract Iterator<K> keyIterator();
		
		abstract Iterator<K> descendingKeyIterator();
		
		@Override
		public boolean isEmpty() {
			return (fromStart && toEnd) ? m.isEmpty() : entrySet().isEmpty();
		}
	
		@Override
		public int size() {
			return (fromStart && toEnd) ? m.size() :entrySet().size();
		}
		
		@Override
		public final boolean containsKey(Object key) {
			return inRange(key) && m.containsKey(key);
		}
		
		@Override
		public final V put(K key, V value) {
			if(!inRange(key)) {
				throw new IllegalArgumentException("key out of range");
			}
			return m.put(key, value);
		}
		
		@Override
		public final V get(Object key) {
			return !inRange(key) ? null : m.get(key);
		}
		
		@Override
		public final V remove(Object key) {
			return !inRange(key) ? null : m.remove(key);
		}
		
		@Override
		public final Map.Entry<K, V> ceilingEntry(K key) {
			return exportEntry(subCeiling(key));
		}
		
		@Override
		public final K ceilingKey(K key) {
			return keyOrNull(subCeiling(key));
		}
		
		@Override
		public final Map.Entry<K, V> higherEntry(K key) {
			return exportEntry(subHigher(key));
		}
		
		@Override
		public final K higherKey(K key) {
			return keyOrNull(subHigher(key));
		}
		
		@Override
		public final Map.Entry<K, V> floorEntry(K key) {
			return exportEntry(subFloor(key));
		}
		
		@Override
		public final K floorKey(K key) {
			return keyOrNull(subFloor(key));
		}
		
		@Override
		public final Map.Entry<K, V> lowerEntry(K key) {
			return exportEntry(subLower(key));
		}
		
		@Override
		public final K lowerKey(K key) {
			return keyOrNull(subLower(key));
		}
		
		@Override
		public K firstKey() {
			return key(subLowest());
		}
		
		@Override
		public final K lastKey() {
			return key(subHighest());
		}

		@Override
		public final Map.Entry<K, V> firstEntry() {
			return exportEntry(subLowest());
		}
		
		@Override
		public final Map.Entry<K, V> lastEntry() {
			return exportEntry(subHighest());
		}
		
		@Override
		public final Map.Entry<K, V> pollFirstEntry() {
			TreeMap.Entry<K, V> e = subLowest();
			Map.Entry<K, V> result = exportEntry(e);
			if(e != null) {
				m.deleteEntry(e);
			}
			return result;
		}
		
		@Override
		public final Map.Entry<K, V> pollLastEntry() {
			TreeMap.Entry<K, V> e = subHighest();
			Map.Entry<K, V> result = exportEntry(e);
			if(e != null) {
				m.deleteEntry(e);
			}
			return result;
		}
		
		transient NavigableMap<K, V> descendingMapView = null;
		transient EntrySetView entrySetView = null;
		transient KeySet<K> navigableKeySetView = null;
		
		@SuppressWarnings({ "rawtypes", "unchecked" })
		@Override
		public NavigableSet<K> navigableKeySet() {
			KeySet<K> nksv = navigableKeySetView;
			return (nksv != null) ? nksv : (navigableKeySetView = new TreeMap.KeySet(this));
		}
		
		@Override
		public  Set<K> keySet() {
			return navigableKeySet();
		}
		
		@Override
		public NavigableSet<K> descendingKeySet() {
			return descendingMap().navigableKeySet();
		}
		
		@Override
		public  SortedMap<K, V> subMap(K fromKey, K toKey) {
			return subMap(fromKey, true, toKey, false);
		}
		
		@Override
		public  SortedMap<K, V> headMap(K toKey) {
			return headMap(toKey, false);
		}
		
		@Override
		public  SortedMap<K, V> tailMap(K fromKey) {
			return tailMap(fromKey, true);
		}
		
		abstract class EntrySetView extends AbstractSet<Map.Entry<K, V>> {
			private transient int size = -1, sizeModCount;
			
			@SuppressWarnings("rawtypes")
			public int size() {
				if(fromStart && toEnd) {
					return m.size();
				}
				
				if(size == -1 || sizeModCount != m.modCount) {
					sizeModCount = m.modCount;
					size = 0;
					Iterator it = iterator();
					while(it.hasNext()) {
						size ++;
						it.next();
					}
				}
				return size;
			}
			
			public boolean isEmpty() {
				TreeMap.Entry<K, V> n = absLowest();
				return n == null || tooHigh(n.key);
			}
			
			@SuppressWarnings({ "unchecked", "rawtypes" })
			public boolean contains(Object o) {
				if(! (o instanceof Map.Entry)) {
					return false;
				}
				
				Map.Entry<K, V> entry = (Map.Entry<K, V>) o;
				K key = entry.getKey();
				if(!inRange(key)) {
					return false;
				}
				
				TreeMap.Entry node = m.getEntry(key);
				return node != null && valEquals(node.getValue(), entry.getValue());
			}
			
			@SuppressWarnings("unchecked")
			public boolean remove(Object o) {
				if(!(o instanceof Map.Entry)) {
					return false;
				}
				Map.Entry<K, V> entry = (Map.Entry<K, V>) o;
				K key = entry.getKey();
				if(!inRange(key)) {
					return false;
				}
				
				TreeMap.Entry<K, V> node = m.getEntry(key);
				if(node != null && valEquals(node.getValue(), entry.getValue())) {
					m.deleteEntry(node);
					return true;
				}
				return false;
			}
		}
		
		abstract class SubMapIterator<T> implements Iterator<T> {
			TreeMap.Entry<K, V> lastReturned;
			TreeMap.Entry<K, V> next;
			final K fenceKey;
			int expectedModCount;
			
			SubMapIterator(TreeMap.Entry<K, V> first, TreeMap.Entry<K,V> fence) {
				expectedModCount = m.modCount;
				lastReturned = null;
				next = first;
				fenceKey = fence == null ? null : fence.key;
			}
			
			public final boolean hasNext() {
				return next != null && next.key != fenceKey;
			}
			
			final TreeMap.Entry<K, V> nextEntry() {
				TreeMap.Entry<K, V> e = next;
				if(e == null || e.key == fenceKey) {
					throw new NoSuchElementException();
				}
				if(m.modCount != expectedModCount) {
					throw new ConcurrentModificationException();
				}
				next = successor(e);
				lastReturned = e;
				return e ;
			}
			
			final TreeMap.Entry<K, V> prevEntry() {
				TreeMap.Entry<K, V> e = next;
				if(e == null || e.key == fenceKey) {
					throw new NoClassDefFoundError();
				}
				if(m.modCount != expectedModCount) {
					throw new ConcurrentModificationException();
				}
				next = predecessor(e);
				lastReturned = e;
				return e;
			}
			
			final void removeAscending() {
				if(lastReturned == null) {
					throw new IllegalSelectorException();
				}
				if(m.modCount != expectedModCount) {
					throw new ConcurrentModificationException();
				}
				
				if(lastReturned.left != null && lastReturned.right != null) {
					next = lastReturned;
				}
				m.deleteEntry(lastReturned);
				lastReturned = null;
				expectedModCount = m.modCount;
			}
			
			final void removeDescending() {
				if(lastReturned == null) {
					throw new IllegalSelectorException();
				}
				if(m.modCount != expectedModCount) {
					throw new ConcurrentModificationException();
				}
				m.deleteEntry(lastReturned);
				lastReturned = null;
				expectedModCount = m.modCount;
			}
		}
		
		final class SubMapEntryIterator extends SubMapIterator<Map.Entry<K, V>> {

			SubMapEntryIterator(TreeMap.Entry<K, V> first,TreeMap.Entry<K, V> fence) {
				super(first, fence);
			}

			@Override
			public Map.Entry<K, V> next() {
				return nextEntry();
			}

			@Override
			public void remove() {
				removeAscending();
			}
		}
		
		final class SubMapKeyIterator extends SubMapIterator<K> {

			SubMapKeyIterator(TreeMap.Entry<K, V> first,TreeMap.Entry<K, V> fence) {
				super(first, fence);
			}

			@Override
			public K next() {
                return nextEntry().key;
            }

			@Override
			public void remove() {
				removeAscending();
			}
		}
		
		final class DescendingSubMapEntryIterator extends SubMapIterator<Map.Entry<K, V>> {

			DescendingSubMapEntryIterator(TreeMap.Entry<K, V> first,TreeMap.Entry<K, V> fence) {
				super(first, fence);
			}

			@Override
			public Map.Entry<K, V> next() {
				return prevEntry();
			}

			@Override
			public void remove() {
				removeDescending();
			}
			
		}
		
		final class DescendingSubMapKeyIterator extends SubMapIterator<K> {

			DescendingSubMapKeyIterator(TreeMap.Entry<K, V> last,TreeMap.Entry<K, V> fence) {
				super(last, fence);
			}

			@Override
			public K next() {
				return prevEntry().key;
			}

			@Override
			public void remove() {
				removeDescending();
			}
		}
	}
	
	static final class AscendingSubMap<K, V> extends NavigableSubMap<K, V> {

		private static final long serialVersionUID = 1L;

		AscendingSubMap(TreeMap<K, V> m, boolean fromStart, K lo,
				boolean loInclusive, boolean toEnd, K hi,
				boolean hiInclusive) {
			super(m, fromStart, lo, loInclusive, toEnd, hi, hiInclusive);
		}

		@Override
		public Comparator<? super K> comparator() {
			return m.comparator();
		}
		
		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
		public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive,
				K toKey, boolean toInclusive) {
			if(!inRange(fromKey, fromInclusive)) {
				throw new IllegalArgumentException("fromKey out of range");
			}
			
			if(!inRange(toKey, toInclusive)) {
				throw new IllegalArgumentException("toKey out of range");
			}
			
			return new AscendingSubMap(m, false, fromKey, fromInclusive, false, toKey, toInclusive);
		}
		
		@SuppressWarnings({ "rawtypes", "unchecked" })
		@Override
		public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
			if(!inRange(toKey, inclusive)) {
				throw new IllegalArgumentException("toKey out of range");
			}
			return new AscendingSubMap(m, fromStart, lo, loInclusive, false, toKey, inclusive);
		}
		
		@SuppressWarnings({ "rawtypes", "unchecked" })
		@Override
		public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
			if (!inRange(fromKey, inclusive))
				throw new IllegalArgumentException("fromKey out of range");
			return new AscendingSubMap(m, false, fromKey, inclusive, toEnd, hi, hiInclusive);
		}
		
		@SuppressWarnings({ "rawtypes", "unchecked" })
		@Override
		public NavigableMap<K, V> descendingMap() {
			NavigableMap<K, V> mv = descendingMapView;
			return (mv != null) ? mv : (descendingMapView = new DescendingSubMap(m, fromStart, lo, loInclusive, toEnd, hi, hiInclusive));
		}
		
		@Override
		Iterator<K> keyIterator() {
			return new SubMapKeyIterator(absLowest(), absHighFence());
		}
		
		@Override
		Iterator<K> descendingKeyIterator() {
			return new DescendingSubMapKeyIterator(absHighest(), absLowFence());
		}
		
		final class AscendingEntrySetView extends EntrySetView{
			public Iterator<Map.Entry<K, V>> iterator() {
				return new SubMapEntryIterator(absLowest(), absHighest());
			}
		}
		
		@Override
		public Set<Map.Entry<K, V>> entrySet() {
			EntrySetView es = entrySetView;
			return (es != null) ? es : new AscendingEntrySetView();
		}

		@Override
		TreeMap.Entry<K, V> subLowest() {
			return absLowest();
		}

		@Override
		TreeMap.Entry<K, V> subHighest() {
			return absHighest();
		}

		@Override
		TreeMap.Entry<K, V> subCeiling(K key) {
			return absCeiling(key);
		}

		@Override
		TreeMap.Entry<K, V> subHigher(K key) {
			return absHigher(key);
		}

		@Override
		TreeMap.Entry<K, V> subFloor(K key) {
			return absFloor(key);
		}

		@Override
		TreeMap.Entry<K, V> subLower(K key) {
			return absLower(key);
		}

	}
	
	
	@SuppressWarnings("serial")
	static final class DescendingSubMap<K, V> extends NavigableSubMap<K, V> {
		
		public DescendingSubMap(TreeMap<K,V> m, 
				boolean fromStart, K lo, boolean loInclusive,
				boolean toEnd, 	   K hi, boolean hiInclusive) {
			super(m, fromStart, lo, loInclusive, toEnd, hi, hiInclusive);
		}
		
	
		@Override
		public NavigableMap<K, V> descendingMap() {
			return null;
		}

		@Override
		public Comparator<? super K> comparator() {
			return null;
		}

		@Override
		public SortedMap<K, V> subMap(K fromKey, K toKey) {
			return null;
		}

		@Override
		public SortedMap<K, V> headMap(K toKey) {
			return null;
		}

		@Override
		public SortedMap<K, V> tailMap(K fromKey) {
			return null;
		}

		@Override
		public K firstKey() {
			return null;
		}

		@Override
		public Set<Map.Entry<K, V>> entrySet() {
			return null;
		}

		@Override
		public NavigableSet<K> navigableKeySet() {
			return null;
		}

		@Override
		public NavigableSet<K> descendingKeySet() {
			return null;
		}

		@Override
		public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive,
				K toKey, boolean toInclusive) {
			return null;
		}

		@Override
		public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
			return null;
		}

		@Override
		public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
			return null;
		}

		@Override
		TreeMap.Entry<K, V> subLowest() {
			return null;
		}

		@Override
		TreeMap.Entry<K, V> subHighest() {
			return null;
		}

		@Override
		TreeMap.Entry<K, V> subCeiling(K key) {
			return null;
		}

		@Override
		TreeMap.Entry<K, V> subHigher(K key) {
			return null;
		}

		@Override
		TreeMap.Entry<K, V> subFloor(K key) {
			return null;
		}

		@Override
		TreeMap.Entry<K, V> subLower(K key) {
			return null;
		}

		@Override
		Iterator<K> keyIterator() {
			return null;
		}

		@Override
		Iterator<K> descendingKeyIterator() {
			return null;
		}
	}
	
	@SuppressWarnings({ "unused", "serial" })
	private class SubMap extends AbstractMap<K, V> 
		implements SortedMap<K, V> , Serializable {
		
		private boolean fromStart = false, toEnd = false;
		private K fromKey, toKey;
		
		@SuppressWarnings({ "rawtypes", "unchecked" })
		private Object readResolve() {
			return new AscendingSubMap(TreeMap.this, fromStart, fromKey, true, toEnd, toKey, false);
		}
		
		@Override
		public Set<Map.Entry<K,V>> entrySet() { throw new InternalError(); }
        
		@Override
		public K lastKey() { throw new InternalError(); }
        
		@Override
		public K firstKey() { throw new InternalError(); }
        
		@Override
		public SortedMap<K,V> subMap(K fromKey, K toKey) { throw new InternalError(); }
        
		@Override
		public SortedMap<K,V> headMap(K toKey) { throw new InternalError(); }
        
		@Override
		public SortedMap<K,V> tailMap(K fromKey) { throw new InternalError(); }
        
		@Override
		public Comparator<? super K> comparator() { throw new InternalError(); }
		
	}
	
	private static final boolean RED = false;
	private static final boolean BLACK = true;
	
	static final class Entry<K, V> implements Map.Entry<K, V> {
		
		K key;
		V value;
		Entry<K, V> left = null;
		Entry<K, V> right = null;
		Entry<K, V> parent;
		boolean color = BLACK;
		
		Entry(K key, V value, Entry<K, V> parent) {
			this.key = key;
			this.value = value;
			this.parent = parent;
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
			V oldValue = this.value;
			this.value = value;
			return oldValue;
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public boolean equals(Object o) {
			if(! (o instanceof Map.Entry)) {
				return false;
			}
			
			Map.Entry<K, V> e = (Map.Entry<K, V>) o;
			return valEquals(key, e.getKey()) && valEquals(value, e.getValue());
		}
		
		@Override
		public int hashCode() {
			int keyHash = (key == null ? 0 : key.hashCode());
			int valueHash = (value == null ? 0 : value.hashCode());
			return keyHash ^ valueHash;
		}
		
		@Override
		public String toString() {
			return key + "=" + value;
		}
	}
	
	final Entry<K, V> getFirstEntry() {
		Entry<K, V> p = root;
		if(p != null) {
			while(p.left != null) {
				p = p.left;
			}
		}
		return p;
	}
	
	final Entry<K, V> getLastEntry() {
		Entry<K, V> p = root;
		if(p != null) {
			while(p.right != null) {
				p = p.right;
			}
		}
		return p;
	}
	
	static <K, V> TreeMap.Entry<K, V> successor(Entry<K, V> e) {
		if(e == null) {
			return null;
		} else if(e.right != null) {
			Entry<K, V> p = e.right;
			while(p.left != null) {
				p = p.left;
			}
			return p;
		} else {
			Entry<K, V> p = e.parent;
			Entry<K, V> ch = e;
			while(p != null && ch == p.right) {
				ch = p;
				p = p.parent;
			}
			return p;
		}
	}
	
	static <K, V> Entry<K, V> predecessor(Entry<K, V> t) {
		if(t == null) {
			return null;
		} else if(t.left != null) {
			Entry<K, V> p = t.left;
			while(p.right != null) {
				p = p.right;
			}
			return p;
		} else {
			Entry<K, V> p = t.parent;
			Entry<K, V> ch = t;
			while(p != null && ch == p.left) {
				ch = p;
				p = p.parent;
			}
			return p;
		}
		
	}
	
	private static <K, V> boolean colorOf(Entry<K, V> p) {
		return (p == null ? BLACK : p.color); 
	}
	
	private static <K, V> Entry<K, V> parentOf(Entry<K, V> p) {
		return (p == null ? null : p.parent);
	}
	
	private static <K, V> void setColor(Entry<K, V> p, boolean c) {
		if(p != null) {
			p.color = c;
		}
	}
	
	private static <K, V> Entry<K, V> leftOf(Entry<K, V> p) {
		return (p == null) ? null : p.left;
	}

	private static <K, V> Entry<K, V> rightOf(Entry<K, V> p) {
		return (p == null) ? null : p.right;
	}
	
	private void rotateLeft(Entry<K, V> p) {
		if(p != null) {
			Entry<K, V> r = p.right;
			p.right = r.left;
			if(r.left != null) {
				r.parent = p.parent;
			}
			if(p.parent == null) {
				root = r;
			} else if(p.parent.left == p) {
				p.parent.left = r;
			} else {
				p.parent.right = r;
			}
			r.left = p;
			p.parent = r;
		}
	}
	
	private void rotateRight(Entry<K, V> p) {
		if(p != null) {
			Entry<K, V> l = p.left;
			if(l.right != null) {
				l.right.parent = p;
			}
			l.parent = p.parent;
			if(p.parent == null) {
				root = l;
			} else if(p.parent.right == p) {
				p.parent.right = l;
			} else {
				p.parent.left = l;
			}
			l.right = p;
			p.parent = l;
		}

	}
	
	
	private void fixAfterInsertion(Entry<K, V> x) {
		x.color = RED;
		
		while(x != null && x != root && x.parent.color == RED) {
			if(parentOf(x) == leftOf(parentOf(parentOf(x)))) {
				Entry<K, V> y = rightOf(parentOf(parentOf(x)));
				if(colorOf(y) == RED) {
					setColor(parentOf(x), BLACK);
					setColor(y, BLACK);
					setColor(parentOf(x), RED);
					x = parentOf(parentOf(x));
				} else {
					if(x == rightOf(parentOf(x))) {
						x = parentOf(x);
						rotateLeft(x);
					}
					setColor(parentOf(x), BLACK);
					setColor(parentOf(parentOf(x)), RED);
					rotateRight(parentOf(parentOf(x)));
				}
			} else {
				Entry<K, V> y = leftOf(parentOf(parentOf(x)));
				if(colorOf(y) == RED) {
					setColor(parentOf(x), BLACK);
					setColor(y, BLACK);
					setColor(parentOf(parentOf(x)), RED);
					x = parentOf(parentOf(x));
				} else {
					if(x == leftOf(parentOf(x))) {
						x = parentOf(x);
						rotateRight(x);
					}
					setColor(parentOf(x), BLACK);
					setColor(parentOf(parentOf(x)), RED);
					rotateLeft(parentOf(parentOf(x)));
				}
			}
		}
		root.color = BLACK;
	}
	
	private void deleteEntry(Entry<K, V> p) {
		modCount ++;
		size --;
		
		if(p.left != null && p.right != null) {
			Entry<K, V> s = successor(p);
			p.key = s.key;
			p.value = s.value;
			p = s;
		}
		
		Entry<K, V> replacement = (p.left != null ? p.left : p.right);
		
		if(replacement != null) {
			replacement.parent = p.parent;
			if(p.parent == null) {
				root = replacement;
			} else if(p == p.parent.left) {
				p.parent.left = replacement;
			} else {
				p.parent.right = replacement;
			}
			
			p.left = p.right = p.parent = null;
			if(p.color == BLACK) {
				fixAfterDeletion(replacement);
			}
		} else if(p.parent == null) {
			root = null;
		} else {
			if(p.color == BLACK) {
				fixAfterDeletion(p);
			}
			
			if(p.parent != null) {
				if(p == p.parent.left) {
					p.parent.left = null;
				} else if(p == p.parent.right) {
					p.parent.right = null;
				}
				p.parent = null;
			}
		}
	}

	private void fixAfterDeletion(Entry<K, V> x) {
		while(x != root && colorOf(x) == BLACK) {
			if(x == leftOf(parentOf(x))) {
				Entry<K, V> sib = rightOf(parentOf(x));
				
				if(colorOf(sib) == RED) {
					setColor(sib, BLACK);
					setColor(parentOf(x), RED);
					rotateLeft(parentOf(x));
					sib = rightOf(parentOf(x));
				}
				
				if(colorOf(leftOf(sib)) == BLACK && colorOf(rightOf(sib)) == BLACK) {
					setColor(sib, RED);
					x = parentOf(x);
				} else {
					if(colorOf(rightOf(sib)) == BLACK) {
						setColor(leftOf(sib), BLACK);
						setColor(sib, RED);
						rotateRight(sib);
						sib = rightOf(parentOf(x));
					}
					
					setColor(sib, colorOf(parentOf(x)));
					setColor(parentOf(x), BLACK);
					setColor(rightOf(sib), BLACK);
					rotateLeft(parentOf(x));
					x = parentOf(x);
				}
			} else {
				Entry<K, V> sib = leftOf(parentOf(x));
				
				if(colorOf(sib) == RED) {
					setColor(sib, BLACK);
					setColor(parentOf(x), RED);
					rotateRight(parentOf(x));
					sib = leftOf(parentOf(x));
				}
				
				if(colorOf(rightOf(sib)) == BLACK && colorOf(leftOf(sib)) == BLACK) {
					setColor(sib, RED);
					x = parentOf(x);
				} else {
					if(colorOf(leftOf(sib)) == BLACK) {
						setColor(rightOf(sib), BLACK);
						setColor(sib, RED);
						rotateLeft(sib);
						sib = leftOf(parentOf(x));
					}
					setColor(sib, colorOf(parentOf(x)));
					setColor(parentOf(x), BLACK);
					setColor(leftOf(sib), BLACK);
					rotateRight(parentOf(x));
					x = root;
				}
			}
		}
		setColor(x, BLACK);
	}
	
	private void writeObject(ObjectOutputStream s) throws IOException, ClassNotFoundException {
		s.defaultWriteObject();
		s.writeInt(size);
		
		for(Iterator<Map.Entry<K, V>> it = entrySet().iterator(); it.hasNext();) {
			Map.Entry<K, V> e = it.next();
			s.writeObject(e.getKey());
			s.writeObject(e.getValue());
		}
	}
	
	private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
		s.defaultReadObject();
		
		int size = s.readInt();
		
		buildFromSorted(size, null, s, null);
	}
	
	public void readTreeSet(int size, ObjectInputStream s, V defaultVal) throws IOException, ClassNotFoundException {
		buildFromSorted(size, null, s, defaultVal);
	}
	
	public void addAllForTreeSet(SortedSet<? extends K> set, V defaultVal) {
		try {
			buildFromSorted(set.size(), set.iterator(), null, defaultVal);
		} catch (IOException e) {
		} catch (ClassNotFoundException e) {
		}
	}
	
	@SuppressWarnings("rawtypes")
	private void buildFromSorted(int size, Iterator it, ObjectInputStream is, V defaultVal) throws IOException, ClassNotFoundException {
		this.size = size;
		root = buildFromSorted(0, 0, size - 1, computeRedLevel(size), it, is, defaultVal);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private final Entry<K, V> buildFromSorted(int level, int lo, int hi, 
			int redLevel, Iterator it, ObjectInputStream is, V defaultVal) throws IOException, ClassNotFoundException {
		if(hi < lo) {
			return null;
		}
		
		int mid = (lo + hi) / 2;
		
		Entry<K, V> left = null;
		
		if(lo < mid) {
			left = buildFromSorted(level + 1, lo, mid - 1, redLevel, it, is, defaultVal);
		}
		
		K key;
		V value;
		if(it != null) {
			if(defaultVal == null) {
				Map.Entry<K, V> entry = (Map.Entry<K, V>) it.next();
				key = entry.getKey();
				value = entry.getValue();
			} else {
				key = (K) it.next();
				value = defaultVal;
			}
		} else {
			key = (K) is.readObject();
			value = (defaultVal != null ? defaultVal : (V) is.readObject());
		}
		
		Entry<K, V> middle = new Entry<K, V> (key, value, null);
		
		if(level == redLevel) {
			middle.color = RED;
		}
	
		if(left != null) {
			middle.left = left;
			left.parent = middle;
		}
		
		if(mid < hi) {
			Entry<K, V> right = buildFromSorted(level + 1, mid + 1, hi, redLevel, it, is, defaultVal);
			middle.right = right;
			right.parent = middle;
		}
		
		return middle;
	}
	
	private static int computeRedLevel(int sz) {
		int level = 0;
		for(int m = sz - 1; m >= 0; m = m / 2 -1) {
			level ++;
		}
		return level;
	}
}
