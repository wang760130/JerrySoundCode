package com.jerry.soundcode.map;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.channels.IllegalSelectorException;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;
import java.util.TreeSet;

import com.jerry.soundcode.list.AbstractCollection;
import com.jerry.soundcode.list.Collection;
import com.jerry.soundcode.list.Comparator;
import com.jerry.soundcode.list.Iterator;
import com.jerry.soundcode.set.AbstractSet;
import com.jerry.soundcode.set.NavigableSet;
import com.jerry.soundcode.set.Set;
import com.jerry.soundcode.set.SortedSet;

public class TreeMap<K, V> extends AbstractMap<K, V>
	implements NavigableMap<K, V>, Cloneable, Serializable {

	private static final long serialVersionUID = 1L;

	private final Comparator<? super K> comparator;
	
	private transient Entry<K, V> root = null;
	
	private transient int size = 0;
	
	private transient int modCount = 0;
	
	public TreeMap() {
		comparator = null;
	}
	
	public TreeMap(Comparator<? super K> comparator) {
		this.comparator = comparator;
	}
	
	public TreeMap(Map<? extends K, ? extends V> map) {
		comparator = null;
		putAll(map);
	}
	
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
	
	public boolean containsKey(Object key) {
		return getEntry(key) != null;
	}
	
	public boolean containsValue(Object value) {
		for(Entry<K, V> e = getFirstEntry(); e != null; e = successor(e)) {
			if(valEquals(value, e.value)) {
				return true;
			}
		}
		return false;
	} 
	
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
	
	public V remove(Object key) {
		Entry<K, V> p = getEntry(key);
		if(p == null) {
			return null;
		}
		V oldValue = p.value;
		deleteEntry(p);
		return oldValue;
	}

	public void clear() {
		modCount ++;
		size = 0;
		root = null;
	}
	
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

	@Override
	public K lowerKey(K key) {
		return keyOrNull(getLowerEntry(key));
	}
	
	@Override
	public Map.Entry<K, V> floorEntry(K key) {
		return exportEntry(getFloorEntry(key));
	}
	
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

	@Override
	public K higherKey(K key) {
		return keyOrNull(getHigherEntry(key));
	}
	
	private transient EntrySet entrySet = null;
	private transient KeySet<K> navigableKeySet = null;
	private transient NavigableMap<K, V> descendingMap = null;
	
	public Set<K> keySet() {
		return navigableKeySet();
	}
	
	public NavigableSet<K> navigableKeySet() {
		KeySet<K> nks = navigableKeySet;
		return (nks != null) ? nks : (navigableKeySet = new KeySet(this));
	}

	public NavigableSet<K> descendingKeySet() {
		return descendingMap().navigableKeySet();
	}
	
	public Collection<V> values() {
		Collection<V> vs = values;
		return (vs != null) ? vs : (values = new Values());
	}
	
	public Set<Map.Entry<K, V>> entrySet() {
		EntrySet es = entrySet;
		return (es != null) ? es : (entrySet = new EntrySet());
	}
	
	@Override
	public NavigableMap<K, V> descendingMap() {
		NavigableMap<K, V> km = descendingMap;
		return (km != null) ? km : (descendingMap = new DescendingSubMap(this, true, null, true, true, null, true));
	}
 	
	public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, 
			K toKey, boolean toInclusive) {
		return new AscendingSubMap(this, false, fromKey, fromInclusive, false, toKey, toInclusive);
	}
	
	public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
		return new AscendingSubMap(this, true, null, true, false, toKey, inclusive);
	}
	
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
		
		@Override
		public Iterator<T> iterator() {
			if(m instanceof TreeMap) {
				return ((TreeMap<T,Object>) m).keyIterator();
			} else {
				return (Iterator<T>)(((TreeMap.NavigableSubMap)m).keyIterator());
			}
		}
		
		@Override
		public Iterator<T> descendingIterator() {
			if(m instanceof TreeMap) {
				return ((TreeMap<T, Object>) m).keyIterator();
			} else {
				return (Iterator<T>)(((TreeMap.NavigableSubMap)m).keyIterator()); 
			}

		}

		public int size() {
			return m.size();
		}
		
		public boolean isEmpty() {
			return m.isEmpty();
		}
		
		public boolean contains(Object o) {
			return m.containsKey(o);
		}
		
		public void clear() {
			m.clear();
		}
		
		public T lower(T t) {
			return m.lowerKey(t);
		}
		
		public T floor(T t) {
			return m.floorKey(t);
		}
		
		public T ceiling(T t) {
			return m.ceilingKey(t);
		}
		
		public T higher(T t) {
			return m.higherKey(t);
		}
		
		public T first() {
			return m.firstKey();
		}
		
		public T last() {
			return m.lastKey();
		}
		
		public Comparator<? super T> comparator() {
			return m.comparator();
		}
		
		public T pollFirst() {
			Map.Entry<T, Object> e = m.pollFirstEntry();
			return e == null ? null : e.getKey();
		}
		
		public T pollLast() {
			Map.Entry<T, Object> e = m.pollLastEntry();
			return e == null ? null : e.getKey();
		}
		
		public boolean remove(Object o) {
			int oldSize = size();
			m.remove(o);
			return size() != oldSize;
		}
		
		public NavigableSet<T> subSet(T fromElement, boolean fromInclusive,
				T toElement, boolean toInclusive) {
			// TODO
			return null;
		}
		
		public NavigableSet<T> headSet(T toElement, boolean inclusive) {
			// TODO
			return null;
		}

		@Override
		public SortedSet<T> tailSet(T fromElement) {
			// TODO
			return null;
		}

		@Override
		public NavigableSet<T> descendingSet() {
			// TODO
			return null;
		}

		@Override
		public NavigableSet<T> tailSet(T fromElement, boolean inclusive) {
			// TODO
			return null;
		}

		@Override
		public SortedSet<T> subSet(T fromElement, T toElement) {
			// TODO
			return null;
		}

		@Override
		public SortedSet<T> headSet(T toElement) {
			// TODO
			return null;
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
	
	static abstract class NavigableSubMap<K, V> extends AbstractMap<K, V> 
		implements NavigableMap<K, V>, Serializable {
		
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
		
		public boolean isEmpty() {
			return (fromStart && toEnd) ? m.isEmpty() : entrySet().isEmpty();
		}

		public int size() {
			return (fromStart && toEnd) ? m.size() :entrySet().size();
		}
		
		public final boolean containsKey(Object key) {
			return inRange(key) && m.containsKey(key);
		}
		
		public final V put(K key, V value) {
			if(!inRange(key)) {
				throw new IllegalArgumentException("key out of range");
			}
			return m.put(key, value);
		}
		
		public final V get(Object key) {
			return !inRange(key) ? null : m.get(key);
		}
		
		public final V remove(Object key) {
			return !inRange(key) ? null : m.remove(key);
		}
		
		public final Map.Entry<K, V> ceilingEntry(K key) {
			return exportEntry(subCeiling(key));
		}
		
		public final K ceilingKey(K key) {
			return keyOrNull(subCeiling(key));
		}
		
		public final Map.Entry<K, V> higherEntry(K key) {
			return exportEntry(subHigher(key));
		}
		
		public final K higherKey(K key) {
			return keyOrNull(subHigher(key));
		}
		
		public final Map.Entry<K, V> floorEntry(K key) {
			return exportEntry(subFloor(key));
		}
		
		public final K floorKey(K key) {
			return keyOrNull(subFloor(key));
		}
		
		public final Map.Entry<K, V> lowerEntry(K key) {
			return exportEntry(subLower(key));
		}
		
		public final K lowerKey(K key) {
			return keyOrNull(subLower(key));
		}
		
		public final K fitstKey() {
			return key(subLowest());
		}
		
		public final K lastKey() {
			return key(subHighest());
		}
		
		public final Map.Entry<K, V> pollFirstEntry() {
			TreeMap.Entry<K, V> e = subLowest();
			Map.Entry<K, V> result = exportEntry(e);
			if(e != null) {
				m.deleteEntry(e);
			}
			return result;
		}
		
		transient NavigableMap<K, V> descendingMapView = null;
		transient EntrySetView entrySetView = null;
		transient KeySet<K> navigableKeySetView = null;
		
		abstract class EntrySetView extends AbstractSet<Map.Entry<K, V>> {
			
		}
	}
	
	
 	
	
	static final class AscendingSubMap<K, V> extends NavigableSubMap<K, V> {

		AscendingSubMap(TreeMap<K, V> m, 
				boolean fromStart, K lo, boolean loInclusive, 
				boolean toEnd, 	   K hi, boolean hiInclusive) {
			super(m, fromStart, lo, loInclusive, toEnd, hi, hiInclusive);
		}

		@Override
		public Comparator<? super K> comparator() {
			return m.comparator();
		}
		
		@Override
		public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive,
				K toKey, boolean toInclusive) {
			
			return null;
		}

		
		@Override
		public Map.Entry<K, V> firstEntry() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Map.Entry<K, V> lastEntry() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Map.Entry<K, V> pollLastEntry() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public NavigableMap<K, V> descendingMap() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public NavigableSet<K> navigableKeySet() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public NavigableSet<K> descendingKeySet() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public SortedMap<K, V> subMap(K fromKey, K toKey) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public SortedMap<K, V> headMap(K toKey) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public SortedMap<K, V> tailMap(K fromKey) {
			// TODO Auto-generated method stub
			return null;
		}


		@Override
		public K firstKey() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Set<com.jerry.soundcode.map.Map.Entry<K, V>> entrySet() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		com.jerry.soundcode.map.TreeMap.Entry<K, V> subLowest() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		com.jerry.soundcode.map.TreeMap.Entry<K, V> subHighest() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		com.jerry.soundcode.map.TreeMap.Entry<K, V> subCeiling(K key) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		com.jerry.soundcode.map.TreeMap.Entry<K, V> subHigher(K key) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		com.jerry.soundcode.map.TreeMap.Entry<K, V> subFloor(K key) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		com.jerry.soundcode.map.TreeMap.Entry<K, V> subLower(K key) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		Iterator<K> keyIterator() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		Iterator<K> descendingKeyIterator() {
			// TODO Auto-generated method stub
			return null;
		}
		
	}
	
	static final class DescendingSubMap<K, V> extends NavigableSubMap<K, V> {
		
		public DescendingSubMap(TreeMap<K,V> m, 
				boolean fromStart, K lo, boolean loInclusive,
				boolean toEnd, 	   K hi, boolean hiInclusive) {
			super(m, fromStart, lo, loInclusive, toEnd, hi, hiInclusive);
		}
		
	
		@Override
		public Map.Entry<K, V> firstEntry() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Map.Entry<K, V> lastEntry() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Map.Entry<K, V> pollLastEntry() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public NavigableMap<K, V> descendingMap() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Comparator<? super K> comparator() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public SortedMap<K, V> subMap(K fromKey, K toKey) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public SortedMap<K, V> headMap(K toKey) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public SortedMap<K, V> tailMap(K fromKey) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public K firstKey() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Set<Map.Entry<K, V>> entrySet() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public NavigableSet<K> navigableKeySet() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public NavigableSet<K> descendingKeySet() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive,
				K toKey, boolean toInclusive) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		com.jerry.soundcode.map.TreeMap.Entry<K, V> subLowest() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		com.jerry.soundcode.map.TreeMap.Entry<K, V> subHighest() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		com.jerry.soundcode.map.TreeMap.Entry<K, V> subCeiling(K key) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		com.jerry.soundcode.map.TreeMap.Entry<K, V> subHigher(K key) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		com.jerry.soundcode.map.TreeMap.Entry<K, V> subFloor(K key) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		com.jerry.soundcode.map.TreeMap.Entry<K, V> subLower(K key) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		Iterator<K> keyIterator() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		Iterator<K> descendingKeyIterator() {
			// TODO Auto-generated method stub
			return null;
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
	
	private void buildFromSorted(int size, Iterator it, ObjectInputStream is, V defaultVal) throws IOException, ClassNotFoundException {
		this.size = size;
		root = buildFromSorted(0, 0, size - 1, computeRedLevel(size), it, is, defaultVal);
	}
	
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
