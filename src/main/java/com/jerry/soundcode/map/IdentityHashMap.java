package com.jerry.soundcode.map;


import java.io.IOException;
import java.io.Serializable;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;

import com.jerry.soundcode.list.AbstractCollection;
import com.jerry.soundcode.list.Collection;
import com.jerry.soundcode.list.Iterator;
import com.jerry.soundcode.set.AbstractSet;
import com.jerry.soundcode.set.Set;

/**
 * 说IdentityHashMap与常用的HashMap的区别是： 前者比较key时是“引用相等”而后者是“对象相等”，即对于k1和k2，当k1==k2时，
 * IdentityHashMap认为两个key相等，而HashMap只有在k1.equals(k2) == true 时才会认为两个key相等
 * 其特殊用途，比如序列化或者深度复制。或者记录对象代理。
 */
public class IdentityHashMap<K, V> extends AbstractMap<K, V> implements
		Map<K, V>, Serializable, Cloneable {

	private static final long serialVersionUID = 1L;

	private static final int DEFAULT_CAPACITY = 32;

	private static final int MINIMUM_CAPACITY = 4;

	private static final int MAXIMUM_CAPACITY = 1 << 29;

	private transient Object[] table;

	private int size;

	private transient volatile int modCount;

	private transient int threshold;

	private static final Object NULL_KEY = new Object();

	private static Object maskNull(Object key) {
		return (key == null ? NULL_KEY : key);
	}

	private static Object unmaskNull(Object key) {
		return (key == NULL_KEY ? null : key);
	}

	public IdentityHashMap() {
		init(DEFAULT_CAPACITY);
	}

	public IdentityHashMap(int expectedMaxSize) {
		if (expectedMaxSize < 0)
			throw new IllegalArgumentException("expectedMaxSize is negative: "
					+ expectedMaxSize);
		init(capacity(expectedMaxSize));
	}

	private int capacity(int expectedMaxSize) {
		int minCapacity = (3 * expectedMaxSize) / 2;
		int result;
		if (minCapacity > MAXIMUM_CAPACITY || minCapacity < 0) {
			result = MAXIMUM_CAPACITY;
		} else {
			result = MINIMUM_CAPACITY;
			while (result < minCapacity)
				result <<= 1;
		}
		return result;
	}

	private void init(int initCapacity) {
		threshold = (initCapacity * 2) / 3;
		table = new Object[2 * initCapacity];
	}

	public IdentityHashMap(Map<? extends K, ? extends V> m) {
		this((int) ((1 + m.size()) * 1.1));
		putAll(m);
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public boolean isEmpty() {
		return size == 0;
	}

	private static int hash(Object x, int length) {
		int h = System.identityHashCode(x);
		return ((h << 1) - (h << 8)) & (length - 1);
	}

	private static int nextKeyIndex(int i, int len) {
		return (i + 2 < len ? i + 2 : 0);
	}

	@SuppressWarnings("unchecked")
	public V get(Object key) {
		Object k = maskNull(key);
		Object[] tab = table;
		int len = tab.length;
		int i = hash(k, len);
		while (true) {
			Object item = tab[i];
			if (item == k)
				return (V) tab[i + 1];
			if (item == null)
				return null;
			i = nextKeyIndex(i, len);
		}
	}

	@Override
	public boolean containsKey(Object key) {
		Object k = maskNull(key);
		Object[] tab = table;
		int len = tab.length;
		int i = hash(k, len);
		while (true) {
			Object item = tab[i];
			if (item == k)
				return true;
			if (item == null)
				return false;
			i = nextKeyIndex(i, len);
		}
	}

	@Override
	public boolean containsValue(Object value) {
		Object[] tab = table;
		for (int i = 1; i < tab.length; i += 2)
			if (tab[i] == value && tab[i - 1] != null)
				return true;

		return false;
	}

	private boolean containsMapping(Object key, Object value) {
		Object k = maskNull(key);
		Object[] tab = table;
		int len = tab.length;
		int i = hash(k, len);
		while (true) {
			Object item = tab[i];
			if (item == k)
				return tab[i + 1] == value;
			if (item == null)
				return false;
			i = nextKeyIndex(i, len);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public V put(K key, V value) {
		Object k = maskNull(key);
		Object[] tab = table;
		int len = tab.length;
		int i = hash(k, len);

		Object item;
		while ((item = tab[i]) != null) {
			if (item == k) {
				V oldValue = (V) tab[i + 1];
				tab[i + 1] = value;
				return oldValue;
			}
			i = nextKeyIndex(i, len);
		}

		modCount++;
		tab[i] = k;
		tab[i + 1] = value;
		if (++size >= threshold)
			resize(len); // len == 2 * current capacity.
		return null;
	}

	private void resize(int newCapacity) {
		// assert (newCapacity & -newCapacity) == newCapacity; // power of 2
		int newLength = newCapacity * 2;

		Object[] oldTable = table;
		int oldLength = oldTable.length;
		if (oldLength == 2 * MAXIMUM_CAPACITY) { // can't expand any further
			if (threshold == MAXIMUM_CAPACITY - 1)
				throw new IllegalStateException("Capacity exhausted.");
			threshold = MAXIMUM_CAPACITY - 1; // Gigantic map!
			return;
		}
		if (oldLength >= newLength)
			return;

		Object[] newTable = new Object[newLength];
		threshold = newLength / 3;

		for (int j = 0; j < oldLength; j += 2) {
			Object key = oldTable[j];
			if (key != null) {
				Object value = oldTable[j + 1];
				oldTable[j] = null;
				oldTable[j + 1] = null;
				int i = hash(key, newLength);
				while (newTable[i] != null)
					i = nextKeyIndex(i, newLength);
				newTable[i] = key;
				newTable[i + 1] = value;
			}
		}
		table = newTable;
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		int n = m.size();
		if (n == 0)
			return;
		if (n > threshold) 
			resize(capacity(n));

//		for (Entry<? extends K, ? extends V> e : m.entrySet())
//			put(e.getKey(), e.getValue());
	}

	@SuppressWarnings("unchecked")
	public V remove(Object key) {
		Object k = maskNull(key);
		Object[] tab = table;
		int len = tab.length;
		int i = hash(k, len);

		while (true) {
			Object item = tab[i];
			if (item == k) {
				modCount++;
				size--;
				V oldValue = (V) tab[i + 1];
				tab[i + 1] = null;
				tab[i] = null;
				closeDeletion(i);
				return oldValue;
			}
			if (item == null)
				return null;
			i = nextKeyIndex(i, len);
		}

	}

	private boolean removeMapping(Object key, Object value) {
		Object k = maskNull(key);
		Object[] tab = table;
		int len = tab.length;
		int i = hash(k, len);

		while (true) {
			Object item = tab[i];
			if (item == k) {
				if (tab[i + 1] != value)
					return false;
				modCount++;
				size--;
				tab[i] = null;
				tab[i + 1] = null;
				closeDeletion(i);
				return true;
			}
			if (item == null)
				return false;
			i = nextKeyIndex(i, len);
		}
	}

	private void closeDeletion(int d) {
		Object[] tab = table;
		int len = tab.length;

		Object item;
		for (int i = nextKeyIndex(d, len); (item = tab[i]) != null; i = nextKeyIndex(
				i, len)) {
			int r = hash(item, len);
			if ((i < r && (r <= d || d <= i)) || (r <= d && d <= i)) {
				tab[d] = item;
				tab[d + 1] = tab[i + 1];
				tab[i] = null;
				tab[i + 1] = null;
				d = i;
			}
		}
	}

	@Override
	public void clear() {
		modCount++;
		Object[] tab = table;
		for (int i = 0; i < tab.length; i++)
			tab[i] = null;
		size = 0;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		} else if (o instanceof IdentityHashMap) {
			IdentityHashMap m = (IdentityHashMap) o;
			if (m.size() != size)
				return false;

			Object[] tab = m.table;
			for (int i = 0; i < tab.length; i += 2) {
				Object k = tab[i];
				if (k != null && !containsMapping(k, tab[i + 1]))
					return false;
			}
			return true;
		} else if (o instanceof Map) {
			Map m = (Map) o;
			return entrySet().equals(m.entrySet());
		} else {
			return false; 
		}
	}

	@Override
	public int hashCode() {
		int result = 0;
		Object[] tab = table;
		for (int i = 0; i < tab.length; i += 2) {
			Object key = tab[i];
			if (key != null) {
				Object k = unmaskNull(key);
				result += System.identityHashCode(k)
						^ System.identityHashCode(tab[i + 1]);
			}
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object clone() {
		try {
			IdentityHashMap<K, V> m = (IdentityHashMap<K, V>) super.clone();
			m.entrySet = null;
			m.table = (Object[]) table.clone();
			return m;
		} catch (CloneNotSupportedException e) {
			throw new InternalError();
		}
	}

	private abstract class IdentityHashMapIterator<T> implements Iterator<T> {
		int index = (size != 0 ? 0 : table.length); 
		int expectedModCount = modCount; 
		int lastReturnedIndex = -1; 
		boolean indexValid; 
		Object[] traversalTable = table; 

		public boolean hasNext() {
			Object[] tab = traversalTable;
			for (int i = index; i < tab.length; i += 2) {
				Object key = tab[i];
				if (key != null) {
					index = i;
					return indexValid = true;
				}
			}
			index = tab.length;
			return false;
		}

		protected int nextIndex() {
			if (modCount != expectedModCount)
				throw new ConcurrentModificationException();
			if (!indexValid && !hasNext())
				throw new NoSuchElementException();

			indexValid = false;
			lastReturnedIndex = index;
			index += 2;
			return lastReturnedIndex;
		}

		@SuppressWarnings("unchecked")
		public void remove() {
			if (lastReturnedIndex == -1)
				throw new IllegalStateException();
			if (modCount != expectedModCount)
				throw new ConcurrentModificationException();

			expectedModCount = ++modCount;
			int deletedSlot = lastReturnedIndex;
			lastReturnedIndex = -1;
			size--;
			index = deletedSlot;
			indexValid = false;

			Object[] tab = traversalTable;
			int len = tab.length;

			int d = deletedSlot;
			K key = (K) tab[d];
			tab[d] = null; 
			tab[d + 1] = null;

			if (tab != IdentityHashMap.this.table) {
				IdentityHashMap.this.remove(key);
				expectedModCount = modCount;
				return;
			}

			Object item;
			for (int i = nextKeyIndex(d, len); (item = tab[i]) != null; i = nextKeyIndex(
					i, len)) {
				int r = hash(item, len);
				if ((i < r && (r <= d || d <= i)) || (r <= d && d <= i)) {

					if (i < deletedSlot && d >= deletedSlot
							&& traversalTable == IdentityHashMap.this.table) {
						int remaining = len - deletedSlot;
						Object[] newTable = new Object[remaining];
						System.arraycopy(tab, deletedSlot, newTable, 0,
								remaining);
						traversalTable = newTable;
						index = 0;
					}

					tab[d] = item;
					tab[d + 1] = tab[i + 1];
					tab[i] = null;
					tab[i + 1] = null;
					d = i;
				}
			}
		}
	}

	private class KeyIterator extends IdentityHashMapIterator<K> {
		@SuppressWarnings("unchecked")
		public K next() {
			return (K) unmaskNull(traversalTable[nextIndex()]);
		}
	}

	private class ValueIterator extends IdentityHashMapIterator<V> {
		@SuppressWarnings("unchecked")
		public V next() {
			return (V) traversalTable[nextIndex() + 1];
		}
	}

	private class EntryIterator extends
			IdentityHashMapIterator<Map.Entry<K, V>> implements Map.Entry<K, V> {
		public Map.Entry<K, V> next() {
			nextIndex();
			return this;
		}

		@SuppressWarnings("unchecked")
		@Override
		public K getKey() {
			if (lastReturnedIndex < 0)
				throw new IllegalStateException("Entry was removed");

			return (K) unmaskNull(traversalTable[lastReturnedIndex]);
		}

		@SuppressWarnings("unchecked")
		@Override
		public V getValue() {
			if (lastReturnedIndex < 0)
				throw new IllegalStateException("Entry was removed");

			return (V) traversalTable[lastReturnedIndex + 1];
		}

		@SuppressWarnings("unchecked")
		@Override
		public V setValue(V value) {
			if (lastReturnedIndex < 0)
				throw new IllegalStateException("Entry was removed");
			V oldValue = (V) traversalTable[lastReturnedIndex + 1];
			traversalTable[lastReturnedIndex + 1] = value;
			if (traversalTable != IdentityHashMap.this.table)
				put((K) traversalTable[lastReturnedIndex], value);
			return oldValue;
		}

		@SuppressWarnings("rawtypes")
		@Override
		public boolean equals(Object o) {
			if (lastReturnedIndex < 0)
				return super.equals(o);

			if (!(o instanceof Map.Entry))
				return false;
			Map.Entry e = (Map.Entry) o;
			return e.getKey() == getKey() && e.getValue() == getValue();
		}

		@Override
		public int hashCode() {
			if (lastReturnedIndex < 0)
				return super.hashCode();

			return System.identityHashCode(getKey())
					^ System.identityHashCode(getValue());
		}

		@Override
		public String toString() {
			if (lastReturnedIndex < 0)
				return super.toString();

			return getKey() + "=" + getValue();
		}
	}

	private transient Set<Map.Entry<K, V>> entrySet = null;

	@Override
	public Set<K> keySet() {
		Set<K> ks = keySet;
		if (ks != null)
			return ks;
		else
			return keySet = new KeySet();
	}

	private class KeySet extends AbstractSet<K> {
		@Override
		public Iterator<K> iterator() {
			return new KeyIterator();
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
			int oldSize = size;
			IdentityHashMap.this.remove(o);
			return size != oldSize;
		}

		@SuppressWarnings("rawtypes")
		@Override
		public boolean removeAll(Collection<?> c) {
			boolean modified = false;
			for (Iterator i = iterator(); i.hasNext();) {
				if (c.contains(i.next())) {
					i.remove();
					modified = true;
				}
			}
			return modified;
		}

		@Override
		public void clear() {
			IdentityHashMap.this.clear();
		}

		@Override
		public int hashCode() {
			int result = 0;
//			for (K key : this)
//				result += System.identityHashCode(key);
			return result;
		}
	}

	@Override
	public Collection<V> values() {
		Collection<V> vs = values;
		if (vs != null)
			return vs;
		else
			return values = (Collection<V>) new Values();
	}

	private class Values extends AbstractCollection<V> {
		@Override
		public Iterator<V> iterator() {
			return new ValueIterator();
		}

		@Override
		public int size() {
			return size;
		}

		@Override
		public boolean contains(Object o) {
			return containsValue(o);
		}

		@SuppressWarnings("rawtypes")
		@Override
		public boolean remove(Object o) {
			for (Iterator i = iterator(); i.hasNext();) {
				if (i.next() == o) {
					i.remove();
					return true;
				}
			}
			return false;
		}

		public void clear() {
			IdentityHashMap.this.clear();
		}
	}

	@Override
	public Set<Map.Entry<K, V>> entrySet() {
		Set<Map.Entry<K, V>> es = entrySet;
		if (es != null)
			return es;
		else
			return entrySet = new EntrySet();
	}

	private class EntrySet extends AbstractSet<Map.Entry<K, V>> {
		public Iterator<Map.Entry<K, V>> iterator() {
			return new EntryIterator();
		}

		@SuppressWarnings("rawtypes")
		@Override
		public boolean contains(Object o) {
			if (!(o instanceof Map.Entry))
				return false;
			Map.Entry entry = (Map.Entry) o;
			return containsMapping(entry.getKey(), entry.getValue());
		}

		@SuppressWarnings("rawtypes")
		@Override
		public boolean remove(Object o) {
			if (!(o instanceof Map.Entry))
				return false;
			Map.Entry entry = (Map.Entry) o;
			return removeMapping(entry.getKey(), entry.getValue());
		}

		@Override
		public int size() {
			return size;
		}

		@Override
		public void clear() {
			IdentityHashMap.this.clear();
		}

		@SuppressWarnings("rawtypes")
		@Override
		public boolean removeAll(Collection<?> c) {
			boolean modified = false;
			for (Iterator i = iterator(); i.hasNext();) {
				if (c.contains(i.next())) {
					i.remove();
					modified = true;
				}
			}
			return modified;
		}

		@Override
		public Object[] toArray() {
			int size = size();
			Object[] result = new Object[size];
			Iterator<Map.Entry<K, V>> it = iterator();
			for (int i = 0; i < size; i++)
				result[i] = new AbstractMap.SimpleEntry<K, V>(it.next());
			return result;
		}

		@SuppressWarnings("unchecked")
		public <T> T[] toArray(T[] a) {
			int size = size();
			if (a.length < size)
				a = (T[]) java.lang.reflect.Array.newInstance(a.getClass()
						.getComponentType(), size);
			Iterator<Map.Entry<K, V>> it = iterator();
			for (int i = 0; i < size; i++)
				a[i] = (T) new AbstractMap.SimpleEntry<K, V>(it.next());
			if (a.length > size)
				a[size] = null;
			return a;
		}
	}

	private void writeObject(java.io.ObjectOutputStream s)
			throws java.io.IOException {
		s.defaultWriteObject();
		s.writeInt(size);

		Object[] tab = table;
		for (int i = 0; i < tab.length; i += 2) {
			Object key = tab[i];
			if (key != null) {
				s.writeObject(unmaskNull(key));
				s.writeObject(tab[i + 1]);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void readObject(java.io.ObjectInputStream s)
			throws java.io.IOException, ClassNotFoundException {
		s.defaultReadObject();

		int size = s.readInt();

		init(capacity((size * 4) / 3));

		for (int i = 0; i < size; i++) {
			K key = (K) s.readObject();
			V value = (V) s.readObject();
			putForCreate(key, value);
		}
	}

	@SuppressWarnings("unchecked")
	private void putForCreate(K key, V value) throws IOException {
		K k = (K) maskNull(key);
		Object[] tab = table;
		int len = tab.length;
		int i = hash(k, len);

		Object item;
		while ((item = tab[i]) != null) {
			if (item == k)
				throw new java.io.StreamCorruptedException();
			i = nextKeyIndex(i, len);
		}
		tab[i] = k;
		tab[i + 1] = value;
	}

}
