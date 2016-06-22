package com.jerry.soundcode.thread;

import java.io.Serializable;

public abstract class AbstractOwnableSynchronizer implements Serializable {

	private static final long serialVersionUID = 1L;
	
	protected AbstractOwnableSynchronizer() {}

	private transient Thread exclusiveOwnerThread;

	public Thread getExclusiveOwnerThread() {
		return exclusiveOwnerThread;
	}

	public void setExclusiveOwnerThread(Thread t) {
		this.exclusiveOwnerThread = t;
	}
	
}
