package com.jerry.soundcode.concurrent.collection;

import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import com.jerry.soundcode.concurrent.locks.Condition;
import com.jerry.soundcode.concurrent.locks.ReentrantLock;
import com.jerry.soundcode.list.AbstractQueue;
import com.jerry.soundcode.list.Collection;
import com.jerry.soundcode.list.Iterator;

/**
 * 一个无界阻塞队列，只有在延迟期满时才能从中提取元素。
 * 该队列的头部是延迟期满后保存时间最长的Delayed 元素。
 * 根据这个特性么我们可以使用DelayQueue来实现缓存系统、实时调度系统等。
 */
public class DelayQueue<E extends Delayed> extends AbstractQueue<E> 
	implements BlockingQueue<E> {

	private transient final ReentrantLock lock = new ReentrantLock();
    private transient final Condition available = lock.newCondition();
    private final PriorityQueue<E> q = new PriorityQueue<E>();
    
    public DelayQueue() {}
    
    public DelayQueue(Collection<? extends E> c) {
        this.addAll(c);
    }
    
    @Override
    public boolean add(E e) {
        return offer(e);
    }
    
	@Override
	public boolean offer(E e) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            E first = q.peek();
            q.offer(e);
            if (first == null || e.compareTo(first) < 0)
                available.signalAll();
            return true;
        } finally {
            lock.unlock();
        }
    }
	
	@Override
	public void put(E e) {
        offer(e);
    }
	
	@Override
	public boolean offer(E e, long timeout, TimeUnit unit) {
        return offer(e);
    }
	
	@Override
	public E poll() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            E first = q.peek();
            if (first == null || first.getDelay(TimeUnit.NANOSECONDS) > 0)
                return null;
            else {
                E x = q.poll();
                assert x != null;
                if (q.size() != 0)
                    available.signalAll();
                return x;
            }
        } finally {
            lock.unlock();
        }
    }
	
	@SuppressWarnings("unused")
	@Override
	public E take() throws InterruptedException {
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            for (;;) {
                E first = q.peek();
                if (first == null) {
                    available.await();
                } else {
                    long delay =  first.getDelay(TimeUnit.NANOSECONDS);
                    if (delay > 0) {
                        long tl = available.awaitNanos(delay);
                    } else {
                        E x = q.poll();
                        assert x != null;
                        if (q.size() != 0)
                            available.signalAll(); // wake up other takers
                        return x;

                    }
                }
            }
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
            for (;;) {
                E first = q.peek();
                if (first == null) {
                    if (nanos <= 0)
                        return null;
                    else
                        nanos = available.awaitNanos(nanos);
                } else {
                    long delay = first.getDelay(TimeUnit.NANOSECONDS);
                    if (delay > 0) {
                        if (nanos <= 0)
                            return null;
                        if (delay > nanos)
                            delay = nanos;
                        long timeLeft = available.awaitNanos(delay);
                        nanos -= delay - timeLeft;
                    } else {
                        E x = q.poll();
                        assert x != null;
                        if (q.size() != 0)
                            available.signalAll();
                        return x;
                    }
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
            return q.peek();
        } finally {
            lock.unlock();
        }
    }
	
	public int size() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return q.size();
        } finally {
            lock.unlock();
        }
    }
	
	@Override
	public int drainTo(Collection<? super E> c) {
        if (c == null)
            throw new NullPointerException();
        if (c == this)
            throw new IllegalArgumentException();
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            int n = 0;
            for (;;) {
                E first = q.peek();
                if (first == null || first.getDelay(TimeUnit.NANOSECONDS) > 0)
                    break;
                c.add(q.poll());
                ++n;
            }
            if (n > 0)
                available.signalAll();
            return n;
        } finally {
            lock.unlock();
        }
    }
	
	@Override
	public int drainTo(Collection<? super E> c, int maxElements) {
        if (c == null)
            throw new NullPointerException();
        if (c == this)
            throw new IllegalArgumentException();
        if (maxElements <= 0)
            return 0;
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            int n = 0;
            while (n < maxElements) {
                E first = q.peek();
                if (first == null || first.getDelay(TimeUnit.NANOSECONDS) > 0)
                    break;
                c.add(q.poll());
                ++n;
            }
            if (n > 0)
                available.signalAll();
            return n;
        } finally {
            lock.unlock();
        }
    }
	
	@Override
	public void clear() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            q.clear();
        } finally {
            lock.unlock();
        }
    }
	
	@Override
	public int remainingCapacity() {
        return Integer.MAX_VALUE;
    }
	
	@Override
	public Object[] toArray() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return q.toArray();
        } finally {
            lock.unlock();
        }
    }
	
	@Override
	public <T> T[] toArray(T[] a) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return q.toArray(a);
        } finally {
            lock.unlock();
        }
    }
	
	@Override
	public boolean remove(Object o) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return q.remove(o);
        } finally {
            lock.unlock();
        }
    }
	
	@Override
	public Iterator<E> iterator() {
        return new Itr(toArray());
    }
	
	private class Itr implements Iterator<E> {
        final Object[] array; 
        int cursor;           
        int lastRet;          

        Itr(Object[] array) {
            lastRet = -1;
            this.array = array;
        }

        @Override
        public boolean hasNext() {
            return cursor < array.length;
        }

        @SuppressWarnings("unchecked")
        @Override
		public E next() {
            if (cursor >= array.length)
                throw new NoSuchElementException();
            lastRet = cursor;
            return (E)array[cursor++];
        }

        @Override
        public void remove() {
            if (lastRet < 0)
            	throw new IllegalStateException();
            Object x = array[lastRet];
            lastRet = -1;
            lock.lock();
            try {
                for (Iterator<E> it = q.iterator(); it.hasNext(); ) {
                    if (it.next() == x) {
                        it.remove();
                        return;
                    }
                }
            } finally {
                lock.unlock();
            }
        }
    }

}
