package com.jerry.soundcode.concurrent.collection;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import com.jerry.soundcode.concurrent.atomic.AtomicInteger;
import com.jerry.soundcode.concurrent.locks.Condition;
import com.jerry.soundcode.concurrent.locks.ReentrantLock;
import com.jerry.soundcode.list.AbstractQueue;
import com.jerry.soundcode.list.Collection;
import com.jerry.soundcode.list.Iterator;

/**
 * 一个基于已链接节点的、范围任意的blocking queue的实现，也是线程安全的。
 * 按 FIFO（先进先出）排序元素。队列的头部 是在队列中时间最长的元素。队列的尾部 是在队列中时间最短的元素。 
 */
public class LinkedBlockingQueue<E> extends AbstractQueue<E>
	implements BlockingQueue<E>, Serializable {

	private static final long serialVersionUID = 1L;

	static class Node<E> {
		E item;
		Node<E> next;
		Node(E x) { item = x; }
	}
	
	private final int capacity;
	
	private final AtomicInteger count = new AtomicInteger(0);
	
	private transient Node<E> head;
	
	private transient Node<E> last;
	
	private final ReentrantLock taskLock = new ReentrantLock();
	
	private final Condition notEmpty = taskLock.newCondition();
			
	private final ReentrantLock putLock = new ReentrantLock();
	
	private final Condition notFull = putLock.newCondition();
	
	private void signalNotEmpty() {
		final ReentrantLock takeLock = this.taskLock;
		takeLock.lock();
		try {
			notEmpty.signal();
		} finally {
			takeLock.unlock();
		}
	}
	
	private void signalNotFull() {
		final ReentrantLock putLock = this.putLock;
		putLock.lock();
		try {
			notFull.signal();
		} finally {
			putLock.unlock();
		}
	}
	
	private void enqueue(E x) {
		last = last.next = new Node<E>(x);
	}
	
	private E dequeue() {
		Node<E> h = head;
		Node<E> first = h.next;
		h.next = h;
		E x = first.item;
		first.item = null;
		return x;
	}
	
	void fullyLock() {
		putLock.lock();
		taskLock.lock();
	}
	
	void fullyUnlock() {
		taskLock.unlock();
		putLock.unlock();
	}
	
	boolean isFullyLocked() {
		return (putLock.isHeldByCurrentThread() && taskLock.isHeldByCurrentThread());
	}
	
	public LinkedBlockingQueue(int capacity) {
		if(capacity <= 0) {
			throw new IllegalArgumentException();
		}
		this.capacity = capacity;
		last = head = new Node<E>(null);
	}
	
	public LinkedBlockingQueue(Collection<? extends E> c) {
		this(Integer.MAX_VALUE);
		final ReentrantLock putLock = this.putLock;
		putLock.lock();
		
		try {
			int n = 0;
//			for(E e : c) {
//				if(e == null) {
//					throw new NullPointerException();
//				}
//				if(n == capacity) {
//					throw new IllegalStateException("Queue full");
//				}
//				enqueue(e);
//				++n;
//			}
			count.set(n);
		} finally {
			putLock.unlock();
		}
	}
	
	public int size() {
		return count.get();
	}
	
	public int remainingCapacity() {
		return capacity - count.get();
	}
	
	@Override
	public void put(E e) throws InterruptedException {
		if(e == null) {
			throw new NullPointerException();
		}
		
		int c = -1;
		final ReentrantLock putLock = this.putLock;
		final AtomicInteger count = this.count;
		putLock.lockInterruptibly();
		
		try {
			while(count.get() == capacity) {
				notFull.await();
			}
			enqueue(e);
			c = count.getAndIncrement();
			if(c + 1 < capacity) {
				notFull.signal();
			}
		} finally {
			putLock.unlock();
		}
		
		if(c == 0) {
			signalNotEmpty();
		}
	}
	
	@Override
	public boolean offer(E e, long timeout, TimeUnit unit)
			throws InterruptedException {
		
		if(e == null) {
			throw new NullPointerException();
		}
		
		long nanos = unit.toNanos(timeout);
		int c = -1;
		
		final ReentrantLock putLock = this.putLock;
		final AtomicInteger count = this.count;
		putLock.lockInterruptibly();
		
		try {
			while(count.get() == capacity) {
				if(nanos < 0) {
					return false;
				}
				nanos = notFull.awaitNanos(nanos);
			}
			enqueue(e);
			c = count.getAndIncrement();
			if(c + 1 < capacity) {
				notFull.signal();
			}
			
		} finally {
			putLock.unlock();
		}
		
		if(c == 0) {
			signalNotEmpty();
		}
		
		return true;
	}
	
	@Override
	public boolean offer(E e) {
		if(e == null) {
			throw new NullPointerException();
		}
		final AtomicInteger count = this.count;
		
		if(count.get() == capacity) {
			return false;
		}
		
		int c = -1;
		final ReentrantLock putLock = this.putLock;
		putLock.lock();
		
		try {
			if(count.get() < capacity) {
				enqueue(e);
				c = count.getAndIncrement();
				if(c + 1 < capacity) {
					notFull.signal();
				}
			}
		} finally {
			putLock.unlock();
		} 
		
		if(c == 0) {
			signalNotEmpty();
		}
		return c >= 0;
	}
	
	@Override
	public E take() throws InterruptedException {
		E x ;
		int c = -1;
		final AtomicInteger count = this.count;
		final ReentrantLock takeLock = this.taskLock;
		taskLock.lockInterruptibly();
		
		try {
			while(count.get() == 0) {
				notEmpty.await();
			}
			x = dequeue();
			c = count.getAndDecrement();
			if(c > 1) {
				notEmpty.signal();
			}
		} finally {
			takeLock.unlock();
		}
		
		if(c == capacity) {
			signalNotFull();
		}
		return x;
	}
	
	@Override
	public E poll(long timeout, TimeUnit unit) throws InterruptedException {
		E x = null;
		int c = -1;
		long nanos = unit.toNanos(timeout);
		final AtomicInteger count = this.count;
		final ReentrantLock takeLock = this.taskLock;
		takeLock.lockInterruptibly();
		try {
			while(count.get() == 0) {
				if(nanos <= 0) {
					return null;
				}
				nanos = notEmpty.awaitNanos(nanos);
			}
			x = dequeue();
			c = count.getAndDecrement();
			if(c > 1) {
				notEmpty.signal();
			}
		} finally {
			takeLock.unlock();
		} 
		
		if(c == capacity) {
			signalNotFull();
		}
		
		return x;
	}
	
	@Override
	public E poll() {
		final AtomicInteger count = this.count;
		if(count.get() == 0) {
			return null;
		}
		E x = null;
		int c = -1;
		final ReentrantLock takeLock = this.taskLock;
		takeLock.lock();
		try {
			if(count.get() > 0) {
				x = dequeue();
				c = count.getAndDecrement();
				if(c > 1) {
					notEmpty.signal();
				}
			}
		} finally {
			takeLock.unlock();
		}
		
		if(c == capacity) {
			signalNotFull();
		}
		return x;
	}

	@Override
	public E peek() {
		if(count.get() == 0) {
			return null;
		}
		
		final ReentrantLock takeLock = this.taskLock;
		takeLock.lock();
		
		try {
			Node<E> first = head.next;
			if(first == null) {
				return null;
			} else {
				return first.item;
			}
		} finally {
			takeLock.unlock();
		}
		
	}

	void unlink(Node<E> p, Node<E> trail) {
		p.item = null;
		trail.next = p.next;
		if(last == p) {
			last = trail;
		}
		if(count.getAndDecrement() == capacity) {
			notFull.signal();
		}
	}
	
	public boolean remove(Object o) {
		if(o == null) {
			return false;
		}
		try {
			for(Node<E> trail = head, p = trail.next; p != null; trail = p, p = p.next) {
				if(o.equals(p.item)) {
					unlink(p, trail);
					return true;
				}
			}
			return false;
		} finally {
			fullyUnlock();
		}
	}

	public Object[] toArray() {
		fullyLock();
		try {
			int size = count.get();
			Object[] a = new Object[size];
			int k = 0;
			for(Node<E> p = head.next; p != null; p = p.next) {
				a[k++] = p.item;
			}
			return a;
		} finally {
			fullyUnlock();
		}
	}
	
	@SuppressWarnings("unchecked")
	public <T> T[] toArray(T[] a) {
		fullyLock();
		try {
			int size = count.get();
			if(a.length < size) {
				 a = (T[])java.lang.reflect.Array.newInstance
		                    (a.getClass().getComponentType(), size);
			}
			int k = 0;
			for(Node<E> p = head.next; p != null; p = p.next) {
				a[k++] = (T)p.item;
			}
			if(a.length > k) {
				a[k] = null;
			}
			return a;
		} finally {
			fullyUnlock();
		}
	}
	
	@Override
	public String toString() {
		fullyLock();
		try {
			return super.toString();
		} finally {
			fullyUnlock();
		}
	}
	
	@Override
	public void clear() {
		fullyLock();
		try {
			for(Node<E> p, h = head; (p = h.next) != null; h = p) {
				h.next = h;
				p.item = null;
			}
			head = last;
			if(count.getAndAdd(0) == capacity) {
				notFull.signal();
			}
		} finally {
			fullyUnlock();
		}
	}
	
	@Override
	public int drainTo(Collection<? super E> c) {
		return drainTo(c, Integer.MAX_VALUE);
	}

	@Override
	public int drainTo(Collection<? super E> c, int maxElements) {
		if(c == null) {
			throw new NullPointerException();
		}
		
		if(c == this) {
			throw new IllegalArgumentException();
		}
		
		boolean signalNotFull = false;
		final ReentrantLock takeLock = this.taskLock;
		takeLock.lock();
		
		try {
			int n = Math.min(maxElements, count.get());
			Node<E> h = head;
			int i = 0;
			
			try {
				while(i < n) {
					Node<E> p = h.next;
					c.add(p.item);
					p.item = null;
					h.next = h;
					h = p;
					++i;
				}
				return n;
			} finally {
				if(i > 0) {
					head = h;
					signalNotFull = (count.getAndAdd(-i) == capacity);
				}
			}
		} finally {
			takeLock.unlock();
			if(signalNotFull) {
				signalNotFull();
			}
		}
	}

	@Override
	public Iterator<E> iterator() {
		return new Itr();
	}
	
	private class Itr implements Iterator<E> {

		private Node<E> current;
		private Node<E> lastRet;
		private E currentElement;
		
		Itr() {
			fullyLock();
			try {
				current = head.next;
				if(current != null) {
					currentElement = current.item;
				}
			} finally {
				fullyUnlock();
			}
		}
		
		@Override
		public boolean hasNext() {
			return current != null;
		}

		private Node<E> nextNode(Node<E> p) {
			for(;;) {
				Node<E> s = p.next;
				if(s == p) {
					return head.next;
				}
				if(s == null || s.item != null) {
					return s;
				}
				p = s;
			}
		}
		
		@Override
		public E next() {
			fullyLock();
			try {
				if(current == null) {
					throw new NoSuchElementException();
				}
				E x = currentElement;
				lastRet = current;
				current = nextNode(current);
				currentElement = (current == null) ? null : current.item;
				return x;
			} finally {
				fullyUnlock();
			}
		}

		@Override
		public void remove() {
			if(lastRet == null) {
				throw new IllegalStateException();
			} 
			fullyLock();
			try {
				Node<E> node = lastRet;
				lastRet = null;
				for (Node<E> trail = head, p = trail.next;  p != null;
	                     trail = p, p = p.next) {
					if(p == node) {
						unlink(p, trail);
						break;
					}
				}
			} finally {
				fullyUnlock();
			}
		}
		
	}
	
	private void writeObject(ObjectOutputStream s) throws IOException {
		fullyLock();
		
		try {
			s.defaultWriteObject();
			for(Node<E> p = head.next; p != null; p = p.next) {
				s.writeObject(p.item);
			}
			s.writeObject(null);
		} finally {
			fullyUnlock();
		}
	}
	
	@SuppressWarnings("unused")
	private void readObject(ObjectOutputStream s) throws IOException {
		s.defaultWriteObject();
		
		count.set(0);
		last = head = new Node<E>(null);
		
		for(;;) {
//			E item = (E)s.readObject();
//			if(item == null){
//				break;
//			}
//			add(item);
		}
	}
}
