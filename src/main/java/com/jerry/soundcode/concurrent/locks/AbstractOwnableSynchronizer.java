package com.jerry.soundcode.concurrent.locks;

import java.io.Serializable;
import com.jerry.soundcode.thread.Thread;

public abstract class AbstractOwnableSynchronizer implements Serializable {

	private static final long serialVersionUID = 1L;
	
	protected AbstractOwnableSynchronizer() {}
	
	private transient Thread exclusiveOwnerThread;
	
	protected final void setExclusiveOwnerThread(Thread thread) {
		exclusiveOwnerThread = thread;
	}
	
	protected final Thread getExclusiveOwnerThread() {
		return exclusiveOwnerThread;
	}

}
