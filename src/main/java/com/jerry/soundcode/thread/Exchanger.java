package com.jerry.soundcode.thread;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.jerry.soundcode.concurrent.atomic.AtomicInteger;
import com.jerry.soundcode.concurrent.atomic.AtomicReference;
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
	
	/** 
	 * 单核处理器下这个自旋次数为0 
	 * 多核情况下，这个值设置为大多数系统中上下文切换时间的平均值。 
	 */ 
	private static final int SPINS = (NCPU == 1) ? 0 : 2000;
	
	/** 
	 * 在有超时情况下阻塞等待之前自旋的次数。. 
	 * 超时等待的自旋次数之所以更少，是因为检测时间也需要耗费时间。 
	 * 这里的值是一个经验值。 
	 */  
	private static final int TIME_SPINS = SPINS / 20;
	
	private static final Object CANCEL = new Object();
	
	private static final Object NULL_ITEM = new Object();
	
	private static final class Node extends AtomicReference<Object> {
		
		private static final long serialVersionUID = 1L;

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
	@SuppressWarnings("serial")
	private static final class Slot extends AtomicReference<Object> {
		@SuppressWarnings("unused")
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
		// 创建当前节点me。  
		Node me = new Node(item);
		// 计算出当前slot的下标。
		int index = hashIndex();
		// 用来保存CAS失败的次数。
		int fails = 0;
		
		for(;;) {
			// 用来保存当前slot中可能存在的Node。
			Object y ;
			// 按照前面计算出的下标获取当前slot。
			Slot slot = arena[index];
			if(slot == null) {
				// 如果slot为null，那么创建一个slot，然后继续循环。  
				createSlot(index);
			} else if((y = slot.get()) != null &&
					slot.compareAndSet(y, null)) {
				// 如果slot不为空，那么slot可能被另一个Node给占了，如果确实存在这个Node，尝试将其置空。(表示当前节点要和这个Node交换数据了)  
				
				// 给这个Node转型，赋给you。
				Node you = (Node)y;
				if(you.compareAndSet(null, item)) {
					// 将item设置给you，注意you本身是一个AtomicReference，这里相当于把item设置到you的value字段上。  
					
					// 然后唤醒you节点上等待的线程。
					LockSupport.unpark(you.waiter);
					// 返回you的item。
					return you.item;
				}
			} else if(y == null && 
					slot.compareAndSet(null, me)) {
				// 竞争失败，放弃，继续循环。 
				// 如果slot为空，那么说明没有要和当前线程交换数据的线程，  
				// 那么当前线程先尝试把这个slot给占了。
				
				if(index == 0) {
					// 如果slot下标为0，那么阻塞等待。
					return timed ? awaitNanos(me, slot, nanos) : await(me, slot);
				}
				
				// 如果slot下标不是0，自旋等待，等待其他线程来和当前线程交换数据，然后返回交换后的数据。  
				Object v = spinWait(me, slot);
				
				if(v != CANCEL) {
					return v;
				}
				
				// 如果取消的话，重试，重建一个Node，之前的Node就丢弃了。  
				me = new Node(item);
				// 获取当前slot下标的最大值。
				int m = max.get();
				if(m > (index >>>= 1)) {
					// 如果当前允许的最大索引太大。 
					
					// 递减最大索引  
					max.compareAndSet(m, m - 1);
				}
			} else if(++fails > 1) {
				// 如果1个slot竞争失败超过2次。
				int m = max.get();
				if(fails > 3 && m < FULL && max.compareAndSet(m, m + 1)) {
					//如果竞争失败超过3次，尝试递增最大索引值。  
					
					// 增加索引值。
					index = m + 1;
				} else if(--index < 0) {
					// 换个index。
					
					// 绕回逻辑，防止index越界。
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
		// 在同步块外面创建Slot实例，以减小同步块范围。
		Slot newSlot = new Slot();
		
		Slot[] a = arena;
		synchronized (a) {
			if(a[index] == null) {
				a[index] = newSlot;
			}
		}
	}
	
	private static boolean tryCancel(Node node, Slot slot) {
		// 	尝试取消node 
		if(!node.compareAndSet(null, CANCEL)) {
			return false;
		}
		if(slot.get() == node) {
			// 如果还关联在sot上，断开关联。
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
				// 先自旋  
				--spins;
			} else {
				// 自旋了指定的次数还没等到交换的数据，尝试取消。
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
				// 如果已经被其他线程填充了值，那么返回这个值。
				return v;
			} else if(spins > 0) {
				// 先自旋几次。
				--spins;
			} else if(node.waiter == null) {
				// 自旋阶段完毕后，将当前线程设置到node的waiter域。  
				node.waiter = w;
			} else if(w.isInterrupted()) {
				// 如果当前线程被中断，尝试取消当前node。 
				tryCancel(node, slot);
			} else {
				// 否则阻塞当前线程。  
				LockSupport.park(node);
			}
		}
	}

	/** 
	 * 在下标为0的Slot上等待获取其他线程填充的值。 
	 * 如果在Slot被填充之前超时或者被中断，那么操作失败。 
	 */ 
	private Object awaitNanos(Node node, Slot slot, long nanos) {
		int spins = TIME_SPINS;
		long lastTime = 0;
		Thread w = null;
		for(;;) {
			Object v = node.get();
			if(v != null) {
				//如果已经被其他线程填充了值，那么返回这个值。  
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
					// 先自旋几次。  
					-- spins;
				} else if(node.waiter == null) {
					// 自旋阶段完毕后，将当前线程设置到node的waiter域。
					node.waiter = w; 
				} else if(w.isInterrupted()) {
					// 如果当前线程被中断，尝试取消node。
					tryCancel(node, slot);
				} else {
					// 阻塞给定的时间。  
					LockSupport.parkNanos(node, nanos);
				}
			} else if(tryCancel(node, slot) && !w.isInterrupted()) {
				// 超时后，如果当前线程没有被中断，那么从Slot数组的其他位置看看有没有等待交换数据的节点
				return scanOnTimeout(node);
			}
		}
	}

	private Object scanOnTimeout(Node node) {
		Object y;
		for(int j = arena.length - 1; j >= 0; --j) {
			// 从Slot数组的后面往前找
			Slot slot = arena[j];
			if(slot != null) {
				// 找到了有初始化好的Slot，然后看看里面有没有node。
				while((y = slot.get()) != null) {
					if(slot.compareAndSet(y, null)) {
						// 发现有node，尝试和这个node进行数据交换。  
						Node you = (Node)y;
						if(you.compareAndSet(null, node.item)) {
							// 尝试进行数据交换
							
							// 如果交换成功(把当前节点的数据交给you)，唤醒you上面等待的线程。
							LockSupport.unpark(you.waiter);
							// 返回you的数据。
							return you.item;
						}
					}
				}
			}
		}
		// 没找到其他等待交换数据的线程，最后取消当前节点node。
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
	@SuppressWarnings("unchecked")
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
	@SuppressWarnings("unchecked")
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
