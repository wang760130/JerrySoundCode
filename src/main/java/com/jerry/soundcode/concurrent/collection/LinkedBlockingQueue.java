package com.jerry.soundcode.concurrent.collection;

import java.io.Serializable;
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
		final ReentrantLock lock = this.putLock;
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
	public boolean offer(E t, long timeout, TimeUnit unit)
			throws InterruptedException {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public boolean offer(E t) {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public E poll() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public E peek() {
		// TODO Auto-generated method stub
		return null;
	}

	


	@Override
	public E take() throws InterruptedException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public E poll(long timeout, TimeUnit unit) throws InterruptedException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public int drainTo(Collection<? super E> c) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int drainTo(Collection<? super E> c, int maxElements) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Iterator<E> iterator() {
		// TODO Auto-generated method stub
		return null;
	}


}
