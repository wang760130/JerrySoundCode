package com.jerry.soundcode.concurrent.locks;

/**
 * 读写锁拆成读锁和写锁来理解。读锁可以共享，多个线程可以同时拥有读锁，但是写锁却只能只有一个线程拥有，
 * 而且获取写锁的时候其他线程都已经释放了读锁，而且该线程获取写锁之后，其他线程不能再获取读锁。简单的说就是写锁是排他锁，读锁是共享锁。 
 */
public interface ReadWriteLock {
	
	Lock readLock();
	
	Lock writeLock();
}
