package com.jerry.soundcode.list;

import java.io.Serializable;
import java.util.NoSuchElementException;

public class LinkedList<T> extends AbstractSequentialList<T>
	implements List<T>, Deque<T>, Cloneable, Serializable {

	private static final long serialVersionUID = 1L;

	private transient Entry<T> header = new Entry<T> (null,null,null);
	private transient int size = 0;
	
	public LinkedList() {
		header.next = header.previous = header;
	}
	
	public LinkedList(Collection<? extends T> c) {
		this();
		addAll(c);
	}
	
	@Override
	public T getFirst() {
		if(size == 0) {
			throw new NoSuchElementException();
		}
		return header.next.element;
	}
	
	@Override
	public T getLast() {
		if(size == 0) {
			throw new NoSuchElementException();
		}
		return header.previous.element;
	}
	
	@Override
	public T removeFirst() {
		return remove(header.next);
	}
	
	@Override
	public T removeLast() {
		return remove(header.previous);
	}
	
	@Override
	public void addFirst(T t) {
		addBefore(t, header.next);
	}

	@Override
	public void addLast(T t) {
		addBefore(t, header);
	}

	@Override
	public boolean contains(Object o) {
		return indexOf(o) != -1;
	}
	
	@Override
	public int size() {
		return size;
	}
	
	public boolean add(T t) {
		addBefore(t, header);
		return true;
	}
	
	
	public boolean remove(Object o) {
		if(o == null) {
			for(Entry<T> e = header.next; e != header; e = e.next) {
				if(e.element == null) {
					remove(e);
					return true;
				}
			}
		} else {
			for(Entry<T> e = header.next; e != header; e = e.next) {
				if(o.equals(e.element)) {
					remove(e);
					return true;
				}
			}
		}
		return false;
	}
	
	public boolean addAll(Collection<? extends T> c) {
		return addAll(size, c);
	}
	
	@Override
	public boolean offerFirst(T t) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean offerLast(T t) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public T pollFirst() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public T pollLast() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public T peekFirst() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public T peekLast() {
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
	public boolean offer(T t) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public T remove() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public T poll() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public T element() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public T peek() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void push(T t) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public T pop() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterator<T> descendingIterator() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ListIterator<T> listIterator(int index) {
		// TODO Auto-generated method stub
		return null;
	}



	private Entry<T> addBefore(T t, Entry<T> entry) {
		Entry<T> newEntry = new Entry<T>(t, entry, entry.previous);
		newEntry.previous.next = newEntry;
		newEntry.next.previous = newEntry;
		size ++;
		modCount ++;
		return newEntry;
	}
	
	
	private T remove(Entry<T> e) {
		if(e == header) {
			throw new NoSuchElementException();
		}
		
		T result = e.element;
		e.previous.next = e.next;
		e.next.previous = e.previous;
		e.next = e.previous = null;
		e.element = null;
		size --;
		modCount ++;
		return result;
	}
	
	private static class Entry<T> {
		T element;
		Entry<T> next;
		Entry<T> previous;
		
		Entry(T element, Entry<T> next, Entry<T> previous) {
			this.element = element;
			this.next = next;
			this.previous = previous;
		}
	}

}
