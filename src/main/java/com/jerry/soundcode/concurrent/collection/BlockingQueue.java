package com.jerry.soundcode.concurrent.collection;

import java.util.concurrent.TimeUnit;

import com.jerry.soundcode.list.Collection;
import com.jerry.soundcode.list.Queue;

public interface BlockingQueue<T> extends Queue<T> {
	
	/**
	 *  队列没满的话，放入成功。否则抛出异常
	 */
	boolean add(T t);
	
	/**
	 * 表示如果可能的话,将object加到BlockingQueue里,即如果BlockingQueue可以容纳,则返回true,否则返回false.（本方法不阻塞当前执行方法的线程）
	 */
	boolean offer(T t);
	
	/**
	 * 可以设定等待的时间，如果在指定的时间内，还不能往队列中加入BlockingQueue，则返回失败。
	 */
	boolean offer(T t, long timeout, TimeUnit unit) throws InterruptedException;
	
	/**
	 * 把object加到BlockingQueue里,如果BlockQueue没有空间,则调用此方法的线程阻塞。直到BlockingQueue里面有空间再继续
	 */
	void put(T t) throws InterruptedException;
	
	/**
	 * 取走BlockingQueue里排在首位的对象,若BlockingQueue为空,阻断进入等待状态直到BlockingQueue有新的数据被加入; 
	 */
	T take() throws InterruptedException;
	
	/**
	 * 取走BlockingQueue里排在首位的对象,若不能立即取出,则可以等time参数规定的时间,取不到时返回null;
	 */
	T poll(long timeout, TimeUnit unit) throws InterruptedException;
	
	int remainingCapacity();
	
	boolean remove(Object o);
	
	boolean contains(Object o);
	
	/**
	 *  一次性从BlockingQueue获取所有可用的数据对象（还可以指定获取数据的个数），通过该方法，可以提升获取数据效率；不需要多次分批加锁或释放锁。
	 */
	int drainTo(Collection<? super T> c);
	
	/**
	 *  一次性从BlockingQueue获取所有可用的数据对象,指定获取数据的个数，通过该方法，可以提升获取数据效率；不需要多次分批加锁或释放锁。
	 */
	int drainTo(Collection<? super T> c, int maxElements);
}
