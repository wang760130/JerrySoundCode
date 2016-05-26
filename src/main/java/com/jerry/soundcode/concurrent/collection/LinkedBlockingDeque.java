package com.jerry.soundcode.concurrent.collection;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

import com.jerry.soundcode.list.AbstractQueue;
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
	public Iterator<E> iterator() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int size() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int remainingCapacity() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int drainTo(com.jerry.soundcode.list.Collection<? super E> c) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int drainTo(com.jerry.soundcode.list.Collection<? super E> c,
			int maxElements) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public E removeFirst() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public E removeLast() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public E pollFirst() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public E pollLast() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public E getFirst() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public E getLast() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public E peekFirst() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public E peekLast() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void push(E t) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public E pop() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterator<E> descendingIterator() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addFirst(E e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addLast(E e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean offerFirst(E e) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean offerLast(E e) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void putFirst(E e) throws InterruptedException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void putLast(E e) throws InterruptedException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean offerFirst(E e, long timeout, TimeUnit unit)
			throws InterruptedException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean offerLast(E e, long timeout, TimeUnit unit)
			throws InterruptedException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public E takeFirst() throws InterruptedException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public E takeLast() throws InterruptedException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public E pollFirst(long timeout, TimeUnit unit) throws InterruptedException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public E pollLast(long timeout, TimeUnit unit) throws InterruptedException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean removeFirstOccurrence(Object o) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean removeLastOccurrence(Object o) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void put(E e) throws InterruptedException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean offer(E e, long timeout, TimeUnit unit)
			throws InterruptedException {
		// TODO Auto-generated method stub
		return false;
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
	public void pust(E e) {
		// TODO Auto-generated method stub
		
	}

	

}
