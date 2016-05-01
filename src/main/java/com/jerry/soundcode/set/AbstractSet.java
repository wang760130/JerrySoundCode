package com.jerry.soundcode.set;

import com.jerry.soundcode.list.AbstractCollection;
import com.jerry.soundcode.list.Collection;
import com.jerry.soundcode.list.Iterator;

public abstract class AbstractSet<T> extends AbstractCollection<T> implements Set<T>{
	
	protected AbstractSet() {
		
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(Object o) {
		if(o == this) {
			return true;
		}
		
		if(!(o instanceof Set)) {
			return false;
		}
		
		Collection<T> c = (Collection<T>) o;
		
		if(c.size() != size()) {
			return false;
		}
			
		try {
			return containsAll(c);
		} catch (ClassCastException unused)   {
			return false;
		} catch (NullPointerException unused) {
			return false;
		}
	}
	
	@Override
	public int hashCode() {
		int h = 0;
		Iterator<T> it = iterator();
		while(it.hasNext()) {
			T t = it.next();
			if(t != null) {
				h += t.hashCode();
			}
		}
		return h;
	}
	
	@Override
	public boolean removeAll(Collection<?> c) {
		boolean modified = false;
		
		if(size() > c.size()) {
			for(Iterator<?> it = c.iterator(); it.hasNext(); ) {
				modified |= remove(it.next());
			}
		} else {
			for(Iterator<?> it = iterator(); it.hasNext(); ) {
				if(c.contains(it.next())) {
					it.remove();
					modified = true;
				}
			}
		}
		return modified;
	}
	
}
