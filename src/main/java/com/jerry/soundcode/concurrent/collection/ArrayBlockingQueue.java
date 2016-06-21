package com.jerry.soundcode.concurrent.collection;

import java.io.Serializable;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import com.jerry.soundcode.concurrent.locks.Condition;
import com.jerry.soundcode.concurrent.locks.ReentrantLock;
import com.jerry.soundcode.list.AbstractQueue;
import com.jerry.soundcode.list.Collection;
import com.jerry.soundcode.list.Iterator;


/**
 * 数组支持的线程安全的有界阻塞队列，此队列按 FIFO（先进先出）原则对元素进行排序。
 * 这是一个典型的“有界缓存区”，固定大小的数组在其中保持生产者插入的元素和使用者提取的元素。
 * 一旦创建了这样的缓存区，就不能再增加其容量。试图向已满队列中放入元素会导致操作受阻塞；
 * 试图从空队列中提取元素将导致类似阻塞。此类支持对等待的生产者线程和消费者线程进行排序的可选公平策略。
 * 
 * 默认情况下，不保证是这种排序。然而，通过将公平性 (fairness) 设置为 true 而构造的队列允许按照 FIFO 顺序访问线程。
 * 公平性通常会降低吞吐量，但也减少了可变性和避免了“不平衡性”。 
 */
public class ArrayBlockingQueue<E> extends AbstractQueue<E> 
	implements BlockingQueue<E>, Serializable {

	private static final long serialVersionUID = 1L;

	/** 真正存入数据的数组*/  
	private final E[] items;
	
	/** take, poll, peek or remove的下一个索引 */
	private int takeIndex;
	
	/** put, offer, or add的下一个索引 */  
	private int putIndex;
	
	/**队列中元素个数*/ 
	private int count;
	
	/**可重入锁 */  
	private final ReentrantLock lock;
	
	/** 队列不为空的条件 */ 
	private final Condition notEmpty;
	
	/** 队列未满的条件 */ 
	private final Condition notFull;
	
	/** 
     *当前元素个数+1 
     */  
	final int inc(int i) {
		return (++i == items.length) ? 0 : i;
	}
	
	/** 
     *当前元素个数-1 
     */  
	final int dec(int i) {
		return ((i == 0) ? items.length : i) - 1;
	}
	
	private void insert(E x) {
		items[putIndex] = x;
		putIndex = inc(putIndex);
		++count;
		notEmpty.signal(); //有一个元素加入成功，那肯定队列不为空
	}
	
	private E extract() {
		final E[] items = this.items;
		E x = items[takeIndex];
		items[takeIndex] = null;
		takeIndex = inc(takeIndex);
		--count;
		notFull.signal(); //有一个元素取出成功，那肯定队列不满
		return x;
	}
	
	void removeAt(int i ) {
		final E[] items = this.items;
		if(i == takeIndex) {
			items[takeIndex] = null;
			takeIndex = inc(takeIndex);
		} else {
			for(;;) {
				int nexti = inc(i);
				if(nexti != putIndex) {
					items[i] = items[nexti];
					i = nexti;
				} else {
					items[i] = null;
					putIndex = i;
					break;
				}
			}
		}
	}
	
	public ArrayBlockingQueue(int capacity) {
		this(capacity, false);
	}
	
	@SuppressWarnings("unchecked")
	public ArrayBlockingQueue(int capacity, boolean fair) {
		if(capacity <= 0) {
			throw new IllegalArgumentException();
		}
		
		this.items = (E[]) new Object[capacity];
		lock = new ReentrantLock(fair); //是否为公平锁，如果是的话，那么先到的线程先获得锁对象。  
        //否则，由操作系统调度由哪个线程获得锁，一般为false，性能会比较高  
		notEmpty = lock.newCondition();
		notFull = lock.newCondition();
	}
	
	public ArrayBlockingQueue(int capacity, boolean fair, Collection<? extends E> c) {
		this(capacity, fair);
		if(capacity < c.size()) {
			throw new IllegalArgumentException();
		}
		
		for(Iterator<? extends E> it = c.iterator(); it.hasNext();) {
			add(it.next());
		}
	}
	
	@Override
	public boolean add(E e) {
		return super.add(e);
	}
	
	@Override
	public boolean offer(E e) {
		if(e == null) {
			throw new NullPointerException();
		}
		
		final ReentrantLock lock = this.lock;
		lock.lock();
		
		try { 
			if(count == items.length) { //超过数组的容量  	
				return false;
			} else {
				insert(e);
				return true;
			}
		} finally {
			lock.unlock();
		}
	}
	
	@Override
	public void put(E e) throws InterruptedException {
		if(e == null) {
			throw new NullPointerException();
		}
		
		final E[] items = this.items;
		final ReentrantLock lock = this.lock;
		lock.lockInterruptibly();
		
		try {
			try {
				while(count == items.length) {
					notFull.await();
				} 
			} catch(InterruptedException ie) {
				notFull.signal();
				throw ie;
			}
		} finally {
			lock.unlock();
		}
	}
	
	@Override
	public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
		if(e == null) {
			throw new NullPointerException();
		}
		
		long nanos = unit.toNanos(timeout);
		final ReentrantLock lock = this.lock;
		lock.lockInterruptibly();
		
		try {
			for(;;) {
				if(count != items.length) {
					insert(e);
					return true;
				}
				if(nanos <= 0) {
					return false;
				}
				try {
					nanos = notFull.awaitNanos(nanos);
				} catch (InterruptedException ie) {
					notFull.signal();
					throw ie;
				}
			}
		} finally {
			lock.unlock();
		}
	}
	
	@Override
	public E poll() {
		final ReentrantLock lock = this.lock;
		lock.lock();
		
		try {
			if(count == 0) {
				return null;
			}
			E x = extract();
			return x;
		} finally {
			lock.unlock();
		}
	}

	@Override
	public E take() throws InterruptedException {
		final ReentrantLock lock = this.lock;
		lock.lockInterruptibly();
		
		try {
			try {
				while(count == 0) {
					notEmpty.await();
				} 
			} catch (InterruptedException ie) {
				notEmpty.signal();
				throw ie;
			}
			E x = extract();
			return x;
		} finally {
			lock.unlock();
		}
	}
	
	@Override
	public E poll(long timeout, TimeUnit unit) throws InterruptedException {
		long nanos = unit.toNanos(timeout);
		final ReentrantLock lock = this.lock;
		lock.lockInterruptibly();
		
		try {
			for(;;) {
				if(count != 0) {
					E x = extract();
					return x;
				}
				
				if(nanos <= 0) {
					return null;
				}
				
				try {
					nanos = notEmpty.awaitNanos(nanos);
				} catch (InterruptedException ie) {
					notEmpty.signal();
					throw ie;
				}
			}
		} finally {
			lock.unlock();
		}
	}
	
	@Override
	public E peek() {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			return (count == 0) ? null : items[takeIndex];
		} finally {
			lock.unlock();
		}
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


	@Override
	public int remainingCapacity() {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			return items.length - count;
		} finally {
			lock.unlock();
		}
	}
	
	@Override
	public boolean remove(Object o) {
		if(o == null) 
			return false;
		final E[] items = this.items;
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			int i = takeIndex;
			int k = 0;
			for(;;) {
				if(k++ >= count) {
					return false;
				}
				if(o.equals(items[i])) {
					removeAt(i);
					return true;
				}
				i = inc(i);
			} 
		} finally {
			lock.unlock();
		}
	}
	
	@Override
	public boolean contains(Object o) {
		if(o == null) {
			return false;
		}
		
		final E[] items = this.items;
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			int i = takeIndex;
			int k = 0;
			while(k++ < count) {
				if(o.equals(items[i])) {
					return true;
				}
				i = inc(i);
			}
			return false;
		} finally {
			lock.unlock();
		}
	}
	
	@Override
	public Object[] toArray() {
		final E[] items = this.items;
		final ReentrantLock lock = this.lock;
		lock.lock();
		
		try {
			Object[] a = new Object[count];
			int k = 0;
			int i = takeIndex;
			while(k < count) {
				a[k++] = items[i];
				i = inc(i);
			}
			return a;
		} finally {
			lock.unlock();
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T[] toArray(T[] a) {
		final E[] items = this.items;
		final ReentrantLock lock = this.lock;
		lock.lock();
		
		try {
			if(a.length < count) {
				a = (T[]) java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), count);
			}
			int k = 0;
			int i = takeIndex;
			while(k < count) {
				a[k++] = (T)items[i];
				i = inc(i);
			}
			if(a.length > count) {
				a[count] = null;
			}
			return a;
		} finally {
			lock.unlock();
		}
	}
	
	@Override
	public String toString() {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			return super.toString();
		} finally {
			lock.unlock();
		}
	}
	
	@Override
	public void clear() {
		final E[] items = this.items;
		final ReentrantLock lock = this.lock;
		lock.lock();
		
		try {
			int i = takeIndex;
			int k = count;
			while(k-- > 0) {
				items[i] = null;
				i = inc(i);
			}
			count = 0;
			putIndex = 0;
			takeIndex = 0;
			notFull.signalAll();
		} finally {
			lock.unlock();
		}
	}
	
	
	@Override
	public int drainTo(Collection<? super E> c) {
		if(c == null) {
			throw new NullPointerException();
		}
		
		if(c == this) {
			throw new IllegalArgumentException();
		}
		
		final E[] items = this.items;
		final ReentrantLock lock = this.lock;
		lock.lock();
		
		try {
			int i = takeIndex;
			int n = 0;
			int max = count;
			while(n < max) {
				c.add(items[i]);
				items[i] = null;
				i = inc(i);
				++n;
			}
			
			if(n > 0) {
				count = 0;
				putIndex = 0;
				takeIndex = 0;
				notFull.signalAll();
			}
			
			return n;
		} finally {
			lock.unlock();
		}
		
	}

	@SuppressWarnings("unused")
	@Override
	public int drainTo(Collection<? super E> c, int maxElements) {
		if (c == null)
            throw new NullPointerException();
        if (c == this)
            throw new IllegalArgumentException();
        if (maxElements <= 0)
            return 0;
        final E[] items = this.items;
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            int i = takeIndex;
            int n = 0;
            int sz = count;
            int max = (maxElements < count)? maxElements : count;
            while (n < max) {
                c.add(items[i]);
                items[i] = null;
                i = inc(i);
                ++n;
            }
            if (n > 0) {
                count -= n;
                takeIndex = i;
                notFull.signalAll();
            }
            return n;
        } finally {
            lock.unlock();
        }
	}

	@Override
	public Iterator<E> iterator() {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			return new Itr();
		} finally {
			lock.unlock();
		}
	}
	
	private class Itr implements Iterator<E> {
		
		private int remaining;
		private int nextIndex;
		private E nextItem;
		private E lastItem;
		private int lastRet;
		
		Itr() {
			lastRet = -1;
			if((remaining = count) > 0) {
				nextItem = items[nextIndex = takeIndex];
			}
		}
		
		@Override
		public boolean hasNext() {
			return remaining > 0;
		}

		@Override
		public E next() {
			if(remaining <= 0) {
				throw new NoSuchElementException();
			}
			
			final ReentrantLock lock = ArrayBlockingQueue.this.lock;
			lock.lock();
			
			try {
				lastRet = nextIndex;
				E x = lastItem = nextItem;
				while(--remaining > 0) {
					if ((nextItem = items[nextIndex = inc(nextIndex)]) != null) {
						break;
					}
				}
				return x;
			} finally {
				lock.unlock();
			}
			
		}

		@Override
		public void remove() {
			final ReentrantLock lock = ArrayBlockingQueue.this.lock;
			lock.lock();
			
			try {
				int i = lastRet;
				if(i == -1) {
					throw new IllegalStateException();
				}
				lastRet = -1;
				E x = lastItem;
				if(x == items[i]) {
					boolean removingHead = (i == takeIndex);
					removeAt(i);
					if(!removingHead) {
						nextIndex = dec(nextIndex);
					}
				}
			} finally {
				lock.unlock();
			}
		}
	}
}
