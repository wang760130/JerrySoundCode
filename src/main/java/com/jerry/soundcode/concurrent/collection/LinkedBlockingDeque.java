package com.jerry.soundcode.concurrent.collection;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import com.jerry.soundcode.concurrent.locks.Condition;
import com.jerry.soundcode.concurrent.locks.ReentrantLock;
import com.jerry.soundcode.list.AbstractQueue;
import com.jerry.soundcode.list.Collection;
import com.jerry.soundcode.list.Iterator;

/**
 * LinkedBlockingDeque是用双向链表实现的，需要说明的是LinkedList也已经加入了Deque的一部分
 * 
 * 要想支持阻塞功能，队列的容量一定是固定的，否则无法在入队的时候挂起线程。也就是capacity是final类型的。
 * 既然是双向链表，每一个结点就需要前后两个引用，这样才能将所有元素串联起来，支持双向遍历。也即需要prev/next两个引用。
 * 双向链表需要头尾同时操作，所以需要first/last两个节点，当然可以参考LinkedList那样采用一个节点的双向来完成，那样实现起来就稍微麻烦点。
 * 既然要支持阻塞功能，就需要锁和条件变量来挂起线程。这里使用一个锁两个条件变量来完成此功能。
 */
public class LinkedBlockingDeque<E> extends AbstractQueue<E> implements BlockingDeque<E>, Serializable {
	private static final long serialVersionUID = 1L;
	
	static final class Node<E> {
		E item;
		
		Node<E> prev;
		
		Node<E> next;
		
		Node(E x, Node<E> p, Node<E> n) {
			item = x;
			prev = p;
			next = n;
		}
	}
	
	transient Node<E> first;
	
	transient Node<E> last;
	
	private transient int count;
	
	private final int capacity;
	
	final ReentrantLock lock = new ReentrantLock();
	
	private final Condition notEmpty = lock.newCondition();
	
	private final Condition notFull = lock.newCondition();
	
	public LinkedBlockingDeque() {
		this(Integer.MAX_VALUE);
	}
	
	public LinkedBlockingDeque(int capacity) {
		if(capacity < 0) {
			throw new IllegalArgumentException();
		}
		this.capacity = capacity;
	}
	
	public LinkedBlockingDeque(Collection<? extends E> c) {
		this(Integer.MAX_VALUE);
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
//			if(E e : c) {
//				if(e == null) {
					throw new NullPointerException();
//				}
//				if(!linkLast(e)) {
//					throw new IllegalStateException("Deque full");
//				}
//			}
		} finally {
			lock.unlock();
		}
	}
	
	private boolean linkFirt(E e) {
		if(count >= capacity) {
			return false;
		}
		
		Node<E> f = first;
		Node<E> x = new Node<E>(e, null, f);
		first = x;
		if(last == null) {
			last = x;
		} else {
			f.prev = x;
		}
		++count;
		notEmpty.signal();
		return true;
	}
	
	private boolean linkLast(E e) {
		if(count >= capacity) {
			return false;
		}
		Node<E> l = last;
		Node<E> x = new Node<E>(e, l, null);
		last = x;
		if(first == null) {
			first = x;
		} else {
			l.next = x;
		}
		++count;
		notEmpty.signal();
		return true;
	}
	
	private E unlinkFirst() {
		Node<E> f = first;
		if(f == null) {
			return null;
		}
		Node<E> n = f.next;
		E item = f.item;
		f.item = null;
		first = n;
		if(n == null) {
			last = null;
		} else {
			n.prev = null;
		}
		--count;
		notEmpty.signal();
		return item;
	}
	
	private E unlinkLast() {
		Node<E> l = last;
		if(l == null) {
			return null;
		}
		Node<E> p = l.prev;
		E item = l.item;
		l.item = null;
		l.prev = l;
		 
		last = p;
		if(p == null) {
			first = null;
		} else {
			p.next = null;
		}
		--count;
		notEmpty.signal();
		return item;
	}
	
	void unlink(Node<E> x) {
		Node<E> p = x.prev;
		Node<E> n = x.next;
		if(p == null) {
			unlinkFirst();
		} else if(n == null) {
			unlinkLast();
		} else {
			p.next = n;
			n.prev = p;
			x.item = null;
			--count;
			notEmpty.signal();
		}
	}
	
	public void addFirst(E e) {
		if(!offerFirst(e)) {
			throw new IllegalStateException("Deque full");
		}
	}
	
	@Override
	public void addLast(E e) {
		if(!offerLast(e)) {
			throw new IllegalStateException("Deque full");
		}
	}

	public boolean offerFirst(E e) {
		if(e == null) {
			throw new NullPointerException();
		}
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			return linkFirt(e);
		} finally {
			lock.unlock();
		}
	} 
	
	public boolean offerLast(E e) {
		if(e == null) {
			throw new NullPointerException();
		}
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			return linkLast(e);
		} finally {
			lock.unlock();
		}
	}
	
	public void putFirst(E e) throws InterruptedException {
		if(e == null) {
			throw new NullPointerException();
		}
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			while(!linkFirt(e)) {
				notEmpty.await();
			}
		} finally {
			lock.unlock();
		}
	}
	
	@Override
	public void putLast(E e) throws InterruptedException {
		if(e == null) {
			throw new NullPointerException();
		}
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			while(!linkFirt(e)) {
				notFull.await();
			}
		} finally {
			lock.unlock();
		}
	}

	@Override
	public boolean offerFirst(E e, long timeout, TimeUnit unit)
			throws InterruptedException {
		if(e == null) {
			throw new NullPointerException();
		}
		long nanos = unit.toNanos(timeout);
		final ReentrantLock lock = this.lock;
		lock.lockInterruptibly();
		try {
			while(!linkFirt(e)) {
				if(nanos <= 0) {
					return false;
				}
				nanos = notFull.awaitNanos(nanos);
			}
		} finally {
			lock.unlock();
		}
		return false;
	}

	@Override
	public boolean offerLast(E e, long timeout, TimeUnit unit)
			throws InterruptedException {
		if(e == null) {
			throw new NullPointerException();
		}
		long nanos = unit.toNanos(timeout);
		final ReentrantLock lock = this.lock;
		lock.lockInterruptibly();
		try {
			while(!linkLast(e)) {
				if(nanos < 0) {
					return false;
				}
				nanos = notFull.awaitNanos(nanos);
			}
		} finally {
			lock.unlock();
		}
		return false;
	}

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
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			return unlinkLast();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public E pollLast() {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			return unlinkLast();
		} finally {
			lock.unlock();
		}
	}
	
	@Override
	public E takeFirst() throws InterruptedException {
		final ReentrantLock lock = this.lock;
		try {
			E x;
			while( (x = unlinkFirst()) == null) {
				notEmpty.await();
			}
			return x;
		} finally {
			lock.unlock();
		}
	}

	@Override
	public E takeLast() throws InterruptedException {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			E x;
			while((x = unlinkFirst()) == null) {
				notEmpty.await();
			}
			return x;
		} finally {
			lock.unlock();
		}
	}
	
	@Override
	public E pollFirst(long timeout, TimeUnit unit) throws InterruptedException {
		long nanos = unit.toNanos(timeout);
		final ReentrantLock lock = this.lock;
		lock.lockInterruptibly();
		try {
			E x;
			while((x = unlinkFirst()) == null) {
				if(nanos <= 0) {
					return null;
				}
				nanos = notEmpty.awaitNanos(nanos);
			}
			return x;
		} finally {
			lock.unlock();
		}
	}

	@Override
	public E pollLast(long timeout, TimeUnit unit) throws InterruptedException {
		long nanos = unit.toNanos(timeout);
		final ReentrantLock lock = this.lock;
		lock.lockInterruptibly();
		try {
			E x;
			while((x = unlinkLast()) == null) {
				if(nanos <= 0) {
					return null;
				}
				nanos = notEmpty.awaitNanos(nanos);
			}
			return x;
		} finally {
			lock.unlock();
		} 
	}

	@Override
	public E getFirst() {
		E x = peekFirst();
		if(x == null) {
			throw new NoSuchElementException();
		}
		return x;
	}

	@Override
	public E getLast() {
		E x = peekLast();
		if(x == null) {
			throw new NoSuchElementException();
		}
		return x;
	}

	@Override
	public E peekFirst() {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			return (first == null) ? null : first.item;
		} finally {
			lock.unlock();
		}
	}

	@Override
	public E peekLast() {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			return (last == null) ? null : last.item;
		} finally {
			lock.unlock();
		}
	}
	
	public boolean removeFirstOccurrence(Object o) {
		if(o == null) {
			return false;
		}
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			for(Node<E> p = first; p != null; p = p.next) {
				if(o.equals(p.item)) {
					unlink(p);
					return true;
				}
			}
			return false;
		} finally {
			lock.unlock();
		}
	}
	
	public boolean removeLastOccurrence(Object o) {
		if(o == null) {
			return false;
		}
		final ReentrantLock lock = this.lock;
		lock.lock();
		
		try {
			for(Node<E> p = last; p != null; p = p.prev) {
				if(o.equals(p.item)) {
					unlink(p);
					return true;
				}
			}
			return false;
		} finally {
			lock.unlock();
		}
	}
	
	public boolean add(E e) {
		addLast(e);
		return true;
	}
	
	public boolean offer(E e) {
		return offerLast(e);
	}
	
	public void put(E e) throws InterruptedException {
		push(e);
	}
	
	public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
		return offerLast(e, timeout, unit);
	}
	
	public E remove() {
		return removeFirst();
	}
	
	public E poll() {
		return peekFirst();
	}
	
	public E take() throws InterruptedException {
		return takeFirst();
	}
	
	@Override
	public E poll(long timeout, TimeUnit unit) throws InterruptedException {
		return pollFirst(timeout, unit);
	}
	
	public E element() {
		return getFirst();
	}
	
	public E peek() {
		return peekFirst();
	}
	
	public int remainingCapacity() {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			return capacity - count;
		} finally {
			lock.unlock();
		}
	}
	
	public int drainTo(Collection<? super E> c) {
		return drainTo(c, Integer.MAX_VALUE);
	}
	
	@Override
	public int drainTo(Collection<? super E> c,	int maxElements) {
		if(c == null) {
			throw new NullPointerException();
		}
		if(c == this) {
			throw new IllegalArgumentException();
		}
		final ReentrantLock lock = this.lock;
		try {
			int n = Math.min(maxElements, count);
			for(int i = 0; i < n; i++) {
				c.add(first.item);
				unlinkFirst();
			}
			return n;
		} finally {
			lock.unlock();
		}
		
	}

	@Override
	public void push(E t) {
		addFirst(t);
	}

	@Override
	public E pop() {
		return removeFirst();
	}
	
	public boolean remove(Object o) {
		return removeFirstOccurrence(o);
	}

	@Override
	public int size() {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			return count;
		} finally {
			lock.unlock();
		}
	}

	public boolean contains(Object o) {
		if(o == null) {
			return false;
		}
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			for(Node<E> p = first; p != null; p = p.next) {
				if(o.equals(p.item)) {
					return true;
				}
			}
			return false;
		} finally {
			lock.unlock();
		}
	}
	
	
	public Object[] toArray() {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			Object[] a = new Object[count];
			int k = 0; 
			for(Node<E> p = first; p != null; p = p.next) {
				a[k++] = p.item;
			} 
			return a;
		} finally {
			lock.unlock();
		}
	}
	
	@SuppressWarnings("unchecked")
	public <T> T[] toArray(T[] a) {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			if(a.length < count) {
				 a = (T[])java.lang.reflect.Array.newInstance
			                (a.getClass().getComponentType(), count);
			}
			int k = 0;
			for(Node<E> p = first; p != null; p = p.next) {
				a[k++] = (T) p.item;
			}
			if(a.length > k) {
				a[k] = null; 
			}
			return a;
		} finally {
			lock.unlock();
		}
	}
	
	public String toString() {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			return super.toString();
		} finally {
			lock.unlock();
		}
	}
	
	public void clear() {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			for(Node<E> f = first; f != null; ) {
				f.item = null;
				Node<E> n = f.next;
				f.prev = null;
				f.next = null;
				f = n;
			}
			first = last = null;
			count = 0;
			notEmpty.signalAll();
		} finally {
			lock.unlock();
		}
	}
	
	@Override
	public Iterator<E> iterator() {
		return new Itr();
	}
	
	@Override
	public Iterator<E> descendingIterator() {
		return new DescendingItr();
	}

	private abstract class AbstractItr implements Iterator<E> {
		Node<E> next;
		
		E nextItem;
		
		private Node<E> lastRet;
		
		abstract Node<E> firstNode();
		
		abstract Node<E> nextNode(Node<E> n);
		
		AbstractItr() {
			final ReentrantLock lock = LinkedBlockingDeque.this.lock;
			lock.lock();
			try {
				next = firstNode();
				nextItem = (next == null) ? null : next.item;
			} finally {
				lock.unlock();
			}
		}
		
		void advance() {
			final ReentrantLock lock = LinkedBlockingDeque.this.lock;
			lock.lock();
			try {
				Node<E> s = firstNode();
				if(s == next) {
					next = firstNode();
				} else {
					while(s != null && s.item == null) {
						s = nextNode(s);
					}
					next = s;
				}
				
			} finally {
				lock.unlock();
			}
		}
		
		public boolean hasNext() {
			return next != null;
		}		
		
		public E next() {
			if(next == null) {
				throw new NoSuchElementException();
			}
			lastRet = next;
			E x = nextItem;
			advance();
			return x;
		}
		
		public void remove() {
			Node<E> n = lastRet;
			if(n == null) {
				throw new IllegalStateException();
			}
			lastRet = null;
			
			final ReentrantLock lock = LinkedBlockingDeque.this.lock;
			lock.lock();
			try {
				if(n.item != null) {
					unlink(n);
				}
			} finally {
				lock.unlock();
			}
		}
	}
	
	private class Itr extends AbstractItr {
		Node<E> firstNode() {
			return first;
		}
		
		Node<E> nextNode(Node<E> n) {
			return n.next;
		}
	}
	
	private class DescendingItr extends AbstractItr {
		Node<E> firstNode() {
			return last;
		}
		
		Node<E> nextNode(Node<E> n) {
			return n.prev;
		}
	}
	
	private void writeObject(ObjectOutputStream s) throws IOException {
		final ReentrantLock lock = this.lock;
		lock.lock();
		
		try {
			s.defaultWriteObject();
			for(Node<E> p = first; p != null; p = p.next) {
				s.writeObject(p.item);
			}
			s.writeObject(null);
		} finally {
			lock.unlock();
		}
	}
	
	@SuppressWarnings("unchecked")
	private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
		s.defaultReadObject();
		count = 0;
		first = null;
		last = null;
		for(;;) {
			E item = (E)s.readObject();
			if(item == null) {
				break;
			}
			add(item);
		}
	}
	
}
