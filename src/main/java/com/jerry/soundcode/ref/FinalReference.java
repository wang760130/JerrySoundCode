package com.jerry.soundcode.ref;

import com.jerry.soundcode.list.ReferenceQueue;

public class FinalReference<T> extends Reference<T> {

	public FinalReference(T referent, ReferenceQueue<? super T> q) {
		super(referent, q);
	}

}
