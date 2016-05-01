package com.jerry.soundcode.list;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ConcurrentModificationException;
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
	
	@Override
	public boolean add(T t) {
		addBefore(t, header);
		return true;
	}
	
	@Override
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
	
	@Override
	public boolean addAll(Collection<? extends T> c) {
		return addAll(size, c);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public boolean addAll(int index, Collection<? extends T> c) {
		if(index < 0 || index > size) {
			throw new IndexOutOfBoundsException("Index: " + index + ", Size: +" + size);
		}
		
		Object[] a = c.toArray();
		int numNew = a.length;
		if(numNew == 0) {
			return false;
		}
		
		modCount ++;
		Entry<T> successor = (index == size ? header : entry(index));
		Entry<T> predecssor = successor.previous;
		
		for(int i = 0; i < numNew; i++) {
			Entry<T> e = new Entry<T>((T) a[i], successor, predecssor);
			predecssor.next = e;
			predecssor = e;
		}
		successor.previous = predecssor;
		size += numNew;
		return true;
	}
	
	@Override
	public void clear() {
		Entry<T> e = header.next;
		while(e != header) {
			Entry<T> next = e.next;
			e.next = e.previous = null;
			e.element = null;
			e = next;
		}
		header.next = header.previous = header;
		size = 0;
		modCount ++;
	}
	
	@Override
	public T get(int index) {
		return entry(index).element;
	}
	
	@Override
	public T set(int index, T element) {
		Entry<T> e = entry(index);
		T oldValue = e.element;
		e.element = element;
		return oldValue;
	}
	
	@Override
	public void add(int index, T element) {
		addBefore(element, (index == size ? header : entry(index)));
	}
	
	@Override
	public T remove(int index) {
		return remove(entry(index));
	}
	
	private Entry<T> entry(int index) {
		if(index < 0 || index >= size) {
			 throw new IndexOutOfBoundsException("Index: "+index+ ", Size: "+size);
		}
		Entry<T> e = header;
		if(index < (size >> 1)) {
			for(int i = 0; i <= index; i++) {
				e = e.next;
			}
		} else {
			for(int i = size; i > index; i--) {
				e = e.previous;
			}
		}
		return e;
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public int indexOf(Object o) {
		int index = 0;
		if(o == null) {
			for(Entry e = header.next; e != header; e = e.next) {
				if(e.element == null) {
					return index;
				} 
				index ++;
			}
		} else {
			for(Entry e = header.next; e != header; e = e.next) {
				if(o.equals(e.element)) {
					return index;
				}
				index ++;
			}
		}
		return -1;
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public int lastIndexOf(Object o) {
		int index = size;
		if(o == null) {
			for(Entry e = header.previous; e != header; e = e.previous) {
				index --;
				if(e.element == null) {
					return index;
				}
			}
		} else {
			for(Entry e = header.previous; e != header; e = e.previous) {
				index --;
				if(o.equals(e.element)) {
					return index;
				}
			}
		}
		
		return -1;
	}
	
	@Override
	public T peek() {
		if(size == 0) {
			return null;
		}
		return getFirst();
	}
	
	@Override
	public T element() {
		return getFirst();
	}
	
	@Override
	public T poll() {
		if(size == 0) {
			return null;
		}
		return removeFirst();
	}

	@Override
	public T remove() {
		return removeFirst();
	}
	
	@Override
	public boolean offer(T t) {
		return add(t);
	}
	
	@Override
	public boolean offerFirst(T t) {
		addFirst(t);
		return true;
	}

	@Override
	public boolean offerLast(T t) {
		addLast(t);
		return true;
	}

	@Override
	public T peekFirst() {
		if(size == 0) {
			return null;
		}
		return getFirst();
	}

	@Override
	public T peekLast() {
		if(size == 0) {
			return null;
		}
		return getLast();
	}
	
	@Override
	public T pollFirst() {
		if(size == 0) {
			return null;
		}
		return removeFirst();
	}

	@Override
	public T pollLast() {
		if(size == 0) {
			return null;
		}
		return removeLast();
	}

	@Override
	public void push(T t) {
		addFirst(t);
	}


	@Override
	public T pop() {
		return removeFirst();
	}

	@Override
	public boolean removeFirstOccurrence(Object o) {
		return remove(o);
	}

	@Override
	public boolean removeLastOccurrence(Object o) {
		if(o == null) {
			for(Entry<T> e = header.previous; e != header; e = e.previous) {
				if(e.element == null) {
					remove(e);
					return true;
				}
			}
		} else {
			for(Entry<T> e = header.previous; e != header; e = e.previous) {
				if(o.equals(e.element)) {
					remove(e);
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public ListIterator<T> listIterator(int index) {
		return new ListItr(index);
	}

	private class ListItr implements ListIterator<T> {
		
		private Entry<T> lastReturned = header;
		private Entry<T> next;
		private int nextIndex;
		private int expectedModCount = modCount;
		
		ListItr(int index) {
			if(index < 0 || index > size) {
				throw new IndexOutOfBoundsException("Index: "+index+ ", Size: "+size);
			}
			if(index < (size >> 1)) {
				next = header.next;
				for(nextIndex = 0; nextIndex < index; nextIndex++) {
					next = next.next;
				} 
			} else {
				next = header;
				for(nextIndex = size; nextIndex > index; nextIndex--) {
					next = next.previous;
				}
			}
		}
		
		@Override
		public boolean hasNext() {
			return nextIndex != size;
		}

		@Override
		public T next() {
			checkForComodification();
			if(nextIndex == size) {
				throw new NoSuchElementException();
			}
			
			lastReturned = next;
			next = next.next;
			nextIndex++;
			
			return lastReturned.element;
		}

		@Override
		public boolean hasPrevious() {
			return nextIndex != 0;
		}

		@Override
		public T previous() {
			if(nextIndex == 0) {
				throw new NoSuchElementException();
			}
			
			lastReturned = next = next.previous;
			nextIndex --;
			checkForComodification();
			return lastReturned.element;
		}

		@Override
		public int nextIndex() {
			return nextIndex;
		}

		@Override
		public int previousIndex() {
			return nextIndex - 1;
		}

		@Override
		public void remove() {
			checkForComodification();
			Entry<T> lastNext = lastReturned.next;
			
			LinkedList.this.remove(lastReturned);
			
			if(next == lastReturned){
				next = lastNext;
			} else {
				nextIndex --;
			}
			
			lastReturned = header;
			expectedModCount++;
		}

		@Override
		public void set(T t) {
			if(lastReturned == header) {
				throw new IllegalStateException();
			}
			checkForComodification();
			lastReturned.element = t;
		}

		@Override
		public void add(T t) {
			checkForComodification();
			lastReturned = header;
			addBefore(t, next);
			nextIndex ++;
			expectedModCount ++;
		}
		
		
		final void checkForComodification() {
			if(modCount != expectedModCount) {
				throw new ConcurrentModificationException();
			} 
		}
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
	
	@SuppressWarnings("unchecked")
	@Override
	public Iterator<T> descendingIterator() {
		return new DescendingIterator();
	}
	
	@SuppressWarnings("rawtypes")
	private class DescendingIterator implements Iterator {

		final ListItr itr = new ListItr(size());	
		
		@Override
		public boolean hasNext() {
			return itr.hasPrevious();
		}

		@Override
		public Object next() {
			return itr.previous();
		}

		@Override
		public void remove() {
			itr.remove();
		}
		
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Object clone() {
		LinkedList<T> clone = null;
		
		try {
			clone = (LinkedList<T>) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new InternalError();
		}
		
		clone.header = new Entry<T> (null, null, null);
		clone.header.next = clone.header.previous = clone.header;
		clone.size = 0;
		clone.modCount = 0;
		
		for(Entry<T> e = header.next; e != null; e = e.next) {
			clone.add(e.element);
		}
		
		return clone;
	}
	
	@Override
	public Object[] toArray() {
		Object[] result = new Object[size];
		int i = 0;
		for(Entry<T> t = header.next; t != header; t = t.next) {
			result[i++] = t.element;
		}
		return result;
	}
	
	@SuppressWarnings({ "unchecked", "hiding" })
	@Override
	public <T> T[] toArray(T[] a) {
		if(a.length < size) {
			a = (T[]) Array.newInstance(a.getClass().getComponentType(), size);
		} 
		
		int i = 0;
		Object[] result = a;
		
		for(Entry<T> e = (Entry<T>) header.next; e != header; e = e.next) {
			result[i++] = e.element;
		}
		
		if(a.length > size) {
			a[size] = null;
		}
		return a;
	}
	
	@SuppressWarnings("rawtypes")
	private void writeObject(ObjectOutputStream s) throws IOException {
		s.defaultWriteObject();
		s.writeInt(size);
		
		for(Entry e = header.next; e != header; e = e.next) {
			s.writeObject(e.element);
		}
	}
	
	@SuppressWarnings("unchecked")
	private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
		s.defaultReadObject();
		
		int size = s.readInt();
		
		header = new Entry<T> (null, null, null);
		header.next = header.previous = header;
		
		for(int i = 0; i < size; i++) {
			addBefore((T)s.readObject(), header);
		}
	}
}
