package com.jerry.soundcode.concurrent.collection;

import java.io.Serializable;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;

import com.jerry.soundcode.list.AbstractQueue;
import com.jerry.soundcode.list.ArrayDeque;
import com.jerry.soundcode.list.Collection;
import com.jerry.soundcode.list.Comparable;
import com.jerry.soundcode.list.Comparator;
import com.jerry.soundcode.list.Iterator;
import com.jerry.soundcode.set.SortedSet;

public class PriorityQueue<E> extends AbstractQueue<E> implements Serializable {

	private static final long serialVersionUID = 1L;

	private static final int DEFAULT_INITIAL_CAPACITY = 11;
	
	private transient Object[] queue;
	
	private int size = 0;
	
	private Comparator<? super E> comparator;
	
	private transient int modCount = 0;
	
	PriorityQueue() {
		this(DEFAULT_INITIAL_CAPACITY, null);
	}
	
	PriorityQueue(int initialCapacity) {
		this(initialCapacity, null);
	}
	
	public PriorityQueue(int initialCapacity, Comparator<? super E> comparator) {
		if(initialCapacity < 1) {
			throw new IllegalArgumentException();
		}
		this.queue = new Object[initialCapacity];
		this.comparator = comparator;
	}
	
	@SuppressWarnings("unchecked")
	public PriorityQueue(Collection<? extends E> c) {
		initFromCollection(c);
		if (c instanceof SortedSet) {
			comparator = (Comparator<? super E>)
                ((SortedSet<? extends E>)c).comparator();
		} else if (c instanceof PriorityQueue) {
			comparator = (Comparator<? super E>)
	                ((PriorityQueue<? extends E>)c).comparator();
		} else {
            comparator = null;
            heapify();
        }
	}
	
	@SuppressWarnings("unchecked")
	public PriorityQueue(PriorityQueue<? extends E> c) {
		comparator = (Comparator<? super E>)c.comparator;
		initFromCollection(c);
	}
	
	@SuppressWarnings("unchecked")
	public PriorityQueue(SortedSet<? extends E> c) {
		comparator = (Comparator<? super E>)c.comparator();
		initFromCollection(c);
	}
	
	private void initFromCollection(Collection<? extends E> c) {
		Object[] a = c.toArray();
		if(a.getClass() != Object[].class) {
			a = Arrays.copyOf(a, a.length, Object[].class);
		}
		queue = a;
		size = a.length;
	}
	
	private void grow(int minCapacity) {
		if(minCapacity < 0) {
			throw new OutOfMemoryError();
		}
		
		int oldCapacity = queue.length;
		int newCapacity = ((oldCapacity < 64) ? ((oldCapacity + 1) * 2) : ((oldCapacity / 2) * 3));
		if(newCapacity < 0) {
			newCapacity = Integer.MAX_VALUE;
		} 
		if(newCapacity < minCapacity) {
			newCapacity = minCapacity;
		}
		queue = Arrays.copyOf(queue, newCapacity);
	}
	
	@Override
	public boolean add(E e) {
		return offer(e);
	}
	
	@Override
	public boolean offer(E e) {
		if(e == null) {
			throw new NullPointerException();
		}
		
		modCount++;
		int i = size;
		if(i >= queue.length) {
			grow(i + 1);
		}
		size = i + 1;
		if(i == 0) {
			queue[0] = e;
		} else {
			siftUp(i, e);
		}
		return true;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public E peek() {
		if(size == 0) {
			return null;
		}
		return (E) queue[0];
	}
	
	private int indexOf(Object o) {
		if(o != null) {
			for(int i = 0; i < size; i++) {
				if(o.equals(queue[i])) {
					return i;
				}
			}
		}
		return -1;
	}
	
	@Override
	public boolean remove(Object o) {
		int i = indexOf(o);
		if(i == -1) {
			return false;
		} else {
			removeAt(i);
			return true;
		}
	}
	
	boolean removeEq(Object o) {
		for(int i = 0; i < size; i++) {
			if(o == queue[i]) {
				removeAt(i);
				return true;
			}
		}
		return false;
	}
	
	@Override
	public boolean contains(Object o) {
		return indexOf(o) != -1;
	}
	
	@Override
	public Object[] toArray() {
		return Arrays.copyOf(queue, size);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> T[] toArray(T[] a) {
		if(a.length < size) {
			return (T[]) Arrays.copyOf(queue, size, a.getClass());
		}
		System.arraycopy(queue, 0, a, 0, size);
		if(a.length > size) {
			a[size] = null;
		}
		return a;
	}
	
	@Override
	public Iterator<E> iterator() {
		return new Itr();
	}
	
	private final class Itr implements Iterator<E> {

		private int cursor = 0;
		
		private int lastRet = -1;
		
		private ArrayDeque<E> forgetMeNot = null;
		
		private E lastRetElt = null;
		
		private int expectedModCount = modCount;
		
		@Override
		public boolean hasNext() {
			return cursor < size || (forgetMeNot != null && !forgetMeNot.isEmpty());
		}

		@SuppressWarnings("unchecked")
		@Override
		public E next() {
			if(expectedModCount != modCount) {
				throw new ConcurrentModificationException();
			}
			if(cursor < size) {
				return (E) queue[lastRet = cursor++];
			}
			
			if(forgetMeNot != null) {
				lastRet = -1;
				lastRetElt = forgetMeNot.poll();
				if(lastRetElt != null) {
					return lastRetElt;
				}
			}
			throw new NoSuchElementException();
		}

		@Override
		public void remove() {
			if(expectedModCount != modCount) {
				throw new ConcurrentModificationException();
			}
			if(lastRet != -1) {
				E moved = PriorityQueue.this.removeAt(lastRet);
				lastRet = -1;
				if(moved == null) {
					cursor --;
				} else {
					if(forgetMeNot == null) {
						forgetMeNot = new ArrayDeque<E>();
					}
					forgetMeNot.add(moved);
				}
			} else if(lastRetElt != null) {
				PriorityQueue.this.removeEq(lastRetElt);
			} else {
				throw new IllegalStateException();
			}
			expectedModCount = modCount;
		}
	}
	
	@Override
	public int size() {
		return size;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public E poll() {
		if(size == 0) {
			return null;
		}
		int s = --size;
		modCount++;
		E reuslt = (E) queue[0];
		E x = (E) queue[s];
		queue[s] = null;
		if(s != 0) {
			siftDown(0, x);
		}
		return reuslt;
	}

	
	@SuppressWarnings("unchecked")
	private E removeAt(int i) {
		assert i >= 0 && i < size;
		modCount ++;
		int s = --size;
		if(s == i) {
			queue[i] = null;
		} else {
			E moved = (E) queue[s];
			queue[s] = null;
			siftDown(i, moved);
			if(queue[i] == moved) {
				siftUp(i, moved);
				if(queue[i] != moved) {
					return moved;
				}
			}
		} 
		
		return null;
		
	}

	private void siftUp(int k, E x) {
		if(comparator != null) {
			siftUpUsingComparator(k, x);
		} else {
			siftUpComparable(k, x);
		}
	}

	@SuppressWarnings("unchecked")
	private void siftUpComparable(int k, E x) {
		Comparable<? super E> key = (Comparable<? super E>) x;
		while(k > 0) {
			int parent =(k - 1) >>> 1;
			Object e = queue[parent];
			if(key.compareTo((E)e) > 0) {
				break;
			}
			queue[k] = e;
			k = parent;
		}
		queue[k] = key;
	}

	@SuppressWarnings("unchecked")
	private void siftUpUsingComparator(int k, E x) {
		while(k > 0) {
			int parent = (k - 1) >>> 1;
			Object e = queue[parent];
			if(comparator.compare(x, (E)e) >= 0) {
				break;
			}
			queue[k] = e;
			k = parent;
		}
		queue[k] = x;
	}

	private void siftDown(int k, E x) {
		if(comparator != null) {
			siftDownUsingComparable(k, x);
		} else {
			siftDownComparable(k, x);
		}
	}
	
	@SuppressWarnings("unchecked")
	private void siftDownUsingComparable(int k, E x) {
		Comparable<? super E> key = (Comparable<? super E>)x;
		int half = size >>> 1;
		while(k < half) {
			int child = (k << 1) + 1; // assume left child is least
            Object c = queue[child];
            int right = child + 1;
            if (right < size &&
                ((Comparable<? super E>) c).compareTo((E) queue[right]) > 0)
                c = queue[child = right];
            if (key.compareTo((E) c) <= 0)
                break;
            queue[k] = c;
            k = child;
		}
		queue[k] = key;
	}

	@SuppressWarnings("unchecked")
	private void siftDownComparable(int k, E x) {
		int half = size >>> 1;
        while (k < half) {
            int child = (k << 1) + 1;
            Object c = queue[child];
            int right = child + 1;
            if (right < size &&
                comparator.compare((E) c, (E) queue[right]) > 0)
                c = queue[child = right];
            if (comparator.compare(x, (E) c) <= 0)
                break;
            queue[k] = c;
            k = child;
        }
        queue[k] = x;
	}

	@SuppressWarnings("unchecked")
	private void heapify() {
		for(int i = (size >>> 1) - 1; i >=0; i--) {
			siftDown(i, (E) queue[i]);
		}
	}
	
	public Comparator<? super E> comparator() {
		return comparator;
	}
	
	private void writeObject(java.io.ObjectOutputStream s)
			throws java.io.IOException{

		s.defaultWriteObject();

        s.writeInt(Math.max(2, size + 1));

        for (int i = 0; i < size; i++)
            s.writeObject(queue[i]);
    }
	
	private void readObject(java.io.ObjectInputStream s)
			throws java.io.IOException, ClassNotFoundException {
		s.defaultReadObject();

	    s.readInt();

		queue = new Object[size];

	    for (int i = 0; i < size; i++)
	    	queue[i] = s.readObject();

		heapify();
	}
}
