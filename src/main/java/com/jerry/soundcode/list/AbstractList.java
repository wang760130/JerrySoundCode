package com.jerry.soundcode.list;

import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;

public abstract class AbstractList<T> extends AbstractCollection<T> implements List<T> {
	
	protected AbstractList() {}
	
	@Override
	public boolean add(T t) {
		add(size(), t);
		return true;
	}
	
	@Override
	public abstract T get(int index);
	
	@Override
	public T set(int index, T t) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void add(int index, T t) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public T remove(int index) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public int indexOf(Object o) {
		ListIterator<T> it = listIterator();
		if(o == null) {
			while(it.hasPrevious()) {
				if(it.previous() == null) {
					return it.nextIndex();
				}
			}
		} else {
			while(it.hasPrevious()) {
				if(o.equals(it.previous())) {
					return it.nextIndex();
				}
			}
		}
		return -1;
	}
	
	@Override
	public int lastIndexOf(Object o) {
		ListIterator<T> it = listIterator(size());
		if(o == null) {
			while(it.hasPrevious()) {
				if(it.previous() == null) {
					return it.nextIndex();
				}
			}
		} else {
			while(it.hasPrevious()) {
				if(o.equals(it.previous())) {
					return it.nextIndex();
				}
			}
		}
		return -1;
	}
	
	@Override
	public void clear() {
		removeRange(0, size());
	}
	
	public boolean addAll(int index, Collection<? extends T> c) {
		boolean modified = false;
		Iterator<? extends T> it = c.iterator();
		while(it.hasNext()) {
			add(index++, it.next());
			modified = true;
		}
		return modified;
	}
	
	@Override
	public Iterator<T> iterator() {
		return new Itr();
	}
	
	@Override
	public ListIterator<T> listIterator() {
		return listIterator(0);
	}
	
	@Override
	public ListIterator<T> listIterator(final int index) {
		if(index < 0 || index > size()) {
			throw new IndexOutOfBoundsException("Index: "+index);
		}
		return new ListItr(index);
	}
	
	private class Itr implements Iterator<T> {
		
		int cursor = 0;
		
		int lastRet = -1;
		
		int expectedModCount = modCount;
		
		@Override
		public boolean hasNext() {
			return cursor != size();
		}

		@Override
		public T next() {
			try {
				T next = get(cursor);
				lastRet = cursor++;
				return next;
			} catch (IndexOutOfBoundsException e) {
				checkForComodification();
				throw new NoSuchElementException();
			}
		}

		@Override
		public void remove() {
			if(lastRet == -1) {
				throw new IllegalStateException();
			}
			checkForComodification();
			
			try {
				AbstractList.this.remove(lastRet);
				if(lastRet < cursor)
					cursor--;
				expectedModCount = modCount;
			} catch (IndexOutOfBoundsException e) {
				throw new ConcurrentModificationException();
			}
		}
		
		final void checkForComodification() {
			if(modCount != expectedModCount) {
				throw new ConcurrentModificationException();
			}
		}
	}

	private class ListItr extends Itr implements ListIterator<T> {

		ListItr(int index) {
			cursor = index;
		}
		
		@Override
		public boolean hasPrevious() {
			return cursor != 0;
		}

		@Override
		public T previous() {
			checkForComodification();
			try {
				int i = cursor - 1;
				T previous = get(i);
				lastRet = cursor = i;
				return previous;
			} catch (IndexOutOfBoundsException e) {
				checkForComodification();
				throw new NoSuchElementException();
			}
		}

		@Override
		public int nextIndex() {
			return cursor;
		}

		@Override
		public int previousIndex() {
			return cursor - 1;
		}

		@Override
		public void set(T t) {
			if(lastRet == -1) {
				throw new IllegalStateException();
			}
			checkForComodification();
			
			try {
				AbstractList.this.set(lastRet, t);
				expectedModCount = modCount;
			} catch (IndexOutOfBoundsException e) {
				throw new ConcurrentModificationException();
			}
		}

		@Override
		public void add(T t) {
			checkForComodification();
			
			try {
				AbstractList.this.add(cursor++, t);
				lastRet = -1;
				expectedModCount = modCount;
			} catch (IndexOutOfBoundsException e) {
				throw new ConcurrentModificationException();
			}
		}
		
	}
	
	@Override
	public List<T> subList(int fromIndex, int toIndex) {
		return (this instanceof RandomAccess ? new RandomAccessSubList<T>(this, fromIndex, toIndex) : new SubList<T> (this, fromIndex, toIndex));
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public boolean equals(Object o) {
		if(o == this) 
			return true;
		if(!(o instanceof List)) {
			return false;
		}
		
		ListIterator<T> it1 = listIterator();
		List list = (List) o;
		ListIterator<T> it2 = list.listIterator();
		while(it1.hasNext() && it2.hasNext()) {
			T t1 = it1.next();
			T t2 = it2.next();
			if( !(t1 == null ? t2 == null : t1.equals(t2))) {
				return false;
			}
		}
		
		return !(it1.hasNext() || it2.hasNext());
	}

	@Override
	public int hashCode() {
		int hashCode = 1;
		Iterator<T> it = iterator();
		while(it.hasNext()) {
			T t = it.next();
			hashCode = 31 * hashCode + (t == null ? 0 : t.hashCode());
		}
		return hashCode;
	}
	
	protected void removeRange(int fromIndex, int toIndex) {
		ListIterator<T> it = listIterator(fromIndex);
		for(int i = 0, n = toIndex - fromIndex; i < n; i++) {
			it.next();
			it.remove();
		}
	}
	
	protected transient int modCount = 0;
}

class SubList<T> extends AbstractList<T> {
	private AbstractList<T> list;
	
	private int offset;
	private int size;
	private int expectedModCount;

	SubList(AbstractList<T> list, int fromIndex, int toIndex) {
		 if (fromIndex < 0)
	            throw new IndexOutOfBoundsException("fromIndex = " + fromIndex);
		 if (toIndex > list.size())
	            throw new IndexOutOfBoundsException("toIndex = " + toIndex);
		 if (fromIndex > toIndex)
	            throw new IllegalArgumentException("fromIndex(" + fromIndex +") > toIndex(" + toIndex + ")");
	     
		 this.list = list;   
	     this.offset = fromIndex;
	     this.size = toIndex - fromIndex;
	     this.expectedModCount = list.modCount;
	}
	
	@Override
	public List<T> subList(int fromIndex, int toIndex) {
		return new SubList<T>(this, fromIndex, toIndex);
	}

	@Override
	public T set(int index, T t) {
		rangeCheck(index);
		checkForComodification();
		return list.set(index+offset, t);
	}
	
	@Override
	public T get(int index) {
		rangeCheck(index);
		checkForComodification();
		return list.get(index + offset);
	}

	@Override
	public int size() {
		return size;
	}
	

    private void rangeCheck(int index) {
        if (index < 0 || index >= size)
            throw new IndexOutOfBoundsException("Index: "+index+",Size: "+size);
    }

    private void checkForComodification() {
        if (list.modCount != expectedModCount)
            throw new ConcurrentModificationException();
    }

}

class RandomAccessSubList<T> extends SubList<T> implements RandomAccess {

	RandomAccessSubList(AbstractList<T> list, int fromIndex, int toIndex) {
		super(list, fromIndex, toIndex);
	}
	
	@Override
	public List<T> subList(int fromIndex, int toIndex) {
        return new RandomAccessSubList<T>(this, fromIndex, toIndex);

	}
}

