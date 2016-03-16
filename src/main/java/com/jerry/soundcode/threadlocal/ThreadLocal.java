package com.jerry.soundcode.threadlocal;

import com.jerry.soundcode.concurrent.atomic.AtomicInteger;
import com.jerry.soundcode.ref.WeakReference;

public class ThreadLocal<T> {
	
	private final int threadLocalHashCode = nextHashCode();
	
	private static AtomicInteger nextHashCode = new AtomicInteger();
	
    private static final int HASH_INCREMENT = 0x61c88647;

	private static int nextHashCode() {
		return nextHashCode.getAndAdd(HASH_INCREMENT);
	}
	
	protected T initailValue() {
		return null;
	}
	
	public ThreadLocal() {
		
	}
	
	public T get() {
//		Thread t = Thread.currentThread();
//		ThreadLocalMap
		return null;
	}
	
	T childValue(T parentValue) {
		throw new UnsupportedOperationException();
	}
	
	static class ThreadLocalMap {
		static class Entry extends WeakReference<ThreadLocal> {
			Object value;
			
			Entry(ThreadLocal k, Object v) {
				super(k);
				value = v;
			}
		}
		
		private static final int INITIAL_CAPACITY = 16;
		
		private Entry[] table;
		
		private int size = 0;
		
		private int threshold;
		
		private void setThreshold(int len) {
			threshold = len * 2 / 3;
		}
		
		private static int nextIndex(int i, int len) {
			return ((i + 1 < len) ? i + 1 : 0);
		}
		
		private static int prevIndex(int i , int len) {
			return ((i - 1 >= 0) ? i - 1 : len - 1);
		}
		
		ThreadLocalMap(ThreadLocal firKey, Object firstValue) {
			table = new Entry[INITIAL_CAPACITY];
			int i = firKey.threadLocalHashCode & (INITIAL_CAPACITY - 1);
			table[i] = new Entry(firKey, firstValue);
			size = 1;
			setThreshold(INITIAL_CAPACITY);
		}
		
		private ThreadLocalMap(ThreadLocalMap parentMap) {
			Entry[] parentTable = parentMap.table;
			int len = parentTable.length;
			setThreshold(len);
			table = new Entry[len];
			
			for(int j = 0; j < len; j++) {
				Entry e = parentTable[j];
				if(e != null) {
					ThreadLocal key = e.get();
					Object value = key.childValue(e.value);
					Entry c = new Entry(key, value);
					int h = key.threadLocalHashCode & (len - 1);
					while(table[h] != null) {
						h = nextIndex(h, len);
					}
					table[h] = c;
					size++;
				}
			}
		}
		
		private Entry getEntry(ThreadLocal key) {
			int i = key.threadLocalHashCode & (table.length - 1);
			Entry e = table[i];
			if(e != null && e.get() == key) {
				return e ;
			} else {
				return getEntryAfterMiss(key, i, e);
			}
		}
		
		private Entry getEntryAfterMiss(ThreadLocal key, int i, Entry e) {
			Entry[] tab = table;
			int len = tab.length;
			
			while(e != null) {
				ThreadLocal k = e.get();
				if(k == key) {
					return e;
				}
				if(k == null) {
					expungeStaleEntry(i);
				} else {
					i = nextIndex(i, len);
				}
				e = tab[i];
			}
			return null;
 		}
		
		private void set(ThreadLocal key, Object value) {
			Entry[] tab = table;
			int len = tab.length;
			int i = key.threadLocalHashCode & (len - 1);
			
			for(Entry e = tab[i]; e != null; e = tab[i = nextIndex(i, len)]) {
				ThreadLocal k = e.get();
				
				if(k == key) {
					e.value = value;
					return ;
				}
				
				if(k == null) {
					
				}
			}
		}
		
		private void replaceStaleEntry(ThreadLocal key, Object value, int staleSlot) {
			Entry[] tab = table;
			int len = tab.length;
			Entry e;
			
			int slotToExpunge = staleSlot;
			for(int i = prevIndex(staleSlot, len); (e = tab[i]) != null; i = prevIndex(i, len)) {
				if(e.get() == null) {
					slotToExpunge = i;
				}
			}
			
			for(int i = nextIndex(staleSlot, len); (e = tab[i]) != null; i = nextIndex(i, len)) {
				i = nextIndex(i, len);
				ThreadLocal k = e.get();
				if(k == key) {
					e.value = value;
					tab[i] = tab[staleSlot];
					
					if(slotToExpunge == staleSlot) {
						slotToExpunge = i;
					}
				}
			}
			
		}
		
		private int expungeStaleEntry(int staleSlot) {
			Entry[] tab = table;
			int len = tab.length;
			
			tab[staleSlot].value = null;
			tab[staleSlot] = null;
			size --;
			
			Entry e;
			int i;
			for(i = nextIndex(staleSlot, len); (e = tab[i]) != null; i = nextIndex(i, len)) {
				ThreadLocal k = e.get();
				if(k == null) {
					e.value = null;
					tab[i] = null;
					size --;
				} else {
					int h = k.threadLocalHashCode & (len - 1);
					if(h != i) {
						tab[i] = null;
						while(tab[h] != null) {
							h = nextIndex(i, len);
						}
						tab[h] = e;
					}
				} 
			}
			return i;
		}
	}
}
