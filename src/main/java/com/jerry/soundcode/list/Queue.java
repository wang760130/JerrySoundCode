package com.jerry.soundcode.list;

public interface Queue<T> extends Collection<T> {

	/**
	 * 增加一个元索,如果队列已满，则抛出一个IIIegaISlabEepeplian异常
	 */
	boolean add(T t);
	
	/**
	 * 添加一个元素并返回true,如果队列已满，则返回false
	 * @param t
	 * @return
	 */
	boolean offer(T t);
	
	/**
	 * 移除并返回队列头部的元素,如果队列为空，则抛出一个NoSuchElementException异常
	 * @param t
	 * @return
	 */
	T remove();
	
	/**
	 * 移除并返问队列头部的元素, 如果队列为空，则返回null
	 * @return
	 */
	T poll();
	
	/**
	 * 返回队列头部的元素, 如果队列为空，则抛出一个NoSuchElementException异常
	 * @return
	 */
	T element();
	
	/**
	 * 返回队列头部的元素,如果队列为空，则返回null
	 * @return
	 */
	T peek();
}
