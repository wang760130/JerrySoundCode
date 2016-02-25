package com.jerry.soundcode.concurrent.locks;

public interface ReadWriteLock {
	
	Lock readLock();
	
	Lock writeLock();
}
