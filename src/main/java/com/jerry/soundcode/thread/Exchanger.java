package com.jerry.soundcode.thread;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import com.jerry.soundcode.concurrent.atomic.AtomicInteger;
import com.jerry.soundcode.concurrent.locks.LockSupport;

/**
 * Exchanger是一种线程间安全交换数据的机制。可以和之前分析过的SynchronousQueue对比一下：
 * 线程A通过SynchronousQueue将数据a交给线程B；线程A通过Exchanger和线程B交换数据，
 * 线程A把数据a交给线程B，同时线程B把数据b交给线程A。可见，SynchronousQueue是交给一个数据，Exchanger是交换两个数据。
 */
public class Exchanger<V> {

	private static final int NCPU = Runtime.getRuntime().availableProcessors();

	/** 
	 * arena(Slot数组)的容量。设置这个值用来避免竞争。 
	 */ 
	private static final int CAPACITY = 32;
	
	private static final int FULL = Math.max(0, Math.min(CAPACITY, NCPU / 2) - 1);
	
	private static final int SPINS = (NCPU == 1) ? 0 : 2000;
	
	private static final int TIME_SPINS = SPINS / 20;
	
	private static final Object CANCEL = new Object();
	
	private static final Object NULL_ITEM = new Object();
	
	private static final class Node extends AtomicReference<Object> {
		
		/** 创建这个节点的线程提供的用于交换的数据。 */  
		public final Object item;
		
		/** 等待唤醒的线程 */  
		public volatile Thread waiter;
		
		public Node(Object item) {
			this.item = item;
		}
	}
	
	/** 
	 * 一个Slot就是一对线程交换数据的地方。 
	 * 这里对Slot做了缓存行填充，能够避免伪共享问题。 
	 * 虽然填充导致浪费了一些空间，但Slot是按需创建，一般没什么问题。 
	 */  
	private static final class Slot extends AtomicReference<Object> {
		long q0, q1, q2, q3, q4, q5, q6, q7, q8, q9, qa, qb, qc, qd, qe;
	}
	
	/** 
	 * Slot数组，在需要时才进行初始化。 
	 * 用volatile修饰，因为这样可以安全的使用双重锁检测方式构建。 
	 */  
	private volatile Slot[] arena = new Slot[CAPACITY];
	
	/** 
	 * 正在使用的slot下标的最大值。当一个线程经历了多次CAS竞争后， 
	 * 这个值会递增；当一个线程自旋等待超时后，这个值会递减。 
	 */ 
	private final AtomicInteger max = new AtomicInteger();
	
	/** 
	 * 这个方法会处理不同的情况，使用Object而不是泛型，主要是为了返回一些 
	 * 哨兵值(比如表示null和取消的对象)。 
	 * 
	 * @param item 用来进行交换的数据。 
	 * @param timed 如果有超时延迟，设置为true 
	 * @param nanos 具体的超时时间。 
	 * @return 返回另一个线程(与当前线程交换数据)的数据，或者CANCEL(表示取消) 
	 */  
	private Object doExchange(Object item, boolean timed, long nanos) {
		Node me = new Node(item);
		int index = hashIndex();
		int fails = 0;
		
		for(;;) {
			Object y ;
			Slot slot = arena[index];
			if(slot == null) {
				createSlot(index);
			} else if((y = slot.get()) != null &&
					slot.compareAndSet(y, null)) {
				Node you = (Node)y;
				if(you.compareAndSet(null, item)) {
					LockSupport.unpark(you.waiter);
					return you.item;
				}
			} else if(y == null && 
					slot.compareAndSet(null, me)) {
				if(index == 0) {
					return timed ? awaitNanos(me, slot, nanos) : await(me, slot);
				}
				
				Object v = spinWait(me, slot);
				if(v != CANCEL) {
					return v;
				}
				me = new Node(item);
				int m = max.get();
				if(m > (index >>>= 1)) {
					max.compareAndSet(m, m - 1);
				}
			} else if(++fails > 1) {
				int m = max.get();
				if(fails > 3 && m < FULL && max.compareAndSet(m, m + 1)) {
					index = m + 1;
				} else if(--index < 0) {
					index = m;
				}
				 
			}
		}
	}

	private int hashIndex() {
		long id = Thread.currentThread().getId();
        int hash = (((int)(id ^ (id >>> 32))) ^ 0x811c9dc5) * 0x01000193;

        int m = max.get();
        int nbits = (((0xfffffc00  >> m) & 4) | 
                     ((0x000001f8 >>> m) & 2) | 
                     ((0xffff00f2 >>> m) & 1)); 
        int index;
        while ((index = hash & ((1 << nbits) - 1)) > m)       
            hash = (hash >>> nbits) | (hash << (33 - nbits)); 
        return index;
	}
	
	private void createSlot(int index) {
		Slot newSlot = new Slot();
		Slot[] a = arena;
		synchronized (a) {
			if(a[index] == null) {
				a[index] = newSlot;
			}
		}
	}
	
	private static boolean tryCancel(Node node, Slot slot) {
		if(!node.compareAndSet(null, CANCEL)) {
			return false;
		}
		if(slot.get() == node) {
			slot.compareAndSet(node, null);
		}
		return true;
	}
	
	private Object spinWait(Node node, Slot slot) {
		int spins = SPINS;
		for(;;) {
			Object v = node.get();
			if(v != null) {
				return v;
			} else if(spins > 0) {
				--spins;
			} else {
				tryCancel(node, slot);
			}
		}
	}

	private Object await(Node node, Slot slot) {
		Thread w = Thread.currentThread();
		int spins = SPINS;
		for(;;) {
			Object v = node.get();
			if(v != null) {
				return v;
			} else if(spins > 0) {
				--spins;
			} else if(node.waiter == null) {
				node.waiter = w;
			} else if(w.isInterrupted()) {
				tryCancel(node, slot);
			} else {
				LockSupport.park(node);
			}
		}
	}

	private Object awaitNanos(Node node, Slot slot, long nanos) {
		int spins = TIME_SPINS;
		long lastTime = 0;
		Thread w = null;
		for(;;) {
			Object v = node.get();
			if(v != null) {
				return v;
			} 
			long now = System.nanoTime();
			if(w == null) {
				w = Thread.currentThread();
			} else {
				nanos -= now - lastTime;
			}
			
			lastTime = now;
			if(nanos > 0) {
				if(spins > 0) {
					-- spins;
				} else if(node.waiter == null) {
					node.waiter = w; 
				} else if(w.isInterrupted()) {
					tryCancel(node, slot);
				} else {
					LockSupport.parkNanos(node, nanos);
				}
			} else if(tryCancel(node, slot) && !w.isInterrupted()) {
				return scanOnTimeout(node);
			}
		}
	}

	private Object scanOnTimeout(Node node) {
		Object y;
		for(int j = arena.length - 1; j >= 0; --j) {
			Slot slot = arena[j];
			if(slot != null) {
				while((y = slot.get()) != null) {
					if(slot.compareAndSet(y, null)) {
						Node you = (Node)y;
						if(you.compareAndSet(null, node.item)) {
							LockSupport.unpark(you.waiter);
							return you.item;
						}
					}
				}
			}
		}
		return CANCEL;
	}
	
	public Exchanger() {
		
	}
	
	/** 
	 * 等待其他线程到达交换点，然后与其进行数据交换。 
	 * 
	 * 如果其他线程到来，那么交换数据，返回。 
	 * 
	 * 如果其他线程未到来，那么当前线程等待，知道如下情况发生： 
	 *   1.有其他线程来进行数据交换。 
	 *   2.当前线程被中断。 
	 */  
	public V exchange(V x) throws InterruptedException {
		if(!Thread.interrupted()) {
			//检测当前线程是否被中断。  
			
			//进行数据交换。  
			Object v = doExchange(x == null ? NULL_ITEM : x , false, 0);
			if(v == NULL_ITEM) {
				//检测结果是否为null。  
				return null;
			}
			if(v != CANCEL) {
				//检测是否被取消。  
				return (V) v;
			}
			Thread.interrupted();
			// 清除中断标记。  
		}
		throw new InterruptedException();
	}
	
	/** 
	 * 等待其他线程到达交换点，然后与其进行数据交换。 
	 *  
	 * 如果其他线程到来，那么交换数据，返回。 
	 *  
	 * 如果其他线程未到来，那么当前线程等待，知道如下情况发生： 
	 *   1.有其他线程来进行数据交换。 
	 *   2.当前线程被中断。 
	 *   3.超时。 
	 */  
	public V exchange(V x, long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
		if(!Thread.interrupted()) {
			Object v = doExchange(x == null ? NULL_ITEM : x, true, unit.toNanos(timeout));
			if(v == NULL_ITEM) {
				return null;
			}
			if(v != CANCEL) {
				return (V)v;
			}
			if(!Thread.interrupted()) {
				throw new TimeoutException();
			}
		}
		throw new InterruptedException();
	}
}
