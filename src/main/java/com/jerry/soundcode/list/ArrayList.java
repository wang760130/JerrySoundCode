package com.jerry.soundcode.list;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.ConcurrentModificationException;

public class ArrayList<T> extends AbstractList<T>
	implements List<T>, Cloneable, Serializable {

	private static final long serialVersionUID = 8683452581122892189L;

	private transient Object[] elementData;
	
	private int size;
	
	public ArrayList(int initialCapacity) {
		super();
		if(initialCapacity < 0) 
			  throw new IllegalArgumentException("Illegal Capacity: "+
                      initialCapacity);
		this.elementData = new Object[initialCapacity];
	}
	
	public ArrayList() {
		this(10);
	}
	
	public ArrayList(Collection<? extends T> c) {
		elementData = c.toArray();
		size = elementData.length;
		if (elementData.getClass() != Object[].class) {
			elementData = Arrays.copyOf(elementData, size, Object[].class);
		}
	}
	
	public void trimToSize() {
		modCount ++;
		int oldCapacity = elementData.length;
		if(size < oldCapacity) {
			elementData = Arrays.copyOf(elementData, size);
		}
	}
	
	public void ensureCapacity(int minCapacity) {
		modCount ++;
		int oldCapacity = elementData.length;
		if(minCapacity > oldCapacity) {
			Object oldData[] = elementData;
			int newCapcacity = (oldCapacity * 3) / 2 + 1;
			if(newCapcacity < minCapacity) 
				newCapcacity = minCapacity;
			elementData = Arrays.copyOf(elementData, newCapcacity);
		}
	}
	
	@Override
	public int size() {
		return size;
	}
	
	public boolean isEmpty() {
		return size == 0;
	}
	
	public boolean contains(Object o) {
		return indexOf(0) >= 0;
	}
	
	public int indexOf(Object o) {
		if(o == null) {
			for(int i = 0; i < size; i++) {
				if(elementData[i] == null) {
					return i;
				}
			}
		} else {
			for(int i = 0; i < size; i++) {
				if(o.equals(elementData[i])) {
					return i;
				}
			}
		}
		return -1;
	}
	
	public int lastIndexOf(Object o) {
		if(o == null) {
			for(int i = size - 1; i >= 0; i--) {
				if(elementData[i] == null) 
					return i;
			}
		} else {
			for(int i = size - 1; i >= 0; i--) {
				if(o.equals(elementData[i])) {
					return i;
				}
			}
		}
		return -1;
	}
	
	public Object clone() {
		try {
			ArrayList<T> list = (ArrayList<T>) super.clone();
			list.elementData = Arrays.copyOf(elementData, size);
			list.modCount = 0;
			return list;
		} catch (CloneNotSupportedException e) {
			 throw new InternalError();
		}
	}
	
	public Object[] toArray() {
		return Arrays.copyOf(elementData, size);
	}
	
	public <T> T[] toArray(T[] a) {
		if(a.length < size) {
			return (T[]) Arrays.copyOf(elementData, size);
		}
		System.arraycopy(elementData, 0, a, 0, size);
		if(a.length > size) {
			a[size] = null;
		}
		return a;
	}
	
	@Override
	public T get(int index) {
		this.RangeCheck(index);
		
		return (T) elementData[index];
	}

	public T set(int index, T t) {
		this.RangeCheck(index);
		
		T oldValue = (T) elementData[index];
		elementData[index] = elementData;
		return oldValue;
	}
	
	public boolean add(T t) {
		this.ensureCapacity(size + 1);
		elementData[size++] = t;
		return true;
	}
	
	public void add(int index, T t) {
		if (index > size || index < 0)
		    throw new IndexOutOfBoundsException(
			"Index: "+index+", Size: "+size);
		
		this.ensureCapacity(size + 1);
		System.arraycopy(elementData, index, elementData, index + 1, size - index);
		elementData[index] = elementData;
		size++;
	}
	
	public T remove(int index) {
		this.RangeCheck(index);
		
		modCount ++;
		T oldValue = (T) elementData[index];
		
		int numMoved = size - index - 1;
		if(numMoved > 0) {
			System.arraycopy(elementData, index + 1, elementData, index, numMoved);
		}
		return oldValue;
	}
	
	public boolean remove(Object o) {
		if(o == null) {
			for(int index = 0; index < size; index++) {
				if(elementData[index] == null) {
					this.fastRemove(index);
					return true;
				}
			}
		} else {
			for(int index = 0; index < size; index++) {
				if(o.equals(elementData[index])) {
					this.fastRemove(index);
					return true;
				}
			}
		}
		return false;
	}
	
	private void fastRemove(int index) {
		modCount ++;
		int numMoved = size - index - 1;
		if(numMoved > 0) 
			System.arraycopy(elementData, index + 1, elementData, index, numMoved);
		elementData[--size] = null;
	}
	
	public boolean addAll(Collection<? extends T> c) {
		Object[] a = c.toArray();
		int numNew = a.length;
		this.ensureCapacity(size + numNew);
		System.arraycopy(a, 0, elementData, size, numNew);
		size += numNew;
		return numNew != 0;
	}
	
	public boolean addAll(int index, Collection<? extends T> c) {
		if(index > size || index < 0)
			throw new IndexOutOfBoundsException(
						"Index: " + index + ", Size: " + size);
		
		Object[] a = c.toArray();
		int numNew = a.length;
		this.ensureCapacity(size + numNew);
		
		int numMoved = size - index;
		if(numMoved > 0) 
			System.arraycopy(elementData, index, elementData, index + numNew, numMoved);
		size += numNew;
		return numMoved != 0;
				
	}
	
	protected void removeRange(int fromIndex, int toIndex) {
		modCount ++;
		int numMoved = size - toIndex;
		System.arraycopy(elementData, toIndex, elementData, fromIndex, numMoved);
		
		int newSize = size - (toIndex - fromIndex);
		while(size != newSize) {
			elementData[--size] = null;
		}
	}
	
	private void RangeCheck(int index) {
		if(index >= size) {
			throw new IndexOutOfBoundsException(
					"Index: "+index+", Size: "+size);
		}
	}
	
	public void clear() {
		modCount ++;
		for(int i = 0; i < size; i++)
			elementData[i] = null;
		size = 0;
	}

	@Override
	public List<T> subList(int fromIndex, int toIndex) {
		// TODO Auto-generated method stub
		return null;
	}
	
	private void writeObejct(ObjectOutputStream s) 
		throws IOException {
		int expectedModCount = modCount;
		
		s.defaultWriteObject();
		s.writeInt(elementData.length);
		
		for(int i = 0; i < size; i++) 
			s.writeObject(elementData[i]);
		if(modCount !=expectedModCount) {
			throw new ConcurrentModificationException();
		}
	}
	
	private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
		s.defaultReadObject();
		
		int arrayLength = s.readInt();
		Object[] a = elementData = new Object[arrayLength];
		
		for(int i = 0; i < size; i++) {
			a[i] = s.readObject();
		}
	}
}
