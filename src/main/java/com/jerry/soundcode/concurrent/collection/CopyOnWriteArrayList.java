package com.jerry.soundcode.concurrent.collection;

import java.io.Serializable;
import java.util.Arrays;

import com.jerry.soundcode.concurrent.atomic.Unsafe;
import com.jerry.soundcode.concurrent.locks.ReentrantLock;
import com.jerry.soundcode.list.Collection;
import com.jerry.soundcode.list.Iterator;
import com.jerry.soundcode.list.List;
import com.jerry.soundcode.list.ListIterator;
import com.jerry.soundcode.list.RandomAccess;

public class CopyOnWriteArrayList<E> 
	implements List<E>, RandomAccess, Cloneable, Serializable {

	private static final long serialVersionUID = 1L;

	transient final ReentrantLock lock = new ReentrantLock();
	
	private volatile transient Object[] array;
	
	final Object[] getArray() {
		return array;
	}
	
	final void setArray(Object[] a) {
		array = a;
	}

	public CopyOnWriteArrayList() {
		setArray(new Object[0]);
	}
	
	public CopyOnWriteArrayList(Collection<? extends E> c) {
		Object[] elements = c.toArray();
		if(elements.getClass() != Object[].class) {
			elements = Arrays.copyOf(elements, elements.length, Object[].class);
		}
		setArray(elements);
	}
	
	public CopyOnWriteArrayList(E[] toCopyIn) {
		setArray(Arrays.copyOf(toCopyIn, toCopyIn.length, Object[].class));
	}
	
	@Override
	public int size() {
		return getArray().length;
	}

	@Override
	public boolean isEmpty() {
		return size() == 0;
	}
	
	private static boolean eq(Object o1, Object o2) {
		return (o1 == null ? o2 == null : o1.equals(o2));
	}

	private static int indexOf(Object o, Object[] elements, int index, int fence) {
		if(o == null) {
			for(int i = index; i < fence; i++) {
				if(elements[i] == null) {
					return i;
				}
			}
		} else {
			for(int i = index; i < fence; i++) {
				if(o.equals(elements[i])) {
					return i;
				}
			}
		}
		return -1;
	}
	
	private static int lastIndexOf(Object o, Object[] elements, int index) {
		if(o == null) {
			for(int i = index; i >= 0; i--) {
				if(elements[i] == null) {
					return i;
				}
			}
		} else {
			for(int i = index; i >= 0; i--) {
				if(o.equals(elements[i])) {
					return i;
				}
			}
		}
		return -1;
	}
	
	@Override
	public boolean contains(Object o) {
		Object[] elements = getArray();
		return indexOf(o, elements, 0, elements.length) >= 0;
	}
	
	@Override
	public int indexOf(Object o) {
		Object[] elememts = getArray();
		return indexOf(o, elememts, 0, elememts.length);
	}
	
	public int indexOf(E e, int index) {
		Object[] elements = getArray();
		return indexOf(e, elements, index, elements.length);
	}
	
	public int lastInfexOf(Object o) {
		Object[] elements = getArray();
		return lastIndexOf(o, elements, elements.length - 1);
	}

	public int lastIndexOf(E e, int index) {
		Object[] elements = getArray();
		return lastIndexOf(e, elements, index);
	}
	
	@SuppressWarnings("rawtypes")
	public Object clone() {
		try {
			CopyOnWriteArrayList c = (CopyOnWriteArrayList) (super.clone());
			c.resetLock();
			return c;
		} catch (CloneNotSupportedException e) {
			throw new InternalError();
		}
	}
	
	@Override
	public Object[] toArray() {
		Object[] elements = getArray();
		return Arrays.copyOf(elements, elements.length);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> T[] toArray(T[] a) {
		Object[] elements = getArray();
		int len = elements.length;
		if(a.length < len) {
			return (T[]) Arrays.copyOf(elements, len, a.getClass());
		} else {
			System.arraycopy(elements, 0, a, 0, len);
			if(a.length > len) {
				a[len] = null;
				return a;
			}
		}
				
		return null;
	}
	
	@Override
	public E get(int index) {
		return (E) (getArray()[index]);
	}
	
	@Override
	public E set(int index, E element) {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			Object[] elements = getArray();
			Object oldValue = elements[index];
			
			if(oldValue != element) {
				int len = elements.length;
				Object[] newElements = Arrays.copyOf(elements, len);
				newElements[index] = element;
				setArray(newElements);
			} else {
				setArray(elements);
			}
			return (E) oldValue;
		} finally {
			lock.unlock();
		}
	}
	
	@Override
	public boolean add(E e) {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			Object[] elements = getArray();
			int len = elements.length;
			Object[] newElements = Arrays.copyOf(elements, len + 1);
			newElements[len] = e;
			setArray(newElements);
			return true;
		} finally {
			lock.unlock();
		}
	}
	
	@Override
	public void add(int index, E element) {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			Object[] elements = getArray();
			int len = elements.length;
			if(index > len || index < 0) {
				throw new IndexOutOfBoundsException("Index: "+index+ ", Size: "+len);
			}
			Object[] newElements;
			int numMoved = len - index;
			if(numMoved == 0) {
				newElements = Arrays.copyOf(elements, len + 1);
			} else {
				newElements = new Object[len + 1];
				System.arraycopy(elements, 0, newElements, 0, index);
				System.arraycopy(elements, index, newElements, index + 1, numMoved);
			}
			newElements[index] = element;
			setArray(newElements);
		} finally {
			lock.unlock();
		}
	}
	
	@Override
	public E remove(int index) {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			Object[] elements = getArray();
			int len = elements.length;
			Object oldValue = elements[index];
			int numMoved = len - index - 1;
			if(numMoved == 0) {
				setArray(Arrays.copyOf(elements, len - 1));
			} else {
				Object[] newElements = new Object[len - 1];
				System.arraycopy(elements, 0, newElements, 0, index);
				System.arraycopy(elements, index+ 1, newElements, index, numMoved);
				setArray(newElements);
			}
			return (E)oldValue;
		} finally {
			lock.unlock();
		}
	}
	
	@Override
	public boolean remove(Object o) {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			Object[] elements = getArray();
			int len = elements.length;
			if(len != 0) {
				int newLen = len - 1;
				Object[] newElements = new Object[newLen];
				for(int i = 0; i < newLen; ++i) {
					if(eq(o, elements[i])) {
						for(int k = i + 1; k < len; ++k) {
							newElements[k - 1] = elements[k];
						}
						setArray(newElements);
						return true;
					} else {
						newElements[i] = elements[i];
					}
				}
				
				if(eq(o, elements[newLen])) {
					setArray(newElements);
					return false;
				}
			}
			return false;
		} finally {
			lock.unlock();
		}
	}
	
	private void removeRange(int fromIndex, int toIndex) {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			Object[] elements = getArray();
			int len = elements.length;
			
			if(fromIndex < 0 || fromIndex >= len || toIndex > len || toIndex < fromIndex) {
				throw new IndexOutOfBoundsException();
			}
			int newlen = len - (toIndex - fromIndex);
			int numMoved = len - toIndex;
			if(numMoved == 0) {
				setArray(Arrays.copyOf(elements, newlen));
			} else {
				Object[] newElements = new Object[newlen];
				System.arraycopy(elements, 0, newElements, 0, fromIndex);
				System.arraycopy(elements, toIndex, newElements, fromIndex, newlen);
				setArray(newElements);
			}
			
		} finally {
			lock.unlock();
		}
	} 
	
	public boolean addIfAbsent(E e) {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			Object[] elements = getArray();
			int len = elements.length;
			Object[] newElements = new Object[len + 1];
			for(int i = 0; i < len; ++i) {
				if(eq(e, elements)) {
					return false;
				} else {
					newElements[i] = elements[i];
				}
			}
			
			newElements[len] = e;
			setArray(newElements);
			return true;
		} finally {
			lock.unlock();
		}
	}
	

	@Override
	public boolean containsAll(Collection<?> c) {
		Object[] elements = getArray();
		int len = elements.length;
//		for(Object e : c) {
//			if(indexOf(e, elements, 0, len) < 0) 
//				return false;
//		}
		return true;
	}


	@Override
	public boolean removeAll(Collection<?> c) {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			Object[] elements = getArray();
			int len = elements.length;
			if(len != 0) {
				int newlen = 0;
				Object[] temp = new Object[len];
				for(int i = 0; i < len;  ++i) {
					Object element = elements[i];
					if(!c.contains(element)) {
						temp[newlen++] = element;
					}
				}
				if(newlen != len) {
					setArray(Arrays.copyOf(temp, newlen));
					return true;
				}
			}
			
			return false;
		} finally {
			lock.unlock();
		}
				
	}
	
	@Override
	public boolean retainAll(Collection<?> c) {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public boolean addAll(Collection<? extends E> c) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void clear() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int lastIndexOf(Object o) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public ListIterator<E> listIterator() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ListIterator<E> listIterator(int index) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<E> subList(int fromIndex, int toIndex) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterator<E> iterator() {
		// TODO Auto-generated method stub
		return null;
	}
	
	private static final Unsafe unsafe = Unsafe.getUnsafe();
	private static final long lockOffset;
	
	static {
		try {
			lockOffset = unsafe.objectFieldOffset(CopyOnWriteArrayList.class.getDeclaredField("lock"));
		} catch(Exception e) {throw new Error(e);}
	}

	private void resetLock() {
		unsafe.putObjectVolatile(this, lockOffset, new ReentrantLock());
	}
}
