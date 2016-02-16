package com.jerry.soundcode.map;

import java.io.Serializable;

import com.jerry.soundcode.list.AbstractCollection;
import com.jerry.soundcode.list.Collection;
import com.jerry.soundcode.list.Iterator;
import com.jerry.soundcode.set.AbstractSet;
import com.jerry.soundcode.set.Set;

public abstract class AbstractMap<K, V> implements Map<K, V> {
	
	protected AbstractMap() {
	}
	
	@Override
	public int size() {
		return entrySet().size();
	}
	
	@Override
	public boolean isEmpty() {
		return size() == 0;
	}
	
	@Override
	public boolean containsValue(Object value) {
		Iterator<Entry<K, V>> it = entrySet().iterator();
		if(value == null) {
			while(it.hasNext()) {
				Entry<K, V> e = it.next();
				if(e.getValue() == null) {
					return true;
				}
			}
		} else {
			while(it.hasNext()) {
				Entry<K, V> e = it.next();
				if(value.equals(e.getValue())) {
					return true;
				}
			}
		}
		return false;
	}
	
	@Override
	public boolean containsKey(Object key) {
		Iterator<Map.Entry<K, V>> it = entrySet().iterator();
		if(key == null) {
			while(it.hasNext()) {
				Entry<K, V> e = it.next();
				if(e.getKey() == null) 
					return true;
			}
		} else {
			while(it.hasNext()) {
				Entry<K,V> e = it.next();
				if(key.equals(e.getKey())) {
					return true;
				}
			} 
		}
		return false;
	}
	
	@Override
	public V get(Object key) {
		Iterator<Entry<K, V>> it = entrySet().iterator();
		if(key == null) {
			while(it.hasNext()) {
				Entry<K, V> e = it.next();
				if(e.getKey() == null) 
					return e.getValue();
			}
		} else {
			while(it.hasNext()) {
				Entry<K, V> e = it.next();
				if(key.equals(e.getKey())) {
					return e.getValue();
				}
			}
		}
		return null;
	}
	
	@Override
	public V put(K key, V value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public V remove(Object key) {
		Iterator<Entry<K,V>> it = entrySet().iterator();
		Entry<K, V> correctEntry = null;
		if(key == null) {
			while(correctEntry == null && it.hasNext()) {
				Entry<K, V> e = it.next();
				if(e.getKey() == null) {
					correctEntry = e;
				}
			}
		} else {
			while(correctEntry == null && it.hasNext()) {
				Entry<K, V> e = it.next();
				if(key.equals(e.getKey())) {
					correctEntry = e;
				}
			}
		}
		
		V oldValue = null;
		if(correctEntry != null) {
			oldValue = correctEntry.getValue();
			it.remove();
		}
		return oldValue;
	}
	
	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		/*for(Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
			put(e.getKey(), e.getValue());
		}*/
	}
	
	@Override
	public void clear() {
		entrySet().clear();
	}
	
	transient volatile Set<K> keySet = null;
	transient volatile Collection<V> values = null;
	
	public Set<K> keySet() {
		if(keySet == null) {
			keySet = new AbstractSet<K>() {

				@Override
				public Iterator<K> iterator() {
					return new Iterator<K>() {
						
						private Iterator<Entry<K,V>> it = entrySet().iterator();
						
						@Override
						public boolean hasNext() {
							return it.hasNext();
						}

						@Override
						public K next() {
							return it.next().getKey();
						}

						@Override
						public void remove() {
							it.remove();
						}
						
					};
				}

				@Override
				public int size() {
					return AbstractMap.this.size();
				}
				
				@Override
				public boolean contains(Object key) {
					return AbstractMap.this.containsKey(key);
				}
				
			};
		}
		return keySet;
	}
	
	public Collection<V> values() {
		if(values == null) {
			values = new AbstractCollection<V>() {

				@Override
				public Iterator<V> iterator() {
					return new Iterator<V>() {
						private Iterator<Entry<K, V>> it = entrySet().iterator();

						@Override
						public boolean hasNext() {
							return it.hasNext();
						}

						@Override
						public V next() {
							return it.next().getValue();
						}

						@Override
						public void remove() {
							it.remove();
						}
						
					};
				}

				@Override
				public int size() {
					return AbstractMap.this.size();
				}
				
				@Override
				public boolean contains(Object value) {
					return AbstractMap.this.containsValue(value);
				}
			};
		}
		return values;
	}
	
	public abstract Set<Entry<K,V>> entrySet();
	
	@Override
	public boolean equals(Object o) {
		if(o == this) {
			return true;
		}
		
		if(!(o instanceof Map)) {
			return false;
		}
		
		Map<K, V> map = (Map<K, V>) o;
		if(map.size() != size()) {
			return false;
		}
		
		try {
			Iterator<Entry<K, V>> it = entrySet().iterator();
			while(it.hasNext()) {
				Entry<K, V> e = it.next();
				K key = e.getKey();
				V value = e.getValue();
				if(value == null) {
					if(value == null)
						if(!(map.get(key) == null) && map.containsKey(key)) {
							return false;
					} else {
						if(!value.equals(map.get(key)))
							return false;
					}
				}
			}
		} catch (ClassCastException unused) {
	        return false;
	    } catch (NullPointerException unused) {
	        return false;
	    }
		
		return true;
	}
	
	@Override
	public int hashCode() {
		int h = 0;
		Iterator<Entry<K, V>> it = entrySet().iterator();
		while(it.hasNext()) {
			h += it.next().hashCode();
		}
		return h;
	}
	
	@Override
	public String toString() {
		Iterator<Entry<K, V>> i = entrySet().iterator();
		if (!i.hasNext())
			return "{}";

		StringBuilder sb = new StringBuilder();
		sb.append('{');
		for (;;) {
			Entry<K, V> e = i.next();
			K key = e.getKey();
			V value = e.getValue();
			sb.append(key == this ? "(this Map)" : key);
			sb.append('=');
			sb.append(value == this ? "(this Map)" : value);
			if (!i.hasNext())
				return sb.append('}').toString();
			sb.append(", ");
		}
	}
	
	@Override
	protected Object clone() throws CloneNotSupportedException {
		AbstractMap<K, V> result = (AbstractMap<K, V>) super.clone();
		result.keySet = null;
		result.values = null;
		return result;
	}
	
	private static boolean eq(Object o1, Object o2) {
		return o1 == null ? o2 == null : o1.equals(o2);
	}
	
	public static class SimpleEntry<K, V> implements Entry<K, V> , Serializable {

		private static final long serialVersionUID = 1L;

		private final K key;
		private V value;
		
		public SimpleEntry(K key, V value) {
			this.key = key;
			this.value = value;
		}
		
		public SimpleEntry(Entry<? extends K, ? extends V> entry) {
			this.key = entry.getKey();
			this.value = entry.getValue();
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
		public boolean equals(Object obj) {
			if(!(obj instanceof Map.Entry)) {
				return false;
			}
			Map.Entry<K, V> e = (Map.Entry<K, V>) obj;
			return eq(key, e.getKey()) && eq(value, e.getValue());
		}
		
		@Override
		public int hashCode() {
			return (key == null ? 0 : key.hashCode()) ^ (value == null ? 0 : value.hashCode());
		}
		
		@Override
		public String toString() {
			return key + "=" + value;
		}
	}
	
	public static class SimpleImmutableEntry<K, V> implements Entry<K, V>, Serializable {

		private static final long serialVersionUID = 1L;

		private final K key;
		private final V value;
		
		public SimpleImmutableEntry(K key, V value) {
			this.key = key;
			this.value = value;
		}
		
		public SimpleImmutableEntry(Entry<? extends K, ? extends V> entry) {
			this.key = entry.getKey();
			this.value = entry.getValue();
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
			throw new UnsupportedOperationException();
		}
		
		@Override
		public boolean equals(Object obj) {
			if(!(obj instanceof Map.Entry)) {
				return false;
			}
			Map.Entry e = (Map.Entry) obj;
			return eq(key, e.getKey()) && eq(value, e.getValue());
		}
		
		@Override
		public int hashCode() {
			return (key == null ? 0 : key.hashCode()) ^ (value == null ? 0 : value.hashCode());
		}
		
		@Override
		public String toString() {
			return key + "=" + value;
		}
	}
}
