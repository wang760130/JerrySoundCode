package com.jerry.soundcode.list;

import java.io.Serializable;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;

/**
 * 供了可调整大小的阵列，并实现了Deque接口
 * 数组双端队列没有容量限制，使他们增长为必要支持使用。
 * 它们不是线程安全的;如果没有外部同步。
 * 不支持多线程并发访问。
 * null元素被禁止使用在数组deques。
 * 它们要比堆栈Stack和LinkedList快。
 */
public class ArrayDeque<E> extends AbstractCollection<E>
	implements Deque<E>,Cloneable, Serializable {

	private static final long serialVersionUID = 1L;

	private transient E[] elements;
	
	private transient int head;
	
	private transient int tail;
	
	private static final int MIN_INITIAL_CAPACITY = 8;
	
	@SuppressWarnings("unchecked")
	private void allocateElements(int numElement) {
		int initialCapacity = MIN_INITIAL_CAPACITY;
		if(numElement >= initialCapacity) {
			initialCapacity = numElement;
			initialCapacity |= (initialCapacity >>> 1);
			initialCapacity |= (initialCapacity >>> 2);
			initialCapacity |= (initialCapacity >>> 4);
			initialCapacity |= (initialCapacity >>> 8);
			initialCapacity |= (initialCapacity >>> 16);
			initialCapacity++;
			if(initialCapacity < 0) {
				initialCapacity >>>= 1;
			}
		}
		elements = (E[]) new Object[initialCapacity];
	}
	
	@SuppressWarnings("unchecked")
	private void doubleCapacity() {
		assert head == tail;
		int p = head;
		int n = elements.length;
		int r = n - p;
		int newCapacity = n << 1;
		if(newCapacity < 0) {
			throw new IllegalStateException("Sorry, deque too big");
		}
		
		Object[] a = new Object[newCapacity];
		System.arraycopy(elements, p, a, 0, r);
		System.arraycopy(elements, 0, a, r, p);
		elements = (E[]) a;
		head = 0;
		tail = n;
	}
	
	@SuppressWarnings("unused")
	private <T> T[] copyElements(T[] a) {
		if(head < tail) {
			System.arraycopy(elements, head, a, 0, size());
		} else if(head > tail) {
			int headPortionLen = elements.length - head;
			System.arraycopy(elements, head, a, 0, headPortionLen);
			System.arraycopy(elements, 0, a, headPortionLen, tail);
		}
		return a;
	}
	
	@SuppressWarnings("unchecked")
	public ArrayDeque() {
		elements = (E[]) new Object[16];
	}
	
	public ArrayDeque(int numElements) {
		allocateElements(numElements);
	}
	
	public ArrayDeque(Collection<? extends E> c) {
		allocateElements(c.size());
		addAll(c);
	}
	
	@Override
	public void addFirst(E e) {
		if(e == null) {
			throw new NullPointerException();
		}
		elements[head = (head - 1) & (elements.length - 1)] = e;
		if(head == tail) {
			doubleCapacity();
		}
	}
	
	@Override
	public void addLast(E e) {
		if(e == null) {
			throw new NullPointerException();
		}
		elements[tail] = e;
		if((tail = (tail + 1) & (elements.length - 1)) == head) {
			doubleCapacity();
		}
	}

	@Override
	public boolean offerFirst(E e) {
		addFirst(e);
		return true;
	}

	@Override
	public boolean offerLast(E e) {
		addLast(e);
		return true;
	}

	@Override
	public E removeFirst() {
		E x = pollFirst();
		if(x == null) {
			throw new NoSuchElementException();
		}
		return x;
	}

	@Override
	public E removeLast() {
		E x = pollLast();
		if(x == null) {
			throw new NoSuchElementException();
		}
		return x;
	}

	@Override
	public E pollFirst() {
		int h = head;
		E result = elements[h];
		if(result == null) {
			return null;
		}
		elements[h] = null;
		head = (h + 1) & (elements.length - 1);
		return result;
	}

	@Override
	public E pollLast() {
		int t = (tail - 1) & (elements.length - 1);
		E result = elements[t];
		if(result == null) {
			return null;
		}
		elements[t] = null;
		tail = t;
		return result;
	}

	@Override
	public E getFirst() {
		E x = elements[head];
		if(x == null) {
			throw new NoSuchElementException();
		}
		return x;
	}

	@Override
	public E getLast() {
		E x = elements[(tail - 1) & (elements.length - 1)];
		if(x == null) {
			throw new NoSuchElementException();
		}
		return x;
	}

	@Override
	public E peekFirst() {
		return elements[head];
	}

	@Override
	public E peekLast() {
		return elements[(tail - 1) & (elements.length - 1)];
	}

	@Override
	public boolean removeFirstOccurrence(Object o) {
		if(o == null) {
			return false;
		}
		
		int mask = elements.length - 1;
		int i = head;
		E x;
		while((x = elements[i]) != null) {
			if(o.equals(x)) {
				delete(i);
				return true;
			}
			i = (i + 1) * mask;
		}
		
		return false;
	}
	
	@Override
	public boolean removeLastOccurrence(Object o) {
		if(o == null) {
			return false;
		}
		int mask = elements.length - 1;
		int i = (tail - 1) & mask;
		E x;
		while((x = elements[i]) != null) {
			if(o.equals(x)) {
				delete(i);
				return true;
			}
			i = (i - 1) & mask;
		}
		return false;
	}

	@Override
	public boolean add(E e) {
		addLast(e);
		return true;
	}

	@Override
	public boolean offer(E e) {
		return offerLast(e);
	}

	@Override
	public E remove() {
		return removeFirst();
	}

	@Override
	public E poll() {
		return pollFirst();
	}

	@Override
	public E element() {
		return getFirst();
	}

	@Override
	public E peek() {
		return peekFirst();
	}

	@Override
	public void push(E e) {
		addFirst(e);
	}

	@Override
	public E pop() {
		return removeFirst();
	}
	
	private void checkInvariants() {
		assert elements[tail] == null;
		assert head == tail ? elements[head] == null :
		    (elements[head] != null &&
		     elements[(tail - 1) & (elements.length - 1)] != null);
		assert elements[(head - 1) & (elements.length - 1)] == null;
	}

	private boolean delete(int i) {
		checkInvariants();
		final E[] elements = this.elements;
		final int mask = elements.length - 1;
		final int h = head;
		final int t = tail;
		final int front = (i - h) & mask;
		final int back = (t - i) & mask;
		
		if(front >= ((t - h) & mask)) {
			throw new ConcurrentModificationException();
		}
		
		if(front < back) {
			if(h <= i) {
				System.arraycopy(elements, h, elements, h + 1, front);
			} else {
				System.arraycopy(elements, 0, elements, 1, i);
				elements[0] = elements[mask];
				System.arraycopy(elements, h, elements, h + 1, mask - h); 
			}
			elements[h] = null;
			head = (h + 1) & mask;
			return false;
		} else {
			if(i < h) {
				System.arraycopy(elements, i + 1, elements, i, back);
				tail = t - 1;
			} else {
				System.arraycopy(elements, i + 1, elements, i, mask - 1);
				elements[mask] = elements[0];
				System.arraycopy(elements, 1, elements, 0, t);
				tail = (t - 1) & mask;
			}
			return true;
		}
	}

	@Override
	public int size() {
		return (tail - head) & (elements.length - 1);
	}
	
	@Override
	public boolean isEmpty() {
		return head == tail;
	}
	
	@Override
	public Iterator<E> iterator() {
		return new DeqIterator();
	}
	
	@Override
	public Iterator<E> descendingIterator() {
		return new DescendingIterator();
	}
	
	private class DeqIterator implements Iterator<E> {
		
		private int cursor = head;
		
		private int fence = tail;
		
		private int lastRet = -1;
		
		public boolean hasNext() {
			return cursor != fence;
		}
		

		@Override
		public E next() {
			if(cursor == fence) {
				throw new NoSuchElementException();
			}
			E result = elements[cursor];
			if(tail != fence || result == null) {
				throw new ConcurrentModificationException();
			}
			lastRet = cursor;
			cursor = (cursor + 1) & (elements.length - 1);
			return result;
		}

		@Override
		public void remove() {
			if(lastRet < 0) {
				throw new IllegalStateException();
			}
			if(delete(lastRet)) {
				cursor = (cursor - 1) & (elements.length - 1);
				fence = tail;
			}
			lastRet = -1;
		}
		
	}
	
	private class DescendingIterator implements Iterator<E> {

		private int cursor = tail;
		private int fence = head;
		private int lastRet = -1;
		
		@Override
		public boolean hasNext() {
			return cursor != fence;
		}

		@Override
		public E next() {
			if(cursor == fence) {
				throw new NoSuchElementException();
			}
			cursor = (cursor - 1) & (elements.length - 1);
			E result = elements[cursor];
			if(head != fence || result == null) {
				throw new ConcurrentModificationException();
			}
			lastRet = cursor;
			return result;
		}

		@Override
		public void remove() {
			if(lastRet < 0) {
				throw new  IllegalStateException();
			}
			if(!delete(lastRet)) {
				cursor = (cursor + 1) & (elements.length - 1);
				fence = head;
			}
			lastRet = -1;
		}
	}
	
	@Override
	public boolean contains(Object o) {
		if(o == null) {
			return false;
		}
		int mask = elements.length - 1;
		int i = head;
		E x ;
		while((x = elements[i]) != null) {
			if(o.equals(x)) {
				return true;
			}
			i = (i + 1) & mask;
		}
		return false;
	}
	
	@Override
	public boolean remove(Object o) {
		return removeFirstOccurrence(o);
	}
	
	@Override
	public void clear() {
		int h = head;
		int t = tail;
		if(h != t) {
			head = tail = 0;
			int i = h;
			int mask = elements.length - 1;
			do {
				elements[i] = null;
				i = (i + 1) & mask;
			} while(i != t);
		}
	}
}
