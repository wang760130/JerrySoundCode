package com.jerry.soundcode.list;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.NoSuchElementException;

public class Vector<T> extends AbstractList<T>
	implements List<T>, RandomAccess, Cloneable, Serializable {

	private static final long serialVersionUID = 1L;

	protected Object[] elementData;
	
	protected int elementCount;
	
	protected int capacityIncrement;
	
	public Vector(int initialCapacity, int capacityIncrement) {
		super();
		if (initialCapacity < 0)
			throw new IllegalArgumentException("Illegal Capacity: " + initialCapacity);
		
		this.elementData = new Object[initialCapacity];
		this.capacityIncrement = capacityIncrement;
	}
	
	public Vector(int initialCapactiy) {
		this(initialCapactiy,0);
	}
	
	public Vector() {
		this(10);
	}
	
	public Vector(Collection<? extends T> c) {
		elementData = c.toArray();
		elementCount = elementData.length;
		
		if(elementData.getClass() != Object[].class) {
			elementData = Arrays.copyOf(elementData, elementCount, Object[].class);
		}
	}
	
	public synchronized void copyInto(Object[] anArray) {
		System.arraycopy(elementData, 0, anArray, 0, elementCount);
	}
	
	public synchronized void trimToSize() {
		modCount ++;
		int oldCapacity = elementData.length;
		if(elementCount < oldCapacity) {
			elementData = Arrays.copyOf(elementData, elementCount);
		}
	}
	
	public synchronized void ensureCapacity(int minCapacity) {
		minCapacity ++;
		ensureCapacity(minCapacity);
	}
	
	private void ensureCapacityHelper(int minCapacity) {
		int oldCapacity = elementData.length;
		if(minCapacity > oldCapacity) {
			int newCapacity = (capacityIncrement > 0) ? (oldCapacity + capacityIncrement) : (oldCapacity * 2);
			if(newCapacity < minCapacity) {
				newCapacity = minCapacity;
			}
			elementData = Arrays.copyOf(elementData, newCapacity);
		}
	}
	
	public synchronized void setSize(int newSize) {
		modCount ++;
		if(newSize > elementCount) {
			ensureCapacityHelper(newSize);
		} else {
			for(int i = newSize; i < elementCount; i++) {
				elementData[i] = null;
			}
		}
		elementCount = newSize;
	}
	
	public synchronized int capacity() {
		return elementData.length;
	}
	
	@Override
	public synchronized int size() {
		return elementCount;
	}
	
	@Override
	public synchronized boolean isEmpty() {
		return elementCount == 0;
	}
	
	public Enumeration<T> elements() {
		return new Enumeration<T>() {
			int count = 0;
			
			
			@Override
			public boolean hasMoreElements() {
				return count < elementCount;
			}

			@Override
			public T nextElement() {
				synchronized(Vector.class) {
					if(count < elementCount) {
						return (T) elementData[count++];
					}
				}
				throw new NoSuchElementException("Vector Enumeration");
			}
			
		};
	}
	
	@Override
	public boolean contains(Object o) {
		return indexOf(o, 0) >= 0;
	}
	
	@Override
	public int indexOf(Object o) {
		return indexOf(o, 0);
	}
	
	public synchronized int indexOf(Object o, int index) {
		if(o == null) {
			for(int i = index; i < elementCount; i++) {
				if(elementData[i] == null) {
					return i;
				}
			}
		} else {
			for(int i = index; i < elementCount; i++) {
				if(o.equals(elementData[i])) {
					return i;
				}
			}
		}
		return -1;
	}
	
	@Override
	public synchronized int lastIndexOf(Object o) {
		return lastInfexOf(o, elementCount - 1);
	}
	
	public synchronized int lastInfexOf(Object o, int index) {
		if(index >= elementCount) {
			throw new IndexOutOfBoundsException(index + " >= "+ elementCount);
		}
		if(o == null) {
			for(int i = index; i >= 0; i--) {
				if(elementData[i] == null)
					return i;
			}
		} else {
			for(int i = index; i >= 0; i--) {
				if(o.equals(elementData[i])) {
					return i;
				}
			}
		}
		return -1;
	}
	
	public synchronized T elementAt(int index) {
		if(index >= elementCount) {
			throw new ArrayIndexOutOfBoundsException(index + " >= " + elementCount);
		}
		return (T) elementData[index];
	}

	public synchronized T firstElement() {
		if(elementCount == 0) {
			throw new NoSuchElementException();
		}
		return (T) elementData[0];
	}
	
	public synchronized T lastElement() {
		if(elementCount == 0) {
			throw new NoSuchElementException();
		}
		return (T) elementData[elementCount - 1];
	}
	
	public synchronized void setElementAt(T t, int index) {
		if(index >= elementCount) {
			throw new ArrayIndexOutOfBoundsException(index + " >= " + elementCount);
		}
		elementData[index] = t;
	}
	
	public synchronized void removeElementAt(int index) {
		modCount ++;
		if(index >= elementCount) {
			 throw new ArrayIndexOutOfBoundsException(index + " >= " +
				     elementCount);
		} else if(index < 0) {
			throw new ArrayIndexOutOfBoundsException(index);
		}
		
		int j = elementCount - index - 1;
		if(j > 0) {
			System.arraycopy(elementData, index + 1, elementData, index, j);
		}
		elementCount --;
		elementData[elementCount] = null;
	}
	
	public synchronized void insertElementAt(T t, int index) {
		modCount ++;
		if(index > elementCount) {
			 throw new ArrayIndexOutOfBoundsException(index
				     + " > " + elementCount);
		}
		ensureCapacityHelper(elementCount + 1);
		System.arraycopy(elementData, index, elementData, index + 1, elementCount);
		elementData[index] = t;
		elementCount++;
	}
	
	public synchronized void addElement(T t) {
		modCount++;
		ensureCapacityHelper(elementCount + 1);
		elementData[elementCount++] = t;
	}
	
	public synchronized boolean removeElement(Object obj) {
		modCount ++;
		int i = indexOf(obj);
		if(i >= 0) {
			removeElementAt(i);
			return true;
		}
		return false;
	}
	
	public synchronized void removeAllElements() {
		for(int i = 0; i < elementCount; i++) {
			elementData[i] = null;
		}
		elementCount = 0;
	}

	@Override
	public synchronized Object clone() {
		try {
			Vector<T> v = (Vector<T>) super.clone();
			v.elementData = Arrays.copyOf(elementData, elementCount);
			v.modCount = 0;
			return v;
		} catch (CloneNotSupportedException e) {
			throw new InternalError();
		}
	}
	
	@Override
	public synchronized Object[] toArray() {
		return Arrays.copyOf(elementData, elementCount);
	}
	
	@Override
	public synchronized <T> T[] toArray(T[] t) {
		if(t.length < elementCount) {
			return (T[]) Arrays.copyOf(elementData, elementCount, t.getClass());
		}
		
		System.arraycopy(elementCount, 0, t, 0, elementCount);
		
		if(t.length > elementCount) {
			t[elementCount] = null;
		}
		
		return t;
	}
	
	@Override
	public T get(int index) {
		if(index >= elementCount) {
			throw new ArrayIndexOutOfBoundsException(index);
		}
		return (T) elementData[index];
	}

	@Override
	public synchronized T set(int index, T element) {
		if(index >= elementCount) {
			throw new ArrayIndexOutOfBoundsException(index);
		}
		
		Object oldValue = elementData[index];
		elementData[index] = element;
		return (T) oldValue;
	}
	
	@Override
	public synchronized boolean add(T t) {
		modCount ++;
		ensureCapacityHelper(elementCount + 1);
		elementData[elementCount ++] = t;
		return true;
	}
	
	@Override
	public boolean remove(Object obj) {
		return removeElement(obj);
	}
	
	@Override
	public void add(int index, T t) {
		insertElementAt(t, index);
	}
	
	@Override
	public synchronized T remove(int index) {
		modCount++;
		if(index >= elementCount)
			throw new ArrayIndexOutOfBoundsException(index);
		Object oldValue = elementData[index];
		
		int numMoved = elementCount - index - 1;
		if(numMoved > 0) {
			System.arraycopy(elementData, index + 1, elementData, index, numMoved);
		}
		
		elementData[--elementCount] = null;
		return (T) oldValue;
	}
	
	@Override
	public void clear() {
		removeAllElements();
	}
	
	@Override
	public synchronized boolean containsAll(Collection<?> c) {
		return super.containsAll(c);
	}
	
	@Override
	public synchronized boolean addAll(Collection<? extends T> c) {
		modCount++;
		Object[] a = c.toArray();
		int numNew = a.length;
		ensureCapacityHelper(elementCount + numNew); 
		System.arraycopy(a, 0, elementData, elementCount, numNew);
		elementCount += numNew;
		return numNew != 0;
	}
	
	@Override
	public synchronized boolean removeAll(Collection<?> c) {
		return super.removeAll(c);
	}
	
	@Override
	public synchronized boolean retainAll(Collection<?> c) {
		return super.retainAll(c);
	}
	
	@Override
	public synchronized boolean addAll(int index, Collection<? extends T> c) {
		modCount++;
		if(index < 0 || index > elementCount) {
			throw new ArrayIndexOutOfBoundsException(index);
		}
		Object[] a = c.toArray();
		int numNew = a.length;
		ensureCapacityHelper(elementCount +  numNew);
		
		int numMoved = elementCount - index;
		if(numMoved > 0) {
			System.arraycopy(elementData, index, elementData, index + numNew, numNew);
		}
		System.arraycopy(a, 0, elementData, index, numNew);
		elementCount += numNew;
		return numNew != 0;
	}
	
	@Override
	public synchronized boolean equals(Object obj) {
		return super.equals(obj);
	}
	
	@Override
	public synchronized int hashCode() {
		return super.hashCode();
	}
	
	@Override
	public synchronized String toString() {
		return super.toString();
	}
	
	@Override
	public synchronized List<T> subList(int fromIndex, int toIndex) {
		//return Collections.synchronizedList(super.subList(fromIndex, toIndex), this);
		return null;
	}
	
	protected synchronized void removeRange(int fromIndex, int toIndex) {
		modCount ++;
		int numMoved = elementCount - toIndex;
		System.arraycopy(elementData, toIndex, elementData, fromIndex, numMoved);
		
		int newElementCount = elementCount - (toIndex - fromIndex);
		while(elementCount != newElementCount) {
			elementData[--elementCount] = null;
		}
	}
	
	private synchronized void writeObject(ObjectOutputStream s) throws IOException {
		s.defaultWriteObject();
	}
	
} 
